package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class SiteRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent
    @Mock private lateinit var appSecrets: AppSecrets
    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>
    private lateinit var restClient: SiteRestClient
    private val siteId: Long = 12

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        restClient = SiteRestClient(
                null,
                dispatcher,
                requestQueue,
                appSecrets,
                wpComGsonRequestBuilder,
                accessToken,
                userAgent
        )
        whenever(site.siteId).thenReturn(siteId)
    }

    @Test
    fun `returns success when setting homepage to page`() = test {
        val response = SiteWPComRestResponse()
        response.ID = siteId
        val name = "Updated name"
        response.name = name
        response.URL = "site.com"

        initResponse(response)

        val responseModel = restClient.fetchSite(site)
        assertThat(responseModel.name).isEqualTo(name)
        assertThat(responseModel.siteId).isEqualTo(siteId)
        assertThat(urlCaptor.lastValue)
                .isEqualTo("https://public-api.wordpress.com/rest/v1.1/sites/12")
        assertThat(paramsCaptor.lastValue).isEqualTo(
                mapOf(
                        "fields" to "ID,URL,name,description,jetpack," +
                                "visible,is_private,options,plan,capabilities,quota,icon,meta"
                )
        )
    }

    @Test
    fun `returns error when API call fails`() = test {
        val errorMessage = "message"
        initResponse(
                error = WPComGsonNetworkError(
                        BaseNetworkError(
                                GenericErrorType.NETWORK_ERROR,
                                errorMessage,
                                VolleyError(errorMessage)
                        )
                )
        )
        val errorResponse = restClient.fetchSite(site)

        assertThat(errorResponse.error).isNotNull()
        assertThat(errorResponse.error.type).isEqualTo(GenericErrorType.NETWORK_ERROR)
        assertThat(errorResponse.error.message).isEqualTo(errorMessage)
    }

    private suspend fun initResponse(
        data: SiteWPComRestResponse? = null,
        error: WPComGsonNetworkError? = null
    ): Response<SiteWPComRestResponse> {
        return initResponse(SiteWPComRestResponse::class.java, data ?: mock(), error)
    }

    private suspend fun <T> initResponse(
        kclass: Class<T>,
        data: T,
        error: WPComGsonNetworkError? = null
    ): Response<T> {
        val response = if (error != null) Response.Error<T>(error) else Success(data)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(restClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(kclass),
                        any(),
                        any(),
                        any()

                )
        ).thenReturn(response)
        return response
    }
}
