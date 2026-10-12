package com.example.feature.assessment.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.core.session.UserSession
import com.example.feature.assessment.util.EstimateCalculator
import com.example.feature.ebservice.util.formatConsumerNumber
import com.example.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// SHARED STATE & VIEWMODEL FOR THE 12-STEP SOLAR VENDOR JOURNEY
// ─────────────────────────────────────────────────────────────────────────────

data class SolarVendorState(
    // Step 1: Consumer Number
    val rawConsumerNumber: String = "09440233135",
    val formattedConsumerNumber: String = "09-440-233-135",
    val isConsumerValid: Boolean = true,

    // Step 2: Property Type
    val propertyType: String = "Home", // Home, Business

    // Step 3: Solar System Type
    val systemType: String = "On-Grid", // On-Grid, Off-Grid, Hybrid

    // Step 4: Monthly Consumption (kWh)
    val monthlyConsumptionKwh: Double = 350.0,

    // Step 5: Mount Type / Location
    val mountType: String = "Rooftop", // Rooftop, Ground

    // Step 6: Available Area (sq. m)
    val availableAreaSqM: Double = 30.0,
    val isSquareFeetMode: Boolean = false,

    // Step 7: Sizing Recommendation (Calculated)
    val recommendedCapacityKw: Double = 2.9,
    val requiredAreaSqM: Double = 30.0,
    val isFeasible: Boolean = true,

    // Step 8: Panel Type
    val panelType: String = "DCR Panels", // DCR Panels, Non-DCR Panels

    // Step 9: Nearby Vendors
    val selectedVendorId: String = "abc_solar",
    val selectedVendorName: String = "ABC Solar",

    // Step 11: Request Details
    val customerName: String = "Sanjai",
    val customerPhone: String = "+91 98401 23456",
    val requestNotes: String = "",

    // Step 12: Confirmation
    val referenceId: String = "REQ-SOL-2026-8841",
    val submissionTimestamp: String = "Today, 14:40 IST"
) {
    val docketId: String get() = referenceId
}

class SolarVendorViewModel : ViewModel() {
    private val _state = MutableStateFlow(SolarVendorState())
    val state: StateFlow<SolarVendorState> = _state.asStateFlow()

    init {
        val user = UserSession.currentUser
        val savedEb = user?.ebServiceNumber ?: UserSession.getSavedEbServiceNumber()
        val initialEb = if (!savedEb.isNullOrBlank()) savedEb.filter { it.isDigit() } else "09440233135"
        val name = if (!user?.name.isNullOrBlank()) user?.name!! else "Sanjai"
        val phone = if (!user?.mobileNumber.isNullOrBlank()) user?.mobileNumber!! else "+91 98401 23456"

        _state.update {
            it.copy(
                rawConsumerNumber = initialEb,
                formattedConsumerNumber = formatConsumerNumber(initialEb),
                isConsumerValid = initialEb.length in 10..12,
                customerName = name,
                customerPhone = phone
            )
        }
        recalculateSizing()
    }

    fun updateConsumerNumber(number: String) {
        val clean = number.filter { it.isDigit() }.take(12)
        _state.update {
            it.copy(
                rawConsumerNumber = clean,
                formattedConsumerNumber = formatConsumerNumber(clean),
                isConsumerValid = clean.length in 10..12
            )
        }
    }

    fun clearConsumerNumber() {
        _state.update {
            it.copy(
                rawConsumerNumber = "",
                formattedConsumerNumber = "",
                isConsumerValid = false
            )
        }
    }

    fun selectPropertyType(type: String) {
        _state.update { it.copy(propertyType = type) }
    }

    fun selectSystemType(type: String) {
        _state.update { it.copy(systemType = type) }
    }

    fun updateMonthlyConsumption(kwh: Double) {
        _state.update { it.copy(monthlyConsumptionKwh = kwh) }
        recalculateSizing()
    }

    fun selectMountType(mount: String) {
        _state.update { it.copy(mountType = mount) }
    }

    fun updateAvailableArea(areaSqM: Double) {
        _state.update { it.copy(availableAreaSqM = areaSqM) }
        recalculateSizing()
    }

    fun toggleAreaUnit(isSqFt: Boolean) {
        _state.update { it.copy(isSquareFeetMode = isSqFt) }
    }

    fun selectPanelType(panel: String) {
        _state.update { it.copy(panelType = panel) }
    }

    fun selectVendor(id: String, name: String) {
        _state.update {
            it.copy(
                selectedVendorId = id,
                selectedVendorName = name
            )
        }
    }

    fun updateRequestNotes(notes: String) {
        _state.update { it.copy(requestNotes = notes) }
    }

    fun submitEstimateRequest(): String {
        val uniqueNumber = (1000..9999).random()
        val generatedId = "REQ-SOL-2026-$uniqueNumber"
        val now = java.text.SimpleDateFormat("dd MMM, HH:mm 'IST'", java.util.Locale.getDefault()).format(java.util.Date())
        _state.update {
            it.copy(
                referenceId = generatedId,
                submissionTimestamp = now
            )
        }
        return generatedId
    }

    private fun recalculateSizing() {
        val current = _state.value
        val capacity = EstimateCalculator.calculateRecommendedSystemSize(current.monthlyConsumptionKwh)
        val reqArea = EstimateCalculator.calculateRequiredArea(capacity)
        _state.update {
            it.copy(
                recommendedCapacityKw = capacity,
                requiredAreaSqM = reqArea,
                isFeasible = current.availableAreaSqM >= reqArea
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE STITCH UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StitchVendorTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = StitchSurface,
        border = BorderStroke(0.5.dp, StitchOutline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = StitchOnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
            }

            // Green Profile Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(StitchPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StitchStepProgressBar(
    currentStep: Int,
    totalSteps: Int = 8,
    stepTitle: String = "Step $currentStep of $totalSteps",
    category: String? = null,
    categoryText: String? = null,
    percentageText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(StitchPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$currentStep",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = stepTitle.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary,
                    letterSpacing = 0.5.sp
                )
            }
            val rightLabel = percentageText ?: category ?: categoryText
            if (rightLabel != null) {
                Text(
                    text = rightLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = StitchSecondary
                )
            }
        }
        val progressFraction = (currentStep.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = StitchPrimary,
            trackColor = StitchSurfaceContainer
        )
    }
}

@Composable
fun StitchHeroIllustration(
    drawableRes: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = 280.dp,
    height: androidx.compose.ui.unit.Dp = 128.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(0.85f)
                .height(height),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF1F8F5),
            border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.3f)),
            shadowElevation = 0.5.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun StitchPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = StitchPrimaryContainer,
            contentColor = Color.White,
            disabledContainerColor = StitchSurfaceContainerHigh,
            disabledContentColor = StitchOnSurfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun StitchInfoCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchSecondaryContainer.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(StitchSecondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StitchTertiary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
fun StitchSecurityBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StitchPrimaryFixed.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = StitchPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "Your data is only shared with verified DISCOM vendors you choose.",
                fontSize = 11.5.sp,
                color = StitchOnSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
fun StitchSelectableCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) StitchPrimaryContainer else StitchOutline.copy(alpha = 0.6f)
        ),
        shadowElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) StitchPrimaryFixed.copy(alpha = 0.3f) else StitchSurfaceContainerLow),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) StitchPrimaryContainer else StitchSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StitchPrimaryFixed.copy(alpha = 0.3f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Selection Circle (Stitch Radio Indicator)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 0.dp else 1.5.dp,
                        color = if (selected) Color.Transparent else StitchOutline,
                        shape = CircleShape
                    )
                    .background(if (selected) StitchPrimaryContainer else StitchSurfaceContainerLow.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
