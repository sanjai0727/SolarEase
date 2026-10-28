package com.example.feature.home.ui.modules

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TnebBillStatusWebView(
    viewModel: TnebBillStatusViewModel,
    modifier: Modifier = Modifier
) {
    // Keep a single instance of WebView across recompositions
    val webView = rememberWebViewWithLifecycle()
    val reloadTrigger by viewModel.reloadTrigger.collectAsState()
    val submitTrigger by viewModel.submitTrigger.collectAsState()
    
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val registeredMobileNumber by viewModel.registeredMobileNumber.collectAsState()
    val captchaInput by viewModel.captchaInput.collectAsState()

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            android.util.Log.d("BillStatusDiagnostic", "CAPTCHA_RELOAD_STARTED")
            webView.loadUrl("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
        }
    }

    LaunchedEffect(submitTrigger) {
        if (submitTrigger > 0) {
            android.util.Log.d("BillStatusDiagnostic", "SUBMIT_TRIGGER_RECEIVED")
            val safeScn = serviceConnectionNumber.replace("'", "\\'").replace("\"", "\\\"")
            val safeMob = registeredMobileNumber.replace("'", "\\'").replace("\"", "\\\"")
            val safeCap = captchaInput.replace("'", "\\'").replace("\"", "\\\"")
            
            android.util.Log.d("BillStatusDiagnostic", "SUBMIT_JS_STARTED")
            
            val cookieManager = android.webkit.CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://www.tnebltd.gov.in")
            val cookieList = cookies?.split(";")?.map { it.substringBefore("=").trim() } ?: emptyList()
            val hasJSessionId = cookieList.contains("JSESSIONID")
            val fingerprint = cookies?.hashCode()?.toString(16) ?: "null"
            android.util.Log.d("BillStatusDiagnostic", "--- SESSION DIAGNOSTICS ---\nCOOKIE_COUNT=${cookieList.size}\nJSESSIONID_PRESENT=$hasJSessionId\nCOOKIE_NAMES=${cookieList.joinToString()}\nSESSION_FINGERPRINT=$fingerprint")
            
            val submitJs = """
                javascript:(function() {
                    try {
                        var diag = [];
                        diag.push('CURRENT_URL=' + window.location.href);
                        diag.push('FORM_COUNT=' + document.forms.length);
                        var f = document.forms[0];
                        if (f) {
                            diag.push('FORM_ID=' + f.id);
                            diag.push('FORM_ACTION=' + f.action);
                            diag.push('FORM_METHOD=' + f.method);
                        }
                        diag.push('SERVICENO_PRESENT=' + (document.getElementById('serviceno') != null));
                        diag.push('MOB_PRESENT=' + (document.getElementById('mob') != null));
                        diag.push('CAP_PRESENT=' + (document.getElementById('cap') != null));
                        var subBtn = document.getElementById('submit3');
                        diag.push('SUBMIT3_PRESENT=' + (subBtn != null));
                        if (subBtn) {
                            diag.push('SUBMIT_TAG=' + subBtn.tagName);
                            diag.push('SUBMIT_TYPE=' + subBtn.type);
                            diag.push('SUBMIT_FORM_ID=' + (subBtn.form ? subBtn.form.id : 'none'));
                            diag.push('SUBMIT_DISABLED=' + subBtn.disabled);
                            diag.push('SUBMIT_ONCLICK_PRESENT=' + (subBtn.onclick != null));
                        }
                        diag.push('VIEWSTATE_PRESENT=' + (document.getElementsByName('javax.faces.ViewState').length > 0));
                        var hidden = [];
                        var inputs = document.getElementsByTagName('input');
                        for (var i = 0; i < inputs.length; i++) {
                            if (inputs[i].type == 'hidden') hidden.push(inputs[i].name);
                        }
                        diag.push('HIDDEN_FIELD_NAMES=' + hidden.join(','));
                        
                        if (f) {
                            f.addEventListener('submit', function(e) {
                                AndroidCaptchaBridge.onDiagnosticLog('FORM_SUBMIT_EVENT_RECEIVED=true\nFORM_SUBMIT_ACTION=' + f.action);
                            });
                        }
                        AndroidCaptchaBridge.onDiagnosticLog("--- PRE-SUBMISSION DIAGNOSTICS ---\n" + diag.join('\n'));

                        document.getElementById('serviceno').value = '$safeScn';
                        document.getElementById('mob').value = '$safeMob';
                        document.getElementById('cap').value = '$safeCap';
                        if (subBtn) {
                            console.log('BillStatusDiagnostic: SUBMIT_BUTTON_FOUND');
                            subBtn.click();
                            console.log('BillStatusDiagnostic: SUBMIT_CLICK_EXECUTED');
                        } else {
                            console.log('BillStatusDiagnostic: SUBMIT_BUTTON_NOT_FOUND');
                        }
                    } catch(e) {
                        console.log('BillStatusDiagnostic: SUBMIT_JS_EXCEPTION ' + e.toString());
                    }
                })();
            """.trimIndent()
            webView.evaluateJavascript(submitJs, null)
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { _ ->
                webView.apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Security: Strict SSL validation preserved

                    addJavascriptInterface(
                        AndroidCaptchaBridge(viewModel),
                        "AndroidCaptchaBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            android.util.Log.d("BillStatusDiagnostic", "PAGE_STARTED: ${url?.take(50)}")
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
                            android.util.Log.d("BillStatusDiagnostic", "PAGE_FINISHED: ${url.take(50)}")
                            
                            val currentState = viewModel.submitState.value
                            
                            if (currentState == SubmitState.SUBMITTING) {
                                val currentSubmissionId = viewModel.submitTrigger.value
                                android.util.Log.d("TNEB", "[TNEB] Bill page detected: ${url.take(60)}")
                                android.util.Log.d("BillStatusDiagnostic", "HTML_EXTRACTION_STARTED")
                                
                                // Extract structured DOM and fallback HTML
                                val checkResultJs = """
                                    javascript:(function() {
                                        try {
                                            var result = {
                                                isError: false,
                                                errorMessage: '',
                                                consumerName: '',
                                                consumerNumber: '',
                                                region: '',
                                                circle: '',
                                                section: '',
                                                distribution: '',
                                                serviceNumber: '',
                                                address: '',
                                                serviceStatus: '',
                                                aadhar: '',
                                                gst: '',
                                                pan: '',
                                                phase: '',
                                                sanctionedLoad: '',
                                                tempReducedLoad: '',
                                                meterNumber: '',
                                                securityDeposit: '',
                                                mcdDeposit: '',
                                                serviceCategory: '',
                                                tariffCode: '',
                                                consumerRating: '',
                                                duesText: '',
                                                isDuesNil: false,
                                                records: []
                                            };

                                            var bText = document.body ? (document.body.innerText || '') : '';
                                            
                                            // Check for CAPTCHA or details errors
                                            var errElem = document.querySelector('.ui-messages-error-summary, .ui-messages-error-detail, .ui-message-error-detail, .ui-message-error-summary');
                                            if (errElem && errElem.innerText && errElem.innerText.trim()) {
                                                result.isError = true;
                                                result.errorMessage = errElem.innerText.trim();
                                            } else if (bText.toLowerCase().includes('invalid captcha')) {
                                                result.isError = true;
                                                result.errorMessage = 'INVALID_CAPTCHA';
                                            }

                                            // Extract Consumer Name and Number
                                            var nameM = bText.match(/CONSUMER\s*NAME\s*:\s*([^<\n\r]+)/i);
                                            if (nameM) result.consumerName = nameM[1].trim();
                                            var numM = bText.match(/CONSUMER\s*NUMBER\s*:\s*([0-9]+)/i);
                                            if (numM) result.consumerNumber = numM[1].trim();

                                            // Extract Dues Status
                                            var duesM = bText.match(/DUES\s*TO\s*BE\s*PAID\s*IS\s*["']?([^"'\n\r<]+)["']?/i);
                                            if (duesM) {
                                                result.duesText = duesM[0].trim();
                                                if (result.duesText.toUpperCase().includes('NIL')) result.isDuesNil = true;
                                            } else if (bText.toUpperCase().includes('DUES TO BE PAID IS "NIL"')) {
                                                result.duesText = 'DUES TO BE PAID IS "NIL"';
                                                result.isDuesNil = true;
                                            }

                                            // Extract Tables
                                            var tables = document.getElementsByTagName('table');
                                            for (var t = 0; t < tables.length; t++) {
                                                var tbl = tables[t];
                                                var tblText = (tbl.innerText || '').toUpperCase();

                                                // Consumer Ledger / Metadata Table
                                                if (tblText.includes('REGION') || tblText.includes('SANCTIONED LOAD') || tblText.includes('TARIFF CODE')) {
                                                    var rows = tbl.rows;
                                                    for (var r = 0; r < rows.length; r++) {
                                                        var cells = rows[r].cells;
                                                        for (var c = 0; c < cells.length; c++) {
                                                            var k = (cells[c].innerText || '').trim().toUpperCase();
                                                            var v = (c + 1 < cells.length) ? (cells[c + 1].innerText || '').trim() : '';
                                                            if (k === 'REGION' && v) result.region = v;
                                                            else if (k === 'CIRCLE' && v) result.circle = v;
                                                            else if (k === 'SECTION' && v) result.section = v;
                                                            else if (k === 'DISTRIBUTION' && v) result.distribution = v;
                                                            else if (k === 'SERVICE NUMBER' && v) result.serviceNumber = v;
                                                            else if (k === 'ADDRESS' && v) result.address = v;
                                                            else if (k === 'SERVICE STATUS' && v) result.serviceStatus = v;
                                                            else if (k === 'GST NUMBER' && v) result.gst = v;
                                                            else if (k === 'PHASE' && v) result.phase = v;
                                                            else if (k === 'SANCTIONED LOAD' && v) result.sanctionedLoad = v;
                                                            else if (k === 'TEMPORARILY REDUCED LOAD' && v) result.tempReducedLoad = v;
                                                            else if (k === 'METER NUMBER' && v) result.meterNumber = v;
                                                            else if (k.indexOf('ACCD*') !== -1 && v) result.securityDeposit = v;
                                                            else if (k.indexOf('MCD AS ON') !== -1 && v) result.mcdDeposit = v;
                                                            else if (k === 'SERVICE CATEGORY' && v) result.serviceCategory = v;
                                                            else if (k === 'TARIFF CODE' && v) result.tariffCode = v;
                                                            else if (k === 'CONSUMER RATING' && v) result.consumerRating = v;
                                                        }
                                                    }
                                                }

                                                // Bill History Table (Consumption Charges and Collection Details)
                                                if (tblText.includes('CONSUMPTION CHARGES') || tblText.includes('SPLITUP OF CHARGES') || (tblText.includes('ASSESSMENT') && tblText.includes('TOTAL CHARGES'))) {
                                                    var rows = tbl.rows;
                                                    for (var r = 0; r < rows.length; r++) {
                                                        var cells = rows[r].cells;
                                                        if (cells.length >= 8) {
                                                            var d = (cells[0].innerText || '').trim();
                                                            if (/\d{2}[\/\-]\d{2}[\/\-]\d{4}/.test(d)) {
                                                                var getV = function(idx) { return (idx < cells.length) ? (cells[idx].innerText || '').trim() : ''; };
                                                                result.records.push({
                                                                    assessmentDate: getV(0),
                                                                    entryDate: getV(1),
                                                                    status: getV(2),
                                                                    kwh: getV(3),
                                                                    kvah: getV(4),
                                                                    recordedDemand: getV(5),
                                                                    powerFactor: getV(6),
                                                                    units: getV(7),
                                                                    ccCharges: getV(8),
                                                                    electricityTax: getV(9),
                                                                    weldingCharges: getV(10),
                                                                    excessDemand: getV(11),
                                                                    pfPenalty: getV(12),
                                                                    fixedCharges: getV(13),
                                                                    totalCharges: getV(14),
                                                                    advancePaid: getV(15),
                                                                    adjustment: getV(16),
                                                                    amountDue: getV(17),
                                                                    dueDate: getV(18),
                                                                    amountPaid: getV(19),
                                                                    receiptNumber: getV(20),
                                                                    paymentDate: getV(21)
                                                                });
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Send structured data to the bridge
                                            AndroidCaptchaBridge.onParsedDataReceived(JSON.stringify(result), $currentSubmissionId);
                                        } catch (e) {
                                            AndroidCaptchaBridge.onDiagnosticLog("DOM_EXTRACT_ERROR=" + e.toString());
                                        }

                                        // Fallback: Send raw HTML string
                                        var html = document.documentElement.outerHTML;
                                        AndroidCaptchaBridge.onHtmlResponseReceived(html, $currentSubmissionId);
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(checkResultJs, null)
                            } else if (currentState == SubmitState.IDLE) {
                                val cookieManager = android.webkit.CookieManager.getInstance()
                                CookieManagerBridge.logSession(cookieManager)
                                
                                // Inject JavaScript to extract the CAPTCHA image
                                val js = """
                                    javascript:(function() {
                                        function processCaptcha(img) {
                                            try {
                                                var canvas = document.createElement('canvas');
                                                canvas.width = img.naturalWidth || img.width || 120;
                                                canvas.height = img.naturalHeight || img.height || 40;
                                                var ctx = canvas.getContext('2d');
                                                ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
                                                var dataURL = canvas.toDataURL('image/png');
                                                AndroidCaptchaBridge.onCaptchaExtracted(dataURL);
                                            } catch(e) {
                                                // Handle cross-origin issues or errors silently
                                            }
                                        }
    
                                        var img = document.getElementById('imgCaptchaId');
                                        if (img) {
                                            if (img.complete && img.naturalWidth > 0) {
                                                processCaptcha(img);
                                            } else {
                                                img.onload = function() { processCaptcha(img); };
                                            }
                                        }
                                    })();
                                """.trimIndent()
                                view.evaluateJavascript(js, null)
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val uri = request?.url ?: return true
                            val host = uri.host?.lowercase(java.util.Locale.ROOT) ?: return true
                            val isAllowed = host == "www.tnebltd.gov.in" ||
                                host == "tnebltd.gov.in" ||
                                host == "nsc.tnebltd.gov.in" ||
                                host.endsWith(".tnebnet.org")
                            val isHttps = uri.scheme.equals("https", ignoreCase = true)
                            if (isAllowed && isHttps) {
                                return false
                            }
                            android.util.Log.w("BillStatusDiagnostic", "Blocked WebView navigation to unapproved host: $host")
                            return true
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                android.util.Log.d("BillStatusDiagnostic", "WEBVIEW_ERROR: ${error?.errorCode}")
                                viewModel.onPageLoadError()
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: android.webkit.WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                            if (request?.isForMainFrame == true) {
                                android.util.Log.d("BillStatusDiagnostic", "HTTP_ERROR: ${errorResponse?.statusCode}")
                                viewModel.onPageLoadError()
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            android.util.Log.d("BillStatusDiagnostic", "SSL_ERROR: ${error?.primaryError}")
                            // STRICT SSL RULE: Do NOT call handler.proceed().
                            handler?.cancel()
                            viewModel.onPageLoadError()
                        }
                    }

                    loadUrl("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
                }
            },
            update = { _ -> }
        )
    }
}

private object CookieManagerBridge {
    fun logSession(cookieManager: android.webkit.CookieManager) {
        val cookies = cookieManager.getCookie("https://www.tnebltd.gov.in")
        val fingerprint = cookies?.hashCode()?.toString(16) ?: "null"
        android.util.Log.d("BillStatusDiagnostic", "--- CAPTCHA SESSION DIAGNOSTICS ---\nCAPTCHA_SESSION_FINGERPRINT=$fingerprint")
    }
}

/**
 * JavaScript interface to bridge CAPTCHA, DOM-extracted JSON, and raw HTML to ViewModel.
 */
class AndroidCaptchaBridge(private val viewModel: TnebBillStatusViewModel) {
    @JavascriptInterface
    fun onCaptchaExtracted(base64Data: String) {
        viewModel.onCaptchaReceived(base64Data)
    }

    @JavascriptInterface
    fun onDiagnosticLog(log: String) {
        android.util.Log.d("BillStatusDiagnostic", "\n$log")
    }

    @JavascriptInterface
    fun onParsedDataReceived(json: String, submissionId: Int) {
        android.util.Log.d("TNEB", "[TNEB] onParsedDataReceived called with submissionId=$submissionId")
        viewModel.onParsedDataReceived(json, submissionId)
    }

    @JavascriptInterface
    fun onHtmlResponseReceived(html: String, submissionId: Int) {
        android.util.Log.d("TNEB", "[TNEB] onHtmlResponseReceived called with submissionId=$submissionId")
        viewModel.analyzeSubmissionResponse(html, submissionId)
    }
}

/**
 * Creates and remembers a WebView, cleaning it up when it leaves the composition.
 */
@Composable
fun rememberWebViewWithLifecycle(): WebView {
    val context = androidx.compose.ui.platform.LocalContext.current
    val webView = remember {
        WebView(context)
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    return webView
}
