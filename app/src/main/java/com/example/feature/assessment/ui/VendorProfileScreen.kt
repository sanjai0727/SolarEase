package com.example.feature.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun VendorProfileRoute(
    vendorViewModel: SolarVendorViewModel,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by vendorViewModel.state.collectAsState()

    VendorProfileScreen(
        vendorName = state.selectedVendorName,
        onNavigateNext = onNavigateNext,
        onNavigateBack = onNavigateBack
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VendorProfileScreen(
    vendorName: String,
    onNavigateNext: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var isBookmarked by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            StitchVendorTopBar(
                title = vendorName.ifBlank { "Kaveri Solar Systems" },
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = StitchSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bookmark Button (48dp minimum touch target)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isBookmarked) StitchPrimaryFixed else StitchSurfaceContainerHigh,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(role = Role.Button) { isBookmarked = !isBookmarked }
                                .semantics {
                                    role = Role.Button
                                    stateDescription = if (isBookmarked) "Saved" else "Not saved"
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Save Vendor",
                                    tint = if (isBookmarked) StitchPrimaryContainer else StitchOnSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Request Estimate Button
                        Button(
                            onClick = onNavigateNext,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StitchPrimary,
                                contentColor = StitchOnPrimary
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "REQUEST ESTIMATE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Header: Vendor Title, Badge, and Experience
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = StitchPrimaryFixed,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SolarPower,
                            contentDescription = null,
                            tint = StitchPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = vendorName.ifBlank { "ABC Solar" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchOnSurface,
                            letterSpacing = 0.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StitchPrimaryFixed
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                                    color = StitchOnPrimaryFixed
                                )
                            }
                        }
                    }

                    Text(
                        text = "Chennai • 8 years experience • 450+ Installations",
                        fontSize = 13.sp,
                        color = StitchSecondary
                    )
                }
            }

            // Card 1: Hero Photographic Card with Stats
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Photographic Hero Image with Badges
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(144.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_stitch_vendor_hero),
                            contentDescription = "Solar array installation",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Gradient overlay with bottom badges
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                        startY = 40f
                                    )
                                )
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = StitchPrimaryFixed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Govt Subsidy Approved",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "PM Surya Ghar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // 3 Columns Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Rating
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = StitchSurfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Grade,
                                        contentDescription = null,
                                        tint = StitchSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "RATING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = "4.8 / 5.0",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                                Text(
                                    text = "MNRE Empaneled",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StitchPrimaryContainer
                                )
                            }
                        }

                        // Turnaround
                        Surface(
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(10.dp),
                            color = StitchSurfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = StitchSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "TURNAROUND",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = "24-48 hrs",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                                Text(
                                    text = "Avg Response",
                                    fontSize = 11.sp,
                                    color = StitchSecondary
                                )
                            }
                        }

                        // Subsidy
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = StitchSurfaceContainerLow
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = StitchSecondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "SUBSIDY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchSecondary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = "MNRE",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                                Text(
                                    text = "PM Surya Ghar",
                                    fontSize = 11.sp,
                                    color = StitchSecondary
                                )
                            }
                        }
                    }

                    // Services Offered
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "SERVICES OFFERED",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchSecondary,
                                letterSpacing = 0.8.sp
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("On-Grid", "Off-Grid", "Hybrid").forEach { s ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = StitchSurfaceContainerLowest,
                                        shadowElevation = 0.5.dp
                                    ) {
                                        Text(
                                            text = s,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = StitchOnSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = StitchSurfaceContainerLowest,
                                    shadowElevation = 0.5.dp
                                ) {
                                    Text(
                                        text = "DCR Panels",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Card 2: Customer Reviews Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customer Feedback (Sample)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                        Text(
                            text = "MNRE Verified Partner",
                            fontSize = 12.sp,
                            color = StitchSecondary
                        )
                    }

                    Text(
                        text = "Representative installation feedback from verified Tamil Nadu residential deployments (BIS IS 19000:2022 disclosure).",
                        fontSize = 11.sp,
                        color = StitchSecondary,
                        lineHeight = 15.sp
                    )

                    // Review 1
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "S. Narayanan",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StitchSurfaceContainerLowest
                                ) {
                                    Text(
                                        text = "5.0 ★",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "“Good installation service and polite workers. Very happy with the solar panels.”",
                                fontSize = 12.5.sp,
                                color = StitchOnSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }

                    // Review 2
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Priya M.",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = StitchSurfaceContainerLowest
                                ) {
                                    Text(
                                        text = "4.8 ★",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "“Helpful team who explained the solar subsidy clearly.”",
                                fontSize = 12.5.sp,
                                color = StitchOnSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }

            // Card 3: Privacy callout
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = StitchSurfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StitchPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Your details remain private and are only shared when you request an estimate.",
                        fontSize = 12.sp,
                        color = StitchSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

