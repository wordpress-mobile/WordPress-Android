package org.wordpress.android.ui.uploads

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

/**
 * Tests for structured concurrency in [UploadStarter].
 *
 * This is intentionally a separate class from [UploadStarterTest] because this contains non-deterministic
 * tests.
 */
@RunWith(MockitoJUnitRunner::class)
class UploadStarterConcurrentTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private val site = createSiteModel()
    private val draftPosts = listOf(
            createLocallyChangedPostModel(),
            createLocallyChangedPostModel(),
            createLocallyChangedPostModel(),
            createLocallyChangedPostModel(),
            createLocallyChangedPostModel()
    )

    private val postStore = mock<PostStore> {
        on { getPostsWithLocalChanges(eq(site)) } doReturn draftPosts
    }

    private val pageStore = mock<PageStore> {
        onBlocking { getPagesWithLocalChanges(any()) } doReturn emptyList()
    }

    @Test
    fun `it uploads local drafts concurrently`() {
        // Given
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val starter = createUploadStarter(uploadServiceFacade)

        // When
        runBlocking {
            starter.queueUploadFromSite(site).join()
        }

        // Then
        verify(uploadServiceFacade, times(draftPosts.size)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any()
        )
    }

    private fun createUploadStarter(uploadServiceFacade: UploadServiceFacade) = UploadStarter(
            context = mock(),
            postStore = postStore,
            pageStore = pageStore,
            siteStore = mock(),
            bgDispatcher = Dispatchers.Default,
            ioDispatcher = Dispatchers.IO,
            networkUtilsWrapper = createMockedNetworkUtilsWrapper(),
            connectionStatus = mock(),
            uploadServiceFacade = uploadServiceFacade,
            uploadActionUseCase = UploadActionUseCase(mock(), createMockedPostUtilsWrapper(), uploadServiceFacade),
            tracker = mock(),
            dispatcher = mock()
    )

    private companion object Fixtures {
        fun createMockedNetworkUtilsWrapper() = mock<NetworkUtilsWrapper> {
            on { isNetworkAvailable() } doReturn true
        }

        fun createMockedUploadServiceFacade() = mock<UploadServiceFacade> {
            on { isPostUploadingOrQueued(any()) } doReturn false
        }

        fun createMockedPostUtilsWrapper() = mock<PostUtilsWrapper> {
            on { isPublishable(any()) } doReturn true
            on { isPostInConflictWithRemote(any()) } doReturn false
        }

        fun createLocallyChangedPostModel() = PostModel().apply {
            setStatus(PostStatus.DRAFT.toString())
            setIsLocallyChanged(true)
            setDateLocallyChanged(DateTimeUtils.iso8601FromTimestamp(Date().time / 1000))
        }

        fun createSiteModel(): SiteModel = SiteModel().apply { setIsWPCom(true) }
    }
}
