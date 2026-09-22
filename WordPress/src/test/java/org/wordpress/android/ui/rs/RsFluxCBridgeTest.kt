package org.wordpress.android.ui.rs

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
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostsRequestRetrieveWithEditContextResponse
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.WpErrorCode
import uniffi.wp_api.WpNetworkHeaderMap

class RsFluxCBridgeTest {
    private val wpApiClientProvider: WpApiClientProvider = mock()
    private val postStore: PostStore = mock()
    private val postSqlUtils: PostSqlUtils = mock()
    private val mapper: RsToFluxCMapper = mock()
    private val bridge = RsFluxCBridge(wpApiClientProvider, postStore, postSqlUtils, mapper)

    private val site = SiteModel().apply { id = 1 }

    // region fast path

    @Test
    fun `fast path returns the cached post when lastModified is null`() = runTest {
        val cached = cachedItem(isPage = false)
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridgePost(REMOTE_ID, site)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `fast path returns the cached page when lastModified is null`() = runTest {
        val cached = cachedItem(isPage = true)
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridgePage(REMOTE_ID, site)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `fast path returns the cached item when lastModified matches`() = runTest {
        val cached = cachedItem(isPage = true)
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridgePage(REMOTE_ID, site, lastModified = STAMP)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `fast path is skipped when the cached row is a post and a page was asked for`() = runTest {
        val cached = cachedItem(isPage = false)
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)
        whenever(wpApiClientProvider.getWpApiClient(any(), anyOrNull()))
            .thenThrow(IllegalStateException("network not stubbed"))

        runCatching { bridge.fetchAndBridgePage(REMOTE_ID, site) }

        verify(wpApiClientProvider).getWpApiClient(eq(site), anyOrNull())
    }

    /**
     * The mirror of the case above, and the one behaviour this merge changes: the posts bridge
     * used to accept a cached row whatever its kind, so a row stored as a page would have opened
     * in the post editor. Remote ids do not collide across types, so it should never fire.
     */
    @Test
    fun `fast path is skipped when the cached row is a page and a post was asked for`() = runTest {
        val cached = cachedItem(isPage = true)
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)
        whenever(wpApiClientProvider.getWpApiClient(any(), anyOrNull()))
            .thenThrow(IllegalStateException("network not stubbed"))

        runCatching { bridge.fetchAndBridgePost(REMOTE_ID, site) }

        verify(wpApiClientProvider).getWpApiClient(eq(site), anyOrNull())
    }

    /**
     * Without this the re-fetch would overwrite the row and lose the user's unsynced edits - the
     * likely case on WP.com sites, where FluxC actively syncs posts and holds local drafts.
     */
    @Test
    fun `a stale cached item with local changes is returned instead of being overwritten`() = runTest {
        val cached = cachedItem(isPage = false).apply { setIsLocallyChanged(true) }
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridgePost(REMOTE_ID, site, lastModified = NEWER_STAMP)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
        verify(postSqlUtils, never()).insertOrUpdatePost(any(), any())
    }

    // endregion

    // region slow path

    @Test
    fun `slow path fetches, inserts as a page, and returns the re-read model`() = runTest {
        val stored = cachedItem(isPage = true)
        // No cache on the first lookup; the re-read after insert returns the stored row.
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(null, stored)
        stubFetch()

        val result = bridge.fetchAndBridgePage(REMOTE_ID, site)

        // The bridge returns the re-read row, not the mapper output.
        assertThat(result).isSameAs(stored)
        // The bridge flips isPage on the mapper output before inserting.
        verify(postSqlUtils).insertOrUpdatePost(argThat { isPage }, eq(false))
    }

    @Test
    fun `slow path inserts a post with isPage unset`() = runTest {
        val stored = cachedItem(isPage = false)
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(null, stored)
        stubFetch()

        val result = bridge.fetchAndBridgePost(REMOTE_ID, site)

        assertThat(result).isSameAs(stored)
        verify(postSqlUtils).insertOrUpdatePost(argThat { !isPage }, eq(false))
    }

    @Test
    fun `a stale cached item without local changes is re-fetched from the network`() = runTest {
        val cached = cachedItem(isPage = true).apply { setIsLocallyChanged(false) }
        val stored = cachedItem(isPage = true)
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached, stored)
        stubFetch()

        val result = bridge.fetchAndBridgePage(REMOTE_ID, site, lastModified = NEWER_STAMP)

        assertThat(result).isSameAs(stored)
        verify(wpApiClientProvider).getWpApiClient(eq(site), anyOrNull())
        verify(postSqlUtils).insertOrUpdatePost(any(), eq(false))
    }

    @Test
    fun `slow path throws with the server error message and does not insert`() = runTest {
        val client: WpApiClient = mock()
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(null)
        whenever(wpApiClientProvider.getWpApiClient(eq(site), anyOrNull())).thenReturn(client)
        whenever(client.request<PostsRequestRetrieveWithEditContextResponse>(any()))
            .thenReturn(fetchError("boom"))

        val error = runCatching { bridge.fetchAndBridgePost(REMOTE_ID, site) }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("boom")
        verify(postSqlUtils, never()).insertOrUpdatePost(any(), any())
    }

    // endregion

    private fun cachedItem(isPage: Boolean) = PostModel().apply {
        setRemotePostId(REMOTE_ID)
        setRemoteLastModified(STAMP)
        setIsPage(isPage)
    }

    private suspend fun stubFetch() {
        val client: WpApiClient = mock()
        val fetched = mockRsPost()
        whenever(wpApiClientProvider.getWpApiClient(eq(site), anyOrNull())).thenReturn(client)
        whenever(client.request<PostsRequestRetrieveWithEditContextResponse>(any()))
            .thenReturn(fetchSuccess(fetched))
        whenever(mapper.map(eq(fetched), eq(site)))
            .thenReturn(PostModel().apply { setRemotePostId(REMOTE_ID) })
    }

    // The fetched item is a pass-through to the mocked mapper, so its contents are never
    // read; mocking it avoids building the ~29-field data class by hand.
    @Suppress("DoNotMockDataClass")
    private fun mockRsPost(): AnyPostWithEditContext = mock()

    private fun fetchSuccess(post: AnyPostWithEditContext) =
        WpRequestResult.Success(
            PostsRequestRetrieveWithEditContextResponse(post, mock<WpNetworkHeaderMap>())
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
        private const val STAMP = "2026-06-01T00:00:00Z"
        private const val NEWER_STAMP = "2026-06-05T00:00:00Z"
    }
}
