package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.R
import androidx.compose.ui.Alignment
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.util.extensions.getSerializableCompat
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.compose.state.LoadingState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text

private const val CAMPAIGN_DETAIL_PAGE_SOURCE = "campaign_detail_page_source"
private const val CAMPAIGN_DETAIL_CAMPAIGN_ID = "campaign_detail_campaign_id"

@AndroidEntryPoint
class CampaignDetailFragment : Fragment(), CampaignDetailWebViewClient.CampaignDetailWebViewClientListener {
    companion object {
        fun newInstance(campaignId: String, source: CampaignDetailPageSource) = CampaignDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(CAMPAIGN_DETAIL_PAGE_SOURCE, source)
                putString(CAMPAIGN_DETAIL_CAMPAIGN_ID, campaignId)
            }
        }
    }

    private val viewModel: CampaignDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeM2 {
                CampaignDetailPage(
                    navigationUp = requireActivity().onBackPressedDispatcher::onBackPressed
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViewModel()
        observeViewModel()
    }

    private fun initViewModel() {
        viewModel.start(getCampaignId(), getPageSource())
    }

    private fun observeViewModel() {
        viewModel.actionEvents.onEach(this::handleActionEvents).launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun handleActionEvents(actionEvent: BlazeActionEvent) {
        when (actionEvent) {
            is BlazeActionEvent.FinishActivity -> requireActivity().finish()
            is BlazeActionEvent.LaunchExternalBrowser -> {
                ActivityLauncher.openUrlExternal(
                    requireContext(),
                    actionEvent.url
                )
            }
        }
    }

    private fun getPageSource(): CampaignDetailPageSource {
        return arguments?.getSerializableCompat<CampaignDetailPageSource>(
            CAMPAIGN_DETAIL_PAGE_SOURCE
        )
            ?: CampaignDetailPageSource.UNKNOWN
    }

    private fun getCampaignId() = requireArguments().getString(CAMPAIGN_DETAIL_CAMPAIGN_ID) ?: ""

    override fun onRedirectToExternalBrowser(url: String) = viewModel.onRedirectToExternalBrowser(url)

    override fun onWebViewPageLoaded() = viewModel.onUrlLoaded()

    override fun onWebViewReceivedError() = viewModel.onWebViewError()

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun CampaignDetailPage(
        navigationUp: () -> Unit = { },
        viewModel: CampaignDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    ) {
        val uiState by viewModel.uiState.collectAsState()
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(R.string.blaze_campaign_details_page_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = navigationUp
                )
            },
            content = { CampaignDetailContent(uiState) }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun CampaignDetailContent(uiState: CampaignDetailUiState) {
        when (uiState) {
            is CampaignDetailUiState.Preparing -> LoadingState()
            is CampaignDetailUiState.Prepared, is CampaignDetailUiState.Loaded -> CampaignDetailWebView(uiState)
            is CampaignDetailUiState.Error -> CampaignDetailError(uiState)
        }
    }

    @Composable
    fun CampaignDetailError(error: CampaignDetailUiState.Error) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Text(
                text = uiStringText(uiString = error.title),
                style = MaterialTheme.typography.h5,
                textAlign = TextAlign.Center
            )
            Text(
                text = uiStringText(uiString = error.description),
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (error.button != null) {
                Button(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = error.button.click
                ) {
                    Text(text = uiStringText(uiString = error.button.text))
                }
            }
        }
    }


    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun CampaignDetailWebView(uiState: CampaignDetailUiState) {
        var webView: WebView? by remember { mutableStateOf(null) }

        if (uiState is CampaignDetailUiState.Prepared) {
            val model = uiState.model
            LaunchedEffect(true) {
                webView = WebView(requireContext()).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                    settings.userAgentString = model.userAgent
                    settings.javaScriptEnabled = model.enableJavascript
                    settings.domStorageEnabled = model.enableDomStorage
                    webViewClient = CampaignDetailWebViewClient(this@CampaignDetailFragment)
                    postUrl(WPWebViewActivity.WPCOM_LOGIN_URL, model.addressToLoad.toByteArray())
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState is CampaignDetailUiState.Prepared) {
                LoadingState()
            } else {
                webView?.let { theWebView ->
                    AndroidView(
                        factory = { theWebView },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
