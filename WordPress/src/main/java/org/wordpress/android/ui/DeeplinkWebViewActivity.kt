package org.wordpress.android.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.LoginMagicLinkInterceptActivity

class DeeplinkWebViewActivity : WebViewActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val url = savedInstanceState?.getString(URL_KEY) ?: intent.getStringExtra(URL_KEY)
        ?: throw IllegalArgumentException("Missing URL parameter")
        super.onCreate(savedInstanceState)
        loadUrl(url)
        setTitle(R.string.app_name)
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    override fun configureWebView() {
        mWebView.settings.javaScriptEnabled = true
        mWebView.settings.domStorageEnabled = true
        mWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request?.url?.scheme == "wordpress" && request.url?.host == "magic-login") {
                    // LoginMagicLinkInterceptActivity
                    val intent = Intent(this@DeeplinkWebViewActivity, LoginMagicLinkInterceptActivity::class.java)
                    intent.data = request.url
                    intent.action = Intent.ACTION_VIEW
                    startActivity(intent)
                    finish()
                    return true
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
    }

    companion object {
        private const val URL_KEY = "url_key"
        fun openUrl(context: Activity, url: String) {
            val intent = Intent(context, DeeplinkWebViewActivity::class.java)
            intent.putExtra(URL_KEY, url)
            context.startActivity(intent)
        }
    }
}

