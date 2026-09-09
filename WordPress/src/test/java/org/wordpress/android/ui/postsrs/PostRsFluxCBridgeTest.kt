package org.wordpress.android.ui.postsrs

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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

class PostRsFluxCBridgeTest {
    private val wpApiClientProvider: WpApiClientProvider = mock()
    private val postStore: PostStore = mock()
    private val postSqlUtils: PostSqlUtils = mock()
    private val postMapper: PostRsToFluxCMapper = mock()
    private val bridge = PostRsFluxCBridge(
        wpApiClientProvider, postStore, postSqlUtils, postMapper
    )

    @Test
    fun `fast path returns cached PostModel when lastModified is null`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = cachedPost()
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridge(REMOTE_ID, site)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    @Test
    fun `fast path returns cached PostModel when lastModified matches`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = cachedPost()
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridge(REMOTE_ID, site, lastModified = STAMP)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
    }

    /**
     * Without this the re-fetch would overwrite the row and lose the user's unsynced edits — the
     * likely case on WP.com sites, where FluxC actively syncs posts and holds local drafts.
     */
    @Test
    fun `stale cached post with local changes is returned instead of being overwritten`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = cachedPost().apply { setIsLocallyChanged(true) }
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached)

        val result = bridge.fetchAndBridge(REMOTE_ID, site, lastModified = NEWER_STAMP)

        assertThat(result).isSameAs(cached)
        verify(wpApiClientProvider, never()).getWpApiClient(eq(site), anyOrNull())
        verify(postSqlUtils, never()).insertOrUpdatePost(any(), any())
    }

    @Test
    fun `stale cached post without local changes is re-fetched from the network`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val cached = cachedPost().apply { setIsLocallyChanged(false) }
        val stored = PostModel().apply { setRemotePostId(REMOTE_ID) }
        val client: WpApiClient = mock()
        val rsPost = mockRsPost()
        whenever(postStore.getPostByRemotePostId(REMOTE_ID, site)).thenReturn(cached, stored)
        whenever(wpApiClientProvider.getWpApiClient(eq(site), anyOrNull())).thenReturn(client)
        whenever(client.request<PostsRequestRetrieveWithEditContextResponse>(any()))
            .thenReturn(fetchSuccess(rsPost))
        whenever(postMapper.map(eq(rsPost), eq(site)))
            .thenReturn(PostModel().apply { setRemotePostId(REMOTE_ID) })

        val result = bridge.fetchAndBridge(REMOTE_ID, site, lastModified = NEWER_STAMP)

        assertThat(result).isSameAs(stored)
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

    private fun cachedPost() = PostModel().apply {
        setRemotePostId(REMOTE_ID)
        setRemoteLastModified(STAMP)
    }

    // The fetched post is a pass-through to the mocked mapper, so its contents are never
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
