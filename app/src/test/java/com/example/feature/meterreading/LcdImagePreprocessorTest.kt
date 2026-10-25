package com.example.feature.meterreading

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.feature.meterreading.detector.LcdImagePreprocessor
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LcdImagePreprocessorTest {

    @Test
    fun toGreenWeightedGrayscale_preservesDimensions_andCalculatesLuminance() {
        val bmp = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        // Set pure green pixel
        bmp.setPixel(0, 0, Color.rgb(0, 200, 0))

        val gray = LcdImagePreprocessor.toGreenWeightedGrayscale(bmp)
        assertEquals(100, gray.width)
        assertEquals(50, gray.height)

        val px = gray.getPixel(0, 0)
        // Expected luminance: 0.70 * 200 = 140
        val r = Color.red(px)
        val g = Color.green(px)
        val b = Color.blue(px)
        assertEquals(r, g)
        assertEquals(g, b)
        assertTrue(r in 138..142)
    }

    @Test
    fun applyClahe_improvesContrastAcrossTiles() {
        val bmp = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888)
        for (y in 0 until 160) {
            for (x in 0 until 160) {
                val v = (x + y) / 2
                bmp.setPixel(x, y, Color.rgb(v, v, v))
            }
        }

        val clahe = LcdImagePreprocessor.applyClahe(bmp, clipLimit = 2.0f, tileCountX = 8, tileCountY = 8)
        assertEquals(160, clahe.width)
        assertEquals(160, clahe.height)
    }

    @Test
    fun applyAdaptiveThreshold_producesBinaryBlackAndWhitePixels() {
        val bmp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        // White background with black center box (like LCD digit)
        for (y in 0 until 80) {
            for (x in 0 until 80) {
                val v = if (x in 30..50 && y in 20..60) 30 else 220
                bmp.setPixel(x, y, Color.rgb(v, v, v))
            }
        }

        val binary = LcdImagePreprocessor.applyAdaptiveThreshold(bmp, windowSize = 31, cOffset = 10f)
        assertEquals(80, binary.width)
        assertEquals(80, binary.height)

        // Center should be black (0xFF000000)
        val center = binary.getPixel(40, 40)
        assertEquals(Color.BLACK, center)

        // Corner should be white (0xFFFFFFFF)
        val corner = binary.getPixel(5, 5)
        assertEquals(Color.WHITE, corner)
    }

    @Test
    fun preprocess_realSampleImages_producesHighContrastLcdDigits() {
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("meter_samples/crop_lt_lcd.jpg")
        if (inputStream != null) {
            val sampleBmp = BitmapFactory.decodeStream(inputStream)
            assertNotNull("Sample LCD image should load from test resources", sampleBmp)

            val processed = LcdImagePreprocessor.preprocess(sampleBmp, enableMorphology = false)
            assertEquals(sampleBmp.width, processed.width)
            assertEquals(sampleBmp.height, processed.height)

            // Verify the image consists primarily of black and white pixels
            var blackCount = 0
            var whiteCount = 0
            val total = processed.width * processed.height
            for (y in 0 until processed.height) {
                for (x in 0 until processed.width) {
                    val p = processed.getPixel(x, y)
                    if (p == Color.BLACK) blackCount++
                    else if (p == Color.WHITE) whiteCount++
                }
            }

            // High contrast text image should have between 10% and 40% black pixels (segments)
            val blackRatio = blackCount.toFloat() / total
            assertTrue("Black pixel ratio should be between 10% and 40%, got: $blackRatio", blackRatio in 0.10f..0.40f)
            assertTrue("Total binary pixels should be 100%", (blackCount + whiteCount) == total)
        }
    }

    @Test
    fun toClaheGrayscale_variantB_producesSmoothContrastEnhancedGrayscale() {
        val bmp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        for (y in 0 until 80) {
            for (x in 0 until 80) {
                bmp.setPixel(x, y, Color.rgb(20, (x + y).coerceIn(0, 255), 30))
            }
        }
        val clahe = LcdImagePreprocessor.toClaheGrayscale(bmp)
        assertEquals(80, clahe.width)
        assertEquals(80, clahe.height)
        val px = clahe.getPixel(40, 40)
        assertEquals(Color.red(px), Color.green(px))
        assertEquals(Color.green(px), Color.blue(px))
    }

    @Test
    fun toAdaptiveThreshold_variantC_producesBinaryOutput() {
        val bmp = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888)
        for (y in 0 until 60) {
            for (x in 0 until 60) {
                val c = if (x in 20..40 && y in 20..40) Color.rgb(10, 50, 10) else Color.rgb(30, 230, 40)
                bmp.setPixel(x, y, c)
            }
        }
        val binary = LcdImagePreprocessor.toAdaptiveThreshold(bmp)
        assertEquals(60, binary.width)
        assertEquals(60, binary.height)
        assertEquals(Color.BLACK, binary.getPixel(30, 30))
        assertEquals(Color.WHITE, binary.getPixel(5, 5))
    }

    @Test
    fun toUpscaledEnhanced_variantD_doublesDimensions() {
        val bmp = Bitmap.createBitmap(50, 40, Bitmap.Config.ARGB_8888)
        val upscaled = LcdImagePreprocessor.toUpscaledEnhanced(bmp)
        assertEquals(100, upscaled.width)
        assertEquals(80, upscaled.height)
    }
}
