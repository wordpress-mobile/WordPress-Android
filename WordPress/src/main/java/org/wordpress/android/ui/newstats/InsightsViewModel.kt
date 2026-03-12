package org.wordpress.android.ui.newstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.InsightsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryUseCase
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val cardConfigurationRepository:
        InsightsCardsConfigurationRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val statsSummaryUseCase: StatsSummaryUseCase,
    private val statsInsightsUseCase: StatsInsightsUseCase
) : ViewModel() {
    private val _visibleCards =
        MutableStateFlow<List<InsightsCardType>>(
            InsightsCardType.defaultCards()
        )
    val visibleCards: StateFlow<List<InsightsCardType>> =
        _visibleCards.asStateFlow()

    private val _hiddenCards =
        MutableStateFlow<List<InsightsCardType>>(emptyList())
    val hiddenCards: StateFlow<List<InsightsCardType>> =
        _hiddenCards.asStateFlow()

    private val _isNetworkAvailable = MutableStateFlow(true)
    val isNetworkAvailable: StateFlow<Boolean> =
        _isNetworkAvailable.asStateFlow()

    private val _cardsToLoad =
        MutableStateFlow<List<InsightsCardType>>(emptyList())
    val cardsToLoad: StateFlow<List<InsightsCardType>> =
        _cardsToLoad.asStateFlow()

    // Data fetching coordination
    private val _summaryResult =
        MutableSharedFlow<StatsSummaryResult>(replay = 1)
    val summaryResult: SharedFlow<StatsSummaryResult> =
        _summaryResult.asSharedFlow()

    private val _insightsResult =
        MutableSharedFlow<InsightsResult>(replay = 1)
    val insightsResult: SharedFlow<InsightsResult> =
        _insightsResult.asSharedFlow()

    private val _isDataRefreshing = MutableStateFlow(false)
    val isDataRefreshing: StateFlow<Boolean> =
        _isDataRefreshing.asStateFlow()

    private var isDataLoaded = false
    private var isDataLoading = false

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

    // region Data fetching

    fun loadDataIfNeeded() {
        if (isDataLoaded || isDataLoading) return
        isDataLoading = true
        fetchData()
    }

    @Suppress("TooGenericExceptionCaught")
    fun fetchData(forceRefresh: Boolean = false) {
        val siteId = resolvedSiteId() ?: run {
            isDataLoading = false
            _isDataRefreshing.value = false
            return
        }
        viewModelScope.launch {
            try {
                coroutineScope {
                    launch {
                        try {
                            val result =
                                statsSummaryUseCase(
                                    siteId, forceRefresh
                                )
                            _summaryResult.emit(result)
                        } catch (e: Exception) {
                            AppLog.e(
                                AppLog.T.STATS,
                                "Error fetching stats" +
                                    " summary:" +
                                    " ${e.message}",
                                e
                            )
                            _summaryResult.emit(
                                StatsSummaryResult.Error(
                                    e.message
                                        ?: "Unknown error"
                                )
                            )
                        }
                    }
                    launch {
                        try {
                            val result =
                                statsInsightsUseCase(
                                    siteId, forceRefresh
                                )
                            _insightsResult.emit(result)
                        } catch (e: Exception) {
                            AppLog.e(
                                AppLog.T.STATS,
                                "Error fetching" +
                                    " insights:" +
                                    " ${e.message}",
                                e
                            )
                            _insightsResult.emit(
                                InsightsResult.Error(
                                    e.message
                                        ?: "Unknown error"
                                )
                            )
                        }
                    }
                }
                isDataLoaded = true
            } finally {
                isDataLoading = false
                _isDataRefreshing.value = false
            }
        }
    }

    fun refreshData() {
        isDataLoaded = false
        isDataLoading = true
        _isDataRefreshing.value = true
        fetchData(forceRefresh = true)
    }

    // endregion

    // region Card configuration

    private fun loadConfiguration() {
        val currentSiteId = selectedSiteRepository
            .getSelectedSite()?.siteId ?: run {
            AppLog.w(
                AppLog.T.STATS,
                "No site selected, skipping config load"
            )
            return
        }
        viewModelScope.launch {
            val config = cardConfigurationRepository
                .getConfiguration(currentSiteId)
            updateFromConfiguration(config)
        }
    }

    private fun observeConfigurationChanges() {
        viewModelScope.launch {
            cardConfigurationRepository.configurationFlow
                .collect { pair ->
                    val currentSiteId =
                        resolvedSiteId() ?: return@collect
                    if (pair != null &&
                        pair.first == currentSiteId
                    ) {
                        updateFromConfiguration(pair.second)
                    }
                }
        }
    }

    private fun updateFromConfiguration(
        config: InsightsCardsConfiguration
    ) {
        _visibleCards.value = config.visibleCards
        _hiddenCards.value = config.computeHiddenCards()
        _cardsToLoad.value = config.visibleCards
    }

    fun removeCard(cardType: InsightsCardType) {
        val currentSiteId = resolvedSiteId() ?: return
        viewModelScope.launch {
            cardConfigurationRepository
                .removeCard(currentSiteId, cardType)
        }
    }

    fun addCard(cardType: InsightsCardType) {
        val currentSiteId = resolvedSiteId() ?: return
        viewModelScope.launch {
            cardConfigurationRepository
                .addCard(currentSiteId, cardType)
        }
    }

    fun moveCardUp(cardType: InsightsCardType) {
        val currentSiteId = resolvedSiteId() ?: return
        viewModelScope.launch {
            cardConfigurationRepository
                .moveCardUp(currentSiteId, cardType)
        }
    }

    fun moveCardToTop(cardType: InsightsCardType) {
        val currentSiteId = resolvedSiteId() ?: return
        viewModelScope.launch {
            cardConfigurationRepository
                .moveCardToTop(currentSiteId, cardType)
        }
    }

    fun moveCardDown(cardType: InsightsCardType) {
        val currentSiteId = resolvedSiteId() ?: return
        viewModelScope.launch {
            cardConfigurationRepository
                .moveCardDown(currentSiteId, cardType)
        }
    }

    fun moveCardToBottom(cardType: InsightsCardType) {
        val currentSiteId = resolvedSiteId() ?: return
        viewModelScope.launch {
            cardConfigurationRepository
                .moveCardToBottom(currentSiteId, cardType)
        }
    }

    // endregion

    private fun resolvedSiteId(): Long? {
        return selectedSiteRepository
            .getSelectedSite()?.siteId ?: run {
            AppLog.w(
                AppLog.T.STATS,
                "No site selected for card operation"
            )
            null
        }
    }
}
