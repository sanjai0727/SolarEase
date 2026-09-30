package com.example.feature.home.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.session.User
import com.example.core.session.UserSession
import com.example.feature.home.ui.modules.MeterReadingCache
import com.example.feature.home.ui.modules.MeterValidationResult
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HomeScreenRedesignTest {

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
     * Requirement 1 & 9:
     * Baseline = 12,280 kWh
     * Today's reading = 12,582 kWh -> Current consumption = 302 kWh.
     * Card message: "Find where your 302 kWh is going".
     *
     * Then update reading to 12,587 kWh -> Current consumption = 307 kWh.
     * Card message updates immediately to: "Find where your 307 kWh is going".
     */
    @Test
    fun test1_knowYourConsumptionSolarLensCardReactsDynamicallyToCurrentReading() {
        val testUser = User(id = "USR-200", name = "Sanjai", username = "sanjai", mobileNumber = "9876543210")
        UserSession.saveUser(context, testUser)

        // 1. Establish baseline from previous bill: 12,280 kWh
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            consumerName = "Sanjai",
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12280", // Previous bill meter reading
                    units = "250"
                )
            )
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        // 2. Save today's reading: 12,582 kWh
        val todayIso = MeterReadingCache.getTodayIsoDate()
        val saveResult = MeterReadingCache.saveReading(12582.0, todayIso)
        assertTrue(saveResult is MeterValidationResult.Success)

        // Verify single source of truth current metrics
        val metrics1 = MeterReadingCache.metrics.value
        assertNotNull(metrics1)
        assertEquals(302.0, metrics1!!.unitsConsumed, 0.001)

        // Verify dynamic promotion card text calculation
        val kwhText1 = if (metrics1.unitsConsumed % 1.0 == 0.0) {
            "${metrics1.unitsConsumed.toInt()}"
        } else {
            String.format(Locale.getDefault(), "%.1f", metrics1.unitsConsumed)
        }
        val cardMessage1 = "Find where your $kwhText1 kWh is going"
        assertEquals("Find where your 302 kWh is going", cardMessage1)

        // 3. User updates today's reading: 12,587 kWh
        val updateResult = MeterReadingCache.updateTodayReading(12587.0)
        assertTrue(updateResult is MeterValidationResult.Success)

        // Verify reactive recalculation
        val metrics2 = MeterReadingCache.metrics.value
        assertNotNull(metrics2)
        assertEquals(307.0, metrics2!!.unitsConsumed, 0.001)

        val kwhText2 = if (metrics2.unitsConsumed % 1.0 == 0.0) {
            "${metrics2.unitsConsumed.toInt()}"
        } else {
            String.format(Locale.getDefault(), "%.1f", metrics2.unitsConsumed)
        }
        val cardMessage2 = "Find where your $kwhText2 kWh is going"
        assertEquals("Find where your 307 kWh is going", cardMessage2)

        // Estimated bill also automatically recalculated from 302 to 307 kWh
        assertTrue(metrics2.estimatedBill > 0.0)
    }

    /**
     * Requirement 3 & 4:
     * Daily consumption graph calculates consecutive meter reading differences:
     * Sep 25 = 12,450
     * Sep 26 = 12,455 -> 5 kWh
     * Sep 27 = 12,461 -> 6 kWh
     * Sep 28 = 12,469 -> 8 kWh
     */
    @Test
    fun test2_dailyConsumptionCalculatedFromConsecutiveReadings() {
        // Set baseline at Sep 25 = 12,450 kWh
        MeterReadingCache.setManualBaseline(12450.0, "2026-09-25", "CYCLE-1")

        // Enter consecutive readings
        MeterReadingCache.saveReading(12455.0, "2026-09-26") // Day 1: 5 kWh
        MeterReadingCache.saveReading(12461.0, "2026-09-27") // Day 2: 6 kWh
        MeterReadingCache.saveReading(12469.0, "2026-09-28") // Day 3: 8 kWh

        val dailyPoints = MeterReadingCache.getDailyConsumptionPoints()
        assertEquals(3, dailyPoints.size)

        // Sep 26 = 5 kWh
        assertEquals("2026-09-26", dailyPoints[0].dateIso)
        assertEquals(5.0, dailyPoints[0].dailyKwh, 0.001)
        assertEquals(12455.0, dailyPoints[0].cumulativeReading, 0.001)

        // Sep 27 = 6 kWh
        assertEquals("2026-09-27", dailyPoints[1].dateIso)
        assertEquals(6.0, dailyPoints[1].dailyKwh, 0.001)
        assertEquals(12461.0, dailyPoints[1].cumulativeReading, 0.001)

        // Sep 28 = 8 kWh
        assertEquals("2026-09-28", dailyPoints[2].dateIso)
        assertEquals(8.0, dailyPoints[2].dailyKwh, 0.001)
        assertEquals(12469.0, dailyPoints[2].cumulativeReading, 0.001)
    }

    /**
     * Requirement 3:
     * Do not fabricate data when insufficient readings exist (< 2 readings).
     */
    @Test
    fun test3_insufficientDataCleanStateZeroFabricatedData() {
        // Baseline only: 0 consecutive pairs -> 0 daily points
        MeterReadingCache.setManualBaseline(12450.0, "2026-09-25", "CYCLE-1")
        val pointsEmpty = MeterReadingCache.getDailyConsumptionPoints()
        assertTrue(pointsEmpty.isEmpty())

        // 1 reading only: 1 daily point (needs >= 2 points for a line trend)
        MeterReadingCache.saveReading(12455.0, "2026-09-26")
        val pointsSingle = MeterReadingCache.getDailyConsumptionPoints()
        assertEquals(1, pointsSingle.size)
    }

    /**
     * Requirement 2 & 8:
     * Single source of truth: ViewModel observes state reactively and synchronizes all metrics.
     */
    @Test
    fun test4_singleSourceOfTruthReactiveFlow() {
        MeterReadingCache.setManualBaseline(12280.0, "2026-09-01", "BILL-1")
        MeterReadingCache.saveReading(12582.0, "2026-09-30")

        val viewModel1 = HomeViewModel()
        val state1 = viewModel1.uiState.value

        assertEquals(302.0, state1.currentBillingCycleKwh, 0.001)
        assertEquals(302.0, state1.unitsConsumed ?: 0.0, 0.001)
        assertEquals(302.0, state1.currentEnergyMetrics?.unitsConsumed ?: 0.0, 0.001)
        assertEquals(1, state1.dailyConsumptionPoints.size)

        // Now save another consecutive day: 12,592 kWh
        MeterReadingCache.saveReading(12592.0, "2026-10-01")

        // Single source of truth: Cache metrics update immediately
        val metrics = MeterReadingCache.metrics.value
        assertNotNull(metrics)
        assertEquals(312.0, metrics!!.unitsConsumed, 0.001)

        val dailyPts = MeterReadingCache.getDailyConsumptionPoints()
        assertEquals(2, dailyPts.size)
        assertEquals(10.0, dailyPts.last().dailyKwh, 0.001)

        // Verified on Home Screen state
        val viewModel2 = HomeViewModel()
        val state2 = viewModel2.uiState.value
        assertEquals(312.0, state2.currentBillingCycleKwh, 0.001)
        assertEquals(312.0, state2.unitsConsumed ?: 0.0, 0.001)
        assertEquals(2, state2.dailyConsumptionPoints.size)
        assertEquals(10.0, state2.dailyConsumptionPoints.last().dailyKwh, 0.001)
    }
}
