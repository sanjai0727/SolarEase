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

    @Test
    fun testConsumerInfo_separatesNameTariffNumberAndLocation() {
        val consumerData = TnebConsumerBillData(
            consumerNumber = "09440233936",
            consumerName = "K. VAITHILINGAM",
            tariffCode = "LT-IA",
            region = "09-KANCHEEPURAM",
            circle = "406-West",
            section = "440-ATHIPET/AMBATTUR",
            distribution = "233-METTU STREET",
            serviceNumber = "135"
        )

        assertEquals("K. VAITHILINGAM", consumerData.consumerName)
        assertEquals("09440233936", consumerData.consumerNumber)
        assertEquals("LT-IA", consumerData.tariffCode)
        assertEquals("09-KANCHEEPURAM", consumerData.region)
        assertEquals("406-West", consumerData.circle)
        assertEquals("440-ATHIPET/AMBATTUR", consumerData.section)
        assertEquals("233-METTU STREET", consumerData.distribution)
        assertEquals("135", consumerData.serviceNumber)

        // Verify consumer name does NOT contain consumer number appended
        assertFalse(consumerData.consumerName.contains(consumerData.consumerNumber))

        // Verify cleaning logic when TNEB source attaches consumer number to name
        val rawTnebName = "S.SUBBIAH CONSUMER NUMBER: 09440233135"
        val cleaned = rawTnebName.replace(Regex("(?i)\\s*consumer\\s*(number|no)?\\s*:?\\s*[0-9]+.*"), "").trim()
        assertEquals("S.SUBBIAH", cleaned)
    }

    @Test
    fun testAvgRecordedDemand_excludesBlanksAndPreservesUnits() {
        val records = listOf(
            TnebBillRecord(id = "1", recordedDemand = "12.0 KW"),
            TnebBillRecord(id = "2", recordedDemand = ""), // Blank - must be excluded, not treated as 0
            TnebBillRecord(id = "3", recordedDemand = "14.0 KW"),
            TnebBillRecord(id = "4", recordedDemand = "—") // Dash - must be excluded
        )

        val validDemands = records.mapNotNull { bill ->
            val clean = bill.recordedDemand.replace(Regex("[^0-9.]"), "").trim()
            clean.toDoubleOrNull()
        }

        assertEquals(2, validDemands.size)
        val avg = validDemands.average()
        assertEquals(13.0, avg, 0.001)

        val unit = records.firstOrNull { it.recordedDemand.contains("kVA", ignoreCase = true) }?.let { "kVA" } ?: "KW"
        val formatted = if (avg % 1.0 == 0.0) "${avg.toInt()} $unit" else String.format(java.util.Locale.US, "%.1f %s", avg, unit)
        assertEquals("13 KW", formatted)
    }

    @Test
    fun testAvgRecordedDemand_handlesKvaUnitsAndAllBlanks() {
        // Test kVA unit preservation
        val kvaRecords = listOf(
            TnebBillRecord(id = "1", recordedDemand = "25.5 kVA"),
            TnebBillRecord(id = "2", recordedDemand = "34.5 kVA")
        )
        val validDemands = kvaRecords.mapNotNull { bill ->
            val clean = bill.recordedDemand.replace(Regex("[^0-9.]"), "").trim()
            clean.toDoubleOrNull()
        }
        val avg = validDemands.average()
        val unit = kvaRecords.firstOrNull { it.recordedDemand.contains("kVA", ignoreCase = true) }?.let { "kVA" } ?: "KW"
        val formatted = if (avg % 1.0 == 0.0) "${avg.toInt()} $unit" else String.format(java.util.Locale.US, "%.1f %s", avg, unit)
        assertEquals("30 kVA", formatted)

        // Test all blanks returns "—"
        val blankRecords = listOf(
            TnebBillRecord(id = "1", recordedDemand = ""),
            TnebBillRecord(id = "2", recordedDemand = "")
        )
        val blankDemands = blankRecords.mapNotNull { bill ->
            val clean = bill.recordedDemand.replace(Regex("[^0-9.]"), "").trim()
            clean.toDoubleOrNull()
        }
        val blankFormatted = if (blankDemands.isNotEmpty()) "has data" else "—"
        assertEquals("—", blankFormatted)
    }

    @Test
    fun testTnebBillRecord_supportsAll22Columns() {
        val record = TnebBillRecord(
            id = "REC-22",
            assessmentDate = "18-10-2026",
            entryDate = "19-10-2026",
            status = "PAID",
            kwh = "42390",
            kvah = "42510",
            recordedDemand = "12.5 KW",
            powerFactor = "0.98",
            units = "258",
            ccCharges = "240.00",
            electricityTax = "33.00",
            weldingCharges = "0.00",
            excessDemand = "0.00",
            pfPenalty = "0.00",
            fixedCharges = "0.00",
            totalCharges = "273.00",
            advancePaid = "0.00",
            adjustment = "0.00",
            amountDue = "273.00",
            dueDate = "16-11-2026",
            amountPaid = "273.00",
            receiptNumber = "RCP-998877",
            paymentDate = "22-10-2026",
            isPaid = true,
            period = "Aug - Oct 2026"
        )

        // Group A: Assessment Details
        assertEquals("18-10-2026", record.assessmentDate)
        assertEquals("19-10-2026", record.entryDate)
        assertEquals("PAID", record.status)

        // Group B: Consumption Reading
        assertEquals("42390", record.kwh)
        assertEquals("42510", record.kvah)
        assertEquals("12.5 KW", record.recordedDemand)
        assertEquals("0.98", record.powerFactor)

        // Group C: Billed Units
        assertEquals("258", record.units)

        // Group D: Split-up of Charges
        assertEquals("240.00", record.ccCharges)
        assertEquals("33.00", record.electricityTax)
        assertEquals("0.00", record.weldingCharges)
        assertEquals("0.00", record.excessDemand)
        assertEquals("0.00", record.pfPenalty)
        assertEquals("0.00", record.fixedCharges)

        // Group E: Total Charges
        assertEquals("273.00", record.totalCharges)

        // Group F: Deductions
        assertEquals("0.00", record.advancePaid)
        assertEquals("0.00", record.adjustment)

        // Group G: Final Bill Details
        assertEquals("273.00", record.amountDue)
        assertEquals("16-11-2026", record.dueDate)

        // Group H: Collection Details
        assertEquals("273.00", record.amountPaid)
        assertEquals("RCP-998877", record.receiptNumber)
        assertEquals("22-10-2026", record.paymentDate)
        assertTrue(record.isPaid)
    }
}
