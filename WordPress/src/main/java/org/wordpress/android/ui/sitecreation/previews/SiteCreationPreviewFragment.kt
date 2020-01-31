package org.wordpress.android.ui.sitecreation.previews

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.webkit.WebView
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.android.synthetic.main.site_creation_preview_header_item.*
import kotlinx.android.synthetic.main.site_creation_preview_screen_default.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewData
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenErrorUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewFullscreenProgressUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.sitecreation.services.SiteCreationService
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.AutoForeground.ServiceEventConnection
import org.wordpress.android.util.ErrorManagedWebViewClient.ErrorManagedWebViewClientListener
import org.wordpress.android.util.URLFilteredWebViewClient
import org.wordpress.android.util.getColorFromAttribute
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val ARG_DATA = "arg_site_creation_data"
private const val SLIDE_IN_ANIMATION_DURATION = 450L
// 2 seconds for preview of each text in the site creation "loading" state:
private const val SITE_CREATION_PREVIEW_TEXT_DURATION_MS = 2000L

class SiteCreationPreviewFragment : SiteCreationBaseFormFragment(),
        ErrorManagedWebViewClientListener {
    /**
     * We need to connect to the service, so the service knows when the app is in the background. The service
     * automatically shows system notifications when site creation is in progress and the app is in the background.
     */
    private var serviceEventConnection: ServiceEventConnection? = null

    private lateinit var viewModel: SitePreviewViewModel

    private lateinit var fullscreenErrorLayout: ViewGroup
    private lateinit var fullscreenProgressLayout: ViewGroup
    private lateinit var contentLayout: ViewGroup
    private lateinit var sitePreviewWebView: WebView
    private lateinit var sitePreviewWebError: ViewGroup
    private lateinit var sitePreviewWebViewShimmerLayout: ShimmerFrameLayout
    private lateinit var sitePreviewWebUrlTitle: TextView

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers

    private lateinit var sitePreviewScreenListener: SitePreviewScreenListener
    private lateinit var helpClickedListener: OnHelpClickedListener

    private var okButtonContainer: View? = null

    // an instance helping sequence texts while in `loading` state
    private var textsProgressTextsHelper: SiteCreationTextsProgressHelper? = null

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
        serviceEventConnection = ServiceEventConnection(context, SiteCreationService::class.java, viewModel)
    }

    override fun onPause() {
        super.onPause()
        serviceEventConnection?.disconnect(context, viewModel)
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.site_creation_preview_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        fullscreenErrorLayout = rootView.findViewById(R.id.error_layout)
        fullscreenProgressLayout = rootView.findViewById(R.id.progress_layout)
        contentLayout = rootView.findViewById(R.id.content_layout)
        sitePreviewWebView = rootView.findViewById(R.id.sitePreviewWebView)
        sitePreviewWebError = rootView.findViewById(R.id.sitePreviewWebError)
        sitePreviewWebViewShimmerLayout = rootView.findViewById(R.id.sitePreviewWebViewShimmerLayout)
        sitePreviewWebUrlTitle = rootView.findViewById(R.id.sitePreviewWebUrlTitle)
        okButtonContainer = rootView.findViewById(R.id.sitePreviewOkButtonContainer)
        initViewModel()
        initRetryButton()
        initOkButton()
        initCancelWizardButton()
        initContactSupportButton()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SitePreviewViewModel::class.java)
        viewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                when (uiState) {
                    is SitePreviewContentUiState -> updateContentLayout(uiState.data)
                    is SitePreviewWebErrorUiState -> updateContentLayout(uiState.data)
                    is SitePreviewLoadingShimmerState -> updateContentLayout(uiState.data)
                    is SitePreviewFullscreenProgressUiState -> updateLoadingLayout(uiState)
                    is SitePreviewFullscreenErrorUiState -> updateErrorLayout(uiState)
                }
                uiHelpers.updateVisibility(fullscreenProgressLayout, uiState.fullscreenProgressLayoutVisibility)
                uiHelpers.updateVisibility(contentLayout, uiState.contentLayoutVisibility)
                uiHelpers.updateVisibility(sitePreviewWebView, uiState.webViewVisibility)
                uiHelpers.updateVisibility(sitePreviewWebError, uiState.webViewErrorVisibility)
                uiHelpers.updateVisibility(sitePreviewWebViewShimmerLayout, uiState.shimmerVisibility)
                uiHelpers.updateVisibility(fullscreenErrorLayout, uiState.fullscreenErrorLayoutVisibility)

                // special care required for the animated texts displayed while in loading state
                // which may be currently running the sequence:
                if (textsProgressTextsHelper != null && uiState !is SitePreviewFullscreenProgressUiState) {
                    textsProgressTextsHelper?.let {
                        // capture before using it
                        it.cancel()
                        textsProgressTextsHelper = null
                    }
                }
            }
        })
        viewModel.preloadPreview.observe(this, Observer { url ->
            url?.let {
                sitePreviewWebView.webViewClient = URLFilteredWebViewClient(url, this@SiteCreationPreviewFragment)
                sitePreviewWebView.loadUrl(url)
            }
        })
        viewModel.hideGetStartedBar.observe(this, Observer<Unit> {
            hideGetStartedBar(sitePreviewWebView)
        })
        viewModel.startCreateSiteService.observe(this, Observer { startServiceData ->
            startServiceData?.let {
                SiteCreationService.createSite(
                        requireNotNull(activity),
                        startServiceData.previousState,
                        startServiceData.serviceData
                )
            }
        })
        viewModel.onHelpClicked.observe(this, Observer {
            helpClickedListener.onHelpClicked(HelpActivity.Origin.SITE_CREATION_CREATING)
        })
        viewModel.onSiteCreationCompleted.observe(this, Observer {
            sitePreviewScreenListener.onSiteCreationCompleted()
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
        val cancelBtn = fullscreenErrorLayout.findViewById<View>(R.id.cancel_wizard_button)
        cancelBtn.setOnClickListener { viewModel.onCancelWizardClicked() }
    }

    private fun initOkButton() {
        val okBtn = contentLayout.findViewById<View>(R.id.okButton)
        okBtn.setOnClickListener { viewModel.onOkButtonClicked() }
    }

    private fun updateContentLayout(sitePreviewData: SitePreviewData) {
        sitePreviewData.apply {
            sitePreviewWebUrlTitle.text = createSpannableUrl(
                    requireNotNull(activity),
                    shortUrl,
                    subDomainIndices,
                    domainIndices
            )
        }
        // The view is about to become visible
        if (contentLayout.visibility == View.GONE) {
            animateContentTransition()
            view?.announceForAccessibility(
                    getString(R.string.new_site_creation_preview_title) +
                            getString(R.string.new_site_creation_site_preview_content_description)
            )
        }
    }

    private fun updateLoadingLayout(progressUiState: SitePreviewFullscreenProgressUiState) {
        progressUiState.apply {
            fullscreenProgressLayout.findViewById<TextSwitcher>(R.id.progress_text)?.apply {
                // create a progress helper and let it run
                SiteCreationTextsProgressHelper(WeakReference(this), loadingTextResIds).also {
                    textsProgressTextsHelper = it
                    postDelayed(it, it.delay)
                }
            }
        }
    }

    private fun updateErrorLayout(errorUiStateState: SitePreviewFullscreenErrorUiState) {
        errorUiStateState.apply {
            uiHelpers.setTextOrHide(fullscreenErrorLayout.findViewById(R.id.error_title), titleResId)
            uiHelpers.setTextOrHide(fullscreenErrorLayout.findViewById(R.id.error_subtitle), subtitleResId)
            uiHelpers.updateVisibility(
                    fullscreenErrorLayout.findViewById(R.id.contact_support),
                    errorUiStateState.showContactSupport
            )
            uiHelpers.updateVisibility(
                    fullscreenErrorLayout.findViewById(R.id.cancel_wizard_button),
                    errorUiStateState.showCancelWizardButton
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireNotNull(activity).application as WordPress).component().inject(this)
        if (savedInstanceState == null) {
            // we need to manually clear the SiteCreationService state so we don't for example receive sticky events
            // from the previous run of the SiteCreation flow.
            SiteCreationService.clearSiteCreationServiceState()
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
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.neutral_80)),
                subdomainSpan.first,
                subdomainSpan.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableTitle.setSpan(
                ForegroundColorSpan(context.getColorFromAttribute(R.attr.wpColorTextSubtle)),
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

    override fun onWebViewPageLoaded() {
        viewModel.onUrlLoaded()
    }

    override fun onWebViewReceivedError() {
        viewModel.onWebViewError()
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
        viewModel.onHelpClicked()
    }

    override fun getScreenTitle(): String {
        return arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")
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

                    val titleAnim = createFadeInAnimator(sitePreviewTitle)
                    val webViewAnim = createSlideInFromBottomAnimator(webviewContainer, contentHeight)
                    // OK button should slide in if the container exists and fade in otherwise
                    val okAnim = okButtonContainer?.let { createSlideInFromBottomAnimator(it, contentHeight) }
                            ?: createFadeInAnimator(okButton)
                    AnimatorSet().apply {
                        interpolator = DecelerateInterpolator()
                        duration = SLIDE_IN_ANIMATION_DURATION
                        playTogether(titleAnim, webViewAnim, okAnim)
                        start()
                    }
                }
            }
        })
    }

    private fun createSlideInFromBottomAnimator(view: View, contentHeight: Float): ObjectAnimator {
        return ObjectAnimator.ofFloat(
                view,
                "translationY",
                // start below the bottom edge of the display
                (contentHeight - view.top),
                0f
        )
    }

    private fun createFadeInAnimator(view: View) = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

    /**
     * A simple helper to switch texts used to display progress of site being created.
     * The helper is a runnable and one started (externally) will keep running until cancelled
     * Each run changes the text to the next one in the sequence (stringIds) in a cyclic manner
     *
     * using a WeakReference for the text view will prevent future executions once the view is
     * destroyed (or at least no longer referenced by fullscreenProgressLayout)
     *
     * @param textSwitcher - a week-ref to a text-switcher which displays the sequence
     * @param stringIds - a list of string resource ids to sequence
     * @param delay - how long to delay the next text-change (i.e. how long is each text displayed)
     */
    private inner class SiteCreationTextsProgressHelper(
        val textSwitcher: WeakReference<TextSwitcher>,
        val stringIds: List<Int>,
        val delay: Long = SITE_CREATION_PREVIEW_TEXT_DURATION_MS
    ) : Runnable {
        val nText: Int = stringIds.size

        // the count of how many times `nextText` was executed
        var count = -1

        var canceled = false

        init {
            textSwitcher.get()?.apply {
                removeAllViews()
                visibility = View.VISIBLE
                setFactory {
                    // inflate a text view that matches the design for this screen
                    LayoutInflater.from(context)
                            .inflate(R.layout.site_creation_progress_text, this, false)
                }
                // set fade in/out animations
                inAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                outAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_out)
                // to avoid a cross-over effect - the `in` animation should wait till the `out` animation ends
                inAnimation.startOffset = outAnimation.duration

                inAnimation.setAnimationListener(object : AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        // if still running - post this change-text action again in `delay` ms
                        if (!canceled) {
                            postDelayed(this@SiteCreationTextsProgressHelper, delay)
                        }
                    }

                    override fun onAnimationStart(animation: Animation?) {
                    }
                })
                // now set the first text immediately
                setCurrentText(nextText())
            }
        }

        fun cancel() {
            canceled = true
            textSwitcher.get()?.apply {
                removeCallbacks(this@SiteCreationTextsProgressHelper)
                visibility = View.GONE
            }
        }

        override fun run() {
            if (canceled) {
                return
            }
            // update to the next text
            textSwitcher.get()?.apply {
                setText(nextText())

            }
        }

        private fun nextText(): CharSequence? {
            // progress with the count and get the next text
            count++
            return textSwitcher.get()?.context?.resources?.getString(stringIds[count % nText])
        }
    }

    companion object {
        const val TAG = "site_creation_preview_fragment_tag"

        fun newInstance(
            screenTitle: String,
            siteCreationData: SiteCreationState
        ): SiteCreationPreviewFragment {
            val fragment = SiteCreationPreviewFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            bundle.putParcelable(ARG_DATA, siteCreationData)
            fragment.arguments = bundle
            return fragment
        }
    }
}
