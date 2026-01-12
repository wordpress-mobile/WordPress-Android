package org.wordpress.android.fluxc.network.rest.wpcom.media

import android.content.Context
import com.android.volley.RequestQueue
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.utils.MimeType

@RunWith(RobolectricTestRunner::class)
class MediaRestClientTest {
    private val context: Context = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val okHttpClient: OkHttpClient = mock()
    private val accessToken: AccessToken = mock()
    private val userAgent: UserAgent = mock()
    private val mediaResponseUtils: MediaResponseUtils = mock()

    private lateinit var mediaRestClient: MediaRestClient

    @Before
    fun setUp() {
        mediaRestClient = MediaRestClient(
            context,
            dispatcher,
            requestQueue,
            okHttpClient,
            accessToken,
            userAgent,
            mediaResponseUtils
        )
    }

    @Test
    fun `fetchMediaList includes search parameter when searchTerm is provided`() {
        val site = SiteModel().apply { siteId = 123L }
        val searchTerm = "test query"

        mediaRestClient.fetchMediaList(site, 20, 0, MimeType.Type.IMAGE, searchTerm)

        verify(requestQueue).add(argThat<WPComGsonRequest<*>> { request ->
            request.url.contains("search=test%20query") ||
                request.url.contains("search=test+query")
        })
    }

    @Test
    fun `fetchMediaList does not include search parameter when searchTerm is null`() {
        val site = SiteModel().apply { siteId = 123L }

        mediaRestClient.fetchMediaList(site, 20, 0, MimeType.Type.IMAGE, null)

        verify(requestQueue).add(argThat<WPComGsonRequest<*>> { request ->
            !request.url.contains("search=")
        })
    }

    @Test
    fun `fetchMediaList does not include search parameter when searchTerm is empty`() {
        val site = SiteModel().apply { siteId = 123L }

        mediaRestClient.fetchMediaList(site, 20, 0, MimeType.Type.IMAGE, "")

        verify(requestQueue).add(argThat<WPComGsonRequest<*>> { request ->
            !request.url.contains("search=")
        })
    }

    @Test
    fun `fetchMediaList includes both mimeType and search parameters`() {
        val site = SiteModel().apply { siteId = 123L }
        val searchTerm = "photo"

        mediaRestClient.fetchMediaList(site, 20, 0, MimeType.Type.IMAGE, searchTerm)

        verify(requestQueue).add(argThat<WPComGsonRequest<*>> { request ->
            request.url.contains("mime_type=image") &&
                (request.url.contains("search=photo"))
        })
    }
}
