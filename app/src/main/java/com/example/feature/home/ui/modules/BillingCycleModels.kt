package com.example.feature.home.ui.modules

import kotlinx.serialization.Serializable

enum class BillingCycleStatus {
    COMPLETED,
    CURRENT
}

enum class BillingCycleSource {
    BILL,           // From TNEB bill data
    DAILY_READINGS, // Calculated from user daily meter readings
    ESTIMATED       // Baseline / demo fallback
}

enum class BillingComparisonState {
    CURRENT_HIGHER,       // Active cycle, accumulated consumption >= previous cycle consumption
    CURRENT_LOWER,        // Active cycle, accumulated consumption < previous cycle consumption (in progress)
    CURRENT_NO_READINGS,  // Active cycle, opening reading set but no daily readings recorded yet
    COMPLETED_INCREASED,  // Completed cycle, consumption increased vs previous cycle
    COMPLETED_SAVED,      // Completed cycle, consumption decreased (bill-verified savings)
    COMPLETED_NO_CHANGE;  // Completed cycle, equal consumption

    companion object {
        val CURRENT_HIGHER_RATE get() = CURRENT_HIGHER
        val CURRENT_LOWER_RATE get() = CURRENT_LOWER
        val CURRENT_STEADY_RATE get() = CURRENT_LOWER
    }
}

@Serializable
data class BillingCycleItem(
    val id: String,
    val periodLabel: String,         // e.g. "Feb–Apr", "Apr–Jun", "Jun–Aug", "Aug–Oct"
    val periodStartDate: String = "",
    val periodEndDate: String = "",
    val consumptionKwh: Double,
    val durationDays: Int,           // Total days in cycle or elapsed days so far
    val dailyRate: Double,           // consumptionKwh / durationDays
    val source: BillingCycleSource,
    val status: BillingCycleStatus,
    val openingMeterReading: Double? = null,
    val latestMeterReading: Double? = null,
    val hasMeterReadings: Boolean = true
)

data class BillingCycleComparison(
    val selectedCycle: BillingCycleItem,
    val previousCycle: BillingCycleItem?,
    val state: BillingComparisonState,
    val differenceUnits: Double,     // Positive delta in kWh
    val savedUnits: Double,          // Only > 0 for COMPLETED_SAVED
    val savedCredits: Int,           // Only > 0 for COMPLETED_SAVED
    val primaryMessage: String,      // e.g. "Current usage rate is lower", "65 kWh Saved", "+68 kWh"
    val secondaryMessage: String,    // e.g. "42 kWh so far • Based on daily readings"
    val comparisonNote: String,      // e.g. "Compared with previous cycle (Jun–Aug)"
    val rateComparisonText: String = "", // e.g. "1.40 kWh/day vs 0.92 kWh/day"
    val isConsumptionIncreased: Boolean = false
)
