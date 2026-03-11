package org.wordpress.android.ui.newstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.InsightsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
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
    private val statsRepository: StatsRepository,
    private val accountStore: AccountStore
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

    private val summaryMutex = Mutex()
    private var cachedSummary:
        Pair<Long, StatsSummaryData>? = null

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

    suspend fun getStatsSummary(
        siteId: Long,
        forceRefresh: Boolean = false
    ): StatsSummaryResult {
        val token = accountStore.accessToken
        if (token.isNullOrEmpty()) {
            return StatsSummaryResult.Error(
                "No access token"
            )
        }
        statsRepository.init(token)
        return summaryMutex.withLock {
            val cached = cachedSummary
            if (!forceRefresh &&
                cached != null &&
                cached.first == siteId
            ) {
                return@withLock StatsSummaryResult.Success(
                    cached.second
                )
            }
            val result =
                statsRepository.fetchStatsSummary(siteId)
            if (result is StatsSummaryResult.Success) {
                cachedSummary = siteId to result.data
            }
            result
        }
    }

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
                    val currentSiteId = siteId
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
