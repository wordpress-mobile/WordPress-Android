package org.wordpress.android.fluxc.store

import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.site.XPostsRestClient
import org.wordpress.android.fluxc.persistence.XPostsSqlUtils
import org.wordpress.android.fluxc.store.XPostsSource.DB
import org.wordpress.android.fluxc.store.XPostsSource.REST_API
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
            verify(sqlUtils).insertOrUpdateXPost(apiData.toList(), site)
        }

        val expected = FetchXpostsResult(apiData.toList(), REST_API)
        assertEquals(expected, result)
    }

    @Test
    fun `fetchXPosts checks db if response is error`() = test {
        val dBData = listOf(mock<XPostSiteModel>())
        val restResponse = WPComGsonRequestBuilder.Response.Error<Array<XPostSiteModel>>(mock())
        whenever(restClient.fetch(site)).thenReturn(restResponse)
        whenever(sqlUtils.selectXPostsForSite(site)).thenReturn(dBData)

        val result = store.fetchXPosts(site)

        inOrder(restClient, sqlUtils) {
            verify(restClient).fetch(site)
            verify(sqlUtils).selectXPostsForSite(site)
        }

        val expected = FetchXpostsResult(dBData, DB)
        assertEquals(expected, result)
    }
}
