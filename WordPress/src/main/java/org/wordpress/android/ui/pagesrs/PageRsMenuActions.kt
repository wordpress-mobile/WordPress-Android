package org.wordpress.android.ui.pagesrs

import uniffi.wp_api.PostStatus

/**
 * Computes the overflow-menu actions for a page row, keyed off the page's own status (not the
 * tab) so search results — which mix statuses — get the right menu. Mirrors the visibility
 * rules of the legacy pages list (CreatePageListItemActionsUseCase): the homepage cannot be
 * trashed or drafted, "Set as Homepage / Posts Page" only appear when [canManageHomepage]
 * (for WP.com sites that means manage-options capability plus a static page on front; for
 * self-hosted sites those fields aren't synced, so the actions are offered and verified at
 * execution time), and Blaze requires an eligible site and a non-password-protected
 * published page.
 */
@Suppress("LongParameterList")
internal fun computePageMenuActions(
    status: PostStatus?,
    isHomepage: Boolean,
    isPostsPage: Boolean,
    hasPassword: Boolean,
    isBlazeEligibleSite: Boolean,
    canManageHomepage: Boolean
): List<PageRsMenuAction> = when (status) {
    is PostStatus.Publish, is PostStatus.Private -> buildList {
        add(PageRsMenuAction.VIEW)
        add(PageRsMenuAction.SET_PARENT)
        if (canManageHomepage && !isHomepage) add(PageRsMenuAction.SET_AS_HOMEPAGE)
        if (canManageHomepage && !isPostsPage) add(PageRsMenuAction.SET_AS_POSTS_PAGE)
        if (!isHomepage) add(PageRsMenuAction.MOVE_TO_DRAFT)
        add(PageRsMenuAction.DUPLICATE)
        add(PageRsMenuAction.SHARE)
        add(PageRsMenuAction.COPY_URL)
        if (isBlazeEligibleSite && !hasPassword && status is PostStatus.Publish) {
            add(PageRsMenuAction.BLAZE)
        }
        if (!isHomepage) add(PageRsMenuAction.TRASH)
    }
    is PostStatus.Draft, is PostStatus.Pending -> buildList {
        add(PageRsMenuAction.VIEW)
        add(PageRsMenuAction.SET_PARENT)
        add(PageRsMenuAction.PUBLISH_NOW)
        add(PageRsMenuAction.DUPLICATE)
        add(PageRsMenuAction.SHARE)
        add(PageRsMenuAction.COPY_URL)
        add(PageRsMenuAction.TRASH)
    }
    is PostStatus.Future -> buildList {
        add(PageRsMenuAction.VIEW)
        add(PageRsMenuAction.SET_PARENT)
        add(PageRsMenuAction.SHARE)
        add(PageRsMenuAction.COPY_URL)
        add(PageRsMenuAction.MOVE_TO_DRAFT)
        add(PageRsMenuAction.TRASH)
    }
    is PostStatus.Trash -> listOf(
        PageRsMenuAction.MOVE_TO_DRAFT,
        PageRsMenuAction.DELETE_PERMANENTLY
    )
    is PostStatus.Any, is PostStatus.Custom, null -> emptyList()
}
