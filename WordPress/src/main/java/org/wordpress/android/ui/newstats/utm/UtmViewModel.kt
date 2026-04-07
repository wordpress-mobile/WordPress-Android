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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.UtmItemData
import org.wordpress.android.ui.newstats.repository.UtmResult
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import java.util.concurrent.ConcurrentHashMap

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
        statsRepository.init(accessToken)
        val cat = _selectedCategory.value
        loadingPeriods[cat] = currentPeriod
        setCurrentCategoryState(UtmCardUiState.Loading)
        fetchJobs[cat]?.cancel()
        fetchJobs[cat] = viewModelScope.launch {
            try {
                fetchForCurrentCategory(site.siteId)
            } finally {
                loadingPeriods.remove(cat)
                fetchJobs.remove(cat)
            }
        }
    }

    fun refresh() {
        val site = selectedSiteRepository
            .getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return
        statsRepository.init(accessToken)
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                resetLoadedPeriodForCurrentCategory()
                fetchForCurrentCategory(site.siteId)
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
            statsRepository.init(accessToken)
            loadingPeriods[category] = currentPeriod
            setCurrentCategoryState(
                UtmCardUiState.Loading
            )
            fetchJobs[category]?.cancel()
            fetchJobs[category] =
                viewModelScope.launch {
                    try {
                        fetchForCategory(
                            category, siteId
                        )
                    } finally {
                        loadingPeriods.remove(category)
                        fetchJobs.remove(category)
                    }
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
        siteId: Long
    ) {
        fetchForCategory(
            _selectedCategory.value, siteId
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchForCategory(
        category: UtmCategory,
        siteId: Long
    ) {
        try {
            val result = statsRepository.fetchUtm(
                siteId, category.keys, currentPeriod
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
                    _categoryStates[category]?.value =
                        UtmCardUiState.Error(
                            result.messageResId,
                            result.isAuthError
                        )
                }
            }
        } catch (e: Exception) {
            loadingPeriods.remove(category)
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching UTM data", e
            )
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
