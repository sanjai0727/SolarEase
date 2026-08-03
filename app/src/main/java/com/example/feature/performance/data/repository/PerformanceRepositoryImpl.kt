package com.example.feature.performance.data.repository

import com.example.feature.performance.domain.model.*
import com.example.feature.performance.domain.repository.PerformanceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PerformanceRepositoryImpl : PerformanceRepository {
    override fun getPerformanceDashboard(): Flow<Result<PerformanceDashboard>> = flow {
        delay(1500)
        
        val currentTime = System.currentTimeMillis()
        
        val mockData = PerformanceDashboard(
            isSystemActive = true,
            liveGeneration = LiveGeneration(
                currentOutput = 2.8,
                todayGeneration = 18.5,
                systemStatus = "Generating",
                gridStatus = "Connected",
                lastUpdated = currentTime - 60000,
                weather = "Sunny",
                hasLiveMonitoring = true
            ),
            todayPerformance = TodayPerformance(
                energyGenerated = 18.5,
                peakPower = 4.2,
                operatingHours = 8.5,
                averageEfficiency = 94.0,
                comparedToYesterday = 12.0
            ),
            monthPerformance = MonthPerformance(
                monthlyGeneration = 450.5,
                monthlySavings = 3450.0,
                averageDailyGeneration = 18.0,
                peakProductionDay = "12th July",
                bestPerformingWeek = "Week 2"
            ),
            yearPerformance = YearPerformance(
                annualGeneration = 5400.0,
                annualSavings = 42500.0,
                capacityUtilization = 18.5,
                totalRuntime = 2150,
                expectedGeneration = 5500.0,
                actualGeneration = 5400.0
            ),
            generationHistory = GenerationHistory(
                daily = listOf(
                    ChartDataPoint("6 AM", 0.2),
                    ChartDataPoint("9 AM", 1.5),
                    ChartDataPoint("12 PM", 3.8),
                    ChartDataPoint("3 PM", 2.9),
                    ChartDataPoint("6 PM", 0.5)
                ),
                weekly = listOf(
                    ChartDataPoint("Mon", 18.2),
                    ChartDataPoint("Tue", 17.5),
                    ChartDataPoint("Wed", 19.1),
                    ChartDataPoint("Thu", 16.8),
                    ChartDataPoint("Fri", 18.5),
                    ChartDataPoint("Sat", 20.2),
                    ChartDataPoint("Sun", 19.5)
                ),
                monthly = listOf(
                    ChartDataPoint("Week 1", 120.5),
                    ChartDataPoint("Week 2", 135.2),
                    ChartDataPoint("Week 3", 115.8),
                    ChartDataPoint("Week 4", 79.0)
                ),
                yearly = listOf(
                    ChartDataPoint("Jan", 420.0),
                    ChartDataPoint("Feb", 450.0),
                    ChartDataPoint("Mar", 510.0),
                    ChartDataPoint("Apr", 490.0),
                    ChartDataPoint("May", 480.0),
                    ChartDataPoint("Jun", 460.0),
                    ChartDataPoint("Jul", 450.5)
                )
            ),
            energyFlow = EnergyFlow(
                solarGenerated = 18.5,
                consumed = 12.0,
                exportedToGrid = 6.5,
                importedFromGrid = 2.0,
                batteryStatus = "Not Installed"
            ),
            systemEfficiency = SystemEfficiency(
                performanceRatio = 78.5,
                panelEfficiency = 21.0,
                inverterEfficiency = 97.5,
                temperatureEffectLoss = 4.2,
                shadingLoss = 1.5,
                dustLoss = 2.0
            ),
            insights = listOf(
                PerformanceInsight("Today's production increased by 12%.", "positive"),
                PerformanceInsight("Generation was lower due to cloudy weather yesterday.", "neutral"),
                PerformanceInsight("Peak generation occurred at 12:40 PM.", "positive"),
                PerformanceInsight("Monthly target achieved.", "positive")
            ),
            forecast = ProductionForecast(
                expectedGeneration = 19.2,
                expectedSavings = 150.0,
                weatherImpact = "Clear skies expected",
                confidence = "High"
            )
        )
        emit(Result.success(mockData))
    }
}
