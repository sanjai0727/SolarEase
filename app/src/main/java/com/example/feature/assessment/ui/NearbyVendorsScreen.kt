package com.example.feature.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class StitchVendorItem(
    val id: String,
    val name: String,
    val rating: Double,
    val reviewCount: Int,
    val distance: String,
    val chips: List<String>
)

@Composable
fun NearbyVendorsRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    NearbyVendorsScreen(
        capacityKw = state.recommendedCapacityKw,
        propertyType = state.propertyType,
        systemType = state.systemType,
        mountType = state.mountType,
        panelType = if (state.panelType.contains("Non-DCR")) "Non-DCR" else "DCR",
        onSelectVendor = { vendorId, vendorName ->
            vendorViewModel.selectVendor(vendorId, vendorName)
            onNavigateNext(vendorId)
        },
        onNavigateBack = onNavigateBack
    )
}

@Composable
fun NearbyVendorsScreen(
    capacityKw: Double,
    propertyType: String,
    systemType: String,
    mountType: String,
    panelType: String,
    onSelectVendor: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val vendors = remember {
        listOf(
            StitchVendorItem(
                id = "abc_solar",
                name = "ABC Solar",
                rating = 4.8,
                reviewCount = 126,
                distance = "4.2 km away",
                chips = listOf("On-Grid", "Rooftop", "DCR")
            ),
            StitchVendorItem(
                id = "sunvolt_power",
                name = "SunVolt Power",
                rating = 4.9,
                reviewCount = 84,
                distance = "6.1 km away",
                chips = listOf("On-Grid", "Rooftop", "DCR")
            ),
            StitchVendorItem(
                id = "greenray_solar",
                name = "GreenRay Solar",
                rating = 4.7,
                reviewCount = 95,
                distance = "7.8 km away",
                chips = listOf("On-Grid", "Rooftop", "DCR")
            )
        )
    }

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            StitchVendorTopBar(
                title = "Vendor Discovery",
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Solar vendors near you",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = "Choose a trusted vendor to request an estimate.",
                        fontSize = 15.sp,
                        color = StitchSecondary
                    )
                }
            }

            // Requirement Matched Filter Banner
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = StitchSurfaceContainerHigh,
                    shadowElevation = 0.5.dp
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = StitchPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = StitchOnPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REQUIREMENT MATCHED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = StitchPrimaryContainer,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${String.format("%.1f", capacityKw)} kW • $propertyType • $systemType • $mountType • $panelType",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StitchOnSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Vendor Cards (Exact Stitch layout)
            items(vendors) { vendor ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = StitchSurfaceContainerLowest,
                    border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
                    shadowElevation = 0.5.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header row: Name, rating, distance & verified pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = vendor.name,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchOnSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = StitchPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${vendor.rating}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer
                                    )
                                    Text(
                                        text = "(${vendor.reviewCount} reviews)",
                                        fontSize = 12.sp,
                                        color = StitchSecondary
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 12.sp,
                                        color = StitchSecondary
                                    )
                                    Text(
                                        text = vendor.distance,
                                        fontSize = 12.sp,
                                        color = StitchSecondary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = StitchPrimaryContainer.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = StitchPrimaryContainer,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "Verified Vendor",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer
                                    )
                                }
                            }
                        }

                        // Tags row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            vendor.chips.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StitchSurfaceContainerHigh
                                ) {
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StitchSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        // View Profile Button
                        Button(
                            onClick = { onSelectVendor(vendor.id, vendor.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StitchPrimary,
                                contentColor = StitchOnPrimary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "View Profile",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom trust card
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = StitchPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "All vendors are licensed and certified for TNEB grid-tied installations.",
                        fontSize = 12.sp,
                        color = StitchSecondary
                    )
                }
            }
        }
    }
}

