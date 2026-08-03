package com.example.feature.environmental.domain.model

data class EnvironmentalSummary(
    val co2SavedTons: Double,
    val treesEquivalent: Int,
    val greenEnergyKwh: Double,
    val impactScore: String
)

data class TodayImpact(
    val greenEnergyKwh: Double,
    val co2ReductionKg: Double,
    val treesEquivalent: Double,
    val carbonOffsetKg: Double
)

data class LifetimeImpact(
    val greenEnergyMwh: Double,
    val co2ReductionTons: Double,
    val treesEquivalent: Int,
    val coalOffsetKg: Double,
    val fuelSavedLiters: Double
)

data class EnvironmentalComparison(
    val treesPlanted: Int,
    val carsRemoved: Double,
    val coalNotBurnedKg: Double,
    val petrolSavedLiters: Double,
    val homesPowered: Double
)

data class ChartDataPoint(
    val label: String,
    val value: Double
)

data class ImpactHistory(
    val monthlyCo2: List<ChartDataPoint>,
    val monthlyEnergy: List<ChartDataPoint>,
    val yearlyCo2: List<ChartDataPoint>,
    val yearlyEnergy: List<ChartDataPoint>,
    val lifetimeCo2: List<ChartDataPoint>,
    val lifetimeEnergy: List<ChartDataPoint>
)

data class EnvironmentalMilestone(
    val title: String,
    val isCompleted: Boolean,
    val progressValue: Double? = null,
    val targetValue: Double? = null,
    val currentProgressPercentage: Float? = null,
    val unit: String = ""
)

data class EnvironmentalAchievement(
    val title: String,
    val unlockedDate: String,
    val description: String,
    val iconId: String // Can be used to select specific icon
)

data class EnvironmentalInsight(
    val message: String
)

data class ImpactTimelineEvent(
    val title: String,
    val date: String?,
    val isCompleted: Boolean,
    val description: String? = null
)

data class ImpactTimeline(
    val events: List<ImpactTimelineEvent>,
    val nextMilestone: String,
    val nextMilestoneProgress: Float
)

data class EnvironmentalDashboard(
    val isSystemActive: Boolean,
    val overview: EnvironmentalSummary,
    val today: TodayImpact,
    val lifetime: LifetimeImpact,
    val comparison: EnvironmentalComparison,
    val history: ImpactHistory,
    val sustainabilityGoals: List<EnvironmentalMilestone>,
    val achievements: List<EnvironmentalAchievement>,
    val insights: List<EnvironmentalInsight>,
    val timeline: ImpactTimeline
)
