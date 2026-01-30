package org.wordpress.android.ui.newstats.countries

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
        val maxViewsForBar: Long,
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
 * @param change The change compared to the previous period
 */
data class CountryItem(
    val countryCode: String,
    val countryName: String,
    val views: Long,
    val flagIconUrl: String?,
    val change: CountryViewChange = CountryViewChange.NoChange
)

/**
 * Represents the change in views for a country compared to the previous period.
 */
sealed class CountryViewChange : Parcelable {
    @Parcelize
    data class Positive(val value: Long, val percentage: Double) : CountryViewChange()
    @Parcelize
    data class Negative(val value: Long, val percentage: Double) : CountryViewChange()
    @Parcelize
    data object NoChange : CountryViewChange()
}
