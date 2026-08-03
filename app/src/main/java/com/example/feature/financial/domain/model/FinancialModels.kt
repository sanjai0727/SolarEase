package com.example.feature.financial.domain.model

data class InvestmentSummary(
    val totalInvestment: Double,
    val governmentSubsidy: Double,
    val netAmountPaid: Double,
    val estimatedAnnualSavings: Double,
    val estimatedMonthlySavings: Double,
    val estimatedDailySavings: Double
)

data class SavingsSummary(
    val todaySavings: Double,
    val currentMonthSavings: Double,
    val currentYearSavings: Double,
    val lifetimeSavings: Double
)

data class BillComparison(
    val beforeSolarMonthlyBill: Double,
    val afterSolarMonthlyBill: Double,
    val monthlyReduction: Double,
    val annualReduction: Double,
    val percentageReduction: Double
)

data class RoiSummary(
    val estimatedPaybackPeriodYears: Double,
    val investmentRecovered: Double,
    val remainingAmount: Double,
    val roiPercentage: Double
)

data class Projectedsavings(
    val projectedSavings25Years: Double,
    val electricityCostAvoided: Double,
    val netProfit: Double
)

data class FinancialInsight(
    val message: String,
    val type: String // "positive", "neutral", "warning"
)

data class SubsidyInformation(
    val status: String,
    val amountReceived: Double,
    val applicationStatus: String,
    val disbursementDate: String,
    val hasApplied: Boolean = true
)

data class ChartDataPoint(
    val label: String,
    val savings: Double,
    val estimatedBill: Double,
    val actualBill: Double? = null
)

data class SavingsHistory(
    val monthly: List<ChartDataPoint>,
    val quarterly: List<ChartDataPoint>,
    val yearly: List<ChartDataPoint>
)

data class SavingsMilestone(
    val amountSaved: Double,
    val title: String,
    val equivalents: List<String>
)

data class NextMilestone(
    val targetAmount: Double,
    val progressPercentage: Double
)

data class MilestonesData(
    val achieved: SavingsMilestone?,
    val next: NextMilestone
)

data class FinancialDashboard(
    val isSystemActive: Boolean,
    val investmentSummary: InvestmentSummary,
    val savingsSummary: SavingsSummary,
    val billComparison: BillComparison,
    val roiSummary: RoiSummary,
    val projectedSavings25Years: Projectedsavings,
    val savingsHistory: SavingsHistory,
    val insights: List<FinancialInsight>,
    val subsidyInformation: SubsidyInformation,
    val recommendations: List<FinancialInsight>,
    val milestones: MilestonesData
)
