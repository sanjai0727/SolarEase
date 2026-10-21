package com.example.feature.home.ui.modules

import com.example.core.network.dto.BaselineRequestDto
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
            consumerNumber = "042345678901",
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
            ebServiceNumber = "042345678901",
            tnebRegisteredMobile = "9876543210"
        )

        // Act: User taps Explore Tools -> Bill Status
        val viewModel = TnebBillStatusViewModel()

        // Assert: MUST be IDLE (Input screen appears, NOT Result screen directly)
        assertEquals(SubmitState.IDLE, viewModel.submitState.value)
        assertNull(viewModel.billData.value)

        // Credentials must be prefilled from ebServiceNumber and tnebRegisteredMobile (NOT personal mobileNumber)
        assertEquals("042345678901", viewModel.serviceConnectionNumber.value)
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
            ebServiceNumber = "094402331350"
        )

        // Act: User opens Bill Status
        val viewModel = TnebBillStatusViewModel()

        // Assert: Registered Mobile Number field must NOT be prefilled from personal mobileNumber!
        assertEquals("094402331350", viewModel.serviceConnectionNumber.value)
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
            ebServiceNumber = "094402331350"
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
        viewModel.updateServiceConnectionNumber("094402331350")
        viewModel.updateRegisteredMobileNumber("9840123456")

        val newBillJson = """
        {
            "consumerName": "S.SUBBIAH",
            "consumerNumber": "094402331350",
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
        assertEquals("094402331350", viewModel.serviceConnectionNumber.value)
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
            consumerNumber = "094402331350",
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
            consumerNumber = "094402331350",
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
        TnebBillCache.save(null, TnebConsumerBillData(consumerNumber = "012345678901", bills = records))

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
            consumerNumber = "011111111111",
            bills = listOf(TnebBillRecord(id = "A-1", amountDue = "500"))
        )
        TnebBillCache.save(null, consumerA)

        // Act: Consumer B retrieves bills
        val consumerB = TnebConsumerBillData(
            consumerNumber = "022222222222",
            bills = listOf(TnebBillRecord(id = "B-1", amountDue = "900"))
        )
        TnebBillCache.save(null, consumerB)

        // Assert: Cache only has Consumer B's bills, not merged with Consumer A
        val cached = TnebBillCache.billData.value
        assertEquals("022222222222", cached?.consumerNumber)
        assertEquals(1, cached?.bills?.size)
        assertEquals("B-1", cached?.bills?.first()?.id)
    }

    @Test
    fun test7_valid12DigitServiceConnectionNumber_becomesValidAndPersists() {
        val viewModel = TnebBillStatusViewModel()
        assertFalse(viewModel.isValid.value)

        // 12 numeric digits: Region=04 (Erode), Division=234, Subdivision=567, Consumer=8901
        viewModel.updateServiceConnectionNumber("042345678901")
        viewModel.updateRegisteredMobileNumber("9876543210")

        assertEquals("042345678901", viewModel.serviceConnectionNumber.value)
        assertEquals(12, viewModel.serviceConnectionNumber.value.length)
        assertTrue(viewModel.isValid.value)
    }

    @Test
    fun test8_rejectionOf11DigitNumber_remainsInvalid() {
        val viewModel = TnebBillStatusViewModel()
        // 11 digits must be rejected as invalid
        viewModel.updateServiceConnectionNumber("04234567890")
        viewModel.updateRegisteredMobileNumber("9876543210")

        assertEquals("04234567890", viewModel.serviceConnectionNumber.value)
        assertEquals(11, viewModel.serviceConnectionNumber.value.length)
        assertFalse(viewModel.isValid.value)
    }

    @Test
    fun test9_rejectionOf13DigitNumber_preventsMoreThan12Digits() {
        val viewModel = TnebBillStatusViewModel()
        // Attempt entering 13 or more digits: should be capped/truncated to exactly 12 digits
        viewModel.updateServiceConnectionNumber("042345678901999")
        viewModel.updateRegisteredMobileNumber("9876543210")

        assertEquals("042345678901", viewModel.serviceConnectionNumber.value)
        assertEquals(12, viewModel.serviceConnectionNumber.value.length)
        assertTrue(viewModel.isValid.value)
    }

    @Test
    fun test10_rejectionOfNonNumericCharacters_filtersStrictlyToNumericDigits() {
        val viewModel = TnebBillStatusViewModel()
        // Characters, dashes, symbols should be rejected/stripped
        viewModel.updateServiceConnectionNumber("04-234-567-8901")
        assertEquals("042345678901", viewModel.serviceConnectionNumber.value)

        viewModel.updateServiceConnectionNumber("04#ABC.567@8901!XYZ")
        assertEquals("045678901", viewModel.serviceConnectionNumber.value)
        assertFalse(viewModel.isValid.value)

        viewModel.updateServiceConnectionNumber("NON_NUMERIC")
        assertEquals("", viewModel.serviceConnectionNumber.value)
        assertFalse(viewModel.isValid.value)
    }

    @Test
    fun test11_correctHandlingOfLeadingZeros_preservedWithoutTruncation() {
        val viewModel = TnebBillStatusViewModel()
        // 12-digit number with leading zero e.g. Chennai North "01..." or "00..."
        val leadingZeroNumber = "012345678901"
        viewModel.updateServiceConnectionNumber(leadingZeroNumber)
        viewModel.updateRegisteredMobileNumber("9876543210")

        assertEquals("012345678901", viewModel.serviceConnectionNumber.value)
        assertEquals(12, viewModel.serviceConnectionNumber.value.length)
        assertTrue(viewModel.serviceConnectionNumber.value.startsWith("01"))
        assertTrue(viewModel.isValid.value)

        // Double leading zero
        viewModel.updateServiceConnectionNumber("001234567890")
        assertEquals("001234567890", viewModel.serviceConnectionNumber.value)
        assertEquals(12, viewModel.serviceConnectionNumber.value.length)
        assertTrue(viewModel.serviceConnectionNumber.value.startsWith("00"))
        assertTrue(viewModel.isValid.value)
    }

    @Test
    fun test12_mobileNumberPreservedIndependently_andNotConfusedWithScn() {
        val viewModel = TnebBillStatusViewModel()
        // 10-digit mobile number must not be confused with 12-digit SCN
        viewModel.updateServiceConnectionNumber("9876543210") // Only 10 digits in SCN
        viewModel.updateRegisteredMobileNumber("9876543210") // 10 digits in Mobile
        assertFalse("10-digit SCN must be rejected", viewModel.isValid.value)

        // Providing all 12 digits to SCN makes it valid
        viewModel.updateServiceConnectionNumber("042345678901")
        assertTrue("12-digit SCN + 10-digit mobile must be valid", viewModel.isValid.value)

        // Mobile number field clamps to 10 digits
        viewModel.updateRegisteredMobileNumber("987654321099")
        assertEquals("9876543210", viewModel.registeredMobileNumber.value)
        assertEquals(10, viewModel.registeredMobileNumber.value.length)
    }

    @Test
    fun test13_correctTransmissionOf12DigitsToBackendDto() {
        // Verify that 12-digit SCN with leading zero transmits intact into BaselineRequestDto
        val scn12 = "042345678901"
        val requestDto = BaselineRequestDto(
            cycleId = "BILL-20261019",
            baselineDate = "2026-10-19",
            baselineReading = 15000.0,
            previousBilledUnits = 350.0,
            tariffCode = "LT-1A",
            serviceConnectionNumber = scn12
        )

        assertEquals("042345678901", requestDto.serviceConnectionNumber)
        assertEquals(12, requestDto.serviceConnectionNumber?.length)
        assertTrue(requestDto.serviceConnectionNumber?.startsWith("04") == true)

        // Verify MeterCycleBaseline retains all 12 digits
        val baseline = MeterCycleBaseline(
            cycleId = requestDto.cycleId,
            baselineDate = requestDto.baselineDate,
            baselineReading = requestDto.baselineReading,
            previousBilledUnits = requestDto.previousBilledUnits,
            tariffCode = requestDto.tariffCode ?: "LT-1A",
            serviceConnectionNumber = requestDto.serviceConnectionNumber
        )
        assertEquals("042345678901", baseline.serviceConnectionNumber)
        assertEquals(12, baseline.serviceConnectionNumber?.length)
    }
}
