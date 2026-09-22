package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.StringRes
import org.wordpress.android.ui.rs.data.FeaturedImageUrls
import uniffi.wp_api.PostStatus

/**
 * One post or page, projected onto what the rs list screens need.
 *
 * This is the subset of wordpress-rs' `AnyPostWithEditContext` the lists read, and that is one
 * type for both post types - so a field one screen ignores is not a field that doesn't apply to
 * it. Posts never read [parentId] and pages never read [commentsOpen], but both are real fields of
 * the entity and both are populated.
 *
 * Generic over the screen's own menu-action enum. Those are not merged - they offer different
 * actions and word the shared ones differently - so carrying the type here is what keeps the
 * screens' `when` blocks exhaustive without a cast.
 *
 * Screen-specific decoration lives outside this type: the pages hierarchy and its synthetic rows
 * are on PageRsListItem, and the projection onto a rendered row is each screen's own
 * `toContentListRowUiState`.
 */
data class ContentItemUiModel<A : RsMenuAction>(
    val remoteId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    /**
     * Raw publish date, for bucketing rows into date groups.
     *
     * Populated for pages too, though only the posts list groups by date - pages sort by title, so
     * date buckets there would be meaningless. Anything that later shares the grouping must take
     * "should this group?" as an explicit argument rather than inferring it from this being set.
     */
    val dateGmtMillis: Long = 0L,
    /** Parent page id, 0 when top-level. Read by the pages tree and parent picker only. */
    val parentId: Long = 0L,
    /** All-time views, or null when stats are unavailable or not fetched yet. */
    val viewCount: Long? = null,
    /** All-time comment count, or null when stats are unavailable or not fetched yet. */
    val commentCount: Long? = null,
    /** True while this row's metrics are expected but have not arrived, so it shows a skeleton. */
    val areMetricsPending: Boolean = false,
    val lastModified: String = "",
    val link: String = "",
    val hasPassword: Boolean = false,
    /** Whether the item accepts comments. Read by the posts menu only. */
    val commentsOpen: Boolean = false,
    val status: PostStatus? = null,
    @StringRes val statusLabelResId: Int = 0,
    val authorId: Long = 0L,
    val authorDisplayName: String? = null,
    val featuredImageId: Long = 0L,
    val featuredImage: FeaturedImageUrls? = null,
    /** True when the media lookup answered without a URL, so the row should stop waiting for one. */
    val isFeaturedImageUnresolvable: Boolean = false,
    val actions: List<A> = emptyList(),
    val badges: List<Int> = emptyList(),
    val displayState: ContentDisplayState = ContentDisplayState.NORMAL
) {
    /** Derived rather than stored, so it cannot go stale against an updated [status]. */
    val isTrashed: Boolean get() = status is PostStatus.Trash
}
