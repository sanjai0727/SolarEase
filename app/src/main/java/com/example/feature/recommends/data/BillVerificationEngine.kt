package com.example.feature.recommends.data

import com.example.feature.home.ui.modules.BillingCycleManager
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import com.example.feature.recommends.model.*
import kotlin.math.roundToInt

data class NormalizedBillMetric(
    val billId: String,
    val period: String,
    val units: Double,
    val days: Int,
    val dailyKwh: Double,
    val rawBill: TnebBillRecord
)

/**
 * Engine that automatically evaluates electricity consumption across real TNEB bill records.
 * Compares new bills against historical baselines using normalized daily consumption (kWh/day)
 * and awards coins idempotently based on verified reductions.
 */
object BillVerificationEngine {

    /**
     * Derives a deterministic fallback identifier combining cycle period, date, and units.
     * Prevents collisions between distinct bills sharing the same assessment or entry date.
     */
    fun getFallbackBillId(bill: TnebBillRecord): String {
        val period = BillingCycleManager.formatBiMonthlyPeriod(bill.period, bill.assessmentDate)
            .replace("–", "-")
            .replace("—", "-")
            .replace(" ", "")
            .trim()
        val rawDate = bill.assessmentDate.ifBlank { bill.entryDate }
        val cleanDate = rawDate.replace("/", "").replace("-", "").trim()
        val units = bill.units.trim().ifBlank { bill.kwh.trim().ifBlank { "0" } }
        return "BILL_${period}_${cleanDate}_$units"
    }

    /**
     * Derives a stable, canonical identifier for an electricity bill record.
     * Uses explicit ID when present, otherwise falls back to payment receipt or composite fallback.
     */
    fun getStableBillId(bill: TnebBillRecord): String {
        val explicitId = bill.id.trim()
        if (explicitId.isNotBlank()) {
            return explicitId
        }
        val receipt = bill.receiptNumber.trim()
        if (receipt.isNotBlank()) {
            return receipt
        }
        return getFallbackBillId(bill)
    }

    /**
     * Checks if a bill has already been recognized as historical baseline or previously processed,
     * matching by stable ID, fallback alias ID, or billing period description.
     */
    fun isBillKnownOrProcessed(
        userId: String,
        metric: NormalizedBillMetric,
        rewardStorage: IRewardStorage
    ): Boolean {
        val stableId = metric.billId
        val fallbackId = getFallbackBillId(metric.rawBill)
        val data = rewardStorage.getRewardData(userId)

        // Baseline checks
        if (rewardStorage.isHistoricalBaselineBill(userId, stableId) ||
            rewardStorage.isHistoricalBaselineBill(userId, fallbackId) ||
            data.historicalBaselineBillIds.contains(stableId) ||
            data.historicalBaselineBillIds.contains(fallbackId)
        ) {
            return true
        }

        // Processed bill checks
        if (rewardStorage.hasProcessedBill(userId, stableId) ||
            rewardStorage.hasProcessedBill(userId, fallbackId) ||
            data.processedBillIds.contains(stableId) ||
            data.processedBillIds.contains(fallbackId)
        ) {
            return true
        }

        // Ledger transaction checks by referenceId, idempotencyKey, or period description
        val period = metric.period
        return data.transactions.any { tx ->
            tx.eventType == CoinEventType.BILL_PROCESSED && (
                tx.referenceId == stableId ||
                tx.referenceId == fallbackId ||
                tx.idempotencyKey == "bill_processed_${userId}_$stableId" ||
                tx.idempotencyKey == "bill_processed_${userId}_$fallbackId" ||
                (period.isNotBlank() && tx.description.contains(period))
            )
        }
    }

    /**
     * Initializes pre-existing cached bills as the historical baseline on first app activation.
     * These bills establish the baseline for future comparisons and NEVER receive retroactive coins.
     */
    fun initializeBaselineIfEmpty(
        userId: String,
        billData: TnebConsumerBillData?,
        rewardStorage: IRewardStorage = RewardStorage
    ) {
        val normalizedId = userId.ifBlank { "default_user" }
        val userRewardData = rewardStorage.getRewardData(normalizedId)
        if (userRewardData.isBaselineInitialized) return

        val bills = billData?.bills.orEmpty()
        for (bill in bills) {
            val stableId = getStableBillId(bill)
            val fallbackId = getFallbackBillId(bill)
            if (stableId.isNotBlank()) {
                rewardStorage.markBillAsHistoricalBaseline(normalizedId, stableId)
            }
            if (fallbackId.isNotBlank() && fallbackId != stableId) {
                rewardStorage.markBillAsHistoricalBaseline(normalizedId, fallbackId)
            }
        }
        rewardStorage.markBaselineInitialized(normalizedId)
        rewardStorage.saveVerificationResult(
            normalizedId,
            BillVerificationResult(
                status = BillVerificationStatus.WAITING_FOR_BILL,
                baselineBillCount = bills.size,
                userMessage = if (bills.isNotEmpty()) {
                    "Historical baseline established from ${bills.size} bill(s). Waiting for new bill data."
                } else {
                    "Waiting for new bill data"
                }
            )
        )
    }

    /**
     * Evaluates electricity consumption across all available bill records.
     * Newly received bills (not marked as baseline and not previously processed)
     * are evaluated against the historical baseline to award coins idempotently.
     */
    fun evaluateBills(
        userId: String,
        billData: TnebConsumerBillData?,
        rewardStorage: IRewardStorage = RewardStorage
    ): BillVerificationResult {
        val normalizedId = userId.ifBlank { "default_user" }
        val rawBills = billData?.bills.orEmpty()

        if (rawBills.isEmpty()) {
            val existing = rewardStorage.getVerificationResult(normalizedId)
            return existing ?: BillVerificationResult(
                status = BillVerificationStatus.WAITING_FOR_BILL,
                userMessage = "Waiting for new bill data"
            )
        }

        // 1. Ensure initial baseline is registered if this is first launch
        initializeBaselineIfEmpty(normalizedId, billData, rewardStorage)

        // TNEB bills are listed newest first; chronological order is oldest to newest
        val chronological = rawBills.reversed()

        // Normalize each bill in chronological order
        val normalizedList = mutableListOf<NormalizedBillMetric>()
        for (i in chronological.indices) {
            val bill = chronological[i]
            val priorBill = if (i > 0) chronological[i - 1] else null
            val stableId = getStableBillId(bill)
            val units = BillingCycleManager.extractCycleUnits(bill, priorBill)
            val days = BillingCycleManager.calculateCycleDays(bill, priorBill)
            val dailyRate = if (days > 0) units / days else 0.0

            val periodLabel = BillingCycleManager.formatBiMonthlyPeriod(bill.period, bill.assessmentDate)
            normalizedList.add(
                NormalizedBillMetric(
                    billId = stableId,
                    period = periodLabel,
                    units = units,
                    days = days,
                    dailyKwh = dailyRate,
                    rawBill = bill
                )
            )
        }

        // 2. Identify newly received bills that have not been processed and are not baseline
        val newBills = normalizedList.filter { metric ->
            val isKnown = isBillKnownOrProcessed(normalizedId, metric, rewardStorage)
            if (isKnown) {
                // Link new stable ID or fallback ID if not yet recorded in processedBillIds
                if (!rewardStorage.hasProcessedBill(normalizedId, metric.billId)) {
                    rewardStorage.markBillProcessed(normalizedId, metric.billId)
                }
                val fallbackId = getFallbackBillId(metric.rawBill)
                if (fallbackId != metric.billId && !rewardStorage.hasProcessedBill(normalizedId, fallbackId)) {
                    rewardStorage.markBillProcessed(normalizedId, fallbackId)
                }
            }
            !isKnown
        }

        if (newBills.isEmpty()) {
            val savedResult = rewardStorage.getVerificationResult(normalizedId)
            return savedResult ?: BillVerificationResult(
                status = BillVerificationStatus.WAITING_FOR_BILL,
                baselineBillCount = normalizedList.size,
                userMessage = "All available bills processed. Waiting for next billing cycle."
            )
        }

        // 3. Process new bills in chronological sequence
        var latestResult = BillVerificationResult()

        for (newMetric in newBills) {
            val candidateIdx = normalizedList.indexOfFirst { it.billId == newMetric.billId }
            val precedingMetrics = if (candidateIdx > 0) normalizedList.subList(0, candidateIdx) else emptyList()

            // Validate candidate bill consumption data
            if (newMetric.units <= 0.0 || newMetric.days <= 0) {
                latestResult = BillVerificationResult(
                    status = BillVerificationStatus.INVALID_BILL,
                    billId = newMetric.billId,
                    billPeriod = newMetric.period,
                    userMessage = "Bill record has invalid or missing consumption units."
                )
                rewardStorage.saveVerificationResult(normalizedId, latestResult)
                continue
            }

            // Filter valid preceding baseline bills
            val validPreceding = precedingMetrics.filter { it.units > 0.0 && it.days > 0 && it.dailyKwh > 0.0 }
            val baselineBills = validPreceding.takeLast(3)

            if (baselineBills.isEmpty()) {
                // Minimum 1 prior bill required for comparison
                // Award 10 coins for valid newly received bill processing
                val tx = rewardStorage.recordBillProcessingReward(
                    userId = normalizedId,
                    billId = newMetric.billId,
                    period = newMetric.period
                )
                val earnedProcessCoins = tx?.amount ?: 0
                val fallbackIdEmpty = getFallbackBillId(newMetric.rawBill)
                if (fallbackIdEmpty != newMetric.billId) {
                    rewardStorage.markBillProcessed(normalizedId, fallbackIdEmpty)
                }

                latestResult = BillVerificationResult(
                    status = BillVerificationStatus.INSUFFICIENT_HISTORY,
                    billId = newMetric.billId,
                    billPeriod = newMetric.period,
                    currentDailyKwh = newMetric.dailyKwh,
                    baselineDailyKwh = 0.0,
                    reductionPercent = 0.0,
                    baselineBillCount = 0,
                    billProcessedRewardEarned = earnedProcessCoins,
                    reductionBonusEarned = 0,
                    userMessage = "Not enough bill history to verify yet."
                )
                rewardStorage.saveVerificationResult(normalizedId, latestResult)
            } else {
                // Calculate baseline using average daily kWh of up to 3 preceding bills
                val baselineDailyKwh = baselineBills.map { it.dailyKwh }.average()
                val currentDailyKwh = newMetric.dailyKwh

                // Award 10 coins for newly received bill processing
                val processTx = rewardStorage.recordBillProcessingReward(
                    userId = normalizedId,
                    billId = newMetric.billId,
                    period = newMetric.period
                )
                val earnedProcessCoins = processTx?.amount ?: 0
                val fallbackIdValid = getFallbackBillId(newMetric.rawBill)
                if (fallbackIdValid != newMetric.billId) {
                    rewardStorage.markBillProcessed(normalizedId, fallbackIdValid)
                }

                if (baselineDailyKwh > 0.0) {
                    val reductionPercent = ((baselineDailyKwh - currentDailyKwh) / baselineDailyKwh) * 100.0

                    if (reductionPercent >= RewardConfig.REDUCTION_TIER_1_MIN) {
                        val bonusCoins = RewardConfig.getReductionBonus(reductionPercent)
                        rewardStorage.recordReductionBonusReward(
                            userId = normalizedId,
                            billId = newMetric.billId,
                            period = newMetric.period,
                            reductionPercent = reductionPercent,
                            bonusAmount = bonusCoins
                        )

                        latestResult = BillVerificationResult(
                            status = BillVerificationStatus.VERIFIED_REDUCTION,
                            billId = newMetric.billId,
                            billPeriod = newMetric.period,
                            currentDailyKwh = currentDailyKwh,
                            baselineDailyKwh = baselineDailyKwh,
                            reductionPercent = reductionPercent,
                            baselineBillCount = baselineBills.size,
                            billProcessedRewardEarned = earnedProcessCoins,
                            reductionBonusEarned = bonusCoins,
                            userMessage = "Your latest bill shows a ${reductionPercent.roundToInt()}% reduction in average daily electricity use. You earned $bonusCoins bonus coins."
                        )
                    } else {
                        // Reduction below 5% or increase
                        latestResult = BillVerificationResult(
                            status = BillVerificationStatus.NO_QUALIFYING_REDUCTION,
                            billId = newMetric.billId,
                            billPeriod = newMetric.period,
                            currentDailyKwh = currentDailyKwh,
                            baselineDailyKwh = baselineDailyKwh,
                            reductionPercent = reductionPercent,
                            baselineBillCount = baselineBills.size,
                            billProcessedRewardEarned = earnedProcessCoins,
                            reductionBonusEarned = 0,
                            userMessage = if (reductionPercent > 0.0) {
                                "Electricity consumption decreased by ${reductionPercent.roundToInt()}%, which is below the 5% reward threshold."
                            } else {
                                "Daily consumption was higher than the previous baseline. Keep following the mantras to save energy!"
                            }
                        )
                    }
                } else {
                    latestResult = BillVerificationResult(
                        status = BillVerificationStatus.PROCESSING_ERROR,
                        billId = newMetric.billId,
                        billPeriod = newMetric.period,
                        userMessage = "Baseline daily consumption was not greater than zero."
                    )
                }

                rewardStorage.saveVerificationResult(normalizedId, latestResult)
            }
        }

        return latestResult
    }
}
