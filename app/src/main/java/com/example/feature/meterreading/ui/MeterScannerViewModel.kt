package com.example.feature.meterreading.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.home.ui.modules.MeterCycleBaseline
import com.example.feature.home.ui.modules.MeterReadingCache
import com.example.feature.home.ui.modules.MeterReadingRecord
import com.example.feature.home.ui.modules.MeterValidationResult
import com.example.feature.meterreading.detector.DisplayRegionLocalizer
import com.example.feature.meterreading.detector.MeterOcrDetector
import com.example.feature.meterreading.detector.MlKitMeterOcrDetector
import com.example.feature.meterreading.model.MeterReadingCandidate
import com.example.feature.meterreading.model.MeterScanResult
import com.example.feature.meterreading.parser.ReadingPlausibilityStatus
import com.example.feature.meterreading.parser.ReadingPlausibilityValidator
import com.example.feature.meterreading.util.CameraCoordinateMapper
import java.util.Locale
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface MeterScannerUiState {
    data object Ready : MeterScannerUiState
    data object Capturing : MeterScannerUiState
    data class Processing(val message: String = "Analyzing meter display...") : MeterScannerUiState
    data class Review(
        val croppedBitmap: Bitmap,
        val candidate: MeterReadingCandidate,
        val editableText: String,
        val validationError: String? = null,
        val plausibilityStatus: ReadingPlausibilityStatus = ReadingPlausibilityStatus.Valid,
        val isSaving: Boolean = false,
        val isAutoLocalized: Boolean = true,
        val cropRectNormalized: RectF = DisplayRegionLocalizer.DEFAULT_VIEWFINDER_RECT
    ) : MeterScannerUiState
    data class AdjustCrop(
        val fullBitmap: Bitmap,
        val currentRectNormalized: RectF
    ) : MeterScannerUiState
    data class Success(val savedReadingValue: Double) : MeterScannerUiState
    data class Error(val message: String, val canRetry: Boolean = true) : MeterScannerUiState
}

class MeterScannerViewModel(
    private val ocrDetector: MeterOcrDetector = MlKitMeterOcrDetector(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _uiState = MutableStateFlow<MeterScannerUiState>(MeterScannerUiState.Ready)
    val uiState: StateFlow<MeterScannerUiState> = _uiState.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private var cameraControl: CameraControl? = null
    private var fullCapturedBitmap: Bitmap? = null
    private var lastCropRect: RectF = DisplayRegionLocalizer.DEFAULT_VIEWFINDER_RECT

    val baseline: MeterCycleBaseline?
        get() = MeterReadingCache.baseline.value

    val lastRecordedReading: MeterReadingRecord?
        get() = MeterReadingCache.getLastReading()

    fun attachCameraControl(control: CameraControl?) {
        this.cameraControl = control
    }

    fun toggleTorch() {
        val next = !_isTorchOn.value
        cameraControl?.enableTorch(next)
        _isTorchOn.value = next
    }

    /**
     * Captures a high-resolution still image from CameraX.
     * Maps the on-screen live camera viewfinder frame to the captured image coordinates
     * before performing OCR, ensuring the captured crop exactly matches the green guide box.
     */
    fun captureAndProcess(
        imageCapture: ImageCapture,
        executor: Executor,
        viewFinderRectNormalized: RectF = DisplayRegionLocalizer.DEFAULT_VIEWFINDER_RECT,
        viewWidthPx: Float = 0f,
        viewHeightPx: Float = 0f
    ) {
        if (_uiState.value is MeterScannerUiState.Capturing || _uiState.value is MeterScannerUiState.Processing) {
            return
        }

        _uiState.value = MeterScannerUiState.Capturing

        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    processCapturedProxy(imageProxy, viewFinderRectNormalized, viewWidthPx, viewHeightPx)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("MeterScannerViewModel", "Image capture failed", exception)
                    _uiState.value = MeterScannerUiState.Error(
                        message = "Could not capture photo: ${exception.localizedMessage ?: "Camera error"}"
                    )
                }
            }
        )
    }

    private fun processCapturedProxy(
        imageProxy: ImageProxy,
        viewFinderRectNormalized: RectF,
        viewWidthPx: Float,
        viewHeightPx: Float
    ) {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = MeterScannerUiState.Processing("Extracting meter display...")

            val fullBitmap = try {
                imageProxyToBitmap(imageProxy)
            } catch (e: Exception) {
                Log.e("MeterScannerViewModel", "Bitmap conversion failed", e)
                null
            } finally {
                imageProxy.close()
            }

            if (fullBitmap == null) {
                _uiState.value = MeterScannerUiState.Error("Failed to decode photo from camera.")
                return@launch
            }

            fullCapturedBitmap = fullBitmap

            val mappedReticle = if (viewWidthPx > 0f && viewHeightPx > 0f) {
                CameraCoordinateMapper.mapViewRectToImageRect(
                    viewRectNormalized = viewFinderRectNormalized,
                    viewWidth = viewWidthPx,
                    viewHeight = viewHeightPx,
                    imageWidth = fullBitmap.width.toFloat(),
                    imageHeight = fullBitmap.height.toFloat()
                )
            } else {
                viewFinderRectNormalized
            }

            // Direct cropping from viewfinder reticle (no automatic display localization)
            lastCropRect = mappedReticle

            val cropped = cropToNormalizedRect(fullBitmap, mappedReticle)

            runOcrOnCroppedBitmap(
                croppedBitmap = cropped,
                cropRectNormalized = mappedReticle,
                isAutoLocalized = false
            )
        }
    }

    /**
     * Helper for unit tests to process a bitmap directly with coordinate mapping.
     */
    fun processBitmapForTest(
        bitmap: Bitmap,
        viewFinderRectNormalized: RectF,
        viewWidthPx: Float,
        viewHeightPx: Float
    ) {
        fullCapturedBitmap = bitmap
        val mappedReticle = if (viewWidthPx > 0f && viewHeightPx > 0f) {
            CameraCoordinateMapper.mapViewRectToImageRect(
                viewRectNormalized = viewFinderRectNormalized,
                viewWidth = viewWidthPx,
                viewHeight = viewHeightPx,
                imageWidth = bitmap.width.toFloat(),
                imageHeight = bitmap.height.toFloat()
            )
        } else {
            viewFinderRectNormalized
        }
        lastCropRect = mappedReticle
        val cropped = cropToNormalizedRect(bitmap, mappedReticle)
        viewModelScope.launch(ioDispatcher) {
            runOcrOnCroppedBitmap(cropped, mappedReticle, false)
        }
    }

    /**
     * Runs ML Kit OCR on a specified cropped bitmap and updates the review state.
     */
    private suspend fun runOcrOnCroppedBitmap(
        croppedBitmap: Bitmap,
        cropRectNormalized: RectF,
        isAutoLocalized: Boolean
    ) {
        _uiState.value = MeterScannerUiState.Processing("Reading numbers...")

        val scanResult = ocrDetector.recognizeText(
            bitmap = croppedBitmap,
            baselineKwh = baseline?.baselineReading
        )

        val candidate = scanResult.candidate ?: MeterReadingCandidate(
            rawText = "",
            numericValue = 0.0,
            displayValue = "",
            status = com.example.feature.meterreading.model.MeterOcrStatus.NO_READING_FOUND,
            statusMessage = "No reading could be determined from the frame."
        )

        val initialEditText = if (candidate.numericValue > 0.0) {
            if (candidate.hasDecimal) String.format(Locale.US, "%.1f", candidate.numericValue)
            else String.format(Locale.US, "%.0f", candidate.numericValue)
        } else {
            ""
        }

        val plausibility = if (candidate.numericValue > 0.0) {
            ReadingPlausibilityValidator.validate(candidate.numericValue, baseline, lastRecordedReading)
        } else {
            ReadingPlausibilityStatus.Valid
        }

        _uiState.value = MeterScannerUiState.Review(
            croppedBitmap = croppedBitmap,
            candidate = candidate,
            editableText = initialEditText,
            validationError = null,
            plausibilityStatus = plausibility,
            isAutoLocalized = isAutoLocalized,
            cropRectNormalized = cropRectNormalized
        )
    }

    /**
     * Opens the crop adjustment screen so the user can manually reposition or resize
     * the display box over the meter LCD or rotating wheel strip.
     */
    fun startCropAdjustment() {
        val full = fullCapturedBitmap ?: return
        _uiState.value = MeterScannerUiState.AdjustCrop(
            fullBitmap = full,
            currentRectNormalized = lastCropRect
        )
    }

    /**
     * Applies the user's adjusted display crop rectangle and re-runs OCR.
     */
    fun applyAdjustedCrop(newCropRect: RectF) {
        val full = fullCapturedBitmap ?: return
        lastCropRect = newCropRect

        viewModelScope.launch(ioDispatcher) {
            val cropped = cropToNormalizedRect(full, newCropRect)
            runOcrOnCroppedBitmap(
                croppedBitmap = cropped,
                cropRectNormalized = newCropRect,
                isAutoLocalized = true
            )
        }
    }

    fun cancelCropAdjustment() {
        val full = fullCapturedBitmap ?: return
        viewModelScope.launch(ioDispatcher) {
            val cropped = cropToNormalizedRect(full, lastCropRect)
            runOcrOnCroppedBitmap(
                croppedBitmap = cropped,
                cropRectNormalized = lastCropRect,
                isAutoLocalized = true
            )
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotation = image.imageInfo.rotationDegrees
        return if (rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    private fun cropToNormalizedRect(source: Bitmap, normRect: RectF): Bitmap {
        val width = source.width
        val height = source.height

        val left = (normRect.left * width).toInt().coerceIn(0, width - 1)
        val top = (normRect.top * height).toInt().coerceIn(0, height - 1)
        val right = (normRect.right * width).toInt().coerceIn(left + 1, width)
        val bottom = (normRect.bottom * height).toInt().coerceIn(top + 1, height)

        val cropWidth = right - left
        val cropHeight = bottom - top

        return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
    }

    fun onEditableTextChanged(newText: String) {
        val current = _uiState.value as? MeterScannerUiState.Review ?: return
        val sanitized = newText.filter { it.isDigit() || it == '.' }
        if (sanitized.count { it == '.' } <= 1) {
            val numVal = sanitized.toDoubleOrNull()
            val plausibility = if (numVal != null && numVal > 0.0) {
                ReadingPlausibilityValidator.validate(numVal, baseline, lastRecordedReading)
            } else {
                ReadingPlausibilityStatus.Valid
            }

            _uiState.update {
                current.copy(
                    editableText = sanitized,
                    validationError = null,
                    plausibilityStatus = plausibility
                )
            }
        }
    }

    /**
     * Confirms the reviewed/corrected reading and integrates it directly into SolarEase's MeterReadingCache.
     */
    fun confirmReading(onSuccess: (Double) -> Unit) {
        val current = _uiState.value as? MeterScannerUiState.Review ?: return
        if (current.isSaving) return

        val text = current.editableText.trim()
        val readingVal = text.toDoubleOrNull()

        if (readingVal == null || readingVal <= 0.0) {
            _uiState.update {
                current.copy(validationError = "Please enter a valid numeric reading.")
            }
            return
        }

        _uiState.update { current.copy(isSaving = true) }

        viewModelScope.launch(ioDispatcher) {
            val isTodayRecorded = MeterReadingCache.metrics.value?.isTodayLogged == true
            val result = if (isTodayRecorded) {
                MeterReadingCache.updateTodayReading(readingVal)
            } else {
                MeterReadingCache.saveReading(readingVal)
            }

            withContext(Dispatchers.Main) {
                when (result) {
                    is MeterValidationResult.Success -> {
                        _uiState.value = MeterScannerUiState.Success(readingVal)
                        onSuccess(readingVal)
                    }
                    is MeterValidationResult.LowerThanPrevious -> {
                        _uiState.update {
                            current.copy(
                                isSaving = false,
                                validationError = result.message
                            )
                        }
                    }
                    is MeterValidationResult.DuplicateToday -> {
                        _uiState.value = MeterScannerUiState.Success(readingVal)
                        onSuccess(readingVal)
                    }
                    is MeterValidationResult.Error -> {
                        _uiState.update {
                            current.copy(
                                isSaving = false,
                                validationError = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun retake() {
        fullCapturedBitmap = null
        _uiState.value = MeterScannerUiState.Ready
    }

    override fun onCleared() {
        super.onCleared()
        ocrDetector.close()
        fullCapturedBitmap = null
    }
}
