package com.example.feature.home.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.session.UserSession
import java.util.Calendar

// ─── Theme Colors matching the reference exactly ────────────────────────────────
private val BrandNavy = Color(0xFF1E222D)
private val BrandGreen = Color(0xFF1E7038)
private val BackgroundColor = Color(0xFFF5F7FB)
private val TextGray = Color(0xFF7D8492)
private val TextDark = Color(0xFF11141C)

// ─── Main ───────────────────────────────────────────────────────────────────────

@Composable
fun HomeRoute(
    onNavigateToBillCalculator: () -> Unit,
    onNavigateToElectricityChatbot: () -> Unit,
    onNavigateToSolarEaseLens: () -> Unit,
    onNavigateToTnebBillStatus: () -> Unit,
    onNavigateToSolarPanel: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSolar: () -> Unit = {}
) {
    val userName = remember {
        UserSession.currentUser?.customerName
            ?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotBlank() }
    }
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val greetingText = if (userName != null) "$greeting, $userName 👋" else "$greeting 👋"
    val userInitial = userName?.firstOrNull()?.uppercaseChar() ?: 'S'

    Scaffold(
        containerColor = BackgroundColor,
        bottomBar = {
            BottomNavigationBar(
                onHome = {},
                onActivity = onNavigateToActivity,
                onSolar = onNavigateToSolar,
                onProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // ── 1. Personal Header ──────────────────────────────────
            HeaderSection(userInitial = userInitial)

            Spacer(modifier = Modifier.height(28.dp))
            
            // ── 2. Greeting ─────────────────────────────────────────
            GreetingSection(greetingText = greetingText)

            Spacer(modifier = Modifier.height(28.dp))

            // ── 3. Energy Overview ──────────────────────────────────
            EnergyOverviewCard()

            Spacer(modifier = Modifier.height(32.dp))

            // ── 4. Quick Electricity Actions ────────────────────────
            SectionLabel("Quick Actions")

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = "Bill Calculator",
                    description = "Estimate your bill",
                    icon = Icons.Default.Calculate,
                    backgroundColor = Color(0xFFEEF7EE),
                    accentColor = Color(0xFF2C7D38),
                    decorationType = DecorationType.LEAVES,
                    onClick = onNavigateToBillCalculator,
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    title = "TNEB Status",
                    description = "Check payment status",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    backgroundColor = Color(0xFFF0F5FF),
                    accentColor = Color(0xFF1976D2),
                    decorationType = DecorationType.WAVES,
                    onClick = onNavigateToTnebBillStatus,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = "Chatbot",
                    description = "Ask about electricity",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    backgroundColor = Color(0xFFE8FAF5),
                    accentColor = Color(0xFF00897B),
                    decorationType = DecorationType.BUBBLES,
                    onClick = onNavigateToElectricityChatbot,
                    modifier = Modifier.weight(1f)
                )
                ActionCard(
                    title = "SolarEase Lens",
                    description = "Scan & analyze",
                    icon = Icons.Default.CameraAlt,
                    backgroundColor = Color(0xFFF3EDFF),
                    accentColor = Color(0xFF7E57C2),
                    decorationType = DecorationType.LENS,
                    onClick = onNavigateToSolarEaseLens,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── 5. Solar Journey ────────────────────────────────────
            SectionLabel("Solar Journey")

            Spacer(modifier = Modifier.height(16.dp))

            SolarJourneyCard(onClick = onNavigateToSolarPanel)

            Spacer(modifier = Modifier.height(40.dp)) // Extra padding for bottom nav
        }
    }
}

// ─── Header ─────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderSection(userInitial: Char) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo representation
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sun/Panel Icon imitation
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Sun
                    drawCircle(
                        color = Color(0xFFFFCA28),
                        radius = size.minDimension / 3,
                        center = Offset(size.width / 2, size.height / 2 - 4.dp.toPx())
                    )
                    // Panel roof
                    val path = Path().apply {
                        moveTo(0f, size.height)
                        lineTo(size.width / 2, size.height / 2 + 2.dp.toPx())
                        lineTo(size.width, size.height)
                        close()
                    }
                    drawPath(path, color = Color(0xFF1976D2))
                    
                    // Leaf green accent
                    drawCircle(
                        color = Color(0xFF4CAF50),
                        radius = 4.dp.toPx(),
                        center = Offset(size.width, size.height / 2 + 4.dp.toPx())
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Wordmark
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = BrandNavy)) {
                        append("Solar")
                    }
                    withStyle(style = SpanStyle(color = BrandGreen)) {
                        append("Ease")
                    }
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        // Avatar
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userInitial.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
        }
    }
}

// ─── Greeting Section ───────────────────────────────────────────────────────────

@Composable
private fun GreetingSection(greetingText: String) {
    Column {
        Text(
            text = greetingText,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = BrandNavy,
            lineHeight = 34.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Your energy, simplified.",
            fontSize = 16.sp,
            color = TextGray
        )
    }
}

// ─── Energy Overview Card ───────────────────────────────────────────────────────

@Composable
private fun EnergyOverviewCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF9E6), // Warm cream
                        Color(0xFFFFECB3)  // Soft yellow
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // Decorative Canvas Background
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            // Sun glow
            drawCircle(
                color = Color(0xFFFFD54F).copy(alpha = 0.4f),
                radius = 120.dp.toPx(),
                center = Offset(width * 0.85f, height * 0.2f)
            )
            // Sun core
            drawCircle(
                color = Color(0xFFFFCA28).copy(alpha = 0.8f),
                radius = 40.dp.toPx(),
                center = Offset(width * 0.85f, height * 0.2f)
            )

            // Soft hills
            val hillPath = Path().apply {
                moveTo(0f, height * 0.7f)
                quadraticBezierTo(width * 0.3f, height * 0.6f, width * 0.6f, height * 0.8f)
                quadraticBezierTo(width * 0.8f, height * 0.9f, width, height * 0.75f)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(hillPath, color = Color(0xFFFFE082).copy(alpha = 0.3f))
            
            // Soft clouds
            drawRoundRect(
                color = Color.White.copy(alpha = 0.6f),
                topLeft = Offset(width * 0.55f, height * 0.35f),
                size = Size(60.dp.toPx(), 16.dp.toPx()),
                cornerRadius = CornerRadius(8.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = 12.dp.toPx(),
                center = Offset(width * 0.55f + 16.dp.toPx(), height * 0.35f + 4.dp.toPx())
            )
        }

        // Content
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color(0xFFF57F17),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Energy Overview",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Stats
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "350",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Units this month",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "₹2,450",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Est. bill",
                        fontSize = 13.sp,
                        color = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFB300), Color(0xFFFF8F00))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly avg. consumption",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Sample",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57C00)
                    )
                }
            }
        }
    }
}

// ─── Section Label ──────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = BrandNavy
    )
}

// ─── Action Card Types ──────────────────────────────────────────────────────────

enum class DecorationType {
    LEAVES, WAVES, BUBBLES, LENS
}

// ─── Action Card ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    accentColor: Color,
    decorationType: DecorationType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "cardScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(160.dp)
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        color = backgroundColor,
        interactionSource = interactionSource,
        shadowElevation = 0.dp // Reference cards are flat/soft
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Decoration
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseColor = accentColor.copy(alpha = 0.05f)
                val w = size.width
                val h = size.height
                
                when (decorationType) {
                    DecorationType.LEAVES -> {
                        drawCircle(color = baseColor, radius = 40.dp.toPx(), center = Offset(w * 0.8f, h * 0.3f))
                        drawCircle(color = baseColor, radius = 25.dp.toPx(), center = Offset(w * 0.9f, h * 0.5f))
                    }
                    DecorationType.WAVES -> {
                        val path = Path().apply {
                            moveTo(w * 0.3f, h)
                            quadraticBezierTo(w * 0.6f, h * 0.6f, w, h * 0.4f)
                            lineTo(w, h)
                            close()
                        }
                        drawPath(path, color = baseColor)
                    }
                    DecorationType.BUBBLES -> {
                        drawCircle(color = baseColor, radius = 30.dp.toPx(), center = Offset(w * 0.7f, h * 0.4f))
                        drawCircle(color = baseColor, radius = 15.dp.toPx(), center = Offset(w * 0.9f, h * 0.6f))
                    }
                    DecorationType.LENS -> {
                        drawCircle(color = baseColor, radius = 45.dp.toPx(), center = Offset(w * 0.8f, h * 0.4f))
                        drawCircle(color = Color.Transparent, radius = 35.dp.toPx(), center = Offset(w * 0.8f, h * 0.4f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Text and Chevron
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = TextGray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Solar Journey Hero Card ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolarJourneyCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ctaScale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .scale(scale),
        shape = RoundedCornerShape(24.dp),
        interactionSource = interactionSource,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF9FBE7), // Light yellow
                            Color(0xFFE8F5E9)  // Light green
                        )
                    )
                )
        ) {
            // Decorative Illustration (Right side)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Sun
                drawCircle(
                    color = Color(0xFFFFF59D),
                    radius = 35.dp.toPx(),
                    center = Offset(w * 0.85f, h * 0.25f)
                )
                
                // Distant hills
                drawCircle(
                    color = Color(0xFFC8E6C9),
                    radius = 80.dp.toPx(),
                    center = Offset(w * 0.7f, h)
                )
                drawCircle(
                    color = Color(0xFFA5D6A7),
                    radius = 100.dp.toPx(),
                    center = Offset(w, h * 0.9f)
                )

                // Birds
                val birdPath = Path().apply {
                    moveTo(w * 0.5f, h * 0.15f)
                    quadraticBezierTo(w * 0.52f, h * 0.12f, w * 0.54f, h * 0.15f)
                    quadraticBezierTo(w * 0.56f, h * 0.12f, w * 0.58f, h * 0.15f)
                }
                drawPath(birdPath, color = Color.Gray, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f))

                // Stylized House Body
                drawRect(
                    color = Color(0xFFECEFF1),
                    topLeft = Offset(w * 0.6f, h * 0.6f),
                    size = Size(w * 0.25f, h * 0.4f)
                )
                // Door
                drawRect(
                    color = Color(0xFF795548),
                    topLeft = Offset(w * 0.7f, h * 0.75f),
                    size = Size(w * 0.05f, h * 0.25f)
                )
                // Window
                drawRect(
                    color = Color(0xFF90CAF9),
                    topLeft = Offset(w * 0.62f, h * 0.7f),
                    size = Size(w * 0.04f, h * 0.1f)
                )
                drawRect(
                    color = Color(0xFF90CAF9),
                    topLeft = Offset(w * 0.78f, h * 0.7f),
                    size = Size(w * 0.04f, h * 0.1f)
                )
                // Solar Roof (Blue parallelogram)
                val roofPath = Path().apply {
                    moveTo(w * 0.55f, h * 0.6f)
                    lineTo(w * 0.65f, h * 0.45f)
                    lineTo(w * 0.9f, h * 0.45f)
                    lineTo(w * 0.85f, h * 0.6f)
                    close()
                }
                drawPath(roofPath, color = Color(0xFF1565C0))
                
                // Panel Grid Lines
                drawLine(color = Color.White.copy(alpha=0.4f), start = Offset(w * 0.6f, h * 0.52f), end = Offset(w * 0.87f, h * 0.52f), strokeWidth = 2f)
                drawLine(color = Color.White.copy(alpha=0.4f), start = Offset(w * 0.65f, h * 0.6f), end = Offset(w * 0.72f, h * 0.45f), strokeWidth = 2f)
                drawLine(color = Color.White.copy(alpha=0.4f), start = Offset(w * 0.75f, h * 0.6f), end = Offset(w * 0.82f, h * 0.45f), strokeWidth = 2f)
            }

            // Content (Left side)
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.55f)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ready to explore solar?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Discover how solar energy can reduce your electricity costs.",
                    fontSize = 12.sp,
                    color = BrandNavy.copy(alpha = 0.8f),
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BrandGreen)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Explore Solar",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Floating Bottom Navigation ─────────────────────────────────────────────────

@Composable
private fun BottomNavigationBar(
    onHome: () -> Unit,
    onActivity: () -> Unit,
    onSolar: () -> Unit,
    onProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp), // Floating spacing
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 8.dp, // Premium shadow
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = true,
                    onClick = onHome
                )
                BottomNavItem(
                    icon = Icons.Default.PieChart,
                    label = "Activity",
                    isSelected = false,
                    onClick = onActivity
                )
                BottomNavItem(
                    icon = Icons.Default.WbSunny,
                    label = "Solar",
                    isSelected = false,
                    onClick = onSolar
                )
                BottomNavItem(
                    icon = Icons.Default.Person,
                    label = "Profile",
                    isSelected = false,
                    onClick = onProfile
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = if (isSelected) BrandGreen else Color(0xFFB0B5C2)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Selection Indicator
        Box(
            modifier = Modifier
                .width(16.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(if (isSelected) BrandGreen else Color.Transparent)
        )
    }
}
