package com.example.feature.lens.utils

object ApplianceMaster {
    
    data class ApplianceDef(
        val name: String,
        val defaultWattage: Float,
        val typicalUsageHoursPerDay: Float,
        val monthlyUsageDays: Int
    )

    val appliances = listOf(
        ApplianceDef("LED Tube Light", 36f, 6f, 30),
        ApplianceDef("Standard Bulb (LED 9W)", 9f, 6f, 30),
        ApplianceDef("Ceiling Fan", 75f, 12f, 30),
        ApplianceDef("Table Fan", 50f, 8f, 30),
        ApplianceDef("Television (LED 43\")", 100f, 5f, 30),
        ApplianceDef("Refrigerator (Single Door)", 150f, 24f, 30),
        ApplianceDef("Washing Machine", 500f, 1f, 12),
        ApplianceDef("Air Conditioner (1.5T)", 1500f, 6f, 30),
        ApplianceDef("Microwave", 1000f, 0.25f, 15),
        ApplianceDef("Water Heater / Geyser", 2000f, 1f, 20),
        ApplianceDef("Laptop", 60f, 6f, 30),
        ApplianceDef("Air Cooler", 200f, 8f, 30),
        ApplianceDef("Induction Cooktop", 1800f, 1f, 20),
        ApplianceDef("Iron Box", 1000f, 0.5f, 12),
        ApplianceDef("Mixer Grinder", 600f, 0.25f, 15),
        ApplianceDef("Electric Kettle", 1500f, 0.1f, 15),
        ApplianceDef("Water Pump (0.5–1 HP)", 500f, 1f, 15),
        ApplianceDef("Printer", 50f, 0.5f, 10),
        ApplianceDef("Dishwasher", 1500f, 1f, 10),
        ApplianceDef("Other / Custom Appliance", 0f, 0f, 30)
    )

    /**
     * Get full defaults for an appliance by name.
     * Tries exact match first, then partial match for AI detection label mapping.
     */
    fun getDefaults(name: String): ApplianceDef? {
        // Try exact match first
        val exactMatch = appliances.find { it.name.equals(name, ignoreCase = true) }
        if (exactMatch != null) return exactMatch
        
        // Try partial match to handle AI detection labels mapping
        return appliances.find { 
            it.name.lowercase().contains(name.lowercase()) || 
            name.lowercase().contains(it.name.lowercase()) 
        }
    }

    fun getDefaultWattage(name: String): Float {
        return getDefaults(name)?.defaultWattage ?: 100f
    }
}
