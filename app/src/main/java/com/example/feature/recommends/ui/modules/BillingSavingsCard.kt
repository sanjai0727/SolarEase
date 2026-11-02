package com.example.feature.recommends.ui.modules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.home.ui.modules.BillingComparisonState
import com.example.feature.home.ui.modules.BillingCycleComparison
import com.example.feature.home.ui.modules.BillingCycleItem
import com.example.feature.home.ui.modules.BillingCycleStatus
import com.example.feature.recommends.model.BillingCycleSavings
import com.example.ui.theme.SolarEaseTheme
import java.util.Locale

@Composable
fun BillingSavingsCard(
    savings: BillingCycleSavings,
    availableCycles: List<BillingCycleItem> = emptyList(),
    selectedCycleIndex: Int = 0,
    cycleComparison: BillingCycleComparison? = null,
    onPreviousCycle: () -> Unit = {},
    onNextCycle: () -> Unit = {}
) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current
    var isExpanded by remember { mutableStateOf(false) }

    val comparison = cycleComparison
    val selectedCycle = comparison?.selectedCycle
        ?: availableCycles.getOrNull(selectedCycleIndex)
    val previousCycle = comparison?.previousCycle
        ?: (if (selectedCycleIndex > 0) availableCycles.getOrNull(selectedCycleIndex - 1) else null)

    val isCurrent = selectedCycle?.status == BillingCycleStatus.CURRENT
    val canGoBack = selectedCycleIndex > 0
    val canGoForward = selectedCycleIndex < availableCycles.lastIndex

    // Fallbacks from savings if availableCycles is empty
    val prevUnits = previousCycle?.consumptionKwh ?: savings.previousUnits
    val prevPeriod = previousCycle?.periodLabel?.takeIf { it.isNotBlank() } ?: savings.previousPeriod
    val currUnits = selectedCycle?.consumptionKwh ?: savings.currentUnits
    val currPeriod = selectedCycle?.periodLabel?.takeIf { it.isNotBlank() } ?: savings.currentPeriod

    val state = comparison?.state ?: if (savings.isConsumptionIncreased) BillingComparisonState.COMPLETED_INCREASED else BillingComparisonState.COMPLETED_SAVED

    // Card background & borders based on state
    val isIncreased = when (state) {
        BillingComparisonState.CURRENT_HIGHER,
        BillingComparisonState.COMPLETED_INCREASED -> true
        else -> false
    }

    val isCompletedSaved = state == BillingComparisonState.COMPLETED_SAVED
    val isCurrentLower = state == BillingComparisonState.CURRENT_LOWER

    val cardBg = when {
        isIncreased -> if (isDark) Color(0xFF2C1E0A) else Color(0xFFFFFBEB)
        isCompletedSaved -> if (isDark) Color(0xFF0F2D1F) else Color(0xFFF0FDF4)
        isCurrentLower -> if (isDark) Color(0xFF0D2538) else Color(0xFFF0F9FF)
        else -> colors.surfaceContainerLow
    }

    val cardBorder = when {
        isIncreased -> if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
        isCompletedSaved -> if (isDark) Color(0xFF065F46) else Color(0xFFA7F3D0)
        isCurrentLower -> if (isDark) Color(0xFF0C4A6E) else Color(0xFFBAE6FD)
        else -> colors.outlineVariant
    }

    // Difference calculation: difference = currentCycleConsumption - previousCycleConsumption
    val hasValidComparison = previousCycle != null || savings.previousUnits > 0f
    val consumptionDiff = currUnits - prevUnits
    val isDecrease = hasValidComparison && (consumptionDiff < -0.01f || state == BillingComparisonState.COMPLETED_SAVED || state == BillingComparisonState.CURRENT_LOWER)
    val isIncreaseDiff = hasValidComparison && (consumptionDiff > 0.01f || state == BillingComparisonState.COMPLETED_INCREASED || state == BillingComparisonState.CURRENT_HIGHER)
    val absDiffKwh = kotlin.math.abs(consumptionDiff).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title & Expand/Collapse Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .semantics {
                        role = Role.Button
                        stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                    }
                    .clickable(role = Role.Button) { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = if (isIncreased) Color(0xFFD97706) else Color(0xFF059669),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Last Billing Cycle",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Last Billing Cycle card" else "Expand Last Billing Cycle card",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Collapsed Summary (Always visible):
            // Previous Cycle column | Middle Difference Pill | Current/Selected Cycle column
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Previous Cycle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Previous Cycle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "${prevUnits.toInt()} kWh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onSurface
                    )
                    Text(
                        text = prevPeriod,
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Middle Difference Pill (compact rounded pill with arrow and kWh difference)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isIncreaseDiff -> if (isDark) Color(0xFF3E2D00) else Color(0xFFFEF3C7)
                        isDecrease -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                        else -> colors.surfaceVariant
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isIncreaseDiff -> if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
                            isDecrease -> if (isDark) Color(0xFF065F46) else Color(0xFFA7F3D0)
                            else -> colors.outlineVariant
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isIncreaseDiff -> Icons.Default.ArrowUpward
                                isDecrease -> Icons.Default.ArrowDownward
                                else -> Icons.Default.Remove
                            },
                            contentDescription = null,
                            tint = when {
                                isIncreaseDiff -> if (isDark) Color(0xFFFFB77C) else Color(0xFFD97706)
                                isDecrease -> if (isDark) Color(0xFF86EFAC) else Color(0xFF059669)
                                else -> colors.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                isIncreaseDiff -> "$absDiffKwh kWh"
                                isDecrease -> "$absDiffKwh kWh"
                                hasValidComparison -> "0 kWh"
                                else -> "—"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isIncreaseDiff -> if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
                                isDecrease -> if (isDark) Color(0xFF86EFAC) else Color(0xFF047857)
                                else -> colors.onSurfaceVariant
                            }
                        )
                    }
                }

                // Column 2: Selected / Current Cycle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = if (isCurrent) "Current Cycle" else "Selected Cycle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "${currUnits.toInt()} kWh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currPeriod,
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrent) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = "Live",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFF86EFAC) else Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = colors.surfaceVariant
                            ) {
                                Text(
                                    text = "Done",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expanded Section: Cycle Selector & Full Comparison Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cycle Navigation (< [Period • Live/Done] >)
                    if (availableCycles.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Cycle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                IconButton(
                                    onClick = onPreviousCycle,
                                    enabled = canGoBack,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Previous cycle",
                                        tint = if (canGoBack) colors.primary else colors.outlineVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCurrent) (if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)) else colors.surfaceVariant,
                                    border = BorderStroke(0.5.dp, if (isCurrent) (if (isDark) Color(0xFF065F46) else Color(0xFF6EE7B7)) else colors.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isDark) Color(0xFF86EFAC) else Color(0xFF059669))
                                            )
                                        }
                                        Text(
                                            text = if (isCurrent) "$currPeriod • Live" else "$currPeriod • Done",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCurrent) (if (isDark) Color(0xFF86EFAC) else Color(0xFF065F46)) else colors.onSurface
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onNextCycle,
                                    enabled = canGoForward,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Next cycle",
                                        tint = if (canGoForward) colors.primary else colors.outlineVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Divider separating cycle columns from comparison section
                    HorizontalDivider(
                        color = cardBorder.copy(alpha = 0.7f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Dedicated Full-Width Comparison Section
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        border = BorderStroke(
                            1.dp,
                            when {
                                isIncreased -> if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
                                isCompletedSaved -> if (isDark) Color(0xFF065F46) else Color(0xFFD1FAE5)
                                isCurrentLower -> if (isDark) Color(0xFF075985) else Color(0xFFBAE6FD)
                                else -> colors.outlineVariant
                            }
                        ),
                        shadowElevation = 0.5.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Comparison Icon Box
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (state) {
                                            BillingComparisonState.CURRENT_HIGHER,
                                            BillingComparisonState.COMPLETED_INCREASED -> if (isDark) Color(0xFF3E2D00) else Color(0xFFFEF3C7)
                                            BillingComparisonState.COMPLETED_SAVED -> if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                            BillingComparisonState.CURRENT_LOWER,
                                            BillingComparisonState.CURRENT_NO_READINGS -> if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)
                                            BillingComparisonState.COMPLETED_NO_CHANGE -> colors.surfaceVariant
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (state) {
                                        BillingComparisonState.CURRENT_HIGHER,
                                        BillingComparisonState.COMPLETED_INCREASED -> Icons.Default.TrendingUp
                                        BillingComparisonState.COMPLETED_SAVED -> Icons.Default.ElectricBolt
                                        BillingComparisonState.CURRENT_LOWER -> Icons.Default.TrendingDown
                                        BillingComparisonState.CURRENT_NO_READINGS -> Icons.Default.Info
                                        BillingComparisonState.COMPLETED_NO_CHANGE -> Icons.Default.BarChart
                                    },
                                    contentDescription = null,
                                    tint = when (state) {
                                        BillingComparisonState.CURRENT_HIGHER,
                                        BillingComparisonState.COMPLETED_INCREASED -> if (isDark) Color(0xFFFFB77C) else Color(0xFFD97706)
                                        BillingComparisonState.COMPLETED_SAVED -> if (isDark) Color(0xFF86EFAC) else Color(0xFF059669)
                                        BillingComparisonState.CURRENT_LOWER,
                                        BillingComparisonState.CURRENT_NO_READINGS -> if (isDark) Color(0xFF7DD3FC) else Color(0xFF0284C7)
                                        BillingComparisonState.COMPLETED_NO_CHANGE -> colors.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Comparison Text Information
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                when (state) {
                                    BillingComparisonState.CURRENT_HIGHER -> {
                                        val diff = comparison?.differenceUnits?.toInt() ?: (currUnits - prevUnits).toInt()
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "↑ +$diff kWh",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDark) Color(0xFFFFB77C) else Color(0xFF9A3412)
                                            )
                                            Text(
                                                text = "• Consumption Increased",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFFFB77C) else Color(0xFF9A3412)
                                            )
                                        }
                                        Text(
                                            text = comparison?.secondaryMessage ?: "${currUnits.toInt()} kWh consumed so far, based on daily meter readings.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = comparison?.comparisonNote ?: "Already +$diff kWh higher than previous cycle",
                                            fontSize = 10.sp,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    BillingComparisonState.CURRENT_LOWER -> {
                                        val diff = comparison?.differenceUnits?.toInt() ?: (prevUnits - currUnits).toInt()
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "↓ $diff kWh",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                                            )
                                            Text(
                                                text = "• Below Previous Cycle",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                                            )
                                        }
                                        Text(
                                            text = comparison?.secondaryMessage ?: "${currUnits.toInt()} kWh so far, based on daily meter readings.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = comparison?.comparisonNote ?: "Current cycle still in progress",
                                            fontSize = 10.sp,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    BillingComparisonState.COMPLETED_SAVED -> {
                                        val saved = comparison?.savedUnits?.toInt() ?: savings.savedUnits.toInt()
                                        val credits = comparison?.savedCredits ?: savings.savedCredits
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "↓ $saved kWh",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                            )
                                            Text(
                                                text = "• Saved",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
                                            )
                                        }
                                        Text(
                                            text = if (credits > 0) "+$credits Energy Credits" else comparison?.secondaryMessage ?: "Historical Baseline",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (credits > 0) (if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)) else colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = comparison?.comparisonNote ?: "$saved kWh lower than $prevPeriod",
                                            fontSize = 10.sp,
                                            color = colors.onSurfaceVariant
                                        )

                                        // Environmental Equivalency Chips
                                        if (saved > 0) {
                                            val co2Kg = saved * 0.82f
                                            val trees = co2Kg / 20.0f
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                                ) {
                                                    Text(
                                                        text = "🌱 ${String.format(Locale.US, "%.1f", co2Kg)} kg CO₂ offset",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF166534),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (isDark) Color(0xFF14532D) else Color(0xFFF0FDF4)
                                                ) {
                                                    Text(
                                                        text = "🌳 ${String.format(Locale.US, "%.1f", trees)} trees equiv",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDark) Color(0xFFBBF7D0) else Color(0xFF15803D),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    BillingComparisonState.COMPLETED_INCREASED -> {
                                        val diff = comparison?.differenceUnits?.toInt() ?: savings.differenceUnits.toInt()
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "↑ +$diff kWh",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDark) Color(0xFFFFB77C) else Color(0xFF9A3412)
                                            )
                                            Text(
                                                text = "• Consumption Increased",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFFFB77C) else Color(0xFF9A3412)
                                            )
                                        }
                                        Text(
                                            text = comparison?.secondaryMessage ?: "No Energy Credits",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = comparison?.comparisonNote ?: "Consumption increased by $diff kWh compared with the previous cycle",
                                            fontSize = 10.sp,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    BillingComparisonState.COMPLETED_NO_CHANGE -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "→ 0 kWh",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colors.onSurfaceVariant
                                            )
                                            Text(
                                                text = "• No Change",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.onSurface
                                            )
                                        }
                                        Text(
                                            text = comparison?.secondaryMessage ?: "No Energy Credits",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = comparison?.comparisonNote ?: "0 kWh difference compared with the previous cycle",
                                            fontSize = 10.sp,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    BillingComparisonState.CURRENT_NO_READINGS -> {
                                        Text(
                                            text = comparison?.primaryMessage ?: "Record your meter reading to track this cycle",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                                        )
                                        Text(
                                            text = comparison?.secondaryMessage ?: "Opening reading: ${selectedCycle?.openingMeterReading?.toInt() ?: 0} kWh",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.onSurfaceVariant
                                        )
                                        Text(
                                            text = comparison?.comparisonNote ?: "Log daily readings on Home to track consumption in real-time",
                                            fontSize = 10.sp,
                                            color = colors.onSurfaceVariant
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
