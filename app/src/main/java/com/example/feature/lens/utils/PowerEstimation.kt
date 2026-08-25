package com.example.feature.lens.utils

object PowerEstimation {
    fun estimatePowerForAppliance(applianceType: String): Float {
        val lowerCaseType = applianceType.lowercase()
        return when {
            lowerCaseType.contains("air conditioner") || lowerCaseType.contains("ac") -> 1500f
            lowerCaseType.contains("refrigerator") || lowerCaseType.contains("fridge") -> 150f
            lowerCaseType.contains("television") || lowerCaseType.contains("tv") -> 100f
            lowerCaseType.contains("washing machine") -> 500f
            lowerCaseType.contains("ceiling fan") -> 75f
            lowerCaseType.contains("table fan") -> 50f
            lowerCaseType.contains("water heater") || lowerCaseType.contains("geyser") -> 2000f
            lowerCaseType.contains("microwave") -> 1200f
            lowerCaseType.contains("induction") -> 1500f
            lowerCaseType.contains("mixer") || lowerCaseType.contains("grinder") -> 500f
            lowerCaseType.contains("water pump") || lowerCaseType.contains("motor") -> 750f
            lowerCaseType.contains("computer") || lowerCaseType.contains("desktop") -> 200f
            lowerCaseType.contains("laptop") -> 50f
            lowerCaseType.contains("iron") -> 1000f
            lowerCaseType.contains("kettle") -> 1500f
            lowerCaseType.contains("air cooler") || lowerCaseType.contains("cooler") -> 200f
            else -> 0f
        }
    }

    fun getDisplayEstimate(applianceType: String): String {
        val power = estimatePowerForAppliance(applianceType)
        return if (power > 0f) {
            "Est. ~${power.toInt()} W"
        } else {
            "Power Unknown"
        }
    }
}
