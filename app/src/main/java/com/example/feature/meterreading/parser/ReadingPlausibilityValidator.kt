package com.example.feature.meterreading.parser

import com.example.feature.home.ui.modules.MeterCycleBaseline
import com.example.feature.home.ui.modules.MeterReadingRecord
import java.util.Locale

sealed class ReadingPlausibilityStatus {
    data object Valid : ReadingPlausibilityStatus()
    data class Warning(val message: String) : ReadingPlausibilityStatus()
    data class LowerThanPrevious(
        val enteredReading: Double,
        val previousReading: Double,
        val message: String
    ) : ReadingPlausibilityStatus()
}

/**
 * Checks plausible continuity between an extracted meter reading and historical records.
 */
object ReadingPlausibilityValidator {

    /**
     * Validates a candidate reading value against the existing baseline and latest recorded reading.
     */
    fun validate(
        candidateValue: Double,
        baseline: MeterCycleBaseline?,
        latestReading: MeterReadingRecord?
    ): ReadingPlausibilityStatus {
        val referenceReading = latestReading?.readingValue ?: baseline?.baselineReading

        if (referenceReading != null && referenceReading > 0.0) {
            // Check if reading is strictly lower than previous reading
            if (candidateValue < referenceReading) {
                // If candidate is between 210 and 260 V and reference reading is much larger, it's almost certainly voltage
                if (candidateValue in 200.0..260.0 && referenceReading > 500.0) {
                    return ReadingPlausibilityStatus.Warning(
                        "The reading (${String.format(Locale.getDefault(), "%,.1f", candidateValue)}) appears to be your home's AC Voltage (230 V). Please capture when 'kWh' is displayed on your meter."
                    )
                }

                return ReadingPlausibilityStatus.LowerThanPrevious(
                    enteredReading = candidateValue,
                    previousReading = referenceReading,
                    message = "The entered reading (${String.format(Locale.getDefault(), "%,.0f", candidateValue)} kWh) is lower than your previously recorded reading (${String.format(Locale.getDefault(), "%,.0f", referenceReading)} kWh)."
                )
            }

            // Check for unrealistic jumps (e.g. +10,000 kWh within a single billing cycle)
            val delta = candidateValue - referenceReading
            if (delta > 15000.0) {
                return ReadingPlausibilityStatus.Warning(
                    "Large increase detected (+${String.format(Locale.getDefault(), "%,.0f", delta)} kWh). Please double check the digits on your meter."
                )
            }
        }

        return ReadingPlausibilityStatus.Valid
    }
}
