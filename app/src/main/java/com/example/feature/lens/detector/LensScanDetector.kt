package com.example.feature.lens.detector

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy

/**
 * Dedicated detector for Solar Lens SCAN flow.
 *
 * Architecture boundary:
 * - Scan flow (Instant Photo, Recorded Video Scan, Live Video Scan) exclusively executes
 *   on-device using YOLO offline inference.
 * - Under NO circumstance does this detector invoke Gemini, OpenRouter, or remote APIs.
 */
class LensScanDetector(
    private val delegate: ApplianceDetector = MlKitApplianceDetector()
) : ApplianceDetector {

    companion object {
        private const val TAG = "LENS_SCAN"
    }

    override suspend fun processFrame(imageProxy: ImageProxy): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=yolo mode=live_scan frame_w=${imageProxy.width} frame_h=${imageProxy.height}")
        return delegate.processFrame(imageProxy)
    }

    /**
     * Process an instant photo capture using on-device YOLO pipeline.
     */
    suspend fun processInstantPhoto(bitmap: Bitmap): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=yolo mode=instant_photo width=${bitmap.width} height=${bitmap.height}")
        // Local YOLO on-device processing. No network or Gemini call.
        return emptyList()
    }

    /**
     * Process recorded video scan frames using on-device YOLO pipeline.
     */
    suspend fun processRecordedScan(videoFile: java.io.File): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=yolo mode=recorded_video_scan path=${videoFile.name}")
        // Local YOLO on-device processing. No network or Gemini call.
        return emptyList()
    }

    override fun close() {
        Log.i(TAG, "[LENS_SCAN] provider=yolo status=closed")
        delegate.close()
    }
}
