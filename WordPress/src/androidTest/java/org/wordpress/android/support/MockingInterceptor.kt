package org.wordpress.android.support

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Singleton

@Singleton
class MockingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Redirect all WordPress.com API requests to local mock server
        if (request.url.host == "public-api.wordpress.com") {
            val newUrl = request.url.newBuilder()
                .scheme("http")
                .host("localhost")
                .port(BaseTest.WIREMOCK_PORT)
                .build()
            val newRequest = request.newBuilder()
                .url(newUrl)
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(request)
    }
}
