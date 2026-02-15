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
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

/**
 * ViewModel for managing stats cards configuration.
 * Handles visible/hidden cards state and card add/remove operations.
 */
@HiltViewModel
class NewStatsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardConfigurationRepository: StatsCardsConfigurationRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ViewModel() {
    private val _visibleCards = MutableStateFlow<List<StatsCardType>>(StatsCardType.defaultCards())
    val visibleCards: StateFlow<List<StatsCardType>> = _visibleCards.asStateFlow()

    private val _hiddenCards = MutableStateFlow<List<StatsCardType>>(emptyList())
    val hiddenCards: StateFlow<List<StatsCardType>> = _hiddenCards.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()

    private val siteId: Long
        get() = selectedSiteRepository.getSelectedSite()?.siteId ?: 0L

    init {
        checkNetworkStatus()
        loadConfiguration()
        observeConfigurationChanges()
    }

    fun checkNetworkStatus(): Boolean {
        val isAvailable = networkUtilsWrapper.isNetworkAvailable()
        _isNetworkAvailable.value = isAvailable
        return isAvailable
    }

    private fun loadConfiguration() {
        val currentSiteId = siteId // Capture siteId to avoid race conditions during site switching
        viewModelScope.launch {
            // Repository handles errors internally and returns default config on failure
            val config = cardConfigurationRepository.getConfiguration(currentSiteId)
            updateFromConfiguration(config)
        }
    }

    private fun observeConfigurationChanges() {
        viewModelScope.launch {
            cardConfigurationRepository.configurationFlow.collect { pair ->
                if (pair != null && pair.first == siteId) {
                    updateFromConfiguration(pair.second)
                }
            }
        }
    }

    private fun updateFromConfiguration(config: StatsCardsConfiguration) {
        _visibleCards.value = config.visibleCards
        _hiddenCards.value = config.hiddenCards()
    }

    fun removeCard(cardType: StatsCardType) {
        val currentSiteId = siteId // Capture siteId to avoid race conditions during site switching
        viewModelScope.launch {
            cardConfigurationRepository.removeCard(currentSiteId, cardType)
        }
    }

    fun addCard(cardType: StatsCardType) {
        val currentSiteId = siteId // Capture siteId to avoid race conditions during site switching
        viewModelScope.launch {
            cardConfigurationRepository.addCard(currentSiteId, cardType)
        }
    }

    fun moveCardUp(cardType: StatsCardType) {
        val currentSiteId = siteId // Capture siteId to avoid race conditions during site switching
        viewModelScope.launch {
            cardConfigurationRepository.moveCardUp(currentSiteId, cardType)
        }
    }

    fun moveCardToTop(cardType: StatsCardType) {
        val currentSiteId = siteId // Capture siteId to avoid race conditions during site switching
        viewModelScope.launch {
            cardConfigurationRepository.moveCardToTop(currentSiteId, cardType)
        }
    }

    fun moveCardDown(cardType: StatsCardType) {
        val currentSiteId = siteId // Capture siteId to avoid race conditions during site switching
        viewModelScope.launch {
            cardConfigurationRepository.moveCardDown(currentSiteId, cardType)
        }
    }

    fun moveCardToBottom(cardType: StatsCardType) {
        val currentSiteId = siteId // Capture siteId to avoid race conditions during site switching
        viewModelScope.launch {
            cardConfigurationRepository.moveCardToBottom(currentSiteId, cardType)
        }
    }
}
