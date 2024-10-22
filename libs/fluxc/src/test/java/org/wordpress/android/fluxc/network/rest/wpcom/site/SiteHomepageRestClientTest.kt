package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.MapAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.Posts
import org.wordpress.android.fluxc.model.SiteHomepageSettings.StaticPage
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient.UpdateHomepageResponse
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class SiteHomepageRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: SiteHomepageRestClient
    private val siteId: Long = 12

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = SiteHomepageRestClient(
                dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `returns success when setting homepage to page`() = test {
        val pageSettings = StaticPage(1, 2)
        val isPageOnFront = true
        val response = UpdateHomepageResponse(isPageOnFront, pageSettings.pageOnFrontId, pageSettings.pageForPostsId)
        val expectedParams = mapOf(
                "is_page_on_front" to isPageOnFront.toString(),
                "page_on_front_id" to pageSettings.pageOnFrontId.toString(),
                "page_for_posts_id" to pageSettings.pageForPostsId.toString()
        )

        testSuccessResponse(response, pageSettings, expectedParams)
    }

    @Test
    fun `does not add page on front parameter when it is missing`() = test {
        val pageSettings = StaticPage(1, -1)
        val isPageOnFront = true
        val response = UpdateHomepageResponse(isPageOnFront, pageSettings.pageOnFrontId, pageSettings.pageForPostsId)
        val expectedParams = mapOf(
                "is_page_on_front" to isPageOnFront.toString(),
                "page_for_posts_id" to pageSettings.pageForPostsId.toString()
        )

        testSuccessResponse(response, pageSettings, expectedParams)
    }

    @Test
    fun `does not add page for posts parameter when it is missing`() = test {
        val pageSettings = StaticPage(-1, 2)
        val isPageOnFront = true
        val response = UpdateHomepageResponse(isPageOnFront, pageSettings.pageOnFrontId, pageSettings.pageForPostsId)
        val expectedParams = mapOf(
                "is_page_on_front" to isPageOnFront.toString(),
                "page_on_front_id" to pageSettings.pageOnFrontId.toString()
        )

        testSuccessResponse(response, pageSettings, expectedParams)
    }

    @Test
    fun `returns success when setting homepage to posts`() = test {
        val postsSettings = Posts
        val isPageOnFront = false
        val response = UpdateHomepageResponse(isPageOnFront, null, null)
        val expectedParams = mapOf("is_page_on_front" to isPageOnFront.toString())

        testSuccessResponse(response, postsSettings, expectedParams)
    }

    private suspend fun testSuccessResponse(
        response: UpdateHomepageResponse,
        homepageSettings: SiteHomepageSettings,
        expectedParams: Map<String, String>
    ): MapAssert<String, String>? {
        initHomepageResponse(response)

        val responseModel = restClient.updateHomepage(site, homepageSettings)

        assertThat(responseModel).isEqualTo(Success(response))
        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12/homepage/")
        return assertThat(paramsCaptor.lastValue).isEqualTo(expectedParams)
    }

    @Test
    fun `returns error when API call fails`() = test {
        val errorMessage = "message"
        initHomepageResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )
        val response = restClient.updateHomepage(site, StaticPage(1, 2))
        val errorResponse = response as Error<UpdateHomepageResponse>
        assertThat(errorResponse.error).isNotNull()
        assertThat(errorResponse.error.type).isEqualTo(NETWORK_ERROR)
        assertThat(errorResponse.error.message).isEqualTo(errorMessage)
        Unit
    }

    private suspend fun initHomepageResponse(
        data: UpdateHomepageResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<UpdateHomepageResponse> {
        return initResponse(UpdateHomepageResponse::class.java, data ?: mock(), error)
    }

    private suspend fun <T> initResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error<T>(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncPostRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        eq(null),
                        paramsCaptor.capture(),
                        eq(kclass),
                        isNull(),
                        anyOrNull(),
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }
}
