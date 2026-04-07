package org.wordpress.android.ui.newstats.utm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.UtmItemData
import org.wordpress.android.ui.newstats.repository.UtmResult
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class UtmViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider
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

    val uiState: StateFlow<UtmCardUiState> = combine(
        _selectedCategory,
        *_categoryStates.values.toTypedArray()
    ) { values ->
        val cat = values[0] as UtmCategory
        @Suppress("UNCHECKED_CAST")
        val states = values.drop(1) as List<UtmCardUiState>
        states[cat.ordinal]
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UtmCardUiState.Loading
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> =
        _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriods =
        mutableMapOf<UtmCategory, StatsPeriod?>()

    private var allItemsCache =
        mutableMapOf<UtmCategory, List<UtmUiItem>>()
    private var totalViewsCache =
        mutableMapOf<UtmCategory, Long>()
    private var totalViewsChangeCache =
        mutableMapOf<UtmCategory, Long>()
    private var totalViewsChangePercentCache =
        mutableMapOf<UtmCategory, Double>()

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
        loadingPeriod = currentPeriod
        setCurrentCategoryState(UtmCardUiState.Loading)
        viewModelScope.launch {
            fetchForCurrentCategory(site.siteId)
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
        selectedSiteRepository.getSelectedSite()?.adminUrl

    fun onPeriodChanged(period: StatsPeriod) {
        if (currentPeriod == period &&
            loadingPeriod == period
        ) return
        val cat = _selectedCategory.value
        if (loadedPeriods[cat] == period) return
        currentPeriod = period
        // Reset all category loaded periods
        loadedPeriods.clear()
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
            setCurrentCategoryState(
                UtmCardUiState.Loading
            )
            viewModelScope.launch {
                fetchForCategory(category, siteId)
            }
        }
    }

    fun getDetailData(): UtmDetailData {
        val cat = _selectedCategory.value
        return UtmDetailData(
            items = allItemsCache[cat].orEmpty(),
            totalViews = totalViewsCache[cat] ?: 0L,
            totalViewsChange =
                totalViewsChangeCache[cat] ?: 0L,
            totalViewsChangePercent =
                totalViewsChangePercentCache[cat] ?: 0.0,
            dateRange = currentPeriod
                .toDateRangeString(resourceProvider),
            selectedCategory = cat
        )
    }

    private fun setCurrentCategoryState(
        state: UtmCardUiState
    ) {
        val cat = _selectedCategory.value
        _categoryStates[cat]?.value = state
    }

    private fun resetLoadedPeriodForCurrentCategory() {
        loadedPeriods[_selectedCategory.value] = null
    }

    private suspend fun fetchForCurrentCategory(
        siteId: Long
    ) {
        fetchForCategory(_selectedCategory.value, siteId)
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
                    loadedPeriods[category] = currentPeriod
                    loadingPeriod = null
                    val items = result.items
                        .map { it.toUiItem() }
                    allItemsCache[category] = items
                    totalViewsCache[category] =
                        result.totalViews
                    totalViewsChangeCache[category] =
                        result.totalViewsChange
                    totalViewsChangePercentCache[category] =
                        result.totalViewsChangePercent
                    val cardItems =
                        items.take(CARD_MAX_ITEMS)
                    val maxViews = cardItems
                        .firstOrNull()?.views ?: 0L
                    _categoryStates[category]?.value =
                        UtmCardUiState.Loaded(
                            items = cardItems,
                            maxViewsForBar = maxViews,
                            hasMoreItems =
                                items.size > CARD_MAX_ITEMS
                        )
                }
                is UtmResult.Error -> {
                    loadingPeriod = null
                    _categoryStates[category]?.value =
                        UtmCardUiState.Error(
                            result.messageResId,
                            result.isAuthError
                        )
                }
            }
        } catch (e: Exception) {
            loadingPeriod = null
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
        val change = when {
            viewsChange > 0 -> StatsViewChange.Positive(
                viewsChange, abs(viewsChangePercent)
            )
            viewsChange < 0 -> StatsViewChange.Negative(
                abs(viewsChange),
                abs(viewsChangePercent)
            )
            else -> StatsViewChange.NoChange
        }
        return UtmUiItem(
            title = name,
            views = views,
            change = change,
            topPosts = topPosts.map {
                UtmPostUiItem(it.title, it.views)
            }
        )
    }
}
