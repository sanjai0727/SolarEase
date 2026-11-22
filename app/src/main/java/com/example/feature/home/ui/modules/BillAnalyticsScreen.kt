package com.example.feature.home.ui.modules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.session.UserSession
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/**
 * Dedicated Bill Analytics Route.
 * Displays comprehensive consumption and charges analytics derived from verified TNEB billing records.
 */
@Composable
fun BillAnalyticsRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSolarLens: () -> Unit = {}
) {
    val billData by TnebBillCache.billData.collectAsState()
    val user = UserSession.currentUser
    val serviceNo = billData?.consumerNumber?.ifBlank { user?.ebServiceNumber ?: "" } ?: (user?.ebServiceNumber ?: "")
    val consumerData = billData ?: TnebConsumerBillData(consumerNumber = serviceNo)

    BillAnalyticsView(
        serviceNo = consumerData.consumerNumber.ifBlank { serviceNo },
        consumerData = consumerData,
        bills = consumerData.bills,
        onNavigateBack = onNavigateBack,
        onNavigateToSolarLens = onNavigateToSolarLens
    )
}

/**
 * Full Bill Analytics View.
 * Contains Overview Metrics, Consumption Analytics, Charges Breakdown,
 * Collection Stats, and complete 22-column TNEB Billing Records Inspector.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillAnalyticsView(
    serviceNo: String,
    consumerData: TnebConsumerBillData,
    bills: List<TnebBillRecord>,
    onNavigateBack: () -> Unit,
    onNavigateToSolarLens: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    // ── Metric Calculations ────────────────────────────────────────────────────────
    val validUnits = remember(bills) {
        bills.mapNotNull { it.units.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }
    }
    val totalBilledUnits = remember(validUnits) { validUnits.sum() }
    val avgCycleUnits = remember(validUnits) {
        if (validUnits.isNotEmpty()) validUnits.average() else 0.0
    }

    val validDemands = remember(bills) {
        bills.mapNotNull { it.recordedDemand.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }
    }
    val avgRecordedDemand = remember(validDemands) {
        if (validDemands.isNotEmpty()) validDemands.average() else null
    }

    val validPowerFactors = remember(bills) {
        bills.mapNotNull { it.powerFactor.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }
    }
    val avgPowerFactor = remember(validPowerFactors) {
        if (validPowerFactors.isNotEmpty()) validPowerFactors.average() else null
    }

    val totalAssessedCharges = remember(bills) {
        bills.mapNotNull { it.totalCharges.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalAmountPaid = remember(bills) {
        bills.mapNotNull { it.amountPaid.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val outstandingAmount = remember(bills) {
        bills.filter { !it.isPaid }.mapNotNull { it.amountDue.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }

    val paidCount = remember(bills) { bills.count { it.isPaid } }
    val pendingCount = remember(bills) { bills.count { !it.isPaid } }
    val settlementRate = remember(bills) {
        if (bills.isNotEmpty()) (paidCount.toDouble() / bills.size) * 100.0 else 100.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Bill Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                        )
                        val cleanName = consumerData.consumerName
                            .replace(Regex("(?i)\\s*consumer\\s*(number|no)?\\s*:?\\s*[0-9]+.*"), "")
                            .trim()
                            .ifBlank { UserSession.currentUser?.name.orEmpty() }
                        val sub = if (cleanName.isNotBlank()) "$cleanName • $serviceNo" else serviceNo
                        if (sub.isNotBlank()) {
                            Text(
                                text = sub,
                                fontSize = 11.sp,
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.background else SolarBrandBackground
                )
            )
        },
        containerColor = if (isDark) MaterialTheme.colorScheme.background else SolarBrandBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. Overview Summary Grid ───────────────────────────────────────────
            AnalyticsOverviewSection(
                totalBilledUnits = totalBilledUnits,
                avgCycleUnits = avgCycleUnits,
                avgRecordedDemand = avgRecordedDemand,
                avgPowerFactor = avgPowerFactor,
                totalAssessedCharges = totalAssessedCharges,
                totalAmountPaid = totalAmountPaid,
                outstandingAmount = outstandingAmount,
                settlementRate = settlementRate
            )

            // ── 2. Complete Billing Records — Table View ───────────────────────────
            BillingRecordsTableView(bills = bills)

            // ── 3. Charges & Cost Breakdown Analytics ──────────────────────────────
            ChargesAnalyticsSection(bills = bills)

            // ── 4. Complete 22-Column TNEB Billing Records Inspector ────────────────
            CompleteBillingRecordsSection(bills = bills)

            // ── Bottom Solar Lens CTA ──────────────────────────────────────────────
            // ── Bottom Appliance Audit CTA ──────────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
                border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToSolarLens)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.WbSunny,
                            Icons.Default.Sensors,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else SolarBrandGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Optimize With Solar Lens",
                            text = "Appliance Energy Audit",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                        )
                        Text(
                            text = "Model rooftop solar capacity to offset baseline billing slabs →",
                            text = "Audit connected heavy loads to see where your bi-monthly units go →",
                            fontSize = 11.sp,
                            color = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 1. ANALYTICS OVERVIEW SECTION
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun AnalyticsOverviewSection(
    totalBilledUnits: Double,
    avgCycleUnits: Double,
    avgRecordedDemand: Double?,
    avgPowerFactor: Double?,
    totalAssessedCharges: Double,
    totalAmountPaid: Double,
    outstandingAmount: Double,
    settlementRate: Double
) {
    val isDark = LocalIsDarkTheme.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analytics Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight
                ) {
                    Text(
                        text = "Verified TNEB Data",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else SolarBrandGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Metric Row 1: Consumption & Demand
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricKpiCard(
                    title = "Total Consumption",
                    value = "${formatNumber(totalBilledUnits)} kWh",
                    subtitle = "All recorded cycles",
                    icon = Icons.Outlined.Speed,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "Avg per Cycle",
                    value = "${formatNumber(avgCycleUnits)} kWh",
                    subtitle = "Bi-monthly baseline",
                    icon = Icons.Outlined.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }

            // Metric Row 2: Recorded Demand & Power Factor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val demandStr = if (avgRecordedDemand != null) {
                    if (avgRecordedDemand % 1.0 == 0.0) "${avgRecordedDemand.toInt()} KW"
                    else String.format(Locale.US, "%.1f KW", avgRecordedDemand)
                } else "—"

                MetricKpiCard(
                    title = "Avg Recorded Demand",
                    value = demandStr,
                    subtitle = if (avgRecordedDemand != null) "Active peak load" else "Not reported in bills",
                    icon = Icons.Outlined.Bolt,
                    modifier = Modifier.weight(1f)
                )

                val pfStr = if (avgPowerFactor != null && avgPowerFactor > 0.0) {
                    String.format(Locale.US, "%.2f", avgPowerFactor)
                } else "—"

                MetricKpiCard(
                    title = "Avg Power Factor",
                    value = pfStr,
                    subtitle = if (avgPowerFactor != null && avgPowerFactor >= 0.90) "Optimal efficiency" else "Grid telemetry",
                    icon = Icons.Outlined.EnergySavingsLeaf,
                    modifier = Modifier.weight(1f)
                )
            }

            // Metric Row 3: Financial Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricKpiCard(
                    title = "Total Assessed",
                    value = formatCurrency(totalAssessedCharges),
                    subtitle = "Billed charges",
                    icon = Icons.Outlined.ReceiptLong,
                    modifier = Modifier.weight(1f)
                )
                MetricKpiCard(
                    title = "Total Amount Paid",
                    value = formatCurrency(totalAmountPaid),
                    subtitle = "${settlementRate.toInt()}% on-time rate",
                    icon = Icons.Outlined.CheckCircle,
                    valueColor = if (isDark) Color(0xFF6EE7B7) else SolarBrandGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            // Outstanding Banner if any
            if (outstandingAmount > 0.0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) Color(0xFF3E1F1A) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF7F1D1D) else Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Outstanding Electricity Dues",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            )
                        }
                        Text(
                            text = formatCurrency(outstandingAmount),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else SolarBrandGreenSurface,
        border = BorderStroke(0.8.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGreenLight),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreenSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: (if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 2. COMPLETE BILLING RECORDS — TABLE VIEW
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun BillingRecordsTableView(bills: List<TnebBillRecord>) {
    val isDark = LocalIsDarkTheme.current
    var selectedBill by remember { mutableStateOf<TnebBillRecord?>(null) }

    // Pagination (10 records per page)
    val pageSize = 10
    var currentPage by remember { mutableIntStateOf(0) }
    val pageCount = maxOf(1, (bills.size + pageSize - 1) / pageSize)
    val pagedBills = remember(bills, currentPage) {
        bills.drop(currentPage * pageSize).take(pageSize)
    }

    // Colors matching TNEB official statement & SolarEase green theme
    val cardBg = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg
    val cardBorder = if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder
    val tableBorderColor = if (isDark) MaterialTheme.colorScheme.outlineVariant else Color(0xFFCBD5E1)
    val bannerBg = if (isDark) Color(0xFF2E2415) else Color(0xFFFEF3C7)
    val bannerText = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
    val groupHeaderBg = if (isDark) Color(0xFF1E3A2B) else Color(0xFFDCFCE7)
    val groupHeaderText = if (isDark) Color(0xFF86EFAC) else Color(0xFF14532D)
    val colHeaderBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFF8FAFC)
    val colHeaderText = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF334155)
    val numberRowBg = if (isDark) Color(0xFF1F2937) else Color(0xFFF1F5F9)
    val numberRowText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF64748B)

    // Column widths in dp
    val dateColWidth = 104.dp
    val entryDateColWidth = 96.dp
    val statusColWidth = 76.dp
    val kwhColWidth = 76.dp
    val kvahColWidth = 68.dp
    val demandColWidth = 80.dp
    val pfColWidth = 76.dp
    val unitsColWidth = 76.dp
    val ccChargesColWidth = 84.dp
    val electyTaxColWidth = 76.dp
    val weldingColWidth = 68.dp
    val excessDemandColWidth = 80.dp
    val pfPenaltyColWidth = 76.dp
    val fixedChargesColWidth = 80.dp
    val totalChargesColWidth = 86.dp
    val advancePaidColWidth = 80.dp
    val adjustmentColWidth = 76.dp
    val amountDueColWidth = 86.dp
    val dueDateColWidth = 96.dp
    val amountPaidColWidth = 86.dp
    val receiptColWidth = 144.dp
    val paymentDateColWidth = 96.dp

    // Group header widths (sum of child columns)
    val assessmentScrollWidth = entryDateColWidth + statusColWidth
    val consumptionReadingWidth = kwhColWidth + kvahColWidth + demandColWidth + pfColWidth
    val consumptionWidth = unitsColWidth
    val splitupChargesWidth = ccChargesColWidth + electyTaxColWidth + weldingColWidth + excessDemandColWidth + pfPenaltyColWidth + fixedChargesColWidth
    val totalChargesWidth = totalChargesColWidth
    val deductionsWidth = advancePaidColWidth + adjustmentColWidth
    val finalBillWidth = amountDueColWidth + dueDateColWidth
    val collectionDetailsWidth = amountPaidColWidth + receiptColWidth + paymentDateColWidth
    val totalScrollableWidth = assessmentScrollWidth + consumptionReadingWidth + consumptionWidth + splitupChargesWidth + totalChargesWidth + deductionsWidth + finalBillWidth + collectionDetailsWidth

    // Fixed heights for row alignment
    val bannerHeight = 30.dp
    val groupHeaderHeight = 32.dp
    val colHeaderHeight = 40.dp
    val numberRowHeight = 22.dp
    val rowHeight = 40.dp

    val horizontalScrollState = rememberScrollState()

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Complete Billing Records — Table View",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                    )
                    Text(
                        text = "Official 22-column TNEB statement with horizontal swipe & pinned date",
                        fontSize = 11.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SolarBrandGreen.copy(alpha = 0.12f),
                    border = BorderStroke(0.8.dp, SolarBrandGreen.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "${bills.size} Cycles",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SolarBrandGreen,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Swipe prompt badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TableChart,
                    contentDescription = null,
                    tint = SolarBrandGreen,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Swipe horizontally to inspect all 22 columns • Tap any row for cycle summary",
                    fontSize = 10.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF475569)
                )
            }

            // Table Container
            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, tableBorderColor),
                color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (bills.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No billing records available to display.",
                            fontSize = 12.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                        )
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // ── Pinned Column: Date (Assessment Date, Col 1) ─────────────────
                        Column(
                            modifier = Modifier
                                .width(dateColWidth)
                                .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White)
                        ) {
                            // Top banner segment for pinned col
                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .height(bannerHeight)
                                    .background(bannerBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Statement",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerText
                                )
                            }
                            // Group Header: Assessment
                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .height(groupHeaderHeight)
                                    .background(groupHeaderBg)
                                    .border(BorderStroke(0.5.dp, tableBorderColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Assessment",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = groupHeaderText
                                )
                            }
                            // Column Header: Date
                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .height(colHeaderHeight)
                                    .background(colHeaderBg)
                                    .border(BorderStroke(0.5.dp, tableBorderColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colHeaderText
                                )
                            }
                            // Column Number: 1
                            Box(
                                modifier = Modifier
                                    .width(dateColWidth)
                                    .height(numberRowHeight)
                                    .background(numberRowBg)
                                    .border(BorderStroke(0.5.dp, tableBorderColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = numberRowText
                                )
                            }
                            // Rows: Assessment Date
                            pagedBills.forEachIndexed { index, bill ->
                                val isSelected = selectedBill?.id == bill.id
                                val rowBg = when {
                                    isSelected -> SolarBrandGreen.copy(alpha = 0.15f)
                                    index % 2 == 1 -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF8FAFC)
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .width(dateColWidth)
                                        .height(rowHeight)
                                        .background(rowBg)
                                        .border(BorderStroke(0.5.dp, tableBorderColor))
                                        .clickable {
                                            selectedBill = if (selectedBill?.id == bill.id) null else bill
                                        }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = bill.assessmentDate.ifBlank { bill.period.ifBlank { "—" } },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) SolarBrandGreen else (if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Subtle boundary divider between pinned column and scrollable section
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(bannerHeight + groupHeaderHeight + colHeaderHeight + numberRowHeight + (rowHeight * pagedBills.size))
                                .background(SolarBrandGreen.copy(alpha = 0.35f))
                        )

                        // ── Scrollable Columns: Cols 2 to 22 ────────────────────────────
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScrollState)
                        ) {
                            Column(modifier = Modifier.width(totalScrollableWidth)) {
                                // Top Orange/Amber Banner
                                Box(
                                    modifier = Modifier
                                        .width(totalScrollableWidth)
                                        .height(bannerHeight)
                                        .background(bannerBg)
                                        .padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "Consumption Charges and Collection Details",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = bannerText
                                    )
                                }

                                // Tier 1: Group Headers
                                Row(
                                    modifier = Modifier
                                        .width(totalScrollableWidth)
                                        .height(groupHeaderHeight)
                                ) {
                                    GroupHeaderCell("Assessment (Cont.)", assessmentScrollWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                    GroupHeaderCell("Consumption Reading", consumptionReadingWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                    GroupHeaderCell("Consumption", consumptionWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                    GroupHeaderCell("Splitup of Charges", splitupChargesWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                    GroupHeaderCell("Total Charges", totalChargesWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                    GroupHeaderCell("Deductions", deductionsWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                    GroupHeaderCell("Final Bill Details", finalBillWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                    GroupHeaderCell("Collection Details", collectionDetailsWidth, groupHeaderBg, groupHeaderText, tableBorderColor)
                                }

                                // Tier 2: Column Headers (Cols 2..22)
                                Row(
                                    modifier = Modifier
                                        .width(totalScrollableWidth)
                                        .height(colHeaderHeight)
                                ) {
                                    ColumnHeaderCell("Entry Date", entryDateColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Status", statusColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("KWH", kwhColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("KVAH", kvahColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Recorded\nDemand", demandColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Power\nFactor", pfColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("In Units", unitsColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("CC\nCharges", ccChargesColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Electy\nTax", electyTaxColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Welding", weldingColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Excess\nDemand", excessDemandColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("PF\nPenalty", pfPenaltyColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Fixed\nCharges", fixedChargesColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("15=9+..+14", totalChargesColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Advance\nAmount Paid", advancePaidColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Adjustment", adjustmentColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Amount To\nBe paid", amountDueColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Due Date", dueDateColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Amount\nPaid", amountPaidColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Receipt\nNo", receiptColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                    ColumnHeaderCell("Date", paymentDateColWidth, colHeaderBg, colHeaderText, tableBorderColor)
                                }

                                // Tier 3: Column Numbers Row (2..22)
                                Row(
                                    modifier = Modifier
                                        .width(totalScrollableWidth)
                                        .height(numberRowHeight)
                                ) {
                                    val widths = listOf(
                                        entryDateColWidth, statusColWidth, kwhColWidth, kvahColWidth, demandColWidth, pfColWidth,
                                        unitsColWidth, ccChargesColWidth, electyTaxColWidth, weldingColWidth, excessDemandColWidth,
                                        pfPenaltyColWidth, fixedChargesColWidth, totalChargesColWidth, advancePaidColWidth,
                                        adjustmentColWidth, amountDueColWidth, dueDateColWidth, amountPaidColWidth,
                                        receiptColWidth, paymentDateColWidth
                                    )
                                    (2..22).forEachIndexed { i, num ->
                                        Box(
                                            modifier = Modifier
                                                .width(widths[i])
                                                .height(numberRowHeight)
                                                .background(numberRowBg)
                                                .border(BorderStroke(0.5.dp, tableBorderColor)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = num.toString(),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = numberRowText
                                            )
                                        }
                                    }
                                }

                                // Data Rows
                                pagedBills.forEachIndexed { index, bill ->
                                    val isSelected = selectedBill?.id == bill.id
                                    val rowBg = when {
                                        isSelected -> SolarBrandGreen.copy(alpha = 0.15f)
                                        index % 2 == 1 -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF8FAFC)
                                        else -> Color.Transparent
                                    }
                                    Row(
                                        modifier = Modifier
                                            .width(totalScrollableWidth)
                                            .height(rowHeight)
                                            .background(rowBg)
                                            .clickable {
                                                selectedBill = if (selectedBill?.id == bill.id) null else bill
                                            }
                                    ) {
                                        TableCell(bill.entryDate.ifBlank { "—" }, entryDateColWidth, tableBorderColor, isDark)
                                        TableCell(bill.status.ifBlank { "NORMAL" }, statusColWidth, tableBorderColor, isDark)
                                        TableCell(bill.kwh.ifBlank { "—" }, kwhColWidth, tableBorderColor, isDark)
                                        TableCell(bill.kvah.ifBlank { "—" }, kvahColWidth, tableBorderColor, isDark)
                                        TableCell(bill.recordedDemand.ifBlank { "—" }, demandColWidth, tableBorderColor, isDark)
                                        TableCell(bill.powerFactor.ifBlank { "—" }, pfColWidth, tableBorderColor, isDark)
                                        TableCell(bill.units.ifBlank { "—" }, unitsColWidth, tableBorderColor, isDark, fontWeight = FontWeight.SemiBold)
                                        TableCell(bill.ccCharges.ifBlank { "—" }, ccChargesColWidth, tableBorderColor, isDark, textColor = Color(0xFFB91C1C), fontWeight = FontWeight.SemiBold)
                                        TableCell(bill.electricityTax.ifBlank { "0" }, electyTaxColWidth, tableBorderColor, isDark)
                                        TableCell(bill.weldingCharges.ifBlank { "0" }, weldingColWidth, tableBorderColor, isDark)
                                        TableCell(bill.excessDemand.ifBlank { "0" }, excessDemandColWidth, tableBorderColor, isDark)
                                        TableCell(bill.pfPenalty.ifBlank { "0" }, pfPenaltyColWidth, tableBorderColor, isDark)
                                        TableCell(bill.fixedCharges.ifBlank { "0" }, fixedChargesColWidth, tableBorderColor, isDark)
                                        TableCell(bill.totalCharges.ifBlank { bill.amountDue.ifBlank { "—" } }, totalChargesColWidth, tableBorderColor, isDark, fontWeight = FontWeight.Bold)
                                        TableCell(bill.advancePaid.ifBlank { "0" }, advancePaidColWidth, tableBorderColor, isDark)
                                        TableCell(bill.adjustment.ifBlank { "0" }, adjustmentColWidth, tableBorderColor, isDark)
                                        TableCell(bill.amountDue.ifBlank { "—" }, amountDueColWidth, tableBorderColor, isDark, fontWeight = FontWeight.Bold)
                                        TableCell(bill.dueDate.ifBlank { "—" }, dueDateColWidth, tableBorderColor, isDark)
                                        TableCell(bill.amountPaid.ifBlank { "—" }, amountPaidColWidth, tableBorderColor, isDark, fontWeight = FontWeight.Bold, textColor = SolarBrandGreen)
                                        TableCell(bill.receiptNumber.ifBlank { "—" }, receiptColWidth, tableBorderColor, isDark)
                                        TableCell(bill.paymentDate.ifBlank { "—" }, paymentDateColWidth, tableBorderColor, isDark)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pagination Controls (if > 10 records)
            if (pageCount > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${currentPage + 1} of $pageCount • Showing ${pagedBills.size} of ${bills.size} records",
                        fontSize = 11.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { if (currentPage > 0) currentPage-- },
                            enabled = currentPage > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Page",
                                tint = if (currentPage > 0) SolarBrandGreen else (if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray)
                            )
                        }
                        IconButton(
                            onClick = { if (currentPage < pageCount - 1) currentPage++ },
                            enabled = currentPage < pageCount - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Page",
                                tint = if (currentPage < pageCount - 1) SolarBrandGreen else (if (isDark) MaterialTheme.colorScheme.outlineVariant else Color.LightGray)
                            )
                        }
                    }
                }
            }

            // Selected Row Detail Inspector Card
            selectedBill?.let { bill ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenSurface,
                    border = BorderStroke(1.dp, SolarBrandGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SolarBrandGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Cycle Details: ${bill.assessmentDate.ifBlank { bill.period }}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                                )
                            }
                            IconButton(
                                onClick = { selectedBill = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("In Units", fontSize = 10.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary)
                                Text("${bill.units} kWh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary)
                            }
                            Column {
                                Text("CC Charges", fontSize = 10.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary)
                                Text(formatCurrency(bill.ccCharges), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                            }
                            Column {
                                Text("Total Paid", fontSize = 10.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary)
                                Text(formatCurrency(bill.amountPaid), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SolarBrandGreen)
                            }
                            Column {
                                Text("Receipt No", fontSize = 10.sp, color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary)
                                Text(bill.receiptNumber.ifBlank { "—" }, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    bgColor: Color,
    textColor: Color,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(bgColor)
            .border(BorderStroke(0.5.dp, borderColor)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColumnHeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    bgColor: Color,
    textColor: Color,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .background(bgColor)
            .border(BorderStroke(0.5.dp, borderColor))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    borderColor: Color,
    isDark: Boolean,
    textColor: Color? = null,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .border(BorderStroke(0.5.dp, borderColor))
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = fontWeight,
            color = textColor ?: (if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 3. CHARGES & COST BREAKDOWN ANALYTICS
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChargesAnalyticsSection(bills: List<TnebBillRecord>) {
    val isDark = LocalIsDarkTheme.current

    // Aggregate cost components across all cycles
    val totalCcCharges = remember(bills) {
        bills.mapNotNull { it.ccCharges.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalElectricityTax = remember(bills) {
        bills.mapNotNull { it.electricityTax.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalFixedCharges = remember(bills) {
        bills.mapNotNull { it.fixedCharges.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalExcessDemand = remember(bills) {
        bills.mapNotNull { it.excessDemand.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalWeldingCharges = remember(bills) {
        bills.mapNotNull { it.weldingCharges.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalPfPenalty = remember(bills) {
        bills.mapNotNull { it.pfPenalty.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalAdvancePaid = remember(bills) {
        bills.mapNotNull { it.advancePaid.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }
    val totalAdjustment = remember(bills) {
        bills.mapNotNull { it.adjustment.replace(Regex("[^0-9.]"), "").trim().toDoubleOrNull() }.sum()
    }

    val sumCharges = (totalCcCharges + totalElectricityTax + totalFixedCharges + totalExcessDemand + totalWeldingCharges + totalPfPenalty).coerceAtLeast(1.0)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Billing & Charges Analytics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                )
                Text(
                    text = "Cumulative split-up of tariff charges, government levies, and deductions",
                    fontSize = 11.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                )
            }

            // Proportional Segmented Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenLight)
            ) {
                if (totalCcCharges > 0) {
                    Box(modifier = Modifier.weight((totalCcCharges / sumCharges).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(SolarBrandGreen))
                }
                if (totalElectricityTax > 0) {
                    Box(modifier = Modifier.weight((totalElectricityTax / sumCharges).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFF5BA77E)))
                }
                if (totalFixedCharges > 0) {
                    Box(modifier = Modifier.weight((totalFixedCharges / sumCharges).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFFD4AF37)))
                }
                if (totalExcessDemand > 0) {
                    Box(modifier = Modifier.weight((totalExcessDemand / sumCharges).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(Color(0xFFEF4444)))
                }
            }

            // Component Breakdown Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChargeBreakdownRow(
                    label = "CC Charges (Energy Consumption)",
                    amount = totalCcCharges,
                    total = sumCharges,
                    color = SolarBrandGreen
                )
                ChargeBreakdownRow(
                    label = "Electricity Tax (Govt. Levy)",
                    amount = totalElectricityTax,
                    total = sumCharges,
                    color = Color(0xFF5BA77E)
                )
                ChargeBreakdownRow(
                    label = "Fixed Charges (Sanctioned Load)",
                    amount = totalFixedCharges,
                    total = sumCharges,
                    color = Color(0xFFD4AF37)
                )
                if (totalExcessDemand > 0) {
                    ChargeBreakdownRow(
                        label = "Excess Demand Surcharge",
                        amount = totalExcessDemand,
                        total = sumCharges,
                        color = Color(0xFFEF4444)
                    )
                }
                if (totalWeldingCharges > 0) {
                    ChargeBreakdownRow(
                        label = "Welding Charges",
                        amount = totalWeldingCharges,
                        total = sumCharges,
                        color = Color(0xFF8B5CF6)
                    )
                }
                if (totalPfPenalty > 0) {
                    ChargeBreakdownRow(
                        label = "Power Factor Penalty",
                        amount = totalPfPenalty,
                        total = sumCharges,
                        color = Color(0xFFEC4899)
                    )
                }
            }

            HorizontalDivider(
                color = if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder,
                thickness = 0.8.dp
            )

            // Deductions & Adjustments Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Advance Paid",
                        fontSize = 11.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                    )
                    Text(
                        text = formatCurrency(totalAdvancePaid),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Adjustments / Credits",
                        fontSize = 11.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                    )
                    Text(
                        text = formatCurrency(totalAdjustment),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ChargeBreakdownRow(
    label: String,
    amount: Double,
    total: Double,
    color: Color
) {
    val isDark = LocalIsDarkTheme.current
    val pct = if (total > 0) ((amount / total) * 100.0).toInt() else 0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$pct%",
                fontSize = 11.sp,
                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
            )
            Text(
                text = formatCurrency(amount),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// 4. COMPLETE 22-COLUMN TNEB BILLING RECORDS INSPECTOR
// ──────────────────────────────────────────────────────────────────────────────
@Composable
private fun CompleteBillingRecordsSection(bills: List<TnebBillRecord>) {
    val isDark = LocalIsDarkTheme.current
    var expandedBillId by remember { mutableStateOf<String?>(bills.firstOrNull()?.id) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "Complete Billing Records",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                )
                Text(
                    text = "Detailed 22-column TNEB Consumption, Charges and Collection records",
                    fontSize = 11.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                )
            }

            if (bills.isEmpty()) {
                Text(
                    text = "No detailed records available.",
                    fontSize = 12.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bills.forEach { bill ->
                        val isExpanded = expandedBillId == bill.id

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else SolarBrandGreenSurface,
                            border = BorderStroke(1.dp, if (isExpanded) SolarBrandGreen else (if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGreenLight)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedBillId = if (isExpanded) null else bill.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header row of bill card with SETTLED / PENDING badge on the right
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bill.period.ifBlank { bill.assessmentDate },
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                                        )
                                        Text(
                                            text = "Assessment: ${bill.assessmentDate.ifBlank { "—" }} • ${bill.units.ifBlank { "0" }} kWh",
                                            fontSize = 11.sp,
                                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            val rawAmt = bill.amountDue.trim()
                                            Text(
                                                text = formatCurrency(rawAmt),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (bill.isPaid) (if (isDark) Color(0xFF064E3B) else SolarBrandGreenLight) else (if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7))
                                            ) {
                                                Text(
                                                    text = if (bill.isPaid) "SETTLED" else "PENDING",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (bill.isPaid) (if (isDark) Color(0xFF86EFAC) else SolarBrandGreen) else (if (isDark) Color(0xFFFDBA74) else Color(0xFFB45309)),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                                            tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(top = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        HorizontalDivider(
                                            color = if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGreenLight,
                                            thickness = 0.8.dp
                                        )

                                        // Group A: Assessment Details
                                        RecordGroupView(
                                            title = "A. Assessment Details",
                                            items = listOf(
                                                "Assessment Date" to bill.assessmentDate.ifBlank { "—" },
                                                "Entry Date" to bill.entryDate.ifBlank { "—" },
                                                "Assessment Status" to bill.status.ifBlank { "NORMAL" }
                                            )
                                        )

                                        // Group B: Consumption Reading
                                        RecordGroupView(
                                            title = "B. Consumption Reading",
                                            items = listOf(
                                                "KWH Reading" to bill.kwh.ifBlank { "—" },
                                                "KVAH Reading" to bill.kvah.ifBlank { "—" },
                                                "Recorded Demand" to (if (bill.recordedDemand.isNotBlank()) "${bill.recordedDemand} KW" else "—"),
                                                "Power Factor" to bill.powerFactor.ifBlank { "—" }
                                            )
                                        )

                                        // Group C: Consumption
                                        RecordGroupView(
                                            title = "C. Billed Consumption",
                                            items = listOf(
                                                "Consumption in Units" to "${bill.units.ifBlank { "0" }} kWh"
                                            )
                                        )

                                        // Group D: Split-up of Charges
                                        RecordGroupView(
                                            title = "D. Split-up of Charges",
                                            items = listOf(
                                                "CC Charges" to formatCurrency(bill.ccCharges),
                                                "Electricity Tax" to formatCurrency(bill.electricityTax),
                                                "Fixed Charges" to formatCurrency(bill.fixedCharges),
                                                "Welding Charges" to formatCurrency(bill.weldingCharges),
                                                "Excess Demand" to formatCurrency(bill.excessDemand),
                                                "PF Penalty" to formatCurrency(bill.pfPenalty)
                                            )
                                        )

                                        // Group E: Total Charges
                                        RecordGroupView(
                                            title = "E. Total Charges",
                                            items = listOf(
                                                "Total Assessed Charges" to formatCurrency(bill.totalCharges)
                                            )
                                        )

                                        // Group F: Deductions
                                        RecordGroupView(
                                            title = "F. Deductions & Adjustments",
                                            items = listOf(
                                                "Advance Amount Paid" to formatCurrency(bill.advancePaid),
                                                "Adjustment" to formatCurrency(bill.adjustment)
                                            )
                                        )

                                        // Group G: Final Bill Details
                                        RecordGroupView(
                                            title = "G. Final Bill Details",
                                            items = listOf(
                                                "Amount To Be Paid" to formatCurrency(bill.amountDue),
                                                "Due Date" to bill.dueDate.ifBlank { "—" }
                                            )
                                        )

                                        // Group H: Collection Details
                                        RecordGroupView(
                                            title = "H. Collection Details",
                                            items = listOf(
                                                "Amount Paid" to formatCurrency(bill.amountPaid),
                                                "Receipt Number" to bill.receiptNumber.ifBlank { "—" },
                                                "Payment Date" to bill.paymentDate.ifBlank { "—" },
                                                "Settlement Status" to if (bill.isPaid) "PAID" else "PENDING"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordGroupView(
    title: String,
    items: List<Pair<String, String>>
) {
    val isDark = LocalIsDarkTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
            border = BorderStroke(0.6.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
                        )
                        Text(
                            text = value,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// HELPER FORMATTERS
// ──────────────────────────────────────────────────────────────────────────────
private fun formatCurrency(raw: String): String {
    val clean = raw.replace(Regex("[^0-9.]"), "").trim()
    val num = clean.toDoubleOrNull() ?: return if (raw.isBlank()) "—" else raw
    return formatCurrency(num)
}

private fun formatCurrency(num: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(num).replace("INR", "₹").trim()
}

private fun formatNumber(num: Double): String {
    val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 1
    return format.format(num)
}

private fun formatChartPeriod(rawPeriod: String): String {
    if (rawPeriod.isBlank()) return "Cycle"
    // e.g. "Jun–Aug 2024" -> "Jun-Aug"
    val parts = rawPeriod.split(" ", "–", "-")
    return if (parts.size >= 2) "${parts[0]}-${parts[1]}" else rawPeriod.take(8)
}

