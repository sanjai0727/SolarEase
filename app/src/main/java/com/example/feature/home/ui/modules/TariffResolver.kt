package com.example.feature.home.ui.modules

import java.util.Locale

/**
 * Standard normalized representation of a TNEB consumer tariff.
 */
data class NormalizedTariff(
    val tariffCode: String,
    val tariffName: String,
    val displayLabel: String
)

/**
 * Single source of truth for resolving raw TNEB tariff strings into
 * clean, human-readable tariff codes and category names.
 *
 * In TNEB (Tamil Nadu Generation and Distribution Corporation) LT billing:
 * - "LA1A", "LA-1A", "1A", "01" = LT-IA (Domestic)
 * - "LA1B", "1B" = LT-IB
 * - "LA1C", "1C" = LT-IC
 * - "LA2A", "2A" = LT-IIA (Commercial)
 * - "LA2B", "2B" = LT-IIB
 * - "LA3A", "3A" = LT-IIIA (Industrial)
 * - "LA4", "4"   = LT-IV (Agriculture)
 * - "LA5", "5"   = LT-V
 *
 * Display format: "LT-IA · Domestic"
 */
object TariffResolver {

    fun resolve(rawTariff: String?): NormalizedTariff {
        if (rawTariff.isNullOrBlank()) {
            return NormalizedTariff(
                tariffCode = "LT-IA",
                tariffName = "Domestic",
                displayLabel = "LT-IA · Domestic"
            )
        }

        val cleaned = rawTariff.trim()
        val upper = cleaned.uppercase(Locale.US)

        val delimiterRegex = Regex("[/·•]")
        val tokens = cleaned.split(delimiterRegex).map { it.trim() }.filter { it.isNotBlank() }

        val codeToken = tokens.firstOrNull { token ->
            val u = token.uppercase(Locale.US)
            u.contains("LA") || u.contains("LT") || u.contains("1A") || u.contains("IA") || u.contains("2A")
        } ?: tokens.firstOrNull().orEmpty()

        val normalizedCode = when {
            upper.contains("LA1A") || upper.contains("LA-1A") || upper.contains("LA 1A") || upper.contains("LA IA") || upper.contains("LT1A") || upper.contains("LT-1A") || upper.contains("LT-IA") || upper.contains("LT 1A") || upper.contains("LT IA") || upper.contains("01 - DOMESTIC") || upper.contains("01-DOMESTIC") -> "LT-IA"
            upper.contains("LA1B") || upper.contains("LA-1B") || upper.contains("LT1B") || upper.contains("LT-1B") || upper.contains("LT-IB") -> "LT-IB"
            upper.contains("LA1C") || upper.contains("LA-1C") || upper.contains("LT1C") || upper.contains("LT-1C") || upper.contains("LT-IC") -> "LT-IC"
            upper.contains("LA2A") || upper.contains("LA-2A") || upper.contains("LT2A") || upper.contains("LT-2A") || upper.contains("LT-IIA") -> "LT-IIA"
            upper.contains("LA2B") || upper.contains("LA-2B") || upper.contains("LT2B") || upper.contains("LT-2B") || upper.contains("LT-IIB") -> "LT-IIB"
            upper.contains("LA2C") || upper.contains("LA-2C") || upper.contains("LT2C") || upper.contains("LT-2C") || upper.contains("LT-IIC") -> "LT-IIC"
            upper.contains("LA3A") || upper.contains("LA-3A") || upper.contains("LT3A") || upper.contains("LT-3A") || upper.contains("LT-IIIA") -> "LT-IIIA"
            upper.contains("LA3B") || upper.contains("LA-3B") || upper.contains("LT3B") || upper.contains("LT-3B") || upper.contains("LT-IIIB") -> "LT-IIIB"
            upper.contains("LA4") || upper.contains("LA-4") || upper.contains("LT4") || upper.contains("LT-4") || upper.contains("LT-IV") -> "LT-IV"
            upper.contains("LA5") || upper.contains("LA-5") || upper.contains("LT5") || upper.contains("LT-5") || upper.contains("LT-V") -> "LT-V"
            codeToken.isNotBlank() -> codeToken.replace("LA-", "LT-", ignoreCase = true).replace("LA", "LT-", ignoreCase = true).replace("TANGEDCO", "", ignoreCase = true).trim()
            else -> "LT-IA"
        }

        val normalizedName = when {
            upper.contains("DOMESTIC") -> "Domestic"
            upper.contains("COMMERCIAL") -> "Commercial"
            upper.contains("AGRICULTURE") -> "Agriculture"
            upper.contains("INDUSTRIAL") -> "Industrial"
            tokens.size > 1 -> {
                val candidateName = tokens.firstOrNull { it != codeToken }.orEmpty()
                if (candidateName.isNotBlank() && !candidateName.equals("TANGEDCO", ignoreCase = true)) {
                    candidateName.lowercase(Locale.US).replaceFirstChar { it.uppercaseChar() }
                } else "Domestic"
            }
            normalizedCode.startsWith("LT-I") -> "Domestic"
            normalizedCode.startsWith("LT-II") -> "Commercial"
            normalizedCode.startsWith("LT-III") -> "Industrial"
            normalizedCode.startsWith("LT-IV") || normalizedCode.startsWith("LT-V") -> "Agriculture"
            else -> "Domestic"
        }

        return NormalizedTariff(
            tariffCode = normalizedCode,
            tariffName = normalizedName,
            displayLabel = "$normalizedCode · $normalizedName"
        )
    }
}
