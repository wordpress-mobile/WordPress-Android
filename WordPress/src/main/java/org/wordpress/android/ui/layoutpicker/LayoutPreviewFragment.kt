package org.wordpress.android.ui.layoutpicker

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout.LayoutParams
import androidx.lifecycle.ViewModelProvider
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.LayoutPickerPreviewFragmentBinding
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
import org.wordpress.android.util.extensions.setVisible
import org.wordpress.android.util.skip
import javax.inject.Inject

private const val INITIAL_SCALE = 90
private const val JS_EVALUATION_DELAY = 250L
private const val JS_READY_CALLBACK_ID = 926L

abstract class LayoutPreviewFragment : FullscreenBottomSheetDialogFragment() {
    @Inject lateinit var displayUtilsWrapper: DisplayUtilsWrapper
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: LayoutPickerViewModel
    private lateinit var previewModeSelectorPopup: PreviewModeSelectorPopup

    private var binding: LayoutPickerPreviewFragmentBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = LayoutPickerPreviewFragmentBinding.inflate(inflater, container, false)
        return binding?.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.webView?.settings?.javaScriptEnabled = true
        binding?.webView?.settings?.useWideViewPort = true
        binding?.webView?.setInitialScale(INITIAL_SCALE)
        binding?.chooseButton?.setText(getChooseButtonText())
        initViewModel()
    }

    abstract fun getChooseButtonText(): Int

    abstract fun getViewModel(): LayoutPickerViewModel

    private fun initViewModel() {
        this.viewModel = getViewModel()

        viewModel.previewState.observe(viewLifecycleOwner, { state ->
            when (state) {
                is Loading -> {
                    binding?.desktopPreviewHint?.setVisible(false)
                    binding?.progressBar?.setVisible(true)
                    binding?.webView?.setVisible(false)
                    binding?.errorView?.setVisible(false)
                    binding?.webView?.loadUrl(state.url)
                }
                is Loaded -> {
                    binding?.progressBar?.setVisible(false)
                    binding?.webView?.setVisible(true)
                    binding?.errorView?.setVisible(false)
                    binding?.desktopPreviewHint?.setText(
                            when (viewModel.selectedPreviewMode()) {
                                MOBILE -> R.string.web_preview_mobile
                                TABLET -> R.string.web_preview_tablet
                                DESKTOP -> R.string.web_preview_desktop
                            }
                    )
                    AniUtils.animateBottomBar(binding?.desktopPreviewHint, true)
                }
                is Error -> {
                    binding?.progressBar?.setVisible(false)
                    binding?.webView?.setVisible(false)
                    binding?.errorView?.setVisible(true)
                    state.toast?.let { ToastUtils.showToast(requireContext(), it) }
                }
            }
        })

        // We're skipping the first emitted value since it derives from the view model initialization (`start` method)
        viewModel.previewMode.skip(1).observe(viewLifecycleOwner, { load() })

        viewModel.onPreviewModeButtonPressed.observe(viewLifecycleOwner, {
            previewModeSelectorPopup.show(viewModel)
        })

        binding?.previewTypeSelectorButton?.let {
            previewModeSelectorPopup = PreviewModeSelectorPopup(requireActivity(), it)
        }

        binding?.backButton?.setOnClickListener { closeModal() }

        binding?.chooseButton?.setOnClickListener { viewModel.onPreviewChooseTapped() }

        binding?.previewTypeSelectorButton?.setOnClickListener { viewModel.onPreviewModePressed() }

        binding?.webView?.settings?.userAgentString = WordPress.getUserAgent()
        binding?.webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view == null) return
                val width = viewModel.selectedPreviewMode().previewWidth
                setWebViewWidth(view, width)
                val widthScript = context?.getString(R.string.web_preview_width_script, width)
                if (widthScript != null) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        view.evaluateJavascript(widthScript) {
                            view.postVisualStateCallback(JS_READY_CALLBACK_ID, object : WebView.VisualStateCallback() {
                                override fun onComplete(requestId: Long) {
                                    if (JS_READY_CALLBACK_ID == requestId) {
                                        viewModel.onPreviewLoaded()
                                    }
                                }
                            })
                        }
                    }, JS_EVALUATION_DELAY)
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                viewModel.onPreviewError()
            }
        }

        binding?.errorView?.button?.setOnClickListener { load() }
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
