package org.wordpress.android.ui.deeplinks

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.stats.StatsTimeframe.DAY
import org.wordpress.android.ui.stats.StatsTimeframe.INSIGHTS
import org.wordpress.android.ui.stats.StatsTimeframe.MONTH
import org.wordpress.android.ui.stats.StatsTimeframe.WEEK
import org.wordpress.android.ui.stats.StatsTimeframe.YEAR

@RunWith(MockitoJUnitRunner::class)
class StatsLinkHandlerTest {
    @Mock lateinit var deepLinkUriUtils: DeepLinkUriUtils
    @Mock lateinit var site: SiteModel
    private lateinit var statsLinkHandler: StatsLinkHandler

    @Before
    fun setUp() {
        statsLinkHandler = StatsLinkHandler(deepLinkUriUtils)
    }

    @Test
    fun `handles stats URI`() {
        val statsUri = buildUri(host = "wordpress.com", path1 = "stats")

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
        val statsUri = buildUri(host = "wordpress.org", path1 = "stats")

        val isStatsUri = statsLinkHandler.shouldHandleUrl(statsUri)

        assertThat(isStatsUri).isFalse()
    }

    @Test
    fun `does not handle URI with different path`() {
        val statsUri = buildUri(host = "wordpress.com", path1 = "post")

        val isStatsUri = statsLinkHandler.shouldHandleUrl(statsUri)

        assertThat(isStatsUri).isFalse()
    }

    @Test
    fun `opens stats screen from empty URL`() {
        val uri = buildUri(path1 = "stats")

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
        val uri = buildUri(path1 = "stats", path2 = siteUrl)
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
            val uri = buildUri(path1 = "stats", path2 = key, path3 = siteUrl)
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
        val uri = buildUri(path1 = "stats", path2 = "invalid", path3 = siteUrl)
        whenever(deepLinkUriUtils.hostToSite(siteUrl)).thenReturn(site)

        val buildNavigateAction = statsLinkHandler.buildNavigateAction(uri)

        assertThat(buildNavigateAction).isEqualTo(NavigateAction.OpenStatsForSite(site))
    }

    @Test
    fun `strips applink with all params`() {
        val uri = buildUri("stats", path1 = "day", path2 = "example.com")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress://stats/day/siteUrl")
    }

    @Test
    fun `strips applink without params`() {
        val uri = buildUri("stats")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress://stats")
    }

    @Test
    fun `strips deeplink with all params`() {
        val uri = buildUri("wordpress.com", path1 = "stats", path2 = "day", path3 = "example.com")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/stats/day/siteUrl")
    }

    @Test
    fun `strips deeplink without params`() {
        val uri = buildUri("wordpress.com", path1 = "stats")

        val strippedUrl = statsLinkHandler.stripUrl(uri)

        assertThat(strippedUrl).isEqualTo("wordpress.com/stats")
    }
}
