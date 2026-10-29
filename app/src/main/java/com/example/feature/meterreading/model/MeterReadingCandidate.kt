package com.example.feature.meterreading.model

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * Type of meter display detected or processed.
 */
enum class MeterDisplayType {
    DIGITAL_LCD,
    MECHANICAL_ROTATING_WHEEL,
    UNKNOWN
}

/**
 * Status of the OCR extraction and semantic validation.
 */
enum class MeterOcrStatus {
    /** High or good confidence cumulative energy reading identified. */
    DETECTED_SUCCESS,
    /** Reading was extracted, but digits or units require visual user verification. */
    AMBIGUOUS_VERIFY_NEEDED,
    /** Detected value is likely Voltage (V), Current (A), or Maximum Demand (kW), not energy (kWh). */
    NON_KWH_WARNING,
    /** No plausible numeric reading could be extracted from the display region. */
    NO_READING_FOUND,
    /** Image capture or OCR engine failure (e.g. out of memory, hardware error). */
    OCR_ERROR
}

/**
 * Parsed candidate reading extracted from the electricity meter photograph.
 */
data class MeterReadingCandidate(
    val rawText: String,
    val numericValue: Double,
    val displayValue: String, // e.g. "16596.6" or "16596"
    val hasDecimal: Boolean = false,
    val integerPart: Long = 0L,
    val decimalPart: Int? = null,
    val unitDetected: String? = null, // e.g. "kWh"
    val confidenceScore: Float = 0.5f,
    val meterType: MeterDisplayType = MeterDisplayType.UNKNOWN,
    val status: MeterOcrStatus = MeterOcrStatus.DETECTED_SUCCESS,
    val statusMessage: String = "Reading detected — please verify",
    val isMechanicalAmbiguous: Boolean = false,
    val warningNote: String? = null,
    val alternativeCandidate: Double? = null
)

/**
 * Quality metrics evaluated on the captured frame to guide the user.
 */
data class FrameQualityMetrics(
    val isBlurry: Boolean = false,
    val isTooDark: Boolean = false,
    val isTooBright: Boolean = false,
    val warningMessage: String? = null
)

/**
 * Encapsulates the complete result of the OCR detection and validation.
 */
data class MeterScanResult(
    val candidate: MeterReadingCandidate?,
    val croppedBitmap: Bitmap?,
    val fullRawText: String,
    val qualityMetrics: FrameQualityMetrics = FrameQualityMetrics(),
    val extractedBlocks: List<ExtractedTextBlock> = emptyList(),
    val variantRawTexts: Map<String, String> = emptyMap()
)

/**
 * Low-level text block extracted from ML Kit OCR with spatial bounding coordinates.
 */
data class ExtractedTextBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<String> = emptyList()
)
