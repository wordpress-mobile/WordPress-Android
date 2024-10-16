package org.wordpress.android.fluxc.store

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.site.XPostsRestClient
import org.wordpress.android.fluxc.persistence.XPostsSqlUtils
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class XPostsStoreTest {
    private lateinit var store: XPostsStore
    private val restClient = mock<XPostsRestClient>()
    private val sqlUtils = mock<XPostsSqlUtils>()
    private val site = mock<SiteModel>()

    @Before
    fun setup() {
        store = XPostsStore(initCoroutineEngine(), restClient, sqlUtils)
    }

    @Test
    fun `fetchXPosts handles successful result`() = test {
        val apiData = arrayOf(mock<XPostSiteModel>())
        val restResponse = WPComGsonRequestBuilder.Response.Success(apiData)
        whenever(restClient.fetch(site)).thenReturn(restResponse)

        val result = store.fetchXPosts(site)

        inOrder(restClient, sqlUtils) {
            verify(restClient).fetch(site)
            verify(sqlUtils).setXPostsForSite(apiData.toList(), site)
        }

        val expected = XPostsResult.apiResult(apiData.toList())
        assertEquals(expected, result)
    }

    @Test
    fun `fetchXPosts handles unauthorized`() = test {
        stubResponseWithError("unauthorized")

        val result = store.fetchXPosts(site)

        inOrder(restClient, sqlUtils) {
            verify(restClient).fetch(site)
            verify(sqlUtils).persistNoXpostsForSite(site)
        }

        val expected = XPostsResult.apiResult(emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun `fetchXPosts handles o2_disabled`() = test {
        stubResponseWithError("xposts_require_o2_enabled")

        val result = store.fetchXPosts(site)

        inOrder(restClient, sqlUtils) {
            verify(restClient).fetch(site)
            verify(sqlUtils).persistNoXpostsForSite(site)
        }

        val expected = XPostsResult.apiResult(emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun `fetchXPosts handles unexpected errors when db returns null`() = test {
        stubResponseWithError("an_unexpected_error")
        whenever(sqlUtils.selectXPostsForSite(site)).thenReturn(null)

        val result = store.fetchXPosts(site)

        assertEquals(XPostsResult.Unknown, result)
    }

    @Test
    fun `fetchXPosts handles unexpected errors when db returns list`() = test {
        stubResponseWithError("an_unexpected_error")
        val xPosts = listOf(mock<XPostSiteModel>())
        whenever(sqlUtils.selectXPostsForSite(site)).thenReturn(xPosts)

        val result = store.fetchXPosts(site)

        val expected = XPostsResult.dbResult(xPosts)
        assertEquals(expected, result)
    }

    @Test
    fun `savedXPosts returns Unknown when db returns null`() = test {
        whenever(sqlUtils.selectXPostsForSite(site)).thenReturn(null)
        val result = store.getXPostsFromDb(site)
        assertEquals(XPostsResult.Unknown, result)
    }

    @Test
    fun `savedXPosts returns dbResult when db has empty List`() = test {
        whenever(sqlUtils.selectXPostsForSite(site)).thenReturn(emptyList())

        val result = store.getXPostsFromDb(site)

        val expected = XPostsResult.dbResult(emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun `savedXPosts returns dbResult when db has xposts`() = test {
        val xPosts = listOf(mock<XPostSiteModel>())
        whenever(sqlUtils.selectXPostsForSite(site)).thenReturn(xPosts)

        val result = store.getXPostsFromDb(site)

        val expected = XPostsResult.dbResult(xPosts)
        assertEquals(expected, result)
    }

    private suspend fun stubResponseWithError(apiError: String) {
        val error = mock<WPComGsonRequest.WPComGsonNetworkError>()
        error.apiError = apiError
        val restResponse = WPComGsonRequestBuilder.Response.Error<Array<XPostSiteModel>>(error)
        whenever(restClient.fetch(site)).thenReturn(restResponse)
    }
}
