package org.wordpress.android.ui.newstats.countries

/**
 * UI State for the Countries stats card.
 */
sealed class CountriesCardUiState {
    data object Loading : CountriesCardUiState()

    data class Loaded(
        val countries: List<CountryItem>,
        val mapData: String,
        val minViews: Long,
        val maxViews: Long,
        val hasMoreItems: Boolean
    ) : CountriesCardUiState()

    data class Error(val message: String) : CountriesCardUiState()
}

/**
 * A single country item in the countries list.
 *
 * @param countryCode ISO 3166-1 alpha-2 country code (e.g., "US", "GB")
 * @param countryName Full country name
 * @param views Number of views from this country
 * @param flagIconUrl URL to the country flag icon
 */
data class CountryItem(
    val countryCode: String,
    val countryName: String,
    val views: Long,
    val flagIconUrl: String?
)
