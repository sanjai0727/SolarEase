package com.example.feature.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun MonthlyConsumptionRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    MonthlyConsumptionScreen(
        consumptionKwh = state.monthlyConsumptionKwh,
        onConsumptionChange = { vendorViewModel.updateMonthlyConsumption(it) },
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun MonthlyConsumptionScreen(
    consumptionKwh: Double,
    onConsumptionChange: (Double) -> Unit,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var rawText by remember(consumptionKwh) { mutableStateOf(consumptionKwh.toInt().toString()) }
    val quickOptions = listOf(200.0, 350.0, 500.0, 650.0)

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            Column {
                StitchVendorTopBar(
                    title = "System Requirements",
                    onNavigateBack = onNavigateBack
                )
                StitchStepProgressBar(
                    currentStep = 4,
                    totalSteps = 8,
                    stepTitle = "Electricity Usage"
                )
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StitchPrimaryButton(
                    text = "Continue",
                    onClick = onNavigateNext
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StitchSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Utility data encrypted • Zero spam guarantee",
                        fontSize = 11.5.sp,
                        color = StitchSecondary
                    )
                }
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
            // Visual badge from Stitch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(StitchSurfaceContainerHigh.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = StitchPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )

                    // Top-left speed indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceContainerLowest),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = StitchPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Bottom-right bolt badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(StitchPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = StitchOnPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Header titles
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "How much electricity do you use?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = "Confirm or adjust your typical monthly power consumption.",
                    fontSize = 15.sp,
                    color = StitchSecondary,
                    lineHeight = 22.sp
                )
            }

            // Primary Consumption Instrument Card
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
                    // Header row: display value & edit button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Your average monthly use",
                                fontSize = 13.sp,
                                color = StitchSecondary
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${consumptionKwh.toInt()}",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = "kWh / month",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StitchSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StitchSurfaceContainerLow,
                            modifier = Modifier.clickable { isEditing = !isEditing }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = StitchPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (isEditing) "Done" else "Not correct? Edit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StitchPrimaryContainer
                                )
                            }
                        }
                    }

                    // Editable input field if in edit mode
                    if (isEditing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Enter your monthly consumption (kWh)",
                                fontSize = 12.sp,
                                color = StitchSecondary
                            )
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = StitchSurfaceContainerLow,
                                border = BorderStroke(1.dp, StitchSecondaryContainer.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = StitchSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    BasicTextField(
                                        value = rawText,
                                        onValueChange = { input ->
                                            val digits = input.filter { it.isDigit() }.take(5)
                                            rawText = digits
                                            digits.toDoubleOrNull()?.let { onConsumptionChange(it) }
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = StitchOnSurface
                                        ),
                                        cursorBrush = SolidColor(StitchPrimaryContainer),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "kWh / month",
                                        fontSize = 13.sp,
                                        color = StitchSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Detection callout
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StitchPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "We found this from your electricity information.",
                            fontSize = 12.sp,
                            color = StitchSecondary
                        )
                    }

                    // Quick adjustments pills
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "QUICK ADJUSTMENTS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchSecondary,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickOptions.forEach { opt ->
                                val isSelected = consumptionKwh.toInt() == opt.toInt()
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            rawText = opt.toInt().toString()
                                            onConsumptionChange(opt)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) StitchPrimaryContainer else StitchSurfaceContainerLow,
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (isSelected) StitchPrimaryContainer else StitchOutline.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = "${opt.toInt()} kWh",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) StitchOnPrimary else StitchOnSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // How is this used?
            StitchInfoCard(
                icon = Icons.Default.HelpOutline,
                title = "How is this used?",
                description = "This helps us estimate the exact solar system capacity (kW) needed to reduce your bi-monthly bill to zero."
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

