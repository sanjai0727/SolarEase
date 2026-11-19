package com.example.feature.recommends.ui.modules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.*

@Composable
fun TodayProgressCard(
    modifier: Modifier = Modifier,
    checkedCount: Int,
    totalCount: Int
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else SolarBrandGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Today's Mantra Progress",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary
                    )
                }

                Text(
                    text = "$checkedCount of $totalCount completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen
                )
            }

            val progress = (checkedCount.toFloat() / totalCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenLight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(SolarBrandGreen, SolarBrandGreenSecondary)
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
    val isDark = LocalIsDarkTheme.current
    val checkedCount = mantras.count { it.isCheckedIn }
    val totalCount = mantras.size

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary,
                    letterSpacing = (-0.2).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Check in regularly, build the habit and reduce your electricity consumption.",
                    fontSize = 12.sp,
                    color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
                    lineHeight = 16.sp
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
                            if (isDark) Color(0xFF064E3B) else SolarBrandGreenLight
                        } else {
                            if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenSurface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (checkedCount == totalCount) {
                                if (isDark) Color(0xFF065F46) else SolarBrandGlassCardBorder
                            } else {
                                if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder
                            }
                        )
                    ) {
                        Text(
                            text = "$checkedCount of $totalCount completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (checkedCount == totalCount) {
                                if (isDark) Color(0xFF86EFAC) else SolarBrandGreen
                            } else {
                                if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary
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
                        tint = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
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
        color = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row: Number + Icon + Title + Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Number index badge
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight)
                        .border(
                            1.dp,
                            if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else SolarBrandGlassCardBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.number,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else SolarBrandGreen,
                    )
                }

                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = mantraVectorIcon,
                        contentDescription = null,
                        tint = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Title + Description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) MaterialTheme.colorScheme.onSurface else SolarBrandTextPrimary,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
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
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDark) Color(0xFF059669) else SolarBrandGreen,
                            modifier = Modifier
                                .heightIn(min = 40.dp)
                                .clickable(role = Role.Button) { onClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                        fontWeight = FontWeight.Bold,
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
                                color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextMuted,
                                maxLines = 1
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.heightIn(min = 40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color.White,
                                contentColor = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
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
                    .padding(start = 74.dp),
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
                            tint = if (isDark) Color(0xFF34D399) else SolarBrandGreenSecondary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = item.potentialSavings,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color(0xFF6EE7B7) else SolarBrandGreenSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenLight
                ) {
                    Text(
                        text = item.frequencyBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
