package com.example.feature.home.ui.modules

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.ui.theme.*


import com.example.ui.theme.rememberIsReducedMotionEnabled

@Composable
fun BillCalculatorCalculatingView(
    unitsText: String,
    step: Int,
    onCancel: () -> Unit
) {
    val isReducedMotion = rememberIsReducedMotionEnabled()
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")
    val pulseScaleAnimated by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseScale = if (isReducedMotion) 1f else pulseScaleAnimated

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Orbital hero circle
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            // Ambient outer pulse ring
            Box(
                modifier = Modifier
                    .size((120 * pulseScale).dp)
                    .clip(CircleShape)
                    .background(StitchPrimaryFixed.copy(alpha = 0.35f))
            )
            // Circular Progress Indicator
            CircularProgressIndicator(
                progress = {
                    when (step) {
                        1 -> 0.33f
                        2 -> 0.66f
                        else -> 0.95f
                    }
                },
                modifier = Modifier.size(110.dp),
                color = StitchPrimary,
                trackColor = StitchSurfaceContainer,
                strokeWidth = 6.dp
            )
            // Inner Core Node
            Surface(
                modifier = Modifier.size(80.dp),
                shape = CircleShape,
                color = StitchSurfaceLowest,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "$unitsText kWh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurfaceVariant
                    )
                }
            }
        }

        // Headline & Context
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Calculating your estimated bill",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface
            )
            Text(
                text = "Evaluating $unitsText kWh against active domestic slabs",
                fontSize = 13.sp,
                color = StitchOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        // Stepper Checklist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1
                CalculatingStepRow(
                    title = "Applying Tamil Nadu electricity tariff",
                    subtitle = "Verified TANGEDCO 2024–25 Schedule",
                    isCompleted = step >= 1,
                    isActive = step == 1
                )

                HorizontalDivider(color = StitchSurfaceContainer, thickness = 1.dp)

                // Step 2
                CalculatingStepRow(
                    title = "Calculating tiered energy charges",
                    subtitle = "Allocating slab buckets (0–100, 101–400, 401–500+)",
                    isCompleted = step >= 2,
                    isActive = step == 2
                )

                HorizontalDivider(color = StitchSurfaceContainer, thickness = 1.dp)

                // Step 3
                CalculatingStepRow(
                    title = "Preparing your bill breakdown & slab estimate",
                    subtitle = "Benchmarking domestic tariff schedules",
                    isCompleted = step >= 3,
                    isActive = step == 3
                )
            }
        }

        // Regulatory Reassurance Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StitchSurfaceLow)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(StitchSurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = StitchPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "OFFICIAL STANDARD",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Using official TNERC LT-1A domestic slab structure including free allowance benefits.",
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Cancel Calculation Button
        OutlinedButton(
            onClick = onCancel,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, StitchOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchOnSurfaceVariant),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancel calculation", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CalculatingStepRow(
    title: String,
    subtitle: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> StitchPrimaryFixed
                        isActive -> StitchPrimary
                        else -> StitchSurfaceContainer
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = StitchOnPrimaryFixedVariant,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isActive) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = StitchOnPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StitchOnSurfaceVariant.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (isActive || isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = StitchOnSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = if (isCompleted) StitchPrimary else StitchOnSurfaceVariant
            )
        }
    }
}

