package org.wordpress.android.ui.mysite.items.listitem

import androidx.lifecycle.MutableLiveData
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.fluxc.store.QuickStartStore.Companion.QUICK_START_CHECK_STATS_LABEL
import org.wordpress.android.fluxc.store.QuickStartStore.Companion.QUICK_START_UPLOAD_MEDIA_LABEL
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.blazecampaigns.campaignlisting.CampaignListingPageSource
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Singleton

private const val TYPE = "type"

@Singleton
@Suppress("LongParameterList")
class SiteItemsViewModelSlice @Inject constructor(
    private val quickStartRepository: QuickStartRepository,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val accountStore: AccountStore,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val blazeFeatureUtils: BlazeFeatureUtils,
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    fun buildItems(
        shouldEnableFocusPoints: Boolean = false,
        site: SiteModel,
        activeTask: QuickStartStore.QuickStartTask? = null,
        backupAvailable: Boolean = false,
        scanAvailable: Boolean = false
    ): MySiteCardAndItemBuilderParams.SiteItemsBuilderParams {
        return MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
            site = site,
            activeTask = activeTask,
            backupAvailable = backupAvailable,
            scanAvailable = scanAvailable,
            enableFocusPoints = shouldEnableFocusPoints,
            onClick = this::onItemClick,
            isBlazeEligible = isSiteBlazeEligible()
        )
    }

    @Suppress("ComplexMethod")
    private fun onItemClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.MY_SITE_MENU_ITEM_TAPPED,
                mapOf(TYPE to action.trackingLabel)
            )
            val navigationAction = when (action) {
                ListItemAction.ACTIVITY_LOG -> SiteNavigationAction.OpenActivityLog(selectedSite)
                ListItemAction.BACKUP -> SiteNavigationAction.OpenBackup(selectedSite)
                ListItemAction.SCAN -> SiteNavigationAction.OpenScan(selectedSite)
                ListItemAction.PLAN -> SiteNavigationAction.OpenPlan(selectedSite)
                ListItemAction.POSTS -> SiteNavigationAction.OpenPosts(selectedSite)
                ListItemAction.PAGES -> {
                    quickStartRepository.completeTask(QuickStartStore.QuickStartNewSiteTask.REVIEW_PAGES)
                    SiteNavigationAction.OpenPages(selectedSite)
                }
                ListItemAction.ADMIN -> SiteNavigationAction.OpenAdmin(selectedSite)
                ListItemAction.PEOPLE -> SiteNavigationAction.OpenPeople(selectedSite)
                ListItemAction.SHARING -> {
                    quickStartRepository.requestNextStepOfTask(
                        QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
                    )
                    SiteNavigationAction.OpenSharing(selectedSite)
                }
                ListItemAction.DOMAINS -> SiteNavigationAction.OpenDomains(selectedSite)
                ListItemAction.ME -> SiteNavigationAction.OpenMeScreen
                ListItemAction.SITE_SETTINGS -> SiteNavigationAction.OpenSiteSettings(selectedSite)
                ListItemAction.THEMES -> SiteNavigationAction.OpenThemes(selectedSite)
                ListItemAction.PLUGINS -> SiteNavigationAction.OpenPlugins(selectedSite)
                ListItemAction.STATS -> {
                    quickStartRepository.completeTask(
                        quickStartRepository.quickStartType.getTaskFromString(QUICK_START_CHECK_STATS_LABEL)
                    )
                    getStatsNavigationActionForSite(selectedSite)
                }

                ListItemAction.MEDIA -> {
                    quickStartRepository.requestNextStepOfTask(
                        quickStartRepository.quickStartType.getTaskFromString(QUICK_START_UPLOAD_MEDIA_LABEL)
                    )
                    SiteNavigationAction.OpenMedia(selectedSite)
                }

                ListItemAction.COMMENTS -> SiteNavigationAction.OpenUnifiedComments(selectedSite)

                ListItemAction.BLAZE -> onBlazeMenuItemClick()
                ListItemAction.MORE ->  TODO()
            }
            _onNavigation.postValue(Event(navigationAction))
        } ?: _onSnackbarMessage.postValue(
            Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.site_cannot_be_loaded)))
        )
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

    private fun isSiteBlazeEligible() =
        blazeFeatureUtils.isSiteBlazeEligible(selectedSiteRepository.getSelectedSite()!!)

    private fun onBlazeMenuItemClick(): SiteNavigationAction {
        blazeFeatureUtils.trackEntryPointTapped(BlazeFlowSource.MENU_ITEM)
        if (blazeFeatureUtils.shouldShowBlazeCampaigns()) {
            return SiteNavigationAction.OpenCampaignListingPage(CampaignListingPageSource.MENU_ITEM)
        }
        return SiteNavigationAction.OpenPromoteWithBlazeOverlay(BlazeFlowSource.MENU_ITEM)
    }
}
