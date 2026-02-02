package org.wordpress.android.ui.newstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource
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

    // Data sources for visible Most Viewed cards (in order)
    private val _mostViewedDataSources = MutableStateFlow<List<MostViewedDataSource>>(
        listOf(MostViewedDataSource.POSTS_AND_PAGES, MostViewedDataSource.REFERRERS)
    )
    val mostViewedDataSources: StateFlow<List<MostViewedDataSource>> = _mostViewedDataSources.asStateFlow()

    // Hidden Most Viewed data sources (available to add)
    private val _hiddenMostViewedDataSources = MutableStateFlow<List<MostViewedDataSource>>(emptyList())
    val hiddenMostViewedDataSources: StateFlow<List<MostViewedDataSource>> =
        _hiddenMostViewedDataSources.asStateFlow()

    private val siteId: Long
        get() = selectedSiteRepository.getSelectedSite()?.siteId ?: 0L

    init {
        loadConfiguration()
        observeConfigurationChanges()
    }

    private fun loadConfiguration() {
        viewModelScope.launch {
            val config = cardConfigurationRepository.getConfiguration(siteId)
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
        _mostViewedDataSources.value = config.mostViewedDataSources.mapNotNull { name ->
            runCatching { MostViewedDataSource.valueOf(name) }.getOrNull()
        }
        _hiddenMostViewedDataSources.value = config.hiddenMostViewedDataSources()
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

    /**
     * Removes a Most Viewed card at the given index.
     */
    fun removeMostViewedCard(index: Int) {
        viewModelScope.launch {
            cardConfigurationRepository.removeMostViewedCard(siteId, index)
        }
    }

    /**
     * Adds a Most Viewed card with the given data source.
     */
    fun addMostViewedCard(dataSource: MostViewedDataSource) {
        viewModelScope.launch {
            cardConfigurationRepository.addMostViewedCard(siteId, dataSource.name)
        }
    }

    /**
     * Updates the data source for a Most Viewed card at the given index.
     */
    fun updateMostViewedDataSource(index: Int, dataSource: MostViewedDataSource) {
        // Update locally immediately for responsiveness
        val currentSources = _mostViewedDataSources.value.toMutableList()
        if (index in currentSources.indices) {
            currentSources[index] = dataSource
            _mostViewedDataSources.value = currentSources
            // Update hidden data sources
            _hiddenMostViewedDataSources.value = MostViewedDataSource.entries.filter {
                it !in currentSources
            }
        }
        // Persist
        viewModelScope.launch {
            cardConfigurationRepository.updateMostViewedDataSource(siteId, index, dataSource.name)
        }
    }
}
