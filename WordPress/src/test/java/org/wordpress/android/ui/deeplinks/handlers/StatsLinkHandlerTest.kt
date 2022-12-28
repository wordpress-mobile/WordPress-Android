package org.wordpress.android.ui.deeplinks.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.ui.deeplinks.buildUri
import org.wordpress.android.ui.stats.StatsTimeframe.DAY
import org.wordpress.android.ui.stats.StatsTimeframe.INSIGHTS
import org.wordpress.android.ui.stats.StatsTimeframe.MONTH
import org.wordpress.android.ui.stats.StatsTimeframe.WEEK
import org.wordpress.android.ui.stats.StatsTimeframe.YEAR

@RunWith(MockitoJUnitRunner::class)
class StatsLinkHandlerTest {
    @Mock
    lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock
    lateinit var site: SiteModel
    private lateinit var statsLinkHandler: StatsLinkHandler

    @Before
    fun setUp() {
        statsLinkHandler = StatsLinkHandler(deepLinkUriUtils)
    }

    @Test
    fun `handles stats URI`() {
        val statsUri = buildUri(host = "wordpress.com", "stats")

        val isStatsUri = statsLinkHandler.shouldHandleUrl(statsUri)

        assertThat(isStatsUri).isTrue()
    }

    @Test
    fun `handles stats App link`() {
        val statsUri = buildUri(host = "stats")

        val isStatsUri = statsLinkHandler.shouldHandleUrl(statsUri)

        assertThat(isStatsUri).isTrue()
    }

    @Test
    fun `does not handle stats URI with different host`() {
        val statsUri = buildUri(host = "wordpress.org", "stats")

        val isStatsUri = statsLinkHandler.shouldHandleUrl(statsUri)

        assertThat(isStatsUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val statsUri = buildUri(host = "wordpress.com", "post")

        val isStatsUri = statsLinkHandler.shouldHandleUrl(statsUri)

        assertThat(isStatsUri).isFalse()
    }

    @Test
    fun `opens stats screen from empty URL`() {
        val uri = buildUri(host = null, "stats")

        val buildNavigateAction = statsLinkHandler.buildNavigateAction(uri)

        assertThat(buildNavigateAction).isEqualTo(NavigateAction.OpenStats)
    }

    @Test
    fun `opens stats screen from app link`() {
        val uri = buildUri(host = "stats")

        val buildNavigateAction = statsLinkHandler.buildNavigateAction(uri)

        assertThat(buildNavigateAction).isEqualTo(NavigateAction.OpenStats)
    }

    @Test
    fun `opens stats screen for a site when URL ends with site URL`() {
        val siteUrl = "example.com"
        val uri = buildUri(host = null, "stats", siteUrl)
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val buildNavigateAction = statsLinkHandler.buildNavigateAction(uri)

        assertThat(buildNavigateAction).isEqualTo(NavigateAction.OpenStatsForSite(site))
    }

    @Test
    fun `opens stats screen for a stats timeframe and a site when both present in URL`() {
        val siteUrl = "example.com"
        val timeframes = mapOf(
            "day" to DAY,
            "week" to WEEK,
            "month" to MONTH,
            "year" to YEAR,
            "insights" to INSIGHTS
        )
        timeframes.forEach { (key, timeframe) ->
            val uri = buildUri(host = null, "stats", key, siteUrl)
            whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

            val buildNavigateAction = statsLinkHandler.buildNavigateAction(uri)

            assertThat(buildNavigateAction).isEqualTo(
                NavigateAction.OpenStatsForSiteAndTimeframe(
                    site,
                    timeframe
                )
            )
        }
    }

    @Test
    fun `opens stats screen for a site when timeframe not valid`() {
        val siteUrl = "example.com"
        val uri = buildUri(host = null, "stats", "invalid", siteUrl)
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val buildNavigateAction = statsLinkHandler.buildNavigateAction(uri)

        assertThat(buildNavigateAction).isEqualTo(NavigateAction.OpenStatsForSite(site))
    }

    @Test
    fun `strips applink with all params`() {
        val uri = buildUri(host = "stats", "day", "example.com")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress://stats/day/domain")
    }

    @Test
    fun `strips applink without params`() {
        val uri = buildUri("stats")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress://stats")
    }

    @Test
    fun `strips deeplink with all params`() {
        val uri = buildUri(host = "wordpress.com", "stats", "day", "example.com")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/stats/day/domain")
    }

    @Test
    fun `strips deeplink without params`() {
        val uri = buildUri(host = "wordpress.com", "stats")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/stats")
    }
}
