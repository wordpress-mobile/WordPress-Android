package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.newstats.NewStatsRouting
import org.wordpress.android.util.WpComSiteAccessChecker
import javax.inject.Inject

class ListItemActionHandler @Inject constructor(
    private val accountStore: AccountStore,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val newStatsRouting: NewStatsRouting,
    private val wpComSiteAccessChecker: WpComSiteAccessChecker
) {
    fun handleAction(
        action: ListItemAction,
        selectedSite: SiteModel
    ): SiteNavigationAction {
        return when (action) {
            ListItemAction.ACTIVITY_LOG -> SiteNavigationAction.OpenActivityLog(selectedSite)
            ListItemAction.BACKUP -> SiteNavigationAction.OpenBackup(selectedSite)
            ListItemAction.SCAN -> SiteNavigationAction.OpenScan(selectedSite)
            ListItemAction.POSTS -> SiteNavigationAction.OpenPosts(selectedSite)
            ListItemAction.PAGES -> SiteNavigationAction.OpenPages(selectedSite)
            ListItemAction.POST_TYPES -> SiteNavigationAction.OpenPostTypes(selectedSite)
            ListItemAction.ADMIN -> SiteNavigationAction.OpenAdmin(selectedSite)
            ListItemAction.SUBSCRIBERS -> SiteNavigationAction.OpenSubscribers(selectedSite)
            ListItemAction.PEOPLE -> SiteNavigationAction.OpenPeople(selectedSite)
            ListItemAction.SELF_HOSTED_USERS -> SiteNavigationAction.OpenSelfHostedUsers(selectedSite)
            ListItemAction.SHARING -> SiteNavigationAction.OpenSharing(selectedSite)
            ListItemAction.DOMAINS -> SiteNavigationAction.OpenDomains(selectedSite)
            ListItemAction.ME -> SiteNavigationAction.OpenMeScreen
            ListItemAction.SITE_SETTINGS -> SiteNavigationAction.OpenSiteSettings(selectedSite)
            ListItemAction.THEMES -> SiteNavigationAction.OpenThemes(selectedSite)
            ListItemAction.PLUGINS -> SiteNavigationAction.OpenPlugins(selectedSite)
            ListItemAction.STATS -> getStatsNavigationActionForSite(selectedSite)
            ListItemAction.MEDIA -> SiteNavigationAction.OpenMedia(selectedSite)
            ListItemAction.COMMENTS -> SiteNavigationAction.OpenUnifiedComments(selectedSite)
            ListItemAction.BLAZE -> onBlazeMenuItemClick()
            ListItemAction.MORE -> SiteNavigationAction.OpenMore(selectedSite)
            ListItemAction.SITE_MONITORING -> SiteNavigationAction.OpenSiteMonitoring(selectedSite)
            ListItemAction.APPLICATION_PASSWORDS -> SiteNavigationAction.OpenApplicationPasswordsList
            ListItemAction.MENUS -> SiteNavigationAction.OpenMenus(selectedSite)
        }
    }

    private fun getStatsNavigationActionForSite(site: SiteModel): SiteNavigationAction = when {
        // If the user is not logged in and the site is already connected to Jetpack, ask to login.
        !accountStore.hasAccessToken() && site.isJetpackConnected -> SiteNavigationAction.StartWPComLoginForJetpackStats

        // If it's a WordPress.com or Jetpack site, show the Stats screen. Stats are served by
        // WordPress.com, so a Jetpack site also has to be reachable by the account signed in here --
        // isJetpackConnected only says the site is connected to *some* account, and one connected to a
        // different account reaches the right site and is still refused. See CMM-2344.
        site.isWPCom ||
                (site.isJetpackInstalled && site.isJetpackConnected &&
                        wpComSiteAccessChecker.hasWpComAccess(site)) -> {
            if (newStatsRouting.isNewStatsEnabled()) {
                SiteNavigationAction.OpenNewStats
            } else {
                SiteNavigationAction.OpenStats(site)
            }
        }

        // If it's a self-hosted site, ask to connect to Jetpack -- unless we already hold a WordPress.com
        // copy of it that can serve Stats.
        else -> statsViaWpComCopyOrConnect(site)
    }

    /**
     * The same site can be stored twice, once from an application password and once from /me/sites, because
     * the application-password row can't take the blog id the WordPress.com row already owns. Stats work
     * through that copy, so prefer it over offering to connect a site that already is connected.
     *
     * New Stats always targets the selected site, so this deliberately uses the existing Stats screen,
     * which can be pointed at another site.
     */
    private fun statsViaWpComCopyOrConnect(site: SiteModel): SiteNavigationAction =
        wpComSiteAccessChecker.wpComCounterpart(site)
            ?.let { SiteNavigationAction.OpenStats(it) }
            ?: SiteNavigationAction.ConnectJetpackForStats(site)

    private fun onBlazeMenuItemClick(): SiteNavigationAction {
        if (blazeFeatureUtils.shouldShowBlazeCampaigns()) {
            return SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.MENU_ITEM)
        }
        return SiteNavigationAction.OpenPromoteWithBlazeOverlay(BlazeFlowSource.MENU_ITEM)
    }
}
