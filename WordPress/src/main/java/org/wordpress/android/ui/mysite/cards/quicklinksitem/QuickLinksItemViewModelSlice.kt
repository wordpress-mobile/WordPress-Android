package org.wordpress.android.ui.mysite.cards.quicklinksitem

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

const val QUICK_LINK_TRACKING_PARAMETER = "quick_link"

class QuickLinksItemViewModelSlice @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val listItemActionHandler: ListItemActionHandler,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    lateinit var scope: CoroutineScope

    fun initialization(scope: CoroutineScope) {
        this.scope = scope
    }

    fun site() = selectedSiteRepository.getSelectedSite()

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val navigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    private val _uiState = MutableLiveData<MySiteCardAndItem.Card.QuickLinksItem?>()
    val uiState: LiveData<MySiteCardAndItem.Card.QuickLinksItem?> = _uiState

    fun buildCard(siteModel: SiteModel) {
        buildQuickLinks(siteModel)
    }

    private fun buildQuickLinks(site: SiteModel) {
        scope.launch {
            _uiState.postValue(
                convertToQuickLinkRibbonItem(
                    site,
                    siteItemsBuilder.build(
                        MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                            site = site,
                            enableFocusPoints = false,
                            onClick = this@QuickLinksItemViewModelSlice::onClick,
                            isBlazeEligible = isSiteBlazeEligible(site),
                            backupAvailable = true,
                            scanAvailable = (!site.isWPCom && !site.isWPComAtomic)
                        )
                    ),
                )
            )
        }
    }

    private fun fetchCapabilities(site: SiteModel) {
        scope.launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                _uiState.postValue(
                    convertToQuickLinkRibbonItem(
                        site,
                        siteItemsBuilder.build(
                            MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                                site = site,
                                enableFocusPoints = false,
                                onClick = this@QuickLinksItemViewModelSlice::onClick,
                                isBlazeEligible = isSiteBlazeEligible(site),
                                backupAvailable = it.backup,
                                scanAvailable = it.scan
                            )
                        ),
                        capabilitiesFetched = true
                    ),
                )
            }
        }
    }

    private fun convertToQuickLinkRibbonItem(
        site: SiteModel,
        listItems: List<MySiteCardAndItem>,
        capabilitiesFetched: Boolean = false
    ): MySiteCardAndItem.Card.QuickLinksItem {
        val siteId = site.siteId
        val activeListItems = listItems.filterIsInstance(MySiteCardAndItem.Item.ListItem::class.java)
            .filter { isActiveQuickLink(it.listItemAction, siteId = siteId) }

        // Only fetch the capabilities if the user has activity and back up quick link is active
        if (!capabilitiesFetched) {
            val shouldRequestScanAndBackUpCapability =
                shouldRequestForBackupCapability(activeListItems) || shouldRequestForScanCapability(activeListItems)

            if (shouldRequestScanAndBackUpCapability) {
                fetchCapabilities(site)
            }
        }

        val activeQuickLinks = activeListItems.map { listItem ->
            MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem(
                icon = listItem.primaryIcon,
                disableTint = listItem.disablePrimaryIconTint,
                label = (listItem.primaryText as UiString.UiStringRes),
                onClick = listItem.onClick
            )
        }
        val moreQuickLink = MySiteCardAndItem.Card.QuickLinksItem.QuickLinkItem(
            icon = R.drawable.ic_more_horiz_white_24dp,
            label = UiString.UiStringRes(R.string.more),
            onClick = ListItemInteraction.create(
                ListItemAction.MORE,
                this@QuickLinksItemViewModelSlice::onClick
            )
        )
        return MySiteCardAndItem.Card.QuickLinksItem(
            quickLinkItems = activeQuickLinks + moreQuickLink
        )
    }

    // if there is scan and backup capabilities in active quick links, then only request for that
    private fun shouldRequestForBackupCapability(activeListItems: List<MySiteCardAndItem.Item.ListItem>): Boolean {
        return activeListItems.any { it.listItemAction == ListItemAction.BACKUP }
    }

    private fun shouldRequestForScanCapability(activeListItems: List<MySiteCardAndItem.Item.ListItem>): Boolean {
        return activeListItems.any { it.listItemAction == ListItemAction.SCAN }
    }

    private fun isSiteBlazeEligible(site: SiteModel) =
        blazeFeatureUtils.isSiteBlazeEligible(site)

    private fun onClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.QUICK_LINK_ITEM_TAPPED,
                mapOf(QUICK_LINK_TRACKING_PARAMETER to action.trackingLabel)
            )
            _onNavigation.postValue(Event(listItemActionHandler.handleAction(action, selectedSite)))
        } ?: run {
            _onSnackbarMessage.postValue(
                Event(SnackbarMessageHolder(UiString.UiStringRes(R.string.site_cannot_be_loaded)))
            )
        }
    }

    fun onCleared() {
        jetpackCapabilitiesUseCase.clear()
    }

    private fun isActiveQuickLink(listItemAction: ListItemAction, siteId: Long): Boolean {
        return when (listItemAction) {
            in defaultShortcuts() -> {
                appPrefsWrapper.getShouldShowDefaultQuickLink(
                    listItemAction.toString(), siteId
                )
            }

            else -> {
                appPrefsWrapper.getShouldShowSiteItemAsQuickLink(
                    listItemAction.toString(), siteId
                )
            }
        }
    }

    private fun defaultShortcuts(): List<ListItemAction> {
        return listOf(
            ListItemAction.POSTS,
            ListItemAction.PAGES,
            ListItemAction.STATS
        )
    }

    fun clearValue() {
        _uiState.postValue(null)
    }
}
