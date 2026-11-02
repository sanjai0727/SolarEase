package com.example.feature.recommends.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.recommends.model.CoinTransaction
import com.example.feature.recommends.model.StreakState
import com.example.ui.theme.SolarEaseTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinsRulesAndHistoryBottomSheet(
    coinBalance: Int,
    streakState: StreakState,
    transactions: List<CoinTransaction>,
    onDismiss: () -> Unit
) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SolarEase Coins & Rules",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Balance Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = colors.primary
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Available Balance",
                            fontSize = 12.sp,
                            color = colors.onPrimary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "$coinBalance Coins",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.onPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${streakState.currentStreak} Day Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = colors.surface,
                contentColor = colors.primary,
                divider = { HorizontalDivider(color = colors.outlineVariant) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Activity History (${transactions.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Earning Rules",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 380.dp)
            ) {
                if (selectedTabIndex == 0) {
                    // Full Transaction History
                    if (transactions.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(colors.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No transactions recorded yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Complete daily mantras or verify electric bills to earn SolarEase Coins.",
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            transactions.forEach { tx ->
                                MiniTransactionRow(tx = tx)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    // Earning Rules Breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EarningRuleItem(
                            icon = Icons.Outlined.CheckCircle,
                            iconTint = if (isDark) Color(0xFF86EFAC) else Color(0xFF16A34A),
                            title = "Daily Energy-Saving Activity • 1 Coin",
                            description = "Check in any of the 9 Energy-Saving Mantras or record an eligible daily meter reading. Awarded once per calendar day (unchecking a mantra does not revoke an earned coin)."
                        )

                        EarningRuleItem(
                            icon = Icons.Outlined.LocalFireDepartment,
                            iconTint = if (isDark) Color(0xFFFFB77C) else Color(0xFFEA580C),
                            title = "7-Day Streak Milestone • 5 Bonus Coins",
                            description = "Complete each 7-day cycle of consecutive qualifying activity days. Missing a full calendar day resets the active streak when you next qualify."
                        )

                        EarningRuleItem(
                            icon = Icons.Default.Description,
                            iconTint = if (isDark) Color(0xFF93C5FD) else Color(0xFF2563EB),
                            title = "Newly Validated Bill Processing • 10 Coins",
                            description = "Awarded once for each newly received, successfully validated electricity bill from your utility service."
                        )

                        EarningRuleItem(
                            icon = Icons.Outlined.TrendingDown,
                            iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                            title = "Verified Consumption Reduction • Up to 50 Coins",
                            description = "Compares normalized daily consumption (kWh/day) against the average of up to 3 preceding valid bills:\n• 5% to <10% reduction: 15 bonus coins\n• 10% to <20% reduction: 30 bonus coins\n• 20% or greater reduction: 50 bonus coins\n• Below 5% reduction: No reduction bonus\nOnly the highest qualifying tier is awarded per bill."
                        )

                        EarningRuleItem(
                            icon = Icons.Outlined.Info,
                            iconTint = colors.onSurfaceVariant,
                            title = "Historical Baseline Policy",
                            description = "Pre-existing cached bills on first activation establish your historical baseline and do not earn retroactive rewards. If prior bill history is insufficient, shows 'Not enough bill history to verify yet.'"
                        )

                        // Disclaimer Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0xFF3E2D00) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Important Disclaimer",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color(0xFFFFD54F) else Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Redemption is not available yet. Coins currently track your qualifying activities and verified rewards. SolarEase does not promise cash conversion, payouts, or external redemption at this stage.",
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFFFDBC8) else Color(0xFF92400E),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun EarningRuleItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    val colors = SolarEaseTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceVariant,
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }
    }
}
