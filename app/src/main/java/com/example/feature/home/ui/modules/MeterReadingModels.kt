package com.example.feature.home.ui.modules

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class MeterCycleBaseline(
    val cycleId: String,
    val baselineDate: String, // e.g. "01 Sep 2026" or "01/09/2026"
    val baselineReading: Double, // e.g. 12450.0
    val previousBilledUnits: Double? = null, // Historical baseline only (e.g. 302.0)
    val tariffCode: String = "TANGEDCO LT-1A · DOMESTIC",
    val serviceConnectionNumber: String? = null
)

@Serializable
data class MeterReadingRecord(
    val id: String = UUID.randomUUID().toString(),
    val billingCycleId: String,
    val readingValue: Double,
    val readingDate: String, // e.g. "2026-09-30" (YYYY-MM-DD for deterministic daily deduplication)
    val displayDate: String = "", // e.g. "30 Sep 2026"
    val timestamp: Long = System.currentTimeMillis(),
    val dailyConsumption: Double? = null, // Today - previous reading
    val unitsSinceBaseline: Double = 0.0, // Today - baseline reading
    val notes: String? = null
)

@Serializable
data class CurrentCycleEnergyMetrics(
    val billingCycleId: String,
    val baselineReading: Double,
    val baselineDate: String,
    val currentReading: Double,
    val currentReadingDate: String,
    val unitsConsumed: Double, // Current reading - baseline reading (e.g. 132 kWh)
    val daysElapsed: Int,
    val avgUnitsPerDay: Double,
    val estimatedBill: Double, // Calculated from TNEBBillCalculator
    val isTodayLogged: Boolean,
    val todayReading: Double? = null,
    val tariffCategory: String = "TANGEDCO LT-1A · DOMESTIC",
    val billingPeriodDisplay: String = ""
)

enum class MeterGraphRange(val label: String) {
    CURRENT("CURRENT"),
    M2("2M"),
    M4("4M"),
    M6("6M")
}

@Serializable
data class MeterGraphPoint(
    val dateLabel: String, // e.g. "24 Aug", "30 Sep", "Dec", "Jan"
    val readingValue: Double, // Y-axis: meter reading e.g. 42000, 42440
    val unitsConsumed: Double = 0.0, // kWh consumed since baseline
    val dailyUnits: Double? = null,
    val isBaseline: Boolean = false,
    val fullDate: String = "",
    val subLabel: String = "", // e.g. "Last Bill", "Today", "Start", "End"
    val hasReading: Boolean = true // false if monthly timeline guide without recorded reading
)

data class MeterProgressionWindow(
    val range: MeterGraphRange,
    val windowOffset: Int = 0,
    val periodTitle: String,
    val points: List<MeterGraphPoint>,
    val latestReading: Double,
    val hasOlder: Boolean,
    val hasNewer: Boolean,
    val isInsufficientData: Boolean = false
)

@Serializable
data class DailyConsumptionPoint(
    val dateIso: String, // e.g. "2026-09-26"
    val displayDate: String, // e.g. "26 Sep"
    val fullDate: String, // e.g. "26 Sep 2026"
    val dailyKwh: Double, // e.g. 5.0
    val cumulativeReading: Double // e.g. 12455.0
)

sealed class MeterValidationResult {
    data class Success(val reading: MeterReadingRecord) : MeterValidationResult()
    data class Error(val message: String) : MeterValidationResult()
    data class DuplicateToday(val existingReading: MeterReadingRecord) : MeterValidationResult()
}
