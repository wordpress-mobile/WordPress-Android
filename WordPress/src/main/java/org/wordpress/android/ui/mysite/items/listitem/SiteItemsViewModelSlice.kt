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
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
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
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val listItemActionHandler: ListItemActionHandler
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
            _onNavigation.postValue(Event(listItemActionHandler.handleAction(action, selectedSite)))
        }?: run {
            _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.site_cannot_be_loaded)))
            )
        }
    }

    private fun isSiteBlazeEligible() =
        blazeFeatureUtils.isSiteBlazeEligible(selectedSiteRepository.getSelectedSite()!!)
}
