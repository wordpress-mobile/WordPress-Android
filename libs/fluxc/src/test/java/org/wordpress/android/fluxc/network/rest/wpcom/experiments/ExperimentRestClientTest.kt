package org.wordpress.android.fluxc.network.rest.wpcom.experiments

import com.android.volley.RequestQueue
import org.assertj.core.api.Assertions.assertThat
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
import org.wordpress.android.fluxc.model.experiments.AssignmentsModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Error
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.network.rest.wpcom.experiments.ExperimentRestClient.Companion.DEFAULT_VERSION
import org.wordpress.android.fluxc.network.rest.wpcom.experiments.ExperimentRestClient.FetchAssignmentsResponse
import org.wordpress.android.fluxc.store.ExperimentStore.FetchedAssignmentsPayload
import org.wordpress.android.fluxc.store.ExperimentStore.Platform.CALYPSO
import org.wordpress.android.fluxc.test

@RunWith(MockitoJUnitRunner::class)
class ExperimentRestClientTest {
    @Mock private lateinit var wpComGsonRequestBuilder: WPComGsonRequestBuilder
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var requestQueue: RequestQueue
    @Mock private lateinit var accessToken: AccessToken
    @Mock private lateinit var userAgent: UserAgent

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var paramsCaptor: KArgumentCaptor<Map<String, String>>

    private lateinit var experimentRestClient: ExperimentRestClient

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        paramsCaptor = argumentCaptor()
        experimentRestClient = ExperimentRestClient(
                wpComGsonRequestBuilder,
                null,
                dispatcher,
                requestQueue,
                accessToken,
                userAgent
        )
    }

    @Test
    fun `calls correct url with default values`() = test {
        initRequest(Success(successfulResponse))

        experimentRestClient.fetchAssignments(defaultPlatform, emptyList())

        val expectedUrl = "$EXPERIMENTS_ENDPOINT/$DEFAULT_VERSION/assignments/${defaultPlatform.value}/"
        val expectedParams = mapOf(
                "experiment_names" to "",
                "anon_id" to ""
        )

        assertThat(urlCaptor.lastValue).isEqualTo(expectedUrl)
        assertThat(paramsCaptor.lastValue).isEqualTo(expectedParams)
    }

    @Test
    fun `calls correct url with single experiment name`() = test {
        initRequest(Success(successfulResponse))

        val experimentsNames = listOf("experiment_one")

        experimentRestClient.fetchAssignments(defaultPlatform, experimentsNames)

        val expectedUrl = "$EXPERIMENTS_ENDPOINT/$DEFAULT_VERSION/assignments/${defaultPlatform.value}/"
        val expectedParams = mapOf(
                "experiment_names" to "experiment_one",
                "anon_id" to ""
        )

        assertThat(urlCaptor.lastValue).isEqualTo(expectedUrl)
        assertThat(paramsCaptor.lastValue).isEqualTo(expectedParams)
    }

    @Test
    fun `calls correct url with multiple experiment names`() = test {
        initRequest(Success(successfulResponse))

        val experimentNames = listOf("experiment_one", "experiment_two", "experiment_three")

        experimentRestClient.fetchAssignments(defaultPlatform, experimentNames)

        val expectedUrl = "$EXPERIMENTS_ENDPOINT/$DEFAULT_VERSION/assignments/${defaultPlatform.value}/"
        val expectedParams = mapOf(
                "experiment_names" to "experiment_one,experiment_two,experiment_three",
                "anon_id" to ""
        )

        assertThat(urlCaptor.lastValue).isEqualTo(expectedUrl)
        assertThat(paramsCaptor.lastValue).isEqualTo(expectedParams)
    }

    @Test
    fun `calls correct url with anonymous id`() = test {
        initRequest(Success(successfulResponse))

        val anonymousId = "myAnonymousId"

        experimentRestClient.fetchAssignments(defaultPlatform, emptyList(), anonymousId)

        val expectedUrl = "$EXPERIMENTS_ENDPOINT/$DEFAULT_VERSION/assignments/${defaultPlatform.value}/"
        val expectedParams = mapOf(
                "experiment_names" to "",
                "anon_id" to anonymousId
        )

        assertThat(urlCaptor.lastValue).isEqualTo(expectedUrl)
        assertThat(paramsCaptor.lastValue).isEqualTo(expectedParams)
    }

    @Test
    fun `calls correct url with version`() = test {
        initRequest(Success(successfulResponse))

        val version = "1.0.0"

        experimentRestClient.fetchAssignments(defaultPlatform, emptyList(), null, version)

        val expectedUrl = "$EXPERIMENTS_ENDPOINT/$version/assignments/${defaultPlatform.value}/"
        val expectedParams = mapOf(
                "experiment_names" to "",
                "anon_id" to ""
        )

        assertThat(urlCaptor.lastValue).isEqualTo(expectedUrl)
        assertThat(paramsCaptor.lastValue).isEqualTo(expectedParams)
    }

    @Test
    fun `returns assignments when API call is successful`() = test {
        initRequest(Success(successfulResponse))

        val payload = experimentRestClient.fetchAssignments(defaultPlatform, emptyList())

        assertThat(payload).isNotNull
        assertThat(payload.assignments.variations).isEqualTo(successfulPayload.assignments.variations)
        assertThat(payload.assignments.ttl).isEqualTo(successfulPayload.assignments.ttl)
    }

    @Test
    fun `returns error when API call fails`() = test {
        initRequest(Error(errorResponse))

        val payload = experimentRestClient.fetchAssignments(defaultPlatform, emptyList())

        assertThat(payload).isNotNull
        assertThat(payload.isError).isTrue
    }

    private suspend fun initRequest(response: Response<FetchAssignmentsResponse>) {
        whenever(
                wpComGsonRequestBuilder.syncGetRequest(
                        eq(experimentRestClient),
                        urlCaptor.capture(),
                        paramsCaptor.capture(),
                        eq(FetchAssignmentsResponse::class.java),
                        eq(false),
                        any(),
                        eq(true)
                )
        ).thenReturn(response)
    }

    companion object {
        const val EXPERIMENTS_ENDPOINT = "https://public-api.wordpress.com/wpcom/v2/experiments"

        val defaultPlatform = CALYPSO

        private val successfulVariations = mapOf(
                "experiment_one" to null,
                "experiment_two" to "treatment",
                "experiment_three" to "other"
        )

        val successfulResponse = FetchAssignmentsResponse(successfulVariations, 3600)

        val errorResponse = WPComGsonNetworkError(BaseNetworkError(NETWORK_ERROR))

        val successfulPayload = FetchedAssignmentsPayload(AssignmentsModel(successfulVariations, 3600))
    }
}
