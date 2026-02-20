package org.wordpress.android.ui.newstats.locations

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R
import org.wordpress.android.ui.newstats.components.StatsViewChange
import org.wordpress.android.ui.newstats.components.StatsViewChange.NoChange

/**
 * UI State for the Locations stats card.
 */
sealed class LocationsCardUiState {
    data object Loading : LocationsCardUiState()

    data class Loaded(
        val items: List<LocationItem>,
        val mapData: String,
        val minViews: Long,
        val maxViews: Long,
        val maxViewsForBar: Long,
        val hasMoreItems: Boolean
    ) : LocationsCardUiState()

    data class Error(val message: String) : LocationsCardUiState()
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
@Parcelize
data class LocationItem(
    val id: String,
    val name: String,
    val views: Long,
    val flagIconUrl: String?,
    val change: StatsViewChange = NoChange,
    val latitude: String? = null,
    val longitude: String? = null
) : Parcelable
