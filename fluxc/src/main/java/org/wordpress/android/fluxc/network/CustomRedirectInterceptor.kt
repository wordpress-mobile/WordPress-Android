package org.wordpress.android.fluxc.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Request
import okhttp3.Response

class CustomRedirectInterceptor : Interceptor {
    override fun intercept(chain: Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)
        if (response.isRedirect) {
            val newRequest = getRedirectRequest(originalRequest, response)
            if (newRequest != null) {
                return chain.proceed(newRequest)
            }
        }
        return response
    }

    fun getRedirectRequest(originalRequest: Request, redirectResponse: Response): Request? {
        val location = redirectResponse.header("Location")
        if (!location.isNullOrEmpty()) {
            val newBuilder: Request.Builder = originalRequest.newBuilder().url(location)

            // Remove the authorization header if the hosts' TLD and SLD are not the same
            val originalHost = originalRequest.url.host
            val redirectHttpUrl = location.toHttpUrlOrNull()
            val redirectHost = redirectHttpUrl?.host ?: ""
            if (!tldAndSldAreEqual(originalHost, redirectHost)) {
                newBuilder.removeHeader("Authorization")
            }

            return newBuilder.build()
        }
        return null
    }

    private fun tldAndSldAreEqual(domain1: String, domain2: String): Boolean {
        val parts1 = domain1.split("\\.".toRegex())
        val parts2 = domain2.split("\\.".toRegex())
        return if (parts1.size < 2 || parts2.size < 2) {
            false
        } else {
            parts1[parts1.size - 1] == parts2[parts2.size - 1]
                && parts1[parts1.size - 2] == parts2[parts2.size - 2]
        }
    }
}
