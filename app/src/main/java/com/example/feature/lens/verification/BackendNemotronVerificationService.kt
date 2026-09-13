package com.example.feature.lens.verification

import android.util.Log
import com.example.feature.lens.data.DetectionResult
import com.example.feature.lens.data.LensRepository
import kotlinx.coroutines.CancellationException

/**
 * Backend-mediated appliance verification service for Solar Lens Scan.
 *
 * Dispatches candidate image crops to the existing production backend:
 * POST /lens/analyze -> OpenRouter -> NVIDIA Nemotron
 *
 * Utilizes the existing authenticated Retrofit/OkHttp infrastructure with Firebase ID tokens.
 * Extracts the verified SolarEase appliance category without altering the ML Kit camera-frame bounding box.
 */
class BackendNemotronVerificationService(
    private val repository: LensRepository
) : ApplianceVerificationService {

    companion object {
        private const val TAG = "BackendNemotronVerifier"

        val SUPPORTED_CATEGORIES = setOf(
            "LED Tube Light",
            "Ceiling Fan",
            "Air Conditioner",
            "Refrigerator",
            "Television"
        )
    }

    override suspend fun verifyCandidate(imageCropBytes: ByteArray): Result<VerificationResult> {
        return try {
            val result = repository.analyzeImage(imageCropBytes, "CAMERA")
            result.map { detectionResult ->
                mapDetectionResult(detectionResult)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Backend Nemotron candidate verification failed", e)
            Result.failure(e)
        }
    }

    private fun mapDetectionResult(detectionResult: DetectionResult): VerificationResult {
        if (detectionResult.detections.isEmpty()) {
            return VerificationResult(isVerified = false, label = null, confidence = 0f)
        }

        // Find candidate with matching SolarEase category and highest confidence
        val validDetection = detectionResult.detections
            .mapNotNull { det ->
                val canonicalCategory = normalizeCategory(det.label)
                if (canonicalCategory != null) {
                    Pair(canonicalCategory, det.confidence)
                } else null
            }
            .maxByOrNull { it.second }

        return if (validDetection != null) {
            VerificationResult(
                isVerified = true,
                label = validDetection.first,
                confidence = validDetection.second
            )
        } else {
            VerificationResult(
                isVerified = false,
                label = null,
                confidence = 0f
            )
        }
    }

    private fun normalizeCategory(category: String): String? {
        val lower = category.lowercase().trim()
        return when {
            lower.contains("tube") || lower.contains("led") || lower.contains("light") ->
                "LED Tube Light"
            lower.contains("ceiling") || lower.contains("fan") ->
                "Ceiling Fan"
            lower.contains("air condition") || lower.contains("ac") ->
                "Air Conditioner"
            lower.contains("refrigerator") || lower.contains("fridge") ->
                "Refrigerator"
            lower.contains("television") || lower.contains("tv") ->
                "Television"
            else -> null
        }
    }
}
