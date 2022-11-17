package org.wordpress.android.util.analytics

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.analytics.AnalyticsUtils.AnalyticsSiteType

class AnalyticsUtilsTest {
    @Test
    fun isGutenbergEnabledOnAnySite_no_sites() {
        Assert.assertFalse(AnalyticsUtils.isGutenbergEnabledOnAnySite(siteList()))
    }

    @Test
    fun isGutenbergEnabledOnAnySite_single_gutenberg_site() {
        val sites = siteList(false, true, false)
        Assert.assertTrue(AnalyticsUtils.isGutenbergEnabledOnAnySite(sites))
    }

    @Test
    fun isGutenbergEnabledOnAnySite_multiple_gutenberg_sites() {
        val sites = siteList(true, false, true, false)
        Assert.assertTrue(AnalyticsUtils.isGutenbergEnabledOnAnySite(sites))
    }

    @Test
    fun isGutenbergEnabledOnAnySite_no_gutenberg_sites() {
        val sites = siteList(false, false)
        Assert.assertFalse(AnalyticsUtils.isGutenbergEnabledOnAnySite(sites))
    }

    @Test
    fun `AnalyticsSiteType return correct site type based on content of SiteModel `() {
        val p2Site = mockSite(isp2 = true)
        val nonP2Site = mockSite(isp2 = false)

        assertThat(AnalyticsSiteType.fromSiteModel(p2Site)).isEqualTo(AnalyticsSiteType.P2)
        assertThat(AnalyticsSiteType.fromSiteModel(nonP2Site)).isEqualTo(AnalyticsSiteType.BLOG)

        assertThat(AnalyticsSiteType.toStringFromSiteModel(p2Site)).isEqualTo("p2")
        assertThat(AnalyticsSiteType.toStringFromSiteModel(nonP2Site)).isEqualTo("blog")
    }

    private fun siteList(vararg isGutenbergEnabledList: Boolean): List<SiteModel> {
        val sites: MutableList<SiteModel> = ArrayList()
        for (isEnabled in isGutenbergEnabledList) {
            sites.add(mockSite(isEnabled))
        }
        return sites
    }

    private fun mockSite(isGutenbergEnabled: Boolean = true, isp2: Boolean = false): SiteModel {
        val enabledString = if (isGutenbergEnabled) SiteUtils.GB_EDITOR_NAME else ""
        val mockSite = Mockito.mock(SiteModel::class.java)
        whenever(mockSite.mobileEditor).thenReturn(enabledString)
        whenever(mockSite.isWpForTeamsSite).thenReturn(isp2)
        return mockSite
    }
}
