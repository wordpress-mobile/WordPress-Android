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
}
