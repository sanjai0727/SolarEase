package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Deprecated(
    message = "Use SolarEaseStepProgressBar instead",
    replaceWith = ReplaceWith("SolarEaseStepProgressBar(currentStep = currentStep, totalSteps = totalSteps, modifier = modifier)")
)
@Composable
fun StepProgress(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    SolarEaseStepProgressBar(
        currentStep = currentStep,
        totalSteps = totalSteps,
        modifier = modifier
    )
}

@Deprecated(
    message = "Use SolarEaseSelectableCard instead",
    replaceWith = ReplaceWith("SolarEaseSelectableCard(selected = selected, onClick = onClick, title = title, subtitle = description, icon = icon, modifier = modifier)")
)
@Composable
fun SelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    description: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    SolarEaseSelectableCard(
        selected = selected,
        onClick = onClick,
        title = title,
        subtitle = description,
        icon = icon,
        modifier = modifier
    )
}

/**
 * Applies an elegant, subtle corner gradient originating from the top-right corner
 * and fading smoothly into the card's existing background color.
 *
 * @param shape The shape to which the gradient is clipped (e.g. RoundedCornerShape(16.dp)).
 * @param tint The starting tint color at the top-right corner (defaults to SolarEase primary green #386A20 with subtle alpha).
 * @param radiusMultiplier Radius relative to component width for controlled corner coverage.
 */
@Composable
fun Modifier.subtleTopRightGradient(
    shape: Shape = RoundedCornerShape(16.dp),
    tint: Color = if (LocalIsDarkTheme.current) Color(0xFF5ED8B0).copy(alpha = 0.14f) else Color(0xFF006850).copy(alpha = 0.12f),
    radiusMultiplier: Float = 0.75f
): Modifier = this
    .clip(shape)
    .drawBehind {
        if (size.width <= 0f || size.height <= 0f) return@drawBehind
        val radius = size.width * radiusMultiplier
        val brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to tint,
                0.5f to tint.copy(alpha = tint.alpha * 0.35f),
                1.0f to Color.Transparent
            ),
            center = Offset(size.width, 0f),
            radius = radius
        )
        drawRect(brush = brush)
    }
