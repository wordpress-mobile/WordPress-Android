package org.wordpress.android.ui.newstats.subscribers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.analytics.NewStatsTracker
import org.wordpress.android.ui.newstats.analytics.cardMoveIndices
import java.util.concurrent.atomic.AtomicBoolean
import org.wordpress.android.ui.newstats.repository.SubscribersCardsConfigurationRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@HiltViewModel
class SubscribersTabViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val cardConfigurationRepository:
        SubscribersCardsConfigurationRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val newStatsTracker: NewStatsTracker
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

    private val isInitialLoad = AtomicBoolean(true)

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
        if (isInitialLoad.compareAndSet(true, false)) {
            _cardsToLoad.value = config.visibleCards
        }
    }

    fun removeCard(cardType: SubscribersCardType) {
        newStatsTracker.trackCardRemoved(cardType)
        cardAction { removeCard(it, cardType) }
    }

    fun addCard(cardType: SubscribersCardType) {
        newStatsTracker.trackCardAdded(cardType)
        cardAction { addCard(it, cardType) }
    }

    fun moveCardUp(cardType: SubscribersCardType) {
        trackCardMoved(
            cardType,
            NewStatsTracker.CardMoveDirection.UP
        )
        cardAction { moveCardUp(it, cardType) }
    }

    fun moveCardToTop(cardType: SubscribersCardType) {
        trackCardMoved(
            cardType,
            NewStatsTracker.CardMoveDirection.TOP
        )
        cardAction { moveCardToTop(it, cardType) }
    }

    fun moveCardDown(cardType: SubscribersCardType) {
        trackCardMoved(
            cardType,
            NewStatsTracker.CardMoveDirection.DOWN
        )
        cardAction { moveCardDown(it, cardType) }
    }

    fun moveCardToBottom(
        cardType: SubscribersCardType
    ) {
        trackCardMoved(
            cardType,
            NewStatsTracker.CardMoveDirection.BOTTOM
        )
        cardAction { moveCardToBottom(it, cardType) }
    }

    private fun trackCardMoved(
        cardType: SubscribersCardType,
        direction: NewStatsTracker.CardMoveDirection
    ) {
        _visibleCards.value
            .cardMoveIndices(cardType, direction)
            ?.let { (from, to) ->
                newStatsTracker.trackCardMoved(
                    cardType, direction, from, to
                )
            }
    }

    private fun cardAction(
        action: suspend SubscribersCardsConfigurationRepository.(Long) -> Unit
    ) {
        val currentSiteId = siteId
        viewModelScope.launch {
            cardConfigurationRepository.action(
                currentSiteId
            )
        }
    }
}
