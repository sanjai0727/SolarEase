package com.example.feature.recommends.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.session.SecurePreferencesHelper
import com.example.feature.recommends.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

interface IRewardStorage {
    fun getRewardData(userId: String): UserRewardData
    fun recordDailyActivity(
        userId: String,
        activityId: String,
        date: String = RewardStorage.todayDate()
    ): Pair<StreakState, List<CoinTransaction>>
    fun recordBillProcessingReward(
        userId: String,
        billId: String,
        period: String
    ): CoinTransaction?
    fun recordReductionBonusReward(
        userId: String,
        billId: String,
        period: String,
        reductionPercent: Double,
        bonusAmount: Int
    ): CoinTransaction?
    fun markBillAsHistoricalBaseline(userId: String, billId: String)
    fun isHistoricalBaselineBill(userId: String, billId: String): Boolean
    fun isBaselineInitialized(userId: String): Boolean
    fun markBaselineInitialized(userId: String)
    fun hasProcessedBill(userId: String, billId: String): Boolean
    fun markBillProcessed(userId: String, billId: String)
    fun saveVerificationResult(userId: String, result: BillVerificationResult)
    fun getVerificationResult(userId: String): BillVerificationResult?
    fun clear(userId: String? = null)
}

/**
 * Singleton repository managing persistent, encrypted storage of SolarEase streaks,
 * coin transactions, and bill verification states.
 */
object RewardStorage : IRewardStorage {
    private const val TAG = "RewardStorage"
    private const val PREFS_NAME = "SolarEaseSecureRewardPrefs"
    private const val KEY_PREFIX_USER = "user_reward_data_"

    private var appContext: Context? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // In-memory cache for fast reactive access and unit test support
    private val memoryStore = mutableMapOf<String, UserRewardData>()
    private val stateFlows = mutableMapOf<String, MutableStateFlow<UserRewardData>>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getPrefs(context: Context? = null): SharedPreferences? {
        val ctx = context?.applicationContext ?: appContext ?: return null
        return SecurePreferencesHelper.getEncryptedPrefs(ctx, PREFS_NAME)
    }

    fun todayDate(referenceDate: Date = Date()): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(referenceDate)
    }

    fun previousCalendarDay(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.DAY_OF_YEAR, -1)
            sdf.format(cal.time)
        } catch (_: Exception) {
            ""
        }
    }

    fun getRewardFlow(userId: String): StateFlow<UserRewardData> {
        val normalizedId = userId.ifBlank { "default_user" }
        return stateFlows.getOrPut(normalizedId) {
            MutableStateFlow(getRewardData(normalizedId))
        }.asStateFlow()
    }

    override fun getRewardData(userId: String): UserRewardData {
        val normalizedId = userId.ifBlank { "default_user" }
        val cached = memoryStore[normalizedId]
        if (cached != null) {
            return cached
        }

        val prefs = getPrefs()
        if (prefs != null) {
            try {
                val raw = prefs.getString("$KEY_PREFIX_USER$normalizedId", null)
                if (!raw.isNullOrBlank()) {
                    val decoded = json.decodeFromString<UserRewardData>(raw)
                    memoryStore[normalizedId] = decoded
                    return decoded
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode reward data for user $normalizedId, starting clean", e)
            }
        }

        val fresh = UserRewardData(userId = normalizedId)
        memoryStore[normalizedId] = fresh
        return fresh
    }

    private fun persist(userId: String, data: UserRewardData) {
        val normalizedId = userId.ifBlank { "default_user" }
        memoryStore[normalizedId] = data
        stateFlows[normalizedId]?.value = data

        val prefs = getPrefs()
        if (prefs != null) {
            try {
                val serialized = json.encodeToString(data)
                prefs.edit().putString("$KEY_PREFIX_USER$normalizedId", serialized).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist reward data for user $normalizedId", e)
            }
        }
    }

    override fun recordDailyActivity(
        userId: String,
        activityId: String,
        date: String
    ): Pair<StreakState, List<CoinTransaction>> {
        val normalizedId = userId.ifBlank { "default_user" }
        val currentData = getRewardData(normalizedId)
        val currentStreak = currentData.streakState
        val newTransactions = mutableListOf<CoinTransaction>()

        val idempotencyKeyDaily = "daily_${normalizedId}_$date"
        val hasAlreadyQualifiedToday = currentStreak.lastQualifyingDate == date ||
                currentStreak.completedDates.contains(date) ||
                currentData.transactions.any { it.idempotencyKey == idempotencyKeyDaily }

        if (hasAlreadyQualifiedToday) {
            // Already qualified for today. Idempotently return without modifying streak or duplicating rewards
            return Pair(currentStreak, emptyList())
        }

        val yesterday = previousCalendarDay(date)
        val isConsecutive = currentStreak.lastQualifyingDate != null && currentStreak.lastQualifyingDate == yesterday

        val nextStreakCount = if (isConsecutive) {
            currentStreak.currentStreak + 1
        } else {
            1
        }

        val nextLongestStreak = max(currentStreak.longestStreak, nextStreakCount)
        val updatedCompletedDates = (currentStreak.completedDates + date).distinct().sorted()

        // 1. Issue Daily Activity Reward
        val dailyTx = CoinTransaction(
            transactionId = UUID.randomUUID().toString(),
            userId = normalizedId,
            eventType = CoinEventType.DAILY_ACTIVITY,
            amount = RewardConfig.DAILY_ACTIVITY_COINS,
            timestamp = System.currentTimeMillis(),
            idempotencyKey = idempotencyKeyDaily,
            description = "Daily energy-saving check-in ($date)",
            referenceId = activityId
        )
        newTransactions.add(dailyTx)

        // 2. Evaluate 7-day streak milestone reward (Days 7, 14, 21...)
        val claimedMilestones = if (isConsecutive) {
            currentStreak.claimedMilestones.toMutableList()
        } else {
            mutableListOf()
        }
        val isMilestoneMultiple = nextStreakCount > 0 && nextStreakCount % RewardConfig.STREAK_CYCLE_DAYS == 0
        val isMilestoneAlreadyClaimed = claimedMilestones.contains(nextStreakCount)

        if (isMilestoneMultiple && !isMilestoneAlreadyClaimed) {
            claimedMilestones.add(nextStreakCount)
            val milestoneKey = "milestone_${normalizedId}_${date}_$nextStreakCount"
            val milestoneTx = CoinTransaction(
                transactionId = UUID.randomUUID().toString(),
                userId = normalizedId,
                eventType = CoinEventType.STREAK_MILESTONE,
                amount = RewardConfig.STREAK_MILESTONE_COINS,
                timestamp = System.currentTimeMillis(),
                idempotencyKey = milestoneKey,
                description = "Completed $nextStreakCount-day streak milestone!",
                referenceId = "milestone_$nextStreakCount"
            )
            newTransactions.add(milestoneTx)
        }

        val newMilestoneCycle = if (nextStreakCount <= 0) 1 else ((nextStreakCount - 1) / RewardConfig.STREAK_CYCLE_DAYS) + 1

        val updatedStreakState = currentStreak.copy(
            currentStreak = nextStreakCount,
            longestStreak = nextLongestStreak,
            lastQualifyingDate = date,
            completedDates = updatedCompletedDates,
            currentMilestoneCycle = newMilestoneCycle,
            claimedMilestones = claimedMilestones
        )

        val updatedData = currentData.copy(
            streakState = updatedStreakState,
            transactions = currentData.transactions + newTransactions
        )
        persist(normalizedId, updatedData)

        return Pair(updatedStreakState, newTransactions)
    }

    override fun recordBillProcessingReward(
        userId: String,
        billId: String,
        period: String
    ): CoinTransaction? {
        val normalizedId = userId.ifBlank { "default_user" }
        val currentData = getRewardData(normalizedId)

        if (currentData.historicalBaselineBillIds.contains(billId)) {
            // Historical baseline bills never receive processing rewards
            return null
        }

        val idempotencyKey = "bill_processed_${normalizedId}_$billId"
        val alreadyProcessed = currentData.processedBillIds.contains(billId) ||
            currentData.transactions.any {
                it.eventType == CoinEventType.BILL_PROCESSED && (
                    it.idempotencyKey == idempotencyKey ||
                    it.referenceId == billId ||
                    (period.isNotBlank() && it.description.contains(period))
                )
            }

        if (alreadyProcessed) {
            // Already processed
            return null
        }

        val tx = CoinTransaction(
            transactionId = UUID.randomUUID().toString(),
            userId = normalizedId,
            eventType = CoinEventType.BILL_PROCESSED,
            amount = RewardConfig.BILL_PROCESSED_COINS,
            timestamp = System.currentTimeMillis(),
            idempotencyKey = idempotencyKey,
            description = "Processed electricity bill ($period)",
            referenceId = billId
        )

        val updatedData = currentData.copy(
            transactions = currentData.transactions + tx,
            processedBillIds = (currentData.processedBillIds + billId).distinct()
        )
        persist(normalizedId, updatedData)
        return tx
    }

    override fun recordReductionBonusReward(
        userId: String,
        billId: String,
        period: String,
        reductionPercent: Double,
        bonusAmount: Int
    ): CoinTransaction? {
        if (bonusAmount <= 0) return null

        val normalizedId = userId.ifBlank { "default_user" }
        val currentData = getRewardData(normalizedId)

        if (currentData.historicalBaselineBillIds.contains(billId)) {
            return null
        }

        val idempotencyKey = "bill_reduction_${normalizedId}_$billId"
        val alreadyAwarded = currentData.transactions.any {
            it.eventType == CoinEventType.VERIFIED_CONSUMPTION_REDUCTION && (
                it.idempotencyKey == idempotencyKey ||
                it.referenceId == billId ||
                (period.isNotBlank() && it.description.contains(period))
            )
        }

        if (alreadyAwarded) {
            // Already awarded reduction bonus for this bill
            return null
        }

        val tx = CoinTransaction(
            transactionId = UUID.randomUUID().toString(),
            userId = normalizedId,
            eventType = CoinEventType.VERIFIED_CONSUMPTION_REDUCTION,
            amount = bonusAmount,
            timestamp = System.currentTimeMillis(),
            idempotencyKey = idempotencyKey,
            description = "Verified ${reductionPercent.roundToInt()}% consumption reduction ($period)",
            referenceId = billId
        )

        val updatedData = currentData.copy(
            transactions = currentData.transactions + tx
        )
        persist(normalizedId, updatedData)
        return tx
    }

    override fun markBillAsHistoricalBaseline(userId: String, billId: String) {
        val normalizedId = userId.ifBlank { "default_user" }
        val currentData = getRewardData(normalizedId)
        if (!currentData.historicalBaselineBillIds.contains(billId)) {
            val updated = currentData.copy(
                historicalBaselineBillIds = (currentData.historicalBaselineBillIds + billId).distinct()
            )
            persist(normalizedId, updated)
        }
    }

    override fun isHistoricalBaselineBill(userId: String, billId: String): Boolean {
        val normalizedId = userId.ifBlank { "default_user" }
        return getRewardData(normalizedId).historicalBaselineBillIds.contains(billId)
    }

    override fun isBaselineInitialized(userId: String): Boolean {
        val normalizedId = userId.ifBlank { "default_user" }
        return getRewardData(normalizedId).isBaselineInitialized
    }

    override fun markBaselineInitialized(userId: String) {
        val normalizedId = userId.ifBlank { "default_user" }
        val currentData = getRewardData(normalizedId)
        if (!currentData.isBaselineInitialized) {
            val updated = currentData.copy(isBaselineInitialized = true)
            persist(normalizedId, updated)
        }
    }

    override fun hasProcessedBill(userId: String, billId: String): Boolean {
        val normalizedId = userId.ifBlank { "default_user" }
        val data = getRewardData(normalizedId)
        val key = "bill_processed_${normalizedId}_$billId"
        return data.processedBillIds.contains(billId) ||
            data.transactions.any { it.idempotencyKey == key || it.referenceId == billId }
    }

    override fun markBillProcessed(userId: String, billId: String) {
        val normalizedId = userId.ifBlank { "default_user" }
        val currentData = getRewardData(normalizedId)
        if (!currentData.processedBillIds.contains(billId)) {
            val updated = currentData.copy(
                processedBillIds = (currentData.processedBillIds + billId).distinct()
            )
            persist(normalizedId, updated)
        }
    }

    override fun saveVerificationResult(userId: String, result: BillVerificationResult) {
        val normalizedId = userId.ifBlank { "default_user" }
        val currentData = getRewardData(normalizedId)
        val updated = currentData.copy(latestVerificationResult = result)
        persist(normalizedId, updated)
    }

    override fun getVerificationResult(userId: String): BillVerificationResult? {
        val normalizedId = userId.ifBlank { "default_user" }
        return getRewardData(normalizedId).latestVerificationResult
    }

    override fun clear(userId: String?) {
        if (userId != null) {
            val normalizedId = userId.ifBlank { "default_user" }
            memoryStore.remove(normalizedId)
            stateFlows.remove(normalizedId)
            val prefs = getPrefs()
            prefs?.edit()?.remove("$KEY_PREFIX_USER$normalizedId")?.apply()
        } else {
            memoryStore.clear()
            stateFlows.clear()
            val prefs = getPrefs()
            prefs?.edit()?.clear()?.apply()
        }
    }
}
