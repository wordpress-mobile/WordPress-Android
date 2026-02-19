package org.wordpress.android.ui.newstats.authors

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
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.repository.TopAuthorsResult
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class AuthorsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthorsCardUiState>(AuthorsCardUiState.Loading)
    val uiState: StateFlow<AuthorsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null

    private var allAuthors: List<AuthorUiItem> = emptyList()
    private var cachedTotalViews: Long = 0L
    private var cachedTotalViewsChange: Long = 0L
    private var cachedTotalViewsChangePercent: Double = 0.0

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = AuthorsCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_todays_stats_no_site_selected
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = AuthorsCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_todays_stats_failed_to_load
                )
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = AuthorsCardUiState.Loading

        viewModelScope.launch {
            try {
                fetchTopAuthors(site)
            } finally {
                loadingPeriod = null
            }
        }
    }

    fun refresh() {
        val site = selectedSiteRepository.getSelectedSite() ?: return
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) return

        statsRepository.init(accessToken)
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                fetchTopAuthors(site)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun onRetry() {
        loadData()
    }

    fun onPeriodChanged(period: StatsPeriod) {
        if (loadedPeriod == period || loadingPeriod == period) return
        loadingPeriod = period
        currentPeriod = period
        loadData()
    }

    fun getDetailData(): AuthorsDetailData {
        return AuthorsDetailData(
            authors = allAuthors,
            totalViews = cachedTotalViews,
            totalViewsChange = cachedTotalViewsChange,
            totalViewsChangePercent = cachedTotalViewsChangePercent,
            dateRange = currentPeriod.toDateRangeString(resourceProvider)
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchTopAuthors(site: SiteModel) {
        val siteId = site.siteId

        try {
            when (val result = statsRepository.fetchTopAuthors(
                siteId, currentPeriod
            )) {
                is TopAuthorsResult.Success -> {
                    loadedPeriod = currentPeriod
                    cachedTotalViews = result.totalViews
                    cachedTotalViewsChange = result.totalViewsChange
                    cachedTotalViewsChangePercent =
                        result.totalViewsChangePercent

                    if (result.authors.isEmpty()) {
                        allAuthors = emptyList()
                        _uiState.value = AuthorsCardUiState.Loaded(
                            authors = emptyList(),
                            maxViewsForBar = 0,
                            hasMoreItems = false
                        )
                    } else {
                        val authors = result.authors.map { author ->
                            AuthorUiItem(
                                name = author.name,
                                avatarUrl = author.avatarUrl,
                                views = author.views,
                                change = author.toStatsViewChange()
                            )
                        }

                        allAuthors = authors

                        val cardAuthors = authors.take(CARD_MAX_ITEMS)
                        val maxViewsForBar =
                            cardAuthors.firstOrNull()?.views ?: 0L

                        _uiState.value = AuthorsCardUiState.Loaded(
                            authors = cardAuthors,
                            maxViewsForBar = maxViewsForBar,
                            hasMoreItems =
                                authors.size > CARD_MAX_ITEMS
                        )
                    }
                }
                is TopAuthorsResult.Error -> {
                    _uiState.value =
                        AuthorsCardUiState.Error(result.message)
                }
            }
        } catch (e: Exception) {
            _uiState.value = AuthorsCardUiState.Error(
                e.message ?: resourceProvider.getString(
                    R.string.stats_todays_stats_unknown_error
                )
            )
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

data class AuthorsDetailData(
    val authors: List<AuthorUiItem>,
    val totalViews: Long,
    val totalViewsChange: Long,
    val totalViewsChangePercent: Double,
    val dateRange: String
)
