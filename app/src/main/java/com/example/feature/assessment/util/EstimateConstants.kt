package com.example.feature.assessment.util

object EstimateConstants {
    const val DIVISOR = 120.0
    const val AREA_PER_KW = 10.0
    
    // Authoritative Solar Sizing Constants (MNRE Benchmark & IEC 61724 Standard)
    /** National average daily solar irradiance benchmark for residential rooftop systems in India (h/day). */
    const val PEAK_SUN_HOURS = 4.5
    /** System performance ratio accounting for inverter, thermal, wiring, and soiling derate losses (~79%). */
    const val SYSTEM_PERFORMANCE_RATIO = 0.79
    /** Standard billing cycle month basis for monthly solar generation calculations. */
    const val DAYS_PER_MONTH = 30.0
    
    const val BASE_RATE_ON_GRID = 55000.0
    const val BASE_RATE_OFF_GRID = 70000.0
    const val BASE_RATE_HYBRID = 85000.0
    
    const val PANELS_INVERTER_PERCENTAGE = 0.70
    const val MOUNTING_WIRING_PERCENTAGE = 0.18
    const val INSTALLATION_LABOUR_PERCENTAGE = 0.12
    
    const val GROUND_MOUNT_ADDITIONAL_COST_PER_KW = 5000.0
    
    const val TARIFF_PER_KWH = 7.0
    
    // PM Surya Ghar Subsidy
    const val SUBSIDY_TIER_1_KW = 1.0
    const val SUBSIDY_TIER_1_AMOUNT = 30000.0
    
    const val SUBSIDY_TIER_2_KW = 2.0
    const val SUBSIDY_TIER_2_AMOUNT = 60000.0
    
    const val SUBSIDY_TIER_3_AMOUNT = 78000.0
}
