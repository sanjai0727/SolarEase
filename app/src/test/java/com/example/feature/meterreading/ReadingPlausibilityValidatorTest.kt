package com.example.feature.meterreading

import com.example.feature.home.ui.modules.MeterCycleBaseline
import com.example.feature.home.ui.modules.MeterReadingRecord
import com.example.feature.meterreading.parser.ReadingPlausibilityStatus
import com.example.feature.meterreading.parser.ReadingPlausibilityValidator
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingPlausibilityValidatorTest {

    private val baseline = MeterCycleBaseline(
        cycleId = "CYCLE-1",
        baselineDate = "01 Sep 2026",
        baselineReading = 12450.0
    )

    private val latestRecord = MeterReadingRecord(
        readingValue = 12490.0,
        readingDate = "2026-09-08"
    )

    @Test
    fun validate_normalIncrementalReading_returnsValid() {
        val result = ReadingPlausibilityValidator.validate(12510.0, baseline, latestRecord)
        assertTrue(result is ReadingPlausibilityStatus.Valid)
    }

    @Test
    fun validate_readingEqualToBaseline_returnsValid() {
        val result = ReadingPlausibilityValidator.validate(12450.0, baseline, null)
        assertTrue(result is ReadingPlausibilityStatus.Valid)
    }

    @Test
    fun validate_readingLowerThanPrevious_returnsLowerThanPrevious() {
        val result = ReadingPlausibilityValidator.validate(12400.0, baseline, latestRecord)
        assertTrue(result is ReadingPlausibilityStatus.LowerThanPrevious)
    }

    @Test
    fun validate_candidateResemblingVoltage_returnsWarning() {
        // Candidate 238.0 against baseline of 12,450.0 is almost certainly AC Voltage!
        val result = ReadingPlausibilityValidator.validate(238.0, baseline, latestRecord)
        assertTrue(result is ReadingPlausibilityStatus.Warning)
        val warning = result as ReadingPlausibilityStatus.Warning
        assertTrue(warning.message.contains("Voltage", ignoreCase = true))
    }

    @Test
    fun validate_unusuallyLargeJump_returnsWarning() {
        val result = ReadingPlausibilityValidator.validate(50000.0, baseline, latestRecord)
        assertTrue(result is ReadingPlausibilityStatus.Warning)
        val warning = result as ReadingPlausibilityStatus.Warning
        assertTrue(warning.message.contains("Large increase", ignoreCase = true))
    }
}
