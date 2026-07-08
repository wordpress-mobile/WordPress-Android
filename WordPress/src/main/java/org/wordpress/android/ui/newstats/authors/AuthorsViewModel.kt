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
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class AuthorsViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow<AuthorsCardUiState>(AuthorsCardUiState.Loading)
    val uiState: StateFlow<AuthorsCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = AuthorsCardUiState.Error(
                R.string.stats_error_no_site
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = AuthorsCardUiState.Error(
                R.string.stats_error_api
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

    fun getAdminUrl(): String? =
        selectedSiteRepository.getSelectedSite()?.adminUrl

    fun onPeriodChanged(period: StatsPeriod) {
        if (loadedPeriod == period || loadingPeriod == period) return
        loadingPeriod = period
        currentPeriod = period
        loadData()
    }

    /**
     * The period currently selected for the authors card. The authors detail screen self-fetches its
     * own (unbounded) data for this period rather than receiving the full list via the Intent.
     */
    fun getCurrentPeriod(): StatsPeriod = currentPeriod

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchTopAuthors(site: SiteModel) {
        val siteId = site.siteId

        try {
            when (val result = statsRepository.fetchTopAuthors(
                siteId, currentPeriod
            )) {
                is TopAuthorsResult.Success -> {
                    loadedPeriod = currentPeriod

                    if (result.authors.isEmpty()) {
                        _uiState.value = AuthorsCardUiState.Loaded(
                            authors = emptyList(),
                            maxViewsForBar = 0,
                            hasMoreItems = false
                        )
                    } else {
                        val authors = result.authors.map { it.toAuthorUiItem() }

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
                    _uiState.value = AuthorsCardUiState.Error(
                        result.messageResId,
                        result.isAuthError
                    )
                }
            }
        } catch (e: Exception) {
            AppLog.e(AppLog.T.STATS, "Error fetching top authors", e)
            _uiState.value = AuthorsCardUiState.Error(
                R.string.stats_error_unknown
            )
        }
    }
}

/**
 * Maps a repository [TopAuthorItemData] to the parcelable [AuthorUiItem] shown by the card and the
 * detail screen. Shared by [AuthorsViewModel] and [AuthorsDetailViewModel].
 */
internal fun TopAuthorItemData.toAuthorUiItem() = AuthorUiItem(
    name = name,
    avatarUrl = avatarUrl,
    views = views,
    change = when {
        viewsChange > 0 -> StatsViewChange.Positive(viewsChange, abs(viewsChangePercent))
        viewsChange < 0 -> StatsViewChange.Negative(abs(viewsChange), abs(viewsChangePercent))
        else -> StatsViewChange.NoChange
    }
)
