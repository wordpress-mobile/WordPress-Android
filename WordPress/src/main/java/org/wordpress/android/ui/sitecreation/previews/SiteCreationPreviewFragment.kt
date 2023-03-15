package org.wordpress.android.ui.sitecreation.previews

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
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
import org.wordpress.android.databinding.SiteCreationFormScreenBinding
import org.wordpress.android.databinding.SiteCreationPreviewScreenBinding
import org.wordpress.android.databinding.SiteCreationPreviewScreenDefaultBinding
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewData
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewContentUiState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewWebErrorUiState
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ErrorManagedWebViewClient.ErrorManagedWebViewClientListener
import org.wordpress.android.util.URLFilteredWebViewClient
import org.wordpress.android.widgets.NestedWebView
import javax.inject.Inject

private const val ARG_STATE = "arg_site_creation_state"
private const val SLIDE_IN_ANIMATION_DURATION = 450L

@AndroidEntryPoint
class SiteCreationPreviewFragment : SiteCreationBaseFormFragment(),
    ErrorManagedWebViewClientListener {
    @Inject
    internal lateinit var uiHelpers: UiHelpers

    private val isLandscape get() = resources.configuration.orientation == ORIENTATION_LANDSCAPE

    private lateinit var binding: SiteCreationPreviewScreenBinding
    private val viewModel: SitePreviewViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        check(context is SitePreviewScreenListener) { "Parent activity must implement SitePreviewScreenListener." }
        check(context is OnHelpClickedListener) { "Parent activity must implement OnHelpClickedListener." }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        viewModel.start(requireArguments()[ARG_STATE] as SiteCreationState)
    }

    override fun getContentLayout() = R.layout.site_creation_preview_screen

    override val screenTitle: String
        get() = requireNotNull(arguments?.getString(EXTRA_SCREEN_TITLE)) { "Missing required argument 'screenTitle'." }

    override fun setBindingViewStubListener(parentBinding: SiteCreationFormScreenBinding) {
        parentBinding.siteCreationFormContentStub.setOnInflateListener { _, inflated ->
            binding = SiteCreationPreviewScreenBinding.bind(inflated)
        }
    }

    override fun setupContent() {
        binding.siteCreationPreviewScreenDefault.run {
            observeState()
            observePreview(siteCreationPreviewWebViewContainer.sitePreviewWebView)
            observeHelpClicks(requireActivity() as OnHelpClickedListener)
            observeDismissClicks(requireActivity() as SitePreviewScreenListener)
            okButton.setOnClickListener { viewModel.onOkButtonClicked() }
        }
    }

    private fun SiteCreationPreviewScreenDefaultBinding.observeState() {
        viewModel.uiState.observe(this@SiteCreationPreviewFragment) {
            it?.run {
                when (val ui = this@run) {
                    is SitePreviewContentUiState -> updateContentLayout(ui.data)
                    is SitePreviewWebErrorUiState -> updateContentLayout(ui.data)
                    is SitePreviewLoadingShimmerState -> updateContentLayout(ui.data, isFirstContent = true)
                }
                uiHelpers.updateVisibility(contentLayout, contentLayoutVisibility)
                uiHelpers.updateVisibility(fullscreenErrorWithRetry.errorLayout, fullscreenErrorLayoutVisibility)
                siteCreationPreviewWebViewContainer.apply {
                    uiHelpers.updateVisibility(sitePreviewWebView, webViewVisibility)
                    uiHelpers.updateVisibility(sitePreviewWebError, webViewErrorVisibility)
                    uiHelpers.updateVisibility(sitePreviewWebViewShimmerLayout, shimmerVisibility)
                }
            }
        }
    }

    private fun observePreview(webView: NestedWebView) {
        viewModel.preloadPreview.observe(this) { url ->
            url?.let { urlString ->
                webView.webViewClient = URLFilteredWebViewClient(urlString, this)
                webView.settings.userAgentString = WordPress.getUserAgent()
                webView.loadUrl(urlString)
            }
        }
    }

    private fun observeHelpClicks(listener: OnHelpClickedListener) {
        viewModel.onHelpClicked.observe(this) {
            listener.onHelpClicked(HelpActivity.Origin.SITE_CREATION_CREATED)
        }
    }

    private fun observeDismissClicks(listener: SitePreviewScreenListener) {
        viewModel.onOkButtonClicked.observe(this) { createSiteState ->
            createSiteState?.let { listener.onPreviewScreenClosed() }
        }
    }

    private fun SiteCreationPreviewScreenDefaultBinding.updateContentLayout(
        sitePreviewData: SitePreviewData,
        isFirstContent: Boolean = false,
    ) {
        sitePreviewData.apply {
            siteCreationPreviewWebViewContainer.sitePreviewWebUrlTitle.text = createSpannableUrl(
                requireNotNull(activity),
                shortUrl,
                subDomainIndices,
                domainIndices
            )
        }
        if (isFirstContent) {
            animateContentTransition()
            view?.announceForAccessibility(getString(R.string.new_site_creation_site_preview_content_description))
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

    override fun onWebViewPageLoaded() = viewModel.onUrlLoaded()

    override fun onWebViewReceivedError() = viewModel.onWebViewError()

    override fun onHelp() = viewModel.onHelpClicked()

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

                        // OK button slides in if the container exists else it fades in the diff between land & portrait
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

    companion object {
        const val TAG = "site_creation_preview_fragment_tag"

        fun newInstance(
            screenTitle: String,
            siteCreationState: SiteCreationState,
        ) = SiteCreationPreviewFragment().apply {
            arguments = Bundle().apply {
                putString(EXTRA_SCREEN_TITLE, screenTitle)
                putParcelable(ARG_STATE, siteCreationState)
            }
        }
    }
}
