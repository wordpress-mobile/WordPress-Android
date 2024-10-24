package org.wordpress.android.fluxc.store

import android.net.Uri
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.discovery.DiscoveryWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.NonceRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPComRestClient
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class ReactNativeStoreWpComTest {
    private val wpComRestClient = mock<ReactNativeWPComRestClient>()
    private val discoveryWPAPIRestClient = mock<DiscoveryWPAPIRestClient>()
    private val nonceRestClient = mock<NonceRestClient>()

    private lateinit var store: ReactNativeStore

    @Before
    fun setup() {
        store = ReactNativeStore(
                wpComRestClient,
                mock(),
                nonceRestClient,
                discoveryWPAPIRestClient,
                TestSiteSqlUtils.siteSqlUtils,
                initCoroutineEngine()
        )
    }

    @Test
    fun `makes call to WPcom`() = test {
        val expectedResponse = mock<ReactNativeFetchResponse>()

        val site = mock<SiteModel>()
        whenever(site.siteId).thenReturn(123456L)
        whenever(site.isUsingWpComRestApi).thenReturn(true)

        val expectedUrl = "https://public-api.wordpress.com/wp/v2/sites/${site.siteId}/media"
        whenever(wpComRestClient.getRequest(expectedUrl, mapOf("paramKey" to "paramValue"), ::Success, ::Error))
                .thenReturn(expectedResponse)

        val actualResponse = store.executeGetRequest(site, "/wp/v2/media?paramKey=paramValue")
        assertEquals(expectedResponse, actualResponse)
    }

    @Test
    fun `makes POST request to WPCOM`() = test {
        val expectedResponse = mock<ReactNativeFetchResponse>()

        val site = mock<SiteModel>()
        whenever(site.siteId).thenReturn(123456L)
        whenever(site.isUsingWpComRestApi).thenReturn(true)

        val expectedUrl = "https://public-api.wordpress.com/wp/v2/sites/${site.siteId}/media/100"
        whenever(wpComRestClient.postRequest(
            expectedUrl,
            mapOf("paramKey" to "paramValue"),
            mapOf("title" to "newTitle"),
            ::Success, ::Error))
            .thenReturn(expectedResponse)

        val actualResponse = store.executePostRequest(
            site,
            "/wp/v2/media/100?paramKey=paramValue",
            mapOf("title" to "newTitle"))
        assertEquals(expectedResponse, actualResponse)
    }

    @Test
    fun `handles failure to parse path`() = test {
        val mockUri = mock<Uri>()
        assertNull(mockUri.path, "path must be null to represent failure to parse the path in this test")
        val uriParser = { _: String -> mockUri }

        store = ReactNativeStore(
                wpComRestClient,
                mock(),
                nonceRestClient,
                discoveryWPAPIRestClient,
                TestSiteSqlUtils.siteSqlUtils,
                initCoroutineEngine(),
                uriParser = uriParser)

        val response = store.executeGetRequest(mock(), "")
        val errorType = (response as? Error)?.error?.type
        assertEquals(UNKNOWN, errorType)
    }
}
