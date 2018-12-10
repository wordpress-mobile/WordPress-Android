package org.wordpress.android.ui.sitecreation

import android.content.Context
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import org.wordpress.android.R

class NewSiteCreationPreviewFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    private val url: String = "https://en.blog.wordpress.com"
    private val urlShort: String = "en.blog.wordpress.com"
    private val subdomainSpan: Pair<Int, Int> = Pair(0, "en.blog".length)
    private val domainSpan: Pair<Int, Int> = Pair(Math.min(subdomainSpan.second, urlShort.length), urlShort.length)

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
        sitePreviewWebUrlTitle.text = createSpannableUrl(rootView.context, urlShort, subdomainSpan, domainSpan)
    }

    /**
     * Creates a spannable url with 2 different text colors for the subdomain and domain.
     *
     * @param context Context to get the color resources
     * @param url The url to be used to created the spannable from
     * @param subdomainSpan The subdomain index pair for the start and end positions
     * @param domainSpan The domain index pair for the start and end positions
     *
     * @return [Spannable] styled with two different colors for the subdomain and domain parts
     */
    private fun createSpannableUrl(
        context: Context,
        url: String,
        subdomainSpan: Pair<Int, Int>,
        domainSpan: Pair<Int, Int>
    ): Spannable {
        val spannableTitle = SpannableString(url)
        spannableTitle.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.wp_grey_dark)),
                subdomainSpan.first,
                subdomainSpan.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableTitle.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.wp_grey_darken_20)),
                domainSpan.first,
                domainSpan.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableTitle
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

    companion object {
        const val TAG = "site_creation_preview_fragment_tag"

        fun newInstance(): NewSiteCreationPreviewFragment {
            return NewSiteCreationPreviewFragment()
        }
    }
}
