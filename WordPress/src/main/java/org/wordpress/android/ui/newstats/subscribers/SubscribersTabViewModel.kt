package org.wordpress.android.ui.newstats.subscribers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.SubscribersCardsConfigurationRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@HiltViewModel
class SubscribersTabViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardConfigurationRepository:
        SubscribersCardsConfigurationRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : ViewModel() {
    private val _visibleCards = MutableStateFlow<
        List<SubscribersCardType>>(
        SubscribersCardType.defaultCards()
    )
    val visibleCards: StateFlow<List<SubscribersCardType>> =
        _visibleCards.asStateFlow()

    private val _hiddenCards = MutableStateFlow<
        List<SubscribersCardType>>(emptyList())
    val hiddenCards: StateFlow<List<SubscribersCardType>> =
        _hiddenCards.asStateFlow()

    private val _isNetworkAvailable =
        MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> =
        _isNetworkAvailable.asStateFlow()

    private val _cardsToLoad = MutableStateFlow<
        List<SubscribersCardType>>(emptyList())
    val cardsToLoad: StateFlow<List<SubscribersCardType>> =
        _cardsToLoad.asStateFlow()

    private var isInitialLoad = true

    private val siteId: Long
        get() = selectedSiteRepository
            .getSelectedSite()?.siteId ?: 0L

    init {
        checkNetworkStatus()
        loadConfiguration()
        observeConfigurationChanges()
    }

    fun checkNetworkStatus(): Boolean {
        val isAvailable =
            networkUtilsWrapper.isNetworkAvailable()
        _isNetworkAvailable.value = isAvailable
        return isAvailable
    }

    private fun loadConfiguration() {
        val currentSiteId = siteId
        viewModelScope.launch {
            val config = cardConfigurationRepository
                .getConfiguration(currentSiteId)
            updateFromConfiguration(config)
        }
    }

    private fun observeConfigurationChanges() {
        viewModelScope.launch {
            cardConfigurationRepository
                .configurationFlow
                .collect { pair ->
                    val currentSiteId = siteId
                    if (pair != null &&
                        pair.first == currentSiteId
                    ) {
                        updateFromConfiguration(
                            pair.second
                        )
                    }
                }
        }
    }

    private fun updateFromConfiguration(
        config: SubscribersCardsConfiguration
    ) {
        _visibleCards.value = config.visibleCards
        _hiddenCards.value = config.hiddenCards()
        if (isInitialLoad) {
            _cardsToLoad.value = config.visibleCards
            isInitialLoad = false
        }
    }

    fun removeCard(cardType: SubscribersCardType) {
        val currentSiteId = siteId
        viewModelScope.launch {
            cardConfigurationRepository.removeCard(
                currentSiteId, cardType
            )
        }
    }

    fun addCard(cardType: SubscribersCardType) {
        val currentSiteId = siteId
        viewModelScope.launch {
            cardConfigurationRepository.addCard(
                currentSiteId, cardType
            )
        }
    }

    fun moveCardUp(cardType: SubscribersCardType) {
        val currentSiteId = siteId
        viewModelScope.launch {
            cardConfigurationRepository.moveCardUp(
                currentSiteId, cardType
            )
        }
    }

    fun moveCardToTop(cardType: SubscribersCardType) {
        val currentSiteId = siteId
        viewModelScope.launch {
            cardConfigurationRepository.moveCardToTop(
                currentSiteId, cardType
            )
        }
    }

    fun moveCardDown(cardType: SubscribersCardType) {
        val currentSiteId = siteId
        viewModelScope.launch {
            cardConfigurationRepository.moveCardDown(
                currentSiteId, cardType
            )
        }
    }

    fun moveCardToBottom(
        cardType: SubscribersCardType
    ) {
        val currentSiteId = siteId
        viewModelScope.launch {
            cardConfigurationRepository
                .moveCardToBottom(
                    currentSiteId, cardType
                )
        }
    }
}
