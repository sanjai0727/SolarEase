package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

enum class SolarEaseNavTab {
    HOME,
    ANALYTICS,
    RECOMMENDS,
    PROFILE
}

@Composable
fun SolarEaseTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StitchSurface,
        border = BorderStroke(1.dp, StitchOutline.copy(alpha = 0.5f)),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { role = Role.Button }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = StitchOnSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            actions()
        }
    }
}

@Composable
fun SolarEaseBottomNavigation(
    currentTab: SolarEaseNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SolarEaseBottomNavigationInternal(
        currentTab = currentTab,
        onNavigateToHome = onNavigateToHome,
        onNavigateToRecommends = onNavigateToRecommends,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToAnalytics = onNavigateToAnalytics,
        modifier = modifier
    )
}

/**
 * 5-parameter overload for backward compatibility with existing call sites.
 */
@JvmName("SolarEaseBottomNavigationWithAnalytics")
@Composable
fun SolarEaseBottomNavigation(
    currentTab: SolarEaseNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    SolarEaseBottomNavigationInternal(
        currentTab = currentTab,
        onNavigateToHome = onNavigateToHome,
        onNavigateToRecommends = onNavigateToRecommends,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToAnalytics = onNavigateToAnalytics,
        modifier = modifier
    )
}

@Composable
private fun SolarEaseBottomNavigationInternal(
    currentTab: SolarEaseNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: (() -> Unit)?,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StitchSurface.copy(alpha = 0.98f),
        border = BorderStroke(0.8.dp, StitchOutlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SolarEaseNavItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                isSelected = currentTab == SolarEaseNavTab.HOME,
                onClick = onNavigateToHome,
                modifier = Modifier.weight(1f)
            )

            if (onNavigateToAnalytics != null) {
                SolarEaseNavItem(
                    icon = Icons.Outlined.Analytics,
                    label = "Analytics",
                    isSelected = currentTab == SolarEaseNavTab.ANALYTICS,
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier.weight(1f)
                )
            }

            SolarEaseNavItem(
                icon = Icons.Outlined.Lightbulb,
                label = "Recommends",
                isSelected = currentTab == SolarEaseNavTab.RECOMMENDS,
                onClick = onNavigateToRecommends,
                modifier = Modifier.weight(1f)
            )

            SolarEaseNavItem(
                icon = Icons.Outlined.AccountCircle,
                label = "Profile",
                isSelected = currentTab == SolarEaseNavTab.PROFILE,
                onClick = onNavigateToProfile,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Adaptive vertical Navigation Rail for Medium/Expanded windows (tablets, foldables, landscape).
 */
@Composable
fun SolarEaseNavigationRail(
    currentTab: SolarEaseNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SolarEaseNavigationRailInternal(
        currentTab = currentTab,
        onNavigateToHome = onNavigateToHome,
        onNavigateToRecommends = onNavigateToRecommends,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToAnalytics = onNavigateToAnalytics,
        modifier = modifier
    )
}

/**
 * 5-parameter overload for SolarEaseNavigationRail for backward compatibility.
 */
@JvmName("SolarEaseNavigationRailWithAnalytics")
@Composable
fun SolarEaseNavigationRail(
    currentTab: SolarEaseNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    SolarEaseNavigationRailInternal(
        currentTab = currentTab,
        onNavigateToHome = onNavigateToHome,
        onNavigateToRecommends = onNavigateToRecommends,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToAnalytics = onNavigateToAnalytics,
        modifier = modifier
    )
}

@Composable
private fun SolarEaseNavigationRailInternal(
    currentTab: SolarEaseNavTab,
    onNavigateToHome: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: (() -> Unit)?,
    modifier: Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .widthIn(min = 88.dp, max = 120.dp),
        color = StitchSurface,
        border = BorderStroke(1.dp, StitchOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 24.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            SolarEaseNavItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                isSelected = currentTab == SolarEaseNavTab.HOME,
                onClick = onNavigateToHome
            )

            if (onNavigateToAnalytics != null) {
                SolarEaseNavItem(
                    icon = Icons.Outlined.Analytics,
                    label = "Analytics",
                    isSelected = currentTab == SolarEaseNavTab.ANALYTICS,
                    onClick = onNavigateToAnalytics
                )
            }

            SolarEaseNavItem(
                icon = Icons.Outlined.Lightbulb,
                label = "Recommends",
                isSelected = currentTab == SolarEaseNavTab.RECOMMENDS,
                onClick = onNavigateToRecommends
            )

            SolarEaseNavItem(
                icon = Icons.Outlined.AccountCircle,
                label = "Profile",
                isSelected = currentTab == SolarEaseNavTab.PROFILE,
                onClick = onNavigateToProfile
            )
        }
    }
}

@Composable
private fun SolarEaseNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    val isDark = LocalIsDarkTheme.current
    val activeBrandColor = if (isDark) StitchPrimary else SolarJade
    val activeContentColor = if (isDark) StitchOnPrimary else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics(mergeDescendants = true) {
                role = Role.Tab
                selected = isSelected
                stateDescription = if (isSelected) "Selected" else "Not selected"
            }
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(if (isSelected) activeBrandColor else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) activeContentColor else StitchOnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeBrandColor else StitchOnSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
