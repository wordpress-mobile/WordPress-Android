package org.wordpress.android.ui.blaze.blazecampaigns

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.blaze.blazecampaigns.campaigndetail.CampaignDetailWebViewNavigationDelegate
import org.wordpress.android.ui.utils.AbstractAllowedUrlsWebViewNavigationDelegate.Url

@ExperimentalCoroutinesApi
class CampaignDetailWebViewNavigationDelegateTest : BaseUnitTest() {
    private val navigationDelegate = CampaignDetailWebViewNavigationDelegate

    @Test
    fun `when advertising campaign url, then web view can navigate`() {
        assertThat(
            buildUrls(
                "/advertising/campaigns/12345/dummywpcomsite.wordpress.com"
            )
        ).allMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    @Test
    fun `when non advertising campaign url, then web view can not navigate`() {
        assertThat(
            buildUrls(
                "/dummywpcomsite.wordpress.com",
                "/advertising/dummywpcomsite.wordpress.com/campaigns",
                "/advertising/dummywpcomsite.wordpress.com",
                "/advertising/dummywpcomsite.wordpress.com/campaigns/12345"
            )
        ).noneMatch {
            navigationDelegate.canNavigateTo(it)
        }
    }

    companion object {
        private fun buildUrls(vararg paths: String) = paths.toList().map { Url("wordpress.com", it) }
    }
}
