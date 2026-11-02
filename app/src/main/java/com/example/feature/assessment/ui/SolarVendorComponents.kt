package com.example.feature.assessment.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.SolarEaseSelectableCard
import com.example.ui.components.SolarEaseStepProgressBar

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE STITCH UI COMPONENTS FOR SOLAR VENDOR FLOW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StitchVendorTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StitchSurfaceContainerLow,
        border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = StitchOnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = StitchOnSurface,
                    letterSpacing = 0.sp
                )
            }

            // Profile Badge
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(StitchPrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Deprecated(
    message = "Use SolarEaseStepProgressBar instead",
    replaceWith = ReplaceWith("SolarEaseStepProgressBar(currentStep = currentStep, totalSteps = totalSteps, stepTitle = stepTitle, category = category ?: categoryText, percentageText = percentageText, modifier = modifier)")
)
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
    SolarEaseStepProgressBar(
        currentStep = currentStep,
        totalSteps = totalSteps,
        stepTitle = stepTitle,
        category = category ?: categoryText,
        percentageText = percentageText,
        modifier = modifier
    )
}

@Composable
fun StitchHeroIllustration(
    drawableRes: Int,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 280.dp,
    height: Dp = 128.dp
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
            color = if (LocalIsDarkTheme.current) SurfaceVariantDark else Color(0xFFF1F8F5),
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
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = StitchPrimaryContainer,
            contentColor = Color.White,
            disabledContainerColor = StitchSurfaceContainerHighest,
            disabledContentColor = StitchOnSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 0.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp
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
        shape = RoundedCornerShape(14.dp),
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
        shape = RoundedCornerShape(14.dp),
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

@Deprecated(
    message = "Use SolarEaseSelectableCard instead",
    replaceWith = ReplaceWith("SolarEaseSelectableCard(selected = selected, onClick = onClick, title = title, subtitle = subtitle, icon = icon, badgeText = badgeText, minHeight = 64.dp, modifier = modifier)")
)
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
    SolarEaseSelectableCard(
        selected = selected,
        onClick = onClick,
        title = title,
        subtitle = subtitle,
        icon = icon,
        badgeText = badgeText,
        minHeight = 64.dp,
        modifier = modifier
    )
}

