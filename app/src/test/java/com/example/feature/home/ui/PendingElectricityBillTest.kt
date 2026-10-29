package com.example.feature.home.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.feature.home.ui.modules.PendingBillUiState
import com.example.feature.home.ui.modules.TnebBillRecord
import com.example.feature.home.ui.modules.TnebConsumerBillData
import com.example.feature.home.ui.modules.computePendingBillState
import com.example.feature.home.ui.modules.formatTnebAmount
import com.example.feature.home.ui.modules.formatTnebDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PendingElectricityBillTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ── 1. Date and Amount Formatting Tests ─────────────────────────────────

    @Test
    fun testDateFormatting_variousFormats() {
        assertEquals("5 November 2026", formatTnebDate("05-11-2026"))
        assertEquals("15 October 2024", formatTnebDate("15/10/2024"))
        assertEquals("5 November 2026", formatTnebDate("2026-11-05"))
        assertEquals("25 October 2026", formatTnebDate("25-Oct-2026"))
        assertNull(formatTnebDate(null))
        assertNull(formatTnebDate(""))
    }

    @Test
    fun testAmountFormatting_variousAmounts() {
        assertEquals("₹1,450", formatTnebAmount("1450.00"))
        assertEquals("₹1,450", formatTnebAmount("1450"))
        assertEquals("₹1,450.50", formatTnebAmount("1450.50"))
        assertEquals("₹1,450", formatTnebAmount("₹1,450.00"))
        assertEquals("₹25,000", formatTnebAmount("25000.00"))
        assertNull(formatTnebAmount(null))
        assertNull(formatTnebAmount(""))
    }

    // ── 2. State A: Pending Bill Exists ────────────────────────────────────

    @Test
    fun testStateA_pendingBillComputation() {
        val billData = TnebConsumerBillData(
            consumerNumber = "04-201-005-1234",
            consumerName = "Sanjai",
            isDuesNil = false,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    amountDue = "1450.00",
                    dueDate = "05-11-2026",
                    isPaid = false,
                    period = "09/2026 - 10/2026",
                    assessmentDate = "05-10-2026"
                )
            )
        )

        val state = computePendingBillState(billData, userEbNumber = "04-201-005-1234")
        assertTrue("State should be Pending", state is PendingBillUiState.Pending)

        val pending = state as PendingBillUiState.Pending
        assertEquals("₹1,450", pending.amountFormatted)
        assertEquals(1450.0, pending.rawAmount!!, 0.001)
        assertEquals("5 November 2026", pending.dueDateFormatted)
        assertEquals("09/2026 - 10/2026", pending.billingPeriod)
        assertFalse(pending.isPartiallyAvailable)
    }

    @Test
    fun testStateA_pendingElectricityBillHeroCardRendersCorrectly() {
        var viewDetailsClicked = false

        val pendingState = PendingBillUiState.Pending(
            amountFormatted = "₹1,450",
            rawAmount = 1450.0,
            dueDateFormatted = "5 November 2026",
            rawDueDate = "05-11-2026",
            billingPeriod = "09/2026 - 10/2026",
            billDate = "05-10-2026",
            consumerNumber = "04-201-005-1234",
            isPartiallyAvailable = false
        )

        composeTestRule.setContent {
            PendingElectricityBillHeroCard(
                state = pendingState,
                onViewBillDetails = { viewDetailsClicked = true }
            )
        }

        // Verify badge
        composeTestRule.onNodeWithText("PENDING ELECTRICITY BILL").assertIsDisplayed()

        // Verify consumer number
        composeTestRule.onNodeWithText("04-201-005-1234").assertIsDisplayed()

        // Verify prominent outstanding amount
        composeTestRule.onNodeWithText("₹1,450").assertIsDisplayed()
        composeTestRule.onNodeWithText("Outstanding Amount").assertIsDisplayed()

        // Verify due date
        composeTestRule.onNodeWithText("Due date: 5 November 2026").assertIsDisplayed()

        // Verify billing period
        composeTestRule.onNodeWithText("Billing Period: 09/2026 - 10/2026").assertIsDisplayed()

        // Verify action button & click
        val button = composeTestRule.onNodeWithText("View Bill Details")
        button.assertIsDisplayed()
        button.performClick()
        assertTrue("View Bill Details callback should have triggered", viewDetailsClicked)
    }

    // ── 3. State B: No Pending Bill ────────────────────────────────────────

    @Test
    fun testStateB_noPendingBill() {
        val state = computePendingBillState(billData = null, userEbNumber = null)
        assertTrue("State should be None when no bill connected", state is PendingBillUiState.None)

        val emptyData = TnebConsumerBillData()
        val state2 = computePendingBillState(billData = emptyData, userEbNumber = null)
        assertTrue("State should be None when bill data is empty", state2 is PendingBillUiState.None)
    }

    // ── 4. State C: Bill Status is Loading ─────────────────────────────────

    @Test
    fun testStateC_loadingState() {
        val state = computePendingBillState(billData = null, isLoading = true)
        assertTrue("State should be Loading", state is PendingBillUiState.Loading)

        val stateRefreshing = computePendingBillState(billData = null, isRefreshing = true)
        assertTrue("State should be Loading during refresh without data", stateRefreshing is PendingBillUiState.Loading)
    }

    @Test
    fun testStateC_billStatusLoadingCardRenders() {
        composeTestRule.setContent {
            BillStatusLoadingCard()
        }

        composeTestRule.onNodeWithText("Checking electricity bill status...").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verifying with your electricity distribution provider").assertIsDisplayed()
    }

    // ── 5. State D: Bill Retrieval Fails ───────────────────────────────────

    @Test
    fun testStateD_retrievalFailedState() {
        val state = computePendingBillState(
            billData = null,
            userEbNumber = "04-201-005-1234",
            retrievalError = "Unable to retrieve your latest bill."
        )
        assertTrue("State should be RetrievalFailed", state is PendingBillUiState.RetrievalFailed)
        val failed = state as PendingBillUiState.RetrievalFailed
        assertEquals("Unable to retrieve your latest bill.", failed.message)
        assertEquals("04-201-005-1234", failed.consumerNumber)
    }

    @Test
    fun testStateD_billRetrievalFailedCardRendersAndActionsWork() {
        var retryClicked = false
        var checkStatusClicked = false

        val failedState = PendingBillUiState.RetrievalFailed(
            message = "Unable to retrieve your latest bill.",
            consumerNumber = "04-201-005-1234"
        )

        composeTestRule.setContent {
            BillRetrievalFailedCard(
                state = failedState,
                onCheckBillStatus = { checkStatusClicked = true },
                onRetry = { retryClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Unable to retrieve your latest bill.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Consumer No: 04-201-005-1234").assertIsDisplayed()

        // Test Retry button
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue("Retry callback should have triggered", retryClicked)

        // Test View Bill Status button
        composeTestRule.onNodeWithText("View Bill Status").performClick()
        assertTrue("View Bill Status callback should have triggered", checkStatusClicked)
    }

    // ── 6. State E: Bill Details are Partially Available ───────────────────

    @Test
    fun testStateE_partiallyAvailableMissingAmount() {
        val billData = TnebConsumerBillData(
            consumerNumber = "04-201-005-1234",
            isDuesNil = false,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    amountDue = "", // Missing amount
                    dueDate = "05-11-2026",
                    isPaid = false
                )
            )
        )

        val state = computePendingBillState(billData)
        assertTrue(state is PendingBillUiState.Pending)
        val pending = state as PendingBillUiState.Pending
        assertTrue("Should be marked as partially available", pending.isPartiallyAvailable)
        assertNull(pending.rawAmount)
        assertEquals("Amount unavailable", pending.amountFormatted)
        assertEquals("5 November 2026", pending.dueDateFormatted)
    }

    @Test
    fun testStateE_partiallyAvailableMissingDueDate() {
        val billData = TnebConsumerBillData(
            consumerNumber = "04-201-005-1234",
            isDuesNil = false,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    amountDue = "1450.00",
                    dueDate = "", // Missing due date
                    isPaid = false
                )
            )
        )

        val state = computePendingBillState(billData)
        assertTrue(state is PendingBillUiState.Pending)
        val pending = state as PendingBillUiState.Pending
        assertTrue("Should be marked as partially available", pending.isPartiallyAvailable)
        assertEquals("₹1,450", pending.amountFormatted)
        assertNull(pending.dueDateFormatted)
    }

    @Test
    fun testStateE_partiallyAvailableCardRendersWithoutInventedValues() {
        val partialState = PendingBillUiState.Pending(
            amountFormatted = "Amount unavailable",
            rawAmount = null,
            dueDateFormatted = null,
            rawDueDate = null,
            billingPeriod = null,
            billDate = null,
            consumerNumber = "04-201-005-1234",
            isPartiallyAvailable = true
        )

        composeTestRule.setContent {
            PendingElectricityBillHeroCard(
                state = partialState,
                onViewBillDetails = {}
            )
        }

        // Must display actual message, not invented numbers
        composeTestRule.onNodeWithText("Amount unavailable").assertIsDisplayed()
        composeTestRule.onNodeWithText("Due date: Not specified").assertIsDisplayed()
    }

    // ── 7. State F: Bill is Paid ───────────────────────────────────────────

    @Test
    fun testStateF_billPaidViaDuesNil() {
        val billData = TnebConsumerBillData(
            consumerNumber = "04-201-005-1234",
            isDuesNil = true
        )

        val state = computePendingBillState(billData)
        assertTrue("State should be Paid when isDuesNil is true", state is PendingBillUiState.Paid)
    }

    @Test
    fun testStateF_billPaidViaIsPaidFlag() {
        val billData = TnebConsumerBillData(
            consumerNumber = "04-201-005-1234",
            isDuesNil = false,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    amountDue = "1450.00",
                    dueDate = "05-11-2026",
                    isPaid = true,
                    paymentDate = "28-10-2026"
                )
            )
        )

        val state = computePendingBillState(billData)
        assertTrue("State should be Paid when latest bill isPaid is true", state is PendingBillUiState.Paid)
        val paid = state as PendingBillUiState.Paid
        assertEquals("28-10-2026", paid.lastPaymentDate)
    }

    @Test
    fun testStateF_billPaidViaZeroAmount() {
        val billData = TnebConsumerBillData(
            consumerNumber = "04-201-005-1234",
            isDuesNil = false,
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    amountDue = "0.00",
                    dueDate = "05-11-2026",
                    isPaid = false
                )
            )
        )

        val state = computePendingBillState(billData)
        assertTrue("State should be Paid when amount is 0.00", state is PendingBillUiState.Paid)
    }
}

