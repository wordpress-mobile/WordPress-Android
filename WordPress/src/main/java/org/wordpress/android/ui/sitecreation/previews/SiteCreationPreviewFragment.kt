package org.wordpress.android.ui.sitecreation.previews

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
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
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.databinding.SiteCreationFormScreenBinding
import org.wordpress.android.databinding.SiteCreationPreviewScreenBinding
import org.wordpress.android.databinding.SiteCreationPreviewScreenDefaultBinding
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.ui.sitecreation.SiteCreationActivity.Companion.ARG_STATE
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.SitePreviewLoadingShimmerState
import org.wordpress.android.ui.sitecreation.previews.SitePreviewViewModel.SitePreviewUiState.UrlData
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.ErrorManagedWebViewClient.ErrorManagedWebViewClientListener
import org.wordpress.android.util.URLFilteredWebViewClient
import org.wordpress.android.util.extensions.getParcelableCompat
import org.wordpress.android.widgets.NestedWebView
import javax.inject.Inject

private const val SLIDE_IN_ANIMATION_DURATION = 450L

@AndroidEntryPoint
class SiteCreationPreviewFragment : SiteCreationBaseFormFragment(),
    ErrorManagedWebViewClientListener {
    @Inject
    lateinit var userAgent: UserAgent

    @Inject
    internal lateinit var uiHelpers: UiHelpers

    private val isLandscape get() = resources.configuration.orientation == ORIENTATION_LANDSCAPE

    private lateinit var binding: SiteCreationPreviewScreenBinding
    private val viewModel: SitePreviewViewModel by activityViewModels()

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

        viewModel.start(requireNotNull(requireArguments().getParcelableCompat(ARG_STATE)))
    }

    override fun getContentLayout() = R.layout.site_creation_preview_screen

    override val screenTitle get() = requireArguments().getString(EXTRA_SCREEN_TITLE).orEmpty()

    override fun setBindingViewStubListener(parentBinding: SiteCreationFormScreenBinding) {
        parentBinding.siteCreationFormContentStub.setOnInflateListener { _, inflated ->
            binding = SiteCreationPreviewScreenBinding.bind(inflated)
        }
    }

    override fun setupContent() {
        binding.siteCreationPreviewScreenDefault.run {
            observeState()
            observePreview(siteCreationPreviewWebViewContainer.sitePreviewWebView)
            okButton.setOnClickListener { viewModel.onOkButtonClicked() }
        }
    }

    private fun SiteCreationPreviewScreenDefaultBinding.observeState() {
        viewModel.uiState.observe(this@SiteCreationPreviewFragment) {
            it?.let { ui ->
                uiHelpers.setTextOrHide(siteCreationPreviewHeaderItem.sitePreviewSubtitle, ui.subtitle)
                uiHelpers.setTextOrHide(sitePreviewCaptionText, ui.caption)
                sitePreviewCaption.isVisible = ui.caption != null
                updateContentLayout(ui.urlData, isFirstContent = ui is SitePreviewLoadingShimmerState)
                siteCreationPreviewWebViewContainer.apply {
                    uiHelpers.updateVisibility(sitePreviewWebView, ui.webViewVisibility)
                    uiHelpers.updateVisibility(sitePreviewWebError, ui.webViewErrorVisibility)
                    uiHelpers.updateVisibility(sitePreviewWebViewShimmerLayout, ui.shimmerVisibility)
                }
                ui.errorTitle?.let { error ->
                    siteCreationPreviewHeaderItem.sitePreviewTitle.text =
                        uiHelpers.getTextOfUiString(requireContext(), error)
                }
            }
        }
    }

    private fun observePreview(webView: NestedWebView) {
        viewModel.preloadPreview.observe(this) { url ->
            url?.let { urlString ->
                webView.webViewClient = URLFilteredWebViewClient(urlString, this)
                webView.settings.userAgentString = userAgent.toString()
                webView.loadUrl(urlString)
            }
        }
    }

    private fun SiteCreationPreviewScreenDefaultBinding.updateContentLayout(
        urlData: UrlData,
        isFirstContent: Boolean = false,
    ) {
        urlData.apply {
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

    override fun onHelp() = Unit // noop

    private fun SiteCreationPreviewScreenDefaultBinding.animateContentTransition() {
        contentLayout.addOnLayoutChangeListener(
            object : OnLayoutChangeListener {
                @SuppressLint("Recycle")
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
