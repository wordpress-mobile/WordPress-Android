package org.wordpress.android.ui.blazeCampaigns.campaignlisting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blazeCampaigns.CampaignViewModel
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
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
                CampaignListingPage()
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
    fun CampaignListingPage() {
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
        ) { }
    }
}

