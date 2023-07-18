package org.wordpress.android.ui.blazeCampaigns.campaignlisting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme

@AndroidEntryPoint
class CampaignListingFragment : Fragment() {
    companion object {
        fun newInstance() = CampaignListingFragment()
    }

    private val viewModel: CampaignListingViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            AppTheme {
                val campaigns by viewModel.uiState.observeAsState()
                CampaignListingPage(campaigns ?: CampaignListingUiState.Loading)
            }
        }
    }
}

@Composable
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
fun CampaignListingPage(
    uiState: CampaignListingUiState
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.blaze_campaigns_page_title),
                navigationIcon = NavigationIcons.BackIcon
            )
        }
    ) { CampaignListingContent(uiState) }
}

@Composable
fun CampaignListingContent(
    uiState: CampaignListingUiState
) {
    when (uiState) {
        is CampaignListingUiState.Loading -> CampaignListingLoading()
        is CampaignListingUiState.Error -> CampaignListingError(uiState.error)
        is CampaignListingUiState.Success -> CampaignListingSuccess(uiState.campaigns)
    }
}

@Composable
fun CampaignListingSuccess(campaigns: List<CampaignModel>) {
    TODO("Not yet implemented")
}

@Composable
fun CampaignListingError(error: String) {
    TODO("Not yet implemented")
}

@Composable
fun CampaignListingLoading() {
    TODO("Not yet implemented")
}
