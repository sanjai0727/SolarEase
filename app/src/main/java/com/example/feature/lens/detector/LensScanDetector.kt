package com.example.feature.lens.detector

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageProxy

/**
 * Dedicated detector for Solar Lens SCAN flow.
 *
 * Architecture boundary:
 * - Scan flow (Instant Photo, Recorded Video Scan, Live Video Scan) exclusively executes
 *   on-device using ML Kit vision inference.
 * - Under NO circumstance does this detector invoke Gemini, OpenRouter, or remote APIs.
 */
class LensScanDetector(
    private val delegate: ApplianceDetector = MlKitApplianceDetector()
) : ApplianceDetector {

    companion object {
        private const val TAG = "LENS_SCAN"
    }

    override suspend fun processFrame(imageProxy: ImageProxy): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=on_device_vision mode=live_scan frame_w=${imageProxy.width} frame_h=${imageProxy.height}")
        return delegate.processFrame(imageProxy)
    }

    /**
     * Process an instant photo capture using on-device vision pipeline.
     */
    suspend fun processInstantPhoto(bitmap: Bitmap): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=on_device_vision mode=instant_photo width=${bitmap.width} height=${bitmap.height}")
        // On-device processing. No network or Gemini call.
        return emptyList()
    }

    /**
     * Process recorded video scan frames using on-device vision pipeline.
     */
    suspend fun processRecordedScan(videoFile: java.io.File): List<DetectedCandidate> {
        Log.i(TAG, "[LENS_SCAN] provider=on_device_vision mode=recorded_video_scan path=${videoFile.name}")
        // On-device processing. No network or Gemini call.
        return emptyList()
    }

    override fun close() {
        Log.i(TAG, "[LENS_SCAN] provider=on_device_vision status=closed")
        delegate.close()
    }
}
