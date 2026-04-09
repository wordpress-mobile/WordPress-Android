package org.wordpress.android.ui.newstats.utm

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.repository.UtmResult
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.util.AppLog
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class UtmDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val selectedSiteRepository:
        SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState =
        MutableStateFlow<UtmDetailUiState>(
            UtmDetailUiState.Loading
        )
    val uiState: StateFlow<UtmDetailUiState> =
        _uiState.asStateFlow()

    private var hasLoaded = false

    @Suppress("ReturnCount")
    fun loadData() {
        if (hasLoaded) return
        hasLoaded = true

        val site = selectedSiteRepository
            .getSelectedSite()
        if (site == null) {
            _uiState.value = UtmDetailUiState.Error(
                R.string.stats_error_no_site
            )
            return
        }
        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            _uiState.value = UtmDetailUiState.Error(
                R.string.stats_error_api
            )
            return
        }

        val category = resolveCategory()
        val period = resolvePeriod()

        statsRepository.init(accessToken)
        _uiState.value = UtmDetailUiState.Loading

        viewModelScope.launch {
            fetchData(site.siteId, category, period)
        }
    }

    fun retry() {
        hasLoaded = false
        loadData()
    }

    fun getAdminUrl(): String? =
        selectedSiteRepository.getSelectedSite()
            ?.adminUrl

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchData(
        siteId: Long,
        category: UtmCategory,
        period: StatsPeriod
    ) {
        try {
            val result = statsRepository.fetchUtm(
                siteId, category.keys, period
            )
            when (result) {
                is UtmResult.Success -> {
                    _uiState.value = buildLoadedState(
                        result, category, period
                    )
                }
                is UtmResult.Error -> {
                    _uiState.value =
                        UtmDetailUiState.Error(
                            result.messageResId,
                            result.isAuthError
                        )
                }
            }
        } catch (e: Exception) {
            AppLog.e(
                AppLog.T.STATS,
                "Error fetching UTM detail data", e
            )
            _uiState.value = UtmDetailUiState.Error(
                R.string.stats_error_unknown
            )
        }
    }

    private fun buildLoadedState(
        result: UtmResult.Success,
        category: UtmCategory,
        period: StatsPeriod
    ): UtmDetailUiState.Loaded {
        val items = result.items.map {
            UtmUiItem(
                title = formatUtmName(it.name),
                views = it.views,
                topPosts = it.topPosts.map { post ->
                    UtmPostUiItem(
                        post.title, post.views
                    )
                }
            )
        }
        val maxViews =
            items.firstOrNull()?.views ?: 0L
        return UtmDetailUiState.Loaded(
            items = items,
            maxViewsForBar = maxViews,
            totalViews = result.totalViews,
            dateRange = period.toDateRangeString(
                resourceProvider
            ),
            categoryLabelResId =
                category.labelResId
        )
    }

    private fun resolveCategory(): UtmCategory {
        val name = savedStateHandle
            .get<String>(EXTRA_CATEGORY_NAME)
        return if (name != null) {
            try {
                UtmCategory.valueOf(name)
            } catch (_: IllegalArgumentException) {
                UtmCategory.SOURCE_MEDIUM
            }
        } else {
            UtmCategory.SOURCE_MEDIUM
        }
    }

    private fun resolvePeriod(): StatsPeriod {
        val type = savedStateHandle
            .get<String>(EXTRA_PERIOD_TYPE)
            ?: return StatsPeriod.Last7Days
        val customStart = savedStateHandle
            .get<Long>(EXTRA_CUSTOM_START_DATE)
        val customEnd = savedStateHandle
            .get<Long>(EXTRA_CUSTOM_END_DATE)
        return StatsPeriod.fromTypeString(
            type, customStart, customEnd
        )
    }

    companion object {
        const val EXTRA_CATEGORY_NAME =
            "extra_category_name"
        const val EXTRA_PERIOD_TYPE =
            "extra_period_type"
        const val EXTRA_CUSTOM_START_DATE =
            "extra_custom_start_date"
        const val EXTRA_CUSTOM_END_DATE =
            "extra_custom_end_date"
    }
}
