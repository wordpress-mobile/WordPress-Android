package org.wordpress.android.ui.mysite.menu

import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsViewModelSlice
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import javax.inject.Inject
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Named

data class MenuViewState(
    val items: List<MySiteCardAndItem> // cards and or items
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val siteItemsViewModelSlice: SiteItemsViewModelSlice,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
) : ScopedViewModel(bgDispatcher) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val navigation = merge(_onNavigation, siteItemsViewModelSlice.onNavigation)

    private val _uiState = MutableStateFlow(MenuViewState(items = emptyList()))

    val uiState: StateFlow<MenuViewState> = _uiState

    fun start() {
        val site = selectedSiteRepository.getSelectedSite()!!
        buildSiteMenu(site)
    }

    private fun buildSiteMenu(site: SiteModel) {
        _uiState.value = MenuViewState(
            items = siteItemsBuilder.build(
                siteItemsViewModelSlice.buildItems(
                    defaultTab = MySiteTabType.SITE_MENU,
                    site = site,
                    activeTask = null,
                    backupAvailable = false,
                    scanAvailable = false
                )
            )
        )
        updateSiteItemsForJetpackCapabilities(site)
    }

    private fun updateSiteItemsForJetpackCapabilities(site: SiteModel) {
        launch(bgDispatcher) {
            jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(site.siteId).collect {
                _uiState.value = MenuViewState(
                    items = siteItemsBuilder.build(
                        siteItemsViewModelSlice.buildItems(
                            defaultTab = MySiteTabType.SITE_MENU,
                            site = site,
                            activeTask = null,
                            backupAvailable = it.backup,
                            scanAvailable = (it.scan && !site.isWPCom && !site.isWPComAtomic)
                        )
                    )
                )
            } // end collect
        }
    }

    override fun onCleared() {
        jetpackCapabilitiesUseCase.clear()
        super.onCleared()
    }
}
