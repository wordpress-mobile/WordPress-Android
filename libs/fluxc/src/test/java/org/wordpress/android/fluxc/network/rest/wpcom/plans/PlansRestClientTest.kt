package org.wordpress.android.fluxc.network.rest.wpcom.plans

import com.android.volley.RequestQueue
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient.PlansResponse
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class PostRestClient {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>

    private lateinit var plansRestClient: PlansRestClient
    private val username = "John Smith"
    private val password = "password123"

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        plansRestClient = PlansRestClient(
                dispatcher,
                wpComGsonRequestBuilder,
                null,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `returns plans on successful fetch`() = test {
        initRequest(Success(PLANS_RESPONSE))
        val payload = plansRestClient.fetchPlans()

        Assertions.assertThat(payload).isNotNull
        Assertions.assertThat(payload.plans).isEqualTo(PLAN_MODELS)
    }

    @Test
    fun `returns error on unsuccessful fetch`() = test {
        initRequest(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))
        val payload = plansRestClient.fetchPlans()

        Assertions.assertThat(payload).isNotNull
        Assert.assertTrue(payload.isError)
    }

    private suspend fun initRequest(
        data: WPComGsonRequestBuilder.Response<PlansResponse>? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val response = if (error != null) Response.Error(error) else data

        whenever(site.username).thenReturn(username)
        whenever(site.password).thenReturn(password)
        whenever(site.url).thenReturn(WPCOMV2.plans.mobile.url)
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(plansRestClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(PlansResponse::class.java),
                        eq(true),
                        any(),
                        eq(true)
                )
        ).thenReturn(response)
    }
}