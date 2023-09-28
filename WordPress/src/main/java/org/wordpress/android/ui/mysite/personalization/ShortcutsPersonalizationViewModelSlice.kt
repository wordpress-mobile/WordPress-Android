package org.wordpress.android.ui.mysite.personalization

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject
import javax.inject.Named

class ShortcutsPersonalizationViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val blazeFeatureUtils: BlazeFeatureUtils,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    private val _uiState = MutableStateFlow(ShortcutsState(emptyList(), emptyList()))

    val uiState: StateFlow<ShortcutsState> = _uiState

    fun start(site: SiteModel) {
        _uiState.value = convertToShortCutsState(
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

    private fun convertToShortCutsState(items: List<MySiteCardAndItem>, siteId: Long): ShortcutsState {
        val listItems = items.filterIsInstance(MySiteCardAndItem.Item.ListItem::class.java)
        val shortcuts = listItems.map { listItem ->
            ShortcutState(
                icon = listItem.primaryIcon,
                label = listItem.primaryText as UiString.UiStringRes,
                disableTint = listItem.disablePrimaryIconTint,
                isActive = isActiveShortcut(listItem.listItemAction, siteId)
            )
        }
        return ShortcutsState(
            activeShortCuts = shortcuts.filter { it.isActive },
            inactiveShortCuts = shortcuts.filter { !it.isActive }
        )
    }

    private fun updateSiteItemsForJetpackCapabilities(site: SiteModel) {
        scope.launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                _uiState.value = convertToShortCutsState(
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
            ListItemAction.POSTS, ListItemAction.PAGES, ListItemAction.STATS -> {
                appPrefsWrapper.getShouldShowDefaultQuickLink(
                    listItemAction.toString(), siteId
                )
            }
            else -> {
                appPrefsWrapper.getShouldShowSiteItemAsQuickLink(
                    listItemAction.toString(), siteId)
            }
        }
    }
}


