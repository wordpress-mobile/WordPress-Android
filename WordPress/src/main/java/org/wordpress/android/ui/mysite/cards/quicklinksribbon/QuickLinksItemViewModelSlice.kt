package org.wordpress.android.ui.mysite.cards.quicklinksribbon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore
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
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Named

class QuickLinksItemViewModelSlice @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val listItemActionHandler: ListItemActionHandler,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    lateinit var scope: CoroutineScope

    lateinit var site: SiteModel

    fun initialization(scope: CoroutineScope) {
        this.scope = scope
        this.site = selectedSiteRepository.getSelectedSite()!!
    }

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val navigation = _onNavigation

    private val _onSnackbarMessage = MutableLiveData<Event<SnackbarMessageHolder>>()
    val onSnackbarMessage = _onSnackbarMessage

    private val _uiState = MutableLiveData<MySiteCardAndItem.Card.QuickLinkRibbon>()
    val uiState: LiveData<MySiteCardAndItem.Card.QuickLinkRibbon> = _uiState

    fun start() {
        buildQuickLinks()
    }

    fun onResume() {
        buildQuickLinks()
    }

    fun onRefresh() {
        buildQuickLinks()
    }

    private fun buildQuickLinks() {
        scope.launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                _uiState.postValue(
                    convertToQuickLinkRibbonItem(
                        siteItemsBuilder.build(
                            MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                                site = site,
                                enableFocusPoints = true,
                                activeTask = null,
                                onClick = this@QuickLinksItemViewModelSlice::onClick,
                                isBlazeEligible = isSiteBlazeEligible(),
                                backupAvailable = it.backup,
                                scanAvailable = (it.scan && !site.isWPCom && !site.isWPComAtomic)
                            )
                        ),
                    )
                )
            } // end collect
        }
    }

    private fun convertToQuickLinkRibbonItem(
        listItems: List<MySiteCardAndItem>,
    ): MySiteCardAndItem.Card.QuickLinkRibbon {
        val siteId = selectedSiteRepository.getSelectedSite()!!.siteId
        val activeListItems = listItems.filterIsInstance(MySiteCardAndItem.Item.ListItem::class.java)
            .filter { isActiveQuickLink(it.listItemAction, siteId = siteId) }
        val activeQuickLinks = activeListItems.map { listItem ->
            MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem(
                icon = listItem.primaryIcon,
                disableTint = listItem.disablePrimaryIconTint,
                label = (listItem.primaryText as UiString.UiStringRes),
                onClick = listItem.onClick,
                listItemAction = listItem.listItemAction,
            )
        }
        val moreQuickLink = MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem(
            icon = R.drawable.ic_more_horiz_white_24dp,
            label = UiString.UiStringRes(R.string.more),
            onClick = ListItemInteraction.create(
                ListItemAction.MORE,
                this@QuickLinksItemViewModelSlice::onClick
            ),
            listItemAction = ListItemAction.MORE
        )
        return MySiteCardAndItem.Card.QuickLinkRibbon(
            quickLinkRibbonItems = activeQuickLinks + moreQuickLink
        )
    }

    private fun isSiteBlazeEligible() =
        blazeFeatureUtils.isSiteBlazeEligible(selectedSiteRepository.getSelectedSite()!!)


    private fun onClick(action: ListItemAction) {
        selectedSiteRepository.getSelectedSite()?.let { selectedSite ->
            // add the tracking logic here
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

    fun updateToShowMoreFocusPointIfNeeded(
        quickLinks: MySiteCardAndItem.Card.QuickLinkRibbon,
        activeTask: QuickStartStore.QuickStartTask
    ): MySiteCardAndItem.Card.QuickLinkRibbon {
        val updatedQuickLinks = if (shouldShowMoreFocusPoint(quickLinks.quickLinkRibbonItems, activeTask)) {
            val quickLinkItems = quickLinks.quickLinkRibbonItems.toMutableList()
            val lastItem = quickLinkItems.last().copy(showFocusPoint = true)
            quickLinkItems.removeLast()
            quickLinkItems.add(lastItem)
            quickLinks.copy(quickLinkRibbonItems = quickLinkItems, showMoreFocusPoint = true)
        } else {
            quickLinks
        }
        return updatedQuickLinks
    }

    private fun shouldShowMoreFocusPoint(
        activeShortcuts: List<MySiteCardAndItem.Card.QuickLinkRibbon.QuickLinkRibbonItem>,
        activeTask: QuickStartStore.QuickStartTask?
    ): Boolean {
        if (activeTask == null) return false
        activeShortcuts.find { it.listItemAction in isQuickStartFocusListItemAction() }?.let {
            return isActiveTaskInMoreMenu(activeTask)
        }
        return false
    }

    private fun isQuickStartFocusListItemAction(): List<ListItemAction> {
        return listOf(
            ListItemAction.POSTS,
            ListItemAction.PAGES,
            ListItemAction.STATS,
            ListItemAction.MEDIA
        )
    }

    private fun isActiveTaskInMoreMenu(activeTask: QuickStartStore.QuickStartTask?): Boolean {
        return activeTask == QuickStartStore.QuickStartNewSiteTask.REVIEW_PAGES ||
                activeTask == QuickStartStore.QuickStartNewSiteTask.CHECK_STATS ||
                activeTask == QuickStartStore.QuickStartExistingSiteTask.UPLOAD_MEDIA ||
                activeTask == QuickStartStore.QuickStartNewSiteTask.ENABLE_POST_SHARING
    }
}
