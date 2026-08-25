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
    val boundingBox: Rect,
    val label: String,
    val powerEstimateStr: String,
    val powerWatts: Float
)

class ApplianceAnalyzer(
    private val onApplianceDetected: (DetectedAppliance?) -> Unit
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
                    
                    var bestDetection: DetectedAppliance? = null
                    var largestArea = 0

                    for (detectedObject in objects) {
                        val boundingBox = detectedObject.boundingBox
                        val area = boundingBox.width() * boundingBox.height()
                        
                        // We only process large enough objects to save compute
                        if (area > 10000 && area > largestArea) {
                            
                            // 2. Crop the image to the bounding box
                            val bitmap = imageProxy.toBitmap()
                            if (bitmap != null) {
                                // Ensure bounds are within bitmap
                                val safeLeft = boundingBox.left.coerceAtLeast(0)
                                val safeTop = boundingBox.top.coerceAtLeast(0)
                                val safeRight = boundingBox.right.coerceAtMost(bitmap.width)
                                val safeBottom = boundingBox.bottom.coerceAtMost(bitmap.height)
                                
                                if (safeRight > safeLeft && safeBottom > safeTop) {
                                    val croppedBitmap = Bitmap.createBitmap(
                                        bitmap, safeLeft, safeTop, 
                                        safeRight - safeLeft, safeBottom - safeTop
                                    )
                                    
                                    // 3. Label the cropped image
                                    val croppedInput = InputImage.fromBitmap(croppedBitmap, imageProxy.imageInfo.rotationDegrees)
                                    val labels = imageLabeler.process(croppedInput).await()
                                    
                                    val validLabel = labels.firstOrNull { label ->
                                        supportedAppliances.any { it in label.text.lowercase() }
                                    }
                                    
                                    if (validLabel != null) {
                                        val powerWatts = PowerEstimation.estimatePowerForAppliance(validLabel.text)
                                        val powerEstimateStr = PowerEstimation.getDisplayEstimate(validLabel.text)
                                        
                                        largestArea = area
                                        bestDetection = DetectedAppliance(
                                            boundingBox = boundingBox,
                                            label = validLabel.text,
                                            powerEstimateStr = powerEstimateStr,
                                            powerWatts = powerWatts
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    onApplianceDetected(bestDetection)
                    
                } catch (e: Exception) {
                    // Ignore errors during analysis to keep frame rate smooth
                } finally {
                    imageProxy.close()
                }
            }
        } else {
            imageProxy.close()
        }
    }
}
