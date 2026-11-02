package com.example.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraOutdoor
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SolarEaseSemanticColors
import com.example.ui.theme.StitchOnPrimary
import com.example.ui.theme.StitchOnSurface
import com.example.ui.theme.StitchOnSurfaceVariant
import com.example.ui.theme.StitchOutline
import com.example.ui.theme.StitchPrimaryContainer
import com.example.ui.theme.StitchSurfaceContainerLow
import com.example.ui.theme.StitchSurfaceContainerLowest
import java.util.Locale

/**
 * Solar Lens Priority Action Card shown in Step 3 when audit is pending.
 */
@Composable
fun SolarLensPriorityHeroCard(
    uiState: HomeUiState,
    onOpenSolarLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consumedUnits = uiState.currentEnergyMetrics?.unitsConsumed ?: 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E3A2F),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2E5A44)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF74D99F),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "PRIORITY ACTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF74D99F),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Recommended",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Find Where Your Electricity Goes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (consumedUnits > 0.0) {
                        "You've consumed ${String.format(Locale.getDefault(), "%.1f", consumedUnits)} kWh since your baseline. Scan your appliances with Solar Lens to audit high-power loads before sizing solar."
                    } else {
                        "Audit your high-power appliances with Solar Lens to understand individual device consumption before sizing your rooftop solar system."
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 17.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ApplianceTag(label = "Air Conditioner")
                ApplianceTag(label = "Water Heater")
                ApplianceTag(label = "Water Pump")
            }

            Button(
                onClick = onOpenSolarLens,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF74D99F),
                    contentColor = Color(0xFF0F291E)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Appliance Audit with Solar Lens",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ApplianceTag(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

/**
 * Summary card displayed once appliance audit is completed.
 */
@Composable
fun CompletedAuditSummaryCard(
    uiState: HomeUiState,
    onOpenSolarLens: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SolarEaseSemanticColors.SuccessText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Appliance Audit Completed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val applianceCount = uiState.appliances.size
                        val kwText = String.format(Locale.getDefault(), "%.1f", uiState.totalConnectedLoadKw)
                        Text(
                            text = "$applianceCount appliances cataloged • $kwText kW connected load",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = onOpenSolarLens,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Edit Audit →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = "Your household appliance load has been analyzed. Sizing recommendations now incorporate your actual daytime vs nighttime load profile.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

/**
 * Educational promotion card encouraging appliance audit with dynamic kWh insertion.
 */
@Composable
fun KnowYourConsumptionCard(
    uiState: HomeUiState,
    onOpenSolarLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentKwh = uiState.currentBillingCycleKwh
    val kwhText = if (currentKwh > 0.0) {
        if (currentKwh % 1.0 == 0.0) "${currentKwh.toInt()}" else String.format(Locale.getDefault(), "%.1f", currentKwh)
    } else null

    val headingText = if (kwhText != null) {
        "Find where your $kwhText kWh is going"
    } else {
        "Find where your electricity is going"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "KNOW YOUR CONSUMPTION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )

            Text(
                text = headingText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Run an appliance audit to discover which heavy loads (AC, water heater, pump) can be optimized to lower your bill into the next lower tariff slab.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Button(
                onClick = onOpenSolarLens,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Start Appliance Audit with Solar Lens",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Detailed Solar Lens Card with feature bullets and computer vision status.
 */
@Composable
fun SolarLensHeroCard(
    uiState: HomeUiState,
    onLaunchLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            .background(StitchPrimaryContainer.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraOutdoor,
                            contentDescription = null,
                            tint = StitchPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "INTELLIGENT AUDIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Solar Lens • AI Load Audit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchPrimaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v2.4 CV",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (uiState.hasCompletedAudit) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = StitchSurfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.appliances.size} Active Appliances Profiled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchOnSurface
                            )
                            Text(
                                text = "Optical CV model verified",
                                fontSize = 11.sp,
                                color = StitchOnSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f kW", uiState.totalConnectedLoadKw),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimaryContainer
                            )
                            Text(
                                text = "Connected Load",
                                fontSize = 10.sp,
                                color = StitchOnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureBullet(
                        icon = Icons.Default.ViewInAr,
                        text = "Room-by-room CV spatial detection"
                    )
                    FeatureBullet(
                        icon = Icons.Default.Sensors,
                        text = "Appliance wattage profiling & inductive spikes"
                    )
                    FeatureBullet(
                        icon = Icons.Default.Tune,
                        text = "Precision multi-tier solar array sizing recommendation"
                    )
                }
            }

            Button(
                onClick = onLaunchLens,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchPrimaryContainer,
                    contentColor = StitchOnPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (uiState.hasCompletedAudit) "Open Solar Lens" else "Launch Solar Lens",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureBullet(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StitchPrimaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = StitchOnSurfaceVariant
        )
    }
}
