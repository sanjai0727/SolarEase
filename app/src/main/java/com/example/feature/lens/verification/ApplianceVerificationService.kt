package com.example.feature.lens.verification

/**
 * Result of semantic candidate verification by the LLM tier.
 */
data class VerificationResult(
    val isVerified: Boolean,
    val label: String?, // One of the five SolarEase categories if verified
    val confidence: Float
)

/**
 * Provider-agnostic interface for semantic appliance verification.
 * Isolates the LLM provider (Gemini 3.8 Flash, backend-mediated verification, local edge model, etc.)
 * from CameraX, ML Kit detection, tracking, and UI presentation.
 */
interface ApplianceVerificationService {
    /**
     * Verifies whether an image crop of a detected candidate object belongs to
     * one of the five supported SolarEase appliance categories:
     * 1. LED Tube Light
     * 2. Ceiling Fan
     * 3. Air Conditioner
     * 4. Refrigerator
     * 5. Television
     *
     * @param imageCropBytes JPEG-encoded image crop of the candidate object.
     * @return Result containing VerificationResult indicating verification status and label.
     */
    suspend fun verifyCandidate(imageCropBytes: ByteArray): Result<VerificationResult>
}
