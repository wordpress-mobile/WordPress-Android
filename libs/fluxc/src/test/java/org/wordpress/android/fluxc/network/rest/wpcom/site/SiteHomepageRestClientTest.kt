package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.MapAssert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.Page
import org.wordpress.android.fluxc.model.SiteHomepageSettings.Posts
import org.wordpress.android.fluxc.model.SiteHomepageSettingsMapper
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient.UpdateHomepageResponse
import org.wordpress.android.fluxc.store.SiteOptionsStore.SiteOptionsErrorType.API_ERROR
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class SiteHomepageRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var siteHomepageSettingsMapper: SiteHomepageSettingsMapper
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
                siteHomepageSettingsMapper,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `returns success when setting homepage to page`() = test {
        val pageSettings = Page(1, 2)
        val response = UpdateHomepageResponse(true, pageSettings.pageOnFrontId, pageSettings.pageForPostsId)
        val expectedParams = mapOf(
                "is_page_on_front" to "page",
                "page_on_front_id" to pageSettings.pageOnFrontId.toString(),
                "page_for_posts_id" to pageSettings.pageForPostsId.toString()
        )

        testSuccessResponse(response, pageSettings, expectedParams)
    }

    @Test
    fun `returns success when setting homepage to posts`() = test {
        val postsSettings = Posts
        val response = UpdateHomepageResponse(false, null, null)
        val expectedParams = mapOf("is_page_on_front" to "posts")

        testSuccessResponse(response, postsSettings, expectedParams)
    }

    private suspend fun testSuccessResponse(
        response: UpdateHomepageResponse,
        homepageSettings: SiteHomepageSettings,
        expectedParams: Map<String, String>
    ): MapAssert<String, String>? {
        whenever(siteHomepageSettingsMapper.map(response)).thenReturn(homepageSettings)

        initHomepageResponse(response)

        val responseModel = restClient.updateHomepage(site, homepageSettings)

        assertThat(responseModel.homepageSettings).isEqualTo(homepageSettings)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12/homepage/")
        return assertThat(paramsCaptor.lastValue).isEqualTo(expectedParams)
    }

    @Test
    fun `returns error when API call fails`() = test {
        testErrorResponse(Page(1, 2))
    }

    private suspend fun testErrorResponse(homepageSettings: SiteHomepageSettings) {
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

        val responseModel = restClient.updateHomepage(site, homepageSettings)

        assertThat(responseModel.error).isNotNull()
        assertThat(responseModel.error.type).isEqualTo(API_ERROR)
        assertThat(responseModel.error.message).isEqualTo(errorMessage)
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
                        paramsCaptor.capture(),
                        eq(kclass)
                )
        ).thenReturn(response)
        whenever(site.siteId).thenReturn(siteId)
        return response
    }
}
