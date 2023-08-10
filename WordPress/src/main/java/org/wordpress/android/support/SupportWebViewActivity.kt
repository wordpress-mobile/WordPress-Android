package org.wordpress.android.support

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.activity.result.contract.ActivityResultContract
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.DEFAULT_DOMAIN
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.utils.toMap
import org.wordpress.android.support.SupportWebViewActivity.OpenChatWidget.Companion.CHAT_HISTORY
import org.wordpress.android.ui.WPWebViewActivity

class SupportWebViewActivity : WPWebViewActivity(), SupportWebViewClient.SupportWebViewClientListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)

        // set send message box to appear above system navigation bar
        val previewContainer = findViewById<View>(R.id.preview_container)
        val params = previewContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.margin_64dp)
        previewContainer.layoutParams = params

        supportActionBar?.title = getString(R.string.help)
        supportActionBar?.subtitle = ""

        setupWebView()
        setupJsInterfaceForWebView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't want any menu items
        return true
    }

    override fun onChatSessionClosed(chatHistory: String) {
        intent.putExtra(CHAT_HISTORY, chatHistory)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .addPathHandler("/res/", ResourcesPathHandler(this))
            .build()
        mWebView.webViewClient = SupportWebViewClient(this, assetLoader)

        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        if ( 0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        intent.getStringExtra(URL_TO_LOAD)?.let { mWebView.loadUrl(it) }
    }

    private fun setupJsInterfaceForWebView() {
        val jsObjName = "jsObject"
        val allowedOriginRules = setOf("https://$DEFAULT_DOMAIN")

        // Create a JS object to be injected into frames; Determines if WebMessageListener
        // or WebAppInterface should be used
        createJsObject(
            mWebView,
            jsObjName,
            allowedOriginRules
        ) { message ->
            Log.d("Chat history", message)
            onChatSessionClosed(message)
        }
    }


    class OpenChatWidget : ActivityResultContract<BotOptions, ChatCompletionEvent?>() {
        override fun createIntent(context: Context, input: BotOptions) =
            Intent(context, SupportWebViewActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(URL_TO_LOAD, buildURI(input))
            }

        override fun parseResult(resultCode: Int, intent: Intent?): ChatCompletionEvent? {
            val data = intent?.takeIf { it.hasExtra(CHAT_HISTORY) }
            if (resultCode == RESULT_OK && data != null) {
                val chatHistory = data.getStringExtra(CHAT_HISTORY).orEmpty()
                return ChatCompletionEvent(chatHistory)
            }
            return null
        }

        private fun buildURI(options: BotOptions): String {
            // build url with parameters.
            val botOptions = options.toMap()

            Log.d("BotOptions Map", botOptions.toString())

            val builder = Uri.Builder()
            builder.scheme("https")
                .authority(DEFAULT_DOMAIN)
                .appendPath("assets")
                .appendPath("support_chat_widget.html")

            botOptions.forEach { (key, value) ->
                builder.appendQueryParameter(key, value.toString())
            }

            return builder.build().toString()
        }

        companion object {
            const val CHAT_HISTORY = "CHAT_HISTORY"
        }
    }

    data class ChatCompletionEvent(val chatHistory: String)

    data class BotOptions(
        val id: String,
        val inputPlaceholder: String,
        val firstMessage: String,
        val getSupport: String,
        val suggestions: String,
        val questionOne: String,
        val questionTwo: String,
        val questionThree: String
    )
}
