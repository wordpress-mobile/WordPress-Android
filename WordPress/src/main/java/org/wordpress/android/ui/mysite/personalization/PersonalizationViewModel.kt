package org.wordpress.android.ui.mysite.personalization

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val CARD_TYPE_TRACK_PARAM = "type"

@HiltViewModel
class PersonalizationViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val shortcutsPersonalizationViewModelSlice: ShortcutsPersonalizationViewModelSlice,
    private val dashboardCardPersonalizationViewModelSlice: DashboardCardPersonalizationViewModelSlice
) : ScopedViewModel(bgDispatcher) {
    val uiState = dashboardCardPersonalizationViewModelSlice.uiState
    val shortcutsState = shortcutsPersonalizationViewModelSlice.uiState

    init {
        shortcutsPersonalizationViewModelSlice.initialize(viewModelScope)
        dashboardCardPersonalizationViewModelSlice.initialize(viewModelScope)
    }

    fun start() {
        val siteId = selectedSiteRepository.getSelectedSite()!!.siteId
        dashboardCardPersonalizationViewModelSlice.start(siteId)
        shortcutsPersonalizationViewModelSlice.start(selectedSiteRepository.getSelectedSite()!!)
    }

    fun onCardToggled(cardType: CardType, enabled: Boolean) {
        dashboardCardPersonalizationViewModelSlice.onCardToggled(cardType, enabled)
    }

    override fun onCleared() {
        super.onCleared()
        shortcutsPersonalizationViewModelSlice.onCleared()
        dashboardCardPersonalizationViewModelSlice.onCleared()
    }

    fun removeShortcut(shortcutState: ShortcutState) {
        val siteId = selectedSiteRepository.getSelectedSite()!!.siteId
        shortcutsPersonalizationViewModelSlice.removeShortcut(shortcutState,siteId)
    }

    fun addShortcut(shortcutState: ShortcutState) {
        val siteId = selectedSiteRepository.getSelectedSite()!!.siteId
        shortcutsPersonalizationViewModelSlice.addShortcut(shortcutState,siteId)
    }
}
