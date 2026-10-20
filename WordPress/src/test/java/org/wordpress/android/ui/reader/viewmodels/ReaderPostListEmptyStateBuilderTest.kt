package org.wordpress.android.ui.reader.viewmodels

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.ReaderTestUtils
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType

class ReaderPostListEmptyStateBuilderTest {
    private val builder = ReaderPostListEmptyStateBuilder()

    private val followedSitesTag = defaultTag(ReaderTag.FOLLOWING_PATH)
    private val likedTag = defaultTag(ReaderTag.LIKED_PATH)
    private val bookmarksTag = ReaderTestUtils.createTag("saved", ReaderTagType.BOOKMARKED)
    private val listTag =
        ReaderTag("list", "list", "list", "https://example.com/read/list/1", ReaderTagType.CUSTOM_LIST)
    private val tag = ReaderTestUtils.createTag("cats")

    @Test
    fun `self-hosted CTA wins over everything else`() {
        val result = build(tag = bookmarksTag, isSelfHostedCta = true, isNetworkAvailable = false)

        assertThat(result).isEqualTo(ReaderPostListEmptyState.SelfHostedCta)
    }

    @Test
    fun `bookmarks list shows the bookmarks state even when offline`() {
        val result = build(tag = bookmarksTag, isNetworkAvailable = false)

        assertThat(result).isEqualTo(ReaderPostListEmptyState.Bookmarks)
    }

    @Test
    fun `no connection comes before a failed request`() {
        val result = build(isNetworkAvailable = false, requestFailed = true)

        assertThat(result).isEqualTo(ReaderPostListEmptyState.NoConnection)
    }

    @Test
    fun `failed request is reported`() {
        assertThat(build(requestFailed = true)).isEqualTo(ReaderPostListEmptyState.RequestFailed(isSearch = false))
        assertThat(build(listType = ReaderPostListType.SEARCH_RESULTS, requestFailed = true))
            .isEqualTo(ReaderPostListEmptyState.RequestFailed(isSearch = true))
    }

    @Test
    fun `updating is shown outside of search`() {
        assertThat(build(isUpdating = true)).isEqualTo(ReaderPostListEmptyState.Updating)
    }

    @Test
    fun `followed sites depend on whether the user follows any sites`() {
        assertThat(build(tag = followedSitesTag, hasFollowedBlogs = true))
            .isEqualTo(ReaderPostListEmptyState.FollowedSitesNoRecentPosts)
        assertThat(build(tag = followedSitesTag, hasFollowedBlogs = false))
            .isEqualTo(ReaderPostListEmptyState.NoFollowedSites)
    }

    @Test
    fun `followed tag states`() {
        assertThat(build(tag = likedTag)).isEqualTo(ReaderPostListEmptyState.NoLikedPosts)
        assertThat(build(tag = listTag)).isEqualTo(ReaderPostListEmptyState.NoPostsInList)
        assertThat(build(tag = tag)).isEqualTo(ReaderPostListEmptyState.NoPostsWithTag)
    }

    @Test
    fun `previews have their own states`() {
        assertThat(build(listType = ReaderPostListType.BLOG_PREVIEW, tag = null))
            .isEqualTo(ReaderPostListEmptyState.NoPostsInBlog)
        assertThat(build(listType = ReaderPostListType.TAG_PREVIEW))
            .isEqualTo(ReaderPostListEmptyState.NoPostsWithTag)
    }

    @Test
    fun `search states`() {
        val search = ReaderPostListType.SEARCH_RESULTS
        assertThat(build(listType = search, searchQuery = null)).isEqualTo(ReaderPostListEmptyState.SearchExplainer)
        assertThat(build(listType = search, searchQuery = "cats", isSearchViewEmpty = true))
            .isEqualTo(ReaderPostListEmptyState.SearchExplainer)
        assertThat(build(listType = search, searchQuery = "cats", isUpdating = true))
            .isEqualTo(ReaderPostListEmptyState.SearchUpdating)
        assertThat(build(listType = search, searchQuery = "cats"))
            .isEqualTo(ReaderPostListEmptyState.SearchNoResults("cats"))
    }

    @Suppress("LongParameterList")
    private fun build(
        listType: ReaderPostListType = ReaderPostListType.TAG_FOLLOWED,
        tag: ReaderTag? = this.tag,
        isSelfHostedCta: Boolean = false,
        isNetworkAvailable: Boolean = true,
        requestFailed: Boolean = false,
        isUpdating: Boolean = false,
        hasFollowedBlogs: Boolean = false,
        searchQuery: String? = null,
        isSearchViewEmpty: Boolean = false
    ) = builder.build(
        ReaderPostListEmptyStateBuilder.Input(
            listType = listType,
            tag = tag,
            isSelfHostedCta = isSelfHostedCta,
            isNetworkAvailable = isNetworkAvailable,
            requestFailed = requestFailed,
            isUpdating = isUpdating,
            hasFollowedBlogs = hasFollowedBlogs,
            searchQuery = searchQuery,
            isSearchViewEmpty = isSearchViewEmpty
        )
    )

    private fun defaultTag(path: String) =
        ReaderTag("slug", "name", "title", "https://example.com$path", ReaderTagType.DEFAULT)
}
