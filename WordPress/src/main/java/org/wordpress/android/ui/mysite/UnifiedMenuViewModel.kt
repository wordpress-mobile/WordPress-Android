package org.wordpress.android.ui.mysite

import android.util.Log
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsViewModelSlice
import org.wordpress.android.ui.mysite.tabs.MySiteTabType
import javax.inject.Inject
import org.wordpress.android.util.merge
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Named

data class UnifiedMenuViewState(
    val items: List<MySiteCardAndItem> // cards and or items
)

@HiltViewModel
class UnifiedMenuViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val siteItemsBuilder: SiteItemsBuilder,
    private val siteItemsViewModelSlice: SiteItemsViewModelSlice,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
) : ScopedViewModel(bgDispatcher) {
    // todo: This is a placeholder; I would like to pass along the siteNavigationAction as a channel
//    private val _actionEvents = Channel<Event<SiteNavigationAction>>(Channel.BUFFERED)
//    val actionEvents = _actionEvents.receiveAsFlow()

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val navigation = merge(_onNavigation, siteItemsViewModelSlice.onNavigation)

    private val _uiState = MutableStateFlow(UnifiedMenuViewState(items = emptyList()))

    val uiState: StateFlow<UnifiedMenuViewState> = _uiState

    fun start() {
        val site = selectedSiteRepository.getSelectedSite()!!
        buildSiteMenu(site)
    }

    private fun buildSiteMenu(site: SiteModel) {
        _uiState.value = UnifiedMenuViewState(
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
                Log.i(javaClass.simpleName, "***=> jp capabilities updated")
                _uiState.value = UnifiedMenuViewState(
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
        Log.i(javaClass.simpleName, "**=> onCleared()")
        jetpackCapabilitiesUseCase.clear()
        super.onCleared()
    }
}
