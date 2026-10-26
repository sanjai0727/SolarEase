package com.example.feature.recommends.data

import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import com.example.feature.recommends.model.BillVerificationStatus
import com.example.feature.recommends.model.CoinEventType
import com.example.feature.recommends.model.RewardConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BillVerificationEngineTest {

    private val testUserId = "user_tneb_123"
    private val otherUserId = "user_tneb_456"

    @Before
    fun setUp() {
        RewardStorage.clear(null)
    }

    @After
    fun tearDown() {
        RewardStorage.clear(null)
    }

    private fun createBill(
        id: String,
        period: String,
        units: String,
        assessmentDate: String,
        isPaid: Boolean = true
    ): TnebBillRecord {
        return TnebBillRecord(
            id = id,
            period = period,
            units = units,
            kwh = "12000",
            amountDue = "₹ 500",
            dueDate = assessmentDate,
            status = if (isPaid) "PAID" else "UNPAID",
            isPaid = isPaid,
            assessmentDate = assessmentDate
        )
    }

    @Test
    fun testHistoricalCachedBillsReceiveNoRetroactiveRewards() {
        // First activation: User already has 2 cached bills
        val historicalBills = listOf(
            createBill("B-2024-08", "Jun–Aug 2024", "180", "10/08/2024"),
            createBill("B-2024-06", "Apr–Jun 2024", "240", "10/06/2024")
        )
        val initialData = TnebConsumerBillData(consumerNumber = "04001", bills = historicalBills)

        val result = BillVerificationEngine.evaluateBills(testUserId, initialData, RewardStorage)

        assertEquals("Historical baseline established", BillVerificationStatus.WAITING_FOR_BILL, result.status)
        assertEquals(0, result.billProcessedRewardEarned)
        assertEquals(0, result.reductionBonusEarned)

        // Authoritative ledger must remain strictly 0 coins
        val rewardData = RewardStorage.getRewardData(testUserId)
        assertEquals("Historical bills must award 0 coins", 0, rewardData.balance)
        assertTrue(rewardData.transactions.isEmpty())
        assertTrue("Bill B-2024-08 marked as baseline", RewardStorage.isHistoricalBaselineBill(testUserId, "B-2024-08"))
        assertTrue("Bill B-2024-06 marked as baseline", RewardStorage.isHistoricalBaselineBill(testUserId, "B-2024-06"))
    }

    @Test
    fun testNewBillWithNoPriorHistory_AwardsProcessingCoinsOnlyAndShowsInsufficientHistory() {
        // App starts with NO cached bills
        val emptyData = TnebConsumerBillData(consumerNumber = "04001", bills = emptyList())
        BillVerificationEngine.initializeBaselineIfEmpty(testUserId, emptyData, RewardStorage)

        // Newly received first bill
        val firstBill = createBill("B-2024-10", "Aug–Oct 2024", "120", "10/10/2024")
        val billData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(firstBill))

        val result = BillVerificationEngine.evaluateBills(testUserId, billData, RewardStorage)

        assertEquals(BillVerificationStatus.INSUFFICIENT_HISTORY, result.status)
        assertEquals("B-2024-10", result.billId)
        assertEquals(RewardConfig.BILL_PROCESSED_COINS, result.billProcessedRewardEarned)
        assertEquals(0, result.reductionBonusEarned)
        assertEquals("Not enough bill history to verify yet.", result.userMessage)

        val rewardData = RewardStorage.getRewardData(testUserId)
        assertEquals("Should receive exactly 10 bill processing coins", 10, rewardData.balance)
        assertEquals(1, rewardData.transactions.size)
        assertEquals(CoinEventType.BILL_PROCESSED, rewardData.transactions.first().eventType)
    }

    @Test
    fun testBaselineWithOnePriorBill_EvaluatesReductionCorrectly() {
        // 1 historical bill: 240 units / 60 days = 4.0 kWh/day
        val baselineBill = createBill("B-2024-08", "Jun–Aug 2024", "240", "10/08/2024")
        val initialData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(baselineBill))
        BillVerificationEngine.evaluateBills(testUserId, initialData, RewardStorage)

        // New bill arrives: 180 units / 60 days = 3.0 kWh/day
        // Reduction = (4.0 - 3.0) / 4.0 = 25.0% -> Tier 3: 50 coins bonus + 10 processing coins
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "180", "10/10/2024")
        val updatedData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(newBill, baselineBill))

        val result = BillVerificationEngine.evaluateBills(testUserId, updatedData, RewardStorage)

        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, result.status)
        assertEquals("B-2024-10", result.billId)
        assertEquals(1, result.baselineBillCount)
        assertEquals(4.0, result.baselineDailyKwh, 0.01)
        assertEquals(180.0 / 61.0, result.currentDailyKwh, 0.01)
        assertEquals(26.23, result.reductionPercent, 0.1)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(50, result.reductionBonusEarned)

        val rewardData = RewardStorage.getRewardData(testUserId)
        // 10 processing + 50 bonus = 60 coins
        assertEquals(60, rewardData.balance)
        assertEquals(2, rewardData.transactions.size)
    }

    @Test
    fun testBaselineWithTwoPriorBills_CalculatesAverageDailyRate() {
        // Bill 1: 10/04/2024, 240 units / 60 days = 4.0 kWh/day
        // Bill 2: 10/06/2024, 180 units / 61 days (~3.0 kWh/day)
        val bill1 = createBill("B-2024-04", "Feb–Apr 2024", "240", "10/04/2024")
        val bill2 = createBill("B-2024-06", "Apr–Jun 2024", "180", "10/06/2024")

        val initialData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(bill2, bill1))
        BillVerificationEngine.evaluateBills(testUserId, initialData, RewardStorage)

        // New bill arrives: 10/08/2024
        // Baseline = average of bill 1 and bill 2 daily kWh
        val newBill = createBill("B-2024-08", "Jun–Aug 2024", "120", "10/08/2024")
        val updatedData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(newBill, bill2, bill1))

        val result = BillVerificationEngine.evaluateBills(testUserId, updatedData, RewardStorage)

        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, result.status)
        assertEquals(2, result.baselineBillCount)
        assertTrue("Reduction percent should be greater than 20%", result.reductionPercent >= 20.0)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(50, result.reductionBonusEarned)
    }

    @Test
    fun testBaselineWithThreePriorBills_UsesExactlyThreePrecedingBills() {
        val bill1 = createBill("B-2024-02", "Dec–Feb 2024", "300", "10/02/2024")
        val bill2 = createBill("B-2024-04", "Feb–Apr 2024", "270", "10/04/2024")
        val bill3 = createBill("B-2024-06", "Apr–Jun 2024", "240", "10/06/2024")
        val bill4 = createBill("B-2024-08", "Jun–Aug 2024", "210", "10/08/2024")

        // Initialize 4 bills as historical baseline
        val initialData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(bill4, bill3, bill2, bill1))
        BillVerificationEngine.evaluateBills(testUserId, initialData, RewardStorage)

        // New bill arrives
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "180", "10/10/2024")
        val updatedData = TnebConsumerBillData(consumerNumber = "04001", bills = listOf(newBill, bill4, bill3, bill2, bill1))

        val result = BillVerificationEngine.evaluateBills(testUserId, updatedData, RewardStorage)

        // Baseline must use up to 3 preceding bills (bill4, bill3, bill2), not bill1
        assertEquals(3, result.baselineBillCount)
        assertEquals(10, result.billProcessedRewardEarned)
        assertTrue(result.reductionBonusEarned > 0)
    }

    @Test
    fun testReductionTiers_BelowFivePercentAwardsNoBonus() {
        // Baseline: 200 units / 60 days = 3.333 kWh/day
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // New bill: 196 units / 60 days = 3.267 kWh/day (2% reduction, < 5%)
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "196", "10/10/2024")
        val updatedData = TnebConsumerBillData(bills = listOf(newBill, baseline))

        val result = BillVerificationEngine.evaluateBills(testUserId, updatedData, RewardStorage)

        assertEquals(BillVerificationStatus.NO_QUALIFYING_REDUCTION, result.status)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(0, result.reductionBonusEarned)
        val data = RewardStorage.getRewardData(testUserId)
        assertEquals(10, data.balance)
    }

    @Test
    fun testReductionTiers_TierOneFiveToTenPercentAwardsFifteenCoins() {
        // Baseline: 200 units / 60 days
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // New bill: 186 units / 60 days (7% reduction -> Tier 1: 15 coins)
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "186", "10/10/2024")
        val result = BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(newBill, baseline)), RewardStorage)

        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, result.status)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(RewardConfig.REDUCTION_TIER_1_COINS, result.reductionBonusEarned)
        assertEquals(25, RewardStorage.getRewardData(testUserId).balance) // 10 + 15
    }

    @Test
    fun testReductionTiers_TierTwoTenToTwentyPercentAwardsThirtyCoins() {
        // Baseline: 200 units / 60 days
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // New bill: 170 units / 60 days (15% reduction -> Tier 2: 30 coins)
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "170", "10/10/2024")
        val result = BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(newBill, baseline)), RewardStorage)

        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, result.status)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(RewardConfig.REDUCTION_TIER_2_COINS, result.reductionBonusEarned)
        assertEquals(40, RewardStorage.getRewardData(testUserId).balance) // 10 + 30
    }

    @Test
    fun testReductionTiers_TierThreeTwentyPercentOrMoreAwardsFiftyCoins() {
        // Baseline: 200 units / 60 days
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // New bill: 150 units / 60 days (25% reduction -> Tier 3: 50 coins)
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "150", "10/10/2024")
        val result = BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(newBill, baseline)), RewardStorage)

        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, result.status)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(RewardConfig.REDUCTION_TIER_3_COINS, result.reductionBonusEarned)
        assertEquals(60, RewardStorage.getRewardData(testUserId).balance) // 10 + 50
    }

    @Test
    fun testIncreasedConsumptionAwardsZeroBonusCoins() {
        // Baseline: 100 units / 60 days
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "100", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // New bill: 150 units / 60 days (+50% increase)
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "150", "10/10/2024")
        val result = BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(newBill, baseline)), RewardStorage)

        assertEquals(BillVerificationStatus.NO_QUALIFYING_REDUCTION, result.status)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(0, result.reductionBonusEarned)
        assertEquals(10, RewardStorage.getRewardData(testUserId).balance)
    }

    @Test
    fun testInvalidBillConsumptionHandledSafely() {
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // New bill with 0 or negative units
        val invalidBill = createBill("B-2024-10", "Aug–Oct 2024", "0", "10/10/2024")
        val result = BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(invalidBill, baseline)), RewardStorage)

        assertEquals(BillVerificationStatus.INVALID_BILL, result.status)
        assertEquals(0, result.billProcessedRewardEarned)
        assertEquals(0, result.reductionBonusEarned)
        assertEquals(0, RewardStorage.getRewardData(testUserId).balance)
    }

    @Test
    fun testReprocessingAndCacheRefreshIsStrictlyIdempotent() {
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "150", "10/10/2024")
        val data = TnebConsumerBillData(bills = listOf(newBill, baseline))

        // First evaluation
        val result1 = BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)
        assertEquals(60, RewardStorage.getRewardData(testUserId).balance)
        val txCount1 = RewardStorage.getRewardData(testUserId).transactions.size
        assertEquals(2, txCount1)

        // Second evaluation (simulating reopening screen or cache refresh)
        val result2 = BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)
        assertEquals("Balance must not change on re-evaluation", 60, RewardStorage.getRewardData(testUserId).balance)
        assertEquals("Transaction count must remain unchanged", 2, RewardStorage.getRewardData(testUserId).transactions.size)
        assertEquals(result1.billId, result2.billId)
    }

    @Test
    fun testUserIsolationInBillVerification() {
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "150", "10/10/2024")
        val data = TnebConsumerBillData(bills = listOf(newBill, baseline))

        // Process for User 1
        BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)
        assertEquals(60, RewardStorage.getRewardData(testUserId).balance)

        // User 2 has not processed anything yet
        assertEquals(0, RewardStorage.getRewardData(otherUserId).balance)
        assertFalse(RewardStorage.hasProcessedBill(otherUserId, "B-2024-10"))
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // FOCUSED AUDIT SCENARIO TESTS (Scenarios 1 to 7)
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    fun testScenario1_SameBillLoadedAgainAfterAppRestart() {
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "150", "10/10/2024")
        val data = TnebConsumerBillData(bills = listOf(newBill, baseline))

        // Initial evaluation: earns 10 + 50 = 60 coins
        val initialResult = BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)
        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, initialResult.status)
        assertEquals(60, RewardStorage.getRewardData(testUserId).balance)

        // Simulate app restart: serialized and re-retrieved from storage
        val userRewardData = RewardStorage.getRewardData(testUserId)
        assertEquals(60, userRewardData.balance)
        assertTrue(RewardStorage.hasProcessedBill(testUserId, "B-2024-10"))

        // Re-evaluate after restart with same bills
        val reloadResult = BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)
        assertEquals("Balance must remain exactly 60 after restart", 60, RewardStorage.getRewardData(testUserId).balance)
        assertEquals("Transaction count must remain 2", 2, RewardStorage.getRewardData(testUserId).transactions.size)
        assertEquals(initialResult.billId, reloadResult.billId)
    }

    @Test
    fun testScenario2_SameBillReloadedAfterTnebCacheRefresh() {
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // Bill arrives unpaid
        val unpaidBill = createBill("B-2024-10", "Aug–Oct 2024", "150", "10/10/2024", isPaid = false)
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(unpaidBill, baseline)), RewardStorage)
        assertEquals(60, RewardStorage.getRewardData(testUserId).balance)

        // Cache refresh: same bill is now marked PAID with receipt number
        val paidBill = unpaidBill.copy(
            isPaid = true,
            status = "PAID",
            receiptNumber = "RCPT-987654",
            amountPaid = "₹ 500",
            paymentDate = "15/10/2024"
        )
        val refreshedData = TnebConsumerBillData(bills = listOf(paidBill, baseline))

        val resultAfterRefresh = BillVerificationEngine.evaluateBills(testUserId, refreshedData, RewardStorage)
        assertEquals("Coins must remain strictly 60 after TNEB cache refresh", 60, RewardStorage.getRewardData(testUserId).balance)
        assertEquals("No duplicate transactions issued", 2, RewardStorage.getRewardData(testUserId).transactions.size)
    }

    @Test
    fun testScenario3_BillInitiallyIdentifiedByFallbackFieldLaterReceivesStableId() {
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // Initially arrives with BLANK id (identified via fallback: BILL_Aug-Oct_10102024_150)
        val billWithoutId = TnebBillRecord(
            id = "",
            period = "Aug–Oct 2024",
            units = "150",
            assessmentDate = "10/10/2024",
            entryDate = "12/10/2024"
        )
        val fallbackId = BillVerificationEngine.getFallbackBillId(billWithoutId)
        assertEquals("BILL_Aug-Oct_10102024_150", fallbackId)

        // Initial evaluation with fallback ID: awards 10 processing + 50 reduction = 60 coins
        val result1 = BillVerificationEngine.evaluateBills(
            testUserId,
            TnebConsumerBillData(bills = listOf(billWithoutId, baseline)),
            RewardStorage
        )
        assertEquals(60, RewardStorage.getRewardData(testUserId).balance)
        assertTrue(RewardStorage.hasProcessedBill(testUserId, fallbackId))

        // Later, upstream portal assigns an explicit stable ID to this bill
        val billWithStableId = billWithoutId.copy(id = "B-STABLE-2024-10")

        // Re-evaluate: must recognize it is the same bill and NOT award duplicate coins!
        val result2 = BillVerificationEngine.evaluateBills(
            testUserId,
            TnebConsumerBillData(bills = listOf(billWithStableId, baseline)),
            RewardStorage
        )
        assertEquals("Must NOT award duplicate coins when fallback bill receives stable ID", 60, RewardStorage.getRewardData(testUserId).balance)
        assertEquals("Transaction count remains 2", 2, RewardStorage.getRewardData(testUserId).transactions.size)
        assertTrue("New stable ID is linked as processed", RewardStorage.hasProcessedBill(testUserId, "B-STABLE-2024-10"))
    }

    @Test
    fun testScenario4_TwoDistinctBillsHaveSameAssessmentDateOrEntryDate() {
        val baseline = createBill("B-2024-06", "Apr–Jun 2024", "300", "10/06/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // Two distinct bills sharing the exact same assessmentDate "10/10/2024" and entryDate "12/10/2024"
        val billA = TnebBillRecord(
            id = "",
            period = "Jun–Aug 2024",
            units = "240",
            assessmentDate = "10/10/2024",
            entryDate = "12/10/2024"
        )
        val billB = TnebBillRecord(
            id = "",
            period = "Aug–Oct 2024",
            units = "180",
            assessmentDate = "10/10/2024",
            entryDate = "12/10/2024"
        )

        val idA = BillVerificationEngine.getStableBillId(billA)
        val idB = BillVerificationEngine.getStableBillId(billB)

        // Verify IDs do NOT collide despite sharing the exact same dates
        assertFalse("IDs must not collide when assessment dates match", idA == idB)
        assertEquals("BILL_Jun-Aug_10102024_240", idA)
        assertEquals("BILL_Aug-Oct_10102024_180", idB)

        // Evaluate both bills together: both should be processed independently
        val data = TnebConsumerBillData(bills = listOf(billB, billA, baseline))
        BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)

        assertTrue(RewardStorage.hasProcessedBill(testUserId, idA))
        assertTrue(RewardStorage.hasProcessedBill(testUserId, idB))
    }

    @Test
    fun testScenario5_CorrectedBillRecordReplacesOlderRecord() {
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "200", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)

        // Bill initially processed for Aug–Oct with 150 units (60 coins)
        val initialBill = createBill("B-2024-10", "Aug–Oct 2024", "150", "10/10/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(initialBill, baseline)), RewardStorage)
        assertEquals(60, RewardStorage.getRewardData(testUserId).balance)

        // TNEB issues a corrected bill record for the same billing cycle (units adjusted to 140)
        val correctedBill = createBill("B-2024-10-CORR", "Aug–Oct 2024", "140", "10/10/2024")
        val updatedData = TnebConsumerBillData(bills = listOf(correctedBill, baseline))

        BillVerificationEngine.evaluateBills(testUserId, updatedData, RewardStorage)

        // Must NOT issue a second 10-coin processing fee for the already-evaluated Aug–Oct billing period
        assertEquals("Corrected record must not award duplicate processing fee", 60, RewardStorage.getRewardData(testUserId).balance)
        val processingTxs = RewardStorage.getRewardData(testUserId).transactions.filter { it.eventType == CoinEventType.BILL_PROCESSED }
        assertEquals(1, processingTxs.size)
    }

    @Test
    fun testScenario6_HistoricalBaselineBillsNeverReceiveProcessingOrReductionRewards() {
        // 3 historical bills present at first app launch
        val bill1 = createBill("B-HIST-1", "Feb–Apr 2024", "300", "10/04/2024")
        val bill2 = createBill("B-HIST-2", "Apr–Jun 2024", "260", "10/06/2024")
        val bill3 = createBill("B-HIST-3", "Jun–Aug 2024", "220", "10/08/2024")
        val initialData = TnebConsumerBillData(bills = listOf(bill3, bill2, bill1))

        // First launch baseline registration
        val result = BillVerificationEngine.evaluateBills(testUserId, initialData, RewardStorage)
        assertEquals(BillVerificationStatus.WAITING_FOR_BILL, result.status)
        assertEquals(0, result.billProcessedRewardEarned)
        assertEquals(0, result.reductionBonusEarned)

        // Balance is 0
        assertEquals(0, RewardStorage.getRewardData(testUserId).balance)
        assertTrue(RewardStorage.getRewardData(testUserId).transactions.isEmpty())

        // Re-evaluating historical bills repeatedly never awards retroactive coins
        for (i in 1..3) {
            val repeatResult = BillVerificationEngine.evaluateBills(testUserId, initialData, RewardStorage)
            assertEquals(0, repeatResult.billProcessedRewardEarned)
            assertEquals(0, repeatResult.reductionBonusEarned)
            assertEquals(0, RewardStorage.getRewardData(testUserId).balance)
        }
    }

    @Test
    fun testScenario7_GenuinelyNewValidBillReceivesEachEligibleRewardExactlyOnce() {
        // Baseline established with 1 bill: 240 units / 60 days = 4.0 kWh/day
        val baseline = createBill("B-2024-08", "Jun–Aug 2024", "240", "10/08/2024")
        BillVerificationEngine.evaluateBills(testUserId, TnebConsumerBillData(bills = listOf(baseline)), RewardStorage)
        assertEquals(0, RewardStorage.getRewardData(testUserId).balance)

        // Genuinely new bill arrives: 180 units / 61 days (~2.95 kWh/day -> 26% reduction)
        val newBill = createBill("B-2024-10", "Aug–Oct 2024", "180", "10/10/2024")
        val data = TnebConsumerBillData(bills = listOf(newBill, baseline))

        val result = BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)
        assertEquals(BillVerificationStatus.VERIFIED_REDUCTION, result.status)
        assertEquals("B-2024-10", result.billId)
        assertEquals(10, result.billProcessedRewardEarned)
        assertEquals(50, result.reductionBonusEarned)

        // Authoritative ledger has 60 coins (10 processing + 50 reduction bonus)
        val ledger = RewardStorage.getRewardData(testUserId)
        assertEquals(60, ledger.balance)
        assertEquals(2, ledger.transactions.size)

        val processTx = ledger.transactions.find { it.eventType == CoinEventType.BILL_PROCESSED }
        val reductionTx = ledger.transactions.find { it.eventType == CoinEventType.VERIFIED_CONSUMPTION_REDUCTION }
        assertNotNull(processTx)
        assertEquals(10, processTx!!.amount)
        assertNotNull(reductionTx)
        assertEquals(50, reductionTx!!.amount)

        // Multiple subsequent evaluations must NOT award any further rewards
        for (i in 1..3) {
            BillVerificationEngine.evaluateBills(testUserId, data, RewardStorage)
            assertEquals(60, RewardStorage.getRewardData(testUserId).balance)
            assertEquals(2, RewardStorage.getRewardData(testUserId).transactions.size)
        }
    }
}
