package com.example.feature.lens.detector

import android.graphics.RectF
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.tasks.await

/**
 * On-device local object detector powered by Google ML Kit Object Detection in STREAM_MODE.
 *
 * Responsibilities:
 * - Accepts camera frame from ImageAnalysis.
 * - Extracts spatial bounding boxes and tracking IDs at native camera frame rates.
 * - Maps coordinates to normalized [0.0, 1.0] relative space, accounting for sensor rotation.
 * - Leaves semantic categorization to the verification tier (Gemini 3.8 Flash).
 */
class MlKitApplianceDetector : ApplianceDetector {

    private val detectorOptions = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableMultipleObjects()
        .build()

    private val objectDetector = ObjectDetection.getClient(detectorOptions)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override suspend fun processFrame(imageProxy: ImageProxy): List<DetectedCandidate> {
        val mediaImage = imageProxy.image ?: return emptyList()
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        return try {
            val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
            val mlObjects = objectDetector.process(inputImage).await()

            val isRotated = rotationDegrees == 90 || rotationDegrees == 270
            val rotatedWidth = if (isRotated) imageProxy.height else imageProxy.width
            val rotatedHeight = if (isRotated) imageProxy.width else imageProxy.height

            val candidates = mutableListOf<DetectedCandidate>()

            for (obj in mlObjects) {
                val box = obj.boundingBox
                val width = box.width()
                val height = box.height()

                // Filter out small visual noise (less than 4% of frame dimension)
                if (width < rotatedWidth * 0.05f || height < rotatedHeight * 0.05f) {
                    continue
                }

                val leftRel = (box.left.toFloat() / rotatedWidth).coerceIn(0f, 1f)
                val topRel = (box.top.toFloat() / rotatedHeight).coerceIn(0f, 1f)
                val rightRel = (box.right.toFloat() / rotatedWidth).coerceIn(0f, 1f)
                val bottomRel = (box.bottom.toFloat() / rotatedHeight).coerceIn(0f, 1f)

                if (rightRel > leftRel && bottomRel > topRel) {
                    candidates.add(
                        DetectedCandidate(
                            trackingId = obj.trackingId,
                            boundingBox = box,
                            boundingBoxRelative = RectF(leftRel, topRel, rightRel, bottomRel),
                            confidence = null
                        )
                    )
                }
            }

            candidates
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("MlKitApplianceDetector", "Error detecting objects", e)
            emptyList()
        }
    }

    override fun close() {
        try {
            objectDetector.close()
        } catch (e: Exception) {
            Log.w("MlKitApplianceDetector", "Error closing object detector", e)
        }
    }
}
