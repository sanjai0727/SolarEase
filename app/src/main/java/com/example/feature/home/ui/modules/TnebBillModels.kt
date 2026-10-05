package com.example.feature.home.ui.modules

import kotlinx.serialization.Serializable

/**
 * Structured data models representing real TNEB / TNPDCL portal responses.
 * Extracted directly from https://www.tnebltd.gov.in/BillStatus/consDetails
 */
@Serializable
data class TnebConsumerBillData(
    val consumerName: String = "",
    val consumerNumber: String = "",
    val region: String = "",
    val circle: String = "",
    val section: String = "",
    val distribution: String = "",
    val serviceNumber: String = "",
    val address: String = "",
    val serviceStatus: String = "",
    val aadhar: String = "",
    val gst: String = "",
    val pan: String = "",
    val phase: String = "",
    val sanctionedLoad: String = "",
    val tempReducedLoad: String = "",
    val meterNumber: String = "",
    val securityDeposit: String = "",
    val mcdDeposit: String = "",
    val serviceCategory: String = "",
    val tariffCode: String = "",
    val consumerRating: String = "",
    val duesText: String = "",
    val isDuesNil: Boolean = false,
    val bills: List<TnebBillRecord> = emptyList()
) {
    val latestBill: TnebBillRecord?
        get() = bills.firstOrNull()

    val normalizedTariff: NormalizedTariff
        get() = TariffResolver.resolve(tariffCode.ifBlank { serviceCategory })

    val totalDuesFormatted: String
        get() = when {
            isDuesNil -> "₹0.00"
            latestBill != null && latestBill?.isPaid == false -> {
                val amt = latestBill?.amountDue?.trim() ?: "0"
                if (amt.startsWith("₹")) amt else "₹$amt"
            }
            latestBill != null && latestBill?.isPaid == true -> "₹0.00"
            else -> "₹0.00"
        }

    val displayStatus: String
        get() = when {
            isDuesNil -> "PAID"
            latestBill != null && latestBill?.isPaid == true -> "PAID"
            latestBill != null && latestBill?.isPaid == false -> "UNPAID"
            else -> "NO DUES"
        }
}

/**
 * Represents an individual billing cycle row from the 22-column TNEB table:
 * "Consumption Charges and Collection Details"
 */
@Serializable
data class TnebBillRecord(
    val id: String = "",
    val assessmentDate: String = "",
    val entryDate: String = "",
    val status: String = "",
    val kwh: String = "",
    val kvah: String = "",
    val recordedDemand: String = "",
    val powerFactor: String = "",
    val units: String = "",
    val ccCharges: String = "",
    val electricityTax: String = "",
    val weldingCharges: String = "",
    val excessDemand: String = "",
    val pfPenalty: String = "",
    val fixedCharges: String = "",
    val totalCharges: String = "",
    val advancePaid: String = "",
    val adjustment: String = "",
    val amountDue: String = "",
    val dueDate: String = "",
    val amountPaid: String = "",
    val receiptNumber: String = "",
    val paymentDate: String = "",
    val isPaid: Boolean = false,
    val period: String = ""
)
