package org.wordpress.android.ui.sitecreation.previews

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.FullscreenErrorWithRetryBinding
import org.wordpress.android.databinding.SiteCreationFormScreenBinding
import org.wordpress.android.databinding.SiteCreationPreviewScreenBinding
import org.wordpress.android.databinding.SiteCreationPreviewScreenDefaultBinding
import org.wordpress.android.databinding.SiteCreationProgressCreatingSiteBinding
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
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AutoForeground.ServiceEventConnection
import org.wordpress.android.util.ErrorManagedWebViewClient.ErrorManagedWebViewClientListener
import org.wordpress.android.util.URLFilteredWebViewClient
import javax.inject.Inject

private const val ARG_DATA = "arg_site_creation_data"
private const val SLIDE_IN_ANIMATION_DURATION = 450L

@AndroidEntryPoint
class SiteCreationPreviewFragment : SiteCreationBaseFormFragment(),
        ErrorManagedWebViewClientListener {
    /**
     * We need to connect to the service, so the service knows when the app is in the background. The service
     * automatically shows system notifications when site creation is in progress and the app is in the background.
     */
    private var serviceEventConnection: ServiceEventConnection? = null
    private val viewModel: SitePreviewViewModel by viewModels()
    private var animatorSet: AnimatorSet? = null
    private val isLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    @Inject internal lateinit var uiHelpers: UiHelpers

    private var binding: SiteCreationPreviewScreenBinding? = null

    @Suppress("UseCheckOrError")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is SitePreviewScreenListener) {
            throw IllegalStateException("Parent activity must implement SitePreviewScreenListener.")
        }
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            // we need to manually clear the SiteCreationService state so we don't for example receive sticky events
            // from the previous run of the SiteCreation flow.
            SiteCreationService.clearSiteCreationServiceState()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        viewModel.start(requireArguments()[ARG_DATA] as SiteCreationState, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        viewModel.start(requireArguments()[ARG_DATA] as SiteCreationState, savedInstanceState)
    }

    override fun getContentLayout(): Int {
        return R.layout.site_creation_preview_screen
    }

    @Suppress("UseCheckOrError") override val screenTitle: String
        get() = arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")

    override fun setBindingViewStubListener(parentBinding: SiteCreationFormScreenBinding) {
        parentBinding.siteCreationFormContentStub.setOnInflateListener { _, inflated ->
            binding = SiteCreationPreviewScreenBinding.bind(inflated)
        }
    }

    override fun setupContent() {
        binding?.siteCreationPreviewScreenDefault?.initViewModel()
        binding?.siteCreationPreviewScreenDefault?.fullscreenErrorWithRetry?.initRetryButton()
        binding?.siteCreationPreviewScreenDefault?.initOkButton()
        binding?.siteCreationPreviewScreenDefault?.fullscreenErrorWithRetry?.initCancelWizardButton()
        binding?.siteCreationPreviewScreenDefault?.fullscreenErrorWithRetry?.initContactSupportButton()
    }

    private fun SiteCreationPreviewScreenDefaultBinding.initViewModel() {
        viewModel.uiState.observe(this@SiteCreationPreviewFragment, { uiState ->
            uiState?.let {
                when (uiState) {
                    is SitePreviewContentUiState -> updateContentLayout(uiState.data)
                    is SitePreviewWebErrorUiState -> updateContentLayout(uiState.data)
                    is SitePreviewLoadingShimmerState -> updateContentLayout(uiState.data)
                    is SitePreviewFullscreenProgressUiState ->
                        siteCreationProgressCreatingSite.updateLoadingLayout(uiState)
                    is SitePreviewFullscreenErrorUiState ->
                        fullscreenErrorWithRetry.updateErrorLayout(uiState)
                }
                uiHelpers.updateVisibility(
                        siteCreationProgressCreatingSite.progressLayout,
                        uiState.fullscreenProgressLayoutVisibility
                )

                uiHelpers.updateVisibility(contentLayout, uiState.contentLayoutVisibility)
                uiHelpers.updateVisibility(
                        siteCreationPreviewWebViewContainer.sitePreviewWebView,
                        uiState.webViewVisibility
                )
                uiHelpers.updateVisibility(
                        siteCreationPreviewWebViewContainer.sitePreviewWebError,
                        uiState.webViewErrorVisibility
                )
                uiHelpers.updateVisibility(
                        siteCreationPreviewWebViewContainer.sitePreviewWebViewShimmerLayout,
                        uiState.shimmerVisibility
                )
                uiHelpers.updateVisibility(
                        fullscreenErrorWithRetry.errorLayout,
                        uiState.fullscreenErrorLayoutVisibility
                )
            }
        })

        viewModel.preloadPreview.observe(this@SiteCreationPreviewFragment, { url ->
            url?.let { urlString ->
                siteCreationPreviewWebViewContainer.sitePreviewWebView.webViewClient =
                        URLFilteredWebViewClient(urlString, this@SiteCreationPreviewFragment)
                siteCreationPreviewWebViewContainer.sitePreviewWebView.settings.userAgentString =
                        WordPress.getUserAgent()
                siteCreationPreviewWebViewContainer.sitePreviewWebView.loadUrl(urlString)
            }
        })

        viewModel.startCreateSiteService.observe(this@SiteCreationPreviewFragment, { startServiceData ->
            startServiceData?.let {
                SiteCreationService.createSite(
                        requireNotNull(activity),
                        startServiceData.previousState,
                        startServiceData.serviceData
                )
            }
        })

        initClickObservers()
    }

    private fun initClickObservers() {
        viewModel.onHelpClicked.observe(this, {
            (requireActivity() as OnHelpClickedListener).onHelpClicked(HelpActivity.Origin.SITE_CREATION_CREATING)
        })
        viewModel.onSiteCreationCompleted.observe(this, {
            (requireActivity() as SitePreviewScreenListener).onSiteCreationCompleted()
        })
        viewModel.onOkButtonClicked.observe(this, { createSiteState ->
            createSiteState?.let {
                (requireActivity() as SitePreviewScreenListener).onSitePreviewScreenDismissed(createSiteState)
            }
        })
        viewModel.onCancelWizardClicked.observe(this, { createSiteState ->
            createSiteState?.let {
                (requireActivity() as SitePreviewScreenListener).onSitePreviewScreenDismissed(createSiteState)
            }
        })
    }

    private fun FullscreenErrorWithRetryBinding.initRetryButton() {
        errorRetry.setOnClickListener { viewModel.retry() }
    }

    private fun SiteCreationPreviewScreenDefaultBinding.initOkButton() {
        okButton.setOnClickListener { viewModel.onOkButtonClicked() }
    }

    private fun FullscreenErrorWithRetryBinding.initCancelWizardButton() {
        cancelWizardButton.setOnClickListener { viewModel.onCancelWizardClicked() }
    }

    private fun FullscreenErrorWithRetryBinding.initContactSupportButton() {
        contactSupport.setOnClickListener { viewModel.onHelpClicked() }
    }

    private fun SiteCreationPreviewScreenDefaultBinding.updateContentLayout(sitePreviewData: SitePreviewData) {
        sitePreviewData.apply {
            siteCreationPreviewWebViewContainer.sitePreviewWebUrlTitle.text = createSpannableUrl(
                    requireNotNull(activity),
                    shortUrl,
                    subDomainIndices,
                    domainIndices
            )
        }
        if (contentLayout.visibility == View.GONE) {
            animateContentTransition()
            view?.announceForAccessibility(
                    getString(R.string.new_site_creation_preview_title) +
                            getString(R.string.new_site_creation_site_preview_content_description)
            )
        }
    }

    private fun SiteCreationProgressCreatingSiteBinding.updateLoadingLayout(
        progressUiState: SitePreviewFullscreenProgressUiState
    ) {
        progressUiState.apply {
            val newText = uiHelpers.getTextOfUiString(progressText.context, loadingTextResId)
            AppLog.d(AppLog.T.MAIN, "Changing text - animation: $animate")
            if (animate) {
                updateLoadingTextWithFadeAnimation(newText)
            } else {
                progressText.text = newText
            }
        }
    }

    private fun SiteCreationProgressCreatingSiteBinding.updateLoadingTextWithFadeAnimation(newText: CharSequence) {
        val animationDuration = AniUtils.Duration.SHORT
        val fadeOut = AniUtils.getFadeOutAnim(
                progressTextLayout,
                animationDuration,
                View.VISIBLE
        )
        val fadeIn = AniUtils.getFadeInAnim(
                progressTextLayout,
                animationDuration
        )

        // update the text when the view isn't visible
        fadeIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                progressText.text = newText
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                animatorSet = null
            }
        })
        // Start the fade-in animation right after the view fades out
        fadeIn.startDelay = animationDuration.toMillis(progressTextLayout.context)

        animatorSet = AnimatorSet().apply {
            playSequentially(fadeOut, fadeIn)
            start()
        }
    }

    private fun FullscreenErrorWithRetryBinding.updateErrorLayout(
        errorUiStateState: SitePreviewFullscreenErrorUiState
    ) {
        errorUiStateState.apply {
            uiHelpers.setTextOrHide(errorTitle, titleResId)
            uiHelpers.setTextOrHide(errorSubtitle, subtitleResId)
            uiHelpers.updateVisibility(contactSupport, errorUiStateState.showContactSupport)
            uiHelpers.updateVisibility(cancelWizardButton, errorUiStateState.showCancelWizardButton)
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
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.neutral_40)),
                domainSpan.first,
                domainSpan.second,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return spannableTitle
    }

    override fun onWebViewPageLoaded() {
        viewModel.onUrlLoaded()
    }

    override fun onWebViewReceivedError() {
        viewModel.onWebViewError()
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    private fun SiteCreationPreviewScreenDefaultBinding.animateContentTransition() {
        contentLayout.addOnLayoutChangeListener(
                object : OnLayoutChangeListener {
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
                        if (meetsHeightWidthForAnimation()) {
                            contentLayout.removeOnLayoutChangeListener(this)
                            val contentHeight = contentLayout.measuredHeight.toFloat()
                            val titleAnim = createFadeInAnimator(siteCreationPreviewHeaderItem.sitePreviewTitle)
                            val webViewAnim = createSlideInFromBottomAnimator(
                                    siteCreationPreviewWebViewContainer.webViewContainer,
                                    contentHeight
                            )

                            // OK button should slide in if the container exists and fade in otherwise
                            // difference between land & portrait
                            val okAnim = if (isLandscape) {
                                createFadeInAnimator(okButton)
                            } else {
                                createSlideInFromBottomAnimator(sitePreviewOkButtonContainer as View, contentHeight)
                            }

                            // There is a chance that either of the following fields can be null,
                            // so to avoid a NPE, we only execute playTogether if they are both not null
                            if (titleAnim != null && okAnim != null) {
                                AnimatorSet().apply {
                                    interpolator = DecelerateInterpolator()
                                    duration = SLIDE_IN_ANIMATION_DURATION
                                    playTogether(titleAnim, webViewAnim, okAnim)
                                    start()
                                }
                            }
                        }
                    }
                })
    }

    private fun SiteCreationPreviewScreenDefaultBinding.meetsHeightWidthForAnimation() =
            contentLayout.measuredWidth > 0 && contentLayout.measuredHeight > 0

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

    override fun onResume() {
        super.onResume()
        serviceEventConnection = ServiceEventConnection(context, SiteCreationService::class.java, viewModel)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewModel.writeToBundle(outState)
    }

    override fun onPause() {
        super.onPause()
        serviceEventConnection?.disconnect(context, viewModel)
    }

    override fun onStop() {
        super.onStop()
        if (animatorSet?.isRunning == true) {
            animatorSet?.cancel()
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
