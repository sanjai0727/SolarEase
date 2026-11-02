package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

enum class SolarEaseButtonVariant {
    PRIMARY,
    SECONDARY,
    OUTLINED,
    TEXT,
    TONAL,
    DANGER
}

@Composable
fun SolarEaseButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    variant: SolarEaseButtonVariant = SolarEaseButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isClickable = enabled && !isLoading

    val containerColor: Color
    val contentColor: Color
    val disabledContainerColor: Color
    val disabledContentColor: Color
    val border: BorderStroke?

    when (variant) {
        SolarEaseButtonVariant.PRIMARY -> {
            containerColor = StitchPrimary
            contentColor = StitchOnPrimary
            disabledContainerColor = StitchSurfaceContainerHigh
            disabledContentColor = StitchOnSurfaceVariant.copy(alpha = 0.38f)
            border = null
        }
        SolarEaseButtonVariant.SECONDARY -> {
            containerColor = StitchSecondaryContainer
            contentColor = StitchSecondary
            disabledContainerColor = StitchSurfaceContainerHigh
            disabledContentColor = StitchOnSurfaceVariant.copy(alpha = 0.38f)
            border = null
        }
        SolarEaseButtonVariant.OUTLINED -> {
            containerColor = Color.Transparent
            contentColor = StitchPrimary
            disabledContainerColor = Color.Transparent
            disabledContentColor = StitchOnSurfaceVariant.copy(alpha = 0.38f)
            border = BorderStroke(
                1.dp,
                if (isClickable) StitchPrimary else StitchOutline.copy(alpha = 0.38f)
            )
        }
        SolarEaseButtonVariant.TEXT -> {
            containerColor = Color.Transparent
            contentColor = StitchPrimary
            disabledContainerColor = Color.Transparent
            disabledContentColor = StitchOnSurfaceVariant.copy(alpha = 0.38f)
            border = null
        }
        SolarEaseButtonVariant.TONAL -> {
            containerColor = StitchPrimaryContainer
            contentColor = StitchOnPrimaryContainer
            disabledContainerColor = StitchSurfaceContainerHigh
            disabledContentColor = StitchOnSurfaceVariant.copy(alpha = 0.38f)
            border = null
        }
        SolarEaseButtonVariant.DANGER -> {
            containerColor = SolarEaseSemanticColors.DangerContainer
            contentColor = SolarEaseSemanticColors.DangerText
            disabledContainerColor = StitchSurfaceContainerHigh
            disabledContentColor = StitchOnSurfaceVariant.copy(alpha = 0.38f)
            border = null
        }
    }

    val activeContentColor = if (isClickable) contentColor else disabledContentColor

    Button(
        onClick = { if (isClickable) onClick() },
        enabled = isClickable,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor
        ),
        border = border,
        elevation = if (variant == SolarEaseButtonVariant.PRIMARY && isClickable) {
            ButtonDefaults.buttonElevation(defaultElevation = 1.dp, pressedElevation = 2.dp)
        } else {
            ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        interactionSource = interactionSource,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                if (isLoading) {
                    stateDescription = "Loading"
                }
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = activeContentColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = activeContentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = activeContentColor
            )

            if (!isLoading && trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = activeContentColor
                )
            }
        }
    }
}
