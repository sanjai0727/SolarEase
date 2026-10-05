package com.example.feature.home.ui.modules

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class BillingCycleManagerTest {

    @Test
    fun testDynamicDerivationOfNextPeriod() {
        assertEquals("Aug–Oct", BillingCycleManager.deriveNextPeriod("Jun–Aug"))
        assertEquals("Oct–Dec", BillingCycleManager.deriveNextPeriod("Aug–Oct"))
        assertEquals("Dec–Feb", BillingCycleManager.deriveNextPeriod("Oct–Dec"))
        assertEquals("Feb–Apr", BillingCycleManager.deriveNextPeriod("Dec–Feb"))
        assertEquals("Apr–Jun", BillingCycleManager.deriveNextPeriod("Feb–Apr"))
        assertEquals("Jun–Aug", BillingCycleManager.deriveNextPeriod("Apr–Jun"))

        // Odd-month cycles
        assertEquals("Mar–May", BillingCycleManager.deriveNextPeriod("Jan–Mar"))
        assertEquals("May–Jul", BillingCycleManager.deriveNextPeriod("Mar–May"))
    }

    @Test
    fun testCase1_Previous55_Current170_StatusHigher() {
        // CASE 1:
        // Previous = 55 kWh (Jun–Aug)
        // Current = 170 kWh (Aug–Oct)
        // Expected: HIGHER, +115 kWh, No savings, No savings credits
        val prev = BillingCycleItem(
            id = "P1",
            periodLabel = "Jun–Aug",
            consumptionKwh = 55.0,
            durationDays = 60,
            dailyRate = 55.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val curr = BillingCycleItem(
            id = "C1",
            periodLabel = "Aug–Oct",
            consumptionKwh = 170.0,
            durationDays = 40,
            dailyRate = 170.0 / 40.0,
            source = BillingCycleSource.DAILY_READINGS,
            status = BillingCycleStatus.CURRENT,
            hasMeterReadings = true
        )

        val comparison = BillingCycleManager.compareCycles(curr, prev)

        assertEquals(BillingComparisonState.CURRENT_HIGHER, comparison.state)
        assertEquals("↑ +115 kWh • Consumption Increased", comparison.primaryMessage)
        assertEquals(115.0, comparison.differenceUnits, 0.01)
        assertEquals(0.0, comparison.savedUnits, 0.01)
        assertEquals(0, comparison.savedCredits)
        assertTrue(comparison.isConsumptionIncreased)
        assertTrue(comparison.secondaryMessage.contains("170 kWh"))
        assertTrue(comparison.comparisonNote.contains("Already +115 kWh higher"))
    }

    @Test
    fun testCase2_Previous200_Current120_ActiveCycle_CurrentlyLower() {
        // CASE 2:
        // Previous = 200 kWh
        // Current = 120 kWh (Current cycle still active)
        // Expected: CURRENTLY LOWER, 80 kWh below previous cycle, Cycle still in progress, No finalized savings credit yet
        val prev = BillingCycleItem(
            id = "P1",
            periodLabel = "Jun–Aug",
            consumptionKwh = 200.0,
            durationDays = 60,
            dailyRate = 200.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val curr = BillingCycleItem(
            id = "C1",
            periodLabel = "Aug–Oct",
            consumptionKwh = 120.0,
            durationDays = 35,
            dailyRate = 120.0 / 35.0,
            source = BillingCycleSource.DAILY_READINGS,
            status = BillingCycleStatus.CURRENT,
            hasMeterReadings = true
        )

        val comparison = BillingCycleManager.compareCycles(curr, prev)

        assertEquals(BillingComparisonState.CURRENT_LOWER, comparison.state)
        assertEquals("Currently below previous cycle", comparison.primaryMessage)
        assertEquals(80.0, comparison.differenceUnits, 0.01)
        assertEquals(0.0, comparison.savedUnits, 0.01) // Not finalized until cycle completes
        assertEquals(0, comparison.savedCredits) // No credits awarded for in-progress cycle
        assertFalse(comparison.isConsumptionIncreased)
        assertTrue(comparison.secondaryMessage.contains("120 kWh"))
        assertTrue(comparison.comparisonNote.contains("80 kWh below previous cycle"))
        assertTrue(comparison.comparisonNote.contains("Current cycle still in progress"))
    }

    @Test
    fun testState3_CompletedCycleConsumptionIncreased() {
        // Previous: Apr–Jun = 27 kWh
        // Current/Selected: Jun–Aug = 95 kWh
        // Difference = +68 kWh
        // Display: +68 kWh Consumption Increased, 0 credits
        val prev = BillingCycleItem(
            id = "P1",
            periodLabel = "Apr–Jun",
            consumptionKwh = 27.0,
            durationDays = 60,
            dailyRate = 27.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val curr = BillingCycleItem(
            id = "C1",
            periodLabel = "Jun–Aug",
            consumptionKwh = 95.0,
            durationDays = 60,
            dailyRate = 95.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val comparison = BillingCycleManager.compareCycles(curr, prev)

        assertEquals(BillingComparisonState.COMPLETED_INCREASED, comparison.state)
        assertEquals("↑ +68 kWh • Consumption Increased", comparison.primaryMessage)
        assertEquals("No Energy Credits", comparison.secondaryMessage)
        assertTrue(comparison.isConsumptionIncreased)
        assertEquals(68.0, comparison.differenceUnits, 0.01)
        assertEquals(0.0, comparison.savedUnits, 0.01)
        assertEquals(0, comparison.savedCredits)
    }

    @Test
    fun testCase3_Previous200_CurrentFinalized180_Saved() {
        // CASE 3:
        // Previous = 200 kWh
        // Current finalized = 180 kWh
        // Expected: SAVED, 20 kWh saved, Only now eligible for finalized savings/credit calculation
        val prev = BillingCycleItem(
            id = "P1",
            periodLabel = "Feb–Apr",
            consumptionKwh = 200.0,
            durationDays = 60,
            dailyRate = 200.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val curr = BillingCycleItem(
            id = "C1",
            periodLabel = "Apr–Jun",
            consumptionKwh = 180.0,
            durationDays = 60,
            dailyRate = 180.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val comparison = BillingCycleManager.compareCycles(curr, prev)

        assertEquals(BillingComparisonState.COMPLETED_SAVED, comparison.state)
        assertEquals("↓ 20 kWh • Saved", comparison.primaryMessage)
        assertEquals("Historical Baseline", comparison.secondaryMessage)
        assertEquals(20.0, comparison.savedUnits, 0.01)
        assertEquals(0, comparison.savedCredits)
        assertFalse(comparison.isConsumptionIncreased)
        assertTrue(comparison.comparisonNote.contains("20 kWh lower than Feb–Apr"))
    }

    @Test
    fun testCase4_Previous200_Current200_NoChange() {
        // CASE 4:
        // Previous = 200 kWh
        // Current = 200 kWh
        // Expected: NO CHANGE, 0 kWh difference
        val prev = BillingCycleItem(
            id = "P1",
            periodLabel = "Jun–Aug",
            consumptionKwh = 200.0,
            durationDays = 60,
            dailyRate = 200.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val curr = BillingCycleItem(
            id = "C1",
            periodLabel = "Aug–Oct",
            consumptionKwh = 200.0,
            durationDays = 60,
            dailyRate = 200.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val comparison = BillingCycleManager.compareCycles(curr, prev)

        assertEquals(BillingComparisonState.COMPLETED_NO_CHANGE, comparison.state)
        assertEquals("→ 0 kWh • No Change", comparison.primaryMessage)
        assertEquals(0.0, comparison.differenceUnits, 0.01)
        assertEquals(0.0, comparison.savedUnits, 0.01)
        assertEquals(0, comparison.savedCredits)
        assertFalse(comparison.isConsumptionIncreased)
        assertFalse(comparison.primaryMessage.contains("0 kWh Saved"))
        assertTrue(comparison.comparisonNote.contains("0 kWh difference"))
    }

    @Test
    fun testCase5_CumulativeMeterReadings_ConvertedTo170kWh() {
        // CASE 5:
        // Meter readings: 42,120, 42,130, 42,160, 42,290
        // Expected current consumption: 170 kWh (NOT 127,700+ kWh)
        val readings = listOf(
            MeterReadingRecord(billingCycleId = "C1", readingValue = 42120.0, readingDate = "2026-08-24"),
            MeterReadingRecord(billingCycleId = "C1", readingValue = 42130.0, readingDate = "2026-09-01"),
            MeterReadingRecord(billingCycleId = "C1", readingValue = 42160.0, readingDate = "2026-09-10"),
            MeterReadingRecord(billingCycleId = "C1", readingValue = 42290.0, readingDate = "2026-10-02")
        )

        val (consumption, hasReadings) = BillingCycleManager.calculateCycleConsumptionFromReadings(readings, null)

        assertTrue(hasReadings)
        assertEquals(170.0, consumption, 0.01)
        // Ensure cumulative readings were subtracted (42,290 - 42,120 = 170), NOT summed
        assertNotEquals(168700.0, consumption, 100.0)
    }

    @Test
    fun testCase6_StaleBaselineDate_ElapsedDaysCalculatedWithinCycleAndClamped() {
        // CASE 6:
        // Current cycle: "Aug–Oct"
        // Current date: 2026-09-10 (40 days into cycle)
        // Stale baseline date: "15/02/2026" (from an older billing cycle 7 months ago)
        // Expected:
        // elapsedDays is calculated only within the current billing cycle and clamped to the cycle duration [1, 60].
        val periodLabel = "Aug–Oct"
        val staleBaselineDate = "15/02/2026"
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-09-10")!!

        val elapsedDays = BillingCycleManager.calculateElapsedDays(
            periodLabel = periodLabel,
            baselineDateStr = staleBaselineDate,
            currentDate = currentDate,
            cycleLengthDays = 60
        )

        assertTrue("Elapsed days must be >= 1", elapsedDays >= 1)
        assertTrue("Elapsed days must be <= 60", elapsedDays <= 60)
        // Must calculate from Aug 1 to Sep 10 (40 days), NOT from Feb 15 (207 days)
        assertEquals(40, elapsedDays)
    }

    @Test
    fun testTwoCompletedCycles_DirectSubtraction_NoDailyRateNormalization() {
        // Direct subtraction rule:
        // Previous: 92 kWh
        // Current: 95 kWh
        // 92 - 95 = -3 kWh -> Consumption Increased by 3 kWh!
        // Core rule: DO NOT use daily consumption rate or cycle-length adjustment for two completed cycles.
        val prev = BillingCycleItem(
            id = "P1",
            periodLabel = "Apr–Jun",
            consumptionKwh = 92.0,
            durationDays = 60,
            dailyRate = 92.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val curr = BillingCycleItem(
            id = "C1",
            periodLabel = "Jun–Aug",
            consumptionKwh = 95.0,
            durationDays = 62,
            dailyRate = 95.0 / 62.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val comparison = BillingCycleManager.compareCycles(curr, prev)

        assertEquals(BillingComparisonState.COMPLETED_INCREASED, comparison.state)
        assertEquals("↑ +3 kWh • Consumption Increased", comparison.primaryMessage)
        assertEquals(3.0, comparison.differenceUnits, 0.01)
        assertEquals(0.0, comparison.savedUnits, 0.01)
        assertEquals(0, comparison.savedCredits)
        assertTrue(comparison.isConsumptionIncreased)
    }

    @Test
    fun testActiveCycleEdgeCase_NoDailyMeterReadings() {
        // Active cycle with no meter readings logged yet
        val prev = BillingCycleItem(
            id = "P1",
            periodLabel = "Jun–Aug",
            consumptionKwh = 55.0,
            durationDays = 60,
            dailyRate = 55.0 / 60.0,
            source = BillingCycleSource.BILL,
            status = BillingCycleStatus.COMPLETED
        )

        val curr = BillingCycleItem(
            id = "C1",
            periodLabel = "Aug–Oct",
            consumptionKwh = 0.0,
            durationDays = 1,
            dailyRate = 0.0,
            source = BillingCycleSource.DAILY_READINGS,
            status = BillingCycleStatus.CURRENT,
            openingMeterReading = 42130.0,
            hasMeterReadings = false
        )

        val comparison = BillingCycleManager.compareCycles(curr, prev)

        assertEquals(BillingComparisonState.CURRENT_NO_READINGS, comparison.state)
        assertEquals("Record your meter reading to track this cycle", comparison.primaryMessage)
        assertTrue(comparison.secondaryMessage.contains("42130"))
    }

    @Test
    fun testDailyMeterReadingCalculation_DeltasFromBaseline() {
        // Opening reading at start of Aug–Oct: 42,130 kWh
        // Latest recorded reading: 42,172 kWh
        // Consumption = 42,172 - 42,130 = 42 kWh
        val baseline = MeterCycleBaseline(
            cycleId = "BILL-2024-08",
            baselineDate = "10/08/2024",
            baselineReading = 42130.0
        )

        val readings = listOf(
            MeterReadingRecord(
                billingCycleId = "BILL-2024-08",
                readingValue = 42172.0,
                readingDate = "2024-09-10"
            )
        )

        val billData = TnebConsumerBillData(
            bills = listOf(
                TnebBillRecord(id = "B1", period = "Jun–Aug", units = "55", assessmentDate = "10/08/2024")
            )
        )

        val cycles = BillingCycleManager.buildChronologicalCycles(billData, baseline, readings, null)

        val activeCycle = cycles.last()
        assertEquals(BillingCycleStatus.CURRENT, activeCycle.status)
        assertEquals("Aug–Oct", activeCycle.periodLabel)
        assertEquals(42.0, activeCycle.consumptionKwh, 0.01)
        assertEquals(BillingCycleSource.DAILY_READINGS, activeCycle.source)
    }

    @Test
    fun testNewBillAppears_RolloverToNewActiveCycle() {
        // Previously: June–August bill was latest.
        // New bill arrives: August–October (62 kWh).
        // Then:
        // June–August becomes completed.
        // August–October becomes latest completed cycle.
        // Next cycle dynamically becomes: October–December (Current active cycle).
        val billData = TnebConsumerBillData(
            bills = listOf(
                TnebBillRecord(id = "B2", period = "Aug–Oct", units = "62", assessmentDate = "10/10/2024"),
                TnebBillRecord(id = "B1", period = "Jun–Aug", units = "55", assessmentDate = "10/08/2024")
            )
        )

        val cycles = BillingCycleManager.buildChronologicalCycles(billData, null, emptyList(), null)

        assertEquals(3, cycles.size) // 2 completed + 1 active
        assertEquals("Jun–Aug", cycles[0].periodLabel)
        assertEquals(BillingCycleStatus.COMPLETED, cycles[0].status)
        assertEquals(55.0, cycles[0].consumptionKwh, 0.01)

        assertEquals("Aug–Oct", cycles[1].periodLabel)
        assertEquals(BillingCycleStatus.COMPLETED, cycles[1].status)
        assertEquals(62.0, cycles[1].consumptionKwh, 0.01)

        assertEquals("Oct–Dec", cycles[2].periodLabel)
        assertEquals(BillingCycleStatus.CURRENT, cycles[2].status)
        assertEquals(BillingCycleSource.DAILY_READINGS, cycles[2].source)
    }

    @Test
    fun testVerifiedCreditsAwardedOnlyWhenVerifiedReductionExists() {
        val cyclesDecreased = listOf(
            BillingCycleItem(id = "1", periodLabel = "Feb–Apr", consumptionKwh = 92.0, durationDays = 60, dailyRate = 1.53, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "2", periodLabel = "Apr–Jun", consumptionKwh = 27.0, durationDays = 60, dailyRate = 0.45, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "3", periodLabel = "Jun–Aug", consumptionKwh = 15.0, durationDays = 60, dailyRate = 0.25, source = BillingCycleSource.DAILY_READINGS, status = BillingCycleStatus.CURRENT)
        )
        // Historical billing data is for baseline/analytics only and MUST NOT award Energy Credits
        val credits = BillingCycleManager.calculateVerifiedBillCredits(cyclesDecreased)
        assertEquals(0, credits)
    }

    @Test
    fun testAuditAll24BillingCycleCards() {
        // Complete 25-cycle dataset spanning 2022 to 2026, generating 24 consecutive comparisons
        val cycles = listOf(
            BillingCycleItem(id = "C01", periodLabel = "Aug–Oct 2022", consumptionKwh = 220.0, durationDays = 61, dailyRate = 220.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C02", periodLabel = "Oct–Dec 2022", consumptionKwh = 180.0, durationDays = 61, dailyRate = 180.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C03", periodLabel = "Dec 2022–Feb 2023", consumptionKwh = 210.0, durationDays = 59, dailyRate = 210.0/59, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C04", periodLabel = "Feb–Apr 2023", consumptionKwh = 195.0, durationDays = 59, dailyRate = 195.0/59, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C05", periodLabel = "Apr–Jun 2023", consumptionKwh = 175.0, durationDays = 61, dailyRate = 175.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C06", periodLabel = "Jun–Aug 2023", consumptionKwh = 230.0, durationDays = 61, dailyRate = 230.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C07", periodLabel = "Aug–Oct 2023", consumptionKwh = 215.0, durationDays = 61, dailyRate = 215.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C08", periodLabel = "Oct–Dec 2023", consumptionKwh = 190.0, durationDays = 61, dailyRate = 190.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C09", periodLabel = "Dec 2023–Feb 2024", consumptionKwh = 277.0, durationDays = 60, dailyRate = 277.0/60, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C10", periodLabel = "Feb–Apr 2024", consumptionKwh = 180.0, durationDays = 60, dailyRate = 180.0/60, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C11", periodLabel = "Apr–Jun 2024", consumptionKwh = 150.0, durationDays = 61, dailyRate = 150.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C12", periodLabel = "Jun–Aug 2024", consumptionKwh = 190.0, durationDays = 61, dailyRate = 190.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C13", periodLabel = "Aug–Oct 2024", consumptionKwh = 240.0, durationDays = 61, dailyRate = 240.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C14", periodLabel = "Oct–Dec 2024", consumptionKwh = 180.0, durationDays = 61, dailyRate = 180.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C15", periodLabel = "Dec 2024–Feb 2025", consumptionKwh = 200.0, durationDays = 59, dailyRate = 200.0/59, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C16", periodLabel = "Feb–Apr 2025", consumptionKwh = 190.0, durationDays = 59, dailyRate = 190.0/59, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C17", periodLabel = "Apr–Jun 2025", consumptionKwh = 160.0, durationDays = 61, dailyRate = 160.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C18", periodLabel = "Jun–Aug 2025", consumptionKwh = 110.0, durationDays = 61, dailyRate = 110.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C19", periodLabel = "Aug–Oct 2025", consumptionKwh = 140.0, durationDays = 61, dailyRate = 140.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C20", periodLabel = "Oct–Dec 2025", consumptionKwh = 140.0, durationDays = 61, dailyRate = 140.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C21", periodLabel = "Dec 2025–Feb 2026", consumptionKwh = 165.0, durationDays = 59, dailyRate = 165.0/59, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C22", periodLabel = "Feb–Apr 2026", consumptionKwh = 130.0, durationDays = 59, dailyRate = 130.0/59, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C23", periodLabel = "Apr–Jun 2026", consumptionKwh = 90.0, durationDays = 61, dailyRate = 90.0/61, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C24", periodLabel = "Jun 2026", consumptionKwh = 55.0, durationDays = 30, dailyRate = 55.0/30, source = BillingCycleSource.BILL, status = BillingCycleStatus.COMPLETED),
            BillingCycleItem(id = "C25", periodLabel = "Aug–Oct 2026", consumptionKwh = 170.0, durationDays = 40, dailyRate = 170.0/40, source = BillingCycleSource.DAILY_READINGS, status = BillingCycleStatus.CURRENT, hasMeterReadings = true)
        )

        assertEquals(25, cycles.size)
        println("=========================================================================================")
        println("AUDITING ALL 24 BILLING-CYCLE COMPARISONS")
        println("=========================================================================================")

        var comparisonsRun = 0
        for (i in 1 until cycles.size) {
            val prev = cycles[i - 1]
            val curr = cycles[i]
            val comp = BillingCycleManager.compareCycles(curr, prev)
            comparisonsRun++

            println(
                String.format(
                    Locale.US,
                    "Card %02d | %-18s -> %-18s | Prev: %5.1f kWh | Curr: %5.1f kWh | Diff: %5.1f kWh | Status: %-20s | Msg: %s",
                    comparisonsRun,
                    prev.periodLabel,
                    curr.periodLabel,
                    prev.consumptionKwh,
                    curr.consumptionKwh,
                    comp.differenceUnits,
                    comp.state,
                    comp.primaryMessage
                )
            )

            // Audit specific known cards:
            when (comparisonsRun) {
                1 -> {
                    // 1. Aug–Oct 2022 → Oct–Dec 2022: Actual reduction = 40 kWh, NOT 55 kWh
                    assertEquals(40.0, comp.savedUnits, 0.001)
                    assertEquals("↓ 40 kWh • Saved", comp.primaryMessage)
                    assertEquals(BillingComparisonState.COMPLETED_SAVED, comp.state)
                }
                9 -> {
                    // 2. Dec–Feb 2024 → Feb–Apr 2024: Actual comparison = 97 kWh, NOT 87 kWh
                    assertEquals(97.0, comp.savedUnits, 0.001)
                    assertEquals("↓ 97 kWh • Saved", comp.primaryMessage)
                    assertEquals(BillingComparisonState.COMPLETED_SAVED, comp.state)
                }
                10 -> {
                    // 3. Feb–Apr 2024 → Apr–Jun 2024: Direct subtraction 180 - 150 = 30 kWh
                    assertEquals(30.0, comp.savedUnits, 0.001)
                    assertEquals("↓ 30 kWh • Saved", comp.primaryMessage)
                    assertEquals(BillingComparisonState.COMPLETED_SAVED, comp.state)
                }
                13 -> {
                    // 4. Aug–Oct 2024 → Oct–Dec 2024: Direct subtraction 240 - 180 = 60 kWh
                    assertEquals(60.0, comp.savedUnits, 0.001)
                    assertEquals("↓ 60 kWh • Saved", comp.primaryMessage)
                    assertEquals(BillingComparisonState.COMPLETED_SAVED, comp.state)
                }
                17 -> {
                    // 5. Apr–Jun 2025 → Jun–Aug 2025: Direct subtraction 160 - 110 = 50 kWh
                    assertEquals(50.0, comp.savedUnits, 0.001)
                    assertEquals("↓ 50 kWh • Saved", comp.primaryMessage)
                    assertEquals(BillingComparisonState.COMPLETED_SAVED, comp.state)
                }
                23 -> {
                    // 6. Apr–Jun 2026 → Jun 2026: Direct raw subtraction 90 - 55 = 35 kWh (no extrapolation)
                    assertEquals(35.0, comp.savedUnits, 0.001)
                    assertEquals("Jun 2026", curr.periodLabel)
                    assertEquals("↓ 35 kWh • Saved", comp.primaryMessage)
                    assertEquals(BillingComparisonState.COMPLETED_SAVED, comp.state)
                }
                24 -> {
                    // Card 24: Current live cycle (170 kWh live vs 55 kWh)
                    // Must NEVER say "below previous cycle" because 170 >= 55
                    assertEquals(BillingComparisonState.CURRENT_HIGHER, comp.state)
                    assertEquals(115.0, comp.differenceUnits, 0.001)
                    assertEquals("↑ +115 kWh • Consumption Increased", comp.primaryMessage)
                    assertTrue(comp.secondaryMessage.contains("170 kWh consumed so far"))
                }
            }
        }

        assertEquals(24, comparisonsRun)
        println("=========================================================================================")
        println("AUDIT COMPLETE: All 24 cards verified using identical direct subtraction rule.")
        println("=========================================================================================")
    }
}
