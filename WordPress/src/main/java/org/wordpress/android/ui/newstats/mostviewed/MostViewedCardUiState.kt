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
        val items: List<MostViewedItem>,
        val maxViewsForBar: Long
    ) : MostViewedCardUiState()

    data class Error(
        val message: String,
        val isAuthError: Boolean = false,
        val isNotAvailable: Boolean = false
    ) : MostViewedCardUiState()
}

/**
 * A single item in the Most Viewed list.
 *
 * @param id Unique identifier for the item
 * @param title The title/name of the item (post title or referrer name)
 * @param views The number of views
 * @param change The percentage change compared to previous period
 * @param url The item's URL. Posts open it as detail stats; referrers and clicks open it in a
 * Custom Tab. Null when the item has no link of its own.
 * @param iconUrl A favicon the API supplied for the item, shown at the start of the row.
 * Referrers and clicks carry one; null everywhere else, and for an item the API knows no icon for.
 */
data class MostViewedItem(
    val id: Long,
    val title: String,
    val views: Long,
    val change: MostViewedChange,
    val children: List<MostViewedChildItem> = emptyList(),
    val url: String? = null,
    val iconUrl: String? = null
)

/**
 * A child item nested under a Most Viewed item (e.g. a referrer under a referrer group).
 *
 * @param name The name of the child item
 * @param url The URL opened when the child row is tapped (null if not linkable)
 * @param views The number of views
 * @param children Rows nested one level deeper, revealed when this child is expanded. Only the
 * archives card goes this deep (its "Taxonomies" group lists a taxonomy's terms under it); empty
 * for every other source, which leaves the child row a plain link row.
 */
@Parcelize
data class MostViewedChildItem(
    val name: String,
    val url: String?,
    val views: Long,
    val children: List<MostViewedChildItem> = emptyList(),
    /** A favicon the API supplied for the child. Referrers only; null everywhere else. */
    val iconUrl: String? = null
) : Parcelable

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

    companion object {
        fun fromChange(
            change: Long,
            changePercent: Double
        ): MostViewedChange = when {
            change > 0 -> Positive(
                change, kotlin.math.abs(changePercent)
            )
            change < 0 -> Negative(
                kotlin.math.abs(change),
                kotlin.math.abs(changePercent)
            )
            else -> NoChange
        }
    }
}

/**
 * Data class for passing items to the detail screen via Intent.
 * Implements Parcelable for efficient Intent extras.
 *
 * @param postType Posts and pages only: the API's item type -- "post", "page", "homepage" or
 * "attachment". The detail screen draws a glyph for it and uses it to pick the REST endpoint its
 * featured image is looked up from. Null for every other source, which shows no thumbnail.
 */
@Parcelize
data class MostViewedDetailItem(
    val id: Long,
    val title: String,
    val views: Long,
    val change: MostViewedChange,
    val children: List<MostViewedChildItem> = emptyList(),
    val url: String? = null,
    val postType: String? = null,
    /** See [MostViewedItem.iconUrl]. */
    val iconUrl: String? = null
) : Parcelable
