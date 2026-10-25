package com.example.feature.meterreading.detector

import android.graphics.Bitmap
import com.example.feature.meterreading.model.MeterScanResult

interface MeterOcrDetector {
    /**
     * Processes a cropped meter display bitmap and extracts a candidate reading.
     */
    suspend fun recognizeText(bitmap: Bitmap, baselineKwh: Double? = null): MeterScanResult

    /**
     * Releases any underlying native or thread pool resources.
     */
    fun close()
}
