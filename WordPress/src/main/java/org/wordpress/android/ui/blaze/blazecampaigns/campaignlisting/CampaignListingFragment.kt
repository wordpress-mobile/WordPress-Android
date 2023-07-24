package org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.blazecampaigns.CampaignViewModel
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.compose.state.LoadingState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.extensions.getSerializableCompat

private const val CAMPAIGN_LISTING_PAGE_SOURCE = "campaign_listing_page_source"

@AndroidEntryPoint
class CampaignListingFragment : Fragment() {
    companion object {
        fun newInstance(campaignListingPageSource: CampaignListingPageSource) = CampaignListingFragment().apply {
            arguments = Bundle().apply {
                putSerializable(CAMPAIGN_LISTING_PAGE_SOURCE, campaignListingPageSource)
            }
        }
    }

    private val viewModel: CampaignListingViewModel by viewModels()

    private val campaignViewModel: CampaignViewModel by activityViewModels()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.start(getPageSource())
    }

    private fun getPageSource(): CampaignListingPageSource {
        return arguments?.getSerializableCompat<CampaignListingPageSource>(CAMPAIGN_LISTING_PAGE_SOURCE)
            ?: CampaignListingPageSource.UNKNOWN
    }

    @Composable
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter")
    fun CampaignListingPage(uiState: CampaignListingUiState) {
        Scaffold(
            topBar = {
                MainTopAppBar(
                    title = stringResource(R.string.blaze_campaigns_page_title),
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = {
                        campaignViewModel.onNavigationUp()
                    }
                )
            }
        ) { CampaignListingContent(uiState) }
    }

    @Composable
    fun CampaignListingContent(
        uiState: CampaignListingUiState
    ) {
        when (uiState) {
            is CampaignListingUiState.Loading -> LoadingState()
            is CampaignListingUiState.Error -> CampaignListingError(uiState)
            is CampaignListingUiState.Success -> CampaignListingSuccess(uiState)
        }
    }

    @Composable
    fun CampaignListingSuccess(uiState: CampaignListingUiState.Success) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(uiState.campaigns) { campaign ->
                CampaignListRow(
                    campaignModel = campaign,
                    modifier = Modifier.clickable { uiState.itemClick(campaign) })
            }
        }
    }
}

@Composable
fun CampaignListingError(error: CampaignListingUiState.Error) {
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

@Preview
@Composable
fun CampaignListingErrorPreview() {
    AppTheme {
        CampaignListingError(CampaignListingUiState.Error(
            title = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_message_title),
            description = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_message_description),
            button = CampaignListingUiState.Error.ErrorButton(
                text = UiString.UiStringRes(R.string.campaign_listing_page_no_campaigns_button_text),
                click = { }
            )
        ))
    }
}

