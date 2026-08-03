import sys
import re

file_path = "/app/applet/app/src/main/java/com/example/feature/order/ui/OrderSummaryScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add missing imports
imports_to_add = """import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
"""
# find "import androidx.compose.material3.*" and insert after
if "import androidx.compose.material3.*" in content:
    content = content.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\n" + imports_to_add)

# replace OrderReceiptPopup completely
popup_start = content.find("@Composable\nfun OrderReceiptPopup")
if popup_start != -1:
    content = content[:popup_start]

new_popup = """@Composable
fun OrderReceiptPopup(
    uiState: OrderConfirmationUiState,
    onDismiss: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
        maximumFractionDigits = 0
    }}

    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    fun handleDismiss() {
        visible = false
    }

    Dialog(
        onDismissRequest = { handleDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.95f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.95f)
        ) {
            val configuration = LocalConfiguration.current
            val maxH = configuration.screenHeightDp.dp * 0.85f

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = maxH),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(end = 32.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.WbSunny,
                                    contentDescription = "SolarEase Logo",
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
                            onClick = { handleDismiss() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-8).dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Scrollable Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        // Customer Details
                        Text(text = "Customer Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Name: ${uiState.customerName}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Mobile: ${uiState.mobileNumber}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Address: ${uiState.installationAddress}", style = MaterialTheme.typography.bodyMedium)
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        
                        // Solar System Details
                        Text(text = "Solar System Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Capacity: ${uiState.recommendedCapacityKW} kW", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "System Type: ${uiState.systemType}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Mount Type: ${uiState.mountType}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Panel Type: ${uiState.panelType}", style = MaterialTheme.typography.bodyMedium)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Payment Method: ${uiState.paymentMethod}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Payment Ref: ${uiState.paymentReferenceId}", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Payment Status: ${uiState.paymentStatus}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        
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
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                onDownloadPdf()
                                handleDismiss()
                            },
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
        
        LaunchedEffect(visible) {
            if (!visible) {
                delay(200)
                onDismiss()
            }
        }
    }
}
"""

content += new_popup

with open(file_path, "w") as f:
    f.write(content)
