package com.example.feature.assessment.util

import kotlin.math.round

object EstimateCalculator {
    fun calculateRecommendedSystemSize(monthlyConsumptionKwh: Double): Double {
        val size = monthlyConsumptionKwh / EstimateConstants.DIVISOR
        return round(size * 10) / 10.0 // Round to 1 decimal place
    }

    fun calculateRequiredArea(recommendedSystemSizeKw: Double): Double {
        return recommendedSystemSizeKw * EstimateConstants.AREA_PER_KW
    }
    
    fun calculateCostEstimate(
        monthlyConsumptionKwh: Double,
        availableAreaSqM: Double,
        mountType: String,
        systemType: String,
        panelType: String,
        customerType: String
    ): SolarEstimateResult {
        val capacity = calculateRecommendedSystemSize(monthlyConsumptionKwh)
        val requiredArea = calculateRequiredArea(capacity)
        
        val areaStatus = if (requiredArea <= availableAreaSqM) "Suitable" else "Insufficient Area"
        
        var baseRate = 0.0
        if (panelType == "DCR") {
            baseRate = if (mountType == "Ground") 62000.0 else 55000.0
        } else {
            baseRate = if (mountType == "Ground") 54000.0 else 48000.0
        }
        
        var additionalCost = 0.0
        if (systemType == "Off-Grid") {
            additionalCost = 25000.0
        } else if (systemType == "Hybrid") {
            additionalCost = 20000.0
        }
        
        val finalCostPerKw = baseRate + additionalCost
        val grossTotal = capacity * finalCostPerKw
        
        val panelsCost = grossTotal * EstimateConstants.PANELS_INVERTER_PERCENTAGE
        val mountingCost = grossTotal * EstimateConstants.MOUNTING_WIRING_PERCENTAGE
        val labourCost = grossTotal * EstimateConstants.INSTALLATION_LABOUR_PERCENTAGE
        
        val subsidyResult = SubsidyCalculator.calculate(customerType, systemType, panelType, capacity)
        
        var netPayable = grossTotal - subsidyResult.amount
        if (netPayable < 0) {
            netPayable = 0.0
        }
        
        val annualSavings = monthlyConsumptionKwh * 12 * EstimateConstants.TARIFF_PER_KWH
        
        var paybackYears = 0.0
        if (annualSavings > 0) {
            val payback = netPayable / annualSavings
            paybackYears = round(payback * 10) / 10.0
        }
        
        return SolarEstimateResult(
            recommendedCapacityKW = capacity,
            requiredArea = requiredArea,
            availableArea = availableAreaSqM,
            areaStatus = areaStatus,
            systemType = systemType,
            mountType = mountType,
            panelType = panelType,
            customerType = customerType,
            baseRatePerKW = finalCostPerKw,
            panelsCost = panelsCost,
            mountingCost = mountingCost,
            labourCost = labourCost,
            foundationCost = 0.0,
            grossTotal = grossTotal,
            subsidyEligible = subsidyResult.eligible,
            subsidyReason = subsidyResult.reason,
            subsidyAmount = subsidyResult.amount,
            netPayable = netPayable,
            annualSavings = annualSavings,
            estimatedPaybackYears = paybackYears
        )
    }
}
