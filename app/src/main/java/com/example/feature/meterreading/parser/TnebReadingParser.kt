package com.example.feature.meterreading.parser

import android.graphics.Rect
import com.example.feature.meterreading.model.ExtractedTextBlock
import com.example.feature.meterreading.model.MeterDisplayType
import com.example.feature.meterreading.model.MeterOcrStatus
import com.example.feature.meterreading.model.MeterReadingCandidate
import java.util.Locale

/**
 * Domain-specific parser for TNEB/TANGEDCO electricity meters.
 *
 * Reliably prioritizes actual cumulative energy consumption (kWh) while rejecting:
 * - Meter constants (Imp/kWh, Revs/kWh)
 * - Meter serial numbers, bar codes, P.O. numbers, and batch dates
 * - AC electrical specifications (Voltage 240V, Current 5-20A, Frequency 50Hz, Class 1.0)
 * - Clock times (HH:MM:SS) and Calendar dates (DD-MM-YYYY, MM/YYYY)
 * - Non-energy meter cycling screens (Instantaneous V, A, kW, PF)
 *
 * Implements robust decimal point preservation and seven-segment dropped-decimal recovery.
 */
object TnebReadingParser {

    private val DATE_REGEX = Regex("""\b\d{1,2}[-/. ]\d{1,2}[-/. ]\d{2,4}\b|\b(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[-/. ]\d{2,4}\b""", RegexOption.IGNORE_CASE)
    private val MONTH_YEAR_REGEX = Regex("""\b(0[1-9]|1[0-2])[-/](20\d{2}|\d{2})\b""")
    private val TIME_REGEX = Regex("""\b\d{1,2}[:.]\d{2}([:.]\d{2})?\b""")

    // Serial number and manufacturing markings
    private val SERIAL_PREFIX_REGEX = Regex(
        """\b(S\.?\s*No|Sr\.?\s*No|Serial|SL\.?\s*NO|No\.?\s*[B]?|TYPE|CAT-?|MTR|CODE|IS:\s*\d+|CM/L-?\s*\d+|P\.?O\.?\s*No|RC\.?\s*\d+)\b""",
        RegexOption.IGNORE_CASE
    )

    // Unit patterns: STRICTLY distinguishes genuine energy consumption from impulse constants
    // "3200 imp/kWh" or "750 revs/kWh" are meter constants, NOT energy consumption!
    private val IMPULSE_CONSTANT_REGEX = Regex("""\b\d+\s*(imp|revs|rev|impulses|revolutions)\s*/\s*kwh\b""", RegexOption.IGNORE_CASE)
    private val KWH_STANDALONE_REGEX = Regex("""(?<!imp/|revs/|rev/|/)\b(kwh|units|unit|k\.w\.h|kw-h)\b""", RegexOption.IGNORE_CASE)

    // Electrical specifications
    private val VOLTAGE_REGEX = Regex("""\b(\d{2,3}(\.\d+)?)\s*(v|volts|volt|vac)\b""", RegexOption.IGNORE_CASE)
    private val CURRENT_REGEX = Regex("""\b(\d{1,2}(\.\d+)?|\d{1,2}-\d{1,2})\s*(a|amps|ampere|amp)\b""", RegexOption.IGNORE_CASE)
    private val MAX_DEMAND_REGEX = Regex("""\b(\d+(\.\d+)?)\s*(kw|kva|md)\b""", RegexOption.IGNORE_CASE)
    private val FREQUENCY_REGEX = Regex("""\b\d{2}(\.\d+)?\s*(hz|hertz)\b""", RegexOption.IGNORE_CASE)
    private val CLASS_REGEX = Regex("""\b(cl|class)\s*[0-9.]+\b""", RegexOption.IGNORE_CASE)

    /**
     * Parses low-level OCR text and structured text blocks into a verified [MeterReadingCandidate].
     * Retained for backward-compatibility and single-pass callers.
     */
    fun parse(
        fullRawText: String,
        blocks: List<ExtractedTextBlock> = emptyList(),
        baselineKwh: Double? = null
    ): MeterReadingCandidate {
        return parseDualPass(
            enhancedRawText = fullRawText,
            enhancedBlocks = blocks,
            originalRawText = "",
            originalBlocks = emptyList(),
            baselineKwh = baselineKwh
        )
    }

    data class VariantOcrResult(
        val variantName: String,
        val rawText: String,
        val blocks: List<ExtractedTextBlock> = emptyList()
    )

    /**
     * Dual-pass parser that compares OCR candidates extracted from the preprocessed binary image
     * against the original color crop image instead of blindly merging them.
     */
    fun parseDualPass(
        enhancedRawText: String,
        enhancedBlocks: List<ExtractedTextBlock> = emptyList(),
        originalRawText: String,
        originalBlocks: List<ExtractedTextBlock> = emptyList(),
        baselineKwh: Double? = null
    ): MeterReadingCandidate {
        val variants = mutableListOf<VariantOcrResult>()
        if (originalRawText.isNotBlank()) {
            variants.add(VariantOcrResult("Variant A (Original Color)", originalRawText, originalBlocks))
        }
        if (enhancedRawText.isNotBlank()) {
            variants.add(VariantOcrResult("Variant C (Adaptive Threshold)", enhancedRawText, enhancedBlocks))
        }
        return parseMultiVariant(variants, baselineKwh)
    }

    /**
     * Multi-variant parser that evaluates outputs from multiple preprocessing variants
     * (Variant A: Original Color, Variant B: CLAHE Grayscale, Variant C: Adaptive Threshold, Variant D: Upscaled)
     * and compares them side-by-side rather than concatenating them.
     */
    fun parseMultiVariant(
        variants: List<VariantOcrResult>,
        baselineKwh: Double? = null
    ): MeterReadingCandidate {
        if (variants.all { it.rawText.isBlank() }) {
            return MeterReadingCandidate(
                rawText = "",
                numericValue = 0.0,
                displayValue = "",
                status = MeterOcrStatus.NO_READING_FOUND,
                statusMessage = "No text could be identified in the meter display frame."
            )
        }

        // 1. Check for dedicated non-energy meter cycling screens across all variants
        for (v in variants) {
            val nonEnergy = detectNonEnergyCycleScreen(v.rawText.lines())
            if (nonEnergy != null) return nonEnergy
        }

        // 2. Check for standalone "kWh" presence across any variant (usually preserved in color or CLAHE)
        val overallKwhPresent = variants.any { v ->
            v.rawText.lines().any { line ->
                line.contains(KWH_STANDALONE_REGEX) && !line.contains(IMPULSE_CONSTANT_REGEX)
            }
        }

        // 3. Extract scored candidates from each variant independently
        val variantCandidates = variants.map { v ->
            v.variantName to extractCandidatesFromText(v.rawText, baselineKwh)
        }

        val allCandidates = mutableListOf<ScoredCandidate>()

        // 4. Multi-variant agreement and decimal preservation
        // Check if any variant found a candidate with a decimal point (e.g. 16596.7)
        // versus variants that may have dropped the decimal (e.g. 165967 or 16596)
        val decimalCandidates = variantCandidates.flatMap { it.second }.filter { it.hasDecimal }

        for ((_, candidates) in variantCandidates) {
            for (c in candidates) {
                var candidateToAdd = c

                // Unit propagation if kWh present in another variant
                if (overallKwhPresent && candidateToAdd.unitDetected == null) {
                    candidateToAdd = candidateToAdd.copy(score = candidateToAdd.score + 50f, unitDetected = "kWh")
                }

                // If candidate lacks decimal, check if another variant recovered the exact same integer part with a decimal
                if (!candidateToAdd.hasDecimal && decimalCandidates.isNotEmpty()) {
                    val matchingDecimal = decimalCandidates.firstOrNull { dc ->
                        dc.integerPart == candidateToAdd.integerPart
                    }
                    if (matchingDecimal != null) {
                        // Direct OCR decimal evidence from another variant!
                        candidateToAdd = candidateToAdd.copy(
                            numericValue = matchingDecimal.numericValue,
                            displayValue = matchingDecimal.displayValue,
                            hasDecimal = true,
                            decimalPart = matchingDecimal.decimalPart,
                            score = candidateToAdd.score + 25f
                        )
                    }
                }

                // Check cross-variant consensus
                val consensusCount = variantCandidates.count { (_, otherList) ->
                    otherList.any { kotlin.math.abs(it.numericValue - candidateToAdd.numericValue) < 0.01 }
                }
                if (consensusCount >= 2) {
                    candidateToAdd = candidateToAdd.copy(score = candidateToAdd.score + 20f * (consensusCount - 1))
                }

                allCandidates.add(candidateToAdd)
            }
        }

        if (allCandidates.isEmpty()) {
            val fallbackText = variants.firstOrNull { it.rawText.isNotBlank() }?.rawText ?: ""
            return MeterReadingCandidate(
                rawText = fallbackText.take(60),
                numericValue = 0.0,
                displayValue = "",
                status = MeterOcrStatus.NO_READING_FOUND,
                statusMessage = "No cumulative energy reading could be determined. Please capture the meter display clearly."
            )
        }

        // Deduplicate candidates by value, keeping the highest scored version
        val deduped = allCandidates.groupBy { it.displayValue }
            .map { (_, group) -> group.maxByOrNull { it.score }!! }

        // Check if top candidates disagree
        val sorted = deduped.sortedByDescending { it.score }
        val top = sorted[0]
        val runnerUp = sorted.getOrNull(1)

        // If top and runner-up have significantly different values and both have decent scores (> 30)
        // mark as requiring user verification and attach alternativeCandidate
        val finalCandidate = if (runnerUp != null && runnerUp.score >= 30f &&
            kotlin.math.abs(top.numericValue - runnerUp.numericValue) > 0.05
        ) {
            top.copy(
                isSuspicious = true,
                warningNote = "OCR variants disagree (${top.displayValue} vs ${runnerUp.displayValue}). Please verify digits on display.",
                alternativeCandidate = runnerUp.numericValue
            )
        } else {
            top
        }

        return selectAndBuildCandidate(listOf(finalCandidate))
    }

    private fun extractCandidatesFromText(
        text: String,
        baselineKwh: Double?
    ): List<ScoredCandidate> {
        if (text.isBlank()) return emptyList()

        val rawLines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val blacklistedSerialTokens = identifyBlacklistedSerialTokens(rawLines)
        val candidates = mutableListOf<ScoredCandidate>()

        for (i in rawLines.indices) {
            val line = rawLines[i]

            // Skip lines that are purely dates or times
            if (DATE_REGEX.matches(line) || TIME_REGEX.matches(line) || MONTH_YEAR_REGEX.matches(line)) continue

            val isImpulseLine = line.contains(IMPULSE_CONSTANT_REGEX)
            val hasKwhInLine = line.contains(KWH_STANDALONE_REGEX) && !isImpulseLine
            val hasVoltageInLine = line.contains(VOLTAGE_REGEX) || line.contains(Regex("""\b(v|volts|vac)\b""", RegexOption.IGNORE_CASE))
            val hasCurrentInLine = line.contains(CURRENT_REGEX) || line.contains(Regex("""\b(a|amps)\b""", RegexOption.IGNORE_CASE))
            val hasMdInLine = line.contains(MAX_DEMAND_REGEX) && !hasKwhInLine

            // Evidence-based space-separated decimal detection:
            // e.g. "16597 7 kWh" -> physical gap between 5 whole digits and 1 decimal digit
            val spaceDecimalMatch = Regex("""\b(\d{4,6})\s+(\d{1,2})\b""").find(line)
            val tokens = line.split(Regex("""\s+""")).toMutableList()
            if (spaceDecimalMatch != null) {
                val synthesized = "${spaceDecimalMatch.groupValues[1]}.${spaceDecimalMatch.groupValues[2]}"
                tokens.add(0, synthesized)
            }

            for (token in tokens) {
                if (blacklistedSerialTokens.contains(token) || isBlacklistedSpecificationToken(token)) {
                    continue
                }

                if (token.equals("kwh", ignoreCase = true) || token.equals("units", ignoreCase = true) ||
                    token.equals("unit", ignoreCase = true)) {
                    continue
                }

                val candidateStr = normalizeOcrNumbers(token)
                val parsed = parseNumericString(candidateStr) ?: continue

                if (parsed.numericValue in 2010.0..2030.0 && !hasKwhInLine && !parsed.hasDecimal) {
                    continue
                }

                var score = 0f
                var isSuspicious = false
                var warningNote: String? = null
                var alternativeVal: Double? = null

                // Base digit count scoring: TNEB residential meters typically display 5 whole + 1 decimal digit (5-6 total)
                val totalDigits = parsed.integerPart.toString().length + (parsed.decimalPart?.toString()?.length ?: 0)
                when (totalDigits) {
                    in 5..6 -> score += 40f
                    7 -> score += 20f
                    4 -> score += 10f
                    in 8..9 -> score -= 25f
                    else -> score -= 35f
                }

                if (hasKwhInLine) {
                    score += 55f
                }
                if (isImpulseLine) {
                    score -= 50f
                    isSuspicious = true
                    warningNote = "Value appears to be meter constant (${parsed.numericValue} imp/kWh)."
                }
                if (hasVoltageInLine || (parsed.numericValue in 210.0..255.0 && !hasKwhInLine)) {
                    score -= 45f
                    isSuspicious = true
                    warningNote = "Value resembles AC Voltage (${parsed.numericValue} V)."
                }
                if (hasCurrentInLine) {
                    score -= 40f
                    isSuspicious = true
                    warningNote = "Value resembles Current (${parsed.numericValue} A)."
                }
                if (hasMdInLine) {
                    score -= 30f
                    isSuspicious = true
                    warningNote = "Value resembles Maximum Demand (${parsed.numericValue} kW)."
                }

                var candidateNumeric = parsed.numericValue
                var candidateDisplay = parsed.displayString
                var candidateHasDecimal = parsed.hasDecimal
                var candidateInteger = parsed.integerPart
                var candidateDecimal = parsed.decimalPart
                var recoveredFromDroppedDecimal = false

                if (baselineKwh != null && baselineKwh > 0.0) {
                    val delta = candidateNumeric - baselineKwh

                    // Seven-segment dropped decimal recovery check:
                    // If candidate is a 6-digit integer without decimal point and delta > 20000,
                    // test if candidate / 10.0 matches baseline incremental consumption.
                    if (!parsed.hasDecimal && totalDigits == 6 && delta > 20000.0) {
                        val decimalCandidateValue = candidateNumeric / 10.0
                        val decimalDelta = decimalCandidateValue - baselineKwh
                        if (decimalDelta in -5.0..4000.0) {
                            candidateNumeric = decimalCandidateValue
                            candidateDisplay = String.format(Locale.US, "%.1f", decimalCandidateValue)
                            candidateHasDecimal = true
                            candidateInteger = (candidateNumeric).toLong()
                            candidateDecimal = (candidateNumeric * 10).toLong().rem(10).toInt()
                            recoveredFromDroppedDecimal = true
                            score += 45f
                            alternativeVal = parsed.numericValue
                            warningNote = "Recovered fractional decimal digit (${candidateDisplay} kWh) — please verify."
                        }
                    }

                    val finalDelta = candidateNumeric - baselineKwh
                    when {
                        finalDelta in 0.0..3500.0 -> score += 30f
                        finalDelta in -2.0..0.0 -> score += 10f
                        finalDelta < -20.0 -> {
                            score -= 35f
                            isSuspicious = true
                            warningNote = "Reading is lower than previous recorded baseline (${String.format(Locale.US, "%,.0f", baselineKwh)} kWh)."

                            // Generate alternative candidate for 7-segment 5 vs 6 ambiguity without mutating the digit
                            val strVal = parsed.displayString
                            if (strVal.startsWith("15") || strVal.startsWith("5")) {
                                val altStr = if (strVal.startsWith("15")) "16" + strVal.substring(2) else "6" + strVal.substring(1)
                                val altNum = altStr.toDoubleOrNull()
                                if (altNum != null && (altNum - baselineKwh) in 0.0..4000.0) {
                                    alternativeVal = altNum
                                    warningNote = "Reading $candidateDisplay is lower than previous baseline. If display shows $altStr, please verify."
                                }
                            }
                        }
                        finalDelta > 15000.0 -> {
                            score -= 20f
                            isSuspicious = true
                            warningNote = "Unusually high jump from baseline. Please verify digits."
                        }
                    }
                } else {
                    // Without baseline, if 6 digits without decimal was extracted from an LCD,
                    // flag for user verification
                    if (!parsed.hasDecimal && totalDigits == 6) {
                        val decimalAlt = candidateNumeric / 10.0
                        alternativeVal = decimalAlt
                        isSuspicious = true
                        warningNote = "No decimal point was detected in OCR. If display shows ${String.format(Locale.US, "%.1f", decimalAlt)} kWh, please verify."
                    }
                }

                val isMechanicalAmbiguous = detectMechanicalAmbiguity(token)
                if (isMechanicalAmbiguous) {
                    score -= 15f
                    isSuspicious = true
                    warningNote = "Rotating number wheel may be between digits. Please verify carefully."
                }

                candidates.add(
                    ScoredCandidate(
                        rawToken = token,
                        numericValue = candidateNumeric,
                        displayValue = candidateDisplay,
                        hasDecimal = candidateHasDecimal,
                        integerPart = candidateInteger,
                        decimalPart = candidateDecimal,
                        unitDetected = if (hasKwhInLine) "kWh" else null,
                        score = score,
                        isSuspicious = isSuspicious,
                        isMechanicalAmbiguous = isMechanicalAmbiguous,
                        recoveredDroppedDecimal = recoveredFromDroppedDecimal,
                        warningNote = warningNote,
                        alternativeCandidate = alternativeVal
                    )
                )
            }
        }

        return candidates
    }

    private fun selectAndBuildCandidate(candidates: List<ScoredCandidate>): MeterReadingCandidate {
        val top = candidates.maxByOrNull { it.score }!!

        val finalStatus = when {
            top.score < 20f -> MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED
            top.isMechanicalAmbiguous -> MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED
            top.recoveredDroppedDecimal -> MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED
            top.alternativeCandidate != null -> MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED
            top.isSuspicious -> MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED
            top.unitDetected == "kWh" && top.score >= 50f -> MeterOcrStatus.DETECTED_SUCCESS
            top.score >= 40f -> MeterOcrStatus.DETECTED_SUCCESS
            else -> MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED
        }

        val statusMessage = when (finalStatus) {
            MeterOcrStatus.DETECTED_SUCCESS -> "Reading detected — please verify"
            MeterOcrStatus.AMBIGUOUS_VERIFY_NEEDED -> top.warningNote ?: "Reading may need verification — check digits"
            else -> "Please verify reading"
        }

        return MeterReadingCandidate(
            rawText = top.rawToken,
            numericValue = top.numericValue,
            displayValue = top.displayValue,
            hasDecimal = top.hasDecimal,
            integerPart = top.integerPart,
            decimalPart = top.decimalPart,
            unitDetected = top.unitDetected,
            confidenceScore = (top.score / 100f).coerceIn(0.1f, 0.95f),
            meterType = if (top.isMechanicalAmbiguous) MeterDisplayType.MECHANICAL_ROTATING_WHEEL else MeterDisplayType.DIGITAL_LCD,
            status = finalStatus,
            statusMessage = statusMessage,
            isMechanicalAmbiguous = top.isMechanicalAmbiguous,
            warningNote = top.warningNote,
            alternativeCandidate = top.alternativeCandidate
        )
    }

    /**
     * Identifies serial numbers, barcode tokens, and manufacturing reference numbers
     * across multiple lines and blocks so they are never confused with energy readings.
     */
    private fun identifyBlacklistedSerialTokens(lines: List<String>): Set<String> {
        val blacklisted = mutableSetOf<String>()

        for (i in lines.indices) {
            val line = lines[i]

            // Check if current line contains a serial number prefix
            if (line.contains(SERIAL_PREFIX_REGEX) && !line.contains(KWH_STANDALONE_REGEX)) {
                // All numeric tokens in this line are serial numbers
                line.split(Regex("""\s+""")).forEach { token ->
                    val clean = normalizeOcrNumbers(token)
                    if (clean.any { it.isDigit() }) blacklisted.add(token)
                }

                // In ML Kit OCR, the prefix (e.g. "S. No.") is often on line i,
                // and the actual serial number (e.g. "5654683") is on line i+1!
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1]
                    if (!nextLine.contains(KWH_STANDALONE_REGEX)) {
                        nextLine.split(Regex("""\s+""")).forEach { token ->
                            val clean = normalizeOcrNumbers(token)
                            if (clean.filter { it.isDigit() }.length in 6..9) {
                                blacklisted.add(token)
                            }
                        }
                    }
                }
            }

            // Check for standalone barcode number lines (e.g. "61186297 MAR 2016" or "706959")
            if (line.contains(DATE_REGEX) || line.contains(MONTH_YEAR_REGEX)) {
                line.split(Regex("""\s+""")).forEach { token ->
                    val digits = token.filter { it.isDigit() }
                    if (digits.length in 6..9) {
                        blacklisted.add(token)
                    }
                }
            }
        }

        return blacklisted
    }

    private fun isBlacklistedSpecificationToken(token: String): Boolean {
        val upper = token.uppercase(Locale.US)
        val digitsOnly = token.filter { it.isDigit() || it == '.' }
        return (upper.endsWith("V") && digitsOnly.isNotBlank()) ||
                upper.endsWith("HZ") ||
                (upper.endsWith("A") && digitsOnly.isNotBlank()) ||
                upper.contains("REV/") || upper.contains("IMP/") || upper.startsWith("CL") ||
                upper.contains("TYPE") || upper.contains("CM/L") || upper.contains("IS:") ||
                upper.contains("PHASE") || upper.contains("WIRE") || upper.contains("AMP")
    }

    /**
     * Normalizes common OCR misrecognitions within numeric tokens (7-segment LCD or number wheels).
     */
    fun normalizeOcrNumbers(token: String): String {
        // Convert non-standard decimal separators (e.g. "19130,2", "16597'7", "16596·7", "19130-2") into "."
        var cleaned = Regex("""(?<=\d)[,':\-\·\•](?=\d{1,2}(?!\d))""").replace(token, ".")

        // Strip remaining brackets, currency symbols, thousands commas
        cleaned = cleaned.replace(Regex("""[\[\]\(\)\$₹,\s]"""), "")

        val digitCount = cleaned.count { it.isDigit() }
        val letterCount = cleaned.count { it.isLetter() }

        // Only normalize characters if token is overwhelmingly numeric
        if (digitCount >= 2 && letterCount <= 3) {
            cleaned = cleaned
                .replace('O', '0')
                .replace('o', '0')
                .replace('D', '0')
                .replace('I', '1')
                .replace('l', '1')
                .replace('|', '1')
                .replace('Z', '2')
                .replace('z', '2')
                .replace('S', '5')
                .replace('s', '5')
                .replace('B', '8')
        }
        return cleaned
    }

    /**
     * Parses a string containing digits and an optional decimal separator (. or ,)
     */
    private fun parseNumericString(str: String): ParsedNumeric? {
        val filtered = str.filter { it.isDigit() || it == '.' }
        if (filtered.isBlank() || filtered.count { it == '.' } > 1) return null

        val numVal = filtered.toDoubleOrNull() ?: return null
        if (numVal < 0) return null

        val parts = filtered.split('.')
        val intPart = parts[0].toLongOrNull() ?: return null
        val decPart = if (parts.size > 1 && parts[1].isNotBlank()) parts[1].toIntOrNull() else null

        return ParsedNumeric(
            numericValue = numVal,
            displayString = filtered,
            hasDecimal = parts.size > 1 && decPart != null,
            integerPart = intPart,
            decimalPart = decPart
        )
    }

    /**
     * Checks if a mechanical number wheel digit appears split or ambiguous.
     */
    private fun detectMechanicalAmbiguity(token: String): Boolean {
        return token.contains('/') || token.contains(':') || token.contains('~') ||
                Regex("""\d[|iI]\d""").containsMatchIn(token)
    }

    /**
     * Detects if the meter display is showing a non-energy cycling screen (Voltage, Current, Date, Time).
     */
    private fun detectNonEnergyCycleScreen(lines: List<String>): MeterReadingCandidate? {
        if (lines.any { it.contains(KWH_STANDALONE_REGEX) && !it.contains(IMPULSE_CONSTANT_REGEX) }) {
            return null
        }

        for (line in lines) {
            val vMatch = VOLTAGE_REGEX.find(line)
            if (vMatch != null && !line.contains(KWH_STANDALONE_REGEX)) {
                val vVal = vMatch.groupValues[1].toDoubleOrNull() ?: 230.0
                return MeterReadingCandidate(
                    rawText = line,
                    numericValue = vVal,
                    displayValue = String.format(Locale.US, "%.1f", vVal),
                    unitDetected = "V",
                    status = MeterOcrStatus.NON_KWH_WARNING,
                    statusMessage = "Voltage display detected (${vVal.toInt()} V). Please capture when cumulative 'kWh' is displayed.",
                    warningNote = "The meter was photographed on the Voltage screen. Press the meter button until 'kWh' appears."
                )
            }

            val aMatch = CURRENT_REGEX.find(line)
            if (aMatch != null && !line.contains(KWH_STANDALONE_REGEX)) {
                val aVal = aMatch.groupValues[1].toDoubleOrNull() ?: 5.0
                return MeterReadingCandidate(
                    rawText = line,
                    numericValue = aVal,
                    displayValue = String.format(Locale.US, "%.2f", aVal),
                    unitDetected = "A",
                    status = MeterOcrStatus.NON_KWH_WARNING,
                    statusMessage = "Current display detected (${aVal} A). Please capture when cumulative 'kWh' is displayed.",
                    warningNote = "The meter was photographed on the Current screen. Press the meter button until 'kWh' appears."
                )
            }

            val mdMatch = MAX_DEMAND_REGEX.find(line)
            if (mdMatch != null && !line.contains(KWH_STANDALONE_REGEX)) {
                val mdVal = mdMatch.groupValues[1].toDoubleOrNull() ?: 2.0
                return MeterReadingCandidate(
                    rawText = line,
                    numericValue = mdVal,
                    displayValue = String.format(Locale.US, "%.2f", mdVal),
                    unitDetected = "kW",
                    status = MeterOcrStatus.NON_KWH_WARNING,
                    statusMessage = "Maximum Demand display detected (${mdVal} kW). Please capture when cumulative 'kWh' is displayed.",
                    warningNote = "The meter was photographed on the Maximum Demand screen. Press the meter button until 'kWh' appears."
                )
            }
        }
        return null
    }

    private data class ParsedNumeric(
        val numericValue: Double,
        val displayString: String,
        val hasDecimal: Boolean,
        val integerPart: Long,
        val decimalPart: Int?
    )

    private data class ScoredCandidate(
        val rawToken: String,
        val numericValue: Double,
        val displayValue: String,
        val hasDecimal: Boolean,
        val integerPart: Long,
        val decimalPart: Int?,
        val unitDetected: String?,
        val score: Float,
        val isSuspicious: Boolean,
        val isMechanicalAmbiguous: Boolean,
        val recoveredDroppedDecimal: Boolean = false,
        val warningNote: String?,
        val alternativeCandidate: Double? = null
    )
}
