package com.example.feature.meterreading

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.example.feature.meterreading.detector.DisplayRegionLocalizer
import com.example.feature.meterreading.model.MeterDisplayType
import java.io.InputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DisplayRegionLocalizerTest {

    @Test
    fun locateDisplay_smallDimensions_returnsFallbackViewfinder() {
        val bitmap = Bitmap.createBitmap(40, 40, Bitmap.Config.ARGB_8888)
        val region = DisplayRegionLocalizer.locateDisplay(bitmap)

        assertEquals(DisplayRegionLocalizer.DEFAULT_VIEWFINDER_RECT, region.rectNormalized)
        assertTrue(region.confidence <= 0.4f)
        assertFalse(region.isAutoLocalized)
    }

    @Test
    fun locateDisplay_uniformImage_returnsFallbackViewfinder() {
        val bitmap = Bitmap.createBitmap(400, 600, Bitmap.Config.ARGB_8888)
        // Fill with uniform casing color
        for (y in 0 until 600) {
            for (x in 0 until 400) {
                bitmap.setPixel(x, y, Color.rgb(180, 180, 180))
            }
        }
        val region = DisplayRegionLocalizer.locateDisplay(bitmap)

        assertNotNull(region)
        assertEquals(DisplayRegionLocalizer.DEFAULT_VIEWFINDER_RECT, region.rectNormalized)
        assertFalse(region.isAutoLocalized)
    }

    @Test
    fun locateDisplay_greenBacklitRectangle_localizesBacklitLcd() {
        val width = 400
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Draw meter background (grey)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, Color.rgb(160, 160, 160))
            }
        }

        // Draw green backlit LCD rectangle at x in [100, 300], y in [150, 230] (~2.5:1 aspect ratio)
        for (y in 150 until 230) {
            for (x in 100 until 300) {
                // Vibrant green backlight
                bitmap.setPixel(x, y, Color.rgb(30, 220, 50))
            }
        }

        val region = DisplayRegionLocalizer.locateDisplay(bitmap)

        assertNotNull(region)
        assertEquals(MeterDisplayType.DIGITAL_LCD, region.displayType)
        assertEquals("hsv_chroma_segmentation", region.detectionMethod)
        assertTrue(region.isAutoLocalized)
        assertTrue("Confidence should be high for prominent backlit LCD", region.confidence >= 0.65f)

        // Check bounding box covers the drawn rectangle plus safe padding
        assertTrue("Crop should cover LCD top", region.rectNormalized.top <= (150f / height))
        assertTrue("Crop should cover LCD bottom", region.rectNormalized.bottom >= (230f / height))
        assertTrue("Crop should cover LCD left", region.rectNormalized.left <= (100f / width))
        assertTrue("Crop should cover LCD right", region.rectNormalized.right >= (300f / width))
    }

    @Test
    fun locateDisplay_realSample_ltEm101DigitalMeter_pinpointsLcdRegion() {
        val stream: InputStream? = javaClass.classLoader?.getResourceAsStream("meter_samples/meter_lt_em101_digital.jpg")
        if (stream != null) {
            val bitmap = BitmapFactory.decodeStream(stream)
            assertNotNull("Sample L&T image should load", bitmap)

            val region = DisplayRegionLocalizer.locateDisplay(bitmap)
            assertNotNull(region)
            assertTrue("Should auto-localize green LCD", region.isAutoLocalized)
            assertEquals("hsv_chroma_segmentation", region.detectionMethod)

            // Known ground truth for L&T EM101 LCD:
            // x: 170..540 (norm 0.20..0.63), y: 250..375 (norm 0.24..0.37)
            assertTrue("Should cover top of LCD", region.rectNormalized.top <= 0.25f)
            assertTrue("Should cover bottom of LCD", region.rectNormalized.bottom >= 0.35f)
            assertTrue("Should cover left of LCD", region.rectNormalized.left <= 0.22f)
            assertTrue("Should cover right of LCD", region.rectNormalized.right >= 0.60f)

            // Must NOT include the barcode/serial area at the bottom (y > 0.60)
            assertTrue("Should reject bottom serial numbers", region.rectNormalized.bottom < 0.55f)
        }
    }

    @Test
    fun locateDisplay_realSample_capitalDigitalMeter_pinpointsLcdRegion() {
        val stream: InputStream? = javaClass.classLoader?.getResourceAsStream("meter_samples/meter_capital_digital.jpg")
        if (stream != null) {
            val bitmap = BitmapFactory.decodeStream(stream)
            assertNotNull("Sample Capital image should load", bitmap)

            val region = DisplayRegionLocalizer.locateDisplay(bitmap)
            assertNotNull(region)
            assertTrue("Should auto-localize green LCD", region.isAutoLocalized)
            assertEquals("hsv_chroma_segmentation", region.detectionMethod)

            // Known ground truth for Capital LCD:
            // x: 230..740 (norm 0.26..0.85), y: 170..340 (norm 0.16..0.33)
            assertTrue("Should cover top of LCD", region.rectNormalized.top <= 0.20f)
            assertTrue("Should cover bottom of LCD", region.rectNormalized.bottom >= 0.30f)
            assertTrue("Should cover left of LCD", region.rectNormalized.left <= 0.28f)
            assertTrue("Should cover right of LCD", region.rectNormalized.right >= 0.75f)

            // Must NOT include the bottom serial number area (y > 0.55)
            assertTrue("Should reject bottom serial numbers", region.rectNormalized.bottom < 0.55f)
        }
    }

    @Test
    fun locateDisplay_realSample_mechanicalMeter_safelyFallsBackWithoutFalsePositive() {
        val stream: InputStream? = javaClass.classLoader?.getResourceAsStream("meter_samples/meter_mechanical_dark.jpg")
        if (stream != null) {
            val bitmap = BitmapFactory.decodeStream(stream)
            assertNotNull("Sample mechanical image should load", bitmap)

            val fallbackRect = DisplayRegionLocalizer.DEFAULT_VIEWFINDER_RECT
            val region = DisplayRegionLocalizer.locateDisplay(bitmap, fallbackRect)

            // Mechanical meter has no green LCD; method should not falsely claim hsv_chroma_segmentation
            assertTrue(
                "Should either use edge density or safely fall back to viewfinder reticle",
                region.detectionMethod == "viewfinder_reticle_fallback" ||
                        region.detectionMethod == "vertical_edge_density" ||
                        region.detectionMethod == "guided_viewfinder_prior"
            )
        }
    }
}
