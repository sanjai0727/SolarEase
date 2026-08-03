package com.example.feature.assessment.util

import kotlin.math.round

data class SubsidyResult(
    val eligible: Boolean,
    val amount: Double,
    val reason: String
)

object SubsidyCalculator {
    fun calculate(
        customerType: String,
        systemType: String,
        panelType: String,
        capacityKw: Double
    ): SubsidyResult {
        if (systemType != "On-Grid" || panelType != "DCR") {
            return SubsidyResult(
                eligible = false,
                amount = 0.0,
                reason = "Government subsidy is applicable only for On-Grid solar systems with DCR panels."
            )
        }
        
        var amount = 0.0
        if (capacityKw <= 2.0) {
            amount = capacityKw * 30000.0
        } else if (capacityKw > 2.0 && capacityKw <= 3.0) {
            amount = 60000.0 + ((capacityKw - 2.0) * 18000.0)
        } else if (capacityKw > 3.0) {
            amount = 78000.0
        }
        
        amount = round(amount)
        
        return SubsidyResult(
            eligible = true,
            amount = amount,
            reason = "PM Surya Ghar Subsidy Applied"
        )
    }
}
