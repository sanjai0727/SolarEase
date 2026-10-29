package com.example.feature.meterreading

import android.graphics.RectF
import com.example.feature.meterreading.util.CameraCoordinateMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CameraCoordinateMapperTest {

    private val delta = 0.001f

    @Test
    fun mapViewRectToImageRect_matchingAspectRatio_isIdentity() {
        // View 1000x2000 (1:2), Image 2000x4000 (1:2)
        val viewRect = RectF(0.10f, 0.20f, 0.90f, 0.60f)
        val imageRect = CameraCoordinateMapper.mapViewRectToImageRect(
            viewRectNormalized = viewRect,
            viewWidth = 1000f,
            viewHeight = 2000f,
            imageWidth = 2000f,
            imageHeight = 4000f
        )

        assertEquals(0.10f, imageRect.left, delta)
        assertEquals(0.20f, imageRect.top, delta)
        assertEquals(0.90f, imageRect.right, delta)
        assertEquals(0.60f, imageRect.bottom, delta)
    }

    @Test
    fun mapViewRectToImageRect_tallScreen_fillsCenterAndCropsHorizontalPillars() {
        // Typical smartphone: 1080x2400 (aspect 9:20 = 0.45)
        // Camera sensor: 3000x4000 (aspect 3:4 = 0.75)
        // FILL_CENTER scales by max(1080/3000, 2400/4000) = max(0.36, 0.60) = 0.60
        // Displayed image size: 1800 x 2400.
        // dx = (1080 - 1800) / 2 = -360. dy = 0.
        // View spans ix in [360/0.60 .. 1440/0.60] = [600 .. 2400] out of 3000 => [0.20 .. 0.80] image width.

        val viewFinder = RectF(0.08f, 0.35f, 0.92f, 0.55f)
        val imageRect = CameraCoordinateMapper.mapViewRectToImageRect(
            viewRectNormalized = viewFinder,
            viewWidth = 1080f,
            viewHeight = 2400f,
            imageWidth = 3000f,
            imageHeight = 4000f
        )

        // vx1 = 0.08 * 1080 = 86.4
        // ix1 = (86.4 - (-360)) / 0.60 = 446.4 / 0.60 = 744.0
        // u1 = 744 / 3000 = 0.248
        assertEquals(0.248f, imageRect.left, delta)

        // vy1 = 0.35 * 2400 = 840
        // iy1 = (840 - 0) / 0.60 = 1400
        // v1 = 1400 / 4000 = 0.35
        assertEquals(0.35f, imageRect.top, delta)

        // vx2 = 0.92 * 1080 = 993.6
        // ix2 = (993.6 - (-360)) / 0.60 = 1353.6 / 0.60 = 2256.0
        // u2 = 2256 / 3000 = 0.752
        assertEquals(0.752f, imageRect.right, delta)

        // vy2 = 0.55 * 2400 = 1320
        // iy2 = 1320 / 0.60 = 2200
        // v2 = 2200 / 4000 = 0.55
        assertEquals(0.55f, imageRect.bottom, delta)
    }

    @Test
    fun bidirectionalMapping_imageToViewAndBack_preservesCoordinates() {
        val viewW = 1080f
        val viewH = 2400f
        val imgW = 3000f
        val imgH = 4000f

        val initialViewRect = RectF(0.15f, 0.40f, 0.85f, 0.60f)
        val mappedImageRect = CameraCoordinateMapper.mapViewRectToImageRect(
            viewRectNormalized = initialViewRect,
            viewWidth = viewW,
            viewHeight = viewH,
            imageWidth = imgW,
            imageHeight = imgH
        )

        val restoredViewRect = CameraCoordinateMapper.mapImageRectToViewRect(
            imageRectNormalized = mappedImageRect,
            viewWidth = viewW,
            viewHeight = viewH,
            imageWidth = imgW,
            imageHeight = imgH
        )

        assertEquals(initialViewRect.left, restoredViewRect.left, delta)
        assertEquals(initialViewRect.top, restoredViewRect.top, delta)
        assertEquals(initialViewRect.right, restoredViewRect.right, delta)
        assertEquals(initialViewRect.bottom, restoredViewRect.bottom, delta)
    }

    @Test
    fun calculateFitBounds_letterbox_centersVertically() {
        // Container 1000x2000 (1:2), Image 1000x1000 (1:1)
        // scale = min(1000/1000, 2000/1000) = 1.0
        // destW = 1000, destH = 1000
        // destLeft = 0, destTop = (2000 - 1000) / 2 = 500
        val fit = CameraCoordinateMapper.calculateFitBounds(
            containerWidth = 1000f,
            containerHeight = 2000f,
            imageWidth = 1000f,
            imageHeight = 1000f
        )

        assertEquals(1.0f, fit.scale, delta)
        assertEquals(0f, fit.destLeft, delta)
        assertEquals(500f, fit.destTop, delta)
        assertEquals(1000f, fit.destWidth, delta)
        assertEquals(1000f, fit.destHeight, delta)
    }

    @Test
    fun calculateFitBounds_pillarbox_centersHorizontally() {
        // Container 2000x1000 (2:1), Image 1000x1000 (1:1)
        // scale = min(2000/1000, 1000/1000) = 1.0
        // destW = 1000, destH = 1000
        // destLeft = (2000 - 1000) / 2 = 500, destTop = 0
        val fit = CameraCoordinateMapper.calculateFitBounds(
            containerWidth = 2000f,
            containerHeight = 1000f,
            imageWidth = 1000f,
            imageHeight = 1000f
        )

        assertEquals(1.0f, fit.scale, delta)
        assertEquals(500f, fit.destLeft, delta)
        assertEquals(0f, fit.destTop, delta)
        assertEquals(1000f, fit.destWidth, delta)
        assertEquals(1000f, fit.destHeight, delta)
    }

    @Test
    fun zeroOrNegativeDimensions_doesNotCrash() {
        val testRect = RectF(0.1f, 0.2f, 0.8f, 0.9f)
        val result = CameraCoordinateMapper.mapViewRectToImageRect(
            viewRectNormalized = testRect,
            viewWidth = 0f,
            viewHeight = -10f,
            imageWidth = 0f,
            imageHeight = 100f
        )
        assertEquals(testRect.left, result.left, delta)
        assertEquals(testRect.top, result.top, delta)

        val fit = CameraCoordinateMapper.calculateFitBounds(0f, 0f, 0f, 0f)
        assertEquals(1f, fit.scale, delta)
    }
}
