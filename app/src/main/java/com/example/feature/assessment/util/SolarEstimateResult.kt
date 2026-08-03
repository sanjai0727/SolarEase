package com.example.feature.assessment.util

data class SolarEstimateResult(
    val recommendedCapacityKW: Double,
    val requiredArea: Double,
    val availableArea: Double,
    val areaStatus: String,
    val systemType: String,
    val mountType: String,
    val panelType: String,
    val customerType: String,
    val baseRatePerKW: Double,
    val panelsCost: Double,
    val mountingCost: Double,
    val labourCost: Double,
    val foundationCost: Double,
    val grossTotal: Double,
    val subsidyEligible: Boolean,
    val subsidyReason: String,
    val subsidyAmount: Double,
    val netPayable: Double,
    val annualSavings: Double,
    val estimatedPaybackYears: Double
)
