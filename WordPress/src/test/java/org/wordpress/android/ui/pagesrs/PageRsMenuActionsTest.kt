package org.wordpress.android.ui.pagesrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.PostStatus

internal class PageRsMenuActionsTest {
    @Test
    fun `published page gets the full action set`() {
        val actions = computeActions(status = PostStatus.Publish)

        assertThat(actions).containsExactly(
            PageRsMenuAction.VIEW,
            PageRsMenuAction.SET_PARENT,
            PageRsMenuAction.SET_AS_HOMEPAGE,
            PageRsMenuAction.SET_AS_POSTS_PAGE,
            PageRsMenuAction.MOVE_TO_DRAFT,
            PageRsMenuAction.DUPLICATE,
            PageRsMenuAction.SHARE,
            PageRsMenuAction.COPY_URL,
            PageRsMenuAction.BLAZE,
            PageRsMenuAction.TRASH
        )
    }

    @Test
    fun `homepage cannot be trashed, drafted, or set as homepage again`() {
        val actions = computeActions(status = PostStatus.Publish, isHomepage = true)

        assertThat(actions).doesNotContain(
            PageRsMenuAction.TRASH,
            PageRsMenuAction.MOVE_TO_DRAFT,
            PageRsMenuAction.SET_AS_HOMEPAGE
        )
        assertThat(actions).contains(PageRsMenuAction.SET_AS_POSTS_PAGE)
    }

    @Test
    fun `posts page cannot be set as posts page again`() {
        val actions = computeActions(status = PostStatus.Publish, isPostsPage = true)

        assertThat(actions).doesNotContain(PageRsMenuAction.SET_AS_POSTS_PAGE)
        assertThat(actions).contains(PageRsMenuAction.SET_AS_HOMEPAGE)
        assertThat(actions).contains(PageRsMenuAction.TRASH)
    }

    @Test
    fun `homepage settings hidden when site cannot manage homepage`() {
        val actions = computeActions(status = PostStatus.Publish, canManageHomepage = false)

        assertThat(actions).doesNotContain(
            PageRsMenuAction.SET_AS_HOMEPAGE,
            PageRsMenuAction.SET_AS_POSTS_PAGE
        )
    }

    @Test
    fun `blaze hidden for password protected page`() {
        val actions = computeActions(status = PostStatus.Publish, hasPassword = true)

        assertThat(actions).doesNotContain(PageRsMenuAction.BLAZE)
    }

    @Test
    fun `blaze hidden for ineligible site`() {
        val actions = computeActions(status = PostStatus.Publish, isBlazeEligibleSite = false)

        assertThat(actions).doesNotContain(PageRsMenuAction.BLAZE)
    }

    @Test
    fun `blaze hidden for private page`() {
        val actions = computeActions(status = PostStatus.Private)

        assertThat(actions).doesNotContain(PageRsMenuAction.BLAZE)
    }

    @Test
    fun `draft page gets publish now but not move to draft`() {
        val actions = computeActions(status = PostStatus.Draft)

        assertThat(actions).containsExactly(
            PageRsMenuAction.VIEW,
            PageRsMenuAction.SET_PARENT,
            PageRsMenuAction.PUBLISH_NOW,
            PageRsMenuAction.DUPLICATE,
            PageRsMenuAction.SHARE,
            PageRsMenuAction.COPY_URL,
            PageRsMenuAction.TRASH
        )
    }

    @Test
    fun `pending page gets the draft action set`() {
        assertThat(computeActions(status = PostStatus.Pending))
            .isEqualTo(computeActions(status = PostStatus.Draft))
    }

    @Test
    fun `scheduled page can move to draft but not publish`() {
        val actions = computeActions(status = PostStatus.Future)

        assertThat(actions).containsExactly(
            PageRsMenuAction.VIEW,
            PageRsMenuAction.SET_PARENT,
            PageRsMenuAction.SHARE,
            PageRsMenuAction.COPY_URL,
            PageRsMenuAction.MOVE_TO_DRAFT,
            PageRsMenuAction.TRASH
        )
    }

    @Test
    fun `trashed page can only be drafted or deleted`() {
        val actions = computeActions(status = PostStatus.Trash)

        assertThat(actions).containsExactly(
            PageRsMenuAction.MOVE_TO_DRAFT,
            PageRsMenuAction.DELETE_PERMANENTLY
        )
    }

    @Test
    fun `unknown status gets no actions`() {
        assertThat(computeActions(status = null)).isEmpty()
        assertThat(computeActions(status = PostStatus.Any)).isEmpty()
        assertThat(computeActions(status = PostStatus.Custom("custom"))).isEmpty()
    }

    private fun computeActions(
        status: PostStatus?,
        isHomepage: Boolean = false,
        isPostsPage: Boolean = false,
        hasPassword: Boolean = false,
        isBlazeEligibleSite: Boolean = true,
        canManageHomepage: Boolean = true
    ) = computePageMenuActions(
        status = status,
        isHomepage = isHomepage,
        isPostsPage = isPostsPage,
        hasPassword = hasPassword,
        isBlazeEligibleSite = isBlazeEligibleSite,
        canManageHomepage = canManageHomepage
    )
}
