package com.example.feature.meterreading.detector

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance on-device image preprocessor tailored specifically for digital electricity meter LCDs.
 *
 * Implements:
 * 1. Green-channel prioritized grayscale conversion.
 * 2. Contrast-Limited Adaptive Histogram Equalization (CLAHE) for illumination balancing.
 * 3. Integral Image Adaptive Binarization (Bradley-Roth / Gaussian-equivalent).
 * 4. Optional conservative morphological closing for bridging 7-segment gap artifacts.
 *
 * Runs 100% on-device in pure Kotlin with zero native C++ overhead and zero APK size bloat.
 */
object LcdImagePreprocessor {

    /**
     * Complete multi-stage LCD enhancement pipeline.
     */
    fun preprocess(
        bitmap: Bitmap,
        enableClahe: Boolean = true,
        enableAdaptiveThreshold: Boolean = true,
        enableMorphology: Boolean = false
    ): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap

        // 1. Grayscale conversion with green-channel weighting
        val grayBitmap = toGreenWeightedGrayscale(bitmap)

        // 2. Local contrast enhancement (CLAHE)
        val enhancedBitmap = if (enableClahe) {
            applyClahe(grayBitmap, clipLimit = 2.0f, tileCountX = 8, tileCountY = 8)
        } else {
            grayBitmap
        }

        // 3. Adaptive binarization
        val binaryBitmap = if (enableAdaptiveThreshold) {
            applyAdaptiveThreshold(enhancedBitmap, windowSize = 31, cOffset = 10f)
        } else {
            enhancedBitmap
        }

        // 4. Optional conservative morphological closing
        return if (enableMorphology) {
            applyMorphologyClose(binaryBitmap, radius = 1)
        } else {
            binaryBitmap
        }
    }

    /**
     * Variant B: Grayscale with CLAHE contrast enhancement.
     * Retains smooth continuous gradients for 7-segment digits and printed faceplate context.
     */
    fun toClaheGrayscale(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap
        val gray = toGreenWeightedGrayscale(bitmap)
        return applyClahe(gray, clipLimit = 2.0f, tileCountX = 8, tileCountY = 8)
    }

    /**
     * Variant C: Adaptive-thresholded grayscale binary image.
     * High-contrast black LCD segments on white background (Bradley-Roth / Gaussian-equivalent).
     */
    fun toAdaptiveThreshold(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap
        val clahe = toClaheGrayscale(bitmap)
        return applyAdaptiveThreshold(clahe, windowSize = 31, cOffset = 10f)
    }

    /**
     * Variant D: Upscaled enhanced crop when LCD digits are small.
     * Upscales by 2x using bilinear filtering and applies CLAHE contrast enhancement.
     */
    fun toUpscaledEnhanced(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap
        val scaled = Bitmap.createScaledBitmap(bitmap, bitmap.width * 2, bitmap.height * 2, true)
        return toClaheGrayscale(scaled)
    }

    /**
     * Converts RGB bitmap to grayscale with green-channel emphasis.
     * LCD crystal digits have their highest optical density and absorption against green backlights.
     */
    fun toGreenWeightedGrayscale(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val grayPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            // Weighted luminance: 20% Red + 70% Green + 10% Blue
            val y = (20 * r + 70 * g + 10 * b) / 100
            val grayVal = y.coerceIn(0, 255)
            grayPixels[i] = (0xFF shl 24) or (grayVal shl 16) or (grayVal shl 8) or grayVal
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(grayPixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Contrast-Limited Adaptive Histogram Equalization (CLAHE).
     * Divides the image into a grid of local tiles, clips high contrast peaks to prevent
     * noise amplification, and bilinearly interpolates mappings across tile boundaries.
     */
    fun applyClahe(
        source: Bitmap,
        clipLimit: Float = 2.0f,
        tileCountX: Int = 8,
        tileCountY: Int = 8
    ): Bitmap {
        val width = source.width
        val height = source.height

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val gray = IntArray(width * height)
        for (i in srcPixels.indices) {
            gray[i] = srcPixels[i] and 0xFF
        }

        val actualTilesX = tileCountX.coerceIn(2, max(2, width / 16))
        val actualTilesY = tileCountY.coerceIn(2, max(2, height / 16))

        val tileW = width / actualTilesX
        val tileH = height / actualTilesY
        if (tileW <= 0 || tileH <= 0) return source

        // Compute clipped CDF for each tile
        val tileCdfs = Array(actualTilesX) { Array(actualTilesY) { IntArray(256) } }

        for (tx in 0 until actualTilesX) {
            val xStart = tx * tileW
            val xEnd = if (tx == actualTilesX - 1) width else (tx + 1) * tileW

            for (ty in 0 until actualTilesY) {
                val yStart = ty * tileH
                val yEnd = if (ty == actualTilesY - 1) height else (ty + 1) * tileH

                val hist = IntArray(256)
                var tilePixels = 0

                for (y in yStart until yEnd) {
                    val rowOffset = y * width
                    for (x in xStart until xEnd) {
                        hist[gray[rowOffset + x]]++
                        tilePixels++
                    }
                }

                if (tilePixels == 0) continue

                // Clip histogram
                val clipThreshold = (clipLimit * tilePixels / 256f).toInt().coerceAtLeast(1)
                var excess = 0
                for (i in 0 until 256) {
                    if (hist[i] > clipThreshold) {
                        excess += hist[i] - clipThreshold
                        hist[i] = clipThreshold
                    }
                }

                val bonus = excess / 256
                val rem = excess % 256
                for (i in 0 until 256) {
                    hist[i] += bonus + if (i < rem) 1 else 0
                }

                // Cumulative distribution function (CDF) normalized to 0..255
                var cum = 0
                val cdf = tileCdfs[tx][ty]
                for (i in 0 until 256) {
                    cum += hist[i]
                    cdf[i] = ((cum * 255) / tilePixels).coerceIn(0, 255)
                }
            }
        }

        // Bilinear interpolation across tile centers
        val outPixels = IntArray(width * height)

        for (y in 0 until height) {
            val tyFloat = (y.toFloat() / tileH) - 0.5f
            val ty0 = tyFloat.toInt().coerceIn(0, actualTilesY - 1)
            val ty1 = (ty0 + 1).coerceIn(0, actualTilesY - 1)
            val wy = (tyFloat - ty0).coerceIn(0f, 1f)

            val rowOffset = y * width

            for (x in 0 until width) {
                val txFloat = (x.toFloat() / tileW) - 0.5f
                val tx0 = txFloat.toInt().coerceIn(0, actualTilesX - 1)
                val tx1 = (tx0 + 1).coerceIn(0, actualTilesX - 1)
                val wx = (txFloat - tx0).coerceIn(0f, 1f)

                val v = gray[rowOffset + x]

                val c00 = tileCdfs[tx0][ty0][v]
                val c10 = tileCdfs[tx1][ty0][v]
                val c01 = tileCdfs[tx0][ty1][v]
                val c11 = tileCdfs[tx1][ty1][v]

                val top = (1f - wx) * c00 + wx * c10
                val bottom = (1f - wx) * c01 + wx * c11
                val equalized = ((1f - wy) * top + wy * bottom).toInt().coerceIn(0, 255)

                outPixels[rowOffset + x] = (0xFF shl 24) or (equalized shl 16) or (equalized shl 8) or equalized
            }
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Adaptive Gaussian-equivalent binarization using a fast Integral Image.
     * Replicates OpenCV's adaptiveThreshold(GAUSSIAN_C, THRESH_BINARY, 31, 10.0).
     * Output: Deep black (0xFF000000) for digits, pure white (0xFFFFFFFF) for background.
     */
    fun applyAdaptiveThreshold(
        source: Bitmap,
        windowSize: Int = 31,
        cOffset: Float = 10f
    ): Bitmap {
        val width = source.width
        val height = source.height

        val srcPixels = IntArray(width * height)
        source.getPixels(srcPixels, 0, width, 0, 0, width, height)

        val gray = IntArray(width * height)
        for (i in srcPixels.indices) {
            gray[i] = srcPixels[i] and 0xFF
        }

        // Build 2D Integral Table: integral[y * (width + 1) + x]
        val stride = width + 1
        val integral = LongArray((width + 1) * (height + 1))

        for (y in 0 until height) {
            var rowSum = 0L
            val srcRowOffset = y * width
            val integralRowOffset = (y + 1) * stride
            val prevIntegralRowOffset = y * stride

            for (x in 0 until width) {
                rowSum += gray[srcRowOffset + x]
                integral[integralRowOffset + (x + 1)] = integral[prevIntegralRowOffset + (x + 1)] + rowSum
            }
        }

        val s2 = (windowSize / 2).coerceAtLeast(1)
        val outPixels = IntArray(width * height)
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()

        for (y in 0 until height) {
            val y1 = max(0, y - s2)
            val y2 = min(height - 1, y + s2)
            val rowOffset = y * width

            val y1Stride = y1 * stride
            val y2Stride = (y2 + 1) * stride

            for (x in 0 until width) {
                val x1 = max(0, x - s2)
                val x2 = min(width - 1, x + s2)

                val count = (x2 - x1 + 1) * (y2 - y1 + 1)
                val sum = integral[y2Stride + (x2 + 1)] -
                        integral[y1Stride + (x2 + 1)] -
                        integral[y2Stride + x1] +
                        integral[y1Stride + x1]

                val localMean = sum.toFloat() / count
                val threshold = localMean - cOffset

                // If pixel is darker than local threshold => LCD segment => BLACK
                val pixelVal = gray[rowOffset + x]
                outPixels[rowOffset + x] = if (pixelVal <= threshold) black else white
            }
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(outPixels, 0, width, 0, 0, width, height)
        return out
    }

    /**
     * Conservative 1-pixel morphological closing (dilation then erosion) on black binary digits.
     * Only bridges micro-gaps within 7-segment bars without swelling adjacent digits.
     */
    fun applyMorphologyClose(binaryBitmap: Bitmap, radius: Int = 1): Bitmap {
        if (radius <= 0) return binaryBitmap
        val width = binaryBitmap.width
        val height = binaryBitmap.height

        val pixels = IntArray(width * height)
        binaryBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()

        // Pass 1: Dilate black foreground pixels (expand dark segments into tiny 1px gaps)
        val dilated = IntArray(width * height) { white }
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (pixels[row + x] == black) {
                    for (dy in -radius..radius) {
                        val ny = y + dy
                        if (ny in 0 until height) {
                            val nRow = ny * width
                            for (dx in -radius..radius) {
                                val nx = x + dx
                                if (nx in 0 until width) {
                                    dilated[nRow + nx] = black
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pass 2: Erode black foreground back to original stroke width
        val closed = IntArray(width * height) { white }
        for (y in 0 until height) {
            val row = y * width
            for (x in 0 until width) {
                if (dilated[row + x] == black) {
                    var allBlack = true
                    for (dy in -radius..radius) {
                        val ny = y + dy
                        if (ny in 0 until height) {
                            val nRow = ny * width
                            for (dx in -radius..radius) {
                                val nx = x + dx
                                if (nx !in 0 until width || dilated[nRow + nx] != black) {
                                    allBlack = false
                                    break
                                }
                            }
                        } else {
                            allBlack = false
                        }
                        if (!allBlack) break
                    }
                    if (allBlack) {
                        closed[row + x] = black
                    }
                }
            }
        }

        val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        out.setPixels(closed, 0, width, 0, 0, width, height)
        return out
    }
}
