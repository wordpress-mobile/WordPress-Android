package org.wordpress.android.ui.pagesrs

import org.wordpress.android.R
import uniffi.wp_api.PostStatus
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpApiParamPostsOrderBy

internal enum class PageRsListTab(
    val labelResId: Int,
    val emptyMessageResId: Int,
    val statuses: List<PostStatus>,
    val orderBy: WpApiParamPostsOrderBy,
    val order: WpApiParamOrder
) {
    // PUBLISHED / DRAFTS / TRASHED are alphabetical to match the legacy pages list
    // (see PR #22896). Sibling order within the published hierarchy comes from this sort.
    PUBLISHED(
        labelResId = R.string.pages_published,
        emptyMessageResId = R.string.pages_empty_published,
        statuses = listOf(PostStatus.Publish, PostStatus.Private),
        orderBy = WpApiParamPostsOrderBy.TITLE,
        order = WpApiParamOrder.ASC
    ),
    DRAFTS(
        labelResId = R.string.pages_drafts,
        emptyMessageResId = R.string.pages_empty_drafts,
        statuses = listOf(PostStatus.Draft, PostStatus.Pending),
        orderBy = WpApiParamPostsOrderBy.TITLE,
        order = WpApiParamOrder.ASC
    ),
    // SCHEDULED stays chronological so "next to publish" is at the top; legacy groups by
    // date with dividers, but until those land an alphabetical sort here would be useless.
    // Follow-up: add date-divider grouping (e.g. Today / Tomorrow / Next week) to match legacy.
    SCHEDULED(
        labelResId = R.string.pages_scheduled,
        emptyMessageResId = R.string.pages_empty_scheduled,
        statuses = listOf(PostStatus.Future),
        orderBy = WpApiParamPostsOrderBy.DATE,
        order = WpApiParamOrder.ASC
    ),
    TRASHED(
        labelResId = R.string.pages_trashed,
        emptyMessageResId = R.string.pages_empty_trashed,
        statuses = listOf(PostStatus.Trash),
        orderBy = WpApiParamPostsOrderBy.TITLE,
        order = WpApiParamOrder.ASC
    );
}
