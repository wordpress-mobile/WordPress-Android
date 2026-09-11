package org.wordpress.android.ui.rs.contentlist

/**
 * One row of the redesigned posts/pages list.
 *
 * Deliberately post- and page-agnostic: the overflow menu is a slot so each screen can supply its
 * own actions, and everything else is plain data. Both the posts and pages rs screens render from
 * this so the two lists cannot drift apart again.
 */
data class ContentListRowUiState(
    val id: Long,
    val title: String,
    val excerpt: String = "",
    val dateLabel: String,
    val imageUrl: String? = null,
    val isImagePending: Boolean = false,
    val viewCount: Long? = null,
    val commentCount: Long? = null,
    val areMetricsPending: Boolean = false,
    val badges: List<Int> = emptyList(),
    val isSyncing: Boolean = false,
    val hasSyncFailed: Boolean = false
)
