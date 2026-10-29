package com.example.feature.meterreading.detector

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import com.example.feature.meterreading.model.ExtractedTextBlock
import com.example.feature.meterreading.model.FrameQualityMetrics
import com.example.feature.meterreading.model.MeterOcrStatus
import com.example.feature.meterreading.model.MeterReadingCandidate
import com.example.feature.meterreading.model.MeterScanResult
import com.example.feature.meterreading.parser.TnebReadingParser
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * On-device implementation of [MeterOcrDetector] powered by Google ML Kit Text Recognition.
 *
 * Runs fully on-device without internet access, zero external API keys, and zero cloud uploads.
 */
class MlKitMeterOcrDetector : MeterOcrDetector {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    companion object {
        private const val TAG = "MlKitMeterOcrDetector"
    }

    override suspend fun recognizeText(bitmap: Bitmap, baselineKwh: Double?): MeterScanResult =
        withContext(Dispatchers.Default) {
            val quality = evaluateImageQuality(bitmap)

            try {
                // Variant A: Original color crop
                // Preserves printed label markings ("kWh", "V", "imp/kWh", faceplate context)
                val originalInput = InputImage.fromBitmap(bitmap, 0)
                val originalVisionText = recognizer.process(originalInput).await()
                val originalBlocks = originalVisionText.textBlocks.map { block ->
                    ExtractedTextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox,
                        lines = block.lines.map { it.text }
                    )
                }

                // Variant B: Grayscale with CLAHE contrast enhancement
                // Retains continuous anti-aliased gradient edges for 7-segment digits
                val claheBitmap = LcdImagePreprocessor.toClaheGrayscale(bitmap)
                val claheInput = InputImage.fromBitmap(claheBitmap, 0)
                val claheVisionText = recognizer.process(claheInput).await()
                val claheBlocks = claheVisionText.textBlocks.map { block ->
                    ExtractedTextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox,
                        lines = block.lines.map { it.text }
                    )
                }

                // Variant C: Adaptive-thresholded grayscale binary image
                // High-contrast black LCD segments on white background
                val thresholdBitmap = LcdImagePreprocessor.toAdaptiveThreshold(bitmap)
                val thresholdInput = InputImage.fromBitmap(thresholdBitmap, 0)
                val thresholdVisionText = recognizer.process(thresholdInput).await()
                val thresholdBlocks = thresholdVisionText.textBlocks.map { block ->
                    ExtractedTextBlock(
                        text = block.text,
                        boundingBox = block.boundingBox,
                        lines = block.lines.map { it.text }
                    )
                }

                // Variant D: Upscaled enhanced crop when LCD digits are small (< 240px height)
                val shouldUpscale = bitmap.height in 1..240 && bitmap.width < 900
                val (upscaledVisionText, upscaledBlocks) = if (shouldUpscale) {
                    val upscaledBitmap = LcdImagePreprocessor.toUpscaledEnhanced(bitmap)
                    val upscaledInput = InputImage.fromBitmap(upscaledBitmap, 0)
                    val vt = recognizer.process(upscaledInput).await()
                    val blocks = vt.textBlocks.map { block ->
                        ExtractedTextBlock(
                            text = block.text,
                            boundingBox = block.boundingBox,
                            lines = block.lines.map { it.text }
                        )
                    }
                    Pair(vt, blocks)
                } else {
                    Pair(null, emptyList())
                }

                // Build structured variant result list
                val variantList = mutableListOf(
                    TnebReadingParser.VariantOcrResult("Variant A (Original Color)", originalVisionText.text, originalBlocks),
                    TnebReadingParser.VariantOcrResult("Variant B (Grayscale CLAHE)", claheVisionText.text, claheBlocks),
                    TnebReadingParser.VariantOcrResult("Variant C (Adaptive Threshold)", thresholdVisionText.text, thresholdBlocks)
                )
                if (upscaledVisionText != null) {
                    variantList.add(
                        TnebReadingParser.VariantOcrResult(
                            "Variant D (Upscaled Enhanced)",
                            upscaledVisionText.text,
                            upscaledBlocks
                        )
                    )
                }

                // Compare outputs across variants rather than concatenating them
                val candidate = TnebReadingParser.parseMultiVariant(
                    variants = variantList,
                    baselineKwh = baselineKwh
                )

                val variantRawTexts = variantList.associate { it.variantName to it.rawText }
                val allBlocks = claheBlocks + thresholdBlocks + originalBlocks + upscaledBlocks

                MeterScanResult(
                    candidate = candidate,
                    croppedBitmap = bitmap,
                    fullRawText = claheVisionText.text.ifBlank { thresholdVisionText.text.ifBlank { originalVisionText.text } },
                    qualityMetrics = quality,
                    extractedBlocks = allBlocks,
                    variantRawTexts = variantRawTexts
                )
            } catch (e: Exception) {
                Log.e(TAG, "OCR recognition failed", e)
                val errorCandidate = MeterReadingCandidate(
                    rawText = "",
                    numericValue = 0.0,
                    displayValue = "",
                    status = MeterOcrStatus.OCR_ERROR,
                    statusMessage = "Unable to process display image: ${e.localizedMessage ?: "Unknown error"}",
                    warningNote = e.message
                )
                MeterScanResult(
                    candidate = errorCandidate,
                    croppedBitmap = bitmap,
                    fullRawText = "",
                    qualityMetrics = quality,
                    extractedBlocks = emptyList()
                )
            }
        }

    /**
     * Efficient sampled analysis to detect severe glare or underexposure without slowing down OCR.
     */
    private fun evaluateImageQuality(bitmap: Bitmap): FrameQualityMetrics {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) return FrameQualityMetrics()

        var totalLuma = 0.0
        var sampleCount = 0
        val stepX = (width / 32).coerceAtLeast(1)
        val stepY = (height / 32).coerceAtLeast(1)

        for (y in 0 until height step stepY) {
            for (x in 0 until width step stepX) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val luma = 0.299 * r + 0.587 * g + 0.114 * b
                totalLuma += luma
                sampleCount++
            }
        }

        val avgLuma = if (sampleCount > 0) totalLuma / sampleCount else 128.0
        val isTooDark = avgLuma < 30.0
        val isTooBright = avgLuma > 235.0

        val warning = when {
            isTooDark -> "Display appears very dark. Turn on flashlight or improve ambient lighting."
            isTooBright -> "Direct glare or flash reflection detected on meter window. Tilt phone slightly."
            else -> null
        }

        return FrameQualityMetrics(
            isTooDark = isTooDark,
            isTooBright = isTooBright,
            warningMessage = warning
        )
    }

    override fun close() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close text recognizer", e)
        }
    }
}
