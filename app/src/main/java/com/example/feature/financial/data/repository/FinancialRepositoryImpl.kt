package com.example.feature.financial.data.repository

import com.example.feature.financial.domain.model.*
import com.example.feature.financial.domain.repository.FinancialRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FinancialRepositoryImpl : FinancialRepository {
    override fun getFinancialDashboard(): Flow<Result<FinancialDashboard>> = flow {
        delay(1500)
        
        val mockData = FinancialDashboard(
            isSystemActive = true,
            investmentSummary = InvestmentSummary(
                totalInvestment = 250000.0,
                governmentSubsidy = 78000.0,
                netAmountPaid = 172000.0,
                estimatedAnnualSavings = 42500.0,
                estimatedMonthlySavings = 3541.0,
                estimatedDailySavings = 116.0
            ),
            savingsSummary = SavingsSummary(
                todaySavings = 118.5,
                currentMonthSavings = 3420.0,
                currentYearSavings = 41200.0,
                lifetimeSavings = 85400.0
            ),
            billComparison = BillComparison(
                beforeSolarMonthlyBill = 4500.0,
                afterSolarMonthlyBill = 959.0,
                monthlyReduction = 3541.0,
                annualReduction = 42492.0,
                percentageReduction = 78.6
            ),
            roiSummary = RoiSummary(
                estimatedPaybackPeriodYears = 4.0,
                investmentRecovered = 85400.0,
                remainingAmount = 86600.0, // 172000 - 85400
                roiPercentage = 49.6
            ),
            projectedSavings25Years = Projectedsavings(
                projectedSavings25Years = 1062500.0,
                electricityCostAvoided = 1450000.0,
                netProfit = 890500.0
            ),
            savingsHistory = SavingsHistory(
                monthly = listOf(
                    ChartDataPoint("Jan", 3200.0, 4200.0),
                    ChartDataPoint("Feb", 3450.0, 4300.0),
                    ChartDataPoint("Mar", 3600.0, 4500.0),
                    ChartDataPoint("Apr", 3800.0, 4800.0),
                    ChartDataPoint("May", 3750.0, 4700.0),
                    ChartDataPoint("Jun", 3500.0, 4400.0)
                ),
                quarterly = listOf(
                    ChartDataPoint("Q1", 10250.0, 13000.0),
                    ChartDataPoint("Q2", 11050.0, 13900.0)
                ),
                yearly = listOf(
                    ChartDataPoint("2022", 38500.0, 52000.0),
                    ChartDataPoint("2023", 41200.0, 55000.0)
                )
            ),
            insights = listOf(
                FinancialInsight("You have saved ₹3,420 this month.", "positive"),
                FinancialInsight("Your electricity bill has reduced by 78%.", "positive"),
                FinancialInsight("Your investment recovery is 49.6% complete.", "positive"),
                FinancialInsight("You are on track to recover your investment in approximately 4.0 years.", "positive")
            ),
            subsidyInformation = SubsidyInformation(
                status = "Approved",
                amountReceived = 78000.0,
                applicationStatus = "Completed",
                disbursementDate = "15-Aug-2023"
            ),
            recommendations = listOf(
                FinancialInsight("Cleaning your panels could improve savings by 2-5%.", "neutral"),
                FinancialInsight("Higher summer production expected next month.", "positive"),
                FinancialInsight("Your system is performing within the expected financial range.", "positive")
            ),
            milestones = MilestonesData(
                achieved = SavingsMilestone(
                    amountSaved = 75000.0,
                    title = "You have saved ₹75,000!",
                    equivalents = listOf(
                        "15 months of electricity bills",
                        "1 premium smartphone",
                        "1 new laptop"
                    )
                ),
                next = NextMilestone(
                    targetAmount = 100000.0,
                    progressPercentage = 85.4
                )
            )
        )
        emit(Result.success(mockData))
    }
}
