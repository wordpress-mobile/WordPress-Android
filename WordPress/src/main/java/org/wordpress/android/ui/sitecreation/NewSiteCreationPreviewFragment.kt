package org.wordpress.android.ui.sitecreation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.arch.lifecycle.Observer
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
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import kotlinx.android.synthetic.main.new_site_creation_preview_screen.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState
import org.wordpress.android.ui.sitecreation.NewSitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.ui.sitecreation.PreviewWebViewClient.PageFullyLoadedListener
import org.wordpress.android.ui.sitecreation.creation.NewSiteCreationService
import org.wordpress.android.ui.sitecreation.creation.SitePreviewScreenListener
import org.wordpress.android.util.AutoForeground.ServiceEventConnection
import org.wordpress.android.util.URLFilteredWebViewClient
import javax.inject.Inject

private const val ARG_DATA = "arg_site_creation_data"
private const val SLIDE_IN_ANIMATION_DURATION = 450L

class NewSiteCreationPreviewFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>(),
        PageFullyLoadedListener {
    /**
     * We need to connect to the service, so the service knows when the app is in the background. The service
     * automatically shows system notifications when site creation is in progress and the app is in the background.
     */
    private var serviceEventConnection: ServiceEventConnection? = null

    private lateinit var viewModel: NewSitePreviewViewModel

    private lateinit var fullscreenErrorLayout: ViewGroup
    private lateinit var fullscreenProgressLayout: ViewGroup
    private lateinit var contentLayout: ViewGroup
    private lateinit var sitePreviewWebView: WebView
    private lateinit var sitePreviewWebUrlTitle: TextView

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var sitePreviewScreenListener: SitePreviewScreenListener
    private lateinit var helpClickedListener: OnHelpClickedListener

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is SitePreviewScreenListener) {
            throw IllegalStateException("Parent activity must implement SitePreviewScreenListener.")
        }
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
        sitePreviewScreenListener = context
        helpClickedListener = context
    }

    override fun onResume() {
        super.onResume()
        serviceEventConnection = ServiceEventConnection(context, NewSiteCreationService::class.java, viewModel)
    }

    override fun onPause() {
        super.onPause()
        serviceEventConnection?.disconnect(context, viewModel)
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.new_site_creation_preview_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        fullscreenErrorLayout = rootView.findViewById(R.id.error_layout)
        fullscreenProgressLayout = rootView.findViewById(R.id.progress_layout)
        contentLayout = rootView.findViewById(R.id.content_layout)
        sitePreviewWebView = rootView.findViewById(R.id.sitePreviewWebView)
        sitePreviewWebUrlTitle = rootView.findViewById(R.id.sitePreviewWebUrlTitle)
        initViewModel()
        initRetryButton()
        initOkButton()
        initCancelWizardButton()
        initContactSupportButton()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(NewSitePreviewViewModel::class.java)
        viewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                when (uiState) {
                    is SitePreviewContentUiState -> updateContentLayout(uiState)
                    is SitePreviewFullscreenProgressUiState -> updateLoadingLayout(uiState)
                    is SitePreviewFullscreenErrorUiState -> updateErrorLayout(uiState)
                }
                updateVisibility(fullscreenProgressLayout, uiState.fullscreenProgressLayoutVisibility)
                updateVisibility(contentLayout, uiState.contentLayoutVisibility)
                updateVisibility(fullscreenErrorLayout, uiState.fullscreenErrorLayoutVisibility)
            }
        })
        viewModel.preloadPreview.observe(this, Observer { url ->
            url?.let {
                sitePreviewWebView.webViewClient = PreviewWebViewClient(this@NewSiteCreationPreviewFragment, url)
                sitePreviewWebView.loadUrl(url)
            }
        })
        viewModel.hideGetStartedBar.observe(this, Observer<Unit> {
            hideGetStartedBar(sitePreviewWebView)
        })
        viewModel.startCreateSiteService.observe(this, Observer { startServiceData ->
            startServiceData?.let {
                NewSiteCreationService.createSite(
                        activity!!,
                        startServiceData.previousState,
                        startServiceData.serviceData
                )
            }
        })
        viewModel.onHelpClicked.observe(this, Observer {
            helpClickedListener.onHelpClicked(HelpActivity.Origin.SITE_CREATION_CREATING)
        })
        viewModel.onOkButtonClicked.observe(this, Observer { createSiteState ->
            createSiteState?.let {
                sitePreviewScreenListener.onSitePreviewScreenDismissed(createSiteState)
            }
        })
        viewModel.onCancelWizardClicked.observe(this, Observer { createSiteState ->
            createSiteState?.let {
                sitePreviewScreenListener.onSitePreviewScreenDismissed(createSiteState)
            }
        })

        viewModel.start(arguments!![ARG_DATA] as SiteCreationState)
    }

    private fun initRetryButton() {
        val retryBtn = fullscreenErrorLayout.findViewById<View>(R.id.error_retry)
        retryBtn.setOnClickListener { viewModel.retry() }
    }

    private fun initContactSupportButton() {
        val contactSupport = fullscreenErrorLayout.findViewById<View>(R.id.contact_support)
        contactSupport.setOnClickListener { viewModel.onHelpClicked() }
    }

    private fun initCancelWizardButton() {
        val retryBtn = fullscreenErrorLayout.findViewById<View>(R.id.cancel_wizard_button)
        retryBtn.setOnClickListener { viewModel.onCancelWizardClicked() }
    }

    private fun initOkButton() {
        val okBtn = contentLayout.findViewById<View>(R.id.okButton)
        okBtn.setOnClickListener { viewModel.onOkButtonClicked() }
    }

    private fun updateContentLayout(uiState: SitePreviewContentUiState) {
        uiState.data.apply {
            sitePreviewWebUrlTitle.text = createSpannableUrl(activity!!, shortUrl, subDomainIndices, domainIndices)
        }
        // The view is about to become visible
        if (contentLayout.visibility == View.GONE) {
            animateContentTransition()
        }
    }

    private fun updateLoadingLayout(progressUiState: SitePreviewFullscreenProgressUiState) {
        progressUiState.apply {
            setTextOrHide(fullscreenProgressLayout.findViewById(R.id.progress_text), loadingTextResId)
        }
    }

    private fun updateErrorLayout(errorUiStateState: SitePreviewFullscreenErrorUiState) {
        errorUiStateState.apply {
            setTextOrHide(fullscreenErrorLayout.findViewById(R.id.error_title), titleResId)
            setTextOrHide(fullscreenErrorLayout.findViewById(R.id.error_subtitle), subtitleResId)
            updateVisibility(
                    fullscreenErrorLayout.findViewById(R.id.contact_support),
                    errorUiStateState.showContactSupport
            )
            updateVisibility(
                    fullscreenErrorLayout.findViewById(R.id.cancel_wizard_button),
                    errorUiStateState.showCancelWizardButton
            )
        }
    }

    private fun setTextOrHide(textView: TextView, resId: Int?) {
        textView.visibility = if (resId == null) View.GONE else View.VISIBLE
        resId?.let {
            textView.text = resources.getString(resId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
        if (savedInstanceState == null) {
            // we need to manually clear the NewSiteCreationService state so we don't for example receive sticky events
            // from the previous run of the SiteCreation flow.
            NewSiteCreationService.clearSiteCreationServiceState()
        }
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
        viewModel.onUrlLoaded()
    }

    // Hacky solution to https://github.com/wordpress-mobile/WordPress-Android/issues/8233
    // Ideally we would hide "get started" bar on server side
    @SuppressLint("SetJavaScriptEnabled")
    private fun hideGetStartedBar(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        val javascript = "document.querySelector('html').style.cssText += '; margin-top: 0 !important;';\n" +
                "document.getElementById('wpadminbar').style.display = 'none';\n"

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

    private fun updateVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun animateContentTransition() {
        contentLayout.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                if (contentLayout.measuredWidth > 0 && contentLayout.measuredHeight > 0) {
                    contentLayout.removeOnLayoutChangeListener(this)
                    val contentHeight = contentLayout.measuredHeight.toFloat()

                    val webViewAnim = createWebViewContainerAnimator(contentHeight)
                    val okContainerAnim = createOkButtonContainerAnimator(contentHeight)
                    val titleAnim = createTitleAnimator()
                    AnimatorSet().apply {
                        duration = SLIDE_IN_ANIMATION_DURATION
                        playTogether(webViewAnim, okContainerAnim, titleAnim)
                        start()
                    }
                }
            }
        })
    }

    private fun createWebViewContainerAnimator(contentHeight: Float): ObjectAnimator =
            createSlideInFromBottomAnimator(webviewContainer, contentHeight)

    private fun createOkButtonContainerAnimator(contentHeight: Float): ObjectAnimator =
            createSlideInFromBottomAnimator(sitePreviewOkButtonContainer, contentHeight)

    private fun createSlideInFromBottomAnimator(view: View, contentHeight: Float): ObjectAnimator {
        return ObjectAnimator.ofFloat(
                view,
                "translationY",
                // start below the bottom edge of the display
                (contentHeight - view.top),
                0f
        )
    }

    private fun createTitleAnimator() = ObjectAnimator.ofFloat(sitePreviewTitle, "alpha", 0f, 1f)

    companion object {
        const val TAG = "site_creation_preview_fragment_tag"

        fun newInstance(
            screenTitle: String,
            siteCreationData: SiteCreationState
        ): NewSiteCreationPreviewFragment {
            val fragment = NewSiteCreationPreviewFragment()
            val bundle = Bundle()
            bundle.putString(NewSiteCreationBaseFormFragment.EXTRA_SCREEN_TITLE, screenTitle)
            bundle.putParcelable(ARG_DATA, siteCreationData)
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
