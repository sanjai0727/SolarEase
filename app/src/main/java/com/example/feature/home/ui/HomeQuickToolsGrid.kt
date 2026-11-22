package com.example.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraOutdoor
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.subtleTopRightGradient
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarEaseSemanticColors
import com.example.ui.theme.SolarEmerald
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SolarIvory
import com.example.ui.theme.SolarJade
import com.example.ui.theme.*

/**
 * Compact expandable Explore Tools section for connected users.
 * When collapsed, shows only the section heading and dropdown arrow.
 * When expanded, displays the 2x2 grid of utility tools. Default state is collapsed.
 */
@Composable
fun CollapsibleExploreToolsSection(
    onBillStatus: () -> Unit,
    onChatbot: () -> Unit,
    onSolarLens: () -> Unit,
    onCalculator: () -> Unit,
    hasConnectedBill: Boolean,
    tariffCode: String = "LT-1A",
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    var isExpanded by rememberSaveable { mutableStateOf(initialExpanded) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                stateDescription = if (isExpanded) "Expanded" else "Collapsed"
            },
        shape = RoundedCornerShape(20.dp),
        color = OceanGlassCardBg,
        border = BorderStroke(1.dp, OceanGlassCardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Explore Tools",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = OceanDeepNavy
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse Explore Tools" else "Expand Explore Tools",
                    tint = OceanTeal,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isDark = LocalIsDarkTheme.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickToolCard(
                            modifier = Modifier.weight(1f),
                            title = "Bill Status",
                            subtitle = if (hasConnectedBill) "TNEB $tariffCode" else "Check dues & units",
                            icon = Icons.Default.ReceiptLong,
                            containerBg = if (isDark) Color(0xFF13382D) else Color(0xFFD8F1E8),
                            iconTint = if (isDark) Color(0xFF5ED8B0) else SolarJade,
                            onClick = onBillStatus
                        )
                        QuickToolCard(
                            modifier = Modifier.weight(1f),
                            title = "Chatbot",
                            subtitle = "Ask Energy AI",
                            icon = Icons.Default.SmartToy,
                            containerBg = if (isDark) Color(0xFF10363A) else Color(0xFFDCF3F5),
                            iconTint = if (isDark) Color(0xFF7DE1E7) else Color(0xFF006B70),
                            onClick = onChatbot
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickToolCard(
                            modifier = Modifier.weight(1f),
                            title = "Solar Lens",
                            subtitle = "AI load & audit",
                            icon = Icons.Default.CameraOutdoor,
                            containerBg = if (isDark) Color(0xFF0D2F1D) else Color(0xFFD4EFE4),
                            iconTint = if (isDark) Color(0xFF72DEAA) else SolarEmerald,
                            onClick = onSolarLens
                        )
                        QuickToolCard(
                            modifier = Modifier.weight(1f),
                            title = "Calculator",
                            subtitle = "Solar sizing & savings",
                            subtitle = "Tariff & slab estimate",
                            icon = Icons.Default.Calculate,
                            containerBg = if (isDark) Color(0xFF382C07) else SolarIvory,
                            iconTint = if (isDark) Color(0xFFFFD54F) else SolarGold,
                            onClick = onCalculator
                        )
                    }
                }
            }
        }
    }
}

/**
 * 2x2 Quick access tools grid for primary application destinations.
 */
@Composable
fun QuickToolsSection(
    onBillStatus: () -> Unit,
    onChatbot: () -> Unit,
    onSolarLens: () -> Unit,
    onCalculator: () -> Unit,
    hasConnectedBill: Boolean,
    tariffCode: String = "LT-IA",
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore Tools",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "QUICK ACCESS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.8.sp
            )
        }

        val isDark = LocalIsDarkTheme.current

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Bill Status",
                subtitle = if (hasConnectedBill) "TNEB $tariffCode" else "Check dues & units",
                icon = Icons.Default.ReceiptLong,
                containerBg = if (isDark) Color(0xFF13382D) else Color(0xFFD8F1E8),
                iconTint = if (isDark) Color(0xFF5ED8B0) else SolarJade,
                onClick = onBillStatus
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Chatbot",
                subtitle = "Ask Energy AI",
                icon = Icons.Default.SmartToy,
                containerBg = if (isDark) Color(0xFF10363A) else Color(0xFFDCF3F5),
                iconTint = if (isDark) Color(0xFF7DE1E7) else Color(0xFF006B70),
                onClick = onChatbot
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Solar Lens",
                subtitle = "AI load & audit",
                icon = Icons.Default.CameraOutdoor,
                containerBg = if (isDark) Color(0xFF0D2F1D) else Color(0xFFD4EFE4),
                iconTint = if (isDark) Color(0xFF72DEAA) else SolarEmerald,
                onClick = onSolarLens
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Calculator",
                subtitle = "Solar sizing & savings",
                subtitle = "Tariff & slab estimate",
                icon = Icons.Default.Calculate,
                containerBg = if (isDark) Color(0xFF382C07) else SolarIvory,
                iconTint = if (isDark) Color(0xFFFFD54F) else SolarGold,
                onClick = onCalculator
            )
        }
    }
}

/**
 * Interactive card component for quick utility launcher.
 */
@Composable
fun QuickToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerBg: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified
) {
    val actualContainerBg = if (containerBg != Color.Unspecified) containerBg else MaterialTheme.colorScheme.surfaceVariant
    val actualIconTint = if (iconTint != Color.Unspecified) iconTint else MaterialTheme.colorScheme.primary
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "toolScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        interactionSource = interactionSource,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .subtleTopRightGradient(
                    shape = RoundedCornerShape(16.dp),
                    radiusMultiplier = 0.75f
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(actualContainerBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = actualIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Activity log showing recent meter readings, audit events, or bill updates.
 */
@Composable
fun RecentActivitySection(
    activities: List<HomeActivityEvent>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "LOG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
            }

            if (activities.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No energy assessments yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Your bill updates, home checks, and savings progress will appear here.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activities.forEachIndexed { index, event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (event.isPositive) SolarEaseSemanticColors.SuccessText else SolarEaseSemanticColors.WarningText)
                                )
                                Column {
                                    Text(
                                        text = event.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${event.subtitle} • ${event.timeLabel}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!event.metricLabel.isNullOrBlank()) {
                                Text(
                                    text = event.metricLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (event.isPositive) SolarEaseSemanticColors.SuccessText else SolarEaseSemanticColors.WarningText
                                )
                            }
                        }

                        if (index < activities.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
