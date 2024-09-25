package org.wordpress.android.ui.mysite.cards

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.quickstart.QuickStartEvent
import javax.inject.Inject

class ListItemActionHandler @Inject constructor(
    private val accountStore: AccountStore,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val blazeFeatureUtils: BlazeFeatureUtils
) {
    fun handleAction(
        action: ListItemAction,
        selectedSite: SiteModel,
        quickStartEvent: QuickStartEvent? = null
    ): SiteNavigationAction {
        return when (action) {
            ListItemAction.ACTIVITY_LOG -> SiteNavigationAction.OpenActivityLog(selectedSite)
            ListItemAction.BACKUP -> SiteNavigationAction.OpenBackup(selectedSite)
            ListItemAction.SCAN -> SiteNavigationAction.OpenScan(selectedSite)
            ListItemAction.PLAN -> SiteNavigationAction.OpenPlan(selectedSite)
            ListItemAction.POSTS -> SiteNavigationAction.OpenPosts(selectedSite)
            ListItemAction.PAGES -> SiteNavigationAction.OpenPages(selectedSite)
            ListItemAction.ADMIN -> SiteNavigationAction.OpenAdmin(selectedSite)
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
            ListItemAction.MORE -> SiteNavigationAction.OpenMore(selectedSite, quickStartEvent)
            ListItemAction.SITE_MONITORING -> SiteNavigationAction.OpenSiteMonitoring(selectedSite)
        }
    }

    private fun getStatsNavigationActionForSite(site: SiteModel): SiteNavigationAction = when {
        // if we are in static posters phase - we don't want to show any connection/login messages
        jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage() ->
            SiteNavigationAction.ShowJetpackRemovalStaticPostersView

        // If the user is not logged in and the site is already connected to Jetpack, ask to login.
        !accountStore.hasAccessToken() && site.isJetpackConnected -> SiteNavigationAction.StartWPComLoginForJetpackStats

        // If it's a WordPress.com or Jetpack site, show the Stats screen.
        site.isWPCom || site.isJetpackInstalled && site.isJetpackConnected -> SiteNavigationAction.OpenStats(site)

        // If it's a self-hosted site, ask to connect to Jetpack.
        else -> SiteNavigationAction.ConnectJetpackForStats(site)
    }

    private fun onBlazeMenuItemClick(): SiteNavigationAction {
        if (blazeFeatureUtils.shouldShowBlazeCampaigns()) {
            return SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.MENU_ITEM)
        }
        return SiteNavigationAction.OpenPromoteWithBlazeOverlay(BlazeFlowSource.MENU_ITEM)
    }
}
