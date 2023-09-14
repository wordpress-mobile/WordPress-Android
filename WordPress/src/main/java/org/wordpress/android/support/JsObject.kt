package org.wordpress.android.support

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

// Create a handler that runs on the UI thread
private val handler: Handler = Handler(Looper.getMainLooper())

/**
 * Injects a JavaScript object which supports a {@code postMessage()} method.
 * A feature check is used to determine if the preferred API, WebMessageListener, is supported.
 * If it is, then WebMessageListener will be used to create a JavaScript object. The object will be
 * injected into all of the frames that have an origin matching those in {@code allowedOriginRules}.
 * <p>
 * If WebMessageListener is not supported then the method will defer to using JavascriptInterface
 * to create the JavaScript object.
 * <p>
 * The {@code postMessage()} methods in the Javascript objects created by WebMessageListener and
 * JavascriptInterface both make calls to the same callback, {@code onMessageReceived()}.
 * In this case, the callback invokes native Android sharing.
 * <p>
 * The WebMessageListener invokes callbacks on the UI thread by default. However,
 * JavascriptInterface invokes callbacks on a background thread by default. In order to
 * guarantee thread safety and that the caller always gets consistent behavior the the callback
 * should always be called on the UI thread. To change the default behavior of JavascriptInterface,
 * the callback is wrapped in a handler which will tell it to run on the UI thread instead of the default
 * background thread it would otherwise be invoked on.
 * <p>
 * @param webview the component that WebMessageListener or JavascriptInterface will be added to
 * @param jsObjName the name that will be given to the Javascript objects created by either
 *        WebMessageListener or JavascriptInterface
 * @param allowedOriginRules a set of origins used only by WebMessageListener, if a frame matches an
 * origin in this set then it will have the JS object injected into it
 * @param onMessageReceived invoked on UI thread with message passed in from JavaScript postMessage() call
 */
fun createJsObject(
    webview: WebView,
    jsObjName: String,
    allowedOriginRules: Set<String>,
    onMessageReceived: (message: String) -> Unit
) {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
        WebViewCompat.addWebMessageListener(
            webview, jsObjName, allowedOriginRules
        ) { _, message, _, _, _ -> onMessageReceived(message.data!!) }
    } else {
        webview.addJavascriptInterface(object {
            @JavascriptInterface
            fun postMessage(message: String) {
                // Use the handler to invoke method on UI thread
                handler.post { onMessageReceived(message) }
            }
        }, jsObjName)
    }
}

