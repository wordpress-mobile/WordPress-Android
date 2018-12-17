package org.wordpress.android.ui.sitecreation

import android.annotation.SuppressLint
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.sitecreation.PreviewWebViewClient.PageFullyLoadedListener
import org.wordpress.android.util.URLFilteredWebViewClient
import javax.inject.Inject

class NewSiteCreationPreviewFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>(),
        PageFullyLoadedListener {
    private val url: String = "https://en.blog.wordpress.com"
    private val urlShort: String = "en.blog.wordpress.com"
    private val subdomainSpan: Pair<Int, Int> = Pair(0, "en.blog".length)
    private val domainSpan: Pair<Int, Int> = Pair(Math.min(subdomainSpan.second, urlShort.length), urlShort.length)

    private lateinit var viewModel: NewSitePreviewViewModel

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_preview_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        val sitePreviewWebView = rootView.findViewById<WebView>(R.id.sitePreviewWebView)
        sitePreviewWebView.webViewClient = PreviewWebViewClient(this, url)
        sitePreviewWebView.loadUrl(url)
        val sitePreviewWebUrlTitle = rootView.findViewById<TextView>(R.id.sitePreviewWebUrlTitle)
        sitePreviewWebUrlTitle.text = createSpannableUrl(rootView.context, urlShort, subdomainSpan, domainSpan)

        initViewModel()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(NewSitePreviewViewModel::class.java)
        viewModel.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
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

    override fun onPageFullyLoaded() {
        val view = view
        if (view != null) {
            // TODO go through VM
            hideGetStartedBar(view.findViewById(R.id.sitePreviewWebView))
        }
    }

    // Hacky solution to https://github.com/wordpress-mobile/WordPress-Android/issues/8233
    // Ideally we would hide "get started" bar on server side
    @SuppressLint("SetJavaScriptEnabled")
    private fun hideGetStartedBar(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        val javascript = "document.querySelector('html').style.cssText += '; margin-top: 0 !important;';\n" + "document.getElementById('wpadminbar').style.display = 'none';\n"

        webView.evaluateJavascript(
                javascript
        ) { webView.settings.javaScriptEnabled = false }
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    override fun getScreenTitle(): String {
        val arguments = arguments
        if (arguments == null || !arguments.containsKey(EXTRA_SCREEN_TITLE)) {
            throw IllegalStateException("Required argument screen title is missing.")
        }
        return arguments.getString(EXTRA_SCREEN_TITLE)
    }

    companion object {
        const val TAG = "site_creation_preview_fragment_tag"

        fun newInstance(screenTitle: String): NewSiteCreationPreviewFragment {
            val fragment = NewSiteCreationPreviewFragment()
            val bundle = Bundle()
            bundle.putString(NewSiteCreationBaseFormFragment.EXTRA_SCREEN_TITLE, screenTitle)
            fragment.arguments = bundle
            return fragment
        }
    }
}

private class PreviewWebViewClient internal constructor(
    val pageLoadedListener: PageFullyLoadedListener,
    siteAddress: String
) : URLFilteredWebViewClient(siteAddress) {
    interface PageFullyLoadedListener {
        fun onPageFullyLoaded()
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        pageLoadedListener.onPageFullyLoaded()
    }
}
