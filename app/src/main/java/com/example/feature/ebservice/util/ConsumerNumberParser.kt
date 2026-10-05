package com.example.feature.ebservice.util

data class ConsumerNumberDetails(
    val regionCode: String,
    val regionName: String,
    val sectionCode: String,
    val distributionCode: String,
    val serviceNumber: String
)

object ConsumerNumberParser {
    fun parse(consumerNumber: String): Result<ConsumerNumberDetails> {
        val clean = consumerNumber.filter { it.isDigit() }
        if (clean.length !in 10..12) {
            return Result.failure(IllegalArgumentException("Consumer number must be 10 to 12 digits."))
        }

        val regionCode = clean.substring(0, 2)
        val regionName = RegionCodeMapper.getRegionName(regionCode)
            ?: return Result.failure(IllegalArgumentException("Invalid Region Code."))

        val sectionCode = clean.substring(2, 5)
        val distributionCode = clean.substring(5, 8)
        val serviceNumber = clean.substring(8)

        return Result.success(
            ConsumerNumberDetails(
                regionCode = regionCode,
                regionName = regionName,
                sectionCode = sectionCode,
                distributionCode = distributionCode,
                serviceNumber = serviceNumber
            )
        )
    }
}

fun formatConsumerNumber(consumerNumber: String): String {
    val digits = consumerNumber.filter { it.isDigit() }
    return when {
        digits.length == 12 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5, 8)}-${digits.substring(8, 12)}"
        digits.length == 11 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5, 8)}-${digits.substring(8, 11)}"
        digits.length == 10 -> "${digits.substring(0, 2)}-${digits.substring(2, 5)}-${digits.substring(5, 8)}-${digits.substring(8, 10)}"
        else -> consumerNumber
    }
}

fun cleanIndianMobile(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    return when {
        digits.length == 10 -> digits
        digits.length == 11 && digits.startsWith("0") -> digits.substring(1)
        digits.length == 12 && digits.startsWith("91") -> digits.substring(2)
        else -> digits
    }
}

fun isValidIndianMobile(raw: String): Boolean {
    val clean = cleanIndianMobile(raw)
    return clean.length == 10 && clean[0] in '6'..'9'
}

fun formatIndianMobile(raw: String): String {
    val clean = cleanIndianMobile(raw)
    return if (clean.length == 10) {
        "+91 ${clean.substring(0, 5)} ${clean.substring(5)}"
    } else {
        raw
    }
}

