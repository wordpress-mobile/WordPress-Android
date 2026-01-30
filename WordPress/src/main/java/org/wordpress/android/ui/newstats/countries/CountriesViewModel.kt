package org.wordpress.android.ui.newstats.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.CountryViewItemData
import org.wordpress.android.ui.newstats.repository.CountryViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10
private const val MONTH_ABBREVIATION_LENGTH = 3

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

    private var allCountries: List<CountryItem> = emptyList()
    private var cachedMapData: String = ""
    private var cachedMinViews: Long = 0L
    private var cachedMaxViews: Long = 0L
    private var cachedTotalViews: Long = 0L
    private var cachedTotalViewsChange: Long = 0L
    private var cachedTotalViewsChangePercent: Double = 0.0

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = CountriesCardUiState.Loading

            val site = selectedSiteRepository.getSelectedSite()
            if (site == null) {
                _uiState.value = CountriesCardUiState.Error("No site selected")
                return@launch
            }

            initializeRepository()
            fetchCountryViews(site)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true

            val site = selectedSiteRepository.getSelectedSite()
            if (site != null) {
                initializeRepository()
                fetchCountryViews(site)
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

    private fun initializeRepository() {
        accountStore.accessToken?.let { token ->
            statsRepository.init(token)
        }
    }

    private suspend fun fetchCountryViews(site: SiteModel) {
        val siteId = site.siteId

        when (val result = statsRepository.fetchCountryViews(siteId, currentPeriod)) {
            is CountryViewsResult.Success -> {
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

                    _uiState.value = CountriesCardUiState.Loaded(
                        countries = countries.take(CARD_MAX_ITEMS),
                        mapData = mapData,
                        minViews = cachedMinViews,
                        maxViews = cachedMaxViews,
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
            "['${country.countryCode}',${country.views}]"
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

private fun StatsPeriod.toDateRangeString(resourceProvider: ResourceProvider): String {
    return when (this) {
        is StatsPeriod.Today -> resourceProvider.getString(R.string.stats_period_today)
        is StatsPeriod.Last7Days -> resourceProvider.getString(R.string.stats_period_last_7_days)
        is StatsPeriod.Last30Days -> resourceProvider.getString(R.string.stats_period_last_30_days)
        is StatsPeriod.Last6Months -> resourceProvider.getString(R.string.stats_period_last_6_months)
        is StatsPeriod.Last12Months -> resourceProvider.getString(R.string.stats_period_last_12_months)
        is StatsPeriod.Custom -> "${startDate.dayOfMonth}-${endDate.dayOfMonth} ${
            endDate.month.name.take(MONTH_ABBREVIATION_LENGTH).lowercase().replaceFirstChar { it.uppercase() }
        }"
    }
}
