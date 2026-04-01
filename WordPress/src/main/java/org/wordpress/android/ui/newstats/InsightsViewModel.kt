package org.wordpress.android.ui.newstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.InsightsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryUseCase
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val cardConfigurationRepository:
        InsightsCardsConfigurationRepository,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val statsSummaryUseCase: StatsSummaryUseCase,
    private val statsInsightsUseCase: StatsInsightsUseCase,
    private val statsTagsUseCase: StatsTagsUseCase,
    private val resourceProvider: ResourceProvider
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

    private val isDataLoaded = AtomicBoolean(false)
    private val isDataLoading = AtomicBoolean(false)
    private val summaryFetched = AtomicBoolean(false)
    private val insightsFetched = AtomicBoolean(false)
    // Main-thread-confined: only accessed from
    // viewModelScope (Dispatchers.Main).
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            statsSummaryUseCase.clearCache()
            statsInsightsUseCase.clearCache()
            statsTagsUseCase.clearCache()
        }
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
        if (isDataLoaded.get() ||
            !isDataLoading.compareAndSet(false, true)
        ) return
        fetchData()
    }

    fun fetchData(forceRefresh: Boolean = false) {
        val siteId = resolvedSiteId() ?: run {
            isDataLoading.set(false)
            _isDataRefreshing.value = false
            return
        }
        val cards = _cardsToLoad.value
        val shouldFetchSummary = cards.needsSummary()
        val shouldFetchInsights = cards.needsInsights()
        if (!shouldFetchSummary && !shouldFetchInsights) {
            isDataLoading.set(false)
            _isDataRefreshing.value = false
            return
        }
        val job = viewModelScope.launch {
            try {
                coroutineScope {
                    if (shouldFetchSummary) {
                        launch {
                            fetchSummary(siteId, forceRefresh)
                        }
                    }
                    if (shouldFetchInsights) {
                        launch {
                            fetchInsights(
                                siteId, forceRefresh
                            )
                        }
                    }
                }
                isDataLoaded.set(true)
            } finally {
                if (fetchJob === coroutineContext[Job]) {
                    isDataLoading.set(false)
                    _isDataRefreshing.value = false
                }
            }
        }
        fetchJob = job
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "InstanceOfCheckForException"
    )
    private suspend fun fetchSummary(
        siteId: Long,
        forceRefresh: Boolean
    ) {
        try {
            val result = statsSummaryUseCase(
                siteId, forceRefresh
            )
            if (result is StatsSummaryResult.Success) {
                summaryFetched.set(true)
            }
            _summaryResult.emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching stats summary:" +
                    " ${e.message}",
                e
            )
            _summaryResult.emit(
                StatsSummaryResult.Error(
                    resourceProvider.getString(
                        R.string.stats_error_unknown
                    )
                )
            )
        }
    }

    @Suppress(
        "TooGenericExceptionCaught",
        "InstanceOfCheckForException"
    )
    private suspend fun fetchInsights(
        siteId: Long,
        forceRefresh: Boolean
    ) {
        try {
            val result = statsInsightsUseCase(
                siteId, forceRefresh
            )
            if (result is InsightsResult.Success) {
                insightsFetched.set(true)
            }
            _insightsResult.emit(result)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching insights:" +
                    " ${e.message}",
                e
            )
            _insightsResult.emit(
                InsightsResult.Error(
                    resourceProvider.getString(
                        R.string.stats_error_unknown
                    )
                )
            )
        }
    }

    fun refreshData() {
        val oldJob = fetchJob
        fetchJob = null
        oldJob?.cancel()
        isDataLoaded.set(false)
        summaryFetched.set(false)
        insightsFetched.set(false)
        isDataLoading.set(true)
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
        _hiddenCards.value = config.hiddenCards
        val cards = config.visibleCards
        val needsNewFetch =
            (cards.needsSummary() &&
                !summaryFetched.get()) ||
                (cards.needsInsights() &&
                    !insightsFetched.get())
        _cardsToLoad.value = config.visibleCards
        if (needsNewFetch) {
            fetchJob?.cancel()
            isDataLoaded.set(false)
            isDataLoading.set(true)
            fetchData()
        }
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

    companion object {
        // TAGS_AND_CATEGORIES is intentionally absent
        // from both checks: it has its own dedicated
        // fetch path via StatsTagsUseCase in
        // TagsAndCategoriesViewModel.
        private fun List<InsightsCardType>.needsSummary():
            Boolean = any {
            it == InsightsCardType.ALL_TIME_STATS ||
                it == InsightsCardType.MOST_POPULAR_DAY
        }

        private fun List<InsightsCardType>.needsInsights():
            Boolean = any {
            it == InsightsCardType.YEAR_IN_REVIEW ||
                it == InsightsCardType.MOST_POPULAR_TIME
        }
    }
}
