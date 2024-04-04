package org.wordpress.android.ui.mysite.items.listitem

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

private const val TYPE = "type"

@Suppress("LongParameterList")
class SiteItemsViewModelSlice @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val listItemActionHandler: ListItemActionHandler,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    private val _uiModel = MutableLiveData<List<MySiteCardAndItem>?>()
    val uiModel: LiveData<List<MySiteCardAndItem>?> = _uiModel.distinctUntilChanged()

    // Quick start is disabled in all the cases where site items are built.
    suspend fun buildSiteItems(
        site: SiteModel
    ) {
        _uiModel.postValue(
            siteItemsBuilder.build(
                getParams(
                    shouldEnableFocusPoints = false,
                    site = site,
                    backupAvailable = false,
                    scanAvailable = false
                )
            )
        )
        rebuildSiteItemsForJetpackCapabilities(site)
    }

    private suspend fun rebuildSiteItemsForJetpackCapabilities(site: SiteModel) {
        jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect { purchasedProducts ->
            // if the site has scan or backup enabled, then only rebuild the site items
            if(purchasedProducts.scan || purchasedProducts.backup) {
                val items = siteItemsBuilder.build(
                    getParams(
                        shouldEnableFocusPoints = false,
                        site = site,
                        activeTask = null,
                        backupAvailable = purchasedProducts.backup,
                        scanAvailable = purchasedProducts.scan && !site.isWPCom && !site.isWPComAtomic
                    )
                )
                _uiModel.postValue(items)
            }
        } // end collect
    }

    fun getParams(
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
            isBlazeEligible = isSiteBlazeEligible(site)
        )
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun onItemClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.MY_SITE_MENU_ITEM_TAPPED,
                mapOf(TYPE to action.trackingLabel)
            )
            _onNavigation.postValue(Event(listItemActionHandler.handleAction(action, selectedSite)))
        } ?: run {
            _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.site_cannot_be_loaded)))
            )
        }
    }

    private fun isSiteBlazeEligible(site: SiteModel) =
        blazeFeatureUtils.isSiteBlazeEligible(site)

    fun clearValue() {
        _uiModel.postValue(null)
    }
}
