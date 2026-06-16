package org.wordpress.android.ui.pagesrs

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.postsrs.PostRsToFluxCMapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostsRequestRetrieveWithEditContextResponse
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.WpErrorCode
import uniffi.wp_api.WpNetworkHeaderMap

class PageRsFluxCBridgeTest {
    private val wpApiClientProvider: WpApiClientProvider = mock()
    private val postStore: PostStore = mock()
    private val postSqlUtils: PostSqlUtils = mock()
    private val postMapper: PostRsToFluxCMapper = mock()
    private val bridge = PageRsFluxCBridge(
        wpApiClientProvider, postStore, postSqlUtils, postMapper
    )

    @Test
    fun `fast path returns cached PostModel when isPage and lastModified is null`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = PostModel().apply {
            setRemotePostId(REMOTE_ID)
            setIsPage(true)
            setRemoteLastModified("2026-06-01T00:00:00Z")
        }
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridge(REMOTE_ID, site)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `fast path returns cached PostModel when lastModified matches`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val stamp = "2026-06-01T00:00:00Z"
        val cached = PostModel().apply {
            setRemotePostId(REMOTE_ID)
            setIsPage(true)
            setRemoteLastModified(stamp)
        }
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridge(REMOTE_ID, site, lastModified = stamp)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `stale cached page with local changes short-circuits to avoid wasted fetch`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = PostModel().apply {
            setRemotePostId(REMOTE_ID)
            setIsPage(true)
            setRemoteLastModified("2026-06-01T00:00:00Z")
            setIsLocallyChanged(true)
        }
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridge(
            REMOTE_ID, site, lastModified = "2026-06-05T00:00:00Z"
        )

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `fast path is skipped when cached row is not a page`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = PostModel().apply {
            setRemotePostId(REMOTE_ID)
            setIsPage(false)
        }
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)
        whenever(wpApiClientProvider.getWpApiClient(any(), anyOrNull()))
            .thenThrow(IllegalStateException("network not stubbed"))

        runCatching { bridge.fetchAndBridge(REMOTE_ID, site) }

        verify(wpApiClientProvider).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `slow path fetches, inserts as a page, and returns the re-read model`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val client: WpApiClient = mock()
        val rsPage = mockRsPage()
        val mapped = PostModel().apply { setRemotePostId(REMOTE_ID) }
        val stored = PostModel().apply {
            setRemotePostId(REMOTE_ID)
            setIsPage(true)
        }
        // No cache on the first lookup; the re-read after insert returns the stored row.
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(null, stored)
        whenever(wpApiClientProvider.getWpApiClient(eq(site), anyOrNull())).thenReturn(client)
        whenever(client.request<PostsRequestRetrieveWithEditContextResponse>(any()))
            .thenReturn(fetchSuccess(rsPage))
        whenever(postMapper.map(eq(rsPage), eq(site))).thenReturn(mapped)

        val result = bridge.fetchAndBridge(REMOTE_ID, site)

        // The bridge returns the re-read row, not the mapper output.
        assertThat(result).isSameAs(stored)
        verify(wpApiClientProvider).getWpApiClient(eq(site), anyOrNull())
        // The bridge flips isPage on the mapper output before inserting.
        verify(postSqlUtils).insertOrUpdatePost(argThat { isPage }, eq(false))
    }

    @Test
    fun `stale cached page without local changes is re-fetched from the network`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = PostModel().apply {
            setRemotePostId(REMOTE_ID)
            setIsPage(true)
            setRemoteLastModified("2026-06-01T00:00:00Z")
            setIsLocallyChanged(false)
        }
        val client: WpApiClient = mock()
        val rsPage = mockRsPage()
        val mapped = PostModel().apply { setRemotePostId(REMOTE_ID) }
        val stored = PostModel().apply {
            setRemotePostId(REMOTE_ID)
            setIsPage(true)
        }
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached, stored)
        whenever(wpApiClientProvider.getWpApiClient(eq(site), anyOrNull())).thenReturn(client)
        whenever(client.request<PostsRequestRetrieveWithEditContextResponse>(any()))
            .thenReturn(fetchSuccess(rsPage))
        whenever(postMapper.map(eq(rsPage), eq(site))).thenReturn(mapped)

        val result = bridge.fetchAndBridge(REMOTE_ID, site, lastModified = "2026-06-05T00:00:00Z")

        assertThat(result).isSameAs(stored)
        verify(wpApiClientProvider).getWpApiClient(eq(site), anyOrNull())
        verify(postSqlUtils).insertOrUpdatePost(any(), eq(false))
    }

    @Test
    fun `slow path throws with the server error message and does not insert`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val client: WpApiClient = mock()
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(null)
        whenever(wpApiClientProvider.getWpApiClient(eq(site), anyOrNull())).thenReturn(client)
        whenever(client.request<PostsRequestRetrieveWithEditContextResponse>(any()))
            .thenReturn(fetchError("boom"))

        val error = runCatching { bridge.fetchAndBridge(REMOTE_ID, site) }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("boom")
        verify(postSqlUtils, never()).insertOrUpdatePost(any(), any())
    }

    // The fetched page is a pass-through to the mocked mapper, so its contents are never
    // read; mocking it avoids building the ~29-field data class by hand.
    @Suppress("DoNotMockDataClass")
    private fun mockRsPage(): AnyPostWithEditContext = mock()

    private fun fetchSuccess(page: AnyPostWithEditContext) =
        WpRequestResult.Success(
            PostsRequestRetrieveWithEditContextResponse(page, mock<WpNetworkHeaderMap>())
        )

    private fun fetchError(message: String) =
        WpRequestResult.WpError<PostsRequestRetrieveWithEditContextResponse>(
            errorCode = WpErrorCode.InvalidParam(),
            errorMessage = message,
            statusCode = 400.toUInt(),
            response = "",
            requestUrl = "https://example.com",
            requestMethod = RequestMethod.GET,
        )

    companion object {
        private const val REMOTE_ID = 100L
    }
}
