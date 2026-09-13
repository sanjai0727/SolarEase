package com.example.feature.lens.utils

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class DetectedAppliance(
    val trackingId: Int?,
    val boundingBox: Rect,
    val boundingBoxRelative: android.graphics.RectF,
    val label: String,
    val powerEstimateStr: String,
    val powerWatts: Float,
    val isVerified: Boolean = false,
    val isVerifying: Boolean = false
)

@Deprecated("Replaced by still-frame Camera Scan with OpenRouter/Nemotron AI analysis.")
class ApplianceAnalyzer(
    private val onAppliancesDetected: (List<DetectedAppliance>) -> Unit
) : ImageAnalysis.Analyzer {

    // Use single image mode to ensure we process one frame at a time and avoid backlog
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .build()
    )

    private val imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
    private val scope = CoroutineScope(Dispatchers.Default)

    // Allowed categories for SolarEase Lens
    private val supportedAppliances = setOf(
        "air conditioner", "refrigerator", "television", "tv", "washing machine",
        "ceiling fan", "fan", "water heater", "microwave", "oven", "induction", "stove",
        "mixer", "grinder", "water pump", "computer", "laptop", "iron", "kettle", "cooler"
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            scope.launch {
                try {
                    // 1. Detect objects to get bounding boxes
                    val objects = objectDetector.process(image).await()
                    
                    val detections = mutableListOf<DetectedAppliance>()
                    
                    val rawBitmap = imageProxy.toBitmap()
                    val rotatedBitmap = if (rawBitmap != null) {
                        val matrix = Matrix().apply { postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else null

                    if (rotatedBitmap != null) {
                        for (detectedObject in objects) {
                            val boundingBox = detectedObject.boundingBox
                            val trackingId = detectedObject.trackingId
                            val area = boundingBox.width() * boundingBox.height()
                            
                            // Calculate relative bounding box
                            val leftRel = boundingBox.left.toFloat() / rotatedBitmap.width
                            val topRel = boundingBox.top.toFloat() / rotatedBitmap.height
                            val rightRel = boundingBox.right.toFloat() / rotatedBitmap.width
                            val bottomRel = boundingBox.bottom.toFloat() / rotatedBitmap.height
                            val boundingBoxRelative = android.graphics.RectF(leftRel, topRel, rightRel, bottomRel)
                            
                            // We only process large enough objects to save compute
                            if (area > 10000) {
                                // Ensure bounds are within bitmap
                                val safeLeft = boundingBox.left.coerceAtLeast(0)
                                val safeTop = boundingBox.top.coerceAtLeast(0)
                                val safeRight = boundingBox.right.coerceAtMost(rotatedBitmap.width)
                                val safeBottom = boundingBox.bottom.coerceAtMost(rotatedBitmap.height)
                                
                                if (safeRight > safeLeft && safeBottom > safeTop) {
                                    val croppedBitmap = Bitmap.createBitmap(
                                        rotatedBitmap, safeLeft, safeTop, 
                                        safeRight - safeLeft, safeBottom - safeTop
                                    )
                                    
                                    // 3. Label the cropped image (rotation is 0 since we manually rotated the bitmap)
                                    val croppedInput = InputImage.fromBitmap(croppedBitmap, 0)
                                    val labels = imageLabeler.process(croppedInput).await()
                                    
                                    val validLabel = labels.firstOrNull { label ->
                                        supportedAppliances.any { it in label.text.lowercase() }
                                    }
                                    
                                    if (validLabel != null) {
                                        val powerWatts = ApplianceMaster.getDefaultWattage(validLabel.text)
                                        val powerEstimateStr = if (powerWatts > 0f) "Est. ~${powerWatts.toInt()} W" else "Power Unknown"
                                        
                                        detections.add(
                                            DetectedAppliance(
                                                trackingId = trackingId,
                                                boundingBox = boundingBox,
                                                boundingBoxRelative = boundingBoxRelative,
                                                label = validLabel.text,
                                                powerEstimateStr = powerEstimateStr,
                                                powerWatts = powerWatts
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    onAppliancesDetected(detections)
                    
                } catch (e: Exception) {
                    android.util.Log.e("ApplianceAnalyzer", "Error analyzing image", e)
                } finally {
                    imageProxy.close()
                }
            }
        } else {
            imageProxy.close()
        }
    }
}
