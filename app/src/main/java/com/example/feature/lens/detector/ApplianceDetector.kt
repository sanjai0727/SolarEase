package com.example.feature.lens.detector

import android.graphics.Rect
import android.graphics.RectF
import androidx.camera.core.ImageProxy

/**
 * Clean candidate detection model representing a generic object candidate
 * detected on-device by local vision (e.g. ML Kit Object Detection).
 *
 * Notice: This does NOT assign or fabricate a SolarEase appliance category.
 * Local vision detects generic candidate objects, their spatial bounding boxes,
 * and tracking IDs. Semantic identification is handled by the verification tier.
 */
data class DetectedCandidate(
    val trackingId: Int?,
    val boundingBox: Rect,
    val boundingBoxRelative: RectF,
    val label: String,
    val confidence: Float? = null
)

/**
 * Abstraction for on-device real-time appliance/object candidate detection.
 *
 * Decouples CameraX and LensViewModel from specific local vision frameworks
 * (Google ML Kit, LiteRT / TFLite custom models, etc.).
 */
interface ApplianceDetector {
    /**
     * Process an incoming camera frame and return detected candidates.
     * The implementation must handle coordinate space mapping and ensure
     * ImageProxy is closed appropriately.
     */
    suspend fun processFrame(imageProxy: ImageProxy): List<DetectedCandidate>

    /**
     * Process a static bitmap image and return detected candidates.
     */
    suspend fun processBitmap(bitmap: android.graphics.Bitmap): List<DetectedCandidate> = emptyList()

    /**
     * Release detector resources, threads, and models.
     */
    fun close()
}
