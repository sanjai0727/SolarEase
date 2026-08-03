import sys

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/OrderSummaryScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add imports for dialog and context
imports = """import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.feature.order.util.ReceiptPdfGenerator
"""

content = content.replace("import androidx.compose.material3.*\\nimport androidx.compose.runtime.*", imports)

# Add showPopup state in OrderSummaryRoute
state_var = """    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(true) }
    var showReceiptPopup by remember { mutableStateOf(false) }
    val context = LocalContext.current"""

content = content.replace("""    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isLoading by remember { mutableStateOf(true) }""", state_var)

# Find the OutlinedButton and update it to show the popup
btn_old = """        OutlinedButton(
            onClick = { /* View order details action - placeholder */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 12.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("View Order Details", style = MaterialTheme.typography.titleMedium)
        }"""

btn_new = """        OutlinedButton(
            onClick = { showReceiptPopup = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 12.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("View Order Details", style = MaterialTheme.typography.titleMedium)
        }
        
        if (showReceiptPopup) {
            OrderReceiptPopup(
                uiState = uiState,
                onDismiss = { showReceiptPopup = false },
                onDownloadPdf = {
                    ReceiptPdfGenerator.generateAndSharePdf(context, uiState)
                }
            )
        }"""

content = content.replace(btn_old, btn_new)

# Add the popup Composable at the end
popup_composable = """
@Composable
fun OrderReceiptPopup(
    uiState: OrderConfirmationUiState,
    onDismiss: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }}

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(end = 40.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Order Receipt",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ref ID: ${uiState.orderReference}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = uiState.orderDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("✕", style = MaterialTheme.typography.titleLarge)
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Customer Details
                    Text(text = "Customer Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Name: ${uiState.customerName}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Mobile: ${uiState.mobileNumber}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Address: ${uiState.installationAddress}", style = MaterialTheme.typography.bodyMedium)
                    
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    // Solar System Details
                    Text(text = "Solar System Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Capacity: ${uiState.recommendedCapacityKW} kW", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "System Type: ${uiState.systemType}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Mount Type: ${uiState.mountType}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Panel Type: ${uiState.panelType}", style = MaterialTheme.typography.bodyMedium)

                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                    
                    // Payment Summary
                    Text(text = "Payment Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Gross Total:", style = MaterialTheme.typography.bodyMedium)
                        Text(text = currencyFormat.format(uiState.grossTotal), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Government Subsidy:", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "-${currencyFormat.format(uiState.subsidyAmount)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Net Amount Paid:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = currencyFormat.format(uiState.netPayable), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Payment Method: ${uiState.paymentMethod}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Payment Ref: ${uiState.paymentReferenceId}", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Payment Status: ${uiState.paymentStatus}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Footer
                    Text(
                        text = "Thank you for choosing SolarEase.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Fixed Download Button
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onDownloadPdf,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text("Download PDF", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
"""

content = content + popup_composable

with open(file_path, "w") as f:
    f.write(content)
