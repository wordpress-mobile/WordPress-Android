package org.wordpress.android.ui.mysite.personalization

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject
import javax.inject.Named

const val SHORTCUT_NAME_TRACKING_PARAMETER = "type"

class ShortcutsPersonalizationViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    private val _uiState = MutableStateFlow(ShortcutsState(emptyList(), emptyList()))

    val uiState: StateFlow<ShortcutsState> = _uiState

    fun start(site: SiteModel) {
        convertToShortCutsState(
            items = siteItemsBuilder.build(
                MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                    site = site,
                    activeTask = null,
                    backupAvailable = false,
                    scanAvailable = false,
                    enableFocusPoints = false,
                    onClick = { },
                    isBlazeEligible = isSiteBlazeEligible(site)
                )
            ),
            site.siteId
        )
        updateSiteItemsForJetpackCapabilities(site)
    }

    private fun convertToShortCutsState(items: List<MySiteCardAndItem>, siteId: Long) {
        val listItems = items.filterIsInstance(MySiteCardAndItem.Item.ListItem::class.java)
        val shortcuts = listItems.map { listItem ->
            ShortcutState(
                icon = listItem.primaryIcon,
                label = listItem.primaryText as UiString.UiStringRes,
                disableTint = listItem.disablePrimaryIconTint,
                isActive = isActiveShortcut(listItem.listItemAction, siteId),
                listItemAction = listItem.listItemAction
            )
        }
        groupByActiveAndInactiveShortcuts(shortcuts)
    }

    private fun groupByActiveAndInactiveShortcuts(shortcuts: List<ShortcutState>) {
        _uiState.value = ShortcutsState(
            activeShortCuts = shortcuts.filter { it.isActive },
            inactiveShortCuts = shortcuts.filter { !it.isActive }
        )
    }

    private fun updateSiteItemsForJetpackCapabilities(site: SiteModel) {
        scope.launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                convertToShortCutsState(
                    items = siteItemsBuilder.build(
                        MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
                            site = site,
                            activeTask = null,
                            backupAvailable = it.backup,
                            scanAvailable = (it.scan && !site.isWPCom && !site.isWPComAtomic),
                            enableFocusPoints = false,
                            onClick = { },
                            isBlazeEligible = isSiteBlazeEligible(site)
                        )
                    ),
                    siteId = site.siteId
                )
            } // end collect
        }
    }

    private fun isSiteBlazeEligible(site: SiteModel) =
        blazeFeatureUtils.isSiteBlazeEligible(site)

    fun onCleared() {
        this.scope.cancel()
        jetpackCapabilitiesUseCase.clear()
    }

    // Note: More item is not a list item and hence the check is dropped here, it will be shown as a quick link always
    private fun isActiveShortcut(listItemAction: ListItemAction, siteId: Long): Boolean {
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

    fun removeShortcut(shortcutState: ShortcutState, siteId: Long) {
        scope.launch(bgDispatcher) {
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.PERSONALIZATION_SCREEN_SHORTCUT_HIDE_QUICK_LINK_TAPPED,
                mapOf(SHORTCUT_NAME_TRACKING_PARAMETER to shortcutState.listItemAction.trackingLabel)
            )
            if (shortcutState.listItemAction in defaultShortcuts())
                updateVisibilityOfDefaultShortcut(shortcutState.listItemAction, siteId, false)
            else updateVisibilityOfListItem(shortcutState.listItemAction, siteId, false)
            updateUiState(shortcutState, isActive = false)
        }
    }

    fun addShortcut(shortcutState: ShortcutState, siteId: Long) {
        scope.launch(bgDispatcher) {
            analyticsTrackerWrapper.track(
                AnalyticsTracker.Stat.PERSONALIZATION_SCREEN_SHORTCUT_SHOW_QUICK_LINK_TAPPED,
                mapOf(SHORTCUT_NAME_TRACKING_PARAMETER to shortcutState.listItemAction.trackingLabel)
            )
            if (shortcutState.listItemAction in defaultShortcuts())
                updateVisibilityOfDefaultShortcut(shortcutState.listItemAction, siteId, true)
            else updateVisibilityOfListItem(shortcutState.listItemAction, siteId, true)
            updateUiState(shortcutState, isActive = true)
        }
    }

    private fun updateUiState(shortcutState: ShortcutState, isActive: Boolean) {
        // is active means changed to active from inactive
        val currentState = _uiState.value
        val updatedState = shortcutState.copy(isActive = isActive)
        val activeShortcuts = currentState.activeShortCuts.toMutableList()
        val inactiveShortcuts = currentState.inactiveShortCuts.toMutableList()
        if (isActive) {
            inactiveShortcuts.remove(shortcutState)
            activeShortcuts.add(updatedState)
        } else {
            activeShortcuts.remove(shortcutState)
            inactiveShortcuts.add(updatedState)
        }
        _uiState.value = ShortcutsState(
            activeShortCuts = activeShortcuts,
            inactiveShortCuts = inactiveShortcuts
        )
    }

    private fun updateVisibilityOfListItem(listItemAction: ListItemAction, siteId: Long, shouldShow: Boolean) {
        appPrefsWrapper.setShouldShowSiteItemAsQuickLink(
            listItemAction.toString(), siteId, shouldShow
        )
    }

    private fun updateVisibilityOfDefaultShortcut(listItemAction: ListItemAction, siteId: Long, shouldShow: Boolean) {
        appPrefsWrapper.setShouldShowDefaultQuickLink(
            listItemAction.toString(), siteId, shouldShow
        )
    }
}


