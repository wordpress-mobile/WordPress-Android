package org.wordpress.android.ui.blaze.blazeCampaigns

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.R
import org.wordpress.android.ui.blaze.blazeCampaigns.campaigndetail.CampaignDetailFragment
import org.wordpress.android.ui.blaze.blazeCampaigns.campaignlisting.CampaignListingFragment
import org.wordpress.android.util.extensions.getParcelableExtraCompat

const val ARG_EXTRA_BLAZE_CAMPAIGN_PAGE = "blaze_campaign_page"

@AndroidEntryPoint
class BlazeCampaignParentActivity : AppCompatActivity() {
    private val viewModel: CampaignViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blaze_campaign)
        viewModel.start(getCampaignUiPage())
        observe()
    }

    private fun observe() {
        viewModel.uiState.observe(this) { uiState ->
            when (uiState) {
                is BlazeCampaignPage.CampaignListingPage -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, CampaignListingFragment.newInstance(uiState.source))
                        .commitNow()
                }

                is BlazeCampaignPage.CampaignDetailsPage -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.container, CampaignDetailFragment.newInstance(uiState.source))
                        .commitNow()
                }

                is BlazeCampaignPage.Done -> {
                    finish()
                }

                else -> {}
            }
        }
    }

    private fun getCampaignUiPage(): BlazeCampaignPage? {
        return intent.getParcelableExtraCompat(ARG_EXTRA_BLAZE_CAMPAIGN_PAGE)
    }
}
