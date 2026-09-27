package com.example.feature.lens.utils

import android.util.Log
import com.example.feature.lens.data.DetectionResult
import com.example.feature.lens.data.LensRepository

/**
 * Dedicated analyzer for Solar Lens UPLOAD flow.
 *
 * Architecture boundary:
 * - Upload flow (Uploaded Image, Uploaded Video) exclusively delegates to
 *   Google Gemini 3.8 Flash via the SolarEase backend (/lens/analyze).
 * - Under NO circumstance does this analyzer invoke local YOLO or Scan detectors.
 */
class LensUploadAnalyzer(
    private val repository: LensRepository
) {
    companion object {
        private const val TAG = "LENS_UPLOAD"
    }

    suspend fun analyzeImage(
        imageBytes: ByteArray,
        source: String = "UPLOAD_IMAGE"
    ): Result<DetectionResult> {
        Log.i(TAG, "[LENS_UPLOAD] provider=gemini model=gemini-3.8-flash input_type=image size_bytes=${imageBytes.size}")
        return repository.analyzeMedia(imageBytes, "image/jpeg", source)
    }

    suspend fun analyzeVideo(
        videoBytes: ByteArray,
        mimeType: String = "video/mp4",
        source: String = "UPLOAD_VIDEO"
    ): Result<DetectionResult> {
        Log.i(TAG, "[LENS_UPLOAD] provider=gemini model=gemini-3.8-flash input_type=video size_bytes=${videoBytes.size} mime=$mimeType")
        return repository.analyzeMedia(videoBytes, mimeType, source)
    }
}
