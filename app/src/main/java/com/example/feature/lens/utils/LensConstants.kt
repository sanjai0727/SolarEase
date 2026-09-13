package com.example.feature.lens.utils

object LensConstants {
    @Deprecated(
        message = "Use ApplianceMaster.authoritativeTariffRate or calculateMonthlyCost directly",
        replaceWith = ReplaceWith("ApplianceMaster.authoritativeTariffRate.toFloat()", "com.example.feature.lens.utils.ApplianceMaster")
    )
    val DEFAULT_TARIFF_RATE: Float
        get() = ApplianceMaster.authoritativeTariffRate.toFloat()
}
