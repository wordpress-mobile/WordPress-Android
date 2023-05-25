package org.wordpress.android.ui.uploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
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
import org.wordpress.android.ui.uploads.UploadFixtures.createLocallyChangedPostModel
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedNetworkUtilsWrapper
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedPostUtilsWrapper
import org.wordpress.android.ui.uploads.UploadFixtures.createMockedUploadServiceFacade
import org.wordpress.android.ui.uploads.UploadFixtures.createSiteModel
import org.wordpress.android.ui.uploads.UploadFixtures.resetTestPostIdIndex

/**
 * Tests for structured concurrency in [UploadStarter].
 *
 * Intentionally separated from [UploadStarterTest] because it contains non-deterministic tests.
 */
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UploadStarterConcurrentTest : BaseUnitTest() {
    private lateinit var starter: UploadStarter
    private lateinit var uploadServiceFacade: UploadServiceFacade

    private val site = createSiteModel()
    private val draftPosts = listOf(
        createLocallyChangedPostModel(),
        createLocallyChangedPostModel(),
        createLocallyChangedPostModel(),
        createLocallyChangedPostModel(),
        createLocallyChangedPostModel(),
    )

    private val postStore = mock<PostStore> { on { getPostsWithLocalChanges(eq(site)) } doReturn draftPosts }
    private val pageStore = mock<PageStore> { onBlocking { getPagesWithLocalChanges(any()) } doReturn emptyList() }

    @Before
    fun setUp() {
        resetTestPostIdIndex()
        uploadServiceFacade = createMockedUploadServiceFacade()
        starter = UploadStarter(
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
            dispatcher = mock(),
            mutex = mock(),
        )
    }

    @Test
    fun `it uploads local drafts concurrently`() = test {
        starter.queueUploadFromSite(site).join()

        verify(uploadServiceFacade, times(draftPosts.size)).uploadPost(
            context = any(),
            post = any(),
            trackAnalytics = any()
        )
    }
}
