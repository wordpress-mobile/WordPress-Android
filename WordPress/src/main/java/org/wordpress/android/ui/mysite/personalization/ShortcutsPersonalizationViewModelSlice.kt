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
import org.wordpress.android.ui.utils.UiString
import javax.inject.Inject
import javax.inject.Named

class ShortcutsPersonalizationViewModelSlice @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    private val blazeFeatureUtils: BlazeFeatureUtils
) {
    lateinit var scope: CoroutineScope

    fun initialize(scope: CoroutineScope) {
        this.scope = scope
    }

    private val _uiState = MutableStateFlow(emptyList<ShortcutsState>())

    val uiState: StateFlow<List<ShortcutsState>> = _uiState

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
            )
        )
        updateSiteItemsForJetpackCapabilities(site)
    }

    private fun convertToShortCutsState(items: List<MySiteCardAndItem>): List<ShortcutsState> {
        val listItems = items.filterIsInstance(MySiteCardAndItem.Item.ListItem::class.java)
        return listItems.map { listItem ->
            ShortcutsState(
                icon = listItem.primaryIcon,
                label = listItem.primaryText as UiString.UiStringRes,
                disableTint = listItem.disablePrimaryIconTint
            )
        }
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
                    )
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
}


