package com.example.feature.home.ui.modules

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.core.session.User
import com.example.core.session.UserSession
import com.example.feature.home.ui.HomeViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ColdRestartPersistenceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        UserSession.clear(context)
        TnebBillCache.clear(context)
        MeterReadingCache.clear(context)
        UserSession.init(context)
        TnebBillCache.init(context)
        MeterReadingCache.init(context)
    }

    @After
    fun tearDown() {
        UserSession.clear(context)
        TnebBillCache.clear(context)
        MeterReadingCache.clear(context)
    }

    /**
     * TEST 1:
     * Sign in -> Connect bill -> Enter reading -> Cold restart -> Everything restored.
     */
    @Test
    fun test1_coldRestartRestoresCompleteUserBillAndMeterState() {
        // 1. Sign in
        val testUser = User(
            id = "USR-101",
            name = "Sanjai Kumar",
            username = "sanjai",
            mobileNumber = "9876543210"
        )
        UserSession.saveUser(context, testUser)

        // 2. Connect TNEB bill
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            consumerName = "Sanjai Kumar",
            tariffCode = "TANGEDCO LT-1A · DOMESTIC",
            isDuesNil = true,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-98765",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450", // Baseline
                    units = "302",  // Historical
                    amountPaid = "1150",
                    isPaid = true
                )
            )
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        // 3. Enter current meter reading
        val saveRes = MeterReadingCache.saveReading(12582.0, "2026-09-30")
        assertTrue(saveRes is MeterValidationResult.Success)

        // 4. Verify initial metrics
        val initialMetrics = MeterReadingCache.metrics.value
        assertNotNull(initialMetrics)
        assertEquals(132.0, initialMetrics!!.unitsConsumed, 0.01)

        // ==========================================
        // 5. SIMULATE COLD RESTART (Process Death)
        // Reset in-memory reference, re-run init() from SharedPreferences
        // ==========================================
        UserSession.currentUser = null
        // Re-initialize from persistent SharedPreferences
        UserSession.init(context)
        TnebBillCache.init(context)
        MeterReadingCache.init(context)

        // 6. Verify Firebase/User session restored
        assertNotNull(UserSession.currentUser)
        assertEquals("USR-101", UserSession.currentUser?.id)

        // 7. Verify connected bill remains connected
        val restoredBill = TnebBillCache.billData.value
        assertNotNull(restoredBill)
        assertEquals("09234567890", restoredBill?.consumerNumber)

        // 8. Verify baseline remained
        val restoredBase = MeterReadingCache.baseline.value
        assertNotNull(restoredBase)
        assertEquals(12450.0, restoredBase!!.baselineReading, 0.01)

        // 9. Verify today's / latest reading remains
        val restoredReadings = MeterReadingCache.readings.value
        assertEquals(1, restoredReadings.size)
        assertEquals(12582.0, restoredReadings[0].readingValue, 0.01)

        // 10. Verify current consumption remains
        val restoredMetrics = MeterReadingCache.metrics.value
        assertNotNull(restoredMetrics)
        assertEquals(132.0, restoredMetrics!!.unitsConsumed, 0.01)

        // 11. Verify estimated bill is calculated (132 units <= 200 free units in TNEB bi-monthly domestic)
        assertEquals(0.0, restoredMetrics.estimatedBill, 0.01)

        // 12. Verify graph remains
        val graphPoints = MeterReadingCache.getGraphPoints()
        assertEquals(2, graphPoints.size) // baseline + today
        assertEquals(12450.0, graphPoints[0].readingValue, 0.01)
        assertEquals(12582.0, graphPoints[1].readingValue, 0.01)

        // 13. Verify HomeViewModel starts with restored state (NO FLASH)
        val homeViewModel = HomeViewModel()
        val uiState = homeViewModel.uiState.value
        assertTrue(uiState.hasConnectedBill)
        assertTrue(uiState.hasCurrentReading)
        assertEquals(132.0, uiState.currentEnergyMetrics?.unitsConsumed ?: 0.0, 0.01)
        assertEquals(2, uiState.meterGraphPoints.size)
    }

    /**
     * TEST 2:
     * Multiple meter readings on different days survive cold restart with graph and average intact.
     */
    @Test
    fun test2_multipleDailyReadingsSurviveColdRestart() {
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450",
                    units = "300",
                    isPaid = true
                )
            )
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        MeterReadingCache.saveReading(12460.0, "2026-09-02")
        MeterReadingCache.saveReading(12472.0, "2026-09-04")
        MeterReadingCache.saveReading(12500.0, "2026-09-10")

        // Cold restart
        TnebBillCache.init(context)
        MeterReadingCache.init(context)

        val restoredReadings = MeterReadingCache.readings.value
        assertEquals(3, restoredReadings.size)

        val graphPts = MeterReadingCache.getGraphPoints()
        assertEquals(4, graphPts.size) // baseline + 3 readings

        val metrics = MeterReadingCache.metrics.value
        assertNotNull(metrics)
        assertEquals(50.0, metrics!!.unitsConsumed, 0.01) // 12500 - 12450
        assertTrue(metrics.avgUnitsPerDay > 0)
    }

    /**
     * TEST 3:
     * Connect bill -> Do NOT enter today's reading -> Cold restart
     * -> App displays "Enter today's meter reading" (State B), NOT "Connect TNEB Bill".
     */
    @Test
    fun test3_billConnectedWithoutReadingRestoresPromptState() {
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            consumerName = "Sanjai",
            bills = listOf(
                TnebBillRecord(
                    id = "REC-100",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450",
                    isPaid = true
                )
            )
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        // No meter readings entered
        assertTrue(MeterReadingCache.readings.value.isEmpty())

        // Cold restart
        TnebBillCache.init(context)
        MeterReadingCache.init(context)

        val homeViewModel = HomeViewModel()
        val uiState = homeViewModel.uiState.value

        // Bill is connected
        assertTrue(uiState.hasConnectedBill)
        // Reading is NOT entered
        assertFalse(uiState.hasCurrentReading)
        assertNotNull(uiState.meterBaseline)
        assertEquals(12450.0, uiState.meterBaseline!!.baselineReading, 0.01)
    }

    /**
     * TEST 4:
     * Connect bill -> Enter today's reading -> Cold restart
     * -> App displays Current Energy Dashboard (State C/D), NOT initial setup.
     */
    @Test
    fun test4_billConnectedWithReadingRestoresLiveDashboard() {
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            bills = listOf(
                TnebBillRecord(
                    id = "REC-200",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450",
                    isPaid = true
                )
            )
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)
        MeterReadingCache.saveReading(12582.0, "2026-09-30")

        // Cold restart
        TnebBillCache.init(context)
        MeterReadingCache.init(context)

        val homeViewModel = HomeViewModel()
        val uiState = homeViewModel.uiState.value

        assertTrue(uiState.hasConnectedBill)
        assertTrue(uiState.hasCurrentReading)
        assertNotNull(uiState.currentEnergyMetrics)
        assertEquals(132.0, uiState.currentEnergyMetrics!!.unitsConsumed, 0.01)
    }

    /**
     * TEST 5:
     * Sign out -> Cold restart -> App requires sign-in, previous dashboard data is cleared.
     */
    @Test
    fun test5_signOutClearsDashboardData() {
        val testUser = User(id = "USR-200", username = "tester", mobileNumber = "9999999999")
        UserSession.saveUser(context, testUser)

        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            bills = listOf(
                TnebBillRecord(
                    id = "REC-300",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450",
                    isPaid = true
                )
            )
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)
        MeterReadingCache.saveReading(12582.0, "2026-09-30")

        // Perform Sign Out
        UserSession.clear(context)
        TnebBillCache.clear(context)
        MeterReadingCache.clear(context)

        // Cold restart after sign out
        UserSession.init(context)
        TnebBillCache.init(context)
        MeterReadingCache.init(context)

        // Must require sign-in
        assertNull(UserSession.currentUser)
        assertNull(TnebBillCache.billData.value)
        assertNull(MeterReadingCache.baseline.value)
        assertTrue(MeterReadingCache.readings.value.isEmpty())

        val homeViewModel = HomeViewModel()
        val uiState = homeViewModel.uiState.value
        assertFalse(uiState.hasConnectedBill)
        assertFalse(uiState.hasCurrentReading)
    }
}
