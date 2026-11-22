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



@Composable
fun BillCalculatorResultView(
    result: BillResult,
    billingCycle: BillingCycle,
    onRecalculate: () -> Unit,
    onNavigateToSolarLens: () -> Unit,
    onNavigateToHistory: () -> Unit
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
            )

            {
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
                            fontWeight = FontWeight.Medium,
                            color = StitchOnSurface,
                            letterSpacing = 0.sp
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

        // Energy Conservation & Tariff Slab Insight Card
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
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = StitchPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Optimize Your Electricity Bill",
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
                            text = "Tariff Tips",
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
                            text = "ENERGY CONSERVATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Keep your bi-monthly usage under threshold slab tiers (e.g., 200 kWh, 400 kWh, 500 kWh) to avoid steeper unit rates under TNEB LT-1A.",
                            fontSize = 12.sp,
                            color = StitchOnSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Efficiency banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceLow)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = null,
                        tint = StitchPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shift heavy laundry and water pumping away from peak domestic hours.",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Actual bill savings depend on appliance wattage, operating hours, and adherence to lower tariff slab brackets.",
                    fontSize = 10.sp,
                    color = StitchOnSurfaceVariant,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Primary CTA to Appliance Audit
                Button(
                    onClick = onNavigateToSolarLens,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StitchPrimary,
                        contentColor = StitchOnPrimary
                    )
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Audit Connected Appliances",
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

        // View History button
        OutlinedButton(
            onClick = onNavigateToHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, StitchOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchOnSurface)
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Calculation History", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

