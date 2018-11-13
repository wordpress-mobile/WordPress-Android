package org.wordpress.android.ui.sitecreation

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress

class NewSiteCreationPreviewFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    private val url: String = "https://leweo7test.wordpress.com"
    private val urlShort: String = "leweo7test.wordpress.com"

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_preview_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        val sitePreviewWebView = rootView.findViewById<WebView>(R.id.sitePreviewWebView)
        sitePreviewWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
               return true
            }
        }
        sitePreviewWebView.loadUrl(url)
        val sitePreviewWebUrlTitle = rootView.findViewById<TextView>(R.id.sitePreviewWebUrlTitle)
        sitePreviewWebUrlTitle.text = urlShort
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
    }

    companion object {
        const val TAG = "site_creation_preview_fragment_tag"
    }
}
