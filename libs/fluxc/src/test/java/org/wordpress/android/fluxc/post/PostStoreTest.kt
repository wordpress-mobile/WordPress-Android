package org.wordpress.android.fluxc.post

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListResponsePayload
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.PostStore.PostListItem

@RunWith(MockitoJUnitRunner::class)
class PostStoreTest {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var postSqlUtils: PostSqlUtils
    @Mock lateinit var dispatcher: Dispatcher
    private lateinit var store: PostStore
    @Mock lateinit var mockedListDescriptor: PostListDescriptor

    @Before
    fun setUp() {
        store = PostStore(dispatcher, mock(), mock(), postSqlUtils)
        whenever(mockedListDescriptor.site).thenReturn(mock())
        // verify "register" so we can use verifyNoMoreInteractions in all the test methods
        verify(dispatcher).register(any())
    }

    @Test
    fun `handleFetchedPostList emits FetchedListItemsAction on success`() {
        // Arrange
        val action = createFetchedPostListAction()

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    @Test
    fun `handleFetchedPostList emits FetchedListItemsAction on error`() {
        // Arrange
        val action = createFetchedPostListAction(postError = PostError(GENERIC_ERROR))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    @Test
    fun `handleFetchedPostList emits FetchedListItemsAction with an error field set on error`() {
        // Arrange
        val action = createFetchedPostListAction(postError = PostError(GENERIC_ERROR))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.payload as FetchedListItemsPayload).isError
        })
        verifyNoMoreInteractions(dispatcher)
    }

    @Test
    fun `handleFetchedPostList emits just FetchedListItemsAction when post not changed`() {
        // Arrange
        val postInLocalDb = createPostModel()
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb)
        val action = createFetchedPostListAction(postListItems = listOf(remotePostListItem))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    @Test
    fun `handleFetchedPostList emits FetchPostAction when post changed in remote`() {
        // Arrange
        val postInLocalDb = createPostModel()
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb, lastModified = "modified in remote")
        val action = createFetchedPostListAction(postListItems = listOf(remotePostListItem))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == PostAction.FETCH_POST)
        })
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    @Test
    fun `handleFetchedPostList emits UpdatePostAction when post changed in both remote and local`() {
        // Arrange
        val postInLocalDb = createPostModel(isLocallyChanged = true)
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb, lastModified = "modified in remote")
        val action = createFetchedPostListAction(postListItems = listOf(remotePostListItem))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == PostAction.UPDATE_POST)
        })
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    @Test
    fun `handleFetchedPostList emits UpdatePostAction when post changed locally and post status changed in remote`() {
        // Arrange
        val postInLocalDb = createPostModel(isLocallyChanged = true)
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb, status = PostStatus.TRASHED.toString())
        val action = createFetchedPostListAction(postListItems = listOf(remotePostListItem))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == PostAction.UPDATE_POST)
        })
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    @Test
    fun `handleFetchedPostList sets remoteLastModified field when post changed in both remote and local`() {
        // Arrange
        val postInLocalDb = createPostModel(isLocallyChanged = true)
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb, lastModified = "modified in remote")
        val action = createFetchedPostListAction(postListItems = listOf(remotePostListItem))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            ((this.payload as? PostModel)?.remoteLastModified == remotePostListItem.lastModified)
        })
    }

    @Test
    fun `handleFetchedPostList emits FetchPostAction when post status changed in remote`() {
        // Arrange
        val postInLocalDb = createPostModel()
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb, status = PostStatus.TRASHED.toString())
        val action = createFetchedPostListAction(postListItems = listOf(remotePostListItem))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == PostAction.FETCH_POST)
        })
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    private fun createFetchedPostListAction(
        postListItems: List<PostListItem> = listOf(),
        listDescriptor: PostListDescriptor = mockedListDescriptor,
        postError: PostError? = null
    ) = PostActionBuilder.newFetchedPostListAction(
            FetchPostListResponsePayload(
                    listDescriptor,
                    postListItems,
                    false,
                    false,
                    postError
            )
    )

    private fun createPostModel(isLocallyChanged: Boolean = false, postStatus: PostStatus = PUBLISHED): PostModel {
        val post = PostModel()
        post.remotePostId = 1
        post.status = postStatus.toString()
        post.setIsLocallyChanged(isLocallyChanged)
        return post
    }

    private fun createRemotePostListItem(
        post: PostModel,
        status: String = post.status,
        lastModified: String = post.lastModified
    ) = PostListItem(post.remotePostId, lastModified, status)
}
