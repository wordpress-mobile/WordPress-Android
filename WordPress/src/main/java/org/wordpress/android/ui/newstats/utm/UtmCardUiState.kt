package org.wordpress.android.ui.newstats.utm

import android.os.Parcelable
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R

/**
 * Represents the available UTM category combinations
 * for the dropdown selector.
 */
enum class UtmCategory(
    @StringRes val labelResId: Int,
    val keys: List<String>
) {
    SOURCE_MEDIUM(
        R.string.stats_utm_source_medium,
        listOf("utm_source", "utm_medium")
    ),
    CAMPAIGN_SOURCE_MEDIUM(
        R.string.stats_utm_campaign_source_medium,
        listOf("utm_campaign", "utm_source", "utm_medium")
    ),
    SOURCE(
        R.string.stats_utm_source,
        listOf("utm_source")
    ),
    MEDIUM(
        R.string.stats_utm_medium,
        listOf("utm_medium")
    ),
    CAMPAIGN(
        R.string.stats_utm_campaign,
        listOf("utm_campaign")
    )
}

/**
 * UI State for the UTM stats card.
 */
sealed class UtmCardUiState {
    data object Loading : UtmCardUiState()

    data class Loaded(
        val items: List<UtmUiItem>,
        val maxViewsForBar: Long,
        val hasMoreItems: Boolean
    ) : UtmCardUiState()

    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : UtmCardUiState()
}

/**
 * A single UTM value row for display in the card.
 */
data class UtmUiItem(
    val title: String,
    val views: Long,
    val topPosts: List<UtmPostUiItem>
)

/**
 * A single post within a UTM value row.
 */
data class UtmPostUiItem(
    val title: String,
    val views: Long
)

/**
 * Data passed to the UTM detail screen.
 */
data class UtmDetailData(
    val items: List<UtmUiItem>,
    val totalViews: Long,
    val totalViewsChange: Long,
    val totalViewsChangePercent: Double,
    val dateRange: String,
    val selectedCategory: UtmCategory
)

/**
 * Parcelable UTM item for passing to the detail
 * activity via Intent extras.
 */
@Parcelize
data class UtmDetailItem(
    val title: String,
    val views: Long,
    val topPosts: List<UtmDetailPostItem>
) : Parcelable

/**
 * Parcelable UTM post item for the detail activity.
 */
@Parcelize
data class UtmDetailPostItem(
    val title: String,
    val views: Long
) : Parcelable
