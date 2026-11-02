package com.example.feature.lens.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Precision Telemetry Energy System Design Tokens
 * Replicating the exact color palette, typography, and shape specs from the UX Flow with full dark mode responsiveness.
 */
val GeistFontFamily: FontFamily = FontFamily.Default
val HankenGroteskFontFamily: FontFamily = FontFamily.Default

object LensColors {
    // Static Base Tokens for Auditing and Non-Composable Verification
    val BaseSlateCaption: Color = Color(0xFF4F5A52)
    val BaseSlateMuted: Color = Color(0xFF4F5A52)

    // Primary Emerald Tier
    val Primary: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryDark else Color(0xFF245B3A)
    val PrimaryHover: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryContainerDark else Color(0xFF2E7048)
    val PrimaryLive: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryDark else Color(0xFF3D8A5A)
    val PrimaryFixed: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryContainerDark else Color(0xFFDDEFD9)
    val PrimaryFixedDim: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryContainerDark else Color(0xFFC9E1C5)
    val OnPrimary: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnPrimaryDark else Color(0xFFFFFFFF)
    val OnPrimaryFixed: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnPrimaryContainerDark else Color(0xFF123B27)
    val PrimaryContainer: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) PrimaryContainerDark else Color(0xFF245B3A)
    val OnPrimaryContainer: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnPrimaryContainerDark else Color(0xFFDDEFD9)

    // Secondary & Industrial Slate Tier
    val SlateDark: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFFE2E3DD) else Color(0xFF0F172A)
    val SlateText: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnBackgroundDark else Color(0xFF17201B)
    val SlateMuted: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnSurfaceVariantDark else Color(0xFF4F5A52)
    val SlateVariant: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnSurfaceVariantDark else Color(0xFF4F5A52)
    val SlateCaption: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnSurfaceVariantDark else Color(0xFF4F5A52)

    // Standard Color Aliases
    val OnSurface: Color @Composable @ReadOnlyComposable get() = SlateText
    val OnSurfaceVariant: Color @Composable @ReadOnlyComposable get() = SlateVariant
    val Secondary: Color @Composable @ReadOnlyComposable get() = SlateMuted
    val SecondaryFixed: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SecondaryContainerDark else Color(0xFFE7F2F3)
    val SecondaryFixedDim: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SecondaryContainerDark else Color(0xFFC6E0E2)
    val SecondaryContainer: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SecondaryContainerDark else Color(0xFFE7F2F3)

    // Tertiary Cyan Tier
    val Tertiary: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SecondaryDark else Color(0xFF276A70)
    val TertiaryContainer: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SecondaryContainerDark else Color(0xFF1F7378)
    val TertiaryFixed: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SecondaryContainerDark else Color(0xFFE7F2F3)
    val TertiaryFixedDim: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SecondaryContainerDark else Color(0xFFC6E0E2)
    val OnTertiary: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OnSecondaryDark else Color(0xFFFFFFFF)

    // Surfaces & Spatial Elevation
    val Background: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) BackgroundDark else Color(0xFFF8F8F2)
    val Surface: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SurfaceDark else Color(0xFFF8F8F2)
    val SurfaceContainerLowest: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SurfaceDark else Color(0xFFFFFFFF)
    val SurfaceContainerLow: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFF20251D) else Color(0xFFF4F6F0)
    val SurfaceContainer: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) SurfaceVariantDark else Color(0xFFEEF2E9)
    val SurfaceContainerHigh: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFF292F25) else Color(0xFFE6ECE1)
    val SurfaceContainerHighest: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFF333B2E) else Color(0xFFDDE5D9)
    val Outline: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OutlineDark else Color(0xFFD2DBD1)
    val OutlineVariant: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) OutlineDark.copy(alpha = 0.5f) else Color(0xFFC5CFC3)

    // Dark HUD & Inverted Surfaces
    val InverseSurface: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFFE2E3DD) else Color(0xFF193126)
    val InverseOnSurface: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFF17201B) else Color(0xFFE8F1E6)

    // Status & Warnings
    val Error: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)
    val ErrorContainer: Color @Composable @ReadOnlyComposable get() = if (LocalIsDarkTheme.current) Color(0xFF93000A) else Color(0xFFFFDAD6)
}

object LensTypography {
    val displayLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    )

    val headlineLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    )

    val headlineMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )

    val headlineSm = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )

    // Tabular Numerical Telemetry (Geist inspired)
    val metricValLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp
    )

    val metricValMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )

    val labelCaps = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )

    val labelSm = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val bodyLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )

    val bodyMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}
