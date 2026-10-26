package com.example.feature.home.ui.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TnebBillStatusHtmlParserTest {

    @Test
    fun parseJson_realPortalData_extractsAllFieldsCorrectly() {
        val realJson = """
        {
            "consumerName": "S.SUBBIAH",
            "consumerNumber": "09440233135",
            "region": "09-CHENNAI-SOUTH",
            "circle": "440-CHENNAI-SOUTH",
            "section": "233-ALWARTHIRUNAGAR",
            "distribution": "135-KALAIGNAR KARUNANIDHI NAGAR",
            "serviceNumber": "09-440-233-135",
            "address": "PLOT NO 24/F-1 DOOR NO 1, 1ST MAIN ROAD, SURESH NAGAR VALASARAWAKKAM CHENNAI 600087",
            "serviceStatus": "LIVE",
            "aadhar": "XXXXXXXX4567",
            "gst": "Not applicable",
            "pan": "Not applicable",
            "phase": "1",
            "sanctionedLoad": "4 KW",
            "tempReducedLoad": "0",
            "meterNumber": "123",
            "securityDeposit": "1500.00",
            "mcdDeposit": "0.00",
            "serviceCategory": "LT",
            "tariffCode": "LA1A / DOMESTIC",
            "consumerRating": "GOOD",
            "duesText": "DUES TO BE PAID IS \"NIL\"",
            "isDuesNil": true,
            "records": [
                {
                    "assessmentDate": "18-09-2024",
                    "entryDate": "19-09-2024",
                    "status": "ASSESSED",
                    "kwh": "302",
                    "kvah": "0",
                    "recordedDemand": "0",
                    "powerFactor": "0.95",
                    "units": "302",
                    "ccCharges": "479.00",
                    "electyTax": "0.00",
                    "welding": "0.00",
                    "excessDemand": "0.00",
                    "pfPenalty": "0.00",
                    "fixedCharges": "0.00",
                    "totalCharges": "479.00",
                    "advancePaid": "0.00",
                    "adjustment": "0.00",
                    "amountDue": "479.00",
                    "dueDate": "08-10-2024",
                    "amountPaid": "479.00",
                    "receiptNo": "09440233135092404",
                    "paymentDate": "26-09-2024",
                    "isPaid": true
                },
                {
                    "assessmentDate": "19-07-2024",
                    "entryDate": "20-07-2024",
                    "status": "ASSESSED",
                    "kwh": "250",
                    "kvah": "0",
                    "recordedDemand": "0",
                    "powerFactor": "0.95",
                    "units": "250",
                    "ccCharges": "350.00",
                    "electyTax": "0.00",
                    "welding": "0.00",
                    "excessDemand": "0.00",
                    "pfPenalty": "0.00",
                    "fixedCharges": "0.00",
                    "totalCharges": "350.00",
                    "advancePaid": "0.00",
                    "adjustment": "0.00",
                    "amountDue": "350.00",
                    "dueDate": "08-08-2024",
                    "amountPaid": "350.00",
                    "receiptNo": "09440233135072401",
                    "paymentDate": "25-07-2024",
                    "isPaid": true
                }
            ]
        }
        """.trimIndent()

        val result = TnebBillStatusHtmlParser.parseJson(realJson)
        assertNotNull(result)
        requireNotNull(result)

        // 1. Consumer Details
        assertEquals("S.SUBBIAH", result.consumerName)
        assertEquals("09440233135", result.consumerNumber)
        assertEquals("09-CHENNAI-SOUTH", result.region)
        assertEquals("440-CHENNAI-SOUTH", result.circle)
        assertEquals("233-ALWARTHIRUNAGAR", result.section)
        assertEquals("135-KALAIGNAR KARUNANIDHI NAGAR", result.distribution)
        assertEquals("09-440-233-135", result.serviceNumber)
        assertEquals("LIVE", result.serviceStatus)
        assertEquals("4 KW", result.sanctionedLoad)
        assertEquals("123", result.meterNumber)
        assertEquals("LA1A / DOMESTIC", result.tariffCode)
        assertTrue(result.isDuesNil)

        // 2. Bill Records
        assertEquals(2, result.bills.size)
        val latestBill = result.bills[0]
        assertEquals("18-09-2024", latestBill.assessmentDate)
        assertEquals("302", latestBill.units)
        assertEquals("479.00", latestBill.totalCharges)
        assertEquals("479.00", latestBill.amountDue)
        assertEquals("08-10-2024", latestBill.dueDate)
        assertEquals("479.00", latestBill.amountPaid)
        assertEquals("09440233135092404", latestBill.receiptNumber)
        assertEquals("26-09-2024", latestBill.paymentDate)
        assertTrue(latestBill.isPaid)

        // 3. Formatting Helpers
        assertEquals("₹0.00", result.totalDuesFormatted)
        assertEquals("PAID", result.displayStatus)
    }

    @Test
    fun parseJson_unpaidBill_extractsAmountDueAndDueDate() {
        val unpaidJson = """
        {
            "consumerName": "K. RAMANATHAN",
            "consumerNumber": "04234567891",
            "tariffCode": "01 - DOMESTIC",
            "duesText": "AMOUNT TO BE PAID: ₹1450.00",
            "isDuesNil": false,
            "records": [
                {
                    "assessmentDate": "15-10-2026",
                    "entryDate": "16-10-2026",
                    "units": "450",
                    "totalCharges": "1450.00",
                    "amountDue": "1450.00",
                    "dueDate": "05-11-2026",
                    "amountPaid": "0.00",
                    "receiptNo": "",
                    "paymentDate": "",
                    "isPaid": false
                }
            ]
        }
        """.trimIndent()

        val result = TnebBillStatusHtmlParser.parseJson(unpaidJson)
        assertNotNull(result)
        requireNotNull(result)

        assertFalse(result.isDuesNil)
        assertEquals("₹1450.00", result.totalDuesFormatted)
        assertEquals("UNPAID", result.displayStatus)
        assertEquals(1, result.bills.size)

        val latestBill = result.bills[0]
        assertFalse(latestBill.isPaid)
        assertEquals("1450.00", latestBill.amountDue)
        assertEquals("05-11-2026", latestBill.dueDate)
        assertEquals("Not available", latestBill.receiptNumber.ifBlank { "Not available" })
    }

    @Test
    fun parseHtml_fallbackTable_extractsLedgerData() {
        val html = """
        <html>
        <body>
            <div id="consdetails">
                <table>
                    <tr><td>Consumer Name</td><td>S.SUBBIAH</td></tr>
                    <tr><td>Consumer Number</td><td>09440233135</td></tr>
                    <tr><td>Sanctioned Load</td><td>4 KW</td></tr>
                    <tr><td>Tariff</td><td>LA1A / DOMESTIC</td></tr>
                    <tr><td>Meter Number</td><td>123</td></tr>
                    <tr><td>DUES TO BE PAID IS "NIL"</td></tr>
                </table>
                <table class="ui-datatable">
                    <thead><tr><th>Date</th><th>Entry</th><th>Status</th><th>Units</th><th>Total</th><th>Amount Due</th><th>Due Date</th><th>Amount Paid</th><th>Receipt No</th><th>Date</th></tr></thead>
                    <tbody>
                        <tr>
                            <td>18-09-2024</td><td>19-09-2024</td><td>ASSESSED</td><td>302</td><td>0</td><td>0</td><td>0</td><td>302</td>
                            <td>479.00</td><td>0.00</td><td>0.00</td><td>0.00</td><td>0.00</td><td>0.00</td>
                            <td>479.00</td><td>0.00</td><td>0.00</td><td>479.00</td><td>08-10-2024</td>
                            <td>479.00</td><td>09440233135092404</td><td>26-09-2024</td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </body>
        </html>
        """.trimIndent()

        val result = TnebBillStatusHtmlParser.parseHtml(html)
        assertNotNull(result)
        requireNotNull(result)

        assertEquals("S.SUBBIAH", result.consumerName)
        assertEquals("09440233135", result.consumerNumber)
        assertEquals("4 KW", result.sanctionedLoad)
        assertEquals("LA1A / DOMESTIC", result.tariffCode)
        assertTrue(result.isDuesNil)
        assertEquals(1, result.bills.size)

        val bill = result.bills[0]
        assertEquals("18-09-2024", bill.assessmentDate)
        assertEquals("302", bill.units)
        assertEquals("479.00", bill.totalCharges)
        assertEquals("09440233135092404", bill.receiptNumber)
        assertTrue(bill.isPaid)
    }

    @Test
    fun parseJson_errorPayload_returnsNull() {
        val errorJson = """
        {
            "isError": true,
            "errorMessage": "Invalid CAPTCHA entered"
        }
        """.trimIndent()

        val result = TnebBillStatusHtmlParser.parseJson(errorJson)
        assertNull(result)
    }

    @Test
    fun parseJson_invalidJson_returnsNull() {
        val result = TnebBillStatusHtmlParser.parseJson("not valid json string {{{")
        assertNull(result)
    }
}
