package com.example.feature.recommends.data

import com.example.feature.recommends.model.CoinEventType
import com.example.feature.recommends.model.RewardConfig
import com.example.feature.recommends.model.UserRewardData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RewardStorageTest {

    private val testUserId = "test_user_1"
    private val otherUserId = "test_user_2"

    @Before
    fun setUp() {
        RewardStorage.clear(null)
    }

    @After
    fun tearDown() {
        RewardStorage.clear(null)
    }

    @Test
    fun testFirstQualifyingActivityAwardsDailyCoinAndStartsStreak() {
        val date = "2026-10-01"
        val (streak, txs) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", date)

        assertEquals("First qualifying activity starts streak at 1", 1, streak.currentStreak)
        assertEquals(1, streak.longestStreak)
        assertEquals(date, streak.lastQualifyingDate)
        assertEquals(listOf(date), streak.completedDates)
        assertEquals(1, streak.currentMilestoneCycle)
        assertEquals(1, streak.daysInCurrentCycle)

        assertEquals("Should emit 1 daily coin transaction", 1, txs.size)
        val tx = txs.first()
        assertEquals(testUserId, tx.userId)
        assertEquals(CoinEventType.DAILY_ACTIVITY, tx.eventType)
        assertEquals(RewardConfig.DAILY_ACTIVITY_COINS, tx.amount)
        assertEquals("daily_${testUserId}_$date", tx.idempotencyKey)

        val data = RewardStorage.getRewardData(testUserId)
        assertEquals(1, data.balance)
        assertEquals(1, data.transactions.size)
    }

    @Test
    fun testMultipleActivitiesOnSameDayAreIdempotent() {
        val date = "2026-10-01"
        val (streak1, txs1) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", date)
        assertEquals(1, streak1.currentStreak)
        assertEquals(1, txs1.size)

        // Second activity on same day
        val (streak2, txs2) = RewardStorage.recordDailyActivity(testUserId, "mantra_2", date)
        assertEquals("Streak count must remain 1", 1, streak2.currentStreak)
        assertTrue("No additional transactions should be issued for same calendar day", txs2.isEmpty())

        // Third activity (meter reading) on same day
        val (streak3, txs3) = RewardStorage.recordDailyActivity(testUserId, "meter_reading", date)
        assertEquals(1, streak3.currentStreak)
        assertTrue(txs3.isEmpty())

        val data = RewardStorage.getRewardData(testUserId)
        assertEquals("Total balance must remain 1 coin for same calendar day", 1, data.balance)
        assertEquals(1, data.transactions.size)
    }

    @Test
    fun testConsecutiveCalendarDaysIncrementStreak() {
        val days = listOf("2026-10-01", "2026-10-02", "2026-10-03", "2026-10-04")
        days.forEachIndexed { index, day ->
            val (streak, txs) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", day)
            assertEquals("Streak should match consecutive day index", index + 1, streak.currentStreak)
            assertEquals(index + 1, streak.longestStreak)
            assertEquals(day, streak.lastQualifyingDate)
            assertEquals(1, txs.size)
        }

        val data = RewardStorage.getRewardData(testUserId)
        assertEquals(4, data.streakState.currentStreak)
        assertEquals(4, data.streakState.longestStreak)
        assertEquals(4, data.balance)
        assertEquals(4, data.transactions.size)
    }

    @Test
    fun testMissedCalendarDayResetsActiveStreakAndPreservesLongestStreak() {
        // Day 1 & Day 2
        RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-01")
        val (streak2, _) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-02")
        assertEquals(2, streak2.currentStreak)
        assertEquals(2, streak2.longestStreak)

        // Skip 2026-10-03 and qualify on 2026-10-04
        val (streakAfterSkip, txs) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-04")
        assertEquals("Active streak must reset to 1 after missing a day", 1, streakAfterSkip.currentStreak)
        assertEquals("All-time longest streak must be preserved", 2, streakAfterSkip.longestStreak)
        assertEquals("2026-10-04", streakAfterSkip.lastQualifyingDate)
        assertEquals(1, txs.size)

        val data = RewardStorage.getRewardData(testUserId)
        assertEquals("Total balance should be 3 coins (Day 1 + Day 2 + Day 4)", 3, data.balance)
    }

    @Test
    fun testSevenDayMilestoneAwardsFiveBonusCoinsOnce() {
        // Complete 6 days
        for (i in 1..6) {
            val date = "2026-10-%02d".format(i)
            RewardStorage.recordDailyActivity(testUserId, "mantra_1", date)
        }

        val dataDay6 = RewardStorage.getRewardData(testUserId)
        assertEquals(6, dataDay6.streakState.currentStreak)
        assertEquals(6, dataDay6.balance)
        assertFalse(dataDay6.streakState.isMilestonePending)

        // Day 7: Milestone reached!
        val (streakDay7, txsDay7) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-07")
        assertEquals(7, streakDay7.currentStreak)
        assertEquals(7, streakDay7.longestStreak)
        assertTrue(streakDay7.claimedMilestones.contains(7))
        assertEquals(1, streakDay7.currentMilestoneCycle)
        assertEquals(7, streakDay7.daysInCurrentCycle)
        assertEquals(14, streakDay7.nextMilestoneTarget)

        // Day 7 emits: 1 daily coin + 5 milestone bonus coins = 2 transactions
        assertEquals(2, txsDay7.size)
        val dailyTx = txsDay7.find { it.eventType == CoinEventType.DAILY_ACTIVITY }
        val milestoneTx = txsDay7.find { it.eventType == CoinEventType.STREAK_MILESTONE }
        assertNotNull(dailyTx)
        assertEquals(1, dailyTx!!.amount)
        assertNotNull(milestoneTx)
        assertEquals(RewardConfig.STREAK_MILESTONE_COINS, milestoneTx!!.amount)
        assertEquals("milestone_${testUserId}_2026-10-07_7", milestoneTx.idempotencyKey)

        val dataDay7 = RewardStorage.getRewardData(testUserId)
        // 7 daily coins + 5 milestone bonus = 12 coins
        assertEquals(12, dataDay7.balance)

        // Day 8: Advances into cycle 2
        val (streakDay8, txsDay8) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-08")
        assertEquals(8, streakDay8.currentStreak)
        assertEquals(1, streakDay8.daysInCurrentCycle)
        assertEquals(2, streakDay8.currentMilestoneCycle)
        assertEquals(14, streakDay8.nextMilestoneTarget)
        assertEquals(1, txsDay8.size) // Daily coin only, no duplicate milestone
    }

    @Test
    fun testStreakProgressionAcrossDays1To14AndMilestones() {
        // Complete 14 consecutive days (Cycle 1 & Cycle 2)
        for (day in 1..14) {
            val date = "2026-10-%02d".format(day)
            RewardStorage.recordDailyActivity(testUserId, "mantra_1", date)
        }

        val data = RewardStorage.getRewardData(testUserId)
        // 14 daily coins + 5 bonus (Day 7) + 5 bonus (Day 14) = 24 coins total
        assertEquals(24, data.balance)
        assertEquals(14, data.streakState.currentStreak)
        assertEquals(14, data.streakState.longestStreak)
        assertEquals(2, data.streakState.currentMilestoneCycle)
        assertEquals(7, data.streakState.daysInCurrentCycle)
        assertEquals(21, data.streakState.nextMilestoneTarget)
        assertTrue(data.streakState.claimedMilestones.contains(7))
        assertTrue(data.streakState.claimedMilestones.contains(14))

        // Ledger has 16 transactions: 14 daily + 2 milestones
        assertEquals(16, data.transactions.size)
        val milestones = data.transactions.filter { it.eventType == CoinEventType.STREAK_MILESTONE }
        assertEquals(2, milestones.size)
        assertEquals(5, milestones[0].amount)
        assertEquals(5, milestones[1].amount)
    }

    @Test
    fun testMissedDayResetAllowsNewMilestoneRewardOnSubsequentCycle() {
        // Complete 7 days for Streak 1
        for (day in 1..7) {
            RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-%02d".format(day))
        }
        val dataStreak1 = RewardStorage.getRewardData(testUserId)
        assertEquals(12, dataStreak1.balance) // 7 + 5

        // Miss a day: Skip 2026-10-08, check in on 2026-10-09
        val (resetStreak, _) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-09")
        assertEquals(1, resetStreak.currentStreak)
        assertEquals(7, resetStreak.longestStreak) // Preserved!
        assertEquals(1, resetStreak.currentMilestoneCycle)
        assertEquals(1, resetStreak.daysInCurrentCycle)

        // Complete 6 more consecutive days to reach 7 days on the new streak (2026-10-10 through 2026-10-15)
        for (day in 10..15) {
            RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-%02d".format(day))
        }

        val dataStreak2 = RewardStorage.getRewardData(testUserId)
        assertEquals(7, dataStreak2.streakState.currentStreak)
        assertEquals(7, dataStreak2.streakState.longestStreak)
        assertEquals(1, dataStreak2.streakState.currentMilestoneCycle)
        assertEquals(7, dataStreak2.streakState.daysInCurrentCycle)

        // Balance: 12 (from Streak 1) + 1 (Day 1 of Streak 2) + 6 (Days 2-7 of Streak 2) + 5 (Bonus for new Day 7 milestone) = 24 coins!
        assertEquals(24, dataStreak2.balance)

        val milestones = dataStreak2.transactions.filter { it.eventType == CoinEventType.STREAK_MILESTONE }
        assertEquals("Both completed 7-day cycles award milestone bonuses", 2, milestones.size)
    }

    @Test
    fun testDuplicateMilestonePreventionOnRecheck() {
        // Re-evaluating day 7 when already qualified
        for (i in 1..7) {
            RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-%02d".format(i))
        }

        val balanceBefore = RewardStorage.getRewardData(testUserId).balance
        assertEquals(12, balanceBefore)

        // Repeated check-in on day 7
        val (streakRecheck, txsRecheck) = RewardStorage.recordDailyActivity(testUserId, "mantra_2", "2026-10-07")
        assertEquals(7, streakRecheck.currentStreak)
        assertTrue("No transactions issued on recheck", txsRecheck.isEmpty())
        assertEquals(balanceBefore, RewardStorage.getRewardData(testUserId).balance)
    }

    @Test
    fun testDateBoundaryHandlingMonthRollover() {
        // Test month end boundary: 2026-10-31 -> 2026-11-01
        RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-31")
        val (streakNov1, _) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-11-01")
        assertEquals("Streak should continue across month boundary", 2, streakNov1.currentStreak)

        // Test year end boundary: 2026-12-31 -> 2027-01-01
        RewardStorage.clear(testUserId)
        RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-12-31")
        val (streakJan1, _) = RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2027-01-01")
        assertEquals("Streak should continue across year boundary", 2, streakJan1.currentStreak)
    }

    @Test
    fun testBillProcessingAndReductionRewardIssuanceAndIdempotency() {
        val billId = "BILL_2024_10"
        val period = "Aug–Oct 2024"

        // Initial processing reward
        val processTx1 = RewardStorage.recordBillProcessingReward(testUserId, billId, period)
        assertNotNull(processTx1)
        assertEquals(CoinEventType.BILL_PROCESSED, processTx1!!.eventType)
        assertEquals(RewardConfig.BILL_PROCESSED_COINS, processTx1.amount)

        // Duplicate processing attempt for same bill
        val processTx2 = RewardStorage.recordBillProcessingReward(testUserId, billId, period)
        assertNull("Duplicate processing reward must be rejected", processTx2)

        // Reduction bonus reward
        val reductionTx1 = RewardStorage.recordReductionBonusReward(
            userId = testUserId,
            billId = billId,
            period = period,
            reductionPercent = 14.5,
            bonusAmount = RewardConfig.REDUCTION_TIER_2_COINS
        )
        assertNotNull(reductionTx1)
        assertEquals(CoinEventType.VERIFIED_CONSUMPTION_REDUCTION, reductionTx1!!.eventType)
        assertEquals(RewardConfig.REDUCTION_TIER_2_COINS, reductionTx1.amount)

        // Duplicate reduction bonus attempt for same bill
        val reductionTx2 = RewardStorage.recordReductionBonusReward(
            userId = testUserId,
            billId = billId,
            period = period,
            reductionPercent = 14.5,
            bonusAmount = RewardConfig.REDUCTION_TIER_2_COINS
        )
        assertNull("Duplicate reduction bonus must be rejected", reductionTx2)

        val data = RewardStorage.getRewardData(testUserId)
        // 10 + 30 = 40 coins
        assertEquals(40, data.balance)
        assertEquals(2, data.transactions.size)
        assertTrue(RewardStorage.hasProcessedBill(testUserId, billId))
    }

    @Test
    fun testHistoricalBaselineBillReceivesNoRewards() {
        val historicalBillId = "HISTORICAL_BILL_001"
        RewardStorage.markBillAsHistoricalBaseline(testUserId, historicalBillId)
        assertTrue(RewardStorage.isHistoricalBaselineBill(testUserId, historicalBillId))

        // Attempt to award processing reward
        val processTx = RewardStorage.recordBillProcessingReward(testUserId, historicalBillId, "Jun–Aug")
        assertNull("Historical baseline bill must not receive processing reward", processTx)

        // Attempt to award reduction bonus
        val bonusTx = RewardStorage.recordReductionBonusReward(
            testUserId,
            historicalBillId,
            "Jun–Aug",
            25.0,
            50
        )
        assertNull("Historical baseline bill must not receive reduction bonus", bonusTx)

        val data = RewardStorage.getRewardData(testUserId)
        assertEquals(0, data.balance)
        assertTrue(data.transactions.isEmpty())
    }

    @Test
    fun testUserIsolationAcrossAccounts() {
        // User 1 performs activity
        RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-01")
        RewardStorage.recordBillProcessingReward(testUserId, "BILL_U1", "Aug–Oct")

        val user1Data = RewardStorage.getRewardData(testUserId)
        assertEquals(11, user1Data.balance) // 1 + 10
        assertEquals(1, user1Data.streakState.currentStreak)

        // User 2 should start completely clean
        val user2Data = RewardStorage.getRewardData(otherUserId)
        assertEquals(0, user2Data.balance)
        assertEquals(0, user2Data.streakState.currentStreak)
        assertTrue(user2Data.transactions.isEmpty())

        // User 2 performs activity
        RewardStorage.recordDailyActivity(otherUserId, "mantra_1", "2026-10-01")
        assertEquals(1, RewardStorage.getRewardData(otherUserId).balance)

        // User 1 remains unchanged
        assertEquals(11, RewardStorage.getRewardData(testUserId).balance)
    }

    @Test
    fun testSerializationAndDeserializationPreservesState() {
        val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
        RewardStorage.recordDailyActivity(testUserId, "mantra_1", "2026-10-01")
        RewardStorage.recordBillProcessingReward(testUserId, "BILL_SER", "Aug–Oct")

        val originalData = RewardStorage.getRewardData(testUserId)
        val serialized = json.encodeToString(originalData)
        val deserialized = json.decodeFromString<UserRewardData>(serialized)

        assertEquals(originalData.userId, deserialized.userId)
        assertEquals(originalData.balance, deserialized.balance)
        assertEquals(originalData.streakState.currentStreak, deserialized.streakState.currentStreak)
        assertEquals(originalData.streakState.longestStreak, deserialized.streakState.longestStreak)
        assertEquals(originalData.transactions.size, deserialized.transactions.size)
        assertEquals(originalData.transactions[0].idempotencyKey, deserialized.transactions[0].idempotencyKey)
    }
}
