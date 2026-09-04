package com.example.feature.home.ui.modules

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun BillCalculatorRoute(
    onNavigateBack: () -> Unit,
    viewModel: BillCalculatorViewModel = viewModel()
) {
    val inputText by viewModel.inputText.collectAsState()
    val billResult by viewModel.billResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EB Bill Calculator", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (billResult != null) {
                        TextButton(onClick = { viewModel.reset() }) {
                            Text("Clear", color = Color(0xFF1E7038))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F7FB),
                    titleContentColor = Color(0xFF1E222D),
                    navigationIconContentColor = Color(0xFF1E222D)
                )
            )
        },
        containerColor = Color(0xFFF5F7FB)
    ) { padding ->
        AnimatedContent(
            targetState = billResult,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            modifier = Modifier.padding(padding),
            label = "calculator_state"
        ) { stateSnapshot ->
            if (stateSnapshot != null) {
                ResultState(
                    result = stateSnapshot,
                    onCalculateAgain = { viewModel.reset() }
                )
            } else {
                InputState(
                    inputText = inputText,
                    onKeyPress = { viewModel.onKeyPress(it) },
                    onCalculate = { viewModel.calculate() }
                )
            }
        }
    }
}

@Composable
fun InputState(
    inputText: String,
    onKeyPress: (String) -> Unit,
    onCalculate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Text("⚡", fontSize = 24.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "EB Bill Calculator",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1E222D)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Calculate your estimated electricity bill based on Tamil Nadu tariff slabs.",
            fontSize = 15.sp,
            color = Color(0xFF7D8492),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Display Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = if (inputText.isEmpty()) "0" else inputText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E222D)
                )
                Text(
                    text = "units",
                    fontSize = 14.sp,
                    color = Color(0xFF7D8492),
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Custom Numeric Keypad
        val keys = listOf(
            "1", "2", "3",
            "4", "5", "6",
            "7", "8", "9",
            ".", "0", "⌫"
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            userScrollEnabled = false
        ) {
            items(keys) { key ->
                KeypadButton(text = key, onClick = { onKeyPress(key) })
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCalculate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E7038),
                disabledContainerColor = Color(0xFFA5D6A7)
            ),
            enabled = inputText.isNotEmpty()
        ) {
            Text(
                text = "Calculate Bill",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun KeypadButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E222D)
        )
    }
}

@Composable
fun ResultState(
    result: BillResult,
    onCalculateAgain: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Estimated EB Bill",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E222D),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                ResultRow("Total Units Consumed", "${result.totalUnits} units", Color(0xFF1E222D))
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F2F5))
                ResultRow("Free Units", "${result.freeUnits} units", Color(0xFF4CAF50))
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F2F5))
                ResultRow("Chargeable Units", "${result.chargeableUnits} units", Color(0xFF1E222D))
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F5E9))
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "Estimated Energy Charge",
                            fontSize = 14.sp,
                            color = Color(0xFF1E7038)
                        )
                        Text(
                            text = currencyFormat.format(result.totalEnergyCharge),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E7038)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Breakdown Section
        Text(
            text = "Bill Breakdown",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E222D)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (result.breakdown.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No chargeable units",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E222D)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your consumption is within the free units limit. No energy charge is applicable.",
                        fontSize = 14.sp,
                        color = Color(0xFF7D8492),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Slab", fontSize = 12.sp, color = Color(0xFF7D8492), modifier = Modifier.weight(1.5f))
                        Text("Units", fontSize = 12.sp, color = Color(0xFF7D8492), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("Rate", fontSize = 12.sp, color = Color(0xFF7D8492), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("Amount", fontSize = 12.sp, color = Color(0xFF7D8492), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F2F5))
                    
                    result.breakdown.forEachIndexed { index, slab ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (slab.toUnit == Double.POSITIVE_INFINITY) "> ${slab.fromUnit.toInt()}" else "${slab.fromUnit.toInt()}–${slab.toUnit.toInt()}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E222D),
                                modifier = Modifier.weight(1.5f)
                            )
                            Text(
                                text = slab.unitsInSlab.toString(),
                                fontSize = 13.sp,
                                color = Color(0xFF1E222D),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "₹${slab.ratePerUnit}",
                                fontSize = 13.sp,
                                color = Color(0xFF1E222D),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = currencyFormat.format(slab.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E222D),
                                modifier = Modifier.weight(1.5f),
                                textAlign = TextAlign.End
                            )
                        }
                        if (index < result.breakdown.size - 1) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF0F2F5), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Disclaimer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF7D8492),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Estimate based on the configured tariff slabs. Actual electricity bills may vary based on applicable charges and billing rules.",
                fontSize = 12.sp,
                color = Color(0xFF7D8492),
                lineHeight = 16.sp
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onCalculateAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E7038)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Calculate Again",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ResultRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = Color(0xFF7D8492)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}
