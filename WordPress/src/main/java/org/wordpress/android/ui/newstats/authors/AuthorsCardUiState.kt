package org.wordpress.android.ui.newstats.authors

import android.os.Parcelable
import androidx.annotation.StringRes
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

    data class Error(
        @StringRes val messageResId: Int,
        val isAuthError: Boolean = false
    ) : AuthorsCardUiState()
}

/**
 * A single author item in the authors list.
 *
 * @param name The author's display name
 * @param avatarUrl URL to the author's avatar image
 * @param views Number of views from this author's posts
 * @param change The change compared to the previous period
 * @param posts The author's posts in the period, most viewed first. A row with posts expands to
 * list them (as old Stats did); one without is inert.
 */
@Parcelize
data class AuthorUiItem(
    val name: String,
    val avatarUrl: String?,
    val views: Long,
    val change: StatsViewChange = StatsViewChange.NoChange,
    val posts: List<AuthorPostUiItem> = emptyList()
) : Parcelable

/**
 * A single post listed under an expanded author row. Tapping it opens the post's detail stats, so
 * the [id] and [title] that screen is launched with travel with the item.
 */
@Parcelize
data class AuthorPostUiItem(
    val id: Long,
    val title: String,
    val views: Long
) : Parcelable
