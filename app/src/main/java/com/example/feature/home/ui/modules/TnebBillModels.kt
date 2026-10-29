package com.example.feature.home.ui.modules

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale

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

/**
 * UI State representing the electricity bill status for the Home screen hero area.
 */
sealed interface PendingBillUiState {
    /** State B: No connected bill, or user has no pending bill to display */
    data object None : PendingBillUiState

    /** State C: Bill status is loading / verifying with utility */
    data object Loading : PendingBillUiState

    /** State D: Bill retrieval failed or requires verification */
    data class RetrievalFailed(
        val message: String = "Unable to retrieve your latest bill.",
        val consumerNumber: String? = null
    ) : PendingBillUiState

    /** State A & E: Pending / outstanding electricity bill exists */
    data class Pending(
        val amountFormatted: String,
        val rawAmount: Double?,
        val dueDateFormatted: String?,
        val rawDueDate: String?,
        val billingPeriod: String?,
        val billDate: String?,
        val consumerNumber: String?,
        val isPartiallyAvailable: Boolean = false
    ) : PendingBillUiState

    /** State F: Latest bill is verified as paid / no outstanding dues */
    data class Paid(
        val consumerNumber: String?,
        val lastPaymentDate: String? = null,
        val billingPeriod: String? = null
    ) : PendingBillUiState
}

/**
 * Safely parses and formats raw dates from TNEB (e.g. "05-11-2026", "25-Oct-2026", "15/10/2024")
 * into human-readable format like "5 November 2026".
 * Falls back to the raw string if already formatted or unusual.
 */
fun formatTnebDate(rawDate: String?): String? {
    if (rawDate.isNullOrBlank()) return null
    val trimmed = rawDate.trim()
    val patterns = listOf(
        "dd-MM-yyyy",
        "dd/MM/yyyy",
        "dd.MM.yyyy",
        "yyyy-MM-dd",
        "dd-MMM-yyyy",
        "dd MMM yyyy",
        "d-M-yyyy",
        "d/M/yyyy"
    )
    for (pattern in patterns) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
            sdf.isLenient = false
            val parsed = sdf.parse(trimmed)
            if (parsed != null) {
                val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
                return outputFormat.format(parsed)
            }
        } catch (_: Exception) {}
    }
    return trimmed
}

/**
 * Formats raw amount strings from TNEB (e.g. "1450.00", "1450") into clean Indian Rupee format "₹1,450".
 */
fun formatTnebAmount(rawAmount: String?): String? {
    if (rawAmount.isNullOrBlank()) return null
    val cleaned = rawAmount.replace("₹", "").replace(",", "").trim()
    val num = cleaned.toDoubleOrNull() ?: return if (rawAmount.startsWith("₹")) rawAmount else "₹$rawAmount"
    return if (num % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "₹%,.0f", num)
    } else {
        String.format(Locale.getDefault(), "₹%,.2f", num)
    }
}

/**
 * Computes the PendingBillUiState based on real consumer data, active retrieval state, and errors.
 */
fun computePendingBillState(
    billData: TnebConsumerBillData?,
    userEbNumber: String? = null,
    isRefreshing: Boolean = false,
    isLoading: Boolean = false,
    retrievalError: String? = null
): PendingBillUiState {
    if (isLoading || (isRefreshing && billData == null)) {
        return PendingBillUiState.Loading
    }
    if (retrievalError != null) {
        val consumerNo = billData?.consumerNumber?.ifBlank { userEbNumber } ?: userEbNumber
        return PendingBillUiState.RetrievalFailed(
            message = retrievalError,
            consumerNumber = consumerNo
        )
    }
    if (billData == null || (billData.bills.isEmpty() && billData.consumerNumber.isBlank())) {
        return PendingBillUiState.None
    }

    val consumerNo = billData.consumerNumber.ifBlank { userEbNumber }
    if (billData.isDuesNil) {
        return PendingBillUiState.Paid(
            consumerNumber = consumerNo,
            billingPeriod = billData.latestBill?.period
        )
    }

    val latest = billData.latestBill
    if (latest == null) {
        return PendingBillUiState.RetrievalFailed(
            message = "Unable to retrieve your latest bill.",
            consumerNumber = consumerNo
        )
    }

    if (latest.isPaid) {
        return PendingBillUiState.Paid(
            consumerNumber = consumerNo,
            lastPaymentDate = latest.paymentDate.takeIf { it.isNotBlank() },
            billingPeriod = latest.period.takeIf { it.isNotBlank() } ?: latest.assessmentDate.takeIf { it.isNotBlank() }
        )
    }

    // Bill is unpaid
    val rawAmountStr = latest.amountDue.trim()
    val cleanedAmount = rawAmountStr.replace("₹", "").replace(",", "").trim()
    val rawAmount = cleanedAmount.toDoubleOrNull()

    if (rawAmount == 0.0) {
        return PendingBillUiState.Paid(
            consumerNumber = consumerNo,
            billingPeriod = latest.period.takeIf { it.isNotBlank() } ?: latest.assessmentDate.takeIf { it.isNotBlank() }
        )
    }

    val hasAmount = rawAmount != null && rawAmount > 0.0
    val hasDueDate = latest.dueDate.isNotBlank()
    val isPartiallyAvailable = !hasAmount || !hasDueDate

    val formattedAmount = if (hasAmount) {
        formatTnebAmount(rawAmountStr) ?: "₹$rawAmountStr"
    } else if (rawAmountStr.isNotBlank() && rawAmountStr != "0") {
        if (rawAmountStr.startsWith("₹")) rawAmountStr else "₹$rawAmountStr"
    } else {
        "Amount unavailable"
    }

    val formattedDueDate = if (hasDueDate) {
        formatTnebDate(latest.dueDate)
    } else {
        null
    }

    return PendingBillUiState.Pending(
        amountFormatted = formattedAmount,
        rawAmount = rawAmount,
        dueDateFormatted = formattedDueDate,
        rawDueDate = latest.dueDate.takeIf { it.isNotBlank() },
        billingPeriod = latest.period.takeIf { it.isNotBlank() } ?: latest.assessmentDate.takeIf { it.isNotBlank() },
        billDate = latest.assessmentDate.takeIf { it.isNotBlank() },
        consumerNumber = consumerNo,
        isPartiallyAvailable = isPartiallyAvailable
    )
}
