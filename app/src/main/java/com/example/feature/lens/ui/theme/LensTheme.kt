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
    // Primary Energy Green Tier (#386A20)
    val Primary = Color(0xFF386A20)
    val PrimaryHover = Color(0xFF2D5518)
    val PrimaryLive = Color(0xFF386A20)
    val PrimaryFixed = Color(0xFFE5F2D9)
    val PrimaryFixedDim = Color(0xFFD3E8C5)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnPrimaryFixed = Color(0xFF19380A)
    val PrimaryContainer = Color(0xFF386A20)
    val OnPrimaryContainer = Color(0xFFE5F2D9)

    // Secondary & Text Tier (#191C18, #747873)
    val SlateDark = Color(0xFF191C18)
    val SlateText = Color(0xFF191C18)
    val SlateMuted = Color(0xFF747873)
    val SlateVariant = Color(0xFF747873)
    val SlateCaption = Color(0xFF747873)

    // Standard Color Aliases
    val OnSurface = SlateText
    val OnSurfaceVariant = SlateVariant
    val Secondary = SlateMuted
    val SecondaryFixed = Color(0xFFE5F2D9)
    val SecondaryFixedDim = Color(0xFFD3E8C5)
    val SecondaryContainer = Color(0xFFE5F2D9)

    // Tertiary Cyan/Teal Tier
    val Tertiary = Color(0xFF2D6D79)
    val TertiaryContainer = Color(0xFF1E7888)
    val TertiaryFixed = Color(0xFFB5E7EF)
    val TertiaryFixedDim = Color(0xFF9ACAD2)
    val OnTertiary = Color(0xFFFFFFFF)

    // Surfaces & Spatial Elevation (#FBFCF3, #FFFFFF)
    val Background = Color(0xFFFBFCF3)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF7F9F0)
    val SurfaceContainer = Color(0xFFF1F5E9)
    val SurfaceContainerHigh = Color(0xFFEAF0E1)
    val SurfaceContainerHighest = Color(0xFFE1E9D8)
    val Outline = Color(0xFFDDE4D5)
    val OutlineVariant = Color(0xFFC9D3C0)

    // Dark HUD & Inverted Surfaces
    val InverseSurface = Color(0xFF2E352A)
    val InverseOnSurface = Color(0xFFF1F5E9)

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
