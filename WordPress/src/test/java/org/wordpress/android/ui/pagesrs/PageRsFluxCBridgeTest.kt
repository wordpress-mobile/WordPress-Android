package org.wordpress.android.ui.pagesrs

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
import org.wordpress.android.ui.postsrs.PostRsToFluxCMapper

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

    companion object {
        private const val REMOTE_ID = 100L
    }
}
