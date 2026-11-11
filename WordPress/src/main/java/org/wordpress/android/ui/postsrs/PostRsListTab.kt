package org.wordpress.android.ui.postsrs

import org.wordpress.android.R
import uniffi.wp_api.PostStatus
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpApiParamPostsOrderBy

enum class PostRsListTab(
    val labelResId: Int,
    val emptyMessageResId: Int,
    val statuses: List<PostStatus>,
    val order: WpApiParamOrder,
    /** When set, the tab sorts by modified date and wraps each row's date in it, e.g. "Modified: %s". */
    val modifiedDateLabelResId: Int = 0
) {
    PUBLISHED(
        labelResId = R.string.post_list_tab_published_posts,
        emptyMessageResId = R.string.posts_published_empty,
        statuses = listOf(PostStatus.Publish, PostStatus.Private),
        order = WpApiParamOrder.DESC
    ),
    DRAFTS(
        labelResId = R.string.post_list_tab_drafts,
        emptyMessageResId = R.string.posts_draft_empty,
        statuses = listOf(PostStatus.Draft, PostStatus.Pending),
        order = WpApiParamOrder.DESC,
        // Most recently edited first, matching iOS. The rows show and group by the same date.
        modifiedDateLabelResId = R.string.rs_date_modified
    ),
    SCHEDULED(
        labelResId = R.string.post_list_tab_scheduled_posts,
        emptyMessageResId = R.string.posts_scheduled_empty,
        statuses = listOf(PostStatus.Future),
        order = WpApiParamOrder.ASC
    ),
    TRASHED(
        labelResId = R.string.post_list_tab_trashed_posts,
        emptyMessageResId = R.string.posts_trashed_empty,
        statuses = listOf(PostStatus.Trash),
        order = WpApiParamOrder.DESC,
        // Trashing bumps the modified date, so this lists the most recently trashed first.
        modifiedDateLabelResId = R.string.rs_date_trashed
    );

    val orderBy: WpApiParamPostsOrderBy
        get() = if (modifiedDateLabelResId != 0) WpApiParamPostsOrderBy.MODIFIED else WpApiParamPostsOrderBy.DATE
}
