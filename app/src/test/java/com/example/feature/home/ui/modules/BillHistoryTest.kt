package com.example.feature.home.ui.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BillHistoryTest {

    @Before
    fun setUp() {
        TnebBillCache.clear(null)
    }

    @Test
    fun testHistoricalBillRecordProperties() {
        val record = TnebBillRecord(
            id = "BILL-001",
            period = "Aug - Sep 2026",
            assessmentDate = "18-09-2026",
            amountDue = "1840",
            amountPaid = "1840",
            paymentDate = "22-09-2026",
            receiptNumber = "RCP-774921",
            isPaid = true,
            units = "420",
            kwh = "420",
            status = "PAID"
        )

        assertEquals("Aug - Sep 2026", record.period)
        assertEquals("18-09-2026", record.assessmentDate)
        assertEquals("1840", record.amountDue)
        assertEquals("1840", record.amountPaid)
        assertEquals("22-09-2026", record.paymentDate)
        assertEquals("RCP-774921", record.receiptNumber)
        assertTrue(record.isPaid)
        assertEquals("420", record.units)
    }

    @Test
    fun testConsumerBillData_withHistoryRecords() {
        val historicalBills = listOf(
            TnebBillRecord(
                id = "BILL-001",
                period = "Jun - Jul 2026",
                assessmentDate = "18-07-2026",
                amountDue = "1250",
                paymentDate = "20-07-2026",
                receiptNumber = "RCP-112233",
                isPaid = true,
                units = "310"
            ),
            TnebBillRecord(
                id = "BILL-002",
                period = "Apr - May 2026",
                assessmentDate = "18-05-2026",
                amountDue = "980",
                paymentDate = "25-05-2026",
                receiptNumber = "RCP-445566",
                isPaid = true,
                units = "250"
            ),
            TnebBillRecord(
                id = "BILL-003",
                period = "Feb - Mar 2026",
                assessmentDate = "18-03-2026",
                amountDue = "0",
                isPaid = false,
                units = "100"
            )
        )

        val consumerData = TnebConsumerBillData(
            consumerNumber = "09440233135",
            consumerName = "S.SUBBIAH",
            region = "09-CHENNAI-SOUTH",
            tariffCode = "LT-1A",
            bills = historicalBills
        )

        assertEquals(3, consumerData.bills.size)
        val paidBills = consumerData.bills.filter { it.isPaid }
        val pendingBills = consumerData.bills.filter { !it.isPaid }

        assertEquals(2, paidBills.size)
        assertEquals(1, pendingBills.size)

        // Verify paid bill fields
        val firstPaid = paidBills.first()
        assertEquals("Jun - Jul 2026", firstPaid.period)
        assertEquals("1250", firstPaid.amountDue)
        assertEquals("20-07-2026", firstPaid.paymentDate)
        assertEquals("RCP-112233", firstPaid.receiptNumber)
        assertTrue(firstPaid.isPaid)
    }

    @Test
    fun testTnebBillCache_persistsAndRetrievesBillHistory() {
        val testData = TnebConsumerBillData(
            consumerNumber = "09123456789",
            consumerName = "A. RAMESH",
            bills = listOf(
                TnebBillRecord(
                    id = "REC-1",
                    period = "Jul 2026",
                    amountDue = "1450",
                    isPaid = true
                )
            )
        )

        TnebBillCache.save(null, testData)
        val cached = TnebBillCache.billData.value

        assertNotNull(cached)
        assertEquals("09123456789", cached?.consumerNumber)
        assertEquals(1, cached?.bills?.size)
        assertTrue(cached?.bills?.first()?.isPaid == true)
    }
}
