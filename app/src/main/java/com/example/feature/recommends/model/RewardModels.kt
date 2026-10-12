package com.example.feature.recommends.model

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.max

/**
 * Event categories that can generate coin transactions.
 */
@Serializable
enum class CoinEventType {
    DAILY_ACTIVITY,
    STREAK_MILESTONE,
    BILL_PROCESSED,
    VERIFIED_CONSUMPTION_REDUCTION
}

/**
 * Immutable ledger entry representing an individual coin credit or reward event.
 */
@Serializable
data class CoinTransaction(
    val transactionId: String = UUID.randomUUID().toString(),
    val userId: String,
    val eventType: CoinEventType,
    val amount: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val idempotencyKey: String,
    val description: String,
    val referenceId: String? = null
)

/**
 * State representing a user's daily energy-saving streak and progress toward 7-day milestones.
 */
@Serializable
data class StreakState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastQualifyingDate: String? = null,
    val completedDates: List<String> = emptyList(),
    val currentMilestoneCycle: Int = 1,
    val claimedMilestones: List<Int> = emptyList()
) {
    /**
     * Number of completed days (1 to 7) within the active 7-day milestone cycle.
     */
    val daysInCurrentCycle: Int
        get() = when {
            currentStreak <= 0 -> 0
            currentStreak % 7 == 0 -> 7
            else -> currentStreak % 7
        }

    /**
     * Target day for the current milestone (e.g., 7, 14, 21...).
     */
    val nextMilestoneTarget: Int
        get() = if (daysInCurrentCycle == 7) (currentMilestoneCycle + 1) * 7 else max(7, currentMilestoneCycle * 7)

    /**
     * Normalized progress (0.0f to 1.0f) towards the next 7-day milestone.
     */
    val progressTowardsNextMilestone: Float
        get() = (daysInCurrentCycle.toFloat() / 7f).coerceIn(0f, 1f)

    /**
     * True if the current streak reaches a multiple of 7 that has not yet been rewarded.
     */
    val isMilestonePending: Boolean
        get() = currentStreak > 0 && currentStreak % 7 == 0 && !claimedMilestones.contains(currentStreak)
}

/**
 * Central configuration and business rules for SolarEase Coin rewards.
 * Keeping amounts and thresholds centralized avoids hardcoding scattered values.
 */
object RewardConfig {
    const val DAILY_ACTIVITY_COINS = 1
    const val STREAK_MILESTONE_COINS = 5
    const val STREAK_CYCLE_DAYS = 7
    const val BILL_PROCESSED_COINS = 10

    const val REDUCTION_TIER_1_MIN = 5.0
    const val REDUCTION_TIER_1_COINS = 15

    const val REDUCTION_TIER_2_MIN = 10.0
    const val REDUCTION_TIER_2_COINS = 30

    const val REDUCTION_TIER_3_MIN = 20.0
    const val REDUCTION_TIER_3_COINS = 50

    /**
     * Evaluates verified reduction percentage against mutually exclusive tiers.
     * Returns the highest qualifying bonus amount, or 0 if below 5%.
     */
    fun getReductionBonus(reductionPercent: Double): Int {
        return when {
            reductionPercent >= REDUCTION_TIER_3_MIN -> REDUCTION_TIER_3_COINS
            reductionPercent >= REDUCTION_TIER_2_MIN -> REDUCTION_TIER_2_COINS
            reductionPercent >= REDUCTION_TIER_1_MIN -> REDUCTION_TIER_1_COINS
            else -> 0
        }
    }
}

/**
 * Verification state for electricity bill consumption analysis.
 */
@Serializable
enum class BillVerificationStatus {
    WAITING_FOR_BILL,
    PROCESSING,
    VERIFIED_REDUCTION,
    NO_QUALIFYING_REDUCTION,
    INSUFFICIENT_HISTORY,
    INVALID_BILL,
    PROCESSING_ERROR
}

/**
 * Result data model detailing consumption reduction calculations and earned rewards.
 */
@Serializable
data class BillVerificationResult(
    val status: BillVerificationStatus = BillVerificationStatus.WAITING_FOR_BILL,
    val billId: String = "",
    val billPeriod: String = "",
    val currentDailyKwh: Double = 0.0,
    val baselineDailyKwh: Double = 0.0,
    val reductionPercent: Double = 0.0,
    val baselineBillCount: Int = 0,
    val billProcessedRewardEarned: Int = 0,
    val reductionBonusEarned: Int = 0,
    val userMessage: String = "Waiting for new bill data"
)

/**
 * Top-level container representing an authenticated user's complete reward state.
 */
@Serializable
data class UserRewardData(
    val userId: String,
    val streakState: StreakState = StreakState(),
    val transactions: List<CoinTransaction> = emptyList(),
    val processedBillIds: List<String> = emptyList(),
    val historicalBaselineBillIds: List<String> = emptyList(),
    val latestVerificationResult: BillVerificationResult? = null,
    val isBaselineInitialized: Boolean = false
) {
    /**
     * Authoritative coin balance derived directly from the transaction ledger.
     */
    val balance: Int
        get() = transactions.sumOf { it.amount }
}
