package org.wordpress.android.editor.gutenberg

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class GutenbergView : WebView {
    var filePathCallback: ValueCallback<Array<Uri?>?>? = null
    val pickImageRequestCode = 1
    var onFileChooserRequested: ((Intent, Int) -> Unit)? = null

    var editorDidBecomeAvailable: ((GutenbergView) -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    @SuppressLint("SetJavaScriptEnabled")
    fun startWithDevServer() {
        this.settings.allowFileAccess = true
        this.settings.javaScriptCanOpenWindowsAutomatically = true
        this.settings.javaScriptEnabled = true

        this.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Log.e("GutenbergView", error.toString())
                super.onReceivedError(view, request, error)
            }
        }

        this.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                newFilePathCallback: ValueCallback<Array<Uri?>?>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback = newFilePathCallback
                val allowMultiple = fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                val mimeTypes = fileChooserParams?.acceptTypes

                val intent = Intent(Intent.ACTION_PICK).apply {
                    type = "*/*"  // Default to all types
                }

                if (!mimeTypes.isNullOrEmpty()) {
                    intent.type = mimeTypes.joinToString("|")
                }

                if (allowMultiple) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }

                onFileChooserRequested?.invoke(Intent.createChooser(intent, "Select Files"), pickImageRequestCode)
                return true
            }
        }

        this.loadUrl("http://192.168.1.81:5173/")
    }

    fun resetFilePathCallback() {
        filePathCallback = null
    }
}
