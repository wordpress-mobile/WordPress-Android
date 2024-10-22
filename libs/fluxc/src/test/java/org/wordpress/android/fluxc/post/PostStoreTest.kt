package org.wordpress.android.fluxc.post

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.revisions.LocalDiffModel
import org.wordpress.android.fluxc.model.revisions.LocalRevisionModel
import org.wordpress.android.fluxc.model.revisions.RevisionModel
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.PostStore
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

    @Test
    fun `handleFetchedPostList emits FetchPostAction when autosave object changed in remote`() {
        // Arrange
        val postInLocalDb = createPostModel()
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb, autoSaveModified = "modified in remote")
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

    /**
     *  We can't fetch the post from the remote as we'd override the local changes. The plan is to introduce improved
     *  conflict resolution on the UI and handle even the scenario for cases when the only thing that has changed is
     *  the autosave object. The current (temporary) solution simply ignores the fact that the auto-save object was
     *  updated in the remote.
     */
    @Test
    fun `handleFetchedPostList doesn't emit UpdatePostAction when changed locally and autosave changed in remote`() {
        // Arrange
        val postInLocalDb = createPostModel(isLocallyChanged = true)
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(postInLocalDb, autoSaveModified = "modified in remote")
        val action = createFetchedPostListAction(postListItems = listOf(remotePostListItem))

        // Act
        store.onAction(action)

        // Assert
        verify(dispatcher).dispatch(argThat {
            (this.type == ListAction.FETCHED_LIST_ITEMS)
        })
        verifyNoMoreInteractions(dispatcher)
    }

    /**
     * This is handled as if the only thing that has changed was the post status - we invoke UpdatePostAction and
     * the UI needs to take care of conflict resolution. The fact that the autosave object has changed is being
     * currently ignored.
     */
    @Test
    fun `handleFetchedPostList emits UpdatePostAction when changed locally and status + autosave changed in remote`() {
        // Arrange
        val postInLocalDb = createPostModel(isLocallyChanged = true)
        whenever(postSqlUtils.getPostsByRemoteIds(any(), any())).thenReturn(listOf(postInLocalDb))

        val remotePostListItem = createRemotePostListItem(
                postInLocalDb,
                status = PostStatus.TRASHED.toString(),
                autoSaveModified = "modified in remote"
        )
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
    fun `Should return mapped RevisionModel when getRevisionById is called`() {
        // Arrange
        val localRevision = LocalRevisionModel(1)
        val localDiffs = listOf<LocalDiffModel>(LocalDiffModel(1))
        whenever(postSqlUtils.getRevisionById(any(), any(), any())).thenReturn(localRevision)
        whenever(postSqlUtils.getLocalRevisionDiffs(any())).thenReturn(localDiffs)

        // Act
        val expected = RevisionModel.fromLocalRevisionAndDiffs(localRevision, localDiffs)
        val actual = store.getRevisionById(1L, 1L, 1L)

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return null when PostSqlUtils getRevisionById returns null`() {
        // Arrange
        whenever(postSqlUtils.getRevisionById(any(), any(), any())).thenReturn(null)

        // Act
        val expected = null
        val actual = store.getRevisionById(1L, 1L, 1L)

        // Assert
        assertEquals(expected, actual)
    }

    @Test
    fun `Should call PostSqlUtils getRevisionById when getRevisionById is called`() {
        // Arrange
        whenever(postSqlUtils.getRevisionById(any(), any(), any())).thenReturn(LocalRevisionModel())

        // Act
        store.getRevisionById(1L, 1L, 1L)

        // Assert
        verify(postSqlUtils).getRevisionById("1", 1L, 1L)
    }

    @Test
    fun `Should delete all revisions and diffs of a Post when removePost is called`() {
        // Arrange
        whenever(postSqlUtils.deletePost(any())).thenReturn(1)
        val postModel = createPostModel()

        // Act
        store.onAction(PostActionBuilder.newRemovePostAction(postModel))

        // Assert
        verify(postSqlUtils).deleteLocalRevisionAndDiffsOfAPostOrPage(postModel)
    }

    @Test
    fun `Should remove all revisions and diffs when removeAllPosts is called`() {
        // Arrange
        val postModel = createPostModel()

        // Act
        store.onAction(PostActionBuilder.newRemoveAllPostsAction())

        // Assert
        verify(postSqlUtils).deleteAllLocalRevisionsAndDiffs()
    }

    private fun createFetchedPostListAction(
        postListItems: List<PostListItem> = listOf(),
        listDescriptor: PostListDescriptor = mockedListDescriptor,
        postError: PostError? = null
    ) = PostActionBuilder.newFetchedPostListAction(
        PostStore.FetchPostListResponsePayload(
            listDescriptor,
            postListItems,
            false,
            false,
            postError
        )
    )

    private fun createPostModel(isLocallyChanged: Boolean = false, postStatus: PostStatus = PUBLISHED): PostModel {
        val post = PostModel()
        post.setRemotePostId(1)
        post.setStatus(postStatus.toString())
        post.setIsLocallyChanged(isLocallyChanged)
        post.setAutoSaveModified("1955-11-05T14:15:00Z")
        return post
    }

    private fun createRemotePostListItem(
        post: PostModel,
        status: String = post.status,
        lastModified: String = post.lastModified,
        autoSaveModified: String? = post.autoSaveModified
    ) = PostListItem(post.remotePostId, lastModified, status, autoSaveModified)
}
