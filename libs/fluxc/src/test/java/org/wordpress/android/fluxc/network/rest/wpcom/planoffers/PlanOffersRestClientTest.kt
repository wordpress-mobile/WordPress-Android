package org.wordpress.android.fluxc.network.rest.wpcom.planoffers

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PlanOffersRestClient.PlanOffersResponse
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class PlanOffersRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>

    private lateinit var planOffersRestClient: PlanOffersRestClient

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        planOffersRestClient = PlanOffersRestClient(
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
        initRequest(Success(PLAN_OFFERS_RESPONSE))
        val payload = planOffersRestClient.fetchPlanOffers()

        Assertions.assertThat(payload).isNotNull
        Assertions.assertThat(payload.planOffers).isEqualTo(PLAN_OFFER_MODELS)
    }

    @Test
    fun `returns error on unsuccessful fetch`() = test {
        initRequest(error = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR)))
        val payload = planOffersRestClient.fetchPlanOffers()

        Assertions.assertThat(payload).isNotNull
        Assertions.assertThat(payload.planOffers).isNull()
        Assert.assertTrue(payload.isError)
    }

    private suspend fun initRequest(
        data: WPComGsonRequestBuilder.Response<PlanOffersResponse>? = null,
        error: WPComGsonNetworkError? = null
    ) {
        val response = if (error != null) Response.Error(error) else data

        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(planOffersRestClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(PlanOffersResponse::class.java),
                        eq(false),
                        any(),
                        eq(true)
                )
        ).thenReturn(response)
    }
}
