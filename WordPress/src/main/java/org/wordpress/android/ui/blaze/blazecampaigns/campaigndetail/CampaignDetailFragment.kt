package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
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
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.blaze.BlazeActionEvent
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.extensions.getSerializableCompat

private const val CAMPAIGN_DETAIL_PAGE_SOURCE = "campaign_detail_page_source"
private const val CAMPAIGN_DETAIL_CAMPAIGN_ID = "campaign_detail_campaign_id"

@AndroidEntryPoint
class CampaignDetailFragment : Fragment(), CampaignDetailWebViewClient.CampaignDetailWebViewClientListener {
    companion object {
        fun newInstance(campaignId: Int, source: CampaignDetailPageSource) = CampaignDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(CAMPAIGN_DETAIL_PAGE_SOURCE, source)
                putInt(CAMPAIGN_DETAIL_CAMPAIGN_ID, campaignId)
            }
        }
    }

    private val viewModel: CampaignDetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
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
            is BlazeActionEvent.FinishActivityWithMessage -> {
                ToastUtils.showToast(requireContext(), actionEvent.id)
                requireActivity().finish()
            }
        }
    }

    private fun getPageSource(): CampaignDetailPageSource {
        return arguments?.getSerializableCompat<CampaignDetailPageSource>(
            CAMPAIGN_DETAIL_PAGE_SOURCE
        )
            ?: CampaignDetailPageSource.UNKNOWN
    }

    private fun getCampaignId() = requireArguments().getInt(CAMPAIGN_DETAIL_CAMPAIGN_ID)

    override fun onRedirectToExternalBrowser(url: String) = viewModel.onRedirectToExternalBrowser(url)

    override fun onWebViewPageLoaded() = viewModel.onUrlLoaded()

    override fun onWebViewReceivedError() = viewModel.onWebViewError()

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun CampaignDetailPage(
        navigationUp: () -> Unit = { },
        viewModel: CampaignDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    ) {
        val data by viewModel.model.collectAsState()
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(R.string.blaze_campaign_details_page_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = navigationUp
                )
            },
            content = { CampaignDetailContent(data) }
        )
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun CampaignDetailContent(model: CampaignDetailUIModel) {
        AndroidView(factory = {
            WebView(it).apply {
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
        })
    }
}
