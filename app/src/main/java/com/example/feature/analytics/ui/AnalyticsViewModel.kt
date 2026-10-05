package com.example.feature.analytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.UserSession
import com.example.feature.analytics.model.*
import com.example.feature.home.ui.modules.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

class AnalyticsViewModel : ViewModel() {

    private val _selectedRange = MutableStateFlow(AnalyticsRange.M6)
    val selectedRange: StateFlow<AnalyticsRange> = _selectedRange.asStateFlow()

    private val _selectedDailyPoint = MutableStateFlow<DailyUsagePoint?>(null)
    val selectedDailyPoint: StateFlow<DailyUsagePoint?> = _selectedDailyPoint.asStateFlow()

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                TnebBillCache.billData,
                MeterReadingCache.baseline,
                MeterReadingCache.metrics,
                MeterReadingCache.readings,
                _selectedRange,
                _selectedDailyPoint
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                buildAnalyticsUiState(
                    billData = args[0] as? TnebConsumerBillData,
                    baseline = args[1] as? MeterCycleBaseline,
                    metrics = args[2] as? CurrentCycleEnergyMetrics,
                    readings = (args[3] as? List<*>)?.filterIsInstance<MeterReadingRecord>() ?: emptyList(),
                    range = args[4] as? AnalyticsRange ?: AnalyticsRange.M6,
                    selectedPoint = args[5] as? DailyUsagePoint
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setRange(range: AnalyticsRange) {
        _selectedRange.value = range
    }

    fun selectDailyPoint(point: DailyUsagePoint?) {
        _selectedDailyPoint.value = point
    }

    private fun buildAnalyticsUiState(
        billData: TnebConsumerBillData?,
        baseline: MeterCycleBaseline?,
        metrics: CurrentCycleEnergyMetrics?,
        readings: List<MeterReadingRecord>,
        range: AnalyticsRange,
        selectedPoint: DailyUsagePoint?
    ): AnalyticsUiState {
        val user = UserSession.currentUser
        val userLocation = user?.houseAddress?.takeIf { it.isNotBlank() } ?: "Valmiki Nagar, Chennai"

        val hasBill = billData != null && (billData.bills.isNotEmpty() || billData.consumerNumber.isNotBlank())
        val hasBaseline = baseline != null && baseline.baselineReading > 0.0
        val hasReadings = readings.isNotEmpty()

        if (!hasBill && !hasBaseline && !hasReadings) {
            return AnalyticsUiState(
                isLoading = false,
                hasData = false,
                userLocation = userLocation,
                selectedRange = range
            )
        }

        // 1. Current Cycle Overview
        val baselineDate = baseline?.baselineDate?.ifBlank { "24 Aug" } ?: "24 Aug"
        val currentDate = metrics?.currentReadingDate?.ifBlank { "Today" } ?: "Today"
        val periodDisplay = metrics?.billingPeriodDisplay ?: "$baselineDate – $currentDate"

        val rawTariff = billData?.tariffCode.orEmpty().ifBlank {
            billData?.serviceCategory.orEmpty().ifBlank {
                baseline?.tariffCode.orEmpty()
            }
        }
        val normalizedTariff = TariffResolver.resolve(rawTariff)

        val unitsConsumed = metrics?.unitsConsumed ?: 0.0
        val daysElapsed = metrics?.daysElapsed ?: 1
        val avgUnitsPerDay = if (daysElapsed > 0) unitsConsumed / daysElapsed else unitsConsumed
        val baseReading = baseline?.baselineReading ?: 0.0
        val currReading = metrics?.currentReading ?: (baseReading + unitsConsumed)
        val estimatedBill = metrics?.estimatedBill ?: 0.0

        val freeAllowanceTotal = if (unitsConsumed <= 500.0) 200.0 else 100.0
        val freeAllowanceRemaining = max(0.0, freeAllowanceTotal - unitsConsumed)
        val freeAllowanceDesc = when {
            unitsConsumed <= 100.0 -> "Within 100-unit free allowance"
            unitsConsumed <= freeAllowanceTotal -> "Within ${freeAllowanceTotal.toInt()}-unit free allowance"
            else -> "Exceeded by ${(unitsConsumed - freeAllowanceTotal).toInt()} kWh"
        }

        val slabLabel = when {
            unitsConsumed <= 100.0 -> "Slab 0"
            unitsConsumed <= 200.0 -> "Slab 1"
            unitsConsumed <= 400.0 -> "Slab 2"
            unitsConsumed <= 500.0 -> "Slab 3"
            else -> "Slab 4"
        }

        val currentCycle = CurrentCycleOverviewData(
            periodDisplay = periodDisplay,
            prevBillDate = baselineDate,
            currentDate = currentDate,
            tariffCategory = normalizedTariff.displayLabel,
            unitsConsumed = unitsConsumed,
            daysElapsed = daysElapsed,
            avgUnitsPerDay = avgUnitsPerDay,
            baselineReading = baseReading,
            currentReading = currReading,
            estimatedBill = estimatedBill,
            freeAllowanceTotal = freeAllowanceTotal,
            freeAllowanceRemaining = freeAllowanceRemaining,
            freeAllowanceDescription = freeAllowanceDesc,
            slabLabel = slabLabel
        )

        // 2. Daily Consumption Points
        val rawDaily = MeterReadingCache.getDailyConsumptionPoints()
        val dailyPoints = if (rawDaily.isNotEmpty()) {
            rawDaily.map {
                DailyUsagePoint(
                    dateLabel = it.displayDate,
                    fullDate = it.fullDate,
                    dailyKwh = it.dailyKwh,
                    cumulativeKwh = it.cumulativeReading
                )
            }
        } else if (unitsConsumed > 0.0) {
            // Anchor start and end
            listOf(
                DailyUsagePoint(
                    dateLabel = formatToShort(baselineDate),
                    fullDate = baselineDate,
                    dailyKwh = 0.0,
                    cumulativeKwh = baseReading
                ),
                DailyUsagePoint(
                    dateLabel = formatToShort(currentDate),
                    fullDate = currentDate,
                    dailyKwh = unitsConsumed,
                    cumulativeKwh = currReading
                )
            )
        } else {
            emptyList()
        }

        val activeDailyPoint = selectedPoint ?: dailyPoints.maxByOrNull { it.dailyKwh }

        val dailySummary = if (dailyPoints.isNotEmpty()) {
            val high = dailyPoints.filter { it.dailyKwh > 0 }.maxByOrNull { it.dailyKwh }
                ?: dailyPoints.maxByOrNull { it.dailyKwh }
            val low = dailyPoints.filter { it.dailyKwh > 0 }.minByOrNull { it.dailyKwh }
                ?: dailyPoints.minByOrNull { it.dailyKwh }
            val avg = if (dailyPoints.isNotEmpty()) dailyPoints.map { it.dailyKwh }.average() else avgUnitsPerDay
            DailyUsageSummary(
                highestDay = high,
                lowestDay = low,
                avgDailyKwh = avg,
                totalDays = daysElapsed
            )
        } else null

        // 3. Comparison with Previous Cycle
        val rawBills = billData?.bills ?: emptyList()
        val prevBilledUnits = baseline?.previousBilledUnits
            ?: rawBills.firstOrNull()?.units?.toDoubleOrNull()
            ?: (unitsConsumed * 0.9).coerceAtLeast(100.0)

        val prevUnitsEq = prevBilledUnits * (daysElapsed / 60.0).coerceIn(0.1, 1.0)
        val deltaUnits = unitsConsumed - prevUnitsEq
        val deltaPercent = if (prevUnitsEq > 0) (deltaUnits / prevUnitsEq) * 100.0 else 0.0
        val isIncrease = deltaUnits >= 0

        val cycleComparison = CycleComparisonData(
            currentUnits = unitsConsumed,
            prevUnitsEq = prevUnitsEq,
            deltaUnits = abs(deltaUnits),
            deltaPercent = abs(deltaPercent),
            isIncrease = isIncrease
        )

        // 4. Historical Cycles & Range filtering
        val limit = when (range) {
            AnalyticsRange.M2 -> 1
            AnalyticsRange.M4 -> 2
            AnalyticsRange.M6 -> 4
        }

        val filteredBills = rawBills.take(limit)
        val allHistoryItems = mutableListOf<HistoricalCycleItem>()

        // Add current active cycle
        allHistoryItems.add(
            HistoricalCycleItem(
                id = "current",
                period = formatShortPeriod(baselineDate, currentDate),
                fullPeriod = "$baselineDate – $currentDate",
                units = unitsConsumed,
                amount = if (estimatedBill > 0.0) "₹${estimatedBill.toInt()}" else "₹0",
                status = "CURRENT · PENDING",
                isCurrent = true,
                ratio = 1f
            )
        )

        for (bill in filteredBills) {
            val bUnits = bill.units.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                ?: bill.kwh.filter { it.isDigit() || it == '.' }.toDoubleOrNull()
                ?: 0.0
            val bAmount = bill.amountDue.trim().let { if (it.startsWith("₹")) it else "₹$it" }
            val bPeriod = bill.period.ifBlank { bill.assessmentDate.ifBlank { "Past Cycle" } }
            val isPaid = bill.isPaid || (billData?.isDuesNil == true)
            allHistoryItems.add(
                HistoricalCycleItem(
                    id = bill.id.ifBlank { UUID.randomUUID().toString() },
                    period = formatShortPeriodFromText(bPeriod),
                    fullPeriod = bPeriod,
                    units = bUnits,
                    amount = bAmount,
                    status = if (isPaid) "PAID" else "DUE",
                    isCurrent = false,
                    ratio = 1f
                )
            )
        }

        val maxUnitsInHistory = allHistoryItems.maxOfOrNull { it.units }?.coerceAtLeast(1.0) ?: 1.0
        val historyWithRatios = allHistoryItems.map {
            it.copy(ratio = (it.units / maxUnitsInHistory).toFloat().coerceIn(0.1f, 1f))
        }

        val avgHist = if (allHistoryItems.isNotEmpty()) {
            allHistoryItems.map { it.units }.average()
        } else unitsConsumed

        // 5. Tariff Slabs (TNEB Slabs: 0-100, 101-200, 201+)
        val slab1Fill = (unitsConsumed / 100.0).coerceIn(0.0, 1.0).toFloat()
        val slab2Fill = ((unitsConsumed - 100.0) / 100.0).coerceIn(0.0, 1.0).toFloat()
        val slab3Fill = ((unitsConsumed - 200.0) / 300.0).coerceIn(0.0, 1.0).toFloat()

        val tariffSlabs = listOf(
            TariffSlabProgress(
                name = "0 – 100 kWh (Free Base Tier)",
                rateStr = "₹0.00 / unit",
                fillFraction = slab1Fill,
                tierColorHex = 0xFF047857
            ),
            TariffSlabProgress(
                name = "101 – 200 kWh (Subsidized Tier)",
                rateStr = "₹2.25 / unit",
                fillFraction = slab2Fill,
                tierColorHex = 0xFF2563EB
            ),
            TariffSlabProgress(
                name = "201+ kWh (Standard Tier)",
                rateStr = "₹4.50 / unit",
                fillFraction = slab3Fill,
                tierColorHex = 0xFFD97706
            )
        )

        // 6. Bill & Consumption Breakdown
        val calc = TNEBBillCalculator.calculateTNEBBill(unitsConsumed)
        val billBreakdown = BillBreakdownData(
            cycleConsumptionKwh = unitsConsumed,
            freeAllowanceKwh = calc.freeUnits,
            chargeableUnitsKwh = calc.chargeableUnits,
            estimatedEnergyCharge = calc.totalEnergyCharge,
            fixedCharges = 0.0,
            totalEstimatedDues = calc.totalEnergyCharge
        )

        // 7. Energy Insights
        val insights = mutableListOf<EnergyInsightItem>()

        val highestCycle = allHistoryItems.maxByOrNull { it.units }
        if (highestCycle != null) {
            insights.add(
                EnergyInsightItem(
                    title = "Highest Consumption",
                    description = "${highestCycle.period} recorded the highest consumption at ${highestCycle.units.toInt()} kWh across monitored billing cycles.",
                    iconType = "north_east",
                    accentColorHex = 0xFF2563EB
                )
            )
        }

        insights.add(
            EnergyInsightItem(
                title = "Average Consumption",
                description = "Historical baseline across ${allHistoryItems.size} monitored billing cycles is ${avgHist.toInt()} kWh/cycle.",
                iconType = "speed",
                accentColorHex = 0xFF047857
            )
        )

        val deltaSign = if (isIncrease) "+" else "-"
        insights.add(
            EnergyInsightItem(
                title = "Recent Change",
                description = "Consumption ${if (isIncrease) "increased" else "decreased"} $deltaSign${String.format(Locale.US, "%.1f", abs(deltaPercent))}% ($deltaSign${abs(deltaUnits).toInt()} kWh) compared to the preceding cycle.",
                iconType = "trending_up",
                accentColorHex = 0xFF7C3AED
            )
        )

        val lowestCycle = allHistoryItems.minByOrNull { it.units }
        if (lowestCycle != null) {
            insights.add(
                EnergyInsightItem(
                    title = "Most Efficient Cycle",
                    description = "${lowestCycle.period} was the most energy-efficient period with ${lowestCycle.units.toInt()} kWh recorded.",
                    iconType = "energy_savings_leaf",
                    accentColorHex = 0xFFD97706
                )
            )
        }

        val trendPercentage = "${if (isIncrease) "+" else "-"}${String.format(Locale.US, "%.1f", abs(deltaPercent))}% vs Previous Cycle"

        return AnalyticsUiState(
            isLoading = false,
            hasData = true,
            userLocation = userLocation,
            selectedRange = range,
            currentCycle = currentCycle,
            dailyPoints = dailyPoints,
            dailySummary = dailySummary,
            selectedDailyPoint = activeDailyPoint,
            cycleComparison = cycleComparison,
            historicalCycles = historyWithRatios,
            historyAvgUnits = avgHist,
            tariffSlabs = tariffSlabs,
            billBreakdown = billBreakdown,
            insights = insights,
            trendPercentageText = trendPercentage
        )
    }

    private fun formatShortPeriod(start: String, end: String): String {
        val s = formatToShort(start)
        val e = formatToShort(end)
        return "$s – $e"
    }

    private fun formatShortPeriodFromText(text: String): String {
        return text.replace("2026", "").replace("2025", "").trim(' ', '-', '–')
    }

    private fun formatToShort(dateStr: String): String {
        val formats = listOf("dd MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "dd-MM-yyyy")
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.US)
                val d = sdf.parse(dateStr)
                if (d != null) {
                    return SimpleDateFormat("d MMM", Locale.US).format(d)
                }
            } catch (_: Exception) {}
        }
        return dateStr
    }
}
