package org.wordpress.android.ui.newstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsCardsConfigurationRepository
import javax.inject.Inject

/**
 * ViewModel for managing stats cards configuration.
 * Handles visible/hidden cards state and card add/remove operations.
 */
@HiltViewModel
class NewStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardConfigurationRepository: StatsCardsConfigurationRepository
) : ViewModel() {

    private val _visibleCards = MutableStateFlow<List<StatsCardType>>(StatsCardType.defaultCards())
    val visibleCards: StateFlow<List<StatsCardType>> = _visibleCards.asStateFlow()

    private val _hiddenCards = MutableStateFlow<List<StatsCardType>>(emptyList())
    val hiddenCards: StateFlow<List<StatsCardType>> = _hiddenCards.asStateFlow()

    private val siteId: Long
        get() = selectedSiteRepository.getSelectedSite()?.siteId ?: 0L

    init {
        loadConfiguration()
        observeConfigurationChanges()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            val config = cardConfigurationRepository.getConfiguration(siteId)
            _visibleCards.value = config.visibleCards
            _hiddenCards.value = config.hiddenCards()
        }
    }

    private fun observeConfigurationChanges() {
        viewModelScope.launch {
            cardConfigurationRepository.configurationFlow.collect { pair ->
                if (pair != null && pair.first == siteId) {
                    _visibleCards.value = pair.second.visibleCards
                    _hiddenCards.value = pair.second.hiddenCards()
                }
            }
        }
    }

    fun removeCard(cardType: StatsCardType) {
        viewModelScope.launch {
            cardConfigurationRepository.removeCard(siteId, cardType)
        }
    }

    fun addCard(cardType: StatsCardType) {
        viewModelScope.launch {
            cardConfigurationRepository.addCard(siteId, cardType)
        }
    }
}
