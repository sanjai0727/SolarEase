package com.example.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BaselineRequestDto(
    @Json(name = "cycle_id") val cycleId: String,
    @Json(name = "baseline_date") val baselineDate: String,
    @Json(name = "baseline_reading") val baselineReading: Double,
    @Json(name = "previous_billed_units") val previousBilledUnits: Double? = null,
    @Json(name = "tariff_code") val tariffCode: String? = "LT-1A",
    @Json(name = "service_connection_number") val serviceConnectionNumber: String? = null
)

@JsonClass(generateAdapter = true)
data class BaselineResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "cycle_id") val cycleId: String,
    @Json(name = "baseline_date") val baselineDate: String,
    @Json(name = "baseline_reading") val baselineReading: Double,
    @Json(name = "previous_billed_units") val previousBilledUnits: Double? = null,
    @Json(name = "tariff_code") val tariffCode: String? = null,
    @Json(name = "service_connection_number") val serviceConnectionNumber: String? = null,
    @Json(name = "is_active") val isActive: Boolean
)

@JsonClass(generateAdapter = true)
data class MeterReadingCreateDto(
    @Json(name = "reading_value") val readingValue: Double,
    @Json(name = "reading_date") val readingDate: String? = null,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class MeterReadingUpdateDto(
    @Json(name = "reading_value") val readingValue: Double,
    @Json(name = "notes") val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class MeterReadingResponseDto(
    @Json(name = "id") val id: String,
    @Json(name = "billing_cycle_id") val billingCycleId: String,
    @Json(name = "reading_value") val readingValue: Double,
    @Json(name = "reading_date") val readingDate: String,
    @Json(name = "daily_consumption") val dailyConsumption: Double? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class CurrentCycleConsumptionDto(
    @Json(name = "billing_cycle_id") val billingCycleId: String,
    @Json(name = "baseline_reading") val baselineReading: Double,
    @Json(name = "baseline_date") val baselineDate: String,
    @Json(name = "latest_reading") val latestReading: Double? = null,
    @Json(name = "latest_reading_date") val latestReadingDate: String? = null,
    @Json(name = "units_consumed") val unitsConsumed: Double,
    @Json(name = "days_elapsed") val daysElapsed: Int,
    @Json(name = "avg_units_per_day") val avgUnitsPerDay: Double,
    @Json(name = "estimated_bill") val estimatedBill: Double,
    @Json(name = "is_today_logged") val isTodayLogged: Boolean,
    @Json(name = "today_reading") val todayReading: Double? = null,
    @Json(name = "tariff_code") val tariffCode: String? = null
)

@JsonClass(generateAdapter = true)
data class GraphPointDto(
    @Json(name = "date") val date: String,
    @Json(name = "reading_value") val readingValue: Double,
    @Json(name = "units_consumed") val unitsConsumed: Double,
    @Json(name = "daily_units") val dailyUnits: Double? = null,
    @Json(name = "is_baseline") val isBaseline: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GraphResponseDto(
    @Json(name = "billing_cycle_id") val billingCycleId: String,
    @Json(name = "baseline_reading") val baselineReading: Double,
    @Json(name = "baseline_date") val baselineDate: String,
    @Json(name = "points") val points: List<GraphPointDto>,
    @Json(name = "total_points") val totalPoints: Int
)
