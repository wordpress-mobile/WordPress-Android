package org.wordpress.android.ui.newstats.authors

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.newstats.components.StatsViewChange

/**
 * UI State for the Authors stats card.
 */
sealed class AuthorsCardUiState {
    data object Loading : AuthorsCardUiState()

    data class Loaded(
        val authors: List<AuthorUiItem>,
        val maxViewsForBar: Long,
        val hasMoreItems: Boolean
    ) : AuthorsCardUiState()

    data class Error(val message: String) : AuthorsCardUiState()
}

/**
 * A single author item in the authors list.
 *
 * @param name The author's display name
 * @param avatarUrl URL to the author's avatar image
 * @param views Number of views from this author's posts
 * @param change The change compared to the previous period
 */
@Parcelize
data class AuthorUiItem(
    val name: String,
    val avatarUrl: String?,
    val views: Long,
    val change: StatsViewChange = StatsViewChange.NoChange
) : Parcelable
