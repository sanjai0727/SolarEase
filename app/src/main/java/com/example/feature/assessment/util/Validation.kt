package com.example.feature.assessment.util

object Validation {
    fun isValidConsumption(consumption: String): Boolean {
        val value = consumption.toDoubleOrNull() ?: return false
        return value > 0 && consumption.length <= 5
    }

    fun isValidArea(area: String): Boolean {
        val value = area.toDoubleOrNull() ?: return false
        return value > 0 && value <= 100000.0
    }
}
