package com.example.feature.recommends.ui

import com.example.feature.home.ui.modules.BillingCycleStatus
import com.example.feature.home.ui.modules.MeterCycleBaseline
import com.example.feature.home.ui.modules.MeterReadingCache
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import com.example.feature.recommends.data.MantraPreferences
import com.example.feature.recommends.data.RewardStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecommendsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TnebBillCache.clear(null)
        MeterReadingCache.clear(null)
        MantraPreferences.resetToDefaults()
        RewardStorage.clear(null)
    }

    @After
    fun tearDown() {
        TnebBillCache.clear(null)
        MeterReadingCache.clear(null)
        MantraPreferences.resetToDefaults()
        RewardStorage.clear(null)
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialDefaultStateMatchesBaseline() = runTest(testDispatcher) {
        val viewModel = RecommendsViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // 9 Mantras
        assertEquals(9, state.mantras.size)

        // Today's Progress: 0 / 9 for new user
        assertEquals(0, state.todayProgressChecked)
        assertEquals(9, state.todayProgressTotal)

        // Streak: 0 for new user
        assertEquals(0, state.streakDays)

        // Conceptual baseline billing savings (55 vs 170 kWh so far)
        assertEquals(55.0, state.billingCycleSavings.previousUnits, 0.01)
        assertEquals(170.0, state.billingCycleSavings.currentUnits, 0.01)
        assertEquals("Jun–Aug", state.billingCycleSavings.previousPeriod)
        assertEquals("Aug–Oct", state.billingCycleSavings.currentPeriod)
        assertEquals(0.0, state.billingCycleSavings.savedUnits, 0.01)
        assertEquals(0, state.billingCycleSavings.savedCredits)
        assertTrue(state.billingCycleSavings.isConsumptionIncreased)

        // Initial Energy credits for new user MUST be 0
        assertEquals(0, state.energyCredits.totalCredits)
        assertEquals(200, state.energyCredits.nextRewardTarget)
        assertEquals(200, state.energyCredits.creditsToUnlock)
    }

    @Test
    fun testMantraCheckInAwardsDailyCoinIdempotently() = runTest(testDispatcher) {
        val viewModel = RecommendsViewModel()
        advanceUntilIdle()

        val initialCredits = viewModel.uiState.value.energyCredits.totalCredits
        assertEquals(0, initialCredits)
        val initialCheckedCount = viewModel.uiState.value.todayProgressChecked

        // Mantra 2 (Regular cleaning of AC filters) is initially unchecked
        val mantra2Initial = viewModel.uiState.value.mantras.find { it.id == 2 }
        assertNotNull(mantra2Initial)
        assertFalse(mantra2Initial!!.isCheckedIn)

        // Check in Mantra 2 -> Awards 1 coin for first qualifying activity today
        viewModel.toggleMantraCheckIn(null, 2)
        advanceUntilIdle()

        val updatedStateAfterCheckIn = viewModel.uiState.value
        val mantra2Updated = updatedStateAfterCheckIn.mantras.find { it.id == 2 }
        assertNotNull(mantra2Updated)
        assertTrue("Mantra 2 should now be checked in", mantra2Updated!!.isCheckedIn)
        assertEquals("Checked count should increase by 1", initialCheckedCount + 1, updatedStateAfterCheckIn.todayProgressChecked)
        assertEquals(
            "First qualifying mantra check-in awards 1 coin",
            1,
            updatedStateAfterCheckIn.energyCredits.totalCredits
        )
        assertEquals(1, updatedStateAfterCheckIn.coinBalance)
        assertEquals(1, updatedStateAfterCheckIn.streakDays)

        // Check in Mantra 3 on the same day -> Must NOT grant additional daily coins
        viewModel.toggleMantraCheckIn(null, 3)
        advanceUntilIdle()

        val stateAfterSecondCheckIn = viewModel.uiState.value
        assertEquals("Checked count should increase to 2", 2, stateAfterSecondCheckIn.todayProgressChecked)
        assertEquals(
            "Additional check-in on the same day must not award additional daily coins",
            1,
            stateAfterSecondCheckIn.energyCredits.totalCredits
        )

        // Toggle Mantra 2 back to unchecked -> Must NOT revoke earned daily reward
        viewModel.toggleMantraCheckIn(null, 2)
        advanceUntilIdle()

        val stateAfterUncheck = viewModel.uiState.value
        val mantra2Unchecked = stateAfterUncheck.mantras.find { it.id == 2 }
        assertNotNull(mantra2Unchecked)
        assertFalse("Mantra 2 should be unchecked again", mantra2Unchecked!!.isCheckedIn)
        assertEquals("Checked count should now be 1", 1, stateAfterUncheck.todayProgressChecked)
        assertEquals(
            "Unchecking a mantra must not revoke an already-earned daily reward",
            1,
            stateAfterUncheck.energyCredits.totalCredits
        )
        assertEquals(1, stateAfterUncheck.streakDays)
    }

    @Test
    fun testBillVerifiedSavingsCalculatedFromRealBills() = runTest(testDispatcher) {
        val testBill = TnebConsumerBillData(
            consumerNumber = "041234567890",
            consumerName = "K. Ramanathan",
            address = "No. 42 Anna Nagar, Chennai",
            tariffCode = "LT-IA",
            serviceCategory = "Domestic",
            isDuesNil = true,
            bills = listOf(
                TnebBillRecord(
                    id = "B-2024-10",
                    period = "October 2024",
                    units = "180",
                    kwh = "12500",
                    amountDue = "₹ 650",
                    dueDate = "15/10/2024",
                    status = "PAID",
                    isPaid = true,
                    assessmentDate = "10/10/2024"
                ),
                TnebBillRecord(
                    id = "B-2024-08",
                    period = "August 2024",
                    units = "240",
                    kwh = "12260",
                    amountDue = "₹ 890",
                    dueDate = "15/08/2024",
                    status = "PAID",
                    isPaid = true,
                    assessmentDate = "10/08/2024"
                )
            )
        )

        TnebBillCache.save(null, testBill)

        val viewModel = RecommendsViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Verified reduction: 240 - 180 = 60 units saved
        assertEquals(240.0, state.billingCycleSavings.previousUnits, 0.01)
        assertEquals(180.0, state.billingCycleSavings.currentUnits, 0.01)
        assertEquals(60.0, state.billingCycleSavings.savedUnits, 0.01)
        assertEquals(0, state.billingCycleSavings.savedCredits) // Historical baseline does not award credits

        // Historical savings are NEVER backfilled into credit balance (remains 0)
        assertEquals(0, state.energyCredits.totalCredits)
    }

    @Test
    fun testBillingCycleConsumptionIncreasedZeroCredits() = runTest(testDispatcher) {
        // Example from prompt:
        // Apr–Jun = 27 kWh
        // Jun–Aug = 95 kWh
        // Difference = +68 kWh
        // Saved = 0 kWh, Energy Credits = 0
        val testBill = TnebConsumerBillData(
            consumerNumber = "041234567890",
            bills = listOf(
                TnebBillRecord(
                    id = "B-2024-08",
                    period = "Jun–Aug",
                    units = "95",
                    kwh = "12595",
                    assessmentDate = "10/08/2024"
                ),
                TnebBillRecord(
                    id = "B-2024-06",
                    period = "Apr–Jun",
                    units = "27",
                    kwh = "12500",
                    assessmentDate = "10/06/2024"
                )
            )
        )

        TnebBillCache.save(null, testBill)

        val viewModel = RecommendsViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(27.0, state.billingCycleSavings.previousUnits, 0.01)
        assertEquals(95.0, state.billingCycleSavings.currentUnits, 0.01)
        assertTrue("Consumption should be marked as increased", state.billingCycleSavings.isConsumptionIncreased)
        assertEquals(68.0, state.billingCycleSavings.differenceUnits, 0.01)
        assertEquals(0.0, state.billingCycleSavings.savedUnits, 0.01)
        assertEquals(0, state.billingCycleSavings.savedCredits)

        // Energy credits must NOT be awarded for historical cycles
        assertEquals(0, state.energyCredits.totalCredits)
    }

    @Test
    fun testBillingCycleConsumptionDecreasedCreditsAwarded() = runTest(testDispatcher) {
        // Example from prompt:
        // Previous (Apr–Jun) = 92 kWh
        // Current (Jun–Aug) = 27 kWh
        // Saved = 65 kWh, +65 Energy Credits
        val testBill = TnebConsumerBillData(
            consumerNumber = "041234567890",
            bills = listOf(
                TnebBillRecord(
                    id = "B-2024-08",
                    period = "Jun–Aug",
                    units = "27",
                    kwh = "12527",
                    assessmentDate = "10/08/2024"
                ),
                TnebBillRecord(
                    id = "B-2024-06",
                    period = "Apr–Jun",
                    units = "92",
                    kwh = "12500",
                    assessmentDate = "10/06/2024"
                )
            )
        )

        TnebBillCache.save(null, testBill)

        val viewModel = RecommendsViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(92.0, state.billingCycleSavings.previousUnits, 0.01)
        assertEquals(27.0, state.billingCycleSavings.currentUnits, 0.01)
        assertFalse("Consumption should NOT be marked as increased", state.billingCycleSavings.isConsumptionIncreased)
        assertEquals(65.0, state.billingCycleSavings.savedUnits, 0.01)
        assertEquals(65.0, state.billingCycleSavings.differenceUnits, 0.01)
        assertEquals(0, state.billingCycleSavings.savedCredits) // Historical baseline

        // Energy credits: never backfilled from historical cycles
        assertEquals(0, state.energyCredits.totalCredits)
    }

    @Test
    fun testTwoCompletedCycles_DirectSubtraction_NoDailyRateNormalization() = runTest(testDispatcher) {
        // Direct subtraction rule:
        // Previous: 92 kWh
        // Current: 95 kWh
        // 92 - 95 = -3 kWh -> Consumption Increased by 3 kWh!
        val testBill = TnebConsumerBillData(
            consumerNumber = "041234567890",
            bills = listOf(
                TnebBillRecord(
                    id = "B-2024-08",
                    period = "Jun–Aug",
                    units = "95",
                    assessmentDate = "11/08/2024" // 62 days from 10/06/2024
                ),
                TnebBillRecord(
                    id = "B-2024-06",
                    period = "Apr–Jun",
                    units = "92",
                    assessmentDate = "10/06/2024" // 60 days from 11/04/2024
                ),
                TnebBillRecord(
                    id = "B-2024-04",
                    period = "Feb–Apr",
                    units = "90",
                    assessmentDate = "11/04/2024"
                )
            )
        )

        TnebBillCache.save(null, testBill)

        val viewModel = RecommendsViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value

        assertEquals(92.0, state.billingCycleSavings.previousUnits, 0.01)
        assertEquals(95.0, state.billingCycleSavings.currentUnits, 0.01)
        assertTrue("Under core direct subtraction rule, 95 > 92 is an increase", state.billingCycleSavings.isConsumptionIncreased)
        assertEquals(3.0, state.billingCycleSavings.differenceUnits, 0.01)
        assertEquals(0.0, state.billingCycleSavings.savedUnits, 0.01)
        assertEquals(0, state.billingCycleSavings.savedCredits)
        assertEquals(0, state.energyCredits.totalCredits)
    }

    @Test
    fun testCycleNavigationBackwardAndForward() = runTest(testDispatcher) {
        val viewModel = RecommendsViewModel()
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(4, state.availableCycles.size)
        // Default is index 3: Aug–Oct (Current/Live)
        assertEquals(3, state.selectedCycleIndex)
        assertEquals("Aug–Oct", state.availableCycles[state.selectedCycleIndex].periodLabel)
        assertEquals(BillingCycleStatus.CURRENT, state.availableCycles[state.selectedCycleIndex].status)

        // Navigate back to Jun–Aug (Completed)
        viewModel.selectPreviousCycle()
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(2, state.selectedCycleIndex)
        assertEquals("Jun–Aug", state.availableCycles[state.selectedCycleIndex].periodLabel)
        assertEquals(BillingCycleStatus.COMPLETED, state.availableCycles[state.selectedCycleIndex].status)

        // Navigate back to Apr–Jun (Completed)
        viewModel.selectPreviousCycle()
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(1, state.selectedCycleIndex)
        assertEquals("Apr–Jun", state.availableCycles[state.selectedCycleIndex].periodLabel)
        assertEquals(BillingCycleStatus.COMPLETED, state.availableCycles[state.selectedCycleIndex].status)
        // Compared with Feb–Apr (92 kWh): 27 vs 92 -> 65 kWh Saved!
        assertEquals(65.0, state.cycleComparison?.savedUnits ?: 0.0, 0.01)
        assertEquals(0, state.cycleComparison?.savedCredits ?: 0) // Historical baseline

        // Navigate forward to Jun–Aug
        viewModel.selectNextCycle()
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(2, state.selectedCycleIndex)
        assertEquals("Jun–Aug", state.availableCycles[state.selectedCycleIndex].periodLabel)

        // Navigate forward to Aug–Oct (Current)
        viewModel.selectNextCycle()
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(3, state.selectedCycleIndex)
        assertEquals("Aug–Oct", state.availableCycles[state.selectedCycleIndex].periodLabel)
        assertEquals(BillingCycleStatus.CURRENT, state.availableCycles[state.selectedCycleIndex].status)
    }
}
