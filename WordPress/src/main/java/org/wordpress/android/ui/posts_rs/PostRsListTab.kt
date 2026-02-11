package org.wordpress.android.ui.posts_rs

import androidx.annotation.StringRes
import org.wordpress.android.R
import uniffi.wp_api.PostStatus
import uniffi.wp_api.WpApiParamOrder

/**
 * Tabs for the wordpress-rs post list, each mapping
 * to one or more [PostStatus] values.
 */
enum class PostRsListTab(
    @StringRes val titleResId: Int,
    val statuses: List<PostStatus>,
    val order: WpApiParamOrder
) {
    PUBLISHED(
        titleResId = R.string.post_list_tab_published_posts,
        statuses = listOf(
            PostStatus.Publish,
            PostStatus.Private
        ),
        order = WpApiParamOrder.DESC
    ),
    DRAFTS(
        titleResId = R.string.post_list_tab_drafts,
        statuses = listOf(
            PostStatus.Draft,
            PostStatus.Pending
        ),
        order = WpApiParamOrder.DESC
    ),
    SCHEDULED(
        titleResId = R.string.post_list_tab_scheduled_posts,
        statuses = listOf(PostStatus.Future),
        order = WpApiParamOrder.ASC
    ),
    TRASHED(
        titleResId = R.string.post_list_tab_trashed_posts,
        statuses = listOf(PostStatus.Trash),
        order = WpApiParamOrder.DESC
    )
}
