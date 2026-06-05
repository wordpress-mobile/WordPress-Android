package org.wordpress.android.networking

import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.ui.utils.AuthenticationUtils

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class GlideAuthInterceptorTest {
    private val authenticationUtils: AuthenticationUtils = mock()
    private val chain: Interceptor.Chain = mock()

    private lateinit var interceptor: GlideAuthInterceptor

    @Before
    fun setUp() {
        interceptor = GlideAuthInterceptor(authenticationUtils)
        whenever(authenticationUtils.getAuthHeaders(any())).thenReturn(emptyMap())
        whenever(chain.proceed(any())).thenAnswer { dummyResponse(it.getArgument(0)) }
    }

    @Test
    fun `http URL on WPcom host is upgraded to https`() {
        whenever(chain.request()).thenReturn(buildRequest("http://example.wordpress.com/image.jpg"))

        interceptor.intercept(chain)

        val sent = captureSentRequest()
        assertThat(sent.url.scheme).isEqualTo("https")
        assertThat(sent.url.host).isEqualTo("example.wordpress.com")
    }

    @Test
    fun `https URL on WPcom host is unchanged`() {
        whenever(chain.request()).thenReturn(buildRequest("https://example.wordpress.com/image.jpg"))

        interceptor.intercept(chain)

        val sent = captureSentRequest()
        assertThat(sent.url.scheme).isEqualTo("https")
        assertThat(sent.url.host).isEqualTo("example.wordpress.com")
    }

    @Test
    fun `non-WPcom http URL is not upgraded`() {
        whenever(chain.request()).thenReturn(buildRequest("http://example.com/image.jpg"))

        interceptor.intercept(chain)

        val sent = captureSentRequest()
        assertThat(sent.url.scheme).isEqualTo("http")
        assertThat(sent.url.host).isEqualTo("example.com")
    }

    @Test
    fun `auth headers from AuthenticationUtils are applied to the request`() {
        whenever(chain.request()).thenReturn(buildRequest("https://example.wordpress.com/image.jpg"))
        whenever(authenticationUtils.getAuthHeaders("https://example.wordpress.com/image.jpg"))
            .thenReturn(
                mapOf(
                    "Authorization" to "Bearer token",
                    "User-Agent" to "wp-test/1.0"
                )
            )

        interceptor.intercept(chain)

        val sent = captureSentRequest()
        assertThat(sent.header("Authorization")).isEqualTo("Bearer token")
        assertThat(sent.header("User-Agent")).isEqualTo("wp-test/1.0")
    }

    @Test
    fun `getAuthHeaders is called with the upgraded https URL not the original http URL`() {
        whenever(chain.request()).thenReturn(buildRequest("http://example.wordpress.com/image.jpg"))

        interceptor.intercept(chain)

        verify(authenticationUtils).getAuthHeaders("https://example.wordpress.com/image.jpg")
    }

    private fun captureSentRequest(): Request {
        val captor = argumentCaptor<Request>()
        verify(chain).proceed(captor.capture())
        return captor.firstValue
    }

    private fun buildRequest(url: String): Request = Request.Builder().url(url).build()

    private fun dummyResponse(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("".toResponseBody(null))
        .build()
}
