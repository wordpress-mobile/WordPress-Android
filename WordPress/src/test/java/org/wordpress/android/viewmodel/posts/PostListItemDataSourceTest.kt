package org.wordpress.android.viewmodel.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.EndListIndicatorIdentifier
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.SectionHeaderIdentifier

class PostListItemDataSourceTest {
    private lateinit var dataSource: PostListItemDataSource
    private lateinit var postStore: PostStore
    private lateinit var posts: List<PostModel>

    @Before
    fun setup() {
        postStore = mock()
        whenever(postStore.getLocalPostIdsForDescriptor(any())).thenReturn(emptyList())
        dataSource = PostListItemDataSource(mock(), postStore, mock(), mock(), PostListType.SEARCH)
    }

    @Test
    fun `getItemIdentifiers when searching and results are in all categories`() {
        posts = makePosts()
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(posts)

        val identifiers = dataSource.getItemIdentifiers(mock(), remoteItemIds = emptyList(), isListFullyFetched = true)
        assertThat(identifiers.size).isEqualTo(11)

        assertThat((identifiers[0] as SectionHeaderIdentifier).type).isEqualTo(PostListType.PUBLISHED)
        assertThat((identifiers[1] as LocalPostId).id.value).isEqualTo(2)
        assertThat((identifiers[2] as LocalPostId).id.value).isEqualTo(6)

        assertThat((identifiers[3] as SectionHeaderIdentifier).type).isEqualTo(PostListType.DRAFTS)
        assertThat((identifiers[4] as LocalPostId).id.value).isEqualTo(1)
        assertThat((identifiers[5] as LocalPostId).id.value).isEqualTo(3)

        assertThat((identifiers[6] as SectionHeaderIdentifier).type).isEqualTo(PostListType.SCHEDULED)
        assertThat((identifiers[7] as LocalPostId).id.value).isEqualTo(5)

        assertThat((identifiers[8] as SectionHeaderIdentifier).type).isEqualTo(PostListType.TRASHED)
        assertThat((identifiers[9] as LocalPostId).id.value).isEqualTo(4)

        assertThat(identifiers.last()).isInstanceOf(EndListIndicatorIdentifier.javaClass)
    }

    @Test
    fun `getItemIdentifiers list is not fully fetched`() {
        posts = makePosts()
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(posts)

        val identifiers = dataSource.getItemIdentifiers(mock(), remoteItemIds = emptyList(), isListFullyFetched = false)
        assertThat(identifiers.size).isEqualTo(10)
        assertThat(identifiers.last()).isNotInstanceOf(EndListIndicatorIdentifier.javaClass)
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in PUBLISHED`() {
        posts = makePosts().filter { PostListType.fromPostStatus(PostStatus.fromPost(it)) != PostListType.PUBLISHED }
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(posts)

        val identifiers = dataSource.getItemIdentifiers(mock(), remoteItemIds = emptyList(), isListFullyFetched = true)
        assertThat(identifiers.size).isEqualTo(8)

        assertThat((identifiers[0] as SectionHeaderIdentifier).type).isEqualTo(PostListType.DRAFTS)
        assertThat((identifiers[1] as LocalPostId).id.value).isEqualTo(1)
        assertThat((identifiers[2] as LocalPostId).id.value).isEqualTo(3)

        assertThat((identifiers[3] as SectionHeaderIdentifier).type).isEqualTo(PostListType.SCHEDULED)
        assertThat((identifiers[4] as LocalPostId).id.value).isEqualTo(5)

        assertThat((identifiers[5] as SectionHeaderIdentifier).type).isEqualTo(PostListType.TRASHED)
        assertThat((identifiers[6] as LocalPostId).id.value).isEqualTo(4)

        assertThat(identifiers.last()).isInstanceOf(EndListIndicatorIdentifier.javaClass)
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in DRAFTS`() {
        posts = makePosts().filter { PostListType.fromPostStatus(PostStatus.fromPost(it)) != PostListType.DRAFTS }
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(posts)

        val identifiers = dataSource.getItemIdentifiers(mock(), remoteItemIds = emptyList(), isListFullyFetched = true)
        assertThat(identifiers.size).isEqualTo(8)

        assertThat((identifiers[0] as SectionHeaderIdentifier).type).isEqualTo(PostListType.PUBLISHED)
        assertThat((identifiers[1] as LocalPostId).id.value).isEqualTo(2)
        assertThat((identifiers[2] as LocalPostId).id.value).isEqualTo(6)

        assertThat((identifiers[3] as SectionHeaderIdentifier).type).isEqualTo(PostListType.SCHEDULED)
        assertThat((identifiers[4] as LocalPostId).id.value).isEqualTo(5)

        assertThat((identifiers[5] as SectionHeaderIdentifier).type).isEqualTo(PostListType.TRASHED)
        assertThat((identifiers[6] as LocalPostId).id.value).isEqualTo(4)

        assertThat(identifiers.last()).isInstanceOf(EndListIndicatorIdentifier.javaClass)
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in SCHEDULED`() {
        posts = makePosts().filter { PostListType.fromPostStatus(PostStatus.fromPost(it)) != PostListType.SCHEDULED }
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(posts)

        val identifiers = dataSource.getItemIdentifiers(mock(), remoteItemIds = emptyList(), isListFullyFetched = true)
        assertThat(identifiers.size).isEqualTo(9)

        assertThat((identifiers[0] as SectionHeaderIdentifier).type).isEqualTo(PostListType.PUBLISHED)
        assertThat((identifiers[1] as LocalPostId).id.value).isEqualTo(2)
        assertThat((identifiers[2] as LocalPostId).id.value).isEqualTo(6)

        assertThat((identifiers[3] as SectionHeaderIdentifier).type).isEqualTo(PostListType.DRAFTS)
        assertThat((identifiers[4] as LocalPostId).id.value).isEqualTo(1)
        assertThat((identifiers[5] as LocalPostId).id.value).isEqualTo(3)

        assertThat((identifiers[6] as SectionHeaderIdentifier).type).isEqualTo(PostListType.TRASHED)
        assertThat((identifiers[7] as LocalPostId).id.value).isEqualTo(4)

        assertThat(identifiers.last()).isInstanceOf(EndListIndicatorIdentifier.javaClass)
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in TRASHED`() {
        posts = makePosts().filter { PostListType.fromPostStatus(PostStatus.fromPost(it)) != PostListType.TRASHED }
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(posts)

        val identifiers = dataSource.getItemIdentifiers(mock(), remoteItemIds = emptyList(), isListFullyFetched = true)
        assertThat(identifiers.size).isEqualTo(9)

        assertThat((identifiers[0] as SectionHeaderIdentifier).type).isEqualTo(PostListType.PUBLISHED)
        assertThat((identifiers[1] as LocalPostId).id.value).isEqualTo(2)
        assertThat((identifiers[2] as LocalPostId).id.value).isEqualTo(6)

        assertThat((identifiers[3] as SectionHeaderIdentifier).type).isEqualTo(PostListType.DRAFTS)
        assertThat((identifiers[4] as LocalPostId).id.value).isEqualTo(1)
        assertThat((identifiers[5] as LocalPostId).id.value).isEqualTo(3)

        assertThat((identifiers[6] as SectionHeaderIdentifier).type).isEqualTo(PostListType.SCHEDULED)
        assertThat((identifiers[7] as LocalPostId).id.value).isEqualTo(5)

        assertThat(identifiers.last()).isInstanceOf(EndListIndicatorIdentifier.javaClass)
    }

    @Test
    fun `getItemIdentifiers when searching and results are empty`() {
        posts = emptyList()
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(posts)

        val identifiers = dataSource.getItemIdentifiers(mock(), remoteItemIds = emptyList(), isListFullyFetched = true)
        assertThat(identifiers.size).isEqualTo(0)
    }

    private fun makePosts(): List<PostModel> {
        val posts = mutableListOf<PostModel>()
        val statuses = listOf("draft", "publish", "pending", "trash", "future", "private")
        statuses.forEachIndexed { i, status ->
            val post = PostModel()
            post.setId(i + 1)
            post.setStatus(status)
            posts.add(post)
        }

        return posts
    }
}
