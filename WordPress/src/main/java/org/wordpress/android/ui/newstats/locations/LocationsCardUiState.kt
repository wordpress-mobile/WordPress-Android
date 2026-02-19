package org.wordpress.android.ui.newstats.locations

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.StatsViewChange

/**
 * UI State for the Countries stats card.
 */
sealed class LocationsCardUiState {
    data object Loading : LocationsCardUiState()

    data class Loaded(
        val countries: List<CountryItem>,
        val mapData: String,
        val minViews: Long,
        val maxViews: Long,
        val maxViewsForBar: Long,
        val hasMoreItems: Boolean
    ) : LocationsCardUiState()

    data class Error(val message: String) : LocationsCardUiState()
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

/**
 * Converts [CountryViewChange] to [StatsViewChange] for use with shared components.
 */
fun CountryViewChange.toStatsViewChange(): StatsViewChange = when (this) {
    is CountryViewChange.Positive -> StatsViewChange.Positive(value, percentage)
    is CountryViewChange.Negative -> StatsViewChange.Negative(value, percentage)
    is CountryViewChange.NoChange -> StatsViewChange.NoChange
}

/**
 * Represents the type of location data being displayed.
 */
enum class LocationType(@StringRes val labelResId: Int) {
    COUNTRIES(R.string.stats_countries_title),
    REGIONS(R.string.stats_regions_title),
    CITIES(R.string.stats_cities_title)
}

/**
 * A generalized location item used across all location types.
 */
data class LocationItem(
    val id: String,
    val name: String,
    val views: Long,
    val flagIconUrl: String?,
    val change: CountryViewChange = CountryViewChange.NoChange,
    val latitude: String? = null,
    val longitude: String? = null
)
