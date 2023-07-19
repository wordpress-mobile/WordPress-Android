package org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail

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
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.getSerializableCompat

private const val CAMPAIGN_DETAIL_PAGE_SOURCE = "campaign_detail_page_source"

@AndroidEntryPoint
class CampaignDetailFragment : Fragment() {
    companion object {
        fun newInstance(source: CampaignDetailPageSource) = CampaignDetailFragment().apply {
            arguments = Bundle().apply {
                putSerializable(CAMPAIGN_DETAIL_PAGE_SOURCE, source)
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
        viewModel.start(getPageSource())
    }

    private fun getPageSource(): CampaignDetailPageSource {
        return arguments?.getSerializableCompat<CampaignDetailPageSource>(
            CAMPAIGN_DETAIL_PAGE_SOURCE
        )
            ?: CampaignDetailPageSource.UNKNOWN
    }
}

@Composable
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
fun CampaignDetailPage(navigationUp: () -> Unit = { }) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.blaze_campaign_details_page_title),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = navigationUp
            )
        }
    ) { }
}
