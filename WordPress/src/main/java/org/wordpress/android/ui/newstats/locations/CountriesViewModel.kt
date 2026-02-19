package org.wordpress.android.ui.newstats.locations

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
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.repository.CityViewItemData
import org.wordpress.android.ui.newstats.repository.CityViewsResult
import org.wordpress.android.ui.newstats.repository.CountryViewItemData
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.repository.CountryViewsResult
import org.wordpress.android.ui.newstats.repository.RegionViewItemData
import org.wordpress.android.ui.newstats.repository.RegionViewsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.ui.newstats.util.toDateRangeString
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.math.abs

private const val CARD_MAX_ITEMS = 10

@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val selectedSiteRepository: SelectedSiteRepository,
    private val accountStore: AccountStore,
    private val statsRepository: StatsRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _countriesUiState =
        MutableStateFlow<CountriesCardUiState>(CountriesCardUiState.Loading)
    private val _regionsUiState =
        MutableStateFlow<CountriesCardUiState>(CountriesCardUiState.Loading)
    private val _citiesUiState =
        MutableStateFlow<CountriesCardUiState>(CountriesCardUiState.Loading)

    private val _selectedLocationType =
        MutableStateFlow(LocationType.COUNTRIES)
    val selectedLocationType: StateFlow<LocationType> =
        _selectedLocationType.asStateFlow()

    val uiState: StateFlow<CountriesCardUiState> = combine(
        _selectedLocationType,
        _countriesUiState,
        _regionsUiState,
        _citiesUiState
    ) { type, countries, regions, cities ->
        when (type) {
            LocationType.COUNTRIES -> countries
            LocationType.REGIONS -> regions
            LocationType.CITIES -> cities
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        CountriesCardUiState.Loading
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentPeriod: StatsPeriod = StatsPeriod.Last7Days
    private var loadingPeriod: StatsPeriod? = null
    private var loadedPeriod: StatsPeriod? = null

    // Per-type loaded period tracking for lazy fetching
    private var countriesLoadedPeriod: StatsPeriod? = null
    private var regionsLoadedPeriod: StatsPeriod? = null
    private var citiesLoadedPeriod: StatsPeriod? = null

    // Countries cache
    private var allCountries: List<CountryItem> = emptyList()
    private var cachedCountriesMapData: String = ""
    private var cachedCountriesMinViews: Long = 0L
    private var cachedCountriesMaxViews: Long = 0L
    private var cachedCountriesTotalViews: Long = 0L
    private var cachedCountriesTotalViewsChange: Long = 0L
    private var cachedCountriesTotalViewsChangePercent: Double = 0.0

    // Regions cache
    private var allRegions: List<LocationItem> = emptyList()
    private var cachedRegionsMapData: String = ""
    private var cachedRegionsMinViews: Long = 0L
    private var cachedRegionsMaxViews: Long = 0L
    private var cachedRegionsTotalViews: Long = 0L
    private var cachedRegionsTotalViewsChange: Long = 0L
    private var cachedRegionsTotalViewsChangePercent: Double = 0.0

    // Cities cache
    private var allCities: List<LocationItem> = emptyList()
    private var cachedCitiesMapData: String = ""
    private var cachedCitiesMinViews: Long = 0L
    private var cachedCitiesMaxViews: Long = 0L
    private var cachedCitiesTotalViews: Long = 0L
    private var cachedCitiesTotalViewsChange: Long = 0L
    private var cachedCitiesTotalViewsChangePercent: Double = 0.0

    fun loadData() {
        val site = selectedSiteRepository.getSelectedSite()
        if (site == null) {
            loadingPeriod = null
            setAllStatesError(
                resourceProvider.getString(
                    R.string.stats_todays_stats_no_site_selected
                )
            )
            return
        }

        val accessToken = accountStore.accessToken
        if (accessToken.isNullOrEmpty()) {
            loadingPeriod = null
            setAllStatesError(
                resourceProvider.getString(
                    R.string.stats_todays_stats_failed_to_load
                )
            )
            return
        }

        statsRepository.init(accessToken)
        setCurrentTypeLoading()

        viewModelScope.launch {
            try {
                fetchForCurrentType(site)
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
            // Reset loaded period for current type to force re-fetch
            resetLoadedPeriodForCurrentType()
            fetchForCurrentType(site)
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
        // Reset all per-type loaded periods on period change
        countriesLoadedPeriod = null
        regionsLoadedPeriod = null
        citiesLoadedPeriod = null
        loadData()
    }

    @Suppress("ReturnCount")
    fun onLocationTypeChanged(type: LocationType) {
        if (_selectedLocationType.value == type) return
        _selectedLocationType.value = type

        // Lazy-fetch: if this type hasn't been loaded for the current period
        if (!isTypeLoadedForCurrentPeriod(type)) {
            val site = selectedSiteRepository.getSelectedSite() ?: return
            val accessToken = accountStore.accessToken
            if (accessToken.isNullOrEmpty()) return
            statsRepository.init(accessToken)

            setTypeLoading(type)
            viewModelScope.launch {
                fetchForType(type, site)
            }
        }
    }

    fun getDetailData(): CountriesDetailData {
        return when (_selectedLocationType.value) {
            LocationType.COUNTRIES -> CountriesDetailData(
                countries = allCountries,
                locationItems = null,
                mapData = cachedCountriesMapData,
                minViews = cachedCountriesMinViews,
                maxViews = cachedCountriesMaxViews,
                totalViews = cachedCountriesTotalViews,
                totalViewsChange = cachedCountriesTotalViewsChange,
                totalViewsChangePercent =
                    cachedCountriesTotalViewsChangePercent,
                dateRange = currentPeriod.toDateRangeString(
                    resourceProvider
                ),
                locationType = LocationType.COUNTRIES
            )
            LocationType.REGIONS -> CountriesDetailData(
                countries = emptyList(),
                locationItems = allRegions,
                mapData = cachedRegionsMapData,
                minViews = cachedRegionsMinViews,
                maxViews = cachedRegionsMaxViews,
                totalViews = cachedRegionsTotalViews,
                totalViewsChange = cachedRegionsTotalViewsChange,
                totalViewsChangePercent =
                    cachedRegionsTotalViewsChangePercent,
                dateRange = currentPeriod.toDateRangeString(
                    resourceProvider
                ),
                locationType = LocationType.REGIONS
            )
            LocationType.CITIES -> CountriesDetailData(
                countries = emptyList(),
                locationItems = allCities,
                mapData = cachedCitiesMapData,
                minViews = cachedCitiesMinViews,
                maxViews = cachedCitiesMaxViews,
                totalViews = cachedCitiesTotalViews,
                totalViewsChange = cachedCitiesTotalViewsChange,
                totalViewsChangePercent =
                    cachedCitiesTotalViewsChangePercent,
                dateRange = currentPeriod.toDateRangeString(
                    resourceProvider
                ),
                locationType = LocationType.CITIES
            )
        }
    }

    private fun setAllStatesError(message: String) {
        val error = CountriesCardUiState.Error(message)
        _countriesUiState.value = error
        _regionsUiState.value = error
        _citiesUiState.value = error
    }

    private fun setCurrentTypeLoading() {
        setTypeLoading(_selectedLocationType.value)
    }

    private fun setTypeLoading(type: LocationType) {
        when (type) {
            LocationType.COUNTRIES ->
                _countriesUiState.value = CountriesCardUiState.Loading
            LocationType.REGIONS ->
                _regionsUiState.value = CountriesCardUiState.Loading
            LocationType.CITIES ->
                _citiesUiState.value = CountriesCardUiState.Loading
        }
    }

    private fun resetLoadedPeriodForCurrentType() {
        when (_selectedLocationType.value) {
            LocationType.COUNTRIES -> countriesLoadedPeriod = null
            LocationType.REGIONS -> regionsLoadedPeriod = null
            LocationType.CITIES -> citiesLoadedPeriod = null
        }
    }

    private fun isTypeLoadedForCurrentPeriod(
        type: LocationType
    ): Boolean {
        return when (type) {
            LocationType.COUNTRIES ->
                countriesLoadedPeriod == currentPeriod
            LocationType.REGIONS ->
                regionsLoadedPeriod == currentPeriod
            LocationType.CITIES ->
                citiesLoadedPeriod == currentPeriod
        }
    }

    private suspend fun fetchForCurrentType(site: SiteModel) {
        fetchForType(_selectedLocationType.value, site)
    }

    private suspend fun fetchForType(
        type: LocationType,
        site: SiteModel
    ) {
        when (type) {
            LocationType.COUNTRIES -> fetchCountryViews(site)
            LocationType.REGIONS -> fetchRegionViews(site)
            LocationType.CITIES -> fetchCityViews(site)
        }
    }

    private suspend fun fetchCountryViews(site: SiteModel) {
        val siteId = site.siteId

        when (val result = statsRepository.fetchCountryViews(
            siteId, currentPeriod
        )) {
            is CountryViewsResult.Success -> {
                handleCountryViewsSuccess(result)
            }
            is CountryViewsResult.Error -> {
                _countriesUiState.value =
                    CountriesCardUiState.Error(result.message)
            }
        }
    }

    private fun handleCountryViewsSuccess(
        result: CountryViewsResult.Success
    ) {
        loadedPeriod = currentPeriod
        countriesLoadedPeriod = currentPeriod
        cachedCountriesTotalViews = result.totalViews
        cachedCountriesTotalViewsChange = result.totalViewsChange
        cachedCountriesTotalViewsChangePercent =
            result.totalViewsChangePercent

        if (result.countries.isEmpty()) {
            allCountries = emptyList()
            cachedCountriesMapData = ""
            cachedCountriesMinViews = 0L
            cachedCountriesMaxViews = 0L
            _countriesUiState.value = emptyLoadedState()
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

            val mapData = buildCountriesMapData(countries)
            val minViews = countries.minOfOrNull { it.views } ?: 0L
            val maxViews = countries.maxOfOrNull { it.views } ?: 0L

            allCountries = countries
            cachedCountriesMapData = mapData
            cachedCountriesMinViews =
                if (minViews == maxViews) 0L else minViews
            cachedCountriesMaxViews = maxViews

            val cardCountries = countries.take(CARD_MAX_ITEMS)
            val maxViewsForBar =
                cardCountries.firstOrNull()?.views ?: 0L

            _countriesUiState.value = CountriesCardUiState.Loaded(
                countries = cardCountries,
                mapData = mapData,
                minViews = cachedCountriesMinViews,
                maxViews = cachedCountriesMaxViews,
                maxViewsForBar = maxViewsForBar,
                hasMoreItems = countries.size > CARD_MAX_ITEMS
            )
        }
    }

    private suspend fun fetchRegionViews(site: SiteModel) {
        val siteId = site.siteId

        when (val result = statsRepository.fetchRegionViews(
            siteId, currentPeriod
        )) {
            is RegionViewsResult.Success -> {
                handleRegionViewsSuccess(result)
            }
            is RegionViewsResult.Error -> {
                _regionsUiState.value =
                    CountriesCardUiState.Error(result.message)
            }
        }
    }

    @Suppress("CyclomaticComplexity")
    private fun handleRegionViewsSuccess(
        result: RegionViewsResult.Success
    ) {
        loadedPeriod = currentPeriod
        regionsLoadedPeriod = currentPeriod
        cachedRegionsTotalViews = result.totalViews
        cachedRegionsTotalViewsChange = result.totalViewsChange
        cachedRegionsTotalViewsChangePercent =
            result.totalViewsChangePercent

        val regions = result.regions.map { it.toLocationItem() }

        if (regions.isEmpty()) {
            allRegions = emptyList()
            cachedRegionsMapData = ""
            cachedRegionsMinViews = 0L
            cachedRegionsMaxViews = 0L
            _regionsUiState.value = emptyLoadedState()
        } else {
            val mapData = buildRegionsMapData(result.regions)
            val aggregatedViews = result.regions
                .groupBy { it.countryCode }
                .mapValues { (_, items) ->
                    items.sumOf { it.views }
                }.values
            val minViews = aggregatedViews.minOrNull() ?: 0L
            val maxViews = aggregatedViews.maxOrNull() ?: 0L

            allRegions = regions
            cachedRegionsMapData = mapData
            cachedRegionsMinViews =
                if (minViews == maxViews) 0L else minViews
            cachedRegionsMaxViews = maxViews

            val cardRegions = regions.take(CARD_MAX_ITEMS)
            val maxViewsForBar =
                cardRegions.firstOrNull()?.views ?: 0L

            _regionsUiState.value = CountriesCardUiState.Loaded(
                countries = cardRegions.map {
                    it.toCountryItem()
                },
                mapData = mapData,
                minViews = cachedRegionsMinViews,
                maxViews = cachedRegionsMaxViews,
                maxViewsForBar = maxViewsForBar,
                hasMoreItems = regions.size > CARD_MAX_ITEMS
            )
        }
    }

    private suspend fun fetchCityViews(site: SiteModel) {
        val siteId = site.siteId

        when (val result = statsRepository.fetchCityViews(
            siteId, currentPeriod
        )) {
            is CityViewsResult.Success -> {
                handleCityViewsSuccess(result)
            }
            is CityViewsResult.Error -> {
                _citiesUiState.value =
                    CountriesCardUiState.Error(result.message)
            }
        }
    }

    @Suppress("CyclomaticComplexity")
    private fun handleCityViewsSuccess(
        result: CityViewsResult.Success
    ) {
        loadedPeriod = currentPeriod
        citiesLoadedPeriod = currentPeriod
        cachedCitiesTotalViews = result.totalViews
        cachedCitiesTotalViewsChange = result.totalViewsChange
        cachedCitiesTotalViewsChangePercent =
            result.totalViewsChangePercent

        val cities = result.cities.map { it.toLocationItem() }

        if (cities.isEmpty()) {
            allCities = emptyList()
            cachedCitiesMapData = ""
            cachedCitiesMinViews = 0L
            cachedCitiesMaxViews = 0L
            _citiesUiState.value = emptyLoadedState()
        } else {
            val mapData = buildCitiesMapData(cities)
            val minViews = cities.minOfOrNull { it.views } ?: 0L
            val maxViews = cities.maxOfOrNull { it.views } ?: 0L

            allCities = cities
            cachedCitiesMapData = mapData
            cachedCitiesMinViews =
                if (minViews == maxViews) 0L else minViews
            cachedCitiesMaxViews = maxViews

            val cardCities = cities.take(CARD_MAX_ITEMS)
            val maxViewsForBar =
                cardCities.firstOrNull()?.views ?: 0L

            _citiesUiState.value = CountriesCardUiState.Loaded(
                countries = cardCities.map {
                    it.toCountryItem()
                },
                mapData = mapData,
                minViews = cachedCitiesMinViews,
                maxViews = cachedCitiesMaxViews,
                maxViewsForBar = maxViewsForBar,
                hasMoreItems = cities.size > CARD_MAX_ITEMS
            )
        }
    }

    private fun CountryViewItemData.toCountryViewChange(): CountryViewChange {
        return when {
            viewsChange > 0 -> CountryViewChange.Positive(
                viewsChange, abs(viewsChangePercent)
            )
            viewsChange < 0 -> CountryViewChange.Negative(
                abs(viewsChange), abs(viewsChangePercent)
            )
            else -> CountryViewChange.NoChange
        }
    }

    private fun toViewChange(
        change: Long,
        percent: Double
    ): CountryViewChange {
        return when {
            change > 0 -> CountryViewChange.Positive(
                change, abs(percent)
            )
            change < 0 -> CountryViewChange.Negative(
                abs(change), abs(percent)
            )
            else -> CountryViewChange.NoChange
        }
    }

    private fun RegionViewItemData.toLocationItem(): LocationItem {
        return LocationItem(
            id = location,
            name = location,
            views = views,
            flagIconUrl = flagIconUrl,
            change = toViewChange(viewsChange, viewsChangePercent)
        )
    }

    private fun CityViewItemData.toLocationItem(): LocationItem {
        return LocationItem(
            id = location,
            name = location,
            views = views,
            flagIconUrl = flagIconUrl,
            change = toViewChange(viewsChange, viewsChangePercent),
            latitude = latitude,
            longitude = longitude
        )
    }

    private fun LocationItem.toCountryItem(): CountryItem {
        return CountryItem(
            countryCode = id,
            countryName = name,
            views = views,
            flagIconUrl = flagIconUrl,
            change = change
        )
    }

    private fun buildCountriesMapData(
        countries: List<CountryItem>
    ): String {
        return countries.joinToString(",") { country ->
            "['${country.countryCode}',${country.views}]"
        }
    }

    private fun buildRegionsMapData(
        regions: List<RegionViewItemData>
    ): String {
        return regions.groupBy { it.countryCode }
            .mapValues { (_, items) -> items.sumOf { it.views } }
            .entries.joinToString(",") { (code, views) ->
                "['$code',$views]"
            }
    }

    private fun buildCitiesMapData(
        cities: List<LocationItem>
    ): String {
        return cities.filter {
            it.latitude != null && it.longitude != null
        }.joinToString(",") { city ->
            "[${city.latitude},${city.longitude}," +
                "'${escapeJs(city.name)}',${city.views}]"
        }
    }

    private fun escapeJs(value: String): String {
        return value.replace("'", "\\'")
    }

    private fun emptyLoadedState() = CountriesCardUiState.Loaded(
        countries = emptyList(),
        mapData = "",
        minViews = 0,
        maxViews = 0,
        maxViewsForBar = 0,
        hasMoreItems = false
    )

    companion object
}

data class CountriesDetailData(
    val countries: List<CountryItem>,
    val locationItems: List<LocationItem>?,
    val mapData: String,
    val minViews: Long,
    val maxViews: Long,
    val totalViews: Long,
    val totalViewsChange: Long,
    val totalViewsChangePercent: Double,
    val dateRange: String,
    val locationType: LocationType = LocationType.COUNTRIES
)
