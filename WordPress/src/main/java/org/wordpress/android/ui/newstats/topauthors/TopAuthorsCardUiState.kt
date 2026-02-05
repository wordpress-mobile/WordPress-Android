package org.wordpress.android.ui.newstats.topauthors

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.newstats.components.StatsViewChange

/**
 * UI State for the Top Authors stats card.
 */
sealed class TopAuthorsCardUiState {
    data object Loading : TopAuthorsCardUiState()

    data class Loaded(
        val authors: List<TopAuthorUiItem>,
        val maxViewsForBar: Long,
        val hasMoreItems: Boolean
    ) : TopAuthorsCardUiState()

    data class Error(val message: String) : TopAuthorsCardUiState()
}

/**
 * A single author item in the top authors list.
 *
 * @param name The author's display name
 * @param avatarUrl URL to the author's avatar image
 * @param views Number of views from this author's posts
 * @param change The change compared to the previous period
 */
@Parcelize
data class TopAuthorUiItem(
    val name: String,
    val avatarUrl: String?,
    val views: Long,
    val change: StatsViewChange = StatsViewChange.NoChange
) : Parcelable
