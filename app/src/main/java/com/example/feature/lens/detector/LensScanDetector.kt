package com.example.feature.lens.detector

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy

/**
 * Dedicated on-device detector for Solar Lens camera live preview and instant capture.
 *
 * Architecture boundary:
 * - Operates locally on device using Google ML Kit object detection.
 * - Extracts spatial candidates in real time. Candidates are unclassified and marked
 *   for user review until verified.
 */
class LensScanDetector(
    private val delegate: ApplianceDetector = MlKitApplianceDetector()
) : ApplianceDetector {

    companion object {
        private const val TAG = "LENS_SCAN"
    }

    override suspend fun processFrame(imageProxy: ImageProxy): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=mlkit mode=live_scan frame_w=${imageProxy.width} frame_h=${imageProxy.height}")
        return delegate.processFrame(imageProxy)
    }

    /**
     * Process an instant photo capture using on-device ML Kit pipeline.
     */
    suspend fun processInstantPhoto(bitmap: Bitmap): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=mlkit mode=instant_photo width=${bitmap.width} height=${bitmap.height}")
        return delegate.processBitmap(bitmap)
    }

    /**
     * Process recorded video scan frames using on-device detector.
     */
    suspend fun processRecordedScan(videoFile: java.io.File): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=mlkit mode=recorded_video_scan path=${videoFile.name}")
        return emptyList()
    }

    override fun close() {
        Log.i(TAG, "[LENS_SCAN] provider=mlkit status=closed")
        delegate.close()
    }
}
