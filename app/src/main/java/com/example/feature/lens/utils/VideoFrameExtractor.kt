package com.example.feature.lens.utils

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import java.io.ByteArrayOutputStream
import java.io.File

@Deprecated("Legacy video frame extraction replaced by still-frame Camera Scan.")
object VideoFrameExtractor {
    fun extractFrames(videoFile: File, numFrames: Int = 3): List<ByteArray> {
        val frames = mutableListOf<ByteArray>()
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            if (durationMs > 0 && numFrames > 0) {
                val intervalMs = durationMs / (numFrames + 1)
                for (i in 1..numFrames) {
                    val timeUs = (i * intervalMs) * 1000
                    val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap != null) {
                        val stream = ByteArrayOutputStream()
                        // Compress to save bandwidth
                        val scaledBitmap = scaleBitmap(bitmap, 1536)
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                        frames.add(stream.toByteArray())
                        if (scaledBitmap != bitmap) {
                            scaledBitmap.recycle()
                        }
                        bitmap.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { retriever.release() } catch (e: Exception) { }
        }
        return frames
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDim: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap

        val ratio = Math.min(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newWidth = Math.round(ratio * width)
        val newHeight = Math.round(ratio * height)
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
