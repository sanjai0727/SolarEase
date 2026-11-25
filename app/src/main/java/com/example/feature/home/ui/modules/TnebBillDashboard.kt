package com.example.feature.home.ui.modules

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.example.core.session.UserSession
import com.example.ui.theme.*
import com.example.ui.components.SolarEaseButton
import com.example.ui.components.SolarEaseButtonVariant
import com.example.ui.components.subtleTopRightGradient
import com.example.ui.theme.SolarEaseSemanticColors

import java.text.NumberFormat
import java.util.Locale


// Internal Screen Destination within the Bill Module
private enum class BillDashboardScreen {
    CURRENT_BILL,
    BILL_HISTORY,
    BILL_DETAILS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillDashboard(
    onNavigateBack: () -> Unit,
    viewModel: TnebBillStatusViewModel,
    onNavigateToSolarLens: () -> Unit = {},
    onCheckBillAgain: () -> Unit = { viewModel.resetValidationState() },
    onReturnToHome: () -> Unit = onNavigateBack
) {
    var activeSubScreen by remember { mutableStateOf(BillDashboardScreen.CURRENT_BILL) }

    // Intercept back press when inspecting sub-screens to return to the current bill result
    BackHandler(enabled = activeSubScreen != BillDashboardScreen.CURRENT_BILL) {
        activeSubScreen = BillDashboardScreen.CURRENT_BILL
    }
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val rawBillData by viewModel.billData.collectAsState()

    // Default safe fallback if billData is still loading or partially empty
    val consumerData = rawBillData ?: TnebConsumerBillData(
        consumerNumber = serviceConnectionNumber
    )
    val bills = consumerData.bills
    val currentBill = consumerData.latestBill ?: TnebBillRecord(
        id = consumerData.consumerNumber.ifBlank { "BILL-CURRENT" },
        assessmentDate = "Not available",
        dueDate = "Not available",
        amountDue = "0",
        units = "0",
        isPaid = consumerData.isDuesNil
    )

    var selectedBill by remember(consumerData) {
        mutableStateOf(currentBill)
    }

    when (activeSubScreen) {
        BillDashboardScreen.CURRENT_BILL -> {
            CurrentBillView(
                serviceNo = consumerData.consumerNumber.ifBlank { serviceConnectionNumber },
                consumerData = consumerData,
                currentBill = currentBill,
                onNavigateBack = onNavigateBack,
                onViewDetails = {
                    selectedBill = currentBill
                    activeSubScreen = BillDashboardScreen.BILL_DETAILS
                },
                onViewHistory = {
                    activeSubScreen = BillDashboardScreen.BILL_HISTORY
                },
                onNavigateToSolarLens = onNavigateToSolarLens,
                onCheckBillAgain = onCheckBillAgain,
                onReturnToHome = onReturnToHome
            )
        }
        BillDashboardScreen.BILL_HISTORY -> {
            BillHistoryView(
                serviceNo = consumerData.consumerNumber.ifBlank { serviceConnectionNumber },
                consumerData = consumerData,
                bills = bills,
                onNavigateBack = { activeSubScreen = BillDashboardScreen.CURRENT_BILL },
                onSelectBill = { bill ->
                    selectedBill = bill
                    activeSubScreen = BillDashboardScreen.BILL_DETAILS
                },
                onNavigateToSolarLens = onNavigateToSolarLens
            )
        }
        BillDashboardScreen.BILL_DETAILS -> {
            BillDetailsView(
                serviceNo = consumerData.consumerNumber.ifBlank { serviceConnectionNumber },
                consumerData = consumerData,
                bill = selectedBill,
                onNavigateBack = { activeSubScreen = BillDashboardScreen.CURRENT_BILL },
                onNavigateToSolarLens = onNavigateToSolarLens
            )
        }
    }
}

// ==============================================================================
// SCREEN 3: CURRENT BILL RESULT (Stitch Approved UI - Real Data Driven)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentBillView(
    serviceNo: String,
    consumerData: TnebConsumerBillData,
    currentBill: TnebBillRecord,
    onNavigateBack: () -> Unit,
    onViewDetails: () -> Unit,
    onViewHistory: () -> Unit,
    onNavigateToSolarLens: () -> Unit,
    onCheckBillAgain: () -> Unit,
    onReturnToHome: () -> Unit = onNavigateBack
) {
    var technicalExpanded by remember { mutableStateOf(false) }
    val isPaid = consumerData.isDuesNil || currentBill.isPaid
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TNEB Bill Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onReturnToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to Home")
                    }
                },
                actions = {
                    IconButton(onClick = onViewHistory) {
                        Icon(Icons.Default.History, contentDescription = "Bill History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Verified Sync Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE8F5E9))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("METRICS RETRIEVED • TNPDCL SYNCED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text("LIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Hero Card: Amount Due & Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CONSUMER INVOICE #${currentBill.id.ifBlank { serviceNo }}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )

                        // Status Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPaid) SolarEaseSemanticColors.SuccessContainer else SolarEaseSemanticColors.WarningContainer
                        ) {
                            Text(
                                text = if (isPaid) "PAID" else "UNPAID",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPaid) SolarEaseSemanticColors.SuccessText else SolarEaseSemanticColors.WarningText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Total Amount Due",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val displayAmount = if (isPaid) {
                        "₹0.00"
                    } else {
                        val rawAmt = currentBill.amountDue.trim()
                        if (rawAmt.startsWith("₹")) rawAmt else "₹${rawAmt}.00"
                    }

                    Text(
                        text = displayAmount,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Due Date / Payment Status chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentBill.dueDate.isNotBlank()) "Due by: ${currentBill.dueDate}" else "No dues pending",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!isPaid) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PAYMENT DUE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarEaseSemanticColors.WarningText,
                                    modifier = Modifier
                                        .background(SolarEaseSemanticColors.WarningContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            } else if (currentBill.paymentDate.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PAID ON ${currentBill.paymentDate}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Metric Strip: Units, Billed Net, Bill Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Units Used", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("${currentBill.units.ifBlank { "0" }} kWh", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(currentBill.status.ifBlank { "NORMAL" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Billed Net", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            val netBilled = currentBill.totalCharges.ifBlank { currentBill.amountDue }.ifBlank { "0" }
                            Text("₹$netBilled", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Assessment", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column {
                            Text("Bill Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(currentBill.assessmentDate.ifBlank { "Not available" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text(currentBill.period.ifBlank { "Bi-Monthly" }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pay Online Button (Disabled/No-dues indicator if paid)
                    SolarEaseButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
                            )
                            context.startActivity(intent)
                        },
                        text = if (isPaid) "No Dues Pending" else "Pay on Official Portal",
                        enabled = !isPaid,
                        trailingIcon = if (!isPaid) Icons.AutoMirrored.Filled.ArrowForward else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action: Re-trigger fresh bill check flow
                    SolarEaseButton(
                        onClick = onCheckBillAgain,
                        text = "Check Bill Status Again",
                        variant = SolarEaseButtonVariant.OUTLINED,
                        leadingIcon = Icons.Default.Refresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Action: Return to Home Screen (Step 2)
                    SolarEaseButton(
                        onClick = onReturnToHome,
                        text = "Return to Home Screen",
                        leadingIcon = Icons.Default.Home,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions 3-column Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionItem(
                    icon = Icons.Default.ReceiptLong,
                    label = "View Bill",
                    modifier = Modifier.weight(1f),
                    onClick = onViewDetails
                )
                QuickActionItem(
                    icon = Icons.Default.History,
                    label = "History",
                    modifier = Modifier.weight(1f),
                    onClick = onViewHistory
                )
                QuickActionItem(
                    icon = Icons.Default.Share,
                    label = "Share",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "TNEB Bill Summary\nConsumer No: ${consumerData.consumerNumber.ifBlank { serviceNo }}\nBill Date: ${currentBill.assessmentDate}\nUnits: ${currentBill.units} kWh\nAmount: ₹${currentBill.totalCharges.ifBlank { currentBill.amountDue }.ifBlank { "0" }}\nStatus: ${if (isPaid) "Paid" else "Pending"}"
                            )
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Share Bill Details"))
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tariff Optimization & Slab Analysis Card
            val unitsDouble = currentBill.units.toDoubleOrNull()
            if (unitsDouble != null && unitsDouble > 0) {
                val slabStatus = when {
                    unitsDouble <= 100 -> "Free Units Tier (100 kWh Subsidy)"
                    unitsDouble <= 200 -> "Low Consumption Tier"
                    unitsDouble <= 400 -> "Standard Domestic Tier"
                    unitsDouble <= 500 -> "Upper Domestic Tier"
                    else -> "High Consumption Tier (Higher Slab Rates)"
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tariff Slab Analysis", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "LT-1A",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your billing cycle recorded ${currentBill.units} kWh units. Current tier: $slabStatus under Tamil Nadu TNEB LT-1A domestic schedule.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Current Cycle Units", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${currentBill.units} kWh", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Tariff Schedule", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Bi-Monthly", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToSolarLens,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Sensors, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Audit Connected Appliances", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Units Analysis Unavailable", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Consumption units are not specified on this statement. Audit your connected appliances with Lens for detailed load tracking.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Technical & Meter Details Accordion (Populated with Real TNEB Data)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { technicalExpanded = !technicalExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("SERVICE CONNECTION DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                            val displayConsumer = consumerData.consumerName.ifBlank { "TNEB Consumer" }
                            val displayTariff = consumerData.normalizedTariff.displayLabel
                            Text("Consumer: $displayConsumer • $displayTariff", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                        }
                        Icon(
                            imageVector = if (technicalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    AnimatedVisibility(visible = technicalExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))

                            TechnicalRow("Service Connection No.", consumerData.consumerNumber.ifBlank { serviceNo.ifBlank { "Not available" } })
                            TechnicalRow("Tariff & Phase", "${consumerData.normalizedTariff.displayLabel} • Phase ${consumerData.phase.ifBlank { "Not available" }}")
                            TechnicalRow("Sanctioned Load", consumerData.sanctionedLoad.ifBlank { "Not available" })
                            val circleInfo = if (consumerData.region.isNotBlank() || consumerData.circle.isNotBlank()) {
                                "${consumerData.region} / ${consumerData.circle}".trim('/', ' ')
                            } else "Not available"
                            TechnicalRow("Distribution Circle", circleInfo)
                            TechnicalRow("Meter Number", consumerData.meterNumber.ifBlank { "Not available" })
                            TechnicalRow("Security Deposit (SD)", consumerData.securityDeposit.ifBlank { "Not available" })
                            TechnicalRow("Registered Address", consumerData.address.ifBlank { "Not available" })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==============================================================================
// SCREEN 4: BILL HISTORY (Populated dynamically from actual TNEB records)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillHistoryView(
    serviceNo: String,
    consumerData: TnebConsumerBillData,
    bills: List<TnebBillRecord>,
    onNavigateBack: () -> Unit,
    onSelectBill: (TnebBillRecord) -> Unit,
    onNavigateToSolarLens: () -> Unit,
    onNavigateToBillAnalytics: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredBills = remember(selectedFilter, bills) {
        when (selectedFilter) {
            "Paid" -> bills.filter { it.isPaid }
            "Pending" -> bills.filter { !it.isPaid }
            else -> bills
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bill History",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.background else SolarBrandBackground,
                    titleContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary,
                    navigationIconContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                )
            )
        },
        containerColor = if (isDark) MaterialTheme.colorScheme.background else SolarBrandBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // ── 1. Redesigned Consumer Information Card ────────────────────────────
            val rawName = consumerData.consumerName
                .replace(Regex("(?i)\\s*consumer\\s*(number|no)?\\s*:?\\s*[0-9]+.*"), "")
                .trim()
                .ifBlank {
                    UserSession.currentUser?.name?.trim()?.ifBlank { "TNEB Consumer" } ?: "TNEB Consumer"
                }
            val tariffLabel = consumerData.normalizedTariff.displayLabel.ifBlank {
                consumerData.tariffCode.ifBlank { consumerData.serviceCategory.ifBlank { "LT-1A · Domestic" } }
            }
            val effectiveConsumerNo = consumerData.consumerNumber.trim().ifBlank { serviceNo.trim() }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF003820) else SolarBrandGreen,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Consumer Name (Cleaned, larger, bold)
                    Column {
                        Text(
                            text = rawName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = tariffLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.2f),
                        thickness = 0.8.dp
                    )

                    // Consumer Number (Slightly bold label and distinct value)
                    Column {
                        Text(
                            text = "Consumer Number",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = effectiveConsumerNo.ifBlank { "Not Connected" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Consumer Location and Service Details
                    val locationFields = listOfNotNull(
                        if (consumerData.region.isNotBlank()) "Region" to consumerData.region else null,
                        if (consumerData.circle.isNotBlank()) "Circle" to consumerData.circle else null,
                        if (consumerData.section.isNotBlank()) "Section" to consumerData.section else null,
                        if (consumerData.distribution.isNotBlank()) "Distribution" to consumerData.distribution else null,
                        if (consumerData.serviceNumber.isNotBlank()) "Service Number" to consumerData.serviceNumber else null
                    )

                    if (locationFields.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                locationFields.forEach { (label, value) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.75f)
                                        )
                                        Text(
                                            text = value,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 2. TNEB Consumption Baseline Card ──────────────────────────────────
            val validDemands = remember(bills) {
                bills.mapNotNull { bill ->
                    val clean = bill.recordedDemand.replace(Regex("[^0-9.]"), "").trim()
                    clean.toDoubleOrNull()
                }
            }
            val avgDemandFormatted = remember(validDemands) {
                if (validDemands.isNotEmpty()) {
                    val avg = validDemands.average()
                    val unit = bills.firstOrNull { it.recordedDemand.contains("kVA", ignoreCase = true) }?.let { "kVA" } ?: "KW"
                    if (avg % 1.0 == 0.0) "${avg.toInt()} $unit" else String.format(Locale.US, "%.1f %s", avg, unit)
                } else "—"
            }

            val avgBill = remember(bills) {
                if (bills.isNotEmpty()) {
                    val validBills = bills.mapNotNull { it.amountDue.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }
                    if (validBills.isNotEmpty()) validBills.average().toInt() else 0
                } else 0
            }
            val paidCount = remember(bills) { bills.count { it.isPaid } }
            val settlementRate = remember(bills) {
                if (bills.isNotEmpty()) ((paidCount.toDouble() / bills.size) * 100).toInt() else 100
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
                shadowElevation = 0.5.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "TNEB LT CONSUMPTION BASELINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Avg Recorded Demand",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                avgDemandFormatted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                            )
                        }
                        Column {
                            Text(
                                "Avg Cycle Bill",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "₹$avgBill",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                            )
                        }
                        Column {
                            Text(
                                "Settlement Rate",
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "$settlementRate% on-time",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF6EE7B7) else SolarBrandGreen
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── 3. Filter Chips and Bill Analytics Icon ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Paid", "Pending").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    filter,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SolarBrandGreen,
                                selectedLabelColor = Color.White,
                                containerColor = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                                labelColor = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SolarBrandGreen else (if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // Chart / Analytics Icon Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                    border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(role = Role.Button) { onNavigateToBillAnalytics() }
                        .semantics {
                            role = Role.Button
                            contentDescription = "Bill Analytics"
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Bill Analytics",
                            tint = SolarBrandGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of Bill Cards or Empty State
            if (filteredBills.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No billing records found for this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            } else {
                filteredBills.forEach { bill ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelectBill(bill) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bill.period.ifBlank { bill.assessmentDate },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (bill.isPaid) MaterialTheme.colorScheme.primaryContainer else SolarEaseSemanticColors.WarningContainer
                                ) {
                                    Text(
                                        text = if (bill.isPaid) "PAID" else "PENDING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (bill.isPaid) MaterialTheme.colorScheme.primary else SolarEaseSemanticColors.WarningText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${bill.units.ifBlank { "0" }} kWh • Assessed: ${bill.assessmentDate.ifBlank { "Not available" }}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val rawAmt = bill.amountDue.trim()
                                val formattedAmt = if (rawAmt.startsWith("₹")) rawAmt else "₹$rawAmt.00"
                                Text(
                                    text = formattedAmt,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (bill.receiptNumber.isNotBlank() || bill.paymentDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (bill.receiptNumber.isNotBlank()) {
                                        Text("Receipt: ${bill.receiptNumber}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    if (bill.paymentDate.isNotBlank()) {
                                        Text("Paid: ${bill.paymentDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Appliance Audit Teaser
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSolarLens)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audit connected appliances", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Identify heavy power draw across cycles with Lens →", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==============================================================================
// SCREEN 5: ITEMIZED BILL DETAILS (Populated with actual TNEB charges)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BillDetailsView(
    serviceNo: String,
    consumerData: TnebConsumerBillData,
    bill: TnebBillRecord,
    onNavigateBack: () -> Unit,
    onNavigateToSolarLens: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val screenBg = if (isDark) MaterialTheme.colorScheme.background else SolarBrandBackground
    val cardBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder
    val cardGradient = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF102A22), Color(0xFF0C1D18)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFCFDF9), Color(0xFFF1F7F1)))
    }
    val subCardBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenLight.copy(alpha = 0.55f)
    val accentGreen = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { /* Download */ }) {
                        Icon(Icons.Default.Download, contentDescription = "Download")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = screenBg,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Download PDF */ },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, cardBorder)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Bill", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    val rawAmt = bill.amountDue.trim()
                    val payLabel = if (rawAmt.isNotBlank() && rawAmt != "0") "Pay ₹$rawAmt" else "No Dues"
                    Button(
                        onClick = { /* Pay Online */ },
                        enabled = !bill.isPaid,
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentGreen,
                            contentColor = Color.White,
                            disabledContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenLight,
                            disabledContentColor = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandGreen
                        )
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (bill.isPaid) "Paid in Full" else "$payLabel Online", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = screenBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Consumer Header Card (Fixed weight so badge never wraps vertically)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .subtleTopRightGradient(shape = RoundedCornerShape(20.dp), radiusMultiplier = 0.8f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = accentGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val cleanName = consumerData.consumerName
                                    .substringBefore("CONSUMER NUMBER", missingDelimiterValue = consumerData.consumerName)
                                    .trim()
                                    .ifBlank { "TNEB Consumer" }
                                Text(
                                    text = cleanName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "No. ${consumerData.consumerNumber.ifBlank { serviceNo.ifBlank { "Not available" } }}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight,
                            border = BorderStroke(1.dp, accentGreen.copy(alpha = 0.2f))
                        ) {
                            Text(
                                text = consumerData.normalizedTariff.displayLabel,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentGreen,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Bill & Due Date Summary Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .subtleTopRightGradient(shape = RoundedCornerShape(20.dp), radiusMultiplier = 0.8f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TOTAL AMOUNT DUE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentGreen,
                                letterSpacing = 0.6.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (bill.isPaid) SolarEaseSemanticColors.SuccessContainer else SolarEaseSemanticColors.WarningContainer
                            ) {
                                Text(
                                    if (bill.isPaid) "PAID" else "PENDING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bill.isPaid) SolarEaseSemanticColors.SuccessText else SolarEaseSemanticColors.WarningText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val rawAmt = bill.amountDue.trim()
                        val displayAmt = if (bill.isPaid) "₹0.00" else (if (rawAmt.startsWith("₹")) rawAmt else "₹$rawAmt.00")
                        Text(displayAmt, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(subCardBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Bill Date", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(bill.assessmentDate.ifBlank { "Not available" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Due Date", fontSize = 11.sp, color = if (bill.isPaid) MaterialTheme.colorScheme.onSurfaceVariant else SolarEaseSemanticColors.ErrorText)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(bill.dueDate.ifBlank { "Not available" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface else SolarEaseSemanticColors.ErrorText)
                            }
                        }
                    }
                }
            }

            // Consumption & Meter Readings Card (Real TNEB Values)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .subtleTopRightGradient(shape = RoundedCornerShape(20.dp), radiusMultiplier = 0.8f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("METER READING & CONSUMPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentGreen, letterSpacing = 0.6.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(subCardBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Billed Consumption", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("${bill.units.ifBlank { "0" }} kWh", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentGreen)
                            }
                            val unitsD = bill.units.toDoubleOrNull() ?: 0.0
                            val dailyAvg = String.format(Locale.US, "%.1f", unitsD / 60.0)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFCFDF9)
                            ) {
                                Text(
                                    "$dailyAvg kWh / day",
                                    fontSize = 11.5.sp,
                                    color = accentGreen,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(12.dp),
                                color = subCardBg,
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("METER READING (KWH)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(bill.kwh.ifBlank { "Not available" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(bill.assessmentDate.ifBlank { "Current cycle" }, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                shape = RoundedCornerShape(12.dp),
                                color = subCardBg,
                                border = BorderStroke(1.dp, cardBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("RECORDED DEMAND", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(bill.recordedDemand.ifBlank { "Not available" }, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(if (bill.powerFactor.isNotBlank()) "PF: ${bill.powerFactor}" else "Entry: ${bill.entryDate.ifBlank { "Synced" }}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Itemized Bill Breakdown Card (Real TNEB Charges Only)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                border = BorderStroke(1.dp, cardBorder),
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(cardGradient)
                        .subtleTopRightGradient(shape = RoundedCornerShape(20.dp), radiusMultiplier = 0.8f)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("BILL ASSESSMENT BREAKDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentGreen, letterSpacing = 0.6.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        BreakdownLineItem("Current Consumption Charges (CC)", "₹" + bill.ccCharges.ifBlank { "0.00" })
                        BreakdownLineItem("Electricity Tax", "₹" + bill.electricityTax.ifBlank { "0.00" })
                        BreakdownLineItem("Fixed Infrastructure Charges", "₹" + bill.fixedCharges.ifBlank { "0.00" })

                        if (bill.weldingCharges.isNotBlank() && bill.weldingCharges != "0") {
                            BreakdownLineItem("Welding Charges", "₹" + bill.weldingCharges)
                        }
                        if (bill.excessDemand.isNotBlank() && bill.excessDemand != "0") {
                            BreakdownLineItem("Excess Demand Charges", "₹" + bill.excessDemand)
                        }
                        if (bill.pfPenalty.isNotBlank() && bill.pfPenalty != "0") {
                            BreakdownLineItem("Power Factor Penalty", "₹" + bill.pfPenalty)
                        }
                        if (bill.advancePaid.isNotBlank() && bill.advancePaid != "0") {
                            BreakdownLineItem("Advance Amount Paid", "-₹" + bill.advancePaid, isSubsidy = true)
                        }
                        if (bill.adjustment.isNotBlank() && bill.adjustment != "0") {
                            BreakdownLineItem("Adjustment", "₹" + bill.adjustment)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = cardBorder)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Net Total Payable", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            val totalPayable = bill.amountDue.ifBlank { bill.totalCharges }.ifBlank { "0" }
                            Text(if (totalPayable.startsWith("₹")) totalPayable else "₹$totalPayable", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = accentGreen)
                        }

                        // Collection info if paid
                        if (bill.isPaid && (bill.amountPaid.isNotBlank() || bill.receiptNumber.isNotBlank() || bill.paymentDate.isNotBlank())) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = subCardBg,
                                border = BorderStroke(1.dp, cardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text("COLLECTION DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accentGreen, letterSpacing = 0.5.sp)
                                    if (bill.amountPaid.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Amount Paid", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("₹${bill.amountPaid}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (bill.receiptNumber.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Receipt No", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(bill.receiptNumber, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (bill.paymentDate.isNotBlank()) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Payment Date", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(bill.paymentDate, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Shared Micro Components ──────────────────────────────────────────────────

@Composable
private fun QuickActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun TechnicalRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun BreakdownLineItem(title: String, amount: String, isSubsidy: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = if (isSubsidy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSubsidy) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = amount,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSubsidy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
