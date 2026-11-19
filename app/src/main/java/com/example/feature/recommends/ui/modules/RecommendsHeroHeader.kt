package com.example.feature.recommends.ui.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.SolarBrandGlassCardBg
import com.example.ui.theme.SolarBrandGlassCardBorder
import com.example.ui.theme.SolarBrandGreen
import com.example.ui.theme.SolarBrandGreenSecondary
import com.example.ui.theme.SolarBrandTextPrimary
import com.example.ui.theme.SolarBrandTextSecondary

@Composable
fun RecommendsTopBar(
    userAddress: String,
    onProfileClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isDark) MaterialTheme.colorScheme.background else SolarBrandGlassCardBg,
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SolarBrandGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = "SolarEase",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SolarEase",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) MaterialTheme.colorScheme.onBackground else SolarBrandTextPrimary,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = SolarBrandGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = userAddress,
                            fontSize = 11.sp,
                            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .semantics(mergeDescendants = true) {
                        role = Role.Button
                    }
                    .clickable(role = Role.Button) { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SolarBrandGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecommendsHeroHeader(
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = "Efficiency Guidelines",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) MaterialTheme.colorScheme.onBackground else SolarBrandTextPrimary,
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Follow the 9 verified guidelines to reduce electricity consumption.",
            fontSize = 13.sp,
            color = if (isDark) MaterialTheme.colorScheme.onSurfaceVariant else SolarBrandTextSecondary,
            lineHeight = 18.sp
        )
    }
}
