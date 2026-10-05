package com.example.feature.lens.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * SolarEase Lens — Minimal & Aesthetic Design System
 * Clean, modern, and approachable for residential homeowners.
 */
val GeistFontFamily: FontFamily = FontFamily.Default
val HankenGroteskFontFamily: FontFamily = FontFamily.Default

object LensColors {
    // Primary — Fresh Green
    val Primary = Color(0xFF16A34A)
    val PrimaryHover = Color(0xFF15803D)
    val PrimaryLive = Color(0xFF22C55E)
    val PrimaryFixed = Color(0xFFDCFCE7)
    val PrimaryFixedDim = Color(0xFFBBF7D0)
    val OnPrimary = Color(0xFFFFFFFF)
    val OnPrimaryFixed = Color(0xFF14532D)
    val PrimaryContainer = Color(0xFF16A34A)
    val OnPrimaryContainer = Color(0xFFDCFCE7)

    // Text & Neutral
    val SlateDark = Color(0xFF111827)
    val SlateText = Color(0xFF1F2937)
    val SlateMuted = Color(0xFF6B7280)
    val SlateVariant = Color(0xFF4B5563)
    val SlateCaption = Color(0xFF9CA3AF)

    // Standard aliases
    val OnSurface = SlateText
    val OnSurfaceVariant = SlateVariant
    val Secondary = SlateMuted
    val SecondaryFixed = Color(0xFFE5E7EB)
    val SecondaryFixedDim = Color(0xFFD1D5DB)
    val SecondaryContainer = Color(0xFFF3F4F6)

    // Tertiary — Soft Blue
    val Tertiary = Color(0xFF2563EB)
    val TertiaryContainer = Color(0xFF3B82F6)
    val TertiaryFixed = Color(0xFFDBEAFE)
    val TertiaryFixedDim = Color(0xFF93C5FD)
    val OnTertiary = Color(0xFFFFFFFF)

    // Surfaces — Very light, airy
    val Background = Color(0xFFFAFAFA)
    val Surface = Color(0xFFFAFAFA)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerLow = Color(0xFFF5F7F5)
    val SurfaceContainer = Color(0xFFEFF1EF)
    val SurfaceContainerHigh = Color(0xFFE5E8E5)
    val SurfaceContainerHighest = Color(0xFFDBDEDB)
    val Outline = Color(0xFFE5E7EB)
    val OutlineVariant = Color(0xFFD1D5DB)

    // Dark HUD & Inverted Surfaces (for camera screen)
    val InverseSurface = Color(0xFF1F2937)
    val InverseOnSurface = Color(0xFFF3F4F6)

    // Status
    val Error = Color(0xFFDC2626)
    val ErrorContainer = Color(0xFFFEE2E2)
}

object LensTypography {
    val displayLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.02).sp
    )

    val headlineLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.01).sp
    )

    val headlineMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    )

    val headlineSm = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    )

    val metricValLg = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.02).sp
    )

    val metricValMd = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).sp
    )

    val labelCaps = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )

    val labelSm = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
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
