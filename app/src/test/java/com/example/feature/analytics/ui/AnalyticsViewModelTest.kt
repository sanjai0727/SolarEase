package com.example.feature.analytics.ui

import com.example.feature.analytics.model.AnalyticsRange
import com.example.feature.home.ui.modules.MeterCycleBaseline
import com.example.feature.home.ui.modules.MeterReadingCache
import com.example.feature.home.ui.modules.TnebBillCache
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        TnebBillCache.clear(null)
        MeterReadingCache.clear(null)
    }

    @After
    fun tearDown() {
        TnebBillCache.clear(null)
        MeterReadingCache.clear(null)
        Dispatchers.resetMain()
    }

    @Test
    fun testEmptyStateWhenNoDataAvailable() = runTest(testDispatcher) {
        val viewModel = AnalyticsViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("State should not have data when no records exist", state.hasData)
        assertNull("Current cycle should be null in empty state", state.currentCycle)
        assertNull("Daily summary should be null in empty state", state.dailySummary)
        assertNull("Cycle comparison should be null in empty state", state.cycleComparison)
        assertNull("Bill breakdown should be null in empty state", state.billBreakdown)
    }

    @Test
    fun testPopulatedStateWithRealBillAndMeterData() = runTest(testDispatcher) {
        val testBill = TnebConsumerBillData(
            consumerNumber = "041234567890",
            consumerName = "K. Ramanathan",
            address = "No. 42 Anna Nagar, Chennai",
            tariffCode = "LT-1A",
            serviceCategory = "Domestic",
            isDuesNil = true,
            bills = listOf(
                TnebBillRecord(
                    id = "BILL-001",
                    period = "Jan - Feb 2026",
                    units = "240",
                    kwh = "12500",
                    amountDue = "₹ 870",
                    dueDate = "15 Feb 2026",
                    status = "PAID",
                    isPaid = true,
                    assessmentDate = "01 Jan 2026"
                ),
                TnebBillRecord(
                    id = "BILL-002",
                    period = "Nov - Dec 2025",
                    units = "280",
                    kwh = "12260",
                    amountDue = "₹ 1,150",
                    dueDate = "15 Dec 2025",
                    status = "PAID",
                    isPaid = true,
                    assessmentDate = "01 Nov 2025"
                )
            )
        )

        TnebBillCache.save(null, testBill)
        MeterReadingCache.syncBaselineFromTnebBill(testBill)

        // Add meter readings
        MeterReadingCache.saveReading(12620.0, "2026-01-15")
        MeterReadingCache.saveReading(12680.0, "2026-01-25")

        val viewModel = AnalyticsViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("State should be populated when bill and meter data exist", state.hasData)
        assertNotNull("Current cycle overview must not be null", state.currentCycle)

        val overview = state.currentCycle!!
        assertEquals("Baseline reading must match bill reading", 12500.0, overview.baselineReading, 0.01)
        assertEquals("Current reading must match latest saved reading", 12680.0, overview.currentReading, 0.01)
        assertEquals("Units consumed must match difference", 180.0, overview.unitsConsumed, 0.01)
        assertEquals("Tariff label must resolve to LT-IA · Domestic", "LT-IA · Domestic", overview.tariffCategory)
        assertEquals("Free allowance total must be 200 for <= 500 units in TNEB model", 200.0, overview.freeAllowanceTotal, 0.01)
        assertEquals("Free allowance remaining must be 200 - 180 = 20 kWh", 20.0, overview.freeAllowanceRemaining, 0.01)

        // Verify Daily Usage Summary
        assertNotNull("Daily summary must exist", state.dailySummary)
        val daily = state.dailySummary!!
        assertTrue("Daily points must not be empty", state.dailyPoints.isNotEmpty())
        assertNotNull("Highest day point must exist", daily.highestDay)
        assertTrue("Highest day kWh must be positive", daily.highestDay!!.dailyKwh > 0.0)

        // Verify Cycle Comparison
        assertNotNull("Cycle comparison must exist", state.cycleComparison)
        val comp = state.cycleComparison!!
        assertEquals(180.0, comp.currentUnits, 0.01)

        // Verify Tariff Slabs
        assertTrue("Tariff slabs should not be empty", state.tariffSlabs.isNotEmpty())
        val freeSlab = state.tariffSlabs.firstOrNull { it.name.contains("0") && it.name.contains("100") }
        assertNotNull("0-100 free units slab must exist", freeSlab)
        assertEquals("₹0.00 / unit", freeSlab!!.rateStr)

        // Verify Bill Breakdown
        assertNotNull("Bill breakdown must exist", state.billBreakdown)
        val breakdown = state.billBreakdown!!
        assertEquals(180.0, breakdown.cycleConsumptionKwh, 0.01)
        assertEquals(200.0, breakdown.freeAllowanceKwh, 0.01)
        assertEquals(0.0, breakdown.chargeableUnitsKwh, 0.01)

        // Verify Energy Insights
        assertTrue("Energy insights must be generated", state.insights.isNotEmpty())
    }

    @Test
    fun testRangeSwitchingFiltersCycles() = runTest(testDispatcher) {
        val testBill = TnebConsumerBillData(
            consumerNumber = "041234567890",
            consumerName = "K. Ramanathan",
            tariffCode = "LT-1A",
            serviceCategory = "Domestic",
            bills = (1..6).map { i ->
                TnebBillRecord(
                    id = "BILL-$i",
                    period = "Cycle $i",
                    units = "${150 + i * 20}",
                    kwh = "${10000 + i * 200}",
                    amountDue = "₹ ${500 + i * 100}",
                    assessmentDate = "2025-0$i-01"
                )
            }
        )

        TnebBillCache.save(null, testBill)
        MeterReadingCache.syncBaselineFromTnebBill(testBill)

        val viewModel = AnalyticsViewModel()
        advanceUntilIdle()

        assertEquals(AnalyticsRange.M6, viewModel.uiState.value.selectedRange)

        viewModel.setRange(AnalyticsRange.M2)
        advanceUntilIdle()
        assertEquals(AnalyticsRange.M2, viewModel.uiState.value.selectedRange)
        assertTrue(viewModel.uiState.value.historicalCycles.size <= 2)

        viewModel.setRange(AnalyticsRange.M4)
        advanceUntilIdle()
        assertEquals(AnalyticsRange.M4, viewModel.uiState.value.selectedRange)
        assertTrue(viewModel.uiState.value.historicalCycles.size <= 4)
    }

    @Test
    fun testDailyPointSelectionUpdatesTooltip() = runTest(testDispatcher) {
        val testBill = TnebConsumerBillData(
            consumerNumber = "041234567890",
            consumerName = "K. Ramanathan",
            tariffCode = "LT-1A",
            serviceCategory = "Domestic",
            bills = listOf(
                TnebBillRecord(
                    id = "BILL-001",
                    period = "Jan - Feb 2026",
                    units = "200",
                    kwh = "5000",
                    amountDue = "₹ 600"
                )
            )
        )
        TnebBillCache.save(null, testBill)
        MeterReadingCache.syncBaselineFromTnebBill(testBill)
        MeterReadingCache.saveReading(5050.0, "2026-01-10")

        val viewModel = AnalyticsViewModel()
        advanceUntilIdle()

        val points = viewModel.uiState.value.dailyPoints
        assertTrue("Daily points should not be empty", points.isNotEmpty())

        val targetPoint = points.first()
        viewModel.selectDailyPoint(targetPoint)
        advanceUntilIdle()

        assertEquals(targetPoint, viewModel.uiState.value.selectedDailyPoint)

        viewModel.selectDailyPoint(null)
        advanceUntilIdle()
        val defaultPoint = points.maxByOrNull { it.dailyKwh }
        assertEquals(defaultPoint, viewModel.uiState.value.selectedDailyPoint)
    }
}
