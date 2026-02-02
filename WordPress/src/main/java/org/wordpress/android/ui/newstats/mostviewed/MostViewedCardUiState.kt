package org.wordpress.android.ui.newstats.mostviewed

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.R

/**
 * Represents the available data source types for the Most Viewed card.
 * Extensible for future additional data sources.
 */
enum class MostViewedDataSource(val labelResId: Int) {
    POSTS_AND_PAGES(R.string.stats_most_viewed_posts_and_pages),
    REFERRERS(R.string.stats_most_viewed_referrers)
}

/**
 * UI State for the Most Viewed stats card.
 */
sealed class MostViewedCardUiState {
    data object Loading : MostViewedCardUiState()

    data class Loaded(
        val selectedDataSource: MostViewedDataSource,
        val items: List<MostViewedItem>,
        val maxViewsForBar: Long
    ) : MostViewedCardUiState()

    data class Error(val message: String) : MostViewedCardUiState()
}

/**
 * A single item in the Most Viewed list.
 *
 * @param id Unique identifier for the item
 * @param title The title/name of the item (post title or referrer name)
 * @param views The number of views
 * @param change The percentage change compared to previous period
 * @param isHighlighted Whether this item should be highlighted (typically the first item)
 */
data class MostViewedItem(
    val id: Long,
    val title: String,
    val views: Long,
    val change: MostViewedChange,
    val isHighlighted: Boolean = false
)

/**
 * Represents the change in views compared to the previous period.
 */
sealed class MostViewedChange : Parcelable {
    @Parcelize
    data class Positive(val value: Long, val percentage: Double) : MostViewedChange()
    @Parcelize
    data class Negative(val value: Long, val percentage: Double) : MostViewedChange()
    @Parcelize
    data object NoChange : MostViewedChange()
    @Parcelize
    data object NotAvailable : MostViewedChange()
}

/**
 * Data class for passing items to the detail screen via Intent.
 * Implements Parcelable for efficient Intent extras.
 */
@Parcelize
data class MostViewedDetailItem(
    val id: Long,
    val title: String,
    val views: Long,
    val change: MostViewedChange
) : Parcelable
