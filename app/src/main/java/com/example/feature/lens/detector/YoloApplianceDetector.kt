package com.example.feature.lens.detector

import androidx.camera.core.ImageProxy
import android.util.Log

/**
 * Skeleton implementation for the future YOLO-based appliance detector.
 *
 * Responsibilities:
 * - Load trained YOLO model.
 * - Perform on-device, offline inference.
 * - Return DetectedCandidate with appliance label and confidence.
 */
class YoloApplianceDetector : ApplianceDetector {

    override suspend fun processFrame(imageProxy: ImageProxy): List<DetectedCandidate> {
        // TODO: Implement YOLO inference here once the model is available.
        // For now, this is just a placeholder to prepare the architecture.
        imageProxy.close()
        return emptyList()
    }

    override fun close() {
        // TODO: Release YOLO model resources.
        Log.d("YoloApplianceDetector", "Detector closed")
    }
}
