package com.example.feature.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Roofing
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun AvailableAreaRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    AvailableAreaScreen(
        availableAreaSqM = state.availableAreaSqM,
        isSqFtMode = state.isSquareFeetMode,
        onAreaChange = { vendorViewModel.updateAvailableArea(it) },
        onToggleUnit = { vendorViewModel.toggleAreaUnit(it) },
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun AvailableAreaScreen(
    availableAreaSqM: Double,
    isSqFtMode: Boolean,
    onAreaChange: (Double) -> Unit,
    onToggleUnit: (Boolean) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var rawText by remember(availableAreaSqM, isSqFtMode) {
        val displayVal = if (isSqFtMode) (availableAreaSqM * 10.7639).roundToInt() else availableAreaSqM.toInt()
        mutableStateOf(displayVal.toString())
    }

    val sqFtVal = (availableAreaSqM * 10.7639).roundToInt()

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            Column {
                StitchVendorTopBar(
                    title = "System Requirements",
                    onNavigateBack = onNavigateBack
                )
                StitchStepProgressBar(
                    currentStep = 6,
                    totalSteps = 8,
                    stepTitle = "Space Assessment",
                    category = "75% Done"
                )
            }
        },
        bottomBar = {
            Surface(
                color = StitchSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                StitchPrimaryButton(
                    text = "CONTINUE",
                    enabled = availableAreaSqM > 0,
                    onClick = onNavigateNext
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header block
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "How much space is available?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Enter the approximate area where solar panels can be installed.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // Visual Blueprint Mini-Widget (Stitch Layout Frame)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Roofing,
                                contentDescription = null,
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ROOFTOP LAYOUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSecondary,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StitchSurfaceContainerLow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = StitchPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Clear Sun Exposure",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StitchSecondary
                                )
                            }
                        }
                    }

                    // Blueprint SVG Vector Illustration
                    StitchHeroIllustration(
                        drawableRes = R.drawable.ic_stitch_available_area,
                        contentDescription = "Rooftop layout dimensions"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Usable Area: ",
                            fontSize = 12.sp,
                            color = StitchSecondary
                        )
                        Text(
                            text = "6.0m × 5.0m ≈ 30.0 sq. m",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer
                        )
                    }
                }
            }

            // Precision Input Container (Stitch design)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AVAILABLE AREA",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchSecondary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "RECOMMENDED: ≥ 20 SQ. M",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Input box
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = StitchSurfaceContainerLow,
                        border = BorderStroke(1.dp, StitchSecondaryContainer.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            BasicTextField(
                                value = rawText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }.take(5)
                                    rawText = digits
                                    val num = digits.toDoubleOrNull() ?: 0.0
                                    val sqm = if (isSqFtMode) num / 10.7639 else num
                                    onAreaChange(sqm)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface,
                                    letterSpacing = (-0.5).sp
                                ),
                                cursorBrush = SolidColor(StitchPrimaryContainer),
                                modifier = Modifier.weight(1f)
                            )

                            // Unit badge toggle button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = StitchSecondaryContainer,
                                modifier = Modifier.clickable { onToggleUnit(!isSqFtMode) }
                            ) {
                                Text(
                                    text = if (isSqFtMode) "sq. ft" else "sq. m",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchTertiary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Imperial equivalent row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = StitchSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Imperial equivalent:",
                            fontSize = 12.sp,
                            color = StitchSecondary
                        )
                        Text(
                            text = "≈ $sqFtVal sq. ft (${availableAreaSqM.toInt()} sq. m)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                    }
                }
            }

            // Helpful Tip Card (Stitch design)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = StitchPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "HELPFUL TIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "Clear, unobstructed space is usually more useful for solar installation.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                        Text(
                            text = "Shadows from nearby tall trees or water tanks can reduce generation, so count only shadow-free space.",
                            fontSize = 12.sp,
                            color = StitchSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

