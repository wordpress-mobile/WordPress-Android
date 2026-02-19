package org.wordpress.android.ui.newstats.countries

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
import org.wordpress.android.ui.newstats.repository.CountryViewItemData
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.repository.CountryViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10
private const val MAX_COUNTRY_CODE_LENGTH = 2

@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow<CountriesCardUiState>(CountriesCardUiState.Loading)
    val uiState: StateFlow<CountriesCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null

    private var allCountries: List<CountryItem> = emptyList()
    private var cachedMapData: String = ""
    private var cachedMinViews: Long = 0L
    private var cachedMaxViews: Long = 0L
    private var cachedTotalViews: Long = 0L
    private var cachedTotalViewsChange: Long = 0L
    private var cachedTotalViewsChangePercent: Double = 0.0

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            _uiState.value = CountriesCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_todays_stats_no_site_selected
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            _uiState.value = CountriesCardUiState.Error(
                resourceProvider.getString(
                    R.string.stats_todays_stats_failed_to_load
                )
            )
            return
        }

        statsRepository.init(accessToken)
        _uiState.value = CountriesCardUiState.Loading

        viewModelScope.launch {
            try {
                fetchCountryViews(site)
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
            _isRefreshing.value = true
            fetchCountryViews(site)
            _isRefreshing.value = false
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

    fun getDetailData(): CountriesDetailData {
        return CountriesDetailData(
            countries = allCountries,
            mapData = cachedMapData,
            minViews = cachedMinViews,
            maxViews = cachedMaxViews,
            totalViews = cachedTotalViews,
            totalViewsChange = cachedTotalViewsChange,
            totalViewsChangePercent = cachedTotalViewsChangePercent,
            dateRange = currentPeriod.toDateRangeString(resourceProvider)
        )
    }

    private suspend fun fetchCountryViews(site: SiteModel) {
        val siteId = site.siteId

        when (val result = statsRepository.fetchCountryViews(siteId, currentPeriod)) {
            is CountryViewsResult.Success -> {
                loadedPeriod = currentPeriod
                cachedTotalViews = result.totalViews
                cachedTotalViewsChange = result.totalViewsChange
                cachedTotalViewsChangePercent = result.totalViewsChangePercent

                if (result.countries.isEmpty()) {
                    allCountries = emptyList()
                    cachedMapData = ""
                    cachedMinViews = 0L
                    cachedMaxViews = 0L
                    _uiState.value = CountriesCardUiState.Loaded(
                        countries = emptyList(),
                        mapData = "",
                        minViews = 0,
                        maxViews = 0,
                        maxViewsForBar = 0,
                        hasMoreItems = false
                    )
                } else {
                    val countries = result.countries.map { country ->
                        CountryItem(
                            countryCode = country.countryCode,
                            countryName = country.countryName,
                            views = country.views,
                            flagIconUrl = country.flagIconUrl,
                            change = country.toCountryViewChange()
                        )
                    }

                    // Build map data for Google GeoChart
                    val mapData = buildMapData(countries)
                    val minViews = countries.minOfOrNull { it.views } ?: 0L
                    val maxViews = countries.maxOfOrNull { it.views } ?: 0L

                    // Store all data for detail screen
                    allCountries = countries
                    cachedMapData = mapData
                    cachedMinViews = if (minViews == maxViews) 0L else minViews
                    cachedMaxViews = maxViews

                    // For bar percentage, use first item's views (list is sorted by views descending)
                    val cardCountries = countries.take(CARD_MAX_ITEMS)
                    val maxViewsForBar = cardCountries.firstOrNull()?.views ?: 0L

                    _uiState.value = CountriesCardUiState.Loaded(
                        countries = cardCountries,
                        mapData = mapData,
                        minViews = cachedMinViews,
                        maxViews = cachedMaxViews,
                        maxViewsForBar = maxViewsForBar,
                        hasMoreItems = countries.size > CARD_MAX_ITEMS
                    )
                }
            }
            is CountryViewsResult.Error -> {
                _uiState.value = CountriesCardUiState.Error(result.message)
            }
        }
    }

    private fun CountryViewItemData.toCountryViewChange(): CountryViewChange {
        return when {
            viewsChange > 0 -> CountryViewChange.Positive(viewsChange, abs(viewsChangePercent))
            viewsChange < 0 -> CountryViewChange.Negative(abs(viewsChange), abs(viewsChangePercent))
            else -> CountryViewChange.NoChange
        }
    }

    /**
     * Builds the map data string for Google GeoChart.
     * Format: ['countryCode',views],['countryCode',views],...
     */
    private fun buildMapData(countries: List<CountryItem>): String {
        return countries.joinToString(",") { country ->
            val safeCode = country.countryCode
                .filter { it.isLetter() }
                .take(MAX_COUNTRY_CODE_LENGTH)
            "['$safeCode',${country.views}]"
        }
    }
}

data class CountriesDetailData(
    val countries: List<CountryItem>,
    val mapData: String,
    val minViews: Long,
    val maxViews: Long,
    val totalViews: Long,
    val totalViewsChange: Long,
    val totalViewsChangePercent: Double,
    val dateRange: String
)
