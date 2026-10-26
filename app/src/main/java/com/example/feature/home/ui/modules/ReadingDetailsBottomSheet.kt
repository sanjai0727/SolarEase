package com.example.feature.home.ui.modules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Date
import java.util.Locale

private val PrimaryBlue = Color(0xFF2563EB)
private val BlueLightContainer = Color(0xFFEFF6FF)
private val BlueBorder = Color(0xFFDBEAFE)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val WarningAmber = Color(0xFFD97706)
private val WarningBg = Color(0xFFFFFBEB)
private val BorderColor = Color(0xFFE2E8F0)

/**
 * Reading Details Modal Bottom Sheet.
 * Features:
 * - 3 filter tabs: 7D, 30D, All (reading count based).
 * - History list with actual cumulative reading, timestamps, and interval consumption.
 * - Expandable accordion row per entry revealing previous reading, interval, and correction metadata.
 * - Empty states per filter tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingDetailsBottomSheet(
    onDismiss: () -> Unit,
    onEditReading: (String) -> Unit = {},
    initialSelectedReadingId: String? = null,
    anchorDate: Date? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilter by remember { mutableStateOf(ReadingDetailsFilter.D7) }
    var expandedReadingId by remember { mutableStateOf(initialSelectedReadingId) }

    val items = remember(selectedFilter, anchorDate) {
        MeterReadingCache.getFilteredReadingHistory(selectedFilter, anchorDate)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .animateContentSize()
        ) {
            // ── 1. Top Header with Title, Count Badge, and Close Button ──
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
                            .clip(CircleShape)
                            .background(BlueLightContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Reading Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = selectedFilter.description,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (items.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = BlueLightContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                text = "${items.size} ${if (items.size == 1) "reading" else "readings"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_reading_details_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Reading Details",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 2. Filter Tabs (7D | 30D | All) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ReadingDetailsFilter.values().forEach { filter ->
                    val isSelected = (filter == selectedFilter)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) PrimaryBlue else Color.Transparent)
                            .clickable { selectedFilter = filter }
                            .padding(vertical = 8.dp)
                            .testTag("filter_tab_${filter.label}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── 3. Reading History List or Empty State ──
            if (items.isEmpty()) {
                val emptyMessage = when (selectedFilter) {
                    ReadingDetailsFilter.D7 -> "No meter readings recorded."
                    ReadingDetailsFilter.D30 -> "No meter readings recorded."
                    ReadingDetailsFilter.ALL -> "No meter readings yet. Add your first reading to get started."
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = TextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = emptyMessage,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.reading.id }) { item ->
                        val isExpanded = (expandedReadingId == item.reading.id)
                        ReadingHistoryEntryCard(
                            item = item,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedReadingId = if (isExpanded) null else item.reading.id
                            },
                            onEditReading = onEditReading
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ReadingHistoryEntryCard(
    item: ReadingHistoryItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEditReading: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onToggleExpand() }
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) Color(0xFFF8FAFC) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isExpanded) PrimaryBlue.copy(alpha = 0.4f) else BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Main Row: Cumulative Reading, Date/Time, Consumption Pill, Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%,.0f", item.reading.readingValue)} kWh",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    val timeSuffix = if (item.formattedTime.isNotBlank()) " · ${item.formattedTime}" else ""
                    Text(
                        text = "${item.dateDisplay}$timeSuffix",
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    // Consumption or Baseline / Decreased status
                    if (item.isBaseline) {
                        Text(
                            text = "Baseline reading — consumption not yet available",
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (item.isDecreased) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Review required · Lower than previous reading",
                                fontSize = 11.sp,
                                color = WarningAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (item.consumptionSincePrevious != null) {
                        val prevName = item.previousReading?.displayDate?.ifBlank { item.previousReading?.readingDate } ?: "previous"
                        val intervalStr = if (item.intervalDays != null && item.intervalDays > 1) {
                            "${item.intervalDays} days since $prevName"
                        } else {
                            "since $prevName"
                        }
                        Text(
                            text = "Consumption: ${String.format(Locale.getDefault(), "%,.0f", item.consumptionSincePrevious)} kWh $intervalStr",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanded Accordion Details
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = BorderColor.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        DetailRow(
                            label = "Actual Cumulative Reading",
                            value = "${String.format(Locale.getDefault(), "%,.0f", item.reading.readingValue)} kWh"
                        )
                        DetailRow(
                            label = "Recorded Date & Time",
                            value = "${item.dateDisplay}${if (item.formattedTime.isNotBlank()) " at " + item.formattedTime else ""}"
                        )

                        if (item.previousReading != null) {
                            DetailRow(
                                label = "Previous Valid Reading",
                                value = "${String.format(Locale.getDefault(), "%,.0f", item.previousReading.readingValue)} kWh (${item.previousReading.displayDate.ifBlank { item.previousReading.readingDate }})"
                            )
                        }

                        if (item.intervalDays != null) {
                            DetailRow(
                                label = "Elapsed Interval",
                                value = "${item.intervalDays} ${if (item.intervalDays == 1) "day" else "days"}"
                            )
                        }

                        if (item.consumptionSincePrevious != null) {
                            DetailRow(
                                label = "Calculated Consumption",
                                value = "+${String.format(Locale.getDefault(), "%,.0f", item.consumptionSincePrevious)} kWh",
                                valueColor = PrimaryBlue,
                                isBold = true
                            )
                        } else if (item.isDecreased) {
                            DetailRow(
                                label = "Status",
                                value = "Flagged for review (lower than previous)",
                                valueColor = WarningAmber,
                                isBold = true
                            )
                        } else if (item.isBaseline) {
                            DetailRow(
                                label = "Status",
                                value = "Cycle baseline anchor",
                                valueColor = PrimaryBlue
                            )
                        }

                        if (item.reading.isCorrected) {
                            DetailRow(
                                label = "Correction Note",
                                value = item.reading.correctionReason ?: "Corrected reading"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onEditReading(item.reading.readingDate) },
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Correct Reading",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color = TextDark,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
