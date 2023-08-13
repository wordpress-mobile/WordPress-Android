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
import androidx.core.view.isVisible
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.DEFAULT_DOMAIN
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.utils.toMap
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.widgets.WPSnackbar
import javax.inject.Inject

@AndroidEntryPoint
class SupportWebViewActivity : WPWebViewActivity(), SupportWebViewClient.SupportWebViewClientListener {
    @Inject
    lateinit var zendeskHelper: ZendeskHelper

    private val originFromExtras by lazy {
        intent.extras?.getSerializableCompat<HelpActivity.Origin>(ORIGIN_KEY) ?: HelpActivity.Origin.UNKNOWN
    }
    private val extraTagsFromExtras by lazy {
        intent.extras?.getStringArrayList(EXTRA_TAGS_KEY)
    }
    private val selectedSiteFromExtras by lazy {
        intent.extras?.getSerializableCompat<SiteModel>(WordPress.SITE)
    }

    lateinit var progress: ViewGroup

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

        setupLoadingIndicator()
        setupWebView()
        setupJsInterfaceForWebView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't want any menu items
        return true
    }

    override fun onSupportTapped(chatHistory: String) {
        zendeskHelper.requireIdentity(this, selectedSiteFromExtras) {
            progress.isVisible = true
            val description = zendeskHelper.parseChatHistory(chatHistory)
            createNewZendeskRequest(description) {
                progress.isVisible = false
                showTicketMessage()
            }
        }
    }

    private fun setupLoadingIndicator() {
        progress = findViewById(R.id.progress_layout)
        progress.findViewById<MaterialTextView>(R.id.progress_text).apply {
            setText(R.string.contact_support_bot_ticket_loading)
            isVisible = true
        }
    }

    private fun createNewZendeskRequest(description: String, complete: () -> Unit) {
        val callback = object : ZendeskHelper.CreateRequestCallback() {
            override fun onSuccess() {
                complete()
            }

            override fun onError() {
                complete()
            }
        }

        zendeskHelper.createRequest(
            this,
            originFromExtras,
            selectedSiteFromExtras,
            extraTagsFromExtras,
            description,
            callback
        )
    }

    private fun showTicketMessage() {
        WPSnackbar.make(
            findViewById(R.id.webview_wrapper),
            R.string.contact_support_bot_ticket_message,
            Snackbar.LENGTH_LONG
        ).apply {
            setAction(R.string.contact_support_bot_ticket_button) {
                showZendeskTickets()
                finish()
            }
            show()
        }
    }

    private fun showZendeskTickets() {
        zendeskHelper.showAllTickets(this, originFromExtras, selectedSiteFromExtras, extraTagsFromExtras)
    }

    private fun setupWebView() {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", AssetsPathHandler(this))
            .addPathHandler("/res/", ResourcesPathHandler(this))
            .build()
        mWebView.webViewClient = SupportWebViewClient(this, assetLoader)

        // Setup debugging; See https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
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
            onSupportTapped(message)
        }
    }

    class OpenChatWidget : ActivityResultContract<OpenChatWidget.Args, ChatCompletionEvent?>() {
        override fun createIntent(context: Context, input: Args) =
            Intent(context, SupportWebViewActivity::class.java).apply {
                putExtra(USE_GLOBAL_WPCOM_USER, true)
                putExtra(AUTHENTICATION_URL, WPCOM_LOGIN_URL)
                putExtra(URL_TO_LOAD, buildURI(input.botOptions))
                putExtra(ORIGIN_KEY, input.origin)
                input.selectedSite?.let { site -> putExtra(WordPress.SITE, site) }
                input.extraSupportTags?.let { tags -> putStringArrayListExtra(EXTRA_TAGS_KEY, tags) }
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

        data class Args(
            val origin: HelpActivity.Origin,
            val selectedSite: SiteModel?,
            val extraSupportTags: ArrayList<String>?,
            val botOptions: BotOptions
        )

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

        companion object {
            const val CHAT_HISTORY = "CHAT_HISTORY"
        }
    }

    data class ChatCompletionEvent(val chatHistory: String)

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
        private const val EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY"
    }
}
