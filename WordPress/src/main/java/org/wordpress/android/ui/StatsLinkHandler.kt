package org.wordpress.android.ui

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStats
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStatsForSite
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStatsForSiteAndTimeframe
import org.wordpress.android.ui.DeepLinkNavigator.NavigateAction.OpenStatsForTimeframe
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class StatsLinkHandler
@Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils
) {
    /**
     * Builds navigate action from URL like:
     * https://wordpress.com/stats/$timeframe/$site
     * where timeframe and site are optional
     */
    fun buildOpenStatsNavigateAction(uri: UriWrapper): NavigateAction {
        val pathSegments = uri.pathSegments
        val length = pathSegments.size
        val site = pathSegments.getOrNull(length - 1)?.toSite()
        val statsTimeframe = pathSegments.getOrNull(length - 2)?.toStatsTimeframe()
        return when {
            site != null && statsTimeframe != null -> {
                OpenStatsForSiteAndTimeframe(site, statsTimeframe)
            }
            site != null -> {
                OpenStatsForSite(site)
            }
            statsTimeframe != null -> {
                OpenStatsForTimeframe(statsTimeframe)
            }
            else -> {
                // In other cases, launch stats with the current selected site.
                OpenStats
            }
        }
    }

    /**
     * Returns true if the URI should be handled by StatsLinkHandler.
     * The handled links are `https://wordpress.com/stats/day/$site`
     */
    fun isStatsUrl(uri: UriWrapper): Boolean {
        return uri.host == DeepLinkingIntentReceiverViewModel.HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == STATS_PATH
    }

    /**
     * Converts HOST name of a site to SiteModel. It finds the Site in the current local sites and matches the name
     * to the host.
     */
    private fun String.toSite(): SiteModel? {
        return deepLinkUriUtils.hostToSite(this)
    }

    private fun String.toStatsTimeframe(): StatsTimeframe? {
        return when (this) {
            "day" -> StatsTimeframe.DAY
            "week" -> StatsTimeframe.WEEK
            "month" -> StatsTimeframe.MONTH
            "year" -> StatsTimeframe.YEAR
            "insights" -> StatsTimeframe.INSIGHTS
            else -> null
        }
    }
    companion object {
        private const val STATS_PATH = "stats"
    }
}
