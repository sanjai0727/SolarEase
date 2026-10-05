package com.example.feature.home.ui.modules

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
class MeterReadingFlowTest {

    @Before
    fun setUp() {
        MeterReadingCache.clear(null)
        TnebBillCache.clear(null)
    }

    @After
    fun tearDown() {
        MeterReadingCache.clear(null)
        TnebBillCache.clear(null)
    }

    @Test
    fun test1_newUserNoBillConnected() {
        // STATE A: Initial zero-state
        assertNull(MeterReadingCache.baseline.value)
        assertNull(MeterReadingCache.metrics.value)
        assertTrue(MeterReadingCache.readings.value.isEmpty())
        assertTrue(MeterReadingCache.getGraphPoints().isEmpty())
    }

    @Test
    fun test2_and_test3_billConnectionSucceedsAndBaselineRetrieved() {
        // STATE B: Bill connects, previous baseline retrieved
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            consumerName = "Sanjai",
            tariffCode = "TANGEDCO LT-1A · DOMESTIC",
            isDuesNil = true,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-98765",
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450", // Previous bill closing reading
                    units = "302",  // Previous billed units (historical only!)
                    amountPaid = "1150",
                    amountDue = "0",
                    isPaid = true
                )
            )
        )

        TnebBillCache.save(null, billData)
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        val baseline = MeterReadingCache.baseline.value
        assertNotNull(baseline)
        assertEquals(12450.0, baseline!!.baselineReading, 0.001)
        assertEquals("01 Sep 2026", baseline.baselineDate)
        assertEquals(302.0, baseline.previousBilledUnits ?: 0.0, 0.001)

        // Critical verification: Previous month's 302 kWh must NOT be the current consumption!
        val metrics = MeterReadingCache.metrics.value
        assertNotNull(metrics)
        assertEquals(0.0, metrics!!.unitsConsumed, 0.001)
        assertFalse(metrics.isTodayLogged)
        assertNull(metrics.todayReading)
    }

    @Test
    fun test4_and_test5_and_test6_enterValidReadingAndConsumptionCalculated() {
        // Connect Bill
        val billData = TnebConsumerBillData(
            consumerNumber = "09234567890",
            bills = listOf(
                TnebBillRecord(
                    assessmentDate = "01 Sep 2026",
                    kwh = "12450",
                    units = "302"
                )
            )
        )
        MeterReadingCache.syncBaselineFromTnebBill(billData)

        // Save today's reading: 12,582 kWh
        val todayIso = MeterReadingCache.getTodayIsoDate()
        val result = MeterReadingCache.saveReading(12582.0, todayIso)
        assertTrue(result is MeterValidationResult.Success)

        // Current consumption = 12,582 - 12,450 = 132 kWh
        val metrics = MeterReadingCache.metrics.value
        assertNotNull(metrics)
        assertEquals(132.0, metrics!!.unitsConsumed, 0.001)
        assertEquals(12582.0, metrics.currentReading, 0.001)
        assertTrue(metrics.isTodayLogged)
        assertEquals(12582.0, metrics.todayReading ?: 0.0, 0.001)
    }

    @Test
    fun test7_estimatedBillCalculatedCorrectly() {
        MeterReadingCache.setManualBaseline(12450.0, "01 Sep 2026")
        MeterReadingCache.saveReading(12582.0) // 132 units

        val metrics = MeterReadingCache.metrics.value
        assertNotNull(metrics)
        // 132 units <= 500 units: 200 free units -> 0 chargeable energy charge
        assertEquals(0.0, metrics!!.estimatedBill, 0.001)

        // If consumption reaches 350 units (reading = 12,800 kWh)
        MeterReadingCache.clear(null)
        MeterReadingCache.setManualBaseline(12450.0, "01 Sep 2026")
        MeterReadingCache.saveReading(12800.0) // 350 units

        val metrics350 = MeterReadingCache.metrics.value
        assertNotNull(metrics350)
        assertEquals(350.0, metrics350!!.unitsConsumed, 0.001)
        // 350 units: 200 free, 150 chargeable @ 4.70 = 705.0
        val expectedBill = TNEBBillCalculator.calculateTNEBBill(350.0).totalEnergyCharge
        assertEquals(705.0, expectedBill, 0.001)
        assertEquals(expectedBill, metrics350.estimatedBill, 0.001)
    }

    @Test
    fun test8_graphContainsBaselineAndReadings() {
        MeterReadingCache.setManualBaseline(12450.0, "2026-09-01")
        MeterReadingCache.saveReading(12582.0, "2026-09-30")

        val points = MeterReadingCache.getGraphPoints()
        assertEquals(2, points.size)

        // Point 0: Baseline anchor
        assertTrue(points[0].isBaseline)
        assertEquals(12450.0, points[0].readingValue, 0.001)
        assertEquals(0.0, points[0].unitsConsumed, 0.001)

        // Point 1: Recorded reading
        assertFalse(points[1].isBaseline)
        assertEquals(12582.0, points[1].readingValue, 0.001)
        assertEquals(132.0, points[1].unitsConsumed, 0.001)
    }

    @Test
    fun test9_and_test10_nextDayReadingAndAverageCalculation() {
        MeterReadingCache.setManualBaseline(12450.0, "2026-09-01")

        // Day 1
        MeterReadingCache.saveReading(12460.0, "2026-09-02")
        // Day 2
        MeterReadingCache.saveReading(12470.0, "2026-09-03")

        val readings = MeterReadingCache.readings.value
        assertEquals(2, readings.size)

        val metrics = MeterReadingCache.metrics.value
        assertNotNull(metrics)
        assertEquals(20.0, metrics!!.unitsConsumed, 0.001)
        assertTrue(metrics.daysElapsed >= 2)
        assertTrue(metrics.avgUnitsPerDay > 0.0)

        // Consecutive daily consumption check: 12470 - 12460 = 10 kWh
        val lastRecord = readings.last()
        assertEquals(10.0, lastRecord.dailyConsumption ?: 0.0, 0.001)
    }

    @Test
    fun test11_and_test12_billHistoryAndCheckDuesRemainFunctional() {
        val historicalBill = TnebBillRecord(
            id = "REC-AUG2026",
            assessmentDate = "01 Jul 2026",
            kwh = "12148",
            units = "302",
            amountDue = "1250",
            dueDate = "20 Jul 2026",
            amountPaid = "1250",
            receiptNumber = "RCP-00123",
            paymentDate = "15 Jul 2026",
            isPaid = true
        )
        val consumerData = TnebConsumerBillData(
            consumerNumber = "09123456789",
            isDuesNil = true,
            bills = listOf(historicalBill)
        )

        TnebBillCache.save(null, consumerData)

        // 1. Bill History verification:
        val cached = TnebBillCache.billData.value
        assertNotNull(cached)
        assertEquals(1, cached!!.bills.size)
        val bill = cached.bills.first()
        assertEquals("01 Jul 2026", bill.assessmentDate)
        assertEquals("302", bill.units)
        assertEquals("12148", bill.kwh)
        assertEquals("1250", bill.amountPaid)
        assertTrue(bill.isPaid)

        // 2. Check Dues verification:
        assertEquals("₹0.00", cached.totalDuesFormatted)
        assertEquals("PAID", cached.displayStatus)
    }

    @Test
    fun test13_invalidLowerReadingIsRejected() {
        MeterReadingCache.setManualBaseline(12450.0, "01 Sep 2026")

        // 1. Negative reading rejected
        val negRes = MeterReadingCache.saveReading(-100.0)
        assertTrue(negRes is MeterValidationResult.Error)

        // 2. Reading lower than baseline (12,400 < 12,450) rejected
        val lowerBaseRes = MeterReadingCache.saveReading(12400.0)
        assertTrue(lowerBaseRes is MeterValidationResult.Error)
        assertTrue((lowerBaseRes as MeterValidationResult.Error).message.contains("cannot be lower than your last bill reading"))

        // 3. Reading lower than previous day rejected
        MeterReadingCache.saveReading(12480.0, "2026-09-05")
        val lowerPrevRes = MeterReadingCache.saveReading(12470.0, "2026-09-06")
        assertTrue(lowerPrevRes is MeterValidationResult.Error)
        assertTrue((lowerPrevRes as MeterValidationResult.Error).message.contains("cannot be lower than the previous recorded reading"))
    }

    @Test
    fun test14_duplicateSameDayReadingAndEditingHandledCorrectly() {
        MeterReadingCache.setManualBaseline(12450.0, "2026-09-01")
        val today = MeterReadingCache.getTodayIsoDate()

        // First submission for today succeeds
        val res1 = MeterReadingCache.saveReading(12582.0, today)
        assertTrue(res1 is MeterValidationResult.Success)

        // Second submission for same day returns DuplicateToday
        val res2 = MeterReadingCache.saveReading(12585.0, today)
        assertTrue(res2 is MeterValidationResult.DuplicateToday)
        assertEquals(12582.0, (res2 as MeterValidationResult.DuplicateToday).existingReading.readingValue, 0.001)

        // User explicitly edits today's reading
        val editRes = MeterReadingCache.updateTodayReading(12585.0)
        assertTrue(editRes is MeterValidationResult.Success)
        assertEquals(12585.0, MeterReadingCache.metrics.value!!.currentReading, 0.001)
        assertEquals(135.0, MeterReadingCache.metrics.value!!.unitsConsumed, 0.001)
    }

    @Test
    fun test15_newBillingCycleResetsBaselineCorrectly() {
        // Cycle 1
        val bill1 = TnebConsumerBillData(
            consumerNumber = "09234567890",
            bills = listOf(
                TnebBillRecord(id = "BILL-CYCLE-1", assessmentDate = "01 Sep 2026", kwh = "12450", units = "302")
            )
        )
        MeterReadingCache.syncBaselineFromTnebBill(bill1)
        MeterReadingCache.saveReading(12582.0, "2026-09-15")
        assertEquals(132.0, MeterReadingCache.metrics.value!!.unitsConsumed, 0.001)

        // Cycle 2: A new utility bill is issued with closing reading 12,650
        val bill2 = TnebConsumerBillData(
            consumerNumber = "09234567890",
            bills = listOf(
                TnebBillRecord(id = "BILL-CYCLE-2", assessmentDate = "01 Nov 2026", kwh = "12650", units = "200")
            )
        )
        MeterReadingCache.syncBaselineFromTnebBill(bill2)

        // New baseline set to 12,650
        assertEquals(12650.0, MeterReadingCache.baseline.value!!.baselineReading, 0.001)
        // Current consumption reset for new cycle
        assertEquals(0.0, MeterReadingCache.metrics.value!!.unitsConsumed, 0.001)
        assertTrue(MeterReadingCache.readings.value.isEmpty())

        // First reading in new cycle: 12,710 -> 60 kWh
        MeterReadingCache.saveReading(12710.0, "2026-11-05")
        assertEquals(60.0, MeterReadingCache.metrics.value!!.unitsConsumed, 0.001)
    }
}
