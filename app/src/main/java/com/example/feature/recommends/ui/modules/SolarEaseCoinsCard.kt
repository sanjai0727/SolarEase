package com.example.feature.recommends.ui.modules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.feature.recommends.model.BillVerificationResult
import com.example.feature.recommends.model.BillVerificationStatus
import com.example.feature.recommends.model.CoinEventType
import com.example.feature.recommends.model.CoinTransaction
import com.example.feature.recommends.model.StreakState
import com.example.ui.theme.SolarEaseSemanticColors
import com.example.ui.theme.SolarEaseTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SolarEaseCoinsCard(
    coinBalance: Int,
    streakState: StreakState,
    recentTransactions: List<CoinTransaction>,
    verificationResult: BillVerificationResult?,
    onViewHistoryClick: () -> Unit
) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.65f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 14.dp else 0.dp)
        ) {
            // Header Row: Coin Balance (Left) & Streak Badge + Chevron (Right)
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
                // Left: Coins Icon & Balance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isExpanded) 40.dp else 36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) Color(0xFF3E2D00) else Color(0xFFFDFDD0))
                            .border(1.dp, if (isDark) Color(0xFF664D00) else Color(0xFFD4AF37), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFFFD54F) else Color(0xFFD4AF37),
                            modifier = Modifier.size(if (isExpanded) 22.dp else 18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "$coinBalance",
                            fontSize = if (isExpanded) 26.sp else 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onSurface,
                            lineHeight = if (isExpanded) 28.sp else 22.sp
                        )
                        Text(
                            text = if (coinBalance == 1) "SolarEase Coin" else "SolarEase Coins",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Right: Consecutive Streak Pill + Expand/Collapse Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDark) Color(0xFF3E1D00) else Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF7C2D12) else Color(0xFFFFEDD5))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = if (isDark) Color(0xFFFFB77C) else Color(0xFFEA580C),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (streakState.currentStreak > 0) "${streakState.currentStreak} Day Streak" else "0 Day Streak",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFFB77C) else Color(0xFFC2410C)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse SolarEase Coins card" else "Expand SolarEase Coins card",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    // Bill Verification Status Pill
                    BillVerificationStatusPill(verificationResult = verificationResult)

                    // 7-Step Segmented Streak Tracker
                    SevenDayStreakTracker(streakState = streakState)

                    // Divider
                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f), thickness = 0.8.dp)

                    // Recent Transactions Mini-Feed
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Activity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Text(
                                text = if (recentTransactions.isNotEmpty()) "Latest ${recentTransactions.size}" else "",
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariant
                            )
                        }

                        if (recentTransactions.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surfaceVariant)
                                    .border(1.dp, colors.outline.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "No coin transactions yet. Check in a mantra to earn your first coin!",
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant,
                                    lineHeight = 15.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                recentTransactions.forEach { tx ->
                                    MiniTransactionRow(tx = tx)
                                }
                            }
                        }
                    }

                    // Action Button: Earning Rules & History
                    OutlinedButton(
                        onClick = onViewHistoryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, colors.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
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
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Earning Rules & History",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StatusPillConfig(
    val bg: Color,
    val border: Color,
    val tint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String
)

@Composable
fun BillVerificationStatusPill(verificationResult: BillVerificationResult?) {
    val status = verificationResult?.status ?: BillVerificationStatus.WAITING_FOR_BILL
    val isDark = LocalIsDarkTheme.current

    val config = when (status) {
        BillVerificationStatus.WAITING_FOR_BILL -> StatusPillConfig(
            bg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
            border = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
            icon = Icons.Outlined.HourglassEmpty,
            text = "Waiting for next bill data"
        )
        BillVerificationStatus.PROCESSING -> StatusPillConfig(
            bg = if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF),
            border = if (isDark) Color(0xFF1E40AF) else Color(0xFFDBEAFE),
            tint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
            icon = Icons.Outlined.Sync,
            text = "Evaluating electricity consumption..."
        )
        BillVerificationStatus.VERIFIED_REDUCTION -> {
            val reduction = verificationResult?.reductionPercent?.roundToInt() ?: 0
            val bonus = verificationResult?.reductionBonusEarned ?: 0
            val process = verificationResult?.billProcessedRewardEarned ?: 0
            StatusPillConfig(
                bg = if (isDark) Color(0xFF064E3B) else Color(0xFFF0FDF4),
                border = if (isDark) Color(0xFF065F46) else Color(0xFFDCFCE7),
                tint = if (isDark) Color(0xFF86EFAC) else Color(0xFF16A34A),
                icon = Icons.Default.CheckCircle,
                text = "Verified: $reduction% reduction • +${bonus + process} coins"
            )
        }
        BillVerificationStatus.NO_QUALIFYING_REDUCTION -> {
            val reduction = verificationResult?.reductionPercent?.roundToInt() ?: 0
            val text = if (reduction > 0) {
                "Reduction ($reduction%) below 5% threshold • +${verificationResult?.billProcessedRewardEarned ?: 0} bill coins"
            } else {
                "No consumption reduction • +${verificationResult?.billProcessedRewardEarned ?: 0} bill coins"
            }
            StatusPillConfig(
                bg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                border = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                icon = Icons.Outlined.Info,
                text = text
            )
        }
        BillVerificationStatus.INSUFFICIENT_HISTORY -> StatusPillConfig(
            bg = if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB),
            border = if (isDark) Color(0xFF78350F) else Color(0xFFFEF3C7),
            tint = if (isDark) Color(0xFFFDBA74) else Color(0xFFD97706),
            icon = Icons.Outlined.History,
            text = "Not enough bill history to verify yet (+${verificationResult?.billProcessedRewardEarned ?: 0} coins)"
        )
        BillVerificationStatus.INVALID_BILL -> StatusPillConfig(
            bg = if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2),
            border = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
            tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
            icon = Icons.Outlined.Warning,
            text = "Bill record has invalid or missing consumption"
        )
        BillVerificationStatus.PROCESSING_ERROR -> StatusPillConfig(
            bg = if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2),
            border = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFEE2E2),
            tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
            icon = Icons.Outlined.ErrorOutline,
            text = "Consumption baseline calculation error"
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = config.bg,
        border = BorderStroke(1.dp, config.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = config.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = if (isDark) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SevenDayStreakTracker(streakState: StreakState) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Milestone Progress",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = "Day ${streakState.daysInCurrentCycle} of 7 • Cycle ${streakState.currentMilestoneCycle}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }

        // 7 Segmented Day Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (day in 1..7) {
                val isCompleted = day <= streakState.daysInCurrentCycle
                val isMilestoneDay = day == 7

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isMilestoneDay && isCompleted -> Color(0xFFF59E0B)
                                isMilestoneDay -> if (isDark) Color(0xFF3E2D00) else Color(0xFFFFFBEB)
                                isCompleted -> colors.primary
                                else -> colors.surfaceVariant
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isMilestoneDay && isCompleted -> Color(0xFFD97706)
                                isMilestoneDay -> if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A)
                                isCompleted -> colors.primary
                                else -> colors.outlineVariant
                            },
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMilestoneDay) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Milestone Reward",
                                tint = if (isCompleted) Color.White else Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "+5",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCompleted) Color.White else if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
                            )
                        }
                    } else if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed Day $day",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "D$day",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Milestone Subtext
        Text(
            text = if (streakState.daysInCurrentCycle == 7) {
                "Cycle ${streakState.currentMilestoneCycle} complete! +5 bonus coins awarded. Check in tomorrow to advance into Cycle ${streakState.currentMilestoneCycle + 1}!"
            } else {
                "${7 - streakState.daysInCurrentCycle} day(s) until your next 5-coin milestone bonus."
            },
            fontSize = 11.sp,
            color = colors.onSurfaceVariant,
            lineHeight = 15.sp
        )
    }
}

@Composable
fun MiniTransactionRow(tx: CoinTransaction) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when (tx.eventType) {
                                CoinEventType.STREAK_MILESTONE -> if (isDark) Color(0xFF3E2D00) else Color(0xFFFEF3C7)
                                CoinEventType.BILL_PROCESSED,
                                CoinEventType.VERIFIED_CONSUMPTION_REDUCTION -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)
                                else -> if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tx.eventType) {
                            CoinEventType.STREAK_MILESTONE -> Icons.Default.CardGiftcard
                            CoinEventType.BILL_PROCESSED,
                            CoinEventType.VERIFIED_CONSUMPTION_REDUCTION -> Icons.Outlined.TrendingDown
                            else -> Icons.Default.Check
                        },
                        contentDescription = null,
                        tint = when (tx.eventType) {
                            CoinEventType.STREAK_MILESTONE -> if (isDark) Color(0xFFFFD54F) else Color(0xFFD97706)
                            CoinEventType.BILL_PROCESSED,
                            CoinEventType.VERIFIED_CONSUMPTION_REDUCTION -> if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB)
                            else -> if (isDark) Color(0xFF86EFAC) else Color(0xFF16A34A)
                        },
                        modifier = Modifier.size(13.dp)
                    )
                }

                Column {
                    Text(
                        text = tx.description,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTransactionTimestamp(tx.timestamp),
                        fontSize = 10.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "+${tx.amount}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )
        }
    }
}

fun formatTransactionTimestamp(timestampMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
    }
}
