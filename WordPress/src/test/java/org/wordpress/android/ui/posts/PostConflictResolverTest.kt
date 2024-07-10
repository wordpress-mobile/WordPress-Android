package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import kotlin.test.Test
import org.wordpress.android.R

@Suppress("UNCHECKED_CAST")
@ExperimentalCoroutinesApi
class PostConflictResolverTest : BaseUnitTest() {
    private val dispatcher: Dispatcher = mock()
    private val site: SiteModel = mock()

    private val getPostByLocalPostId = mock(Function1::class.java) as (Int) -> PostModel?
    private val invalidateList = mock(Function0::class.java) as () -> Unit
    private val checkNetworkConnection = mock(Function0::class.java) as () -> Boolean
    private val showSnackbar = mock(Function1::class.java) as (SnackbarMessageHolder) -> Unit
    private val uploadStore: UploadStore = mock()
    private val postStore: PostStore = mock()

    // Class under test
    private lateinit var postConflictResolver: PostConflictResolver

    @Before
    fun setUp() {
        postConflictResolver = PostConflictResolver(
            dispatcher,
            site,
            postStore,
            uploadStore,
            getPostByLocalPostId,
            invalidateList,
            checkNetworkConnection,
            showSnackbar,
        )
    }

    @Test
    fun `given network connection, when update conflicted post with local version is invoked, then success`() {
        whenever(checkNetworkConnection.invoke()).thenReturn(true)
        val post = PostModel()
        whenever(getPostByLocalPostId.invoke(anyInt())).thenReturn(post)
        val expectedSnackbarMessage = SnackbarMessageHolder(
            UiString.UiStringRes(R.string.snackbar_conflict_web_version_discarded)
        )

        postConflictResolver.updateConflictedPostWithLocalVersion(123)

        verify(invalidateList).invoke()
        verify(uploadStore).clearUploadErrorForPost(post)
        verify(showSnackbar).invoke(expectedSnackbarMessage)
        verify(dispatcher).dispatch(any())
    }

    @Test
    fun `given no network connection, when update conflicted post with local version is invoked, then no network`() {
        whenever(checkNetworkConnection.invoke()).thenReturn(false)

        postConflictResolver.updateConflictedPostWithLocalVersion(123)

        verifyNoInteractions(getPostByLocalPostId)
        verifyNoInteractions(postStore)
        verifyNoInteractions(showSnackbar)
        verifyNoInteractions(dispatcher)
    }

    @Test
    fun `given post is in conflict with remote, when on post updated, then clear upload error for post`() {
        val updatedPost = PostModel()
        whenever(getPostByLocalPostId.invoke(anyInt())).thenReturn(updatedPost)
        whenever(checkNetworkConnection.invoke()).thenReturn(true)
        val expectedSnackbarMessage = SnackbarMessageHolder(
            UiString.UiStringRes(R.string.snackbar_conflict_local_version_discarded)
        )

        postConflictResolver.updateConflictedPostWithRemoteVersion(123)
        postConflictResolver.onPostSuccessfullyUpdated()

        verify(uploadStore).clearUploadErrorForPost(updatedPost)
        verify(showSnackbar).invoke(expectedSnackbarMessage)
        verify(postStore).removeLocalRevision(updatedPost)
    }
}

