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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

// Color Tokens matching Stitch Consumer Energy System
private val SolarEaseEmerald = Color(0xFF1E7038)
private val SolarEaseEmeraldDark = Color(0xFF145227)
private val SolarEaseEmeraldContainer = Color(0xFFE8F5E9)
private val SoftBackground = Color(0xFFF5F7FB)
private val InfoBlueContainer = Color(0xFFE3F2FD)
private val InfoBlueText = Color(0xFF1976D2)
private val CardBorderColor = Color(0xFFE2E8F0)
private val TextDark = Color(0xFF1E222D)
private val TextMuted = Color(0xFF5D6778)
private val WarningAmber = Color(0xFFD97706)
private val WarningAmberContainer = Color(0xFFFEF3C7)
private val ErrorRed = Color(0xFFDC2626)

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
                    containerColor = SoftBackground,
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark,
                    actionIconContentColor = TextDark
                )
            )
        },
        containerColor = SoftBackground
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
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("METRICS RETRIEVED • TNPDCL SYNCED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SolarEaseEmerald)
                }
                Text("LIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SolarEaseEmerald)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Hero Card: Amount Due & Status
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )

                        // Status Pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPaid) SolarEaseEmeraldContainer else WarningAmberContainer
                        ) {
                            Text(
                                text = if (isPaid) "PAID" else "UNPAID",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPaid) SolarEaseEmerald else WarningAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Total Amount Due",
                        fontSize = 13.sp,
                        color = TextMuted
                    )

                    val displayAmount = if (isPaid) {
                        "₹0.00"
                    } else {
                        val rawAmt = currentBill.amountDue.trim()
                        if (rawAmt.startsWith("₹")) rawAmt else "₹${rawAmt}.00"
                    }

                    Text(
                        text = displayAmount,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Due Date / Payment Status chip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentBill.dueDate.isNotBlank()) "Due by: ${currentBill.dueDate}" else "No dues pending",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            if (!isPaid) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PAYMENT DUE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber,
                                    modifier = Modifier
                                        .background(WarningAmberContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            } else if (currentBill.paymentDate.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PAID ON ${currentBill.paymentDate}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarEaseEmerald,
                                    modifier = Modifier
                                        .background(SolarEaseEmeraldContainer, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Metric Strip: Units, Billed Net, Bill Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Units Used", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text("${currentBill.units.ifBlank { "0" }} kWh", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(currentBill.status.ifBlank { "NORMAL" }, fontSize = 10.sp, color = SolarEaseEmerald, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text("Billed Net", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            val netBilled = currentBill.totalCharges.ifBlank { currentBill.amountDue }.ifBlank { "0" }
                            Text("₹$netBilled", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text("Assessment", fontSize = 10.sp, color = TextMuted)
                        }
                        Column {
                            Text("Bill Date", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(currentBill.assessmentDate.ifBlank { "Not available" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(currentBill.period.ifBlank { "Bi-Monthly" }, fontSize = 10.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Pay Online Button (Disabled/No-dues indicator if paid)
                    Button(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
                            )
                            context.startActivity(intent)
                        },
                        enabled = !isPaid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarEaseEmerald,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Text(if (isPaid) "No Dues Pending" else "Pay on Official Portal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (!isPaid) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Action: Re-trigger fresh bill check flow
                    OutlinedButton(
                        onClick = onCheckBillAgain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, SolarEaseEmerald),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SolarEaseEmerald
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = SolarEaseEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Check Bill Status Again",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary Action: Return to Home Screen (Step 2)
                    Button(
                        onClick = onReturnToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SolarEaseEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                            Text(
                                text = "Return to Home Screen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
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

            // Solar Rooftop Sizing & Return Card
            val unitsDouble = currentBill.units.toDoubleOrNull()
            if (unitsDouble != null && unitsDouble > 0) {
                val solarKw = String.format(Locale.US, "%.1f", (unitsDouble / 150.0).coerceAtLeast(1.0))
                val annualSavingsNum = (unitsDouble * 6.5 * 6).toInt()
                val savingsFormatted = "₹" + NumberFormat.getNumberInstance(Locale.US).format(annualSavingsNum) + " / yr"
                val systemCost = solarKw.toDouble() * 55000.0
                val paybackYears = String.format(Locale.US, "%.1f Yrs ROI", (systemCost / annualSavingsNum.coerceAtLeast(1)).coerceIn(3.0, 8.0))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Solar Rooftop Sizing", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SolarEaseEmeraldContainer
                            ) {
                                Text(
                                    "ESTIMATED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SolarEaseEmerald,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Based on ${currentBill.units} kWh bi-monthly consumption, a recommended $solarKw kW solar system can offset grid demand under Tamil Nadu Net Metering.",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Projected Savings", fontSize = 11.sp, color = TextMuted)
                                Text(savingsFormatted, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SolarEaseEmerald)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Estimated Return", fontSize = 11.sp, color = TextMuted)
                                Text(paybackYears, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onNavigateToSolarLens,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SolarEaseEmerald),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SolarEaseEmerald)
                        ) {
                            Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify with Solar Lens", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Solar Sizing Unavailable", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Consumption units are not specified on this statement. Audit your connected appliances with Solar Lens for verified sizing.",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Technical & Meter Details Accordion (Populated with Real TNEB Data)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor)
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
                            Text("SERVICE CONNECTION DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SolarEaseEmerald, letterSpacing = 0.5.sp)
                            val displayConsumer = consumerData.consumerName.ifBlank { "TNEB Consumer" }
                            val displayTariff = consumerData.normalizedTariff.displayLabel
                            Text("Consumer: $displayConsumer • $displayTariff", fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        }
                        Icon(
                            imageVector = if (technicalExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle",
                            tint = SolarEaseEmerald
                        )
                    }

                    AnimatedVisibility(visible = technicalExpanded) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
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
    onNavigateToSolarLens: () -> Unit
) {
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
                title = { Text("Bill History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftBackground,
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark
                )
            )
        },
        containerColor = SoftBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Consumer Header Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SolarEaseEmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        val name = consumerData.consumerName.ifBlank { "TNEB Consumer" }
                        val tariff = consumerData.normalizedTariff.displayLabel
                        Text("$name • $tariff", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                        val circle = consumerData.region.ifBlank { consumerData.circle.ifBlank { "Tamil Nadu" } }
                        Text("$serviceNo • $circle", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Annual Baseline Overview Strip (Computed from Real History)
            val avgUnits = if (bills.isNotEmpty()) {
                bills.mapNotNull { it.units.toDoubleOrNull() }.average().toInt()
            } else 0
            val avgBill = if (bills.isNotEmpty()) {
                bills.mapNotNull { it.amountDue.toDoubleOrNull() }.average().toInt()
            } else 0
            val paidCount = bills.count { it.isPaid }
            val settlementRate = if (bills.isNotEmpty()) ((paidCount.toDouble() / bills.size) * 100).toInt() else 100

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TNEB LT CONSUMPTION BASELINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Avg Units", fontSize = 11.sp, color = TextMuted)
                            Text("$avgUnits kWh", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                        Column {
                            Text("Avg Cycle Bill", fontSize = 11.sp, color = TextMuted)
                            Text("₹$avgBill", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                        Column {
                            Text("Settlement Rate", fontSize = 11.sp, color = TextMuted)
                            Text("$settlementRate% on-time", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SolarEaseEmerald)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Paid", "Pending").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SolarEaseEmerald,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = TextDark
                        ),
                        border = BorderStroke(1.dp, if (isSelected) SolarEaseEmerald else CardBorderColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of Bill Cards or Empty State
            if (filteredBills.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No billing records found for this filter.", color = TextMuted, fontSize = 13.sp)
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, CardBorderColor)
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
                                    color = TextDark
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (bill.isPaid) SolarEaseEmeraldContainer else WarningAmberContainer
                                ) {
                                    Text(
                                        text = if (bill.isPaid) "PAID" else "PENDING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (bill.isPaid) SolarEaseEmerald else WarningAmber,
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
                                    color = TextMuted
                                )
                                val rawAmt = bill.amountDue.trim()
                                val formattedAmt = if (rawAmt.startsWith("₹")) rawAmt else "₹$rawAmt.00"
                                Text(
                                    text = formattedAmt,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                            }

                            if (bill.receiptNumber.isNotBlank() || bill.paymentDate.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (bill.receiptNumber.isNotBlank()) {
                                        Text("Receipt: ${bill.receiptNumber}", fontSize = 11.sp, color = SolarEaseEmerald)
                                    }
                                    if (bill.paymentDate.isNotBlank()) {
                                        Text("Paid: ${bill.paymentDate}", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Solar Lens Teaser
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSolarLens)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WbSunny, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Simulate solar zero-bill setup", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                        Text("Calculate system size and savings in Solar Lens →", fontSize = 11.sp, color = SolarEaseEmeraldDark)
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
                    containerColor = SoftBackground,
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark,
                    actionIconContentColor = TextDark
                )
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
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
                        border = BorderStroke(1.dp, CardBorderColor)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextDark)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PDF Bill", color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                            containerColor = SolarEaseEmerald,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = TextMuted
                        )
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (bill.isPaid) "Paid in Full" else "$payLabel Online", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = SoftBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Consumer Header Ribbon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(consumerData.consumerName.ifBlank { "TNEB Consumer" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Text("No. ${consumerData.consumerNumber.ifBlank { serviceNo.ifBlank { "Not available" } }}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF1F5F9)) {
                        Text(consumerData.normalizedTariff.displayLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bill & Due Date Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL AMOUNT DUE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (bill.isPaid) SolarEaseEmeraldContainer else WarningAmberContainer
                        ) {
                            Text(
                                if (bill.isPaid) "PAID" else "PENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (bill.isPaid) SolarEaseEmerald else WarningAmber,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val rawAmt = bill.amountDue.trim()
                    val displayAmt = if (bill.isPaid) "₹0.00" else (if (rawAmt.startsWith("₹")) rawAmt else "₹$rawAmt.00")
                    Text(displayAmt, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextDark)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Bill Date", fontSize = 11.sp, color = TextMuted)
                            Text(bill.assessmentDate.ifBlank { "Not available" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Due Date", fontSize = 11.sp, color = if (bill.isPaid) TextMuted else ErrorRed)
                            Text(bill.dueDate.ifBlank { "Not available" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (bill.isPaid) TextDark else ErrorRed)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Consumption & Meter Readings Card (Real TNEB Values)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("METER READING & CONSUMPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SolarEaseEmerald, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Billed Consumption", fontSize = 11.sp, color = TextMuted)
                            Text("${bill.units.ifBlank { "0" }} kWh", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                        val unitsD = bill.units.toDoubleOrNull() ?: 0.0
                        val dailyAvg = String.format(Locale.US, "%.1f", unitsD / 60.0)
                        Text("$dailyAvg kWh / day", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAFCFF),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("METER READING (KWH)", fontSize = 10.sp, color = TextMuted)
                                Text(bill.kwh.ifBlank { "Not available" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(bill.assessmentDate.ifBlank { "Current cycle" }, fontSize = 10.sp, color = TextMuted)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFAFCFF),
                            border = BorderStroke(1.dp, CardBorderColor)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("RECORDED DEMAND", fontSize = 10.sp, color = TextMuted)
                                Text(bill.recordedDemand.ifBlank { "Not available" }, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(if (bill.powerFactor.isNotBlank()) "PF: ${bill.powerFactor}" else "Entry: ${bill.entryDate.ifBlank { "Synced" }}", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Itemized Bill Breakdown Card (Real TNEB Charges Only)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("BILL ASSESSMENT BREAKDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SolarEaseEmerald, letterSpacing = 0.5.sp)
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
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SolarEaseEmeraldContainer, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Net Total Payable", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                        val totalPayable = bill.amountDue.ifBlank { bill.totalCharges }.ifBlank { "0" }
                        Text(if (totalPayable.startsWith("₹")) totalPayable else "₹$totalPayable", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = SolarEaseEmerald)
                    }

                    // Collection info if paid
                    if (bill.isPaid && (bill.amountPaid.isNotBlank() || bill.receiptNumber.isNotBlank() || bill.paymentDate.isNotBlank())) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, CardBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("COLLECTION DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (bill.amountPaid.isNotBlank()) {
                                    Text("Amount Paid: ₹${bill.amountPaid}", fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                                }
                                if (bill.receiptNumber.isNotBlank()) {
                                    Text("Receipt No: ${bill.receiptNumber}", fontSize = 12.sp, color = TextDark)
                                }
                                if (bill.paymentDate.isNotBlank()) {
                                    Text("Payment Date: ${bill.paymentDate}", fontSize = 12.sp, color = TextDark)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorderColor)
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
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextDark)
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
        Text(label, fontSize = 12.sp, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextDark, textAlign = androidx.compose.ui.text.style.TextAlign.End)
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
            color = if (isSubsidy) SolarEaseEmerald else TextDark,
            fontWeight = if (isSubsidy) FontWeight.SemiBold else FontWeight.Normal
        )
        Text(
            text = amount,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSubsidy) SolarEaseEmerald else TextDark
        )
    }
}
