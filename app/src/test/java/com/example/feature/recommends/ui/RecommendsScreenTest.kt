package com.example.feature.recommends.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.User
import com.example.core.session.UserSession
import com.example.feature.home.ui.modules.MeterReadingCache
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import com.example.feature.recommends.data.BillVerificationEngine
import com.example.feature.recommends.data.MantraPreferences
import com.example.feature.recommends.data.RewardStorage
import com.example.feature.recommends.model.BillVerificationStatus
import com.example.feature.recommends.model.CoinEventType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.feature.recommends.model.StreakState
import com.example.feature.recommends.model.BillingCycleSavings

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RecommendsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        UserSession.init(context)
        UserSession.clear(context)
        TnebBillCache.init(context)
        TnebBillCache.clear(context)
        MeterReadingCache.init(context)
        MeterReadingCache.clear(context)
        MantraPreferences.init(context)
        MantraPreferences.resetToDefaults()
        RewardStorage.init(context)
        RewardStorage.clear(null)
    }

    @After
    fun tearDown() {
        UserSession.clear(context)
        TnebBillCache.clear(context)
        MeterReadingCache.clear(context)
        RewardStorage.clear(null)
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialCoinsAndStreakUiStateForNewUser() = runTest(testDispatcher) {
        val testUser = User(id = "user_new_1", name = "Ravi", username = "ravi", mobileNumber = "9876543210")
        UserSession.saveUser(context, testUser)

        val viewModel = RecommendsViewModel()
        viewModel.initPreferences(context)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // Coin Balance & Ledger
        assertEquals(0, state.coinBalance)
        assertTrue(state.coinTransactions.isEmpty())

        // 7-Day Streak
        assertEquals(0, state.streakDays)
        assertEquals(0, state.streakState.currentStreak)
        assertEquals(0, state.streakState.daysInCurrentCycle)
        assertEquals(1, state.streakState.currentMilestoneCycle)
        assertEquals(0f, state.streakState.progressTowardsNextMilestone, 0.01f)

        // Today's Progress
        assertEquals(0, state.todayProgressChecked)
        assertEquals(9, state.todayProgressTotal)
        assertEquals(9, state.mantras.size)

        // Bill Verification Result
        assertNull(state.billVerificationResult)
    }

    @Test
    fun testLiveCoinBalanceAndStreakUpdateOnMantraCheckIn() = runTest(testDispatcher) {
        val testUser = User(id = "user_checkin_1", name = "Priya", username = "priya", mobileNumber = "9876543211")
        UserSession.saveUser(context, testUser)

        val viewModel = RecommendsViewModel()
        viewModel.initPreferences(context)
        advanceUntilIdle()

        // 1. Initial State: 0 coins, 0 streak
        assertEquals(0, viewModel.uiState.value.coinBalance)
        assertEquals(0, viewModel.uiState.value.streakDays)

        // 2. Check in Mantra 1 -> Earns 1 daily coin, streak becomes 1
        viewModel.toggleMantraCheckIn(context, 1)
        advanceUntilIdle()

        val stateAfterFirst = viewModel.uiState.value
        assertEquals("Coin balance must update to 1 live", 1, stateAfterFirst.coinBalance)
        assertEquals("Streak must become 1", 1, stateAfterFirst.streakDays)
        assertEquals(1, stateAfterFirst.streakState.daysInCurrentCycle)
        assertEquals(1, stateAfterFirst.todayProgressChecked)
        assertEquals(1, stateAfterFirst.coinTransactions.size)
        assertEquals(CoinEventType.DAILY_ACTIVITY, stateAfterFirst.coinTransactions.first().eventType)

        // 3. Check in Mantra 2 on same day -> No extra coins, streak remains 1
        viewModel.toggleMantraCheckIn(context, 2)
        advanceUntilIdle()

        val stateAfterSecond = viewModel.uiState.value
        assertEquals("Coin balance must remain 1 for same calendar day", 1, stateAfterSecond.coinBalance)
        assertEquals("Streak remains 1", 1, stateAfterSecond.streakDays)
        assertEquals(2, stateAfterSecond.todayProgressChecked)
        assertEquals(1, stateAfterSecond.coinTransactions.size)

        // 4. Uncheck Mantra 1 -> Must NEVER revoke earned coins or streak
        viewModel.toggleMantraCheckIn(context, 1)
        advanceUntilIdle()

        val stateAfterUncheck = viewModel.uiState.value
        assertEquals("Coins must remain 1 after unchecking mantra", 1, stateAfterUncheck.coinBalance)
        assertEquals("Streak must remain 1 after unchecking mantra", 1, stateAfterUncheck.streakDays)
        assertEquals("Checked mantras count drops to 1", 1, stateAfterUncheck.todayProgressChecked)
        assertEquals(1, stateAfterUncheck.coinTransactions.size)
    }

    @Test
    fun testSevenDayStreakMilestoneAwardsBonusCoinsInUiState() = runTest(testDispatcher) {
        val testUser = User(id = "user_milestone_1", name = "Karthik", username = "karthik", mobileNumber = "9876543212")
        UserSession.saveUser(context, testUser)

        // Simulate 7 consecutive qualifying days in RewardStorage
        for (i in 1..7) {
            RewardStorage.recordDailyActivity(testUser.id, "mantra_1", "2026-10-%02d".format(i))
        }

        val viewModel = RecommendsViewModel()
        viewModel.initPreferences(context)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 7 daily coins + 5 milestone bonus coins = 12 coins
        assertEquals(12, state.coinBalance)
        assertEquals(7, state.streakDays)
        assertEquals(7, state.streakState.currentStreak)
        assertEquals(7, state.streakState.daysInCurrentCycle)
        assertEquals(1, state.streakState.currentMilestoneCycle)
        assertEquals(14, state.streakState.nextMilestoneTarget)
        assertTrue(state.streakState.claimedMilestones.contains(7))

        // Ledger has 8 transactions (7 daily + 1 milestone)
        assertEquals(8, state.coinTransactions.size)
        val milestoneTx = state.coinTransactions.find { it.eventType == CoinEventType.STREAK_MILESTONE }
        assertNotNull(milestoneTx)
        assertEquals(5, milestoneTx!!.amount)
    }

    @Test
    fun testBillVerificationStatusPillShowsVerifiedReductionAndAwardsCoins() = runTest(testDispatcher) {
        val testUser = User(id = "user_bill_1", name = "Meena", username = "meena", mobileNumber = "9876543213")
        UserSession.saveUser(context, testUser)

        // Historical baseline bill: 240 units / 60 days = 4.0 kWh/day
        val baselineBill = TnebBillRecord(
            id = "B-2024-08",
            period = "Jun–Aug 2024",
            units = "240",
            assessmentDate = "10/08/2024"
        )
        val initialData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(baselineBill))
        TnebBillCache.save(context, initialData)

        // Pre-initialize baseline
        BillVerificationEngine.initializeBaselineIfEmpty(testUser.id, initialData, RewardStorage)

        // Now a newly received bill arrives: 180 units (~2.95 kWh/day -> ~26% reduction)
        val newBill = TnebBillRecord(
            id = "B-2024-10",
            period = "Aug–Oct 2024",
            units = "180",
            assessmentDate = "10/10/2024"
        )
        val updatedData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(newBill, baselineBill))
        TnebBillCache.save(context, updatedData)

        val viewModel = RecommendsViewModel()
        viewModel.initPreferences(context)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val result = state.billVerificationResult
        assertNotNull(result)
        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, result!!.status)
        assertEquals("B-2024-10", result.billId)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(50, result.reductionBonusEarned)

        // Authoritative ledger has 60 coins (10 processing + 50 reduction bonus)
        assertEquals(60, state.coinBalance)
        assertEquals(2, state.coinTransactions.size)
    }

    @Test
    fun testInsufficientBillHistoryShowsAccurateStatusAndCoins() = runTest(testDispatcher) {
        val testUser = User(id = "user_nobill_1", name = "Anand", username = "anand", mobileNumber = "9876543214")
        UserSession.saveUser(context, testUser)

        // First bill arrives with NO prior history
        val firstBill = TnebBillRecord(
            id = "B-FIRST",
            period = "Aug–Oct 2024",
            units = "150",
            assessmentDate = "10/10/2024"
        )
        // Mark baseline as initialized with empty prior bills
        RewardStorage.markBaselineInitialized(testUser.id)

        val billData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(firstBill))
        TnebBillCache.save(context, billData)

        val viewModel = RecommendsViewModel()
        viewModel.initPreferences(context)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val result = state.billVerificationResult
        assertNotNull(result)
        assertEquals(BillVerificationStatus.INSUFFICIENT_HISTORY, result!!.status)
        assertEquals("Not enough bill history to verify yet.", result.userMessage)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(0, result.reductionBonusEarned)

        // 10 coins earned for processing newly received bill
        assertEquals(10, state.coinBalance)
        assertEquals(1, state.coinTransactions.size)
    }

    @Test
    fun testUserIsolationAcrossSessionChanges() = runTest(testDispatcher) {
        // User 1 performs activity
        val user1 = User(id = "user_alpha", name = "Alpha", username = "alpha", mobileNumber = "9876543215")
        UserSession.saveUser(context, user1)
        RewardStorage.recordDailyActivity(user1.id, "mantra_1", "2026-10-01")

        val vm1 = RecommendsViewModel()
        vm1.initPreferences(context)
        advanceUntilIdle()
        assertEquals(1, vm1.uiState.value.coinBalance)
        assertEquals(1, vm1.uiState.value.streakDays)

        // Switch to User 2
        val user2 = User(id = "user_beta", name = "Beta", username = "beta", mobileNumber = "9876543216")
        UserSession.saveUser(context, user2)

        val vm2 = RecommendsViewModel()
        vm2.initPreferences(context)
        advanceUntilIdle()
        assertEquals("User 2 must start with 0 coins", 0, vm2.uiState.value.coinBalance)
        assertEquals("User 2 must have 0 streak", 0, vm2.uiState.value.streakDays)
        assertTrue(vm2.uiState.value.coinTransactions.isEmpty())

        // User 1 data remains untouched
        val user1Data = RewardStorage.getRewardData(user1.id)
        assertEquals(1, user1Data.balance)
        assertEquals(1, user1Data.streakState.currentStreak)
    }

    @Test
    fun testCoinsCardAndBillingCardInitialCollapsedAndExpandIndependently() {
        val testState = com.example.feature.recommends.model.RecommendsUiState(
            coinBalance = 5,
            streakDays = 3,
            streakState = StreakState(currentStreak = 3),
            billingCycleSavings = BillingCycleSavings(
                previousUnits = 302.0,
                previousPeriod = "Aug 2024",
                currentUnits = 180.0,
                currentPeriod = "Oct 2024",
                savedUnits = 122.0,
                savedCredits = 10,
                isConsumptionIncreased = false
            )
        )

        composeTestRule.setContent {
            RecommendsScreen(
                uiState = testState,
                onMantraClick = {},
                onNavigateToHome = {},
                onNavigateToProfile = {}
            )
        }

        // 1. Initial collapsed summary is displayed / exists
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("SolarEase Coins").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 Day Streak").assertIsDisplayed()
        composeTestRule.onNodeWithText("Last Billing Cycle").assertExists()
        composeTestRule.onNodeWithText("302 kWh").assertExists()
        composeTestRule.onNodeWithText("180 kWh").assertExists()
        composeTestRule.onNodeWithText("122 kWh").assertExists()

        // 2. Expanded-only content is not displayed while collapsed
        composeTestRule.onNodeWithText("Earning Rules & History").assertDoesNotExist()

        // 3. Expand Coins card
        composeTestRule.onNodeWithContentDescription("Expand SolarEase Coins card").performClick()
        composeTestRule.waitForIdle()

        // 4. Coins card content is revealed
        composeTestRule.onNodeWithText("Earning Rules & History").assertExists()
        composeTestRule.onNodeWithText("Recent Activity").assertExists()

        // 5. Billing card remains collapsed independently
        composeTestRule.onNodeWithText("Select Cycle").assertDoesNotExist()

        // 6. Expand Billing card
        composeTestRule.onNodeWithContentDescription("Expand Last Billing Cycle card").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("• Saved").assertExists()

        // 7. Collapse Coins card without collapsing Billing card
        composeTestRule.onNodeWithContentDescription("Collapse SolarEase Coins card").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Earning Rules & History").assertDoesNotExist()
        composeTestRule.onNodeWithText("• Saved").assertExists()
    }

    @Test
    fun testMantrasSectionExpandedByDefaultOnNewDay() {
        val testState = com.example.feature.recommends.model.RecommendsUiState(
            mantras = MantraPreferences.mantrasState.value,
            isMantrasExpanded = true
        )

        composeTestRule.setContent {
            RecommendsScreen(
                uiState = testState,
                onMantraClick = {},
                onNavigateToHome = {},
                onNavigateToProfile = {}
            )
        }

        // Section header and all 9 mantras exist
        composeTestRule.onNodeWithText("9 Energy-Saving Mantras").assertExists()
        composeTestRule.onNodeWithText("Switch off equipment after use").assertExists()
        composeTestRule.onNodeWithText("Replace frequently used lighting with Solar LED").assertExists()
        composeTestRule.onNodeWithContentDescription("Collapse 9 Energy-Saving Mantras").assertExists()

        // Progress pill should not be shown when expanded
        composeTestRule.onNodeWithTag("mantras_progress_pill", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testMantrasSectionCollapseAndExpandToggle() {
        val isExpandedState = androidx.compose.runtime.mutableStateOf(true)
        val mantrasList = MantraPreferences.mantrasState.value

        composeTestRule.setContent {
            val currentState = com.example.feature.recommends.model.RecommendsUiState(
                mantras = mantrasList,
                isMantrasExpanded = isExpandedState.value
            )
            RecommendsScreen(
                uiState = currentState,
                onToggleMantrasExpanded = { isExpandedState.value = !isExpandedState.value },
                onMantraClick = {},
                onNavigateToHome = {},
                onNavigateToProfile = {}
            )
        }

        // Initially expanded
        composeTestRule.onNodeWithText("Switch off equipment after use").assertExists()
        composeTestRule.onNodeWithContentDescription("Collapse 9 Energy-Saving Mantras").assertExists()

        // Collapse section via chevron
        composeTestRule.onNodeWithContentDescription("Collapse 9 Energy-Saving Mantras").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // After collapse: 9 cards hidden, header retained, progress pill displayed
        composeTestRule.onNodeWithText("9 Energy-Saving Mantras").assertExists()
        composeTestRule.onNodeWithText("Check in regularly, build the habit and reduce your electricity consumption.").assertExists()
        composeTestRule.onNodeWithTag("mantras_progress_pill", useUnmergedTree = true).assertExists()
        composeTestRule.onNodeWithText("Switch off equipment after use").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Expand 9 Energy-Saving Mantras").assertExists()

        // Expand again via header text
        composeTestRule.onNodeWithText("9 Energy-Saving Mantras").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Cards visible again
        composeTestRule.onNodeWithText("Switch off equipment after use").assertExists()
        composeTestRule.onNodeWithTag("mantras_progress_pill", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testMantrasDailyStatePersistenceAndResetOnNextDay() = runTest(testDispatcher) {
        val user = User(id = "user_daily_1", name = "Sanjay", username = "sanjay", mobileNumber = "9876543211")
        UserSession.saveUser(context, user)

        // Day 1: 2026-10-10
        MantraPreferences.setMantrasExpanded(context, isExpanded = true, date = "2026-10-10")
        assertEquals(true, MantraPreferences.mantrasExpandedState.value)

        // User collapses the section on Day 1
        MantraPreferences.setMantrasExpanded(context, isExpanded = false, date = "2026-10-10")
        assertEquals(false, MantraPreferences.mantrasExpandedState.value)

        // Navigating away / re-evaluating same day maintains collapsed preference
        MantraPreferences.checkDateAndResetIfNewDay(context, overrideDate = "2026-10-10")
        assertEquals(false, MantraPreferences.mantrasExpandedState.value)

        // Next calendar day: 2026-10-11
        MantraPreferences.checkDateAndResetIfNewDay(context, overrideDate = "2026-10-11")
        assertEquals(true, MantraPreferences.mantrasExpandedState.value)
    }

    @Test
    fun testMantraCheckInDoesNotCollapseAndAwardsCoinsWithoutRevokingOnUncheck() = runTest(testDispatcher) {
        val user = User(id = "user_mantra_test", name = "Priya", username = "priya", mobileNumber = "9876543212")
        UserSession.saveUser(context, user)

        val viewModel = RecommendsViewModel()
        viewModel.initPreferences(context)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isMantrasExpanded)
        assertEquals(0, viewModel.uiState.value.coinBalance)
        assertEquals(0, viewModel.uiState.value.todayProgressChecked)

        // 1. Check in Mantra 1
        viewModel.toggleMantraCheckIn(context, 1)
        advanceUntilIdle()

        val stateAfterCheck = viewModel.uiState.value
        assertTrue("Section must NOT automatically collapse after check-in", stateAfterCheck.isMantrasExpanded)
        assertEquals(1, stateAfterCheck.todayProgressChecked)
        assertEquals(1, stateAfterCheck.coinBalance)

        // 2. Uncheck Mantra 1
        viewModel.toggleMantraCheckIn(context, 1)
        advanceUntilIdle()

        val stateAfterUncheck = viewModel.uiState.value
        assertTrue("Section remains expanded after uncheck", stateAfterUncheck.isMantrasExpanded)
        assertEquals(0, stateAfterUncheck.todayProgressChecked)
        assertEquals("Earned coins must not be revoked on uncheck", 1, stateAfterUncheck.coinBalance)
    }

    @Test
    fun testCardsAppearInRequestedOrder() {
        val testState = com.example.feature.recommends.model.RecommendsUiState(
            mantras = MantraPreferences.mantrasState.value,
            isMantrasExpanded = true
        )

        composeTestRule.setContent {
            RecommendsScreen(
                uiState = testState,
                onMantraClick = {},
                onNavigateToHome = {},
                onNavigateToProfile = {}
            )
        }

        val coinsTop = composeTestRule.onNodeWithText("SolarEase Coins").getUnclippedBoundsInRoot().top
        val billingTop = composeTestRule.onNodeWithText("Last Billing Cycle").getUnclippedBoundsInRoot().top
        val todayProgressTop = composeTestRule.onNodeWithText("Today's Mantra Progress").getUnclippedBoundsInRoot().top

        assertTrue("Last Billing Cycle must appear after SolarEase Coins", billingTop > coinsTop)
        assertTrue("Today's Mantra Progress must appear after Last Billing Cycle", todayProgressTop > billingTop)

        // 4. 9 Energy-Saving Mantras section exists below
        composeTestRule.onNodeWithText("9 Energy-Saving Mantras").assertExists()

        // 5. Solar Vendor card exists after Mantras
        composeTestRule.onNodeWithText("How much could your home reduce its carbon footprint?").assertExists()
        composeTestRule.onNodeWithText("Explore Solar").assertExists()
    }

    @Test
    fun testMantraCheckInDisplaysCheckedInAndUncheckDisplaysCheckIn() = runTest(testDispatcher) {
        val user = User(id = "user_ui_checkin", name = "Test", username = "test", mobileNumber = "9876543210")
        UserSession.saveUser(context, user)

        val viewModel = RecommendsViewModel()
        viewModel.initPreferences(context)
        advanceUntilIdle()

        composeTestRule.setContent {
            val state by viewModel.uiState.collectAsState()
            RecommendsScreen(
                uiState = state,
                onMantraClick = { id -> viewModel.toggleMantraCheckIn(context, id) },
                onNavigateToHome = {},
                onNavigateToProfile = {}
            )
        }

        // Initially unchecked -> Displays "Check In"
        composeTestRule.onAllNodesWithText("Check In").assertCountEquals(9)
        composeTestRule.onNodeWithText("Checked In").assertDoesNotExist()

        // Check in Mantra 1 by clicking "Check In"
        composeTestRule.onAllNodesWithText("Check In")[0].performScrollTo().performClick()
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        // Now Mantra 1 displays "Checked In"
        composeTestRule.onNodeWithText("Checked In").assertExists()
        composeTestRule.onAllNodesWithText("Check In").assertCountEquals(8)
        assertEquals(1, viewModel.uiState.value.todayProgressChecked)

        // Uncheck Mantra 1 by clicking "Checked In"
        composeTestRule.onNodeWithText("Checked In").performScrollTo().performClick()
        advanceUntilIdle()
        composeTestRule.waitForIdle()

        // Returns to "Check In"
        composeTestRule.onNodeWithText("Checked In").assertDoesNotExist()
        composeTestRule.onAllNodesWithText("Check In").assertCountEquals(9)
        assertEquals(0, viewModel.uiState.value.todayProgressChecked)
    }

    @Test
    fun testMantraCheckInPersistenceAndDateRollover() = runTest(testDispatcher) {
        val user = User(id = "user_rollover", name = "Test", username = "test", mobileNumber = "9876543210")
        UserSession.saveUser(context, user)

        // Check in Mantra 1 on Day 1 (2026-10-10)
        MantraPreferences.toggleCheckIn(context, 1, date = "2026-10-10")
        assertTrue(MantraPreferences.mantrasState.value.find { it.id == 1 }!!.isCheckedIn)

        // Same day reload maintains checked-in state
        MantraPreferences.checkDateAndResetIfNewDay(context, overrideDate = "2026-10-10")
        assertTrue("Same day reload must keep checked in state", MantraPreferences.mantrasState.value.find { it.id == 1 }!!.isCheckedIn)

        // Next calendar day (2026-10-11) resets check-in state
        MantraPreferences.checkDateAndResetIfNewDay(context, overrideDate = "2026-10-11")
        assertFalse("New calendar day must reset check-in state to unchecked", MantraPreferences.mantrasState.value.find { it.id == 1 }!!.isCheckedIn)
    }

    @Test
    fun testSolarVendorCardNavigationAndAccent() {
        var exploredSolarClicked = false
        val testState = com.example.feature.recommends.model.RecommendsUiState()

        composeTestRule.setContent {
            RecommendsScreen(
                uiState = testState,
                onMantraClick = {},
                onNavigateToHome = {},
                onNavigateToProfile = {},
                onNavigateToSolarVendor = { exploredSolarClicked = true }
            )
        }

        // Click Explore Solar button on SolarVendorCard
        composeTestRule.onNodeWithText("Explore Solar").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(exploredSolarClicked)
    }
}
