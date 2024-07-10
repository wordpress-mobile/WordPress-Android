package org.wordpress.android.ui.mysite.personalization

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val CARD_TYPE_TRACK_PARAM = "type"

@HiltViewModel
class PersonalizationViewModel @Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val shortcutsPersonalizationViewModelSlice: ShortcutsPersonalizationViewModelSlice,
    private val dashboardCardPersonalizationViewModelSlice: DashboardCardPersonalizationViewModelSlice,
    private val localeManagerWrapper: LocaleManagerWrapper
) : ScopedViewModel(bgDispatcher) {
    val uiState = dashboardCardPersonalizationViewModelSlice.uiState
    val shortcutsState = shortcutsPersonalizationViewModelSlice.uiState

    private val _onSelectedSiteMissing = MutableLiveData<Unit>()
    val onSelectedSiteMissing = _onSelectedSiteMissing as LiveData<Unit>

    private val _appLanguage = MutableLiveData<String>()
    val appLanguage = _appLanguage as LiveData<String>

    init {
        emitLanguageRefreshIfNeeded(localeManagerWrapper.getLanguage())
        shortcutsPersonalizationViewModelSlice.initialize(viewModelScope)
        dashboardCardPersonalizationViewModelSlice.initialize(viewModelScope)
    }

    fun start() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            _onSelectedSiteMissing.value = Unit
            return
        }

        dashboardCardPersonalizationViewModelSlice.start(site.siteId)
        shortcutsPersonalizationViewModelSlice.start(site)
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

    private fun emitLanguageRefreshIfNeeded(languageCode: String) {
        if (languageCode.isNotEmpty()) {
            val shouldEmitLanguageRefresh = !localeManagerWrapper.isSameLanguage(languageCode)
            if (shouldEmitLanguageRefresh) {
                _appLanguage.value = languageCode
            }
        }
    }
}
