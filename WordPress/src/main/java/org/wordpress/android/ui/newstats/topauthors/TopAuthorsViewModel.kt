package org.wordpress.android.ui.newstats.topauthors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.TopAuthorItemData
import org.wordpress.android.ui.newstats.repository.TopAuthorsResult
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class TopAuthorsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<TopAuthorsCardUiState>(TopAuthorsCardUiState.Loading)
    val uiState: StateFlow<TopAuthorsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days

    private var allAuthors: List<TopAuthorUiItem> = emptyList()
    private var cachedTotalViews: Long = 0L
    private var cachedTotalViewsChange: Long = 0L
    private var cachedTotalViewsChangePercent: Double = 0.0

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = TopAuthorsCardUiState.Loading

            val site = selectedSiteRepository.getSelectedSite()
            if (site == null) {
                _uiState.value = TopAuthorsCardUiState.Error("No site selected")
                return@launch
            }

            initializeRepository()
            fetchTopAuthors(site)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true

            val site = selectedSiteRepository.getSelectedSite()
            if (site != null) {
                initializeRepository()
                fetchTopAuthors(site)
            }

            _isRefreshing.value = false
        }
    }

    fun onRetry() {
        loadData()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        if (currentPeriod != period) {
            currentPeriod = period
            loadData()
        }
    }

    fun getDetailData(): TopAuthorsDetailData {
        return TopAuthorsDetailData(
            authors = allAuthors,
            totalViews = cachedTotalViews,
            totalViewsChange = cachedTotalViewsChange,
            totalViewsChangePercent = cachedTotalViewsChangePercent,
            dateRange = currentPeriod.toDateRangeString(resourceProvider)
        )
    }

    private fun initializeRepository() {
        accountStore.accessToken?.let { token ->
            statsRepository.init(token)
        }
    }

    private suspend fun fetchTopAuthors(site: SiteModel) {
        val siteId = site.siteId

        when (val result = statsRepository.fetchTopAuthors(siteId, currentPeriod)) {
            is TopAuthorsResult.Success -> {
                cachedTotalViews = result.totalViews
                cachedTotalViewsChange = result.totalViewsChange
                cachedTotalViewsChangePercent = result.totalViewsChangePercent

                if (result.authors.isEmpty()) {
                    allAuthors = emptyList()
                    _uiState.value = TopAuthorsCardUiState.Loaded(
                        authors = emptyList(),
                        maxViewsForBar = 0,
                        hasMoreItems = false
                    )
                } else {
                    val authors = result.authors.map { author ->
                        TopAuthorUiItem(
                            name = author.name,
                            avatarUrl = author.avatarUrl,
                            views = author.views,
                            change = author.toStatsViewChange()
                        )
                    }

                    // Store all authors for detail screen
                    allAuthors = authors

                    // For bar percentage, use first item's views (list is sorted by views descending)
                    val cardAuthors = authors.take(CARD_MAX_ITEMS)
                    val maxViewsForBar = cardAuthors.firstOrNull()?.views ?: 1L

                    _uiState.value = TopAuthorsCardUiState.Loaded(
                        authors = cardAuthors,
                        maxViewsForBar = maxViewsForBar,
                        hasMoreItems = authors.size > CARD_MAX_ITEMS
                    )
                }
            }
            is TopAuthorsResult.Error -> {
                _uiState.value = TopAuthorsCardUiState.Error(result.message)
            }
        }
    }

    private fun TopAuthorItemData.toStatsViewChange(): StatsViewChange {
        return when {
            viewsChange > 0 -> StatsViewChange.Positive(viewsChange, abs(viewsChangePercent))
            viewsChange < 0 -> StatsViewChange.Negative(abs(viewsChange), abs(viewsChangePercent))
            else -> StatsViewChange.NoChange
        }
    }
}

data class TopAuthorsDetailData(
    val authors: List<TopAuthorUiItem>,
    val totalViews: Long,
    val totalViewsChange: Long,
    val totalViewsChangePercent: Double,
    val dateRange: String
)
