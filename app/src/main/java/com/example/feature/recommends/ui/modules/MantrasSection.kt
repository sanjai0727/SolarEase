package com.example.feature.recommends.ui.modules

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.recommends.model.MantraItem
import com.example.ui.theme.SolarEaseTheme

@Composable
fun TodayProgressCard(
    modifier: Modifier = Modifier,
    checkedCount: Int,
    totalCount: Int
) {
    val colors = SolarEaseTheme.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Today's Mantra Progress",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )
                }

                Text(
                    text = "$checkedCount of $totalCount completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )
            }

            val progress = (checkedCount.toFloat() / totalCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(colors.primary, Color(0xFF276A70))
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun MantrasSection(
    mantras: List<MantraItem>,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    onMantraClick: (Int) -> Unit
) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current
    val checkedCount = mantras.count { it.isCheckedIn }
    val totalCount = mantras.size

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header Row: Heading + Description (Left) & Progress Pill + Chevron (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(8.dp))
                .semantics {
                    role = Role.Button
                    stateDescription = if (isExpanded) "Expanded" else "Collapsed"
                }
                .clickable(role = Role.Button) { onToggleExpand() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "9 Energy-Saving Mantras",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    letterSpacing = 0.sp
                )
                Text(
                    text = "Check in regularly, build the habit and reduce your electricity consumption.",
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isExpanded) {
                    Surface(
                        modifier = Modifier.testTag("mantras_progress_pill"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (checkedCount == totalCount) {
                            if (isDark) Color(0xFF064E3B) else Color(0xFFF0FDF4)
                        } else {
                            colors.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (checkedCount == totalCount) {
                                if (isDark) Color(0xFF065F46) else Color(0xFFBBF7D0)
                            } else {
                                colors.outlineVariant
                            }
                        )
                    ) {
                        Text(
                            text = "$checkedCount of $totalCount completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (checkedCount == totalCount) {
                                if (isDark) Color(0xFF86EFAC) else Color(0xFF16A34A)
                            } else {
                                colors.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse 9 Energy-Saving Mantras" else "Expand 9 Energy-Saving Mantras",
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
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                mantras.forEach { item ->
                    MantraCard(
                        item = item,
                        onClick = { onMantraClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun MantraCard(
    item: MantraItem,
    onClick: () -> Unit
) {
    val colors = SolarEaseTheme.colors
    val isDark = LocalIsDarkTheme.current

    val mantraVectorIcon = when (item.id) {
        1 -> Icons.Outlined.PowerSettingsNew
        2 -> Icons.Outlined.Tv
        3 -> Icons.Outlined.AcUnit
        4 -> Icons.Outlined.Lightbulb
        5 -> Icons.Outlined.Kitchen
        6 -> Icons.Outlined.LocalLaundryService
        7 -> Icons.Outlined.OfflineBolt
        8 -> Icons.Outlined.WbSunny
        else -> Icons.Outlined.Restaurant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: Number + Icon + Title + Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Number index badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(colors.primaryContainer)
                        .padding(top = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.number,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.onPrimaryContainer,
                    )
                }

                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) colors.primaryContainer else Color(item.iconBgColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = mantraVectorIcon,
                        contentDescription = null,
                        tint = if (isDark) colors.onPrimaryContainer else Color(0xFF19380A),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title + Description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action / Check in button (top-right aligned)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (item.isCheckedIn) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF059669) else Color(0xFF16A34A),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button) { onClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Checked In",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (item.checkInTimeDisplay != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.checkInTimeDisplay,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.heightIn(min = 48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, colors.primary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = colors.surface,
                                contentColor = colors.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Check In",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bottom row: Potential savings + Frequency badge
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.potentialSavings != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = item.potentialSavings,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Frequency badge — always horizontal, never wraps
                val (badgeBg, badgeText) = when (item.frequencyBadge) {
                    "Daily" -> if (isDark) Pair(Color(0xFF1E3A8A), Color(0xFF93C5FD)) else Pair(Color(0xFFEFF6FF), Color(0xFF2563EB))
                    "Weekly" -> if (isDark) Pair(Color(0xFF581C87), Color(0xFFD8B4FE)) else Pair(Color(0xFFFAF5FF), Color(0xFF9333EA))
                    "Monthly" -> if (isDark) Pair(Color(0xFF881337), Color(0xFFFDA4AF)) else Pair(Color(0xFFFFF1F2), Color(0xFFE11D48))
                    else -> if (isDark) Pair(Color(0xFF451A03), Color(0xFFFDBA74)) else Pair(Color(0xFFFFFBEB), Color(0xFFB45309))
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = item.frequencyBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = badgeText,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
