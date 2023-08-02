package org.wordpress.android.support

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import androidx.activity.result.contract.ActivityResultContract
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.DEFAULT_DOMAIN
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.support.SupportWebViewActivity.OpenChatWidget.ChatDetails
import org.wordpress.android.ui.WPWebViewActivity

class SupportWebViewActivity : WPWebViewActivity(), SupportWebViewClient.SupportWebViewClientListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toggleNavbarVisibility(false)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .addPathHandler("/res/", ResourcesPathHandler(this))
            .build()

        configureWebView()
        mWebView.webViewClient = SupportWebViewClient(this, assetLoader)
        mWebView.loadUrl("https://$DEFAULT_DOMAIN/assets/support_chat_widget.html")
    }

    override fun configureWebView() {
        super.configureWebView()
        supportActionBar?.hide()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't want any menu items
        return true
    }

    override fun onChatSessionClosed() {
        setResult(RESULT_OK, intent)
        finish()
    }

    class OpenChatWidget : ActivityResultContract<ChatDetails, ChatCompletionEvent?>() {
        override fun createIntent(context: Context, input: ChatDetails) =
            Intent(context, SupportWebViewActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(URL_TO_LOAD, input.url)
//                putExtra(CHAT_TEXT, input.chatText)
                putExtra(CHAT_EMAIL, input.site?.email)
            }

        override fun parseResult(resultCode: Int, intent: Intent?): ChatCompletionEvent? {
            val data = intent?.takeIf { it.hasExtra(CHAT_TEXT) && it.hasExtra(CHAT_EMAIL) }
            if (resultCode == RESULT_OK && data != null) {
                val chatText = data.getStringExtra(CHAT_TEXT).orEmpty()
                val email = data.getStringExtra(CHAT_EMAIL).orEmpty()
                return ChatCompletionEvent(chatText, email)
            }
            return null
        }

        data class ChatDetails(val site: SiteModel?, val url: String)

        companion object {
            const val CHAT_TEXT = "CHAT_TEXT"
            const val CHAT_EMAIL = "CHAT_EMAIL"
        }
    }

    data class ChatCompletionEvent(val chatText: String, val email: String)
}
