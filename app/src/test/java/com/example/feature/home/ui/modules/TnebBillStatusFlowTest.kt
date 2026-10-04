package com.example.feature.home.ui.modules

import com.example.core.session.User
import com.example.core.session.UserSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TnebBillStatusFlowTest {

    @Before
    fun setUp() {
        TnebBillCache.clear(null)
        UserSession.currentUser = null
    }

    @After
    fun tearDown() {
        TnebBillCache.clear(null)
        UserSession.currentUser = null
    }

    @Test
    fun test1_freshUserWithNoCachedBill_startsInIdleAndEmpty() {
        // Arrange: fresh user, no session, no cache
        UserSession.currentUser = null
        TnebBillCache.clear(null)

        // Act: Navigate to Bill Status (instantiate ViewModel)
        val viewModel = TnebBillStatusViewModel()

        // Assert: Input screen state is IDLE, no bill data loaded, fields are empty
        assertEquals(SubmitState.IDLE, viewModel.submitState.value)
        assertNull(viewModel.billData.value)
        assertEquals("", viewModel.serviceConnectionNumber.value)
        assertEquals("", viewModel.registeredMobileNumber.value)
        assertFalse(viewModel.isValid.value)
        assertNull(viewModel.captchaImageBase64.value)
    }

    @Test
    fun test2_userWithCachedBill_opensInputScreenWithPrefilledDetails_doesNotBypassToResult() {
        // Arrange: user has previously cached bill data and session
        val cachedBill = TnebConsumerBillData(
            consumerNumber = "04234567891",
            consumerName = "K. ARUMUGAM",
            bills = listOf(
                TnebBillRecord(
                    id = "BILL-PREV",
                    assessmentDate = "10-08-2026",
                    amountDue = "850",
                    isPaid = true
                )
            )
        )
        TnebBillCache.save(null, cachedBill)
        UserSession.currentUser = User(
            id = "user-1",
            username = "arumugam",
            mobileNumber = "+919123456789",
            ebServiceNumber = "04234567891",
            tnebRegisteredMobile = "9876543210"
        )

        // Act: User taps Explore Tools -> Bill Status
        val viewModel = TnebBillStatusViewModel()

        // Assert: MUST be IDLE (Input screen appears, NOT Result screen directly)
        assertEquals(SubmitState.IDLE, viewModel.submitState.value)
        assertNull(viewModel.billData.value)

        // Credentials must be prefilled from ebServiceNumber and tnebRegisteredMobile (NOT personal mobileNumber)
        assertEquals("04234567891", viewModel.serviceConnectionNumber.value)
        assertEquals("9876543210", viewModel.registeredMobileNumber.value)
        assertTrue(viewModel.isValid.value)
    }

    @Test
    fun test2b_userWithOnlyPersonalMobile_registeredMobileFieldRemainsEmpty() {
        // Arrange: User has personal auth mobile number and ebServiceNumber, but no actual TNEB registered mobile number yet
        UserSession.currentUser = User(
            id = "user-2",
            username = "testuser",
            mobileNumber = "+919487826286", // Personal auth mobile number
            ebServiceNumber = "09440233135"
        )

        // Act: User opens Bill Status
        val viewModel = TnebBillStatusViewModel()

        // Assert: Registered Mobile Number field must NOT be prefilled from personal mobileNumber!
        assertEquals("09440233135", viewModel.serviceConnectionNumber.value)
        assertEquals("", viewModel.registeredMobileNumber.value)
        assertFalse(viewModel.isValid.value)
    }

    @Test
    fun test2c_enteringTnebRegisteredMobile_persistsToUserSession() {
        // Arrange: User with personal mobile
        val initialUser = User(
            id = "user-3",
            username = "testuser",
            mobileNumber = "+919487826286",
            ebServiceNumber = "09440233135"
        )
        UserSession.currentUser = initialUser

        val viewModel = TnebBillStatusViewModel()
        assertEquals("", viewModel.registeredMobileNumber.value)

        // Act: User enters their actual 10-digit TNEB registered mobile number
        viewModel.updateRegisteredMobileNumber("9876543210")

        // Assert: ViewModel state is valid and tnebRegisteredMobile is persisted to UserSession
        assertEquals("9876543210", viewModel.registeredMobileNumber.value)
        assertTrue(viewModel.isValid.value)
        assertEquals("9876543210", UserSession.currentUser?.tnebRegisteredMobile)
        // Personal mobile is preserved and not altered
        assertEquals("+919487826286", UserSession.currentUser?.mobileNumber)
    }

    @Test
    fun test3_checkBillStatusAgain_resetsToInputScreen_preservesDetails() {
        // Arrange: ViewModel is in SUCCESS state displaying result
        val viewModel = TnebBillStatusViewModel()
        viewModel.updateServiceConnectionNumber("09440233135")
        viewModel.updateRegisteredMobileNumber("9840123456")

        val newBillJson = """
        {
            "consumerName": "S.SUBBIAH",
            "consumerNumber": "09440233135",
            "duesText": "NIL",
            "isDuesNil": true,
            "records": [
                {
                    "assessmentDate": "18-09-2026",
                    "amountDue": "0",
                    "isPaid": true,
                    "receiptNo": "RCP-001"
                }
            ]
        }
        """.trimIndent()
        viewModel.onParsedDataReceived(newBillJson, 0)
        assertEquals(SubmitState.SUCCESS, viewModel.submitState.value)
        assertNotNull(viewModel.billData.value)

        // Act: User taps "Check Bill Status Again"
        viewModel.resetValidationState()

        // Assert: submitState returns to IDLE, billData is reset, credentials remain intact
        assertEquals(SubmitState.IDLE, viewModel.submitState.value)
        assertNull(viewModel.billData.value)
        assertNull(viewModel.captchaImageBase64.value)
        assertEquals("", viewModel.captchaInput.value)
        assertEquals("09440233135", viewModel.serviceConnectionNumber.value)
        assertEquals("9840123456", viewModel.registeredMobileNumber.value)
        assertTrue(viewModel.isValid.value)
    }

    @Test
    fun test4_successfulSecondCheck_updatesLatestBillAndPreservesBillHistory() {
        // Arrange: initial bill in cache
        val historicalBill1 = TnebBillRecord(
            id = "BILL-JUL26",
            assessmentDate = "18-07-2026",
            amountDue = "1200",
            receiptNumber = "RCP-JUL26",
            isPaid = true
        )
        val initialData = TnebConsumerBillData(
            consumerNumber = "09440233135",
            consumerName = "S.SUBBIAH",
            bills = listOf(historicalBill1)
        )
        TnebBillCache.save(null, initialData)

        // Act: Fresh check retrieves a newer bill cycle
        val historicalBill2 = TnebBillRecord(
            id = "BILL-SEP26",
            assessmentDate = "18-09-2026",
            amountDue = "1450",
            receiptNumber = "RCP-SEP26",
            isPaid = false
        )
        val freshData = TnebConsumerBillData(
            consumerNumber = "09440233135",
            consumerName = "S.SUBBIAH",
            bills = listOf(historicalBill2)
        )
        TnebBillCache.save(null, freshData)

        // Assert: Latest bill is the new check result, and previous bill history is preserved
        val cached = TnebBillCache.billData.value
        assertNotNull(cached)
        assertEquals(2, cached?.bills?.size)
        // Newly checked bill is at the front
        assertEquals("RCP-SEP26", cached?.latestBill?.receiptNumber)
        assertEquals("1450", cached?.latestBill?.amountDue)
        assertFalse(cached?.latestBill?.isPaid ?: true)

        // Previous bill remains in history
        val previousRecord = cached?.bills?.find { it.receiptNumber == "RCP-JUL26" }
        assertNotNull(previousRecord)
        assertEquals("1200", previousRecord?.amountDue)
        assertTrue(previousRecord?.isPaid ?: false)
    }

    @Test
    fun test5_billHistory_remainsSeparateAndAccessibleWithoutChecking() {
        // Arrange: populate cache with historical bills
        val records = listOf(
            TnebBillRecord(id = "B1", assessmentDate = "01-01-2026", amountDue = "500", isPaid = true),
            TnebBillRecord(id = "B2", assessmentDate = "01-03-2026", amountDue = "600", isPaid = true)
        )
        TnebBillCache.save(null, TnebConsumerBillData(consumerNumber = "01234567890", bills = records))

        // Act: View Bill History without checking bill
        val currentHistory = TnebBillCache.billData.value?.bills ?: emptyList()

        // Assert: Records are intact
        assertEquals(2, currentHistory.size)
        assertEquals("B1", currentHistory[0].id)
        assertEquals("B2", currentHistory[1].id)
    }

    @Test
    fun test6_differentConsumer_doesNotMergeBills() {
        // Arrange: Consumer A has bills
        val consumerA = TnebConsumerBillData(
            consumerNumber = "01111111111",
            bills = listOf(TnebBillRecord(id = "A-1", amountDue = "500"))
        )
        TnebBillCache.save(null, consumerA)

        // Act: Consumer B retrieves bills
        val consumerB = TnebConsumerBillData(
            consumerNumber = "02222222222",
            bills = listOf(TnebBillRecord(id = "B-1", amountDue = "900"))
        )
        TnebBillCache.save(null, consumerB)

        // Assert: Cache only has Consumer B's bills, not merged with Consumer A
        val cached = TnebBillCache.billData.value
        assertEquals("02222222222", cached?.consumerNumber)
        assertEquals(1, cached?.bills?.size)
        assertEquals("B-1", cached?.bills?.first()?.id)
    }
}
