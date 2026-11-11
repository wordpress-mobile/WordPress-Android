package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.StringRes
import org.wordpress.android.ui.rs.data.FeaturedImageUrls
import uniffi.wp_api.PostStatus

/**
 * One post or page as the rs list screens need it. Generic over the screen's own menu-action enum
 * so each screen's `when` stays exhaustive without a cast.
 */
data class ContentItemUiModel<A : RsMenuAction>(
    val remoteId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    /** Raw publish date, for date groups (posts only; pages sort by title). */
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
    val isTrashed: Boolean get() = status is PostStatus.Trash
}
