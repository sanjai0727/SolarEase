package com.example.feature.ebservice.util

object RegionCodeMapper {
    private val regions = mapOf(
        "01" to "Chennai North",
        "02" to "Villupuram",
        "03" to "Coimbatore",
        "04" to "Erode",
        "05" to "Madurai",
        "06" to "Trichy",
        "07" to "Tirunelveli",
        "08" to "Vellore",
        "09" to "Chennai South"
    )

    fun getRegionName(code: String): String? {
        return regions[code]
    }
}
