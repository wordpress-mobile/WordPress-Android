package org.wordpress.android.ui.pagesrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class PageRsHomepageSettingsTest {
    @Test
    fun `no update when site shows latest posts on front`() {
        val params = computeParams(showOnFront = "posts", target = HomepageTarget.PAGE_ON_FRONT)

        assertThat(params).isNull()
    }

    @Test
    fun `setting homepage keeps an unrelated posts page`() {
        val params = computeParams(target = HomepageTarget.PAGE_ON_FRONT, pageId = 5L)

        assertThat(params?.pageOnFront).isEqualTo(5uL)
        assertThat(params?.pageForPosts).isEqualTo(POSTS_PAGE_ID.toULong())
    }

    @Test
    fun `setting the current posts page as homepage clears the posts page`() {
        val params = computeParams(target = HomepageTarget.PAGE_ON_FRONT, pageId = POSTS_PAGE_ID)

        assertThat(params?.pageOnFront).isEqualTo(POSTS_PAGE_ID.toULong())
        assertThat(params?.pageForPosts).isEqualTo(0uL)
    }

    @Test
    fun `setting posts page keeps an unrelated homepage`() {
        val params = computeParams(target = HomepageTarget.PAGE_FOR_POSTS, pageId = 5L)

        assertThat(params?.pageForPosts).isEqualTo(5uL)
        assertThat(params?.pageOnFront).isEqualTo(HOMEPAGE_ID.toULong())
    }

    @Test
    fun `setting the current homepage as posts page clears the homepage`() {
        val params = computeParams(target = HomepageTarget.PAGE_FOR_POSTS, pageId = HOMEPAGE_ID)

        assertThat(params?.pageForPosts).isEqualTo(HOMEPAGE_ID.toULong())
        assertThat(params?.pageOnFront).isEqualTo(0uL)
    }

    private fun computeParams(
        showOnFront: String = "page",
        target: HomepageTarget,
        pageId: Long = 5L
    ) = computeHomepageUpdateParams(
        showOnFront = showOnFront,
        currentPageOnFront = HOMEPAGE_ID,
        currentPageForPosts = POSTS_PAGE_ID,
        target = target,
        pageId = pageId
    )

    companion object {
        private const val HOMEPAGE_ID = 10L
        private const val POSTS_PAGE_ID = 20L
    }
}
