package com.example.feature.home.ui.modules

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

// Stitch Palette Constants
private val StitchPrimary = Color(0xFF005D42)
private val StitchPrimaryContainer = Color(0xFF047857)
private val StitchPrimaryFixed = Color(0xFF97F5CC)
private val StitchOnPrimaryFixedVariant = Color(0xFF00513A)
private val StitchBackground = Color(0xFFF8F9FF)
private val StitchSurface = Color(0xFFF8F9FF)
private val StitchSurfaceLowest = Color(0xFFFFFFFF)
private val StitchSurfaceLow = Color(0xFFEFF4FF)
private val StitchSurfaceContainer = Color(0xFFE5EEFF)
private val StitchSurfaceHigh = Color(0xFFDCE9FF)
private val StitchOnSurface = Color(0xFF0B1C30)
private val StitchOnSurfaceVariant = Color(0xFF565E74)
private val StitchOutline = Color(0xFFE2E8F0)
private val StitchError = Color(0xFFBA1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillCalculatorRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSolarLens: () -> Unit = {},
    viewModel: BillCalculatorViewModel = viewModel()
) {
    val screenState by viewModel.screenState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val inputError by viewModel.inputError.collectAsState()
    val billingCycle by viewModel.billingCycle.collectAsState()
    val billResult by viewModel.billResult.collectAsState()
    val calculationStep by viewModel.calculationStep.collectAsState()
    val savedCalculations by viewModel.savedCalculations.collectAsState()

    val topBarTitle = when (screenState) {
        CalculatorScreenState.ENTRY -> "Bill Calculator"
        CalculatorScreenState.CALCULATING -> "Bill Calculator"
        CalculatorScreenState.RESULT -> "Bill Estimate"
        CalculatorScreenState.HISTORY -> "Calculation History"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when (screenState) {
                            CalculatorScreenState.CALCULATING -> viewModel.cancelCalculation()
                            CalculatorScreenState.RESULT -> viewModel.navigateToEntry()
                            CalculatorScreenState.HISTORY -> viewModel.navigateToEntry()
                            CalculatorScreenState.ENTRY -> onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StitchOnSurface
                        )
                    }
                },
                actions = {
                    when (screenState) {
                        CalculatorScreenState.ENTRY -> {
                            IconButton(
                                onClick = { viewModel.navigateToHistory() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Calculation History",
                                    tint = StitchOnSurface
                                )
                            }
                        }
                        CalculatorScreenState.RESULT -> {
                            IconButton(
                                onClick = { viewModel.recalculate() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "New Calculation",
                                    tint = StitchOnSurface
                                )
                            }
                        }
                        CalculatorScreenState.HISTORY -> {
                            if (savedCalculations.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearHistory() }) {
                                    Text(
                                        text = "Clear All",
                                        color = StitchError,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        CalculatorScreenState.CALCULATING -> {}
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StitchBackground,
                    titleContentColor = StitchOnSurface,
                    navigationIconContentColor = StitchOnSurface
                )
            )
        },
        containerColor = StitchBackground
    ) { innerPadding ->
        AnimatedContent(
            targetState = screenState,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            label = "bill_calculator_screens"
        ) { state ->
            when (state) {
                CalculatorScreenState.ENTRY -> {
                    BillCalculatorEntryView(
                        inputText = inputText,
                        inputError = inputError,
                        billingCycle = billingCycle,
                        onInputChanged = { viewModel.onInputChanged(it) },
                        onClearInput = { viewModel.clearInput() },
                        onSelectPreset = { viewModel.selectPreset(it) },
                        onSelectCycle = { viewModel.setBillingCycle(it) },
                        onCalculate = { viewModel.calculate() }
                    )
                }
                CalculatorScreenState.CALCULATING -> {
                    BillCalculatorCalculatingView(
                        unitsText = inputText.ifBlank { "0" },
                        step = calculationStep,
                        onCancel = { viewModel.cancelCalculation() }
                    )
                }
                CalculatorScreenState.RESULT -> {
                    billResult?.let { result ->
                        BillCalculatorResultView(
                            result = result,
                            billingCycle = billingCycle,
                            onRecalculate = { viewModel.recalculate() },
                            onNavigateToSolarLens = onNavigateToSolarLens
                        )
                    } ?: run {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No calculation available", color = StitchOnSurfaceVariant)
                        }
                    }
                }
                CalculatorScreenState.HISTORY -> {
                    BillCalculatorHistoryView(
                        historyList = savedCalculations,
                        onSelectCalculation = { viewModel.openSavedResult(it) },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) },
                        onCalculateNew = { viewModel.navigateToEntry() }
                    )
                }
            }
        }
    }
}

// ==========================================
// 1. ENTRY SCREEN (Matching Stitch Entry)
// ==========================================
@Composable
private fun BillCalculatorEntryView(
    inputText: String,
    inputError: String?,
    billingCycle: BillingCycle,
    onInputChanged: (String) -> Unit,
    onClearInput: () -> Unit,
    onSelectPreset: (Double) -> Unit,
    onSelectCycle: (BillingCycle) -> Unit,
    onCalculate: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val presets = listOf(150.0, 300.0, 500.0, 650.0, 800.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Subtitle Header
        Text(
            text = "Estimate your electricity bill from your monthly usage.",
            fontSize = 14.sp,
            color = StitchOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // 1. Monthly Electricity Consumption Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Electricity Consumption",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = StitchOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StitchSurfaceLow)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "0",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = onInputChanged,
                            textStyle = TextStyle(
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnSurface
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    focusManager.clearFocus()
                                    if (inputText.isNotBlank()) onCalculate()
                                }
                            ),
                            cursorBrush = SolidColor(StitchPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (inputText.isNotEmpty()) {
                        IconButton(
                            onClick = onClearInput,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Clear input",
                                tint = StitchOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        text = "kWh",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimary
                    )
                }

                // Error validation message
                if (inputError != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = inputError,
                        fontSize = 12.sp,
                        color = StitchError,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enter the electricity units shown on your latest bill.",
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Common Monthly Presets
                Text(
                    text = "COMMON MONTHLY PRESETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurfaceVariant,
                    letterSpacing = 0.6.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { presetVal ->
                        val isSelected = inputText.toDoubleOrNull() == presetVal
                        val bg = if (isSelected) StitchPrimaryFixed else StitchSurfaceContainer
                        val txtColor = if (isSelected) StitchOnPrimaryFixedVariant else StitchOnSurface

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(bg)
                                .clickable {
                                    focusManager.clearFocus()
                                    onSelectPreset(presetVal)
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "${presetVal.toInt()} kWh",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = txtColor
                            )
                        }
                    }
                }
            }
        }

        // 2. Billing Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Billing Configuration",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = StitchOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Billing Cycle Toggle
                Text(
                    text = "BILLING CYCLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceLow)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    BillingCycle.entries.forEach { cycle ->
                        val isSelected = cycle == billingCycle
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) StitchPrimary else Color.Transparent)
                                .clickable { onSelectCycle(cycle) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cycle.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else StitchOnSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tariff Readout Card
                Text(
                    text = "ELECTRICITY BOARD & TARIFF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurfaceVariant,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceLow)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Tamil Nadu (TANGEDCO)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                        Text(
                            text = "Domestic LT-1A (Subsidized Residential)",
                            fontSize = 11.sp,
                            color = StitchOnSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = StitchOnSurfaceVariant
                    )
                }
            }
        }

        // 3. Information Tip Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StitchSurfaceLow)
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(StitchPrimaryFixed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = StitchOnPrimaryFixedVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Where to find your units?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StitchOnSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Check \"Units Consumed\" or \"Total Billed kWh\" on your physical or digital electricity bill statement.",
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        // 4. Primary CTA & Disclaimer
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onCalculate()
                },
                enabled = inputText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchPrimary,
                    disabledContainerColor = StitchPrimary.copy(alpha = 0.4f),
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Calculate My Bill",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = StitchOnSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "This is an estimated bill. Your actual bill may vary based on fuel surcharge adjustments, fixed meter fees, and local state duty taxes.",
                    fontSize = 11.sp,
                    color = StitchOnSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// 2. CALCULATING SCREEN (Matching Stitch Loading)
// ==========================================
@Composable
private fun BillCalculatorCalculatingView(
    unitsText: String,
    step: Int,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_ring")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

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
                    title = "Preparing your estimate & solar savings",
                    subtitle = "Benchmarking rooftop net-metering yields",
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
            modifier = Modifier.height(40.dp)
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
                    color = Color.White,
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

// ==========================================
// 3. RESULT SCREEN (Matching Stitch Result)
// ==========================================
@Composable
private fun BillCalculatorResultView(
    result: BillResult,
    billingCycle: BillingCycle,
    onRecalculate: () -> Unit,
    onNavigateToSolarLens: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    var subsidyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Context Heading Strip
        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TANGEDCO UTILITY ESTIMATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimary,
                    letterSpacing = 0.5.sp
                )
                Surface(
                    shape = CircleShape,
                    color = StitchPrimaryFixed
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Active Rate Card",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnPrimaryFixedVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your Estimated Electricity Bill",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface
            )
            Text(
                text = "Calculated against ${result.totalUnits.toInt()} kWh metered consumption cycle",
                fontSize = 13.sp,
                color = StitchOnSurfaceVariant
            )
        }

        // Hero Amount Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "ESTIMATED BILL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(result.totalEnergyCharge),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = StitchOnSurface,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StitchSurfaceLow
                    ) {
                        Text(
                            text = "Domestic LT-1A",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceLow)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "For your selected ${billingCycle.displayName} cycle (${billingCycle.days} days)",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
            }
        }

        // Key Metrics Strip (3 Columns)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Units Used", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${result.totalUnits.toInt()} kWh",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(StitchSurfaceContainer)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cycle", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        billingCycle.displayName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(StitchSurfaceContainer)
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("State Grid", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Tamil Nadu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                }
            }
        }

        // Bill Breakdown Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = StitchPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bill Breakdown",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                    }
                    Text(
                        text = "Itemized (INR)",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (result.breakdown.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchSurfaceLow)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Your consumption of ${result.totalUnits.toInt()} kWh is within the ${result.freeUnits.toInt()} free units limit. Zero energy charge is applicable.",
                            fontSize = 12.sp,
                            color = StitchPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    result.breakdown.forEachIndexed { index, slab ->
                        val slabRange = if (slab.toUnit == Double.POSITIVE_INFINITY) {
                            "> ${slab.fromUnit.toInt()} units"
                        } else {
                            "${slab.fromUnit.toInt()}–${slab.toUnit.toInt()} units"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = slabRange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StitchOnSurface
                                )
                                Text(
                                    text = "${slab.unitsInSlab.toInt()} units @ ₹${String.format(Locale.US, "%.2f", slab.ratePerUnit)}/unit",
                                    fontSize = 11.sp,
                                    color = StitchOnSurfaceVariant
                                )
                            }
                            Text(
                                text = currencyFormat.format(slab.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchOnSurface
                            )
                        }

                        if (index < result.breakdown.size - 1) {
                            HorizontalDivider(
                                color = StitchSurfaceContainer,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceLow)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Estimated Total",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = currencyFormat.format(result.totalEnergyCharge),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Government Subsidy Callout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceLowest)
                        .clickable { subsidyExpanded = !subsidyExpanded }
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = StitchPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Government Subsidy Applied",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchPrimary
                            )
                        }
                        Icon(
                            imageVector = if (subsidyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = StitchPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = subsidyExpanded) {
                        Text(
                            text = "First ${result.freeUnits.toInt()} units are subsidized under the Tamil Nadu domestic welfare policy, saving recurring energy charges automatically on this cycle.",
                            fontSize = 12.sp,
                            color = StitchOnSurfaceVariant,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        // Consumption Insight Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "What your usage means",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StitchSurfaceHigh
                    ) {
                        Text(
                            text = "${result.totalUnits.toInt()} kWh / cycle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spectrum labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Low (<200)", fontSize = 10.sp, color = StitchOnSurfaceVariant)
                    Text("Moderate (200-500)", fontSize = 10.sp, color = StitchOnSurfaceVariant)
                    Text("High (500+)", fontSize = 10.sp, color = StitchOnSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Spectrum Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(StitchPrimary.copy(alpha = 0.25f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(StitchPrimary.copy(alpha = 0.55f))
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(StitchPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dynamic Category Status
                val (usageCategory, usageAdvice) = when {
                    result.totalUnits <= 200.0 -> {
                        "Low Usage" to "Your consumption is within the low residential band. Most or all of your usage is covered by the state allowance."
                    }
                    result.totalUnits <= 500.0 -> {
                        "Moderate Usage" to "Your consumption falls in the moderate domestic bracket. You qualify for the 200 free units allowance before standard slabs apply."
                    }
                    else -> {
                        "Higher Tier Usage" to "Your consumption exceeds 500 units, placing you in higher tariff tiers where free units adjust to 100 and higher slab rates apply."
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(StitchSurfaceHigh)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchPrimary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = usageCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceLow)
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = usageAdvice,
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Solar Savings Insight Card (Qualitative presentation without fabricated percentages)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = StitchPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Could Solar reduce this bill?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = StitchPrimaryFixed
                    ) {
                        Text(
                            text = "Net Metering",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnPrimaryFixedVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Qualitative overview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchPrimary.copy(alpha = 0.06f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "SOLAR ROOFTOP OFFSET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rooftop solar can export excess daytime power to the grid through bi-directional net metering, drastically cutting your recurring electricity cost.",
                            fontSize = 12.sp,
                            color = StitchOnSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sunlight potential banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceLow)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ideal sun exposure: ~5.2 peak sun hours/day in Tamil Nadu",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Actual solar offset and savings depend on roof area, shading, system capacity, and household consumption patterns.",
                    fontSize = 10.sp,
                    color = StitchOnSurfaceVariant,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary CTA to Solar Lens
                Button(
                    onClick = onNavigateToSolarLens,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Explore Solar with Solar Lens",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Recalculate button
        OutlinedButton(
            onClick = onRecalculate,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StitchOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchOnSurface)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Calculate Another Bill", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ==========================================
// 4. HISTORY SCREEN (Matching Stitch History)
// ==========================================
@Composable
private fun BillCalculatorHistoryView(
    historyList: List<SavedCalculation>,
    onSelectCalculation: (SavedCalculation) -> Unit,
    onDeleteItem: (String) -> Unit,
    onCalculateNew: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredList = when (selectedFilter) {
        "Bi-monthly" -> historyList.filter { it.cycle == BillingCycle.BIMONTHLY }
        "Monthly" -> historyList.filter { it.cycle == BillingCycle.MONTHLY }
        else -> historyList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = "Calculation History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface
            )
            Text(
                text = "Review your past estimates and comparison logs",
                fontSize = 13.sp,
                color = StitchOnSurfaceVariant
            )
        }

        if (historyList.isNotEmpty()) {
            // Metrics summary cards
            val avgUnits = historyList.map { it.units }.average()
            val avgBill = historyList.map { it.billResult.totalEnergyCharge }.average()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
                    border = BorderStroke(1.dp, StitchOutline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = StitchOnSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AVG. CONSUMPTION", fontSize = 10.sp, color = StitchOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${avgUnits.toInt()} kWh",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                        Text(
                            text = "across ${historyList.size} runs",
                            fontSize = 11.sp,
                            color = StitchOnSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
                    border = BorderStroke(1.dp, StitchOutline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = StitchPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AVG. BILL", fontSize = 10.sp, color = StitchOnSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currencyFormat.format(avgBill),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary
                        )
                        Text(
                            text = "domestic estimate",
                            fontSize = 11.sp,
                            color = StitchOnSurfaceVariant
                        )
                    }
                }
            }

            // Filter rail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Bi-monthly", "Monthly").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val bg = if (isSelected) StitchPrimary else StitchSurfaceContainer
                    val txtColor = if (isSelected) Color.White else StitchOnSurface

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(bg)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = filter,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = txtColor
                        )
                    }
                }
            }

            // Cards list
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredList.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCalculation(item) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
                        border = BorderStroke(1.dp, StitchOutline)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StitchSurfaceLow
                                ) {
                                    Text(
                                        text = "${item.cycle.displayName} • Tamil Nadu",
                                        fontSize = 11.sp,
                                        color = StitchOnSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteItem(item.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete entry",
                                        tint = StitchOnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text("POWER CONSUMED", fontSize = 10.sp, color = StitchOnSurfaceVariant)
                                    Text(
                                        text = "${item.units.toInt()} kWh",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchOnSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("ESTIMATED BILL", fontSize = 10.sp, color = StitchOnSurfaceVariant)
                                    Text(
                                        text = currencyFormat.format(item.billResult.totalEnergyCharge),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = dateFormat.format(Date(item.timestamp)),
                                fontSize = 11.sp,
                                color = StitchOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            // Empty state matching Stitch
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurfaceLowest),
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(StitchSurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = StitchOnSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No Saved Calculations",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Compute an estimated bill to begin tracking your usage trendlines locally.",
                        fontSize = 13.sp,
                        color = StitchOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Local Storage Privacy Reassurance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StitchSurfaceLow)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = StitchOnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Calculations are saved locally in this session for quick comparison.",
                fontSize = 11.sp,
                color = StitchOnSurfaceVariant
            )
        }

        // Calculate New Bill CTA
        Button(
            onClick = onCalculateNew,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = StitchSurfaceLowest,
                contentColor = StitchPrimary
            ),
            border = BorderStroke(1.dp, StitchPrimary)
        ) {
            Icon(
                imageVector = Icons.Default.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Calculate New Bill",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
