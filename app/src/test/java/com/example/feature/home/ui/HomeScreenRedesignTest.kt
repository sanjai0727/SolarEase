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

    /**
     * Requirement 21 Exact Scenario:
     * Last paid bill: Date: 24 Aug, Meter reading: 42,000 kWh
     * Current reading: 30 Sep, Meter reading: 42,440 kWh
     *
     * Expected:
     * CURRENT graph starts at: 24 Aug / 42,000 kWh and ends at: 30 Sep / 42,440 kWh
     * Current consumption: 440 kWh
     * Know Your Consumption card: "Find where your 440 kWh is going"
     *
     * Then update current reading: 42,460 kWh
     * Expected:
     * Graph final point: 42,460 kWh
     * Current consumption: 460 kWh
     * Know Your Consumption card: "Find where your 460 kWh is going"
     */
    @Test
    fun test5_exactScenario_meterReadingProgressionAndDynamicCardUpdate() {
        val testUser = User(id = "USR-201", name = "Sanjai", username = "sanjai", mobileNumber = "9876543210")
        UserSession.saveUser(context, testUser)

        // 1. Last paid bill: Date: 24 Aug, Meter reading: 42,000 kWh
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567891",
            consumerName = "Sanjai",
            bills = listOf(
                TnebBillRecord(
                    id = "REC-AUG",
                    assessmentDate = "24 Aug 2026",
                    kwh = "42000",
                    units = "400",
                    isPaid = true
                )
            )
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        // 2. Current reading: 30 Sep, Meter reading: 42,440 kWh
        val saveResult = MeterReadingCache.saveReading(42440.0, "2026-09-30")
        assertTrue(saveResult is MeterValidationResult.Success)

        // Verify single source of truth metrics
        val metrics1 = MeterReadingCache.metrics.value
        assertNotNull(metrics1)
        assertEquals(440.0, metrics1!!.unitsConsumed, 0.001)
        assertEquals(42440.0, metrics1.currentReading, 0.001)
        assertEquals(42000.0, metrics1.baselineReading, 0.001)

        // Verify CURRENT progression window
        val currentWindow1 = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.CURRENT)
        assertEquals(2, currentWindow1.points.size)

        // Starting point: 24 Aug / 42,000 kWh
        val startPt1 = currentWindow1.points.first()
        assertEquals("24 Aug", startPt1.dateLabel)
        assertEquals(42000.0, startPt1.readingValue, 0.001)
        assertEquals("Last Bill", startPt1.subLabel)
        assertTrue(startPt1.isBaseline)

        // Ending point: 30 Sep / 42,440 kWh
        val endPt1 = currentWindow1.points.last()
        assertEquals("30 Sep", endPt1.dateLabel)
        assertEquals(42440.0, endPt1.readingValue, 0.001)
        assertEquals("Today", endPt1.subLabel)
        assertEquals(42440.0, currentWindow1.latestReading, 0.001)

        // Verify Know Your Consumption card dynamic heading
        val kwhText1 = if (metrics1.unitsConsumed % 1.0 == 0.0) "${metrics1.unitsConsumed.toInt()}" else String.format(Locale.getDefault(), "%.1f", metrics1.unitsConsumed)
        val cardHeading1 = "Find where your $kwhText1 kWh is going"
        assertEquals("Find where your 440 kWh is going", cardHeading1)

        // 3. User updates current reading: 42,460 kWh
        val updateResult = MeterReadingCache.updateTodayReading(42460.0)
        assertTrue(updateResult is MeterValidationResult.Success)

        // Verify immediate update of single source of truth
        val metrics2 = MeterReadingCache.metrics.value
        assertNotNull(metrics2)
        assertEquals(460.0, metrics2!!.unitsConsumed, 0.001)
        assertEquals(42460.0, metrics2.currentReading, 0.001)

        // Verify updated CURRENT progression window
        val currentWindow2 = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.CURRENT)
        assertEquals(2, currentWindow2.points.size)
        val endPt2 = currentWindow2.points.last()
        assertEquals(42460.0, endPt2.readingValue, 0.001)
        assertEquals(42460.0, currentWindow2.latestReading, 0.001)

        // Verify Know Your Consumption card immediately updates to 460 kWh
        val kwhText2 = if (metrics2.unitsConsumed % 1.0 == 0.0) "${metrics2.unitsConsumed.toInt()}" else String.format(Locale.getDefault(), "%.1f", metrics2.unitsConsumed)
        val cardHeading2 = "Find where your $kwhText2 kWh is going"
        assertEquals("Find where your 460 kWh is going", cardHeading2)
    }

    /**
     * Requirement 6, 7, 8, 9 & 18:
     * 2M, 4M, 6M selectors & Left/Right historical window navigation.
     * Rules:
     * - Disable LEFT when no older data.
     * - Disable RIGHT when no newer data.
     * - Insufficient data: clean state without fabricating data.
     */
    @Test
    fun test6_historicalRangesAndNavigation() {
        val testUser = User(id = "USR-202", name = "Sanjai", username = "sanjai", mobileNumber = "9876543210")
        UserSession.saveUser(context, testUser)

        val historicalBills = listOf(
            TnebBillRecord(id = "B0", assessmentDate = "24 Aug 2026", kwh = "42000", units = "800", isPaid = true),
            TnebBillRecord(id = "B1", assessmentDate = "24 Jun 2026", kwh = "41200", units = "800", isPaid = true),
            TnebBillRecord(id = "B2", assessmentDate = "24 Apr 2026", kwh = "40400", units = "800", isPaid = true),
            TnebBillRecord(id = "B3", assessmentDate = "24 Feb 2026", kwh = "39600", units = "800", isPaid = true)
        )

        val billData = TnebConsumerBillData(
            consumerNumber = "09234567892",
            bills = historicalBills
        )
        TnebBillCache.save(context, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        // ── Test 2M Window 0: Jun 2026 – Aug 2026 (Jun, Jul, Aug) ──
        val m2Window0 = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M2, windowOffset = 0, providedBillData = billData)
        assertEquals(3, m2Window0.points.size) // Jun, Jul, Aug
        assertEquals(41200.0, m2Window0.points.first().readingValue, 0.001)
        assertTrue(m2Window0.points.first().hasReading)
        org.junit.Assert.assertFalse(m2Window0.points[1].hasReading) // Jul has no bill, position preserved
        assertEquals(42000.0, m2Window0.points.last().readingValue, 0.001)
        assertTrue(m2Window0.points.last().hasReading)
        assertEquals(42000.0, m2Window0.latestReading, 0.001)
        assertTrue(m2Window0.hasOlder) // Can go older (Apr - Jun)
        org.junit.Assert.assertFalse(m2Window0.hasNewer) // Newer is disabled

        // ── Test 2M Window 1 (Tap LEFT): Apr 2026 – Jun 2026 (Apr, May, Jun) ──
        val m2Window1 = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M2, windowOffset = 1, providedBillData = billData)
        assertEquals(3, m2Window1.points.size)
        assertEquals(40400.0, m2Window1.points.first().readingValue, 0.001)
        assertEquals(41200.0, m2Window1.points.last().readingValue, 0.001)
        assertTrue(m2Window1.hasOlder) // Can go older (Feb - Apr)
        assertTrue(m2Window1.hasNewer) // Can go newer (Jun - Aug)

        // ── Test 2M Window 2 (Tap LEFT again): Feb 2026 – Apr 2026 (Feb, Mar, Apr) ──
        val m2Window2 = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M2, windowOffset = 2, providedBillData = billData)
        assertEquals(3, m2Window2.points.size)
        assertEquals(39600.0, m2Window2.points.first().readingValue, 0.001)
        assertEquals(40400.0, m2Window2.points.last().readingValue, 0.001)
        org.junit.Assert.assertFalse(m2Window2.hasOlder) // Reached oldest available -> Left disabled
        assertTrue(m2Window2.hasNewer) // Can go newer

        // ── Test 4M Window 0: Apr 2026 – Aug 2026 (Apr, May, Jun, Jul, Aug) ──
        val m4Window0 = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M4, windowOffset = 0, providedBillData = billData)
        assertEquals(5, m4Window0.points.size)
        assertEquals("Apr", m4Window0.points[0].dateLabel)
        assertEquals(40400.0, m4Window0.points[0].readingValue, 0.001)
        assertTrue(m4Window0.points[0].hasReading)

        assertEquals("May", m4Window0.points[1].dateLabel)
        org.junit.Assert.assertFalse(m4Window0.points[1].hasReading)

        assertEquals("Jun", m4Window0.points[2].dateLabel)
        assertEquals(41200.0, m4Window0.points[2].readingValue, 0.001)
        assertTrue(m4Window0.points[2].hasReading)

        assertEquals("Jul", m4Window0.points[3].dateLabel)
        org.junit.Assert.assertFalse(m4Window0.points[3].hasReading)

        assertEquals("Aug", m4Window0.points[4].dateLabel)
        assertEquals(42000.0, m4Window0.points[4].readingValue, 0.001)
        assertTrue(m4Window0.points[4].hasReading)
        assertEquals(42000.0, m4Window0.latestReading, 0.001)

        // ── Test 6M Window 0: Feb 2026 – Aug 2026 (Feb, Mar, Apr, May, Jun, Jul, Aug) ──
        val m6Window0 = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M6, windowOffset = 0, providedBillData = billData)
        assertEquals(7, m6Window0.points.size)
        assertEquals("Feb", m6Window0.points[0].dateLabel)
        assertEquals(39600.0, m6Window0.points[0].readingValue, 0.001)
        assertTrue(m6Window0.points[0].hasReading)
        assertEquals("Aug", m6Window0.points[6].dateLabel)
        assertEquals(42000.0, m6Window0.points[6].readingValue, 0.001)
        assertTrue(m6Window0.points[6].hasReading)
        assertEquals(42000.0, m6Window0.latestReading, 0.001)

        // ── Test Insufficient Data: When user has only 1 bill ──
        val singleBillData = TnebConsumerBillData(
            consumerNumber = "09234567893",
            bills = listOf(historicalBills[0]) // Only Aug bill
        )
        val m2Empty = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M2, windowOffset = 0, providedBillData = singleBillData)
        assertTrue(m2Empty.isInsufficientData)
        assertTrue(m2Empty.points.isEmpty())

        val m4Empty = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M4, windowOffset = 0, providedBillData = singleBillData)
        assertTrue(m4Empty.isInsufficientData)

        val m6Empty = MeterReadingCache.getProgressionWindow(com.example.feature.home.ui.modules.MeterGraphRange.M6, windowOffset = 0, providedBillData = singleBillData)
        assertTrue(m6Empty.isInsufficientData)
    }

    /**
     * Requirement: Every month inside historical range must be represented.
     * Example: 4M Window December 2025 -> April 2026.
     * All 5 months (Dec, Jan, Feb, Mar, Apr) must be represented on the graph.
     * Actual data for months with readings (e.g. Feb 26 -> 41,361 kWh).
     * No fabricated values for months without readings.
     */
    @Test
    fun test7_exactUserScenario_4M_DecToApr_EveryMonthRepresented() {
        val testUser = User(id = "USR-203", name = "Sanjai", username = "sanjai", mobileNumber = "9876543210")
        UserSession.saveUser(context, testUser)

        val bills = listOf(
            TnebBillRecord(id = "B_APR", assessmentDate = "26 Apr 2026", kwh = "42000", units = "639", isPaid = true),
            TnebBillRecord(id = "B_FEB", assessmentDate = "26 Feb 2026", kwh = "41361", units = "700", isPaid = true),
            TnebBillRecord(id = "B_DEC", assessmentDate = "26 Dec 2025", kwh = "40661", units = "600", isPaid = true)
        )

        val billData = TnebConsumerBillData(
            consumerNumber = "09234567894",
            bills = bills
        )
        TnebBillCache.save(context, billData)

        val m4 = MeterReadingCache.getProgressionWindow(
            com.example.feature.home.ui.modules.MeterGraphRange.M4,
            windowOffset = 0,
            providedBillData = billData
        )

        // 5 months: Dec, Jan, Feb, Mar, Apr
        assertEquals(5, m4.points.size)
        assertEquals(listOf("Dec", "Jan", "Feb", "Mar", "Apr"), m4.points.map { it.dateLabel })

        // Dec
        val decPoint = m4.points[0]
        assertEquals("Dec", decPoint.dateLabel)
        assertTrue(decPoint.hasReading)
        assertEquals(40661.0, decPoint.readingValue, 0.001)

        // Jan (no bill -> position preserved, hasReading = false)
        val janPoint = m4.points[1]
        assertEquals("Jan", janPoint.dateLabel)
        org.junit.Assert.assertFalse(janPoint.hasReading)

        // Feb (actual reading 41,361 kWh)
        val febPoint = m4.points[2]
        assertEquals("Feb", febPoint.dateLabel)
        assertTrue(febPoint.hasReading)
        assertEquals(41361.0, febPoint.readingValue, 0.001)
        assertEquals("26 Feb 2026", febPoint.fullDate)

        // Mar (no bill -> position preserved, hasReading = false)
        val marPoint = m4.points[3]
        assertEquals("Mar", marPoint.dateLabel)
        org.junit.Assert.assertFalse(marPoint.hasReading)

        // Apr
        val aprPoint = m4.points[4]
        assertEquals("Apr", aprPoint.dateLabel)
        assertTrue(aprPoint.hasReading)
        assertEquals(42000.0, aprPoint.readingValue, 0.001)
    }
}
