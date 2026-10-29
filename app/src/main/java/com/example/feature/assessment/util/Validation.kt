package com.example.feature.assessment.util

/**
 * Validates consumer input metrics for solar site assessment and feasibility calculations.
 */
object AssessmentValidator {
    fun isValidConsumption(consumption: String): Boolean {
        val value = consumption.toDoubleOrNull() ?: return false
        return value > 0 && consumption.length <= 5
    }

    fun isValidArea(area: String): Boolean {
        val value = area.toDoubleOrNull() ?: return false
        return value > 0 && value <= 100000.0
    }
}

/**
 * Backward-compatible alias for existing callers.
 */
@Deprecated("Use AssessmentValidator instead", ReplaceWith("AssessmentValidator"))
typealias Validation = AssessmentValidator
