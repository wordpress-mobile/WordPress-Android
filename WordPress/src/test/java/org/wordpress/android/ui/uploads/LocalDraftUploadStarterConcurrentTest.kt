package org.wordpress.android.ui.uploads

import android.arch.core.executor.testing.InstantTaskExecutorRule
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
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.util.NetworkUtilsWrapper

/**
 * Tests for structured concurrency in [LocalDraftUploadStarter].
 */
@RunWith(MockitoJUnitRunner::class)
class LocalDraftUploadStarterConcurrentTest {
    @get:Rule val rule = InstantTaskExecutorRule()

    private val site = SiteModel()
    private val posts = listOf(
            PostModel(),
            PostModel(),
            PostModel(),
            PostModel(),
            PostModel()
    )

    private val postStore = mock<PostStore> {
        on { getLocalDraftPosts(eq(site)) } doReturn posts
    }

    @Test
    fun `it uploads local drafts concurrently`() {
        // Given
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val starter = createLocalDraftUploadStarter(uploadServiceFacade)

        // When
        runBlocking {
            starter.queueUploadFromSite(site).join()
        }

        // Then
        verify(uploadServiceFacade, times(posts.size)).uploadPost(
                context = any(),
                post = any(),
                trackAnalytics = any(),
                publish = any(),
                isRetry = eq(true)
        )
    }

    private fun createLocalDraftUploadStarter(uploadServiceFacade: UploadServiceFacade) = LocalDraftUploadStarter(
            context = mock(),
            postStore = postStore,
            siteStore = mock(),
            bgDispatcher = Dispatchers.Default,
            ioDispatcher = Dispatchers.IO,
            networkUtilsWrapper = createMockedNetworkUtilsWrapper(),
            connectionStatus = mock(),
            uploadServiceFacade = uploadServiceFacade
    )

    private companion object Fixtures {
        fun createMockedNetworkUtilsWrapper() = mock<NetworkUtilsWrapper> {
            on { isNetworkAvailable() } doReturn true
        }

        fun createMockedUploadServiceFacade() = mock<UploadServiceFacade> {
            on { isPostUploadingOrQueued(any()) } doReturn false
        }
    }
}
