package com.example.feature.lens.utils

import com.example.feature.assessment.util.EstimateConstants
import kotlinx.serialization.Serializable

/**
 * Operational mode defining how appliance usage is structured.
 */
@Serializable
enum class UsageMode {
    DAILY_HOURS,
    MINUTES_PER_USE,
    CYCLES_PER_MONTH,
    CONTINUOUS_THERMOSTATIC
}

/**
 * Pure calculation result for an appliance's electrical and energy metrics.
 * Uses Double for high-precision intermediate calculations.
 */
data class ApplianceCalculationResult(
    val connectedLoadKw: Double,
    val monthlyEnergyKwh: Double,
    val simplifiedBaselineCost: Double
)

/**
 * Authoritative appliance specification data class.
 * Separates physical rated capacity (nameplate) from active operating draw and intermittent duty cycle.
 */
data class ApplianceDef(
    val name: String,
    val ratedWattage: Float,
    val activeOperatingWattage: Float,
    val usageMode: UsageMode = UsageMode.DAILY_HOURS,
    val typicalDailyHours: Float = 0f,
    val minutesPerUse: Float = 0f,
    val usesPerMonth: Int = 0,
    val monthlyDays: Int = 30,
    val dutyCycle: Float = 1.0f
) {
    init {
        require(ratedWattage >= 0f) { "ratedWattage must be non-negative: $ratedWattage" }
        require(activeOperatingWattage >= 0f) { "activeOperatingWattage must be non-negative: $activeOperatingWattage" }
        require(typicalDailyHours >= 0f) { "typicalDailyHours must be non-negative: $typicalDailyHours" }
        require(minutesPerUse >= 0f) { "minutesPerUse must be non-negative: $minutesPerUse" }
        require(usesPerMonth >= 0) { "usesPerMonth must be non-negative: $usesPerMonth" }
        require(monthlyDays in 0..31) { "monthlyDays must be in 0..31: $monthlyDays" }
        require(dutyCycle in 0f..1f) { "dutyCycle must be in 0.0..1.0: $dutyCycle" }
    }

    // Backward-compatibility properties
    val defaultWattage: Float get() = ratedWattage
    val typicalUsageHoursPerDay: Float
        get() = if (typicalDailyHours > 0f) typicalDailyHours else (minutesPerUse / 60f)
    val monthlyUsageDays: Int get() = monthlyDays
}

object ApplianceMaster {

    val appliances = listOf(
        ApplianceDef(
            name = "LED Tube Light",
            ratedWattage = 36f,
            activeOperatingWattage = 36f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 6f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Standard Bulb (LED 9W)",
            ratedWattage = 9f,
            activeOperatingWattage = 9f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 6f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Ceiling Fan",
            ratedWattage = 75f,
            activeOperatingWattage = 75f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 12f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Table Fan",
            ratedWattage = 50f,
            activeOperatingWattage = 50f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 8f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Television (LED 43\")",
            ratedWattage = 100f,
            activeOperatingWattage = 80f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 5f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Refrigerator (Single Door)",
            ratedWattage = 150f,
            activeOperatingWattage = 150f,
            usageMode = UsageMode.CONTINUOUS_THERMOSTATIC,
            typicalDailyHours = 24f,
            monthlyDays = 30,
            dutyCycle = 0.30f
        ),
        ApplianceDef(
            name = "Washing Machine",
            ratedWattage = 500f,
            activeOperatingWattage = 500f,
            usageMode = UsageMode.CYCLES_PER_MONTH,
            typicalDailyHours = 1f,
            minutesPerUse = 60f,
            usesPerMonth = 12,
            monthlyDays = 12,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Air Conditioner (1.5T)",
            ratedWattage = 1500f,
            activeOperatingWattage = 1500f,
            usageMode = UsageMode.CONTINUOUS_THERMOSTATIC,
            typicalDailyHours = 6f,
            monthlyDays = 30,
            dutyCycle = 0.60f
        ),
        ApplianceDef(
            name = "Microwave",
            ratedWattage = 1000f,
            activeOperatingWattage = 1000f,
            usageMode = UsageMode.MINUTES_PER_USE,
            typicalDailyHours = 0.25f,
            minutesPerUse = 15f,
            usesPerMonth = 15,
            monthlyDays = 15,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Water Heater / Geyser",
            ratedWattage = 2000f,
            activeOperatingWattage = 2000f,
            usageMode = UsageMode.CYCLES_PER_MONTH,
            typicalDailyHours = 1f,
            minutesPerUse = 60f,
            usesPerMonth = 20,
            monthlyDays = 20,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Laptop",
            ratedWattage = 60f,
            activeOperatingWattage = 30f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 6f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Air Cooler",
            ratedWattage = 200f,
            activeOperatingWattage = 200f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 8f,
            monthlyDays = 30,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Induction Cooktop",
            ratedWattage = 1800f,
            activeOperatingWattage = 1400f,
            usageMode = UsageMode.MINUTES_PER_USE,
            typicalDailyHours = 1f,
            minutesPerUse = 60f,
            usesPerMonth = 20,
            monthlyDays = 20,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Iron Box",
            ratedWattage = 1000f,
            activeOperatingWattage = 1000f,
            usageMode = UsageMode.MINUTES_PER_USE,
            typicalDailyHours = 0.5f,
            minutesPerUse = 30f,
            usesPerMonth = 12,
            monthlyDays = 12,
            dutyCycle = 0.60f
        ),
        ApplianceDef(
            name = "Mixer Grinder",
            ratedWattage = 600f,
            activeOperatingWattage = 600f,
            usageMode = UsageMode.MINUTES_PER_USE,
            typicalDailyHours = 0.25f,
            minutesPerUse = 15f,
            usesPerMonth = 15,
            monthlyDays = 15,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Electric Kettle",
            ratedWattage = 1500f,
            activeOperatingWattage = 1500f,
            usageMode = UsageMode.MINUTES_PER_USE,
            typicalDailyHours = 0.1f,
            minutesPerUse = 6f,
            usesPerMonth = 15,
            monthlyDays = 15,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Water Pump (0.5–1 HP)",
            ratedWattage = 500f,
            activeOperatingWattage = 500f,
            usageMode = UsageMode.CYCLES_PER_MONTH,
            typicalDailyHours = 1f,
            minutesPerUse = 60f,
            usesPerMonth = 15,
            monthlyDays = 15,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Printer",
            ratedWattage = 50f,
            activeOperatingWattage = 50f,
            usageMode = UsageMode.MINUTES_PER_USE,
            typicalDailyHours = 0.5f,
            minutesPerUse = 30f,
            usesPerMonth = 10,
            monthlyDays = 10,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Dishwasher",
            ratedWattage = 1500f,
            activeOperatingWattage = 1500f,
            usageMode = UsageMode.CYCLES_PER_MONTH,
            typicalDailyHours = 1f,
            minutesPerUse = 60f,
            usesPerMonth = 10,
            monthlyDays = 10,
            dutyCycle = 1.0f
        ),
        ApplianceDef(
            name = "Other / Custom Appliance",
            ratedWattage = 0f,
            activeOperatingWattage = 0f,
            usageMode = UsageMode.DAILY_HOURS,
            typicalDailyHours = 0f,
            minutesPerUse = 0f,
            usesPerMonth = 0,
            monthlyDays = 30,
            dutyCycle = 1.0f
        )
    )

    /**
     * Get full defaults for an appliance by name.
     * Tries exact match first, then partial match for AI detection label mapping.
     */
    fun getDefaults(name: String): ApplianceDef? {
        val exactMatch = appliances.find { it.name.equals(name, ignoreCase = true) }
        if (exactMatch != null) return exactMatch

        return appliances.find {
            it.name.lowercase().contains(name.lowercase()) ||
            name.lowercase().contains(it.name.lowercase())
        }
    }

    fun getDefaultWattage(name: String): Float {
        return getDefaults(name)?.ratedWattage ?: 100f
    }

    /**
     * Pure calculation for connected load in kW.
     * Connected Load = (ratedWattage * quantity) / 1000
     */
    fun calculateConnectedLoadKw(ratedWattage: Float, quantity: Int): Double {
        require(ratedWattage >= 0f) { "ratedWattage must be non-negative: $ratedWattage" }
        require(quantity >= 0) { "quantity must be non-negative: $quantity" }
        return (ratedWattage.toDouble() * quantity.toDouble()) / 1000.0
    }

    fun calculateConnectedLoadKw(appliance: ApplianceDef, quantity: Int): Double {
        return calculateConnectedLoadKw(appliance.ratedWattage, quantity)
    }

    /**
     * Pure calculation for monthly energy consumption in kWh.
     * Supports DAILY_HOURS, MINUTES_PER_USE, CYCLES_PER_MONTH, and CONTINUOUS_THERMOSTATIC.
     */
    fun calculateMonthlyEnergyKwh(
        activeOperatingWattage: Float,
        usageMode: UsageMode = UsageMode.DAILY_HOURS,
        dailyHours: Double = 0.0,
        minutesPerUse: Double = 0.0,
        usesPerMonth: Int = 0,
        monthlyDays: Int = 30,
        quantity: Int = 1,
        dutyCycle: Float = 1.0f
    ): Double {
        require(activeOperatingWattage >= 0f) { "activeOperatingWattage must be non-negative: $activeOperatingWattage" }
        require(quantity >= 0) { "quantity must be non-negative: $quantity" }
        require(dutyCycle in 0f..1f) { "dutyCycle must be between 0.0 and 1.0: $dutyCycle" }
        require(monthlyDays in 0..31) { "monthlyDays must be in 0..31: $monthlyDays" }
        require(dailyHours >= 0.0) { "dailyHours must be non-negative: $dailyHours" }
        require(minutesPerUse >= 0.0) { "minutesPerUse must be non-negative: $minutesPerUse" }
        require(usesPerMonth >= 0) { "usesPerMonth must be non-negative: $usesPerMonth" }

        if (quantity == 0 || activeOperatingWattage == 0f || dutyCycle == 0f) return 0.0

        val effectiveMonthlyHours: Double = when (usageMode) {
            UsageMode.CYCLES_PER_MONTH -> {
                val hoursPerCycle = if (dailyHours > 0.0) dailyHours else (minutesPerUse / 60.0).coerceAtLeast(1.0)
                val cycles = if (usesPerMonth > 0) usesPerMonth.toDouble() else monthlyDays.toDouble()
                hoursPerCycle * cycles
            }
            UsageMode.MINUTES_PER_USE -> {
                val hoursPerUse = minutesPerUse / 60.0
                val uses = if (usesPerMonth > 0) usesPerMonth.toDouble() else monthlyDays.toDouble()
                hoursPerUse * uses
            }
            UsageMode.CONTINUOUS_THERMOSTATIC,
            UsageMode.DAILY_HOURS -> {
                dailyHours * monthlyDays.toDouble()
            }
        }

        return (activeOperatingWattage.toDouble() * effectiveMonthlyHours * quantity.toDouble() * dutyCycle.toDouble()) / 1000.0
    }

    fun calculateMonthlyEnergyKwh(
        appliance: ApplianceDef,
        quantity: Int,
        dailyHours: Double = appliance.typicalDailyHours.toDouble(),
        monthlyDays: Int = appliance.monthlyDays,
        dutyCycle: Float = appliance.dutyCycle
    ): Double {
        return calculateMonthlyEnergyKwh(
            activeOperatingWattage = appliance.activeOperatingWattage,
            usageMode = appliance.usageMode,
            dailyHours = dailyHours,
            minutesPerUse = appliance.minutesPerUse.toDouble(),
            usesPerMonth = appliance.usesPerMonth,
            monthlyDays = monthlyDays,
            quantity = quantity,
            dutyCycle = dutyCycle
        )
    }

    /**
     * Pure function returning comprehensive calculation metrics for an appliance.
     */
    fun calculateApplianceResult(
        appliance: ApplianceDef,
        quantity: Int,
        tariffPerKwh: Double = authoritativeTariffRate
    ): ApplianceCalculationResult {
        val connectedLoadKw = calculateConnectedLoadKw(appliance, quantity)
        val monthlyEnergyKwh = calculateMonthlyEnergyKwh(appliance, quantity)
        return ApplianceCalculationResult(
            connectedLoadKw = connectedLoadKw,
            monthlyEnergyKwh = monthlyEnergyKwh,
            simplifiedBaselineCost = monthlyEnergyKwh * tariffPerKwh
        )
    }

    /**
     * Backward-compatible helper for legacy callers.
     */
    fun calculateMonthlyKwh(
        powerWatts: Float,
        dailyUsageHours: Float,
        monthlyUsageDays: Int,
        quantity: Int = 1
    ): Float {
        val def = appliances.find { it.ratedWattage == powerWatts }
        val activeWatts = def?.activeOperatingWattage ?: powerWatts
        val duty = def?.dutyCycle ?: 1.0f
        return calculateMonthlyEnergyKwh(
            activeOperatingWattage = activeWatts,
            usageMode = def?.usageMode ?: UsageMode.DAILY_HOURS,
            dailyHours = dailyUsageHours.toDouble(),
            minutesPerUse = def?.minutesPerUse?.toDouble() ?: 0.0,
            usesPerMonth = def?.usesPerMonth ?: 0,
            monthlyDays = monthlyUsageDays,
            quantity = quantity,
            dutyCycle = duty
        ).toFloat()
    }

    /**
     * Backward-compatible helper for legacy cost calculation.
     */
    fun calculateMonthlyCost(monthlyKwh: Float): Float {
        return (monthlyKwh.toDouble() * authoritativeTariffRate).toFloat()
    }

    /**
     * Authoritative baseline tariff rate in ₹ per kWh.
     */
    val authoritativeTariffRate: Double
        get() = EstimateConstants.TARIFF_PER_KWH
}
