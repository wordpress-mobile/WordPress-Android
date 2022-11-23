package org.wordpress.android.fluxc.network

import com.android.volley.Cache
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.ServerError
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.HttpResponse
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.RetryOnRedirectBasicNetwork.HTTP_TEMPORARY_REDIRECT
import java.net.HttpURLConnection.HTTP_OK
import kotlin.math.min
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val TIMEOUT = 1
private const val BACKOFF_MULTIPLIER = 1f

@Ignore("Caused by: java.lang.ClassNotFoundException: org.apache.http.StatusLine")
@RunWith(RobolectricTestRunner::class)
class RetryOnRedirectBasicNetworkTest {
    private val redirectResponse = HttpResponse(HTTP_TEMPORARY_REDIRECT, listOf())
    private val successResponse = HttpResponse(HTTP_OK, listOf())

    // Mocked responses sequence that requires 3 retries for a successful response
    private val mockedResponses = listOf(redirectResponse, redirectResponse, redirectResponse, successResponse)

    @Test
    fun successfulRetryOnRedirect() {
        val network = RetryOnRedirectBasicNetwork(MockedHttpStack(mockedResponses))
        val request: Request<String> = MockedRequest
        request.retryPolicy = DefaultRetryPolicy(TIMEOUT, 3, BACKOFF_MULTIPLIER)
        val response = network.performRequest(request)
        assertEquals(response.statusCode, HTTP_OK)
    }

    @Test
    fun unsuccessfulRetryOnRedirect() {
        val network = RetryOnRedirectBasicNetwork(MockedHttpStack(mockedResponses))
        val request: Request<String> = MockedRequest
        request.retryPolicy = DefaultRetryPolicy(TIMEOUT, 2, BACKOFF_MULTIPLIER)
        val response = try {
            network.performRequest(request)
        } catch (error: ServerError) {
            assertEquals(error.networkResponse.statusCode, HTTP_TEMPORARY_REDIRECT)
            null
        }
        assertNull(response)
    }

    private class MockedHttpStack(private val responses: List<HttpResponse>) : BaseHttpStack() {
        private var requestCount = 0

        override fun executeRequest(request: Request<*>, additionalHeaders: Map<String, String>): HttpResponse {
            val index = min(responses.size - 1, requestCount)
            requestCount += 1
            return responses[index]
        }
    }

    private object MockedRequest : Request<String>(Method.GET, "http://foo.bar", null) {
        override fun getHeaders(): Map<String, String> = mapOf()

        override fun getParams(): Map<String, String> = mapOf()

        override fun deliverResponse(response: String?) = Unit // Do nothing (ignore)

        override fun parseNetworkResponse(response: NetworkResponse?): Response<String> =
            Response.success("foo", Cache.Entry())
    }
}
