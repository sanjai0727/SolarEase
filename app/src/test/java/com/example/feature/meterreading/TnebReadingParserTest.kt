package com.example.feature.meterreading

import com.example.feature.meterreading.model.MeterDisplayType
import com.example.feature.meterreading.model.MeterOcrStatus
import com.example.feature.meterreading.parser.TnebReadingParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TnebReadingParserTest {

    // ---------------------------------------------------------
    // TEST CASE 1 REGRESSION: L&T EM101+ Digital Meter
    // Expected: ~16596.7 kWh, rejected: 155957
    // ---------------------------------------------------------
    @Test
    fun parse_testCase1_ltEm101DigitalMeter_preservesDecimalAndRejectsInteger155957() {
        val isolatedLcdCropText = "16596.7 kWh"
        val candidate = TnebReadingParser.parse(isolatedLcdCropText)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(16596.7, candidate.numericValue, 0.001)
        assertEquals("16596.7", candidate.displayValue)
        assertEquals(16596L, candidate.integerPart)
        assertEquals(7, candidate.decimalPart)
        assertTrue(candidate.hasDecimal)
        assertEquals("kWh", candidate.unitDetected)
    }

    @Test
    fun parse_testCase1_ltEm101_fullFaceplateOcr_ignoresBarcodeAndSerial() {
        // Full faceplate text containing barcode number 61186297 MAR 2016, 240V, 50Hz, 3200 imp/kWh
        val faceplateOcr = """
            1 PHASE 2 WIRE AC STATIC WATTHOUR METER EM101+
            Property of "TANGEDCO"
            16596.7 kWh
            61186297 MAR 2016
            3200 imp/kWh
            240V 5-20 A 50Hz
            P.O. No.: CE/MM/SE/MM-II/EEM/A5/M.50/2014-15/
            RC.41/D.250/15DT:28.10.15
        """.trimIndent()

        val candidate = TnebReadingParser.parse(faceplateOcr, baselineKwh = 16400.0)

        // Reading must be 16596.7, NOT the barcode 61186297 and NOT 3200
        assertEquals(16596.7, candidate.numericValue, 0.001)
        assertEquals("16596.7", candidate.displayValue)
        assertNotEquals(61186297.0, candidate.numericValue, 0.001)
        assertNotEquals(3200.0, candidate.numericValue, 0.001)
    }

    @Test
    fun parse_testCase1_droppedDecimalRecovery_whenBaselineIsProvided() {
        // If ML Kit drops the decimal dot on the 7-segment display, yielding 155957 or 165967,
        // baseline comparison recovers the fractional decimal 15595.7 or 16596.7!
        val droppedDotOcr = "165967 kWh"
        val candidate = TnebReadingParser.parse(droppedDotOcr, baselineKwh = 16500.0)

        assertEquals(16596.7, candidate.numericValue, 0.001)
        assertTrue(candidate.hasDecimal)
        assertEquals(16596L, candidate.integerPart)
        assertEquals(7, candidate.decimalPart)
    }

    // ---------------------------------------------------------
    // TEST CASE 2 REGRESSION: Capital Digital Meter
    // Expected: ~19130.2, rejected: 554583 / 5654683
    // ---------------------------------------------------------
    @Test
    fun parse_testCase2_capitalDigitalMeter_extractsReadingAndRejectsSerial5654683() {
        val isolatedLcdCropText = "19130.2 kWh"
        val candidate = TnebReadingParser.parse(isolatedLcdCropText)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(19130.2, candidate.numericValue, 0.001)
        assertEquals("19130.2", candidate.displayValue)
        assertEquals(19130L, candidate.integerPart)
        assertEquals(2, candidate.decimalPart)
        assertTrue(candidate.hasDecimal)
    }

    @Test
    fun parse_testCase2_capital_fullFaceplateOcr_ignoresMultiLineSerialAndImpConstant() {
        // Reproduces the exact root cause where "S. No." is on line 1, "5654683 02-2020" on line 2,
        // and "3200 Imp/kWh" on line 3.
        val faceplateOcr = """
            ac 1ph 2wire Class 1 Static kWh Meter
            19130.2 kWh
            S. No.
            5654683 02-2020
            Property of : TANGEDCO
            3200 Imp/kWh
            240V 50Hz
            CAPITAL POWER SYSTEMS LTD. NOIDA
        """.trimIndent()

        val candidate = TnebReadingParser.parse(faceplateOcr)

        // Reading must be 19130.2, NEVER the serial number 5654683 or 554583
        assertEquals(19130.2, candidate.numericValue, 0.001)
        assertNotEquals(5654683.0, candidate.numericValue, 0.001)
        assertNotEquals(554583.0, candidate.numericValue, 0.001)
        assertNotEquals(3200.0, candidate.numericValue, 0.001)
    }

    // ---------------------------------------------------------
    // MECHANICAL METERS REGRESSION
    // ---------------------------------------------------------
    @Test
    fun parse_mechanicalMeterDark_indiaMeters_extractsReadingWithDecimal() {
        // India Meters Ltd.: "42380.0 kWh", Serial "No. B 0568777", "2880 Revs/kWh", "240 V 50 Hz"
        val ocr = """
            42380.0 kWh
            PROPERTY OF TNEB
            240 V 50 Hz 2880 Revs/Kwh
            INDIA METERS LTD., MADRAS INDIA
            No. B 0568777
        """.trimIndent()

        val candidate = TnebReadingParser.parse(ocr)

        assertEquals(42380.0, candidate.numericValue, 0.001)
        assertTrue(candidate.hasDecimal)
        assertNotEquals(568777.0, candidate.numericValue, 0.001)
        assertNotEquals(2880.0, candidate.numericValue, 0.001)
    }

    @Test
    fun parse_mechanicalMeterLight_centimetersElectricals_extractsWholeUnitsAndRejectsSerial() {
        // Centimeters Electricals: "286626 kWh", "Sr. No. 706959", "750 Revs/kWh"
        val ocr = """
            kWh METER
            286626
            Property of TNEB
            CENTIMETERS ELECTRICALS PVT. LTD. DELHI
            240 V, 50 Hz, (5-20) Amp.
            750 Revs/kWh, Sr. No. 706959
            Made in India, 11/2010
        """.trimIndent()

        val candidate = TnebReadingParser.parse(ocr)

        assertEquals(286626.0, candidate.numericValue, 0.001)
        assertNotEquals(706959.0, candidate.numericValue, 0.001)
        assertNotEquals(750.0, candidate.numericValue, 0.001)
    }

    @Test
    fun parse_mechanicalNumberWheel_detectsHalfwayAmbiguity() {
        val raw = "01454/5 kWh" // Digit halfway between 4 and 5
        val candidate = TnebReadingParser.parse(raw)

        assertTrue(candidate.isMechanicalAmbiguous)
        assertEquals(MeterDisplayType.MECHANICAL_ROTATING_WHEEL, candidate.meterType)
        assertEquals(MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED, candidate.status)
        assertTrue(candidate.statusMessage.contains("verify", ignoreCase = true))
    }

    // ---------------------------------------------------------
    // DECIMAL HANDLING AND SEPARATORS
    // ---------------------------------------------------------
    @Test
    fun parse_commaDecimalSeparator_isCorrectlyNormalizedToPoint() {
        val raw = "19130,2 kWh"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(19130.2, candidate.numericValue, 0.001)
        assertEquals(19130L, candidate.integerPart)
        assertEquals(2, candidate.decimalPart)
        assertTrue(candidate.hasDecimal)
    }

    @Test
    fun parse_readingWithLeadingZeros_preservesIntegerAndDecimal() {
        val raw = "014528.4 kWh"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(14528.4, candidate.numericValue, 0.001)
        assertEquals(14528L, candidate.integerPart)
        assertEquals(4, candidate.decimalPart)
    }

    // ---------------------------------------------------------
    // NON-ENERGY METRIC CYCLING SCREENS
    // ---------------------------------------------------------
    @Test
    fun parse_voltageDisplay_returnsNonKwhWarning() {
        val raw = "238.4 V\n50 Hz"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.NON_KWH_WARNING, candidate.status)
        assertEquals(238.4, candidate.numericValue, 0.001)
        assertEquals("V", candidate.unitDetected)
        assertTrue(candidate.statusMessage.contains("Voltage", ignoreCase = true))
    }

    @Test
    fun parse_currentDisplay_returnsNonKwhWarning() {
        val raw = "05.60 A"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.NON_KWH_WARNING, candidate.status)
        assertEquals(5.6, candidate.numericValue, 0.001)
        assertEquals("A", candidate.unitDetected)
        assertTrue(candidate.statusMessage.contains("Current", ignoreCase = true))
    }

    @Test
    fun parse_maximumDemandDisplay_returnsNonKwhWarning() {
        val raw = "MD 02.45 kW\nTIME 10:15"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.NON_KWH_WARNING, candidate.status)
        assertEquals(2.45, candidate.numericValue, 0.001)
        assertEquals("kW", candidate.unitDetected)
        assertTrue(candidate.statusMessage.contains("Maximum Demand", ignoreCase = true))
    }

    // ---------------------------------------------------------
    // EDGE CASES: AMBIGUOUS CHARACTERS, EMPTY OCR, MULTIPLE NUMBERS
    // ---------------------------------------------------------
    @Test
    fun parse_dateAndTimestampLines_areFilteredOut() {
        val raw = "25-10-2026\n09:35:12\n12450 kWh"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(12450.0, candidate.numericValue, 0.001)
    }

    @Test
    fun parse_ambiguousOcrCharacters_areNormalizedCorrectly() {
        // 'O' -> '0', 'S' -> '5', 'B' -> '8'
        val raw = "O124SB kWh"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(12458.0, candidate.numericValue, 0.001)
    }

    @Test
    fun parse_emptyOcrOutput_returnsNoReadingFound() {
        val raw = "   "
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.NO_READING_FOUND, candidate.status)
        assertEquals(0.0, candidate.numericValue, 0.001)
    }

    @Test
    fun parse_competingCandidates_prefersKwhAssociation() {
        val raw = "240 V\n5 A\n50 Hz\n14892 kWh"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(14892.0, candidate.numericValue, 0.001)
        assertEquals("kWh", candidate.unitDetected)
    }

    @Test
    fun parse_impulseConstantAlone_doesNotWinAsCumulativeReading() {
        // If crop only contains "3200 imp/kWh" and no genuine cumulative reading
        val raw = "3200 imp/kWh\n240V 50Hz"
        val candidate = TnebReadingParser.parse(raw)

        // Must NOT be DETECTED_SUCCESS
        assertNotEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
    }

    // ---------------------------------------------------------
    // DUAL-PASS PARSING & ENHANCED DIGIT RECOVERY
    // ---------------------------------------------------------
    @Test
    fun parseDualPass_prefersHighContrastDigitsFromEnhancedAndUnitsFromOriginal() {
        // Enhanced pass isolated clean digits from high-contrast LCD: "16597.7"
        val enhancedText = "16597.7"
        // Original color crop preserved printed faceplate labels: "kWh", "3200 imp/kWh"
        val originalText = """
            PROPERTY OF TANGEDCO
            3200 imp/kWh
            240V 50Hz kWh
        """.trimIndent()

        val candidate = TnebReadingParser.parseDualPass(
            enhancedRawText = enhancedText,
            originalRawText = originalText
        )

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(16597.7, candidate.numericValue, 0.001)
        assertEquals("16597.7", candidate.displayValue)
        assertEquals("kWh", candidate.unitDetected)
        assertTrue(candidate.hasDecimal)
    }

    @Test
    fun parse_spaceSeparatedDecimal_joinsCorrectly() {
        // Physical LCD spacing gap between whole digits and fractional digit: "16597 7 kWh"
        val raw = "16597 7 kWh"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(16597.7, candidate.numericValue, 0.001)
        assertEquals("16597.7", candidate.displayValue)
        assertEquals(16597L, candidate.integerPart)
        assertEquals(7, candidate.decimalPart)
        assertTrue(candidate.hasDecimal)
    }

    @Test
    fun parse_nonStandardDecimalSeparators_recoversDecimals() {
        // Tests apostrophe/quote (16597'7), middle dot (16596·7), and hyphen (19130-2)
        val quoteCandidate = TnebReadingParser.parse("16597'7 kWh")
        assertEquals(16597.7, quoteCandidate.numericValue, 0.001)
        assertTrue(quoteCandidate.hasDecimal)

        val middleDotCandidate = TnebReadingParser.parse("16596·7 kWh")
        assertEquals(16596.7, middleDotCandidate.numericValue, 0.001)
        assertTrue(middleDotCandidate.hasDecimal)

        val hyphenCandidate = TnebReadingParser.parse("19130-2 kWh")
        assertEquals(19130.2, hyphenCandidate.numericValue, 0.001)
        assertTrue(hyphenCandidate.hasDecimal)
    }

    @Test
    fun parse_ambiguousLeading5_neverSilentlyMutatesDigit_flagsAlternativeForUserConfirmation() {
        // User review requirement: NEVER silently change digits based on billing history!
        // When OCR reads "15597.7" and previous baseline was 16400.0 (delta < -20 kWh),
        // primary reading must remain 15597.7, status AMBIGUOUS_VERIFY_NEEDED,
        // and alternativeCandidate populated with 16597.7 for explicit user review.
        val raw = "15597.7 kWh"
        val candidate = TnebReadingParser.parse(raw, baselineKwh = 16400.0)

        // Raw candidate must be preserved as primary numericValue (NO silent overwrite)
        assertEquals(15597.7, candidate.numericValue, 0.001)
        assertEquals(MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED, candidate.status)
        // Alternative candidate provided for user review
        assertEquals(16597.7, candidate.alternativeCandidate!!, 0.001)
        assertTrue(candidate.warningNote?.contains("16597.7") == true || candidate.warningNote?.contains("baseline") == true)
    }

    @Test
    fun parse_sixDigitIntegerWithoutDecimal_setsAlternativeAndFlagsForReview() {
        // User review requirement: Do not assume every 6-digit integer has a dropped decimal!
        // Without baseline, 6 digits integer like "165967" must flag AMBIGUOUS_VERIFY_NEEDED
        // with alternativeCandidate = 16596.7 for visual confirmation.
        val raw = "165967 kWh"
        val candidate = TnebReadingParser.parse(raw)

        assertEquals(165967.0, candidate.numericValue, 0.001)
        assertEquals(MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED, candidate.status)
        assertEquals(16596.7, candidate.alternativeCandidate!!, 0.001)
        assertTrue(candidate.warningNote?.contains("16596.7") == true)
    }

    // ---------------------------------------------------------
    // MULTI-VARIANT PARSING TESTS (METER-OCR-ANDROID ADAPTATION)
    // ---------------------------------------------------------
    @Test
    fun parseMultiVariant_allVariantsAgree_highConfidence() {
        val variants = listOf(
            TnebReadingParser.VariantOcrResult("Variant A (Color)", "16596.7 kWh"),
            TnebReadingParser.VariantOcrResult("Variant B (Grayscale CLAHE)", "16596.7 kWh"),
            TnebReadingParser.VariantOcrResult("Variant C (Adaptive Threshold)", "16596.7 kWh")
        )
        val candidate = TnebReadingParser.parseMultiVariant(variants)

        assertEquals(MeterOcrStatus.DETECTED_SUCCESS, candidate.status)
        assertEquals(16596.7, candidate.numericValue, 0.001)
        assertEquals("16596.7", candidate.displayValue)
        assertTrue(candidate.hasDecimal)
        assertEquals("kWh", candidate.unitDetected)
    }

    @Test
    fun parseMultiVariant_decimalPreservedFromEnhancedVariant_whenColorDropsDot() {
        val variants = listOf(
            TnebReadingParser.VariantOcrResult("Variant A (Color)", "16596 kWh"), // Color missed dot
            TnebReadingParser.VariantOcrResult("Variant B (Grayscale CLAHE)", "16596.7 kWh"), // CLAHE captured dot
            TnebReadingParser.VariantOcrResult("Variant C (Adaptive Threshold)", "165967 kWh") // Threshold dropped dot
        )
        val candidate = TnebReadingParser.parseMultiVariant(variants)

        assertEquals(16596.7, candidate.numericValue, 0.001)
        assertTrue(candidate.hasDecimal)
        assertEquals(7, candidate.decimalPart)
    }

    @Test
    fun parseMultiVariant_variantsDisagree_flagsAmbiguousVerifyNeeded() {
        val variants = listOf(
            TnebReadingParser.VariantOcrResult("Variant B (Grayscale CLAHE)", "15597.9 kWh"),
            TnebReadingParser.VariantOcrResult("Variant C (Adaptive Threshold)", "16597.9 kWh")
        )
        val candidate = TnebReadingParser.parseMultiVariant(variants)

        // Neither reading should be silently overwritten; flag for user confirmation
        assertEquals(MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED, candidate.status)
        assertNotNull(candidate.alternativeCandidate)
        assertTrue(candidate.statusMessage.contains("verify", ignoreCase = true) || candidate.statusMessage.contains("disagree", ignoreCase = true))
    }
}
