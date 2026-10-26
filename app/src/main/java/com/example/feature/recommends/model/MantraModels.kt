package com.example.feature.recommends.model

import com.example.feature.home.ui.modules.BillingCycleComparison
import com.example.feature.home.ui.modules.BillingCycleItem

data class MantraItem(
    val id: Int,
    val number: String,
    val title: String,
    val description: String,
    val potentialSavings: String? = null,
    val frequencyBadge: String, // "Daily", "Weekly", "Monthly", "One-time"
    val iconEmoji: String,
    val iconBgColorHex: Long,
    val isCheckedIn: Boolean,
    val checkInTimestamp: Long? = null,
    val checkInTimeDisplay: String? = null,
    val isOneTimeAction: Boolean = false
)

data class BillingCycleSavings(
    val previousUnits: Double,
    val previousPeriod: String, // e.g. "Apr–Jun"
    val currentUnits: Double,
    val currentPeriod: String, // e.g. "Jun–Aug"
    val savedUnits: Double,
    val savedCredits: Int,
    val hasRealComparison: Boolean = true,
    val isConsumptionIncreased: Boolean = false,
    val differenceUnits: Double = 0.0,
    val previousDays: Int = 60,
    val currentDays: Int = 60,
    val previousDailyRate: Double = 0.0,
    val currentDailyRate: Double = 0.0
)

data class EnergyCreditsData(
    val totalCredits: Int = 0,
    val nextRewardTarget: Int = 200,
    val progressCredits: Int = 0,
    val creditsToUnlock: Int = 200
)

data class RecommendsUiState(
    val userAddress: String = "Valmiki Nagar, Chennai",
    val energyCredits: EnergyCreditsData = EnergyCreditsData(),
    val todayProgressChecked: Int = 0,
    val todayProgressTotal: Int = 9,
    val streakDays: Int = 0,
    val streakState: StreakState = StreakState(),
    val coinBalance: Int = 0,
    val coinTransactions: List<CoinTransaction> = emptyList(),
    val billVerificationResult: BillVerificationResult? = null,
    val billingCycleSavings: BillingCycleSavings = BillingCycleSavings(
        previousUnits = 55.0,
        previousPeriod = "Jun–Aug",
        currentUnits = 42.0,
        currentPeriod = "Aug–Oct",
        savedUnits = 0.0,
        savedCredits = 0,
        isConsumptionIncreased = false,
        differenceUnits = 13.0
    ),
    val mantras: List<MantraItem> = emptyList(),
    val isMantrasExpanded: Boolean = true,
    val availableCycles: List<BillingCycleItem> = emptyList(),
    val selectedCycleIndex: Int = 0,
    val cycleComparison: BillingCycleComparison? = null
)
