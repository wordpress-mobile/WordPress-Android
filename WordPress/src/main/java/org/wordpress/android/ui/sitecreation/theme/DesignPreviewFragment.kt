package org.wordpress.android.ui.sitecreation.theme

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.home_page_picker_preview_fragment.*
import kotlinx.android.synthetic.main.home_page_picker_preview_fragment.errorView
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.FullscreenBottomSheetDialogFragment
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.PreviewUiState.Error
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.PreviewUiState.Loaded
import org.wordpress.android.ui.sitecreation.theme.HomePagePickerViewModel.PreviewUiState.Loading
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.setVisible
import javax.inject.Inject

/**
 * Implements the Home Page Picker Design Preview UI
 */
class DesignPreviewFragment : FullscreenBottomSheetDialogFragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: HomePagePickerViewModel

    private lateinit var template: String
    private lateinit var url: String

    companion object {
        const val DESIGN_PREVIEW_TAG = "DESIGN_PREVIEW_TAG"
        private const val DESIGN_PREVIEW_TEMPLATE = "DESIGN_PREVIEW_TEMPLATE"
        private const val DESIGN_PREVIEW_URL = "DESIGN_PREVIEW_URL"

        fun newInstance(template: String, url: String) = DesignPreviewFragment().apply {
            arguments = Bundle().apply {
                putString(DESIGN_PREVIEW_TEMPLATE, template)
                putString(DESIGN_PREVIEW_URL, url)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            template = it.getString(DESIGN_PREVIEW_TEMPLATE, "")
            url = it.getString(DESIGN_PREVIEW_URL, "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.home_page_picker_preview_fragment, container)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(HomePagePickerViewModel::class.java)

        viewModel.previewState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is Loading -> {
                    progressBar.setVisible(true)
                    webView.setVisible(false)
                    errorView.setVisible(false)
                    webView.loadUrl(url)
                }
                is Loaded -> {
                    progressBar.setVisible(false)
                    webView.setVisible(true)
                    errorView.setVisible(false)
                }
                is Error -> {
                    progressBar.setVisible(false)
                    webView.setVisible(false)
                    errorView.setVisible(true)
                    state.toast?.let { ToastUtils.showToast(requireContext(), it) }
                }
            }
        })

        backButton.setOnClickListener { closeModal() }

        chooseButton.setOnClickListener { viewModel.onPreviewChooseTapped() }

        webView.settings.userAgentString = WordPress.getUserAgent()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                viewModel.onPreviewLoaded(template)
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                viewModel.onPreviewError()
            }
        }

        errorView.button.setOnClickListener { load() }
        load()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    override fun closeModal() = viewModel.onDismissPreview()

    private fun load() = viewModel.onPreviewLoading(template)
}
