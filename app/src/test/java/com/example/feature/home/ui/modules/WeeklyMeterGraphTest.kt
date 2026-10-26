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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeeklyMeterGraphTest {

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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

    /**
     * Anchor helper: returns a Wednesday anchor Date for a deterministic calendar reference.
     * 2026-10-14 is Wednesday.
     * Corresponding week: Sunday 2026-10-11 to Saturday 2026-10-17.
     */
    private fun getTestAnchorWednesday(): Date {
        return sdf.parse("2026-10-14")!!
    }

    @Test
    fun test1_weeklyBaselineRule1_priorReadingBeforeSunday() {
        // Prior reading on Saturday before the week: 2026-10-10 with reading 12000.0 kWh
        val satRecord = MeterReadingRecord(
            id = "rec-1",
            readingValue = 12000.0,
            readingDate = "2026-10-10",
            displayDate = "10 Oct 2026"
        )
        // In the test week (2026-10-11 Sun to 2026-10-17 Sat):
        // Reading on Monday (2026-10-12): 12015.0 kWh (+15 relative)
        val monRecord = MeterReadingRecord(
            id = "rec-2",
            readingValue = 12015.0,
            readingDate = "2026-10-12",
            displayDate = "12 Oct 2026"
        )
        // Reading on Thursday (2026-10-15): 12040.0 kWh (+40 relative)
        val thuRecord = MeterReadingRecord(
            id = "rec-3",
            readingValue = 12040.0,
            readingDate = "2026-10-15",
            displayDate = "15 Oct 2026"
        )

        MeterReadingCache.setReadingsForTesting(listOf(satRecord, monRecord, thuRecord))

        val anchor = getTestAnchorWednesday()
        val graphData = MeterReadingCache.getWeeklyGraphData(weekOffset = 0, anchorDate = anchor)

        // Baseline must be the prior reading on 2026-10-10
        assertEquals(12000.0, graphData.weeklyBaselineReading ?: 0.0, 0.001)
        assertTrue(graphData.baselineDescription.contains("Baseline from previous reading"))

        assertEquals(7, graphData.days.size)

        // Monday day: relativeKwh = 12015.0 - 12000.0 = 15.0 kWh
        val mon = graphData.days.first { it.dateIso == "2026-10-12" }
        assertNotNull(mon.reading)
        assertEquals(12015.0, mon.cumulativeKwh ?: 0.0, 0.001)
        assertEquals(15.0, mon.relativeKwh ?: 0.0, 0.001)
        assertFalse(mon.isBaselinePoint)
        assertFalse(mon.isDecreasedReading)

        // Thursday day: relativeKwh = 12040.0 - 12000.0 = 40.0 kWh
        val thu = graphData.days.first { it.dateIso == "2026-10-15" }
        assertNotNull(thu.reading)
        assertEquals(12040.0, thu.cumulativeKwh ?: 0.0, 0.001)
        assertEquals(40.0, thu.relativeKwh ?: 0.0, 0.001)
        assertEquals(25.0, thu.intervalConsumption ?: 0.0, 0.001) // 12040 - 12015 = 25
        assertEquals(3, thu.intervalDays) // 12 Oct to 15 Oct = 3 days
    }

    @Test
    fun test2_weeklyBaselineRule2_midweekOnboardingFirstReadingAnchor() {
        // No prior reading exists before Sunday.
        // User starts logging on Wednesday (2026-10-14): 15000.0 kWh
        val wedRecord = MeterReadingRecord(
            id = "rec-wed",
            readingValue = 15000.0,
            readingDate = "2026-10-14",
            displayDate = "14 Oct 2026"
        )
        // User logs on Friday (2026-10-16): 15022.0 kWh
        val friRecord = MeterReadingRecord(
            id = "rec-fri",
            readingValue = 15022.0,
            readingDate = "2026-10-16",
            displayDate = "16 Oct 2026"
        )

        MeterReadingCache.setReadingsForTesting(listOf(wedRecord, friRecord), base = null)

        val anchor = getTestAnchorWednesday()
        val graphData = MeterReadingCache.getWeeklyGraphData(weekOffset = 0, anchorDate = anchor)

        // Baseline must be the midweek first reading (15000.0 kWh)
        assertEquals(15000.0, graphData.weeklyBaselineReading ?: 0.0, 0.001)
        assertTrue(graphData.baselineDescription.contains("Midweek baseline"))

        // Wednesday is the baseline anchor point and appears at 0.0 kWh
        val wed = graphData.days.first { it.dateIso == "2026-10-14" }
        assertNotNull(wed.reading)
        assertTrue(wed.isBaselinePoint)
        assertEquals(0.0, wed.relativeKwh ?: -1.0, 0.001)
        assertEquals(15000.0, wed.cumulativeKwh ?: 0.0, 0.001)

        // Friday displays relative consumption above the midweek baseline: 15022.0 - 15000.0 = 22.0 kWh
        val fri = graphData.days.first { it.dateIso == "2026-10-16" }
        assertNotNull(fri.reading)
        assertFalse(fri.isBaselinePoint)
        assertEquals(22.0, fri.relativeKwh ?: 0.0, 0.001)
        assertEquals(15022.0, fri.cumulativeKwh ?: 0.0, 0.001)
        assertEquals(22.0, fri.intervalConsumption ?: 0.0, 0.001)
    }

    @Test
    fun test3_decreasedReadingSafelyHandledWithoutNegativeConsumption() {
        // Normal prior reading on Sunday (2026-10-11): 10000.0 kWh
        val sunRecord = MeterReadingRecord(
            id = "rec-sun",
            readingValue = 10000.0,
            readingDate = "2026-10-11",
            displayDate = "11 Oct 2026"
        )
        // Erroneous or decreased reading on Monday (2026-10-12): 9950.0 kWh
        val monRecord = MeterReadingRecord(
            id = "rec-mon",
            readingValue = 9950.0,
            readingDate = "2026-10-12",
            displayDate = "12 Oct 2026"
        )

        MeterReadingCache.setReadingsForTesting(listOf(sunRecord, monRecord))

        val anchor = getTestAnchorWednesday()
        val graphData = MeterReadingCache.getWeeklyGraphData(weekOffset = 0, anchorDate = anchor)

        val mon = graphData.days.first { it.dateIso == "2026-10-12" }
        assertNotNull(mon.reading)
        assertTrue(mon.isDecreasedReading)
        // Must NOT compute negative consumption
        assertNull(mon.intervalConsumption)
        assertNull(mon.relativeKwh)
        assertEquals(9950.0, mon.cumulativeKwh ?: 0.0, 0.001)
    }

    @Test
    fun test4_missingDaysPreservedWithoutFabrication() {
        // Only Tuesday reading exists in the week
        val tueRecord = MeterReadingRecord(
            id = "rec-tue",
            readingValue = 8500.0,
            readingDate = "2026-10-13",
            displayDate = "13 Oct 2026"
        )
        MeterReadingCache.setReadingsForTesting(listOf(tueRecord))

        val anchor = getTestAnchorWednesday()
        val graphData = MeterReadingCache.getWeeklyGraphData(weekOffset = 0, anchorDate = anchor)

        assertEquals(7, graphData.days.size)

        // Sunday, Monday, Wednesday, Thursday, Friday, Saturday must be empty without fabricated points
        val emptyDays = graphData.days.filter { it.dateIso != "2026-10-13" }
        assertEquals(6, emptyDays.size)
        emptyDays.forEach { day ->
            assertNull(day.reading)
            assertNull(day.cumulativeKwh)
            assertNull(day.relativeKwh)
            assertNull(day.intervalConsumption)
        }

        // Tuesday has data
        val tue = graphData.days.first { it.dateIso == "2026-10-13" }
        assertNotNull(tue.reading)
    }

    @Test
    fun test5_weekNavigationAndFutureWeekPrevention() {
        // Add readings across two weeks:
        // Week -1 (prior week): 2026-10-06
        // Week 0 (current week): 2026-10-14
        val prevRecord = MeterReadingRecord(
            id = "rec-p",
            readingValue = 7000.0,
            readingDate = "2026-10-06",
            displayDate = "6 Oct 2026"
        )
        val currRecord = MeterReadingRecord(
            id = "rec-c",
            readingValue = 7150.0,
            readingDate = "2026-10-14",
            displayDate = "14 Oct 2026"
        )
        MeterReadingCache.setReadingsForTesting(listOf(prevRecord, currRecord))

        val anchor = getTestAnchorWednesday()

        // Current week (offset 0)
        val currentWeek = MeterReadingCache.getWeeklyGraphData(weekOffset = 0, anchorDate = anchor)
        assertFalse("Navigation forward into future weeks must be disabled", currentWeek.hasNewer)
        assertTrue("Has older readings in previous week", currentWeek.hasOlder)
        assertTrue(currentWeek.periodTitle.startsWith("This Week"))

        // Previous week (offset 1)
        val prevWeek = MeterReadingCache.getWeeklyGraphData(weekOffset = 1, anchorDate = anchor)
        assertTrue("Navigation forward back to current week must be enabled", prevWeek.hasNewer)
        assertEquals(7, prevWeek.days.size)
        assertEquals("2026-10-04", prevWeek.startDateIso)
        assertEquals("2026-10-10", prevWeek.endDateIso)
    }

    @Test
    fun test6_originalCumulativeReadingsPreservedWithoutMutation() {
        val r1 = MeterReadingRecord(id = "1", readingValue = 11000.0, readingDate = "2026-10-01")
        val r2 = MeterReadingRecord(id = "2", readingValue = 11050.0, readingDate = "2026-10-05")
        val r3 = MeterReadingRecord(id = "3", readingValue = 11120.0, readingDate = "2026-10-12")
        MeterReadingCache.setReadingsForTesting(listOf(r1, r2, r3))

        val all = MeterReadingCache.getAllSavedReadings()
        assertEquals(3, all.size)
        assertEquals(11000.0, all[0].readingValue, 0.001)
        assertEquals(11050.0, all[1].readingValue, 0.001)
        assertEquals(11120.0, all[2].readingValue, 0.001)
    }

    @Test
    fun test7_readingDetailsFilter7D_30D_ALL() {
        // Create 10 distinct readings across dates
        val readings = (1..10).map { i ->
            val dayStr = String.format(Locale.US, "%02d", i)
            MeterReadingRecord(
                id = "id-$i",
                readingValue = 10000.0 + (i * 10.0), // 10010.0, 10020.0, ... 10100.0
                readingDate = "2026-10-$dayStr",
                displayDate = "$dayStr Oct 2026",
                timestamp = 1000L * i
            )
        }

        MeterReadingCache.setReadingsForTesting(readings)

        // 7D Filter: should return exactly the latest 7 saved readings (newest first: Oct 10 to Oct 04)
        val d7 = MeterReadingCache.getFilteredReadingHistory(ReadingDetailsFilter.D7)
        assertEquals(7, d7.size)
        assertEquals("2026-10-10", d7[0].reading.readingDate)
        assertEquals("2026-10-09", d7[1].reading.readingDate)
        assertEquals("2026-10-04", d7[6].reading.readingDate)
        // Precomputed interval consumption: 10100 - 10090 = 10.0
        assertEquals(10.0, d7[0].consumptionSincePrevious ?: 0.0, 0.001)
        assertEquals(1, d7[0].intervalDays)

        // 30D Filter: with 10 total readings, returns all 10 saved readings (newest first)
        val d30 = MeterReadingCache.getFilteredReadingHistory(ReadingDetailsFilter.D30)
        assertEquals(10, d30.size)
        assertEquals("2026-10-10", d30[0].reading.readingDate)
        assertEquals("2026-10-01", d30[9].reading.readingDate)

        // ALL Filter: returns all 10 saved readings (newest first)
        val all = MeterReadingCache.getFilteredReadingHistory(ReadingDetailsFilter.ALL)
        assertEquals(10, all.size)
        assertEquals("2026-10-10", all[0].reading.readingDate)
        assertEquals("2026-10-01", all[9].reading.readingDate)
        assertTrue(all[9].isBaseline) // Earliest reading is baseline anchor
    }
}
