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
import androidx.activity.viewModels
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import androidx.webkit.WebViewAssetLoader.DEFAULT_DOMAIN
import androidx.webkit.WebViewAssetLoader.ResourcesPathHandler
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableCompat
import org.wordpress.android.widgets.WPSnackbar
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SupportWebViewActivity : WPWebViewActivity(), SupportWebViewClient.SupportWebViewClientListener {
    @Inject
    lateinit var zendeskHelper: ZendeskHelper

    val viewModel: SupportWebViewActivityViewModel by viewModels()

    private val originFromExtras by lazy {
        intent.extras?.getSerializableCompat<HelpActivity.Origin>(ORIGIN_KEY) ?: HelpActivity.Origin.UNKNOWN
    }
    private val extraTagsFromExtras by lazy {
        intent.extras?.getStringArrayList(EXTRA_TAGS_KEY)
    }
    private val selectedSiteFromExtras by lazy {
        intent.extras?.getSerializableCompat<SiteModel>(WordPress.SITE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.start(chatIdProperty)
        toggleNavbarVisibility(false)

        // set send message box to appear above system navigation bar
        val previewContainer = findViewById<View>(R.id.preview_container)
        val params = previewContainer.layoutParams as ViewGroup.MarginLayoutParams
        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.margin_64dp)
        previewContainer.layoutParams = params

        supportActionBar?.title = getString(R.string.help)
        supportActionBar?.subtitle = ""
        window.navigationBarColor = getColor(R.color.docsbot_chat_container)

        setupWebView()
        setupJsInterfaceForWebView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // We don't want any menu items
        return true
    }

    override fun onSupportTapped(chatHistory: String) {
        zendeskHelper.requireIdentity(this, selectedSiteFromExtras) {
            showTicketCreatingMessage()

            val description = zendeskHelper.parseChatHistory(
                getString(R.string.contact_support_bot_ticket_comment_start),
                getString(R.string.contact_support_bot_ticket_comment_question),
                getString(R.string.contact_support_bot_ticket_comment_answer),
                chatHistory
            )
            createNewZendeskRequest(description, object : ZendeskHelper.CreateRequestCallback() {
                override fun onSuccess() {
                    showZendeskTickets()
                    ToastUtils.showToast(
                        this@SupportWebViewActivity,
                        R.string.contact_support_bot_ticket_message,
                        ToastUtils.Duration.LONG
                    )
                    viewModel.onTicketCreated()
                }

                override fun onError(errorMessage: String?) {
                    showTicketErrorMessage()
                    viewModel.onTicketCreationError(errorMessage)
                }
            })
        }
    }

    override fun onWebViewReceivedError() {
        super.onWebViewReceivedError()
        viewModel.onWebViewReceivedError()
    }

    private fun createNewZendeskRequest(description: String, callback: ZendeskHelper.CreateRequestCallback) {
        zendeskHelper.createRequest(
            this,
            originFromExtras,
            selectedSiteFromExtras,
            extraTagsFromExtras,
            description,
            callback
        )
    }

    private fun showTicketCreatingMessage() {
        WPSnackbar.make(
            findViewById(R.id.webview_wrapper),
            R.string.contact_support_bot_ticket_loading,
            Snackbar.LENGTH_INDEFINITE
        ).show()
    }

    private fun showTicketErrorMessage() {
        WPSnackbar.make(
            findViewById(R.id.webview_wrapper),
            R.string.contact_support_bot_ticket_error,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showZendeskTickets() {
        zendeskHelper.showAllTickets(this, originFromExtras, selectedSiteFromExtras, extraTagsFromExtras)
        finish()
    }

    override fun onRedirectToExternalBrowser(url: String) {
        ActivityLauncher.openUrlExternal(this, url)
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

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
        private const val EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY"
        private val chatId = UUID.randomUUID().toString()
        private val chatIdProperty = mapOf("chat_id" to chatId)

        @JvmStatic
        fun createIntent(
            context: Context,
            origin: HelpActivity.Origin,
            selectedSite: SiteModel?,
            extraSupportTags: ArrayList<String>?
        ) = Intent(context, SupportWebViewActivity::class.java).apply {
            putExtra(URL_TO_LOAD, buildURI(context))
            putExtra(ORIGIN_KEY, origin)
            selectedSite?.let { site -> putExtra(WordPress.SITE, site) }
            extraSupportTags?.let { tags -> putStringArrayListExtra(EXTRA_TAGS_KEY, tags) }
        }

        private fun buildURI(context: Context) = Uri.Builder().scheme("https")
            .authority(DEFAULT_DOMAIN)
            .appendPath("assets")
            .appendPath("support_chat_widget.html")
            .appendQueryParameter("id", BuildConfig.DOCSBOTAI_ID)
            .appendQueryParameter("chatId", chatId)
            .appendQueryParameter("inputPlaceholder", context.getString(R.string.contact_support_input_placeholder))
            .appendQueryParameter("firstMessage", context.getString(R.string.contact_support_first_message))
            .appendQueryParameter("getSupport", context.getString(R.string.contact_support_get_support))
            .appendQueryParameter("suggestions", context.getString(R.string.contact_support_suggestions))
            .appendQueryParameter("questionOne", context.getString(R.string.contact_support_question_one))
            .appendQueryParameter("questionTwo", context.getString(R.string.contact_support_question_two))
            .appendQueryParameter("questionThree", context.getString(R.string.contact_support_question_three))
            .appendQueryParameter("questionFour", context.getString(R.string.contact_support_question_four))
            .appendQueryParameter("questionFive", context.getString(R.string.contact_support_question_five))
            .appendQueryParameter("questionSix", context.getString(R.string.contact_support_question_six))
            .build()
            .toString()
    }
}
