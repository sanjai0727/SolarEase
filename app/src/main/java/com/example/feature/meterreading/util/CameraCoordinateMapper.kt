package com.example.feature.meterreading.util

import android.graphics.RectF
import kotlin.math.max

/**
 * High-precision coordinate mapper between CameraX [androidx.camera.view.PreviewView]
 * (with [androidx.camera.view.PreviewView.ScaleType.FILL_CENTER]) and the captured [android.graphics.Bitmap].
 *
 * Accounts for:
 * 1. Aspect ratio differences between the view (screen) and captured camera sensor image.
 * 2. Uniform scaling and center-crop offsets applied by FILL_CENTER.
 * 3. Bidirectional mapping between screen view coordinates and image coordinates.
 */
object CameraCoordinateMapper {

    /**
     * Maps a normalized rectangle defined on a PreviewView (with ScaleType.FILL_CENTER)
     * into normalized coordinates [0, 1] on the captured image (after sensor rotation).
     *
     * @param viewRectNormalized Rectangle normalized to the PreviewView bounds [0..1]
     * @param viewWidth Width of the PreviewView in pixels
     * @param viewHeight Height of the PreviewView in pixels
     * @param imageWidth Width of the captured image in pixels (in same orientation as preview)
     * @param imageHeight Height of the captured image in pixels (in same orientation as preview)
     * @return Rectangle normalized to the image bounds [0..1]
     */
    fun mapViewRectToImageRect(
        viewRectNormalized: RectF,
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): RectF {
        if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
            return RectF(viewRectNormalized)
        }

        // PreviewView.ScaleType.FILL_CENTER scales uniformly by max(scaleX, scaleY)
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = max(scaleX, scaleY)

        val displayedWidth = imageWidth * scale
        val displayedHeight = imageHeight * scale

        // The image is centered inside the view; dx and dy are the top-left offset of the image in view pixels
        val dx = (viewWidth - displayedWidth) / 2f
        val dy = (viewHeight - displayedHeight) / 2f

        // Convert normalized view coordinates to pixel coordinates on the view
        val vx1 = viewRectNormalized.left * viewWidth
        val vy1 = viewRectNormalized.top * viewHeight
        val vx2 = viewRectNormalized.right * viewWidth
        val vy2 = viewRectNormalized.bottom * viewHeight

        // Map view pixel coordinates to image pixel coordinates
        val ix1 = (vx1 - dx) / scale
        val iy1 = (vy1 - dy) / scale
        val ix2 = (vx2 - dx) / scale
        val iy2 = (vy2 - dy) / scale

        // Normalize to [0, 1] relative to the image dimensions and clamp
        val u1 = (ix1 / imageWidth).coerceIn(0f, 1f)
        val v1 = (iy1 / imageHeight).coerceIn(0f, 1f)
        val u2 = (ix2 / imageWidth).coerceIn(0f, 1f)
        val v2 = (iy2 / imageHeight).coerceIn(0f, 1f)

        return RectF(u1, v1, u2, v2)
    }

    /**
     * Maps a normalized image rectangle [0, 1] back into normalized PreviewView coordinates.
     */
    fun mapImageRectToViewRect(
        imageRectNormalized: RectF,
        viewWidth: Float,
        viewHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): RectF {
        if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
            return RectF(imageRectNormalized)
        }

        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = max(scaleX, scaleY)

        val displayedWidth = imageWidth * scale
        val displayedHeight = imageHeight * scale

        val dx = (viewWidth - displayedWidth) / 2f
        val dy = (viewHeight - displayedHeight) / 2f

        val ix1 = imageRectNormalized.left * imageWidth
        val iy1 = imageRectNormalized.top * imageHeight
        val ix2 = imageRectNormalized.right * imageWidth
        val iy2 = imageRectNormalized.bottom * imageHeight

        val vx1 = (ix1 * scale + dx) / viewWidth
        val vy1 = (iy1 * scale + dy) / viewHeight
        val vx2 = (ix2 * scale + dx) / viewWidth
        val vy2 = (iy2 * scale + dy) / viewHeight

        return RectF(
            vx1.coerceIn(0f, 1f),
            vy1.coerceIn(0f, 1f),
            vx2.coerceIn(0f, 1f),
            vy2.coerceIn(0f, 1f)
        )
    }

    data class FittedBounds(
        val scale: Float,
        val destLeft: Float,
        val destTop: Float,
        val destWidth: Float,
        val destHeight: Float
    )

    /**
     * Calculates the on-screen rendered bounds of an image rendered with [androidx.compose.ui.layout.ContentScale.Fit]
     * inside a container of size [containerWidth] x [containerHeight].
     */
    fun calculateFitBounds(
        containerWidth: Float,
        containerHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): FittedBounds {
        if (containerWidth <= 0f || containerHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
            return FittedBounds(1f, 0f, 0f, containerWidth, containerHeight)
        }
        val scale = kotlin.math.min(containerWidth / imageWidth, containerHeight / imageHeight)
        val destWidth = imageWidth * scale
        val destHeight = imageHeight * scale
        val destLeft = (containerWidth - destWidth) / 2f
        val destTop = (containerHeight - destHeight) / 2f
        return FittedBounds(scale, destLeft, destTop, destWidth, destHeight)
    }
}
