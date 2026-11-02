package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SolarEaseCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = StitchSurfaceContainerLowest,
        contentColor = StitchOnSurface
    ),
    border: BorderStroke? = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content
    )
}

@Composable
fun SolarEaseElevatedCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    elevation: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = StitchSurfaceContainerLow,
            contentColor = StitchOnSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        content = content
    )
}

@Composable
fun SolarEaseOutlinedCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    border: BorderStroke = BorderStroke(1.dp, StitchOutlineVariant),
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = modifier,
        shape = shape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = StitchSurface,
            contentColor = StitchOnSurface
        ),
        border = border,
        content = content
    )
}

@Composable
fun SolarEaseSelectableCard(
    selected: Boolean,
    onClick: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    badgeText: String? = null,
    enabled: Boolean = true,
    selectionRole: Role = Role.RadioButton,
    minHeight: Dp = 56.dp
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = selectionRole,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = selectionRole
                this.selected = selected
                stateDescription = if (selected) "Selected" else "Not selected"
            },
        shape = RoundedCornerShape(16.dp),
        color = if (selected) StitchSurfaceContainerLow else StitchSurfaceContainerLowest,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) StitchPrimary else StitchOutline.copy(alpha = 0.5f)
        ),
        shadowElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) StitchPrimaryFixed.copy(alpha = 0.45f)
                                else StitchSurfaceContainerLow
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) StitchPrimary else StitchSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StitchPrimaryFixed.copy(alpha = 0.45f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) StitchPrimary else StitchOnSurface
                    )

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (selectionRole == Role.Checkbox) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = StitchPrimary,
                        uncheckedColor = StitchOutline
                    )
                )
            } else {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = StitchPrimary,
                        unselectedColor = StitchOutline
                    )
                )
            }
        }
    }
}
