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
        if (consumerNumber.length != 12) {
            return Result.failure(IllegalArgumentException("Consumer number must be exactly 12 digits."))
        }
        if (!consumerNumber.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Consumer number must contain only digits."))
        }

        val regionCode = consumerNumber.substring(0, 2)
        val sectionCode = consumerNumber.substring(2, 5)
        val distributionCode = consumerNumber.substring(5, 8)
        val serviceNumber = consumerNumber.substring(8, 12)

        val regionName = RegionCodeMapper.getRegionName(regionCode)
            ?: return Result.failure(IllegalArgumentException("Invalid Region Code."))

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
