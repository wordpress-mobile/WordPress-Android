package org.wordpress.android.ui.pagesrs

import org.wordpress.android.R
import uniffi.wp_api.PostStatus
import uniffi.wp_api.WpApiParamOrder

internal enum class PageRsListTab(
    val labelResId: Int,
    val emptyMessageResId: Int,
    val statuses: List<PostStatus>,
    val order: WpApiParamOrder
) {
    PUBLISHED(
        labelResId = R.string.pages_published,
        emptyMessageResId = R.string.pages_empty_published,
        statuses = listOf(PostStatus.Publish, PostStatus.Private),
        order = WpApiParamOrder.DESC
    ),
    DRAFTS(
        labelResId = R.string.pages_drafts,
        emptyMessageResId = R.string.pages_empty_drafts,
        statuses = listOf(PostStatus.Draft, PostStatus.Pending),
        order = WpApiParamOrder.DESC
    ),
    SCHEDULED(
        labelResId = R.string.pages_scheduled,
        emptyMessageResId = R.string.pages_empty_scheduled,
        statuses = listOf(PostStatus.Future),
        order = WpApiParamOrder.ASC
    ),
    TRASHED(
        labelResId = R.string.pages_trashed,
        emptyMessageResId = R.string.pages_empty_trashed,
        statuses = listOf(PostStatus.Trash),
        order = WpApiParamOrder.DESC
    );
}
