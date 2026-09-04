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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TnebBillStatusWebView(
    viewModel: TnebBillStatusViewModel,
    modifier: Modifier = Modifier
) {
    // Keep a single instance of WebView across recompositions
    val webView = rememberWebViewWithLifecycle()
    val reloadTrigger by viewModel.reloadTrigger.collectAsState()
    var lastTrigger by remember { mutableIntStateOf(reloadTrigger) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                webView.apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    // Security: We do not disable SSL validation.

                    addJavascriptInterface(
                        AndroidCaptchaBridge(viewModel),
                        "AndroidCaptchaBridge"
                    )

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            super.onPageFinished(view, url)
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
                                            // Handle cross-origin issues or errors silently as per security rules
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

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                viewModel.onPageLoadError()
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: SslErrorHandler?,
                            error: SslError?
                        ) {
                            // STRICT SSL RULE: Do NOT call handler.proceed().
                            // The user explicitly requested to reject invalid/expired certificates.
                            handler?.cancel()
                            viewModel.onPageLoadError()
                        }
                    }

                    loadUrl("https://www.tnebltd.gov.in/BillStatus/billstatus.xhtml")
                }
            },
            update = { view ->
                // Reload the WebView exactly once per user refresh action
                if (reloadTrigger > lastTrigger) {
                    lastTrigger = reloadTrigger
                    view.reload()
                }
            }
        )
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

/**
 * JavaScript interface to pass the CAPTCHA base64 string to the ViewModel.
 */
class AndroidCaptchaBridge(private val viewModel: TnebBillStatusViewModel) {
    @JavascriptInterface
    fun onCaptchaExtracted(base64: String) {
        viewModel.onCaptchaReceived(base64)
    }
}
