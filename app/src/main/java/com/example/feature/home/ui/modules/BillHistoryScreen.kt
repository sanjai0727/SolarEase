package com.example.feature.home.ui.modules

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.core.session.UserSession

/**
 * Dedicated Bill History Screen Route.
 *
 * Displays previously paid and recorded TNEB billing cycles.
 * Reuses existing models [TnebConsumerBillData] and [TnebBillRecord] from [TnebBillCache],
 * and allows tapping on any historical billing cycle to view its itemized charges breakdown.
 */
@Composable
fun BillHistoryRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSolarLens: () -> Unit = {}
) {
    val billData by TnebBillCache.billData.collectAsState()
    val user = UserSession.currentUser
    val serviceNo = billData?.consumerNumber?.ifBlank { user?.ebServiceNumber ?: "" } ?: (user?.ebServiceNumber ?: "")
    val consumerData = billData ?: TnebConsumerBillData(consumerNumber = serviceNo)
    val bills = consumerData.bills

    var selectedBill by remember { mutableStateOf<TnebBillRecord?>(null) }

    // Intercept back press when inspecting a specific bill to return to history list
    BackHandler(enabled = selectedBill != null) {
        selectedBill = null
    }

    if (selectedBill != null) {
        BillDetailsView(
            serviceNo = consumerData.consumerNumber.ifBlank { serviceNo },
            consumerData = consumerData,
            bill = selectedBill!!,
            onNavigateBack = { selectedBill = null },
            onNavigateToSolarLens = onNavigateToSolarLens
        )
    } else {
        BillHistoryView(
            serviceNo = consumerData.consumerNumber.ifBlank { serviceNo },
            consumerData = consumerData,
            bills = bills,
            onNavigateBack = onNavigateBack,
            onSelectBill = { bill -> selectedBill = bill },
            onNavigateToSolarLens = onNavigateToSolarLens
        )
    }
}
