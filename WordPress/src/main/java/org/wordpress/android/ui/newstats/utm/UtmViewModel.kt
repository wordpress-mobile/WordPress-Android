package org.wordpress.android.ui.newstats.utm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.UtmItemData
import org.wordpress.android.ui.newstats.repository.UtmResult
import org.wordpress.android.ui.newstats.util.statsUpgradeUrl
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class UtmViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val appPrefsWrapper: AppPrefsWrapper
) : ViewModel() {
    private val _selectedCategory =
        MutableStateFlow(UtmCategory.SOURCE_MEDIUM)
    val selectedCategory: StateFlow<UtmCategory> =
        _selectedCategory.asStateFlow()

    private val _categoryStates = UtmCategory.entries
        .associateWith {
            MutableStateFlow<UtmCardUiState>(
                UtmCardUiState.Loading
            )
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UtmCardUiState> =
        _selectedCategory.flatMapLatest { cat ->
            _categoryStates[cat]
                ?: MutableStateFlow(
                    UtmCardUiState.Loading
                )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            UtmCardUiState.Loading
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod =
        StatsPeriod.Last7Days
    private val loadingPeriods =
        ConcurrentHashMap<UtmCategory, StatsPeriod>()
    private val loadedPeriods =
        ConcurrentHashMap<UtmCategory, StatsPeriod>()
    private val fetchJobs =
        ConcurrentHashMap<UtmCategory, Job>()

    init {
        loadSavedCategory()
    }

    private fun loadSavedCategory() {
        val siteId = selectedSiteRepository
            .getSelectedSite()?.siteId ?: return
        val saved = appPrefsWrapper
            .getStatsUtmCategory(siteId)
        if (saved != null) {
            try {
                _selectedCategory.value =
                    UtmCategory.valueOf(saved)
            } catch (_: IllegalArgumentException) {
                // ignore invalid saved value
            }
        }
    }

    fun loadData() {
        val site = selectedSiteRepository
            .getSelectedSite()
        if (site == null) {
            setCurrentCategoryState(
                UtmCardUiState.Error(
                    R.string.stats_error_no_site
                )
            )
            return
        }
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            setCurrentCategoryState(
                UtmCardUiState.Error(
                    R.string.stats_error_api
                )
            )
            return
        }
        val cat = _selectedCategory.value
        val period = currentPeriod
        loadingPeriods[cat] = period
        // A period already in memory repaints in the same frame, so the placeholder would only
        // flash a skeleton over data the card is about to render anyway.
        if (!isCached(cat, site.siteId, period)) {
            setCurrentCategoryState(UtmCardUiState.Loading)
        }
        launchFetch(cat) {
            fetchForCurrentCategory(site.siteId)
            revalidateIfNeeded(cat, site.siteId, period)
        }
    }

    /**
     * Replaces any in-flight fetch for [category] with [block].
     *
     * The job is registered before it is started, and only the job that is still registered may
     * remove itself: cancelling a coroutine suspended in a request unwinds it on another thread, so
     * its `finally` runs after this method has already stored the replacement under the same key.
     * Removing blindly there would leave that replacement untracked, so the next period change
     * could no longer cancel it — leaving it free to write the period the user just left over the
     * one they are looking at.
     */
    private fun launchFetch(
        category: UtmCategory,
        block: suspend () -> Unit
    ) {
        fetchJobs[category]?.cancel()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
            } finally {
                if (fetchJobs.remove(category, coroutineContext.job)) {
                    loadingPeriods.remove(category)
                }
            }
        }
        fetchJobs[category] = job
        job.start()
    }

    private fun isCached(
        category: UtmCategory,
        siteId: Long,
        period: StatsPeriod
    ) = statsRepository.isUtmCached(siteId, period, category.keys)

    /**
     * Refreshes a result served from a previous visit to the stats screen, once. The card already
     * shows those numbers, so this runs without a loading state and keeps them if it fails.
     */
    private suspend fun revalidateIfNeeded(
        category: UtmCategory,
        siteId: Long,
        period: StatsPeriod
    ) {
        if (period != currentPeriod) return
        if (!statsRepository.utmNeedsRevalidation(siteId, period, category.keys)) return
        fetchForCategory(category, siteId, forceRefresh = true, applyErrors = false)
    }

    fun refresh() {
        val site = selectedSiteRepository
            .getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                resetLoadedPeriodForCurrentCategory()
                fetchForCurrentCategory(site.siteId, forceRefresh = true)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    fun getAdminUrl(): String? =
        selectedSiteRepository.getSelectedSite()
            ?.adminUrl

    fun getUpgradeUrl(): String? =
        selectedSiteRepository.getSelectedSite()
            ?.statsUpgradeUrl()

    fun getCurrentPeriod(): StatsPeriod = currentPeriod

    fun onPeriodChanged(period: StatsPeriod) {
        val cat = _selectedCategory.value
        if (currentPeriod == period &&
            loadingPeriods[cat] == period
        ) return
        if (loadedPeriods[cat] == period) return
        currentPeriod = period
        cancelAllFetchJobs()
        loadedPeriods.clear()
        loadingPeriods.clear()
        loadData()
    }

    @Suppress("ReturnCount")
    fun onCategoryChanged(category: UtmCategory) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        val siteId = selectedSiteRepository
            .getSelectedSite()?.siteId ?: return
        appPrefsWrapper.setStatsUtmCategory(
            siteId, category.name
        )
        if (loadedPeriods[category] != currentPeriod) {
            val accessToken = accountStore.accessToken
            if (accessToken.isNullOrEmpty()) return
            val period = currentPeriod
            loadingPeriods[category] = period
            if (!isCached(category, siteId, period)) {
                setCurrentCategoryState(
                    UtmCardUiState.Loading
                )
            }
            launchFetch(category) {
                fetchForCategory(category, siteId)
                revalidateIfNeeded(category, siteId, period)
            }
        }
    }

    private fun cancelAllFetchJobs() {
        fetchJobs.values.forEach { it.cancel() }
        fetchJobs.clear()
    }

    private fun setCurrentCategoryState(
        state: UtmCardUiState
    ) {
        val cat = _selectedCategory.value
        _categoryStates[cat]?.value = state
    }

    private fun resetLoadedPeriodForCurrentCategory() {
        loadedPeriods.remove(_selectedCategory.value)
    }

    private suspend fun fetchForCurrentCategory(
        siteId: Long,
        forceRefresh: Boolean = false
    ) {
        fetchForCategory(
            _selectedCategory.value, siteId, forceRefresh
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchForCategory(
        category: UtmCategory,
        siteId: Long,
        forceRefresh: Boolean = false,
        applyErrors: Boolean = true
    ) {
        try {
            val result = statsRepository.fetchUtm(
                siteId, category.keys, currentPeriod, forceRefresh
            )
            when (result) {
                is UtmResult.Success -> {
                    loadedPeriods[category] =
                        currentPeriod
                    loadingPeriods.remove(category)
                    val items = result.items
                        .map { it.toUiItem() }
                    val cardItems =
                        items.take(CARD_MAX_ITEMS)
                    val maxViews = cardItems
                        .firstOrNull()?.views ?: 0L
                    _categoryStates[category]?.value =
                        UtmCardUiState.Loaded(
                            items = cardItems,
                            maxViewsForBar = maxViews,
                            hasMoreItems =
                                items.size >
                                    CARD_MAX_ITEMS
                        )
                }
                is UtmResult.Error -> {
                    loadingPeriods.remove(category)
                    if (!applyErrors) return
                    _categoryStates[category]?.value =
                        UtmCardUiState.Error(
                            result.messageResId,
                            result.isAuthError,
                            result.isPlanGated
                        )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            loadingPeriods.remove(category)
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching UTM data", e
            )
            if (!applyErrors) return
            _categoryStates[category]?.value =
                UtmCardUiState.Error(
                    R.string.stats_error_unknown
                )
        }
    }

    private fun UtmItemData.toUiItem(): UtmUiItem {
        return UtmUiItem(
            title = formatUtmName(name),
            views = views,
            topPosts = topPosts.map {
                UtmPostUiItem(it.title, it.views)
            }
        )
    }
}
