package org.wordpress.android.fluxc.network

import okhttp3.Request
import okhttp3.Response
import okhttp3.Protocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CustomRedirectInterceptorTest {
    private val interceptor = CustomRedirectInterceptor()

    @Test
    fun `interceptor removes Authorization header when TLD and SLD are not the same`() {
        val originalRequest = Request.Builder()
            .url("https://original.com")
            .header("Authorization", "Bearer token")
            .build()
        val redirectResponse = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(302)
            .message("Redirect")
            .header("Location", "https://redirect.com")
            .build()

        val redirectRequest = interceptor.getRedirectRequest(originalRequest, redirectResponse)

        assertNull(redirectRequest?.headers("Authorization")?.firstOrNull())
    }

    @Test
    fun `interceptor keeps Authorization header when TLD and SLD are the same`() {
        val originalRequest = Request.Builder()
            .url("https://original.com")
            .header("Authorization", "Bearer token")
            .build()
        val redirectResponse = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(302)
            .message("Redirect")
            .header("Location", "https://original.com")
            .build()

        val redirectRequest = interceptor.getRedirectRequest(originalRequest, redirectResponse)

        assertEquals(redirectRequest?.headers("Authorization")?.firstOrNull(), "Bearer token")
    }

    @Test
    fun `interceptor resolves a relative Location against the original request URL`() {
        val originalRequest = Request.Builder()
            .url("https://original.com/wp-json/wp/v2/posts")
            .header("Authorization", "Bearer token")
            .build()
        val redirectResponse = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(302)
            .message("Redirect")
            .header("Location", "/media/123")
            .build()

        val redirectRequest = interceptor.getRedirectRequest(originalRequest, redirectResponse)

        // Same host, so the Authorization header is kept and the relative path is resolved.
        assertEquals("https://original.com/media/123", redirectRequest?.url.toString())
        assertEquals("Bearer token", redirectRequest?.headers("Authorization")?.firstOrNull())
    }

    @Test
    fun `interceptor returns null when there is no Location header`() {
        val originalRequest = Request.Builder()
            .url("https://original.com")
            .build()
        val redirectResponse = Response.Builder()
            .request(originalRequest)
            .protocol(Protocol.HTTP_1_1)
            .code(302)
            .message("Redirect")
            .build()

        assertNull(interceptor.getRedirectRequest(originalRequest, redirectResponse))
    }
}
