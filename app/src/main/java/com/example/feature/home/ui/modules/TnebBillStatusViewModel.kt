package com.example.feature.home.ui.modules

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SubmitState {
    IDLE,
    SUBMITTING,
    SUCCESS,
    INVALID_CAPTCHA,
    INVALID_DETAILS,
    PARSING_ERROR,
    ERROR
}

class TnebBillStatusViewModel : ViewModel() {

    private val _serviceConnectionNumber = MutableStateFlow("")
    val serviceConnectionNumber: StateFlow<String> = _serviceConnectionNumber.asStateFlow()

    private val _registeredMobileNumber = MutableStateFlow("")
    val registeredMobileNumber: StateFlow<String> = _registeredMobileNumber.asStateFlow()

    private val _isValid = MutableStateFlow(false)
    val isValid: StateFlow<Boolean> = _isValid.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _captchaImageBase64 = MutableStateFlow<String?>(null)
    val captchaImageBase64: StateFlow<String?> = _captchaImageBase64.asStateFlow()

    private val _captchaInput = MutableStateFlow("")
    val captchaInput: StateFlow<String> = _captchaInput.asStateFlow()

    private val _pageLoadError = MutableStateFlow(false)
    val pageLoadError: StateFlow<Boolean> = _pageLoadError.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)
    val reloadTrigger: StateFlow<Int> = _reloadTrigger.asStateFlow()

    private val _submitState = MutableStateFlow(SubmitState.IDLE)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private val _submitTrigger = MutableStateFlow(0)
    val submitTrigger: StateFlow<Int> = _submitTrigger.asStateFlow()

    // Structured real TNEB bill data
    private val _billData = MutableStateFlow<TnebConsumerBillData?>(null)
    val billData: StateFlow<TnebConsumerBillData?> = _billData.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentSubmissionId = 0
    private var captchaRefreshJob: kotlinx.coroutines.Job? = null
    private var submissionTimeoutJob: kotlinx.coroutines.Job? = null

    init {
        // Pre-fill Service Number and actual TNEB Registered Mobile Number if securely stored
        val user = com.example.core.session.UserSession.currentUser
        val cached = TnebBillCache.billData.value

        val serviceNo = user?.ebServiceNumber?.ifBlank { null }
            ?: cached?.consumerNumber?.ifBlank { null }
            ?: com.example.core.session.UserSession.getSavedEbServiceNumber()
            ?: ""
        val mobileNo = user?.tnebRegisteredMobile?.ifBlank { null }
            ?: com.example.core.session.UserSession.getSavedTnebMobile()
            ?: ""

        if (serviceNo.isNotBlank()) {
            _serviceConnectionNumber.value = serviceNo.filter { it.isDigit() }.take(12)
        }
        if (mobileNo.isNotBlank()) {
            _registeredMobileNumber.value = mobileNo.filter { it.isDigit() }.takeLast(10)
        }

        // Always begin in IDLE state so the user lands on the verification/input screen
        _submitState.value = SubmitState.IDLE
        _billData.value = null

        validate()
    }

    /**
     * Service connection number must contain exactly 12 numeric digits:
     * - Digits 1–2: Region Code (e.g. 04 for Erode, 09 for Chennai South)
     * - Digits 3–5: Division Code
     * - Digits 6–8: Subdivision Code
     * - Digits 9–12: Consumer Number
     * Example format: 042345678901 (RRDDDSSSSCCCC without spaces).
     */
    fun updateServiceConnectionNumber(number: String) {
        val filtered = number.filter { it.isDigit() }.take(12)
        _serviceConnectionNumber.value = filtered
        validate()
        if (filtered.length in 11..12) {
            persistElectricityDetailsIfValid()
        }
    }

    fun updateRegisteredMobileNumber(number: String) {
        val cleaned = com.example.feature.ebservice.util.cleanIndianMobile(number)
        val filtered = cleaned.filter { it.isDigit() }.take(10)
        _registeredMobileNumber.value = filtered
        validate()
        if (filtered.length == 10) {
            persistElectricityDetailsIfValid()
        }
    }

    fun updateCaptchaInput(input: String) {
        _captchaInput.value = input.filter { it.isLetterOrDigit() }.take(6)
    }

    private fun validate() {
        val isValidScn = _serviceConnectionNumber.value.length in 11..12
        val isValidRmn = _registeredMobileNumber.value.length == 10
        _isValid.value = isValidScn && isValidRmn
    }

    private fun persistElectricityDetailsIfValid() {
        val scn = _serviceConnectionNumber.value.filter { it.isDigit() }.take(12)
        val rmn = _registeredMobileNumber.value.filter { it.isDigit() }.takeLast(10)
        val current = com.example.core.session.UserSession.currentUser
        if (current != null) {
            val updated = current.copy(
                ebServiceNumber = if (scn.length in 11..12) scn else current.ebServiceNumber,
                tnebRegisteredMobile = if (rmn.length == 10) rmn else current.tnebRegisteredMobile
            )
            com.example.core.session.UserSession.saveUser(updated)
        } else {
            com.example.core.session.UserSession.saveExplicitElectricityDetails(
                consumerNumber = if (scn.length in 11..12) scn else null,
                tnebMobile = if (rmn.length == 10) rmn else null
            )
        }
    }

    fun checkBillStatus() {
        if (_isValid.value) {
            persistElectricityDetailsIfValid()
            if (_captchaImageBase64.value == null) {
                // Phase 2A: Load TNPDCL and get CAPTCHA
                requestCaptchaRefresh()
            } else if (_captchaInput.value.isNotEmpty()) {
                // Phase 2B: Submit form
                Log.d("TNEB", "[TNEB] CAPTCHA verified, submitting form...")
                Log.d("BillStatusDiagnostic", "SUBMISSION_STARTED")
                _errorMessage.value = null
                _submitState.value = SubmitState.SUBMITTING
                currentSubmissionId++
                _submitTrigger.value = currentSubmissionId

                submissionTimeoutJob?.cancel()
                submissionTimeoutJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(25000)
                    if (_submitState.value == SubmitState.SUBMITTING) {
                        Log.e("TNEB", "[TNEB] Submission timed out waiting for portal response")
                        _errorMessage.value = "The official TNPDCL utility portal is taking too long to respond. Please check your internet connection or try again."
                        _submitState.value = SubmitState.ERROR
                    }
                }
            }
        }
    }

    fun resetValidationState() {
        captchaRefreshJob?.cancel()
        submissionTimeoutJob?.cancel()
        _isChecking.value = false
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        _submitState.value = SubmitState.IDLE
        _errorMessage.value = null
        _billData.value = null
    }

    fun onCaptchaReceived(base64Data: String) {
        captchaRefreshJob?.cancel()
        val cleanBase64 = if (base64Data.startsWith("data:image")) {
            base64Data.substringAfter("base64,")
        } else {
            base64Data
        }
        _captchaImageBase64.value = cleanBase64
    }

    fun onPageLoadError() {
        Log.e("TNEB", "[TNEB] Page load error occurred in WebView")
        android.util.Log.d("BillStatusDiagnostic", "ON_PAGE_LOAD_ERROR called")
        captchaRefreshJob?.cancel()
        submissionTimeoutJob?.cancel()
        _pageLoadError.value = true
        _isChecking.value = true
        if (_submitState.value == SubmitState.SUBMITTING) {
            _errorMessage.value = "The official TNPDCL utility portal is temporarily unreachable."
            _submitState.value = SubmitState.ERROR
        }
    }

    fun requestCaptchaRefresh() {
        android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_REFRESH_STARTED")
        _captchaImageBase64.value = null
        _captchaInput.value = ""
        _pageLoadError.value = false
        _submitState.value = SubmitState.IDLE
        _errorMessage.value = null
        _isChecking.value = true

        _reloadTrigger.value++
        android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_RELOAD_TRIGGERED: ${_reloadTrigger.value}")

        captchaRefreshJob?.cancel()
        captchaRefreshJob = viewModelScope.launch {
            kotlinx.coroutines.delay(10000) // 10 seconds timeout
            if (_captchaImageBase64.value == null) {
                android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_REFRESH_TIMEOUT reached")
                onPageLoadError()
            }
        }
    }

    /**
     * Primary callback: Receives structured JSON extracted directly from the live WebView DOM.
     */
    fun onParsedDataReceived(json: String, submissionId: Int) {
        Log.d("TNEB", "[TNEB] Received DOM-extracted JSON payload (length: ${json.length})")
        if (submissionId != currentSubmissionId) {
            Log.d("TNEB", "[TNEB] Stale DOM callback ignored. Expected: $currentSubmissionId, Got: $submissionId")
            return
        }
        submissionTimeoutJob?.cancel()

        try {
            val jsonObj = org.json.JSONObject(json)
            if (jsonObj.optBoolean("isError", false)) {
                val errorMsg = jsonObj.optString("errorMessage", "").trim()
                if (errorMsg.isNotBlank()) {
                    if (errorMsg.contains("captcha", ignoreCase = true)) {
                        Log.e("TNEB", "[TNEB] Portal returned INVALID_CAPTCHA from DOM: $errorMsg")
                        _errorMessage.value = "Invalid CAPTCHA code entered. Please try again."
                        _submitState.value = SubmitState.INVALID_CAPTCHA
                    } else {
                        Log.e("TNEB", "[TNEB] Portal returned INVALID_DETAILS from DOM: $errorMsg")
                        _errorMessage.value = errorMsg
                        _submitState.value = SubmitState.INVALID_DETAILS
                    }
                    return
                }
            }
        } catch (e: Exception) {
            Log.w("TNEB", "[TNEB] Error checking isError in JSON: ${e.message}")
        }

        val parsedData = TnebBillStatusHtmlParser.parseJson(json)
        if (parsedData != null && (parsedData.bills.isNotEmpty() || parsedData.consumerName.isNotBlank() || parsedData.tariffCode.isNotBlank())) {
            Log.d("TNEB", "[TNEB] UI received real bill data")
            TnebBillCache.save(parsedData)
            _billData.value = TnebBillCache.billData.value ?: parsedData
            val current = com.example.core.session.UserSession.currentUser
            val scn = parsedData.consumerNumber.ifBlank { _serviceConnectionNumber.value.filter { it.isDigit() }.take(12) }
            val rmn = _registeredMobileNumber.value.filter { it.isDigit() }.takeLast(10)
            if (current != null) {
                val updated = current.copy(
                    ebServiceNumber = scn.ifBlank { current.ebServiceNumber },
                    tnebRegisteredMobile = if (rmn.length == 10) rmn else current.tnebRegisteredMobile
                )
                com.example.core.session.UserSession.saveUser(updated)
            } else {
                com.example.core.session.UserSession.saveExplicitElectricityDetails(
                    consumerNumber = scn.ifBlank { null },
                    tnebMobile = if (rmn.length == 10) rmn else null
                )
            }
            _errorMessage.value = null
            _submitState.value = SubmitState.SUCCESS
        } else {
            Log.w("TNEB", "[TNEB] JSON parse yielded insufficient data, awaiting or falling back to raw HTML")
        }
    }

    /**
     * Secondary / fallback callback: Receives the raw HTML response and parses it if JSON was not received or failed.
     */
    fun analyzeSubmissionResponse(html: String, submissionId: Int) {
        Log.d("TNEB", "[TNEB] HTML received, length: ${html.length}")
        if (submissionId != currentSubmissionId) {
            Log.d("TNEB", "[TNEB] Stale HTML callback ignored. Expected: $currentSubmissionId, Got: $submissionId")
            return
        }
        submissionTimeoutJob?.cancel()

        // If JSON extraction already succeeded and populated billData, skip duplicate processing
        if (_billData.value != null && _submitState.value == SubmitState.SUCCESS) {
            Log.d("TNEB", "[TNEB] Bill data already successfully populated from DOM extraction")
            return
        }

        val lowerHtml = html.lowercase()

        // 1. Check for PrimeFaces error markers (invalid captcha, invalid/mismatched consumer details)
        val hasPluralErrors = html.contains("ui-messages-error")
        val hasSingularErrors = html.contains("ui-message-error")
        if (hasPluralErrors || hasSingularErrors) {
            val errorRegex = Regex("""class="ui-messages?-error-(?:summary|detail)"[^>]*>([^<]+)</span>""", RegexOption.IGNORE_CASE)
            val extractedMessages = errorRegex.findAll(html).map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
            val primaryError = extractedMessages.firstOrNull() ?: ""

            val combined = (primaryError + " " + html).lowercase()
            if (combined.contains("captcha")) {
                Log.e("TNEB", "[TNEB] Portal returned INVALID_CAPTCHA")
                _errorMessage.value = "Invalid CAPTCHA code entered. Please try again."
                _submitState.value = SubmitState.INVALID_CAPTCHA
                return
            } else {
                Log.e("TNEB", "[TNEB] Portal returned INVALID_DETAILS: $primaryError")
                _errorMessage.value = if (primaryError.isNotBlank()) primaryError else "Invalid Service Connection Number or Registered Mobile Number."
                _submitState.value = SubmitState.INVALID_DETAILS
                return
            }
        }

        // 2. Check for bill page markers and parse HTML
        val isBillPage = lowerHtml.contains("consdetails") || lowerHtml.contains("consumption charges and collection details")
        if (isBillPage) {
            Log.d("TNEB", "[TNEB] Bill page detected in HTML response")
            val parsedData = TnebBillStatusHtmlParser.parseHtml(html)
            if (parsedData != null && (parsedData.bills.isNotEmpty() || parsedData.consumerName.isNotBlank() || parsedData.tariffCode.isNotBlank())) {
                Log.d("TNEB", "[TNEB] UI received real bill data")
                TnebBillCache.save(parsedData)
                _billData.value = TnebBillCache.billData.value ?: parsedData
                val current = com.example.core.session.UserSession.currentUser
                val scn = parsedData.consumerNumber.ifBlank { _serviceConnectionNumber.value.filter { it.isDigit() }.take(12) }
                val rmn = _registeredMobileNumber.value.filter { it.isDigit() }.takeLast(10)
                if (current != null) {
                    val updated = current.copy(
                        ebServiceNumber = scn.ifBlank { current.ebServiceNumber },
                        tnebRegisteredMobile = if (rmn.length == 10) rmn else current.tnebRegisteredMobile
                    )
                    com.example.core.session.UserSession.saveUser(updated)
                } else {
                    com.example.core.session.UserSession.saveExplicitElectricityDetails(
                        consumerNumber = scn.ifBlank { null },
                        tnebMobile = if (rmn.length == 10) rmn else null
                    )
                }
                _errorMessage.value = null
                _submitState.value = SubmitState.SUCCESS
            } else {
                Log.e("TNEB", "[TNEB] Parsing failed: Bill details could not be read from HTML")
                _errorMessage.value = "Bill details could not be read"
                _submitState.value = SubmitState.PARSING_ERROR
            }
        } else {
            Log.e("TNEB", "[TNEB] Parsing failed: Neither bill page nor standard error recognized")
            _errorMessage.value = "Bill details could not be read"
            _submitState.value = SubmitState.ERROR
        }
    }
}
