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
import org.wordpress.android.ui.newstats.repository.CountryViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import javax.inject.Inject

@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CountriesCardUiState>(CountriesCardUiState.Loading)
    val uiState: StateFlow<CountriesCardUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days

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

    private fun initializeRepository() {
        accountStore.accessToken?.let { token ->
            statsRepository.init(token)
        }
    }

    private suspend fun fetchCountryViews(site: SiteModel) {
        val siteId = site.siteId

        when (val result = statsRepository.fetchCountryViews(siteId, currentPeriod)) {
            is CountryViewsResult.Success -> {
                if (result.countries.isEmpty()) {
                    _uiState.value = CountriesCardUiState.Loaded(
                        countries = emptyList(),
                        mapData = "",
                        minViews = 0,
                        maxViews = 0
                    )
                } else {
                    val countries = result.countries.map { country ->
                        CountryItem(
                            countryCode = country.countryCode,
                            countryName = country.countryName,
                            views = country.views,
                            flagIconUrl = country.flagIconUrl
                        )
                    }

                    // Build map data for Google GeoChart
                    val mapData = buildMapData(countries)
                    val minViews = countries.minOfOrNull { it.views } ?: 0L
                    val maxViews = countries.maxOfOrNull { it.views } ?: 0L

                    _uiState.value = CountriesCardUiState.Loaded(
                        countries = countries,
                        mapData = mapData,
                        minViews = if (minViews == maxViews) 0L else minViews,
                        maxViews = maxViews
                    )
                }
            }
            is CountryViewsResult.Error -> {
                _uiState.value = CountriesCardUiState.Error(result.message)
            }
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
