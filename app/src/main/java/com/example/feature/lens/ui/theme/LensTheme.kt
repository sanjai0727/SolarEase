package com.example.feature.lens.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Precision Telemetry Energy System Design Tokens
 * Replicating the exact color palette, typography, and shape specs from the Stitch UX Flow.
 */
val GeistFontFamily: FontFamily = FontFamily.Default
val HankenGroteskFontFamily: FontFamily = FontFamily.Default

object LensColors {
    // Primary Emerald Tier
    val Primary = Color(0xFF047857)
    val PrimaryHover = Color(0xFF059669)
    val PrimaryLive = Color(0xFF10B981)
    val PrimaryFixed = Color(0xFF97F5CC)
    val PrimaryFixedDim = Color(0xFF7BD8B1)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnPrimaryFixed = Color(0xFF002115)
    val PrimaryContainer = Color(0xFF047857)
    val OnPrimaryContainer = Color(0xFF9FFDD3)

    // Secondary & Industrial Slate Tier
    val SlateDark = Color(0xFF0F172A)
    val SlateText = Color(0xFF0B1C30)
    val SlateMuted = Color(0xFF565E74)
    val SlateVariant = Color(0xFF3E4943)
    val SlateCaption = Color(0xFF64748B)

    // Standard Stitch Color Aliases
    val OnSurface = SlateText
    val OnSurfaceVariant = SlateVariant
    val Secondary = SlateMuted
    val SecondaryFixed = Color(0xFFDAE2FD)
    val SecondaryFixedDim = Color(0xFFBEC6E0)
    val SecondaryContainer = Color(0xFFDAE2FD)

    // Tertiary Cyan Tier
    val Tertiary = Color(0xFF005683)
    val TertiaryContainer = Color(0xFF006FA8)
    val TertiaryFixed = Color(0xFFCCE5FF)
    val TertiaryFixedDim = Color(0xFF93CCFF)
    val OnTertiary = Color(0xFFFFFFFF)

    // Surfaces & Spatial Elevation
    val Background = Color(0xFFF8F9FF)
    val Surface = Color(0xFFF8F9FF)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFEFF4FF)
    val SurfaceContainer = Color(0xFFE5EEFF)
    val SurfaceContainerHigh = Color(0xFFDCE9FF)
    val SurfaceContainerHighest = Color(0xFFD3E4FE)
    val Outline = Color(0xFFE2E8F0)
    val OutlineVariant = Color(0xFFCBD5E1)

    // Dark HUD & Inverted Surfaces
    val InverseSurface = Color(0xFF213145)
    val InverseOnSurface = Color(0xFFEAF1FF)

    // Status & Warnings
    val Error = Color(0xFFBA1A1A)
    val ErrorContainer = Color(0xFFFFDAD6)
}

object LensTypography {
    val displayLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).sp
    )

    val headlineLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.015).sp
    )

    val headlineMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.01).sp
    )

    val headlineSm = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    // Tabular Numerical Telemetry (Geist inspired)
    val metricValLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.03).sp
    )

    val metricValMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.02).sp
    )

    val labelCaps = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.6.sp // +0.06em uppercase
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
