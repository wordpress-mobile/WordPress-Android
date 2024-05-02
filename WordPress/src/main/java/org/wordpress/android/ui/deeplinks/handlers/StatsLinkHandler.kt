package org.wordpress.android.ui.deeplinks.handlers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenJetpackStaticPosterView
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStats
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForSite
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForSiteAndTimeframe
import org.wordpress.android.ui.deeplinks.DeepLinkNavigator.NavigateAction.OpenStatsForTimeframe
import org.wordpress.android.ui.deeplinks.DeepLinkUriUtils
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.APPLINK_SCHEME
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.HOST_WORDPRESS_COM
import org.wordpress.android.ui.deeplinks.DeepLinkingIntentReceiverViewModel.Companion.SITE_DOMAIN
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.stats.StatsTimeframe
import org.wordpress.android.util.UriWrapper
import javax.inject.Inject

class StatsLinkHandler
@Inject constructor(
    private val deepLinkUriUtils: DeepLinkUriUtils,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) : DeepLinkHandler {
    /**
     * Builds navigate action from URL like:
     * https://wordpress.com/stats/$timeframe/$site
     * where timeframe and site are optional
     * or
     * wordpress://stats
     */
    override fun buildNavigateAction(uri: UriWrapper): NavigateAction {
        val pathSegments = uri.pathSegments
        val length = pathSegments.size
        val site = pathSegments.getOrNull(length - 1)?.toSite()
        val timeframeIndex = if (site == null) (length - 1) else (length - 2)
        val statsTimeframe = pathSegments.getOrNull(timeframeIndex)?.toStatsTimeframe()
        return when {
            jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage() -> OpenJetpackStaticPosterView
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
     * The handled links are `https://wordpress.com/stats/day/$site` and `wordpress://stats`
     */
    override fun shouldHandleUrl(uri: UriWrapper): Boolean {
        return (uri.host == HOST_WORDPRESS_COM &&
                uri.pathSegments.firstOrNull() == STATS_PATH) || uri.host == STATS_PATH
    }

    override fun stripUrl(uri: UriWrapper): String {
        return buildString {
            val offset = if (uri.host == STATS_PATH) {
                append(APPLINK_SCHEME)
                0
            } else {
                append("$HOST_WORDPRESS_COM/")
                1
            }
            append(STATS_PATH)
            val pathSegments = uri.pathSegments
            val size = pathSegments.size
            val statsTimeframe = if (size > offset) pathSegments.getOrNull(offset) else null
            val hasSiteUrl = if (size > offset + 1) pathSegments.getOrNull(offset + 1) != null else false
            if (statsTimeframe != null) {
                append("/$statsTimeframe")
            }
            if (hasSiteUrl) {
                append("/$SITE_DOMAIN")
            }
        }
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
            "subscribers" -> StatsTimeframe.SUBSCRIBERS
            else -> null
        }
    }

    companion object {
        private const val STATS_PATH = "stats"
    }
}
