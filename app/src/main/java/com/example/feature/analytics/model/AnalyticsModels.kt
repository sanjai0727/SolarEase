package com.example.feature.analytics.model

enum class AnalyticsRange(val label: String) {
    M2("2M"),
    M4("4M"),
    M6("6M Active")
}

data class CurrentCycleOverviewData(
    val periodDisplay: String,
    val prevBillDate: String,
    val currentDate: String,
    val tariffCategory: String,
    val unitsConsumed: Double,
    val daysElapsed: Int,
    val avgUnitsPerDay: Double,
    val baselineReading: Double,
    val currentReading: Double,
    val estimatedBill: Double,
    val freeAllowanceTotal: Double,
    val freeAllowanceRemaining: Double,
    val freeAllowanceDescription: String,
    val slabLabel: String
)

data class DailyUsagePoint(
    val dateLabel: String,
    val fullDate: String,
    val dailyKwh: Double,
    val cumulativeKwh: Double
)

data class DailyUsageSummary(
    val highestDay: DailyUsagePoint?,
    val lowestDay: DailyUsagePoint?,
    val avgDailyKwh: Double,
    val totalDays: Int
)

data class CycleComparisonData(
    val currentUnits: Double,
    val prevUnitsEq: Double,
    val deltaUnits: Double,
    val deltaPercent: Double,
    val isIncrease: Boolean
)

data class HistoricalCycleItem(
    val id: String,
    val period: String,
    val fullPeriod: String,
    val units: Double,
    val amount: String,
    val status: String,
    val isCurrent: Boolean,
    val ratio: Float
)

data class TariffSlabProgress(
    val name: String,
    val rateStr: String,
    val fillFraction: Float,
    val tierColorHex: Long
)

data class BillBreakdownData(
    val cycleConsumptionKwh: Double,
    val freeAllowanceKwh: Double,
    val chargeableUnitsKwh: Double,
    val estimatedEnergyCharge: Double,
    val fixedCharges: Double,
    val totalEstimatedDues: Double
)

data class EnergyInsightItem(
    val title: String,
    val description: String,
    val iconType: String,
    val accentColorHex: Long
)

data class AnalyticsUiState(
    val isLoading: Boolean = false,
    val hasData: Boolean = false,
    val userLocation: String = "Valmiki Nagar, Chennai",
    val selectedRange: AnalyticsRange = AnalyticsRange.M6,
    val currentCycle: CurrentCycleOverviewData? = null,
    val dailyPoints: List<DailyUsagePoint> = emptyList(),
    val dailySummary: DailyUsageSummary? = null,
    val selectedDailyPoint: DailyUsagePoint? = null,
    val cycleComparison: CycleComparisonData? = null,
    val historicalCycles: List<HistoricalCycleItem> = emptyList(),
    val historyAvgUnits: Double = 0.0,
    val tariffSlabs: List<TariffSlabProgress> = emptyList(),
    val billBreakdown: BillBreakdownData? = null,
    val insights: List<EnergyInsightItem> = emptyList(),
    val trendPercentageText: String = "+0.0% vs Previous Cycle"
)
