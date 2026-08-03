package com.example.feature.environmental.data.repository

import com.example.feature.environmental.domain.model.*
import com.example.feature.environmental.domain.repository.EnvironmentalRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EnvironmentalRepositoryImpl : EnvironmentalRepository {
    override fun getEnvironmentalDashboard(): Flow<Result<EnvironmentalDashboard>> = flow {
        delay(1000)
        
        val mockData = EnvironmentalDashboard(
            isSystemActive = true,
            overview = EnvironmentalSummary(
                co2SavedTons = 8.6,
                treesEquivalent = 392,
                greenEnergyKwh = 12450.0,
                impactScore = "Excellent"
            ),
            today = TodayImpact(
                greenEnergyKwh = 24.5,
                co2ReductionKg = 18.2,
                treesEquivalent = 0.8,
                carbonOffsetKg = 15.0
            ),
            lifetime = LifetimeImpact(
                greenEnergyMwh = 12.45,
                co2ReductionTons = 8.6,
                treesEquivalent = 392,
                coalOffsetKg = 6400.0,
                fuelSavedLiters = 3200.0
            ),
            comparison = EnvironmentalComparison(
                treesPlanted = 392,
                carsRemoved = 1.8,
                coalNotBurnedKg = 6400.0,
                petrolSavedLiters = 3200.0,
                homesPowered = 1.4 // For a year
            ),
            history = ImpactHistory(
                monthlyCo2 = listOf(
                    ChartDataPoint("Jan", 0.5), ChartDataPoint("Feb", 0.6), ChartDataPoint("Mar", 0.8),
                    ChartDataPoint("Apr", 0.9), ChartDataPoint("May", 1.0), ChartDataPoint("Jun", 0.9)
                ),
                monthlyEnergy = listOf(
                    ChartDataPoint("Jan", 400.0), ChartDataPoint("Feb", 450.0), ChartDataPoint("Mar", 520.0),
                    ChartDataPoint("Apr", 600.0), ChartDataPoint("May", 650.0), ChartDataPoint("Jun", 610.0)
                ),
                yearlyCo2 = listOf(
                    ChartDataPoint("2021", 2.1), ChartDataPoint("2022", 3.0), ChartDataPoint("2023", 3.5)
                ),
                yearlyEnergy = listOf(
                    ChartDataPoint("2021", 2500.0), ChartDataPoint("2022", 4000.0), ChartDataPoint("2023", 5950.0)
                ),
                lifetimeCo2 = listOf(),
                lifetimeEnergy = listOf()
            ),
            sustainabilityGoals = listOf(
                EnvironmentalMilestone("1 Ton CO₂ Saved", true),
                EnvironmentalMilestone("5 Tons CO₂ Saved", true),
                EnvironmentalMilestone("10 Tons CO₂ Saved", false, 8.6, 10.0, 0.86f, "Tons"),
                EnvironmentalMilestone("500 Trees Equivalent", false, 392.0, 500.0, 0.784f, "Trees"),
                EnvironmentalMilestone("10 MWh Generated", true)
            ),
            achievements = listOf(
                EnvironmentalAchievement("Green Starter", "12 Jan 2021", "System activated and generating clean energy.", "starter"),
                EnvironmentalAchievement("Solar Champion", "24 Mar 2022", "Generated enough energy to power a home for a month.", "champion"),
                EnvironmentalAchievement("Climate Protector", "05 Nov 2022", "Prevented 5 tons of CO2 emissions.", "protector")
            ),
            insights = listOf(
                EnvironmentalInsight("Your solar system prevented approximately 25 kg of CO₂ emissions this week."),
                EnvironmentalInsight("Your clean energy generation is enough to power an average home for several days."),
                EnvironmentalInsight("You are contributing to a cleaner future every day.")
            ),
            timeline = ImpactTimeline(
                events = listOf(
                    ImpactTimelineEvent("Installation Completed", "10 Jan 2021", true),
                    ImpactTimelineEvent("First Unit Generated", "12 Jan 2021", true),
                    ImpactTimelineEvent("100 kWh Milestone", "25 Jan 2021", true),
                    ImpactTimelineEvent("1 Ton CO₂ Saved", "15 Aug 2021", true),
                    ImpactTimelineEvent("First Year Completed", "12 Jan 2022", true)
                ),
                nextMilestone = "10 Tons CO₂ Saved",
                nextMilestoneProgress = 0.86f
            )
        )
        emit(Result.success(mockData))
    }
}
