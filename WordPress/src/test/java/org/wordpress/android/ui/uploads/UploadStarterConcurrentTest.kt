package org.wordpress.android.ui.uploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.uploads.UploadFixtures.createSimpleLocallyChangedPostModel
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedNetworkUtilsWrapper
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedPostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadFixtures.createSiteModel
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedUploadServiceFacade

/**
 * Tests for structured concurrency in [UploadStarter].
 *
 * Intentionally separated from [UploadStarterTest] because it contains non-deterministic tests.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UploadStarterConcurrentTest : BaseUnitTest() {
    private val site = createSiteModel()
    private val draftPosts = listOf(
        createSimpleLocallyChangedPostModel(),
        createSimpleLocallyChangedPostModel(),
        createSimpleLocallyChangedPostModel(),
        createSimpleLocallyChangedPostModel(),
        createSimpleLocallyChangedPostModel()
    )

    private val postStore = mock<PostStore> {
        on { getPostsWithLocalChanges(eq(site)) } doReturn draftPosts
    }

    private val pageStore = mock<PageStore> {
        onBlocking { getPagesWithLocalChanges(any()) } doReturn emptyList()
    }

    @Test
    fun `it uploads local drafts concurrently`() = test {
        // Given
        val uploadServiceFacade = createMockedUploadServiceFacade()

        val starter = createUploadStarter(uploadServiceFacade)

        // When
        starter.queueUploadFromSite(site).join()

        // Then
        verify(uploadServiceFacade, times(draftPosts.size)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }

    private fun createUploadStarter(uploadServiceFacade: UploadServiceFacade) = UploadStarter(
        appContext = mock(),
        postStore = postStore,
        pageStore = pageStore,
        siteStore = mock(),
        bgDispatcher = testDispatcher(),
        ioDispatcher = testDispatcher(),
        networkUtilsWrapper = createMockedNetworkUtilsWrapper(),
        connectionStatus = mock(),
        uploadServiceFacade = uploadServiceFacade,
        uploadActionUseCase = UploadActionUseCase(mock(), createMockedPostUtilsWrapper(), uploadServiceFacade),
        tracker = mock(),
        dispatcher = mock()
    )
}
