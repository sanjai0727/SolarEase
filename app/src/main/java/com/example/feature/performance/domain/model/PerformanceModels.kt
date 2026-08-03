package com.example.feature.performance.domain.model

data class LiveGeneration(
    val currentOutput: Double,
    val todayGeneration: Double,
    val systemStatus: String,
    val gridStatus: String,
    val lastUpdated: Long,
    val weather: String,
    val hasLiveMonitoring: Boolean = true
)

data class TodayPerformance(
    val energyGenerated: Double,
    val peakPower: Double,
    val operatingHours: Double,
    val averageEfficiency: Double,
    val comparedToYesterday: Double // percentage
)

data class MonthPerformance(
    val monthlyGeneration: Double,
    val monthlySavings: Double,
    val averageDailyGeneration: Double,
    val peakProductionDay: String,
    val bestPerformingWeek: String
)

data class YearPerformance(
    val annualGeneration: Double,
    val annualSavings: Double,
    val capacityUtilization: Double,
    val totalRuntime: Int, // hours
    val expectedGeneration: Double,
    val actualGeneration: Double
)

data class ChartDataPoint(
    val label: String,
    val value: Double
)

data class GenerationHistory(
    val daily: List<ChartDataPoint>,
    val weekly: List<ChartDataPoint>,
    val monthly: List<ChartDataPoint>,
    val yearly: List<ChartDataPoint>
)

data class EnergyFlow(
    val solarGenerated: Double,
    val consumed: Double,
    val exportedToGrid: Double,
    val importedFromGrid: Double,
    val batteryStatus: String = "Not Installed"
)

data class SystemEfficiency(
    val performanceRatio: Double,
    val panelEfficiency: Double,
    val inverterEfficiency: Double,
    val temperatureEffectLoss: Double,
    val shadingLoss: Double,
    val dustLoss: Double
)

data class PerformanceInsight(
    val message: String,
    val type: String // "positive", "neutral", "warning"
)

data class ProductionForecast(
    val expectedGeneration: Double,
    val expectedSavings: Double,
    val weatherImpact: String,
    val confidence: String // "High", "Medium", "Low"
)

data class PerformanceDashboard(
    val liveGeneration: LiveGeneration,
    val todayPerformance: TodayPerformance,
    val monthPerformance: MonthPerformance,
    val yearPerformance: YearPerformance,
    val generationHistory: GenerationHistory,
    val energyFlow: EnergyFlow,
    val systemEfficiency: SystemEfficiency,
    val insights: List<PerformanceInsight>,
    val forecast: ProductionForecast,
    val isSystemActive: Boolean
)
