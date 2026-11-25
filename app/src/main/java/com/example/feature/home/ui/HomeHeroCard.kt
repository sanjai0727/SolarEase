package com.example.feature.home.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraOutdoor
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.home.ui.modules.PendingBillUiState
import com.example.ui.components.subtleTopRightGradient
import com.example.ui.theme.SolarEaseSemanticColors
import com.example.ui.theme.StitchOnSurface
import com.example.ui.theme.StitchOnSurfaceVariant
import com.example.ui.theme.StitchPrimary
import com.example.ui.theme.StitchPrimaryContainer
import com.example.ui.theme.*
import java.util.Locale

/**
 * Modern, airy greeting header for SolarEase Home.
 * Primary heading with 24sp bold title, refined spacing, and dynamic username.
 */
@Composable
fun ModernHomeHeader(
    userName: String?,
    hasConnectedBill: Boolean,
    consumerNumber: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val displayName = userName?.trim()?.takeIf { it.isNotBlank() } ?: "Homeowner"
            Text(
                text = "Welcome, $displayName",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OceanDeepNavy,
                letterSpacing = (-0.4).sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isReducedMotion = LocalIsReducedMotionEnabled.current
                val pulseScale = if (hasConnectedBill && !isReducedMotion) {
                    val infiniteTransition = rememberInfiniteTransition(label = "liveDotPulse")
                    val scaleVal by infiniteTransition.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1400, easing = SolarEaseMotion.easingStandard),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotScale"
                    )
                    scaleVal
                } else 1f

                Box(
                    modifier = Modifier.size(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasConnectedBill) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(OceanTeal.copy(alpha = 0.22f))
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasConnectedBill) OceanTeal else Color(0xFFD4AF37))
                    )
                }
                Text(
                    text = if (hasConnectedBill && !consumerNumber.isNullOrBlank()) {
                        "TNEB LT-1A · Consumer $consumerNumber"
                    } else if (hasConnectedBill) {
                        "TNEB LT-1A Domestic · Live Tracking"
                    } else {
                        "Tamil Nadu LT-1A Domestic Tariff"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = OceanTextSecondary
                )
            }
        }

        Surface(
            shape = CircleShape,
            color = OceanAquaContainer,
            border = BorderStroke(1.dp, OceanAqua.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Peak solar hours",
                    tint = OceanTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Premium Translucent Glassmorphic Hero Card displaying Previous Bill Consumption.
 * Shows verified completed billing cycle consumption and pending bill status.
 */
@Composable
fun PreviousBillConsumptionHeroCard(
    uiState: HomeUiState,
    onViewBillDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prevKwh = uiState.previousBillConsumptionKwh
    var animateTrigger by remember(prevKwh) { mutableStateOf(false) }
    LaunchedEffect(prevKwh) {
        animateTrigger = true
    }
    val targetKwh = (prevKwh ?: 0.0).toFloat()
    val animatedKwh by animateFloatAsState(
        targetValue = if (animateTrigger) targetKwh else 0f,
        animationSpec = motionTween(
            durationMillis = 650,
            easing = SolarEaseMotion.easingDecelerate
        ),
        label = "prevKwhCountUp"
    )
    val kwhText = if (prevKwh != null && prevKwh > 0.0) {
        val displayVal = if (LocalIsReducedMotionEnabled.current) prevKwh else animatedKwh.toDouble()
        if (prevKwh % 1.0 == 0.0) "${displayVal.toInt()}" else String.format(Locale.getDefault(), "%.1f", displayVal)
    } else null

    val pendingBill = uiState.pendingBillState as? PendingBillUiState.Pending
    val isDark = LocalIsDarkTheme.current
    val cardGradient = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF102A22), Color(0xFF0C1D18)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFCFDF9), Color(0xFFF1F7F1)))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = OceanGlassCardBg,
        border = BorderStroke(1.dp, OceanGlassCardBorder),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .subtleTopRightGradient(
                    shape = RoundedCornerShape(22.dp),
                    radiusMultiplier = 0.85f
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header badge row: Feature Category & Bill Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OceanAquaContainer,
                        border = BorderStroke(1.dp, OceanAqua.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = OceanTeal,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "PREVIOUS BILL CONSUMPTION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanTeal,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    if (pendingBill != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Pending Bill",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    } else if (uiState.isPaid || uiState.billData?.isDuesNil == true) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = OceanAquaContainer,
                            border = BorderStroke(1.dp, OceanAqua.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Paid · Nil Dues ✓",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OceanTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Primary Metrics Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CONSUMPTION RECORDED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = OceanTextMuted,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (kwhText != null) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = kwhText,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OceanDeepNavy,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "kWh",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OceanTextSecondary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = "Previous completed billing cycle",
                                fontSize = 11.sp,
                                color = OceanTextSecondary
                            )
                        } else {
                            Text(
                                text = "Unavailable",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanTextSecondary
                            )
                            Text(
                                text = "Billing cycle data not yet synchronized",
                                fontSize = 11.sp,
                                color = OceanTextMuted
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        if (pendingBill != null) {
                            Text(
                                text = "OUTSTANDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309),
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = pendingBill.amountFormatted,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9A3412)
                            )
                            Text(
                                text = "Due: ${pendingBill.dueDateFormatted ?: "Due Soon"}",
                                fontSize = 11.sp,
                                color = OceanTextSecondary
                            )
                        } else {
                            Text(
                                text = "BILL STATUS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanTeal,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹0",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanTeal
                            )
                            Text(
                                text = "No outstanding dues",
                                fontSize = 11.sp,
                                color = OceanTextSecondary
                            )
                        }
                    }
                }

                // Billing Cycle & Tariff Banner Strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = SolarBrandGreenSurface,
                    border = BorderStroke(1.dp, OceanGlassCardBorder.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = OceanTeal,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Cycle: ${uiState.previousBillingPeriodLabel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OceanTextPrimary
                            )
                        }

                        Text(
                            text = uiState.tariffCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OceanTeal
                        )
                    }
                }

                // CTA Button
                if (pendingBill != null) {
                    val blueCta = if (isDark) Color(0xFF3B82F6) else Color(0xFF1D4ED8)
                    Button(
                        onClick = onViewBillDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = blueCta,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
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
                                text = "View Bill Details (${pendingBill.amountFormatted}) →",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = onViewBillDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, OceanTeal.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = OceanTeal,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "View Verified Bill History →",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OceanTeal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean, single-surface hero card for new users without a connected bill.
 * Eliminates nested cards, bloated steppers, and visual congestion.
 * Translucent glass styling with clear call-to-action.
 */
@Composable
fun ConnectBillHeroCard(
    onConnectBill: () -> Unit,
    modifier: Modifier = Modifier,
    onLearnMore: (() -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val connectGradient = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF102A22), Color(0xFF0C1D18)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFCFDF9), Color(0xFFF1F7F1)))
    }
    val ctaInteraction = remember { MutableInteractionSource() }
    val ctaPressed by ctaInteraction.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(
        targetValue = if (ctaPressed) 0.98f else 1f,
        animationSpec = motionTween(durationMillis = 120),
        label = "connectCtaScale"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = OceanGlassCardBg,
        border = BorderStroke(1.dp, OceanGlassCardBorder),
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(connectGradient)
                .subtleTopRightGradient(
                    shape = RoundedCornerShape(22.dp),
                    radiusMultiplier = 0.85f
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = OceanAquaContainer,
                        border = BorderStroke(1.dp, OceanAqua.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = OceanTeal,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "TNEB LT-1A DOMESTIC",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = OceanTeal,
                                letterSpacing = 0.6.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "Bill Not Linked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB45309),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Connect Your Electricity Bill",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanDeepNavy,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Link your 11–12 digit consumer number to establish your baseline tariff schedule, avoid bi-monthly slab jumps, and verify official statements.",
                        fontSize = 13.sp,
                        color = OceanTextSecondary,
                        lineHeight = 18.sp
                    )
                }

                // 3 Benefit Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BenefitChip(label = "Slab Alerts", modifier = Modifier.weight(1f))
                    BenefitChip(label = "Due Reminders", modifier = Modifier.weight(1f))
                    BenefitChip(label = "Statement History", modifier = Modifier.weight(1f))
                }

                // Primary CTA button
                Button(
                    onClick = onConnectBill,
                    interactionSource = ctaInteraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(ctaScale)
                        .heightIn(min = 48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanPrimaryBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Connect TNEB Bill",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (onLearnMore != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLearnMore() }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Where do I find my consumer number?  ",
                            fontSize = 12.sp,
                            color = OceanTeal,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null,
                            tint = OceanTeal,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = OceanAquaContainer,
        border = BorderStroke(1.dp, OceanAqua.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = OceanTeal,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = OceanTeal,
                maxLines = 1
            )
        }
    }
}

/**
 * Airy feature card inviting users to explore Solar Lens AI appliance audit.
 */
@Composable
fun SolarLensShowcaseCard(
    onOpenSolarLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraOutdoor,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "APPLIANCE LOAD AUDIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Camera Assisted",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Audit High-Power Appliances",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Scan air conditioners, water heaters, and pumps to calculate your day vs. night load split and detect heavy power draw.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Air Conditioner", "Water Heater", "Water Pump").forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenSolarLens,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Launch Appliance Audit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
