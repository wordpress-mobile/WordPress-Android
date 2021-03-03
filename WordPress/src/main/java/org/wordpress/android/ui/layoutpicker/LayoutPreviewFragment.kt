package org.wordpress.android.ui.layoutpicker

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout.LayoutParams
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.home_page_picker_preview_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.FullscreenBottomSheetDialogFragment
import org.wordpress.android.ui.PreviewMode.DESKTOP
import org.wordpress.android.ui.PreviewMode.MOBILE
import org.wordpress.android.ui.PreviewMode.TABLET
import org.wordpress.android.ui.PreviewModeSelectorPopup
import org.wordpress.android.ui.layoutpicker.PreviewUiState.Error
import org.wordpress.android.ui.layoutpicker.PreviewUiState.Loaded
import org.wordpress.android.ui.layoutpicker.PreviewUiState.Loading
import org.wordpress.android.util.AniUtils
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.setVisible
import org.wordpress.android.util.skip
import javax.inject.Inject

private const val INITIAL_SCALE = 100
private const val JS_EVALUATION_DELAY = 250L

abstract class LayoutPreviewFragment : FullscreenBottomSheetDialogFragment() {
    @Inject lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    private lateinit var viewModel: LayoutPickerViewModel
    private lateinit var previewModeSelectorPopup: PreviewModeSelectorPopup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.home_page_picker_preview_fragment, container)

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView.settings.javaScriptEnabled = true
        webView.settings.useWideViewPort = true
        webView.setInitialScale(INITIAL_SCALE)
    }

    fun setViewModel(viewModel: LayoutPickerViewModel) {
        this.viewModel = viewModel

        viewModel.previewState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is Loading -> {
                    desktopPreviewHint.setVisible(false)
                    progressBar.setVisible(true)
                    webView.setVisible(false)
                    errorView.setVisible(false)
                    webView.loadUrl(state.url)
                }
                is Loaded -> {
                    progressBar.setVisible(false)
                    webView.setVisible(true)
                    errorView.setVisible(false)
                    desktopPreviewHint.setText(
                            when (viewModel.selectedPreviewMode()) {
                                MOBILE -> R.string.web_preview_mobile
                                TABLET -> R.string.web_preview_tablet
                                DESKTOP -> R.string.web_preview_desktop
                            }
                    )
                    AniUtils.animateBottomBar(desktopPreviewHint, true)
                }
                is Error -> {
                    progressBar.setVisible(false)
                    webView.setVisible(false)
                    errorView.setVisible(true)
                    state.toast?.let { ToastUtils.showToast(requireContext(), it) }
                }
            }
        })

        // We're skipping the first emitted value since it derives from the view model initialization (`start` method)
        viewModel.previewMode.skip(1).observe(viewLifecycleOwner, Observer { load() })

        viewModel.onPreviewModeButtonPressed.observe(viewLifecycleOwner, Observer {
            previewModeSelectorPopup.show(viewModel)
        })

        previewModeSelectorPopup = PreviewModeSelectorPopup(requireActivity(), previewTypeSelectorButton)

        backButton.setOnClickListener { closeModal() }

        chooseButton.setOnClickListener { viewModel.onPreviewChooseTapped() }

        previewTypeSelectorButton.setOnClickListener { viewModel.onPreviewModePressed() }

        webView.settings.userAgentString = WordPress.getUserAgent()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == null) return
                val width = viewModel.selectedPreviewMode().previewWidth
                setWebViewWidth(view, width)
                val widthScript = context?.getString(R.string.web_preview_width_script, width)
                if (widthScript != null) {
                    Handler().postDelayed({
                        view.evaluateJavascript(widthScript) { viewModel.onPreviewLoaded() }
                    }, JS_EVALUATION_DELAY)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                viewModel.onPreviewError()
            }
        }

        errorView.button.setOnClickListener { load() }
        load()
    }

    override fun closeModal() = viewModel.onDismissPreview()

    private fun load() = viewModel.onPreviewLoading()

    private fun setWebViewWidth(view: View, previewWidth: Int) {
        if (!displayUtilsWrapper.isTablet()) return
        view.layoutParams = if (viewModel.selectedPreviewMode() === MOBILE) {
            val width = previewWidth * resources.displayMetrics.density.toInt()
            LayoutParams(width, LayoutParams.MATCH_PARENT).apply { gravity = CENTER_HORIZONTAL }
        } else {
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
    }
}
