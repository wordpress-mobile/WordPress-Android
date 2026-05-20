package org.wordpress.android.networking

import okhttp3.Interceptor
import okhttp3.Response
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.util.UrlUtils
import org.wordpress.android.util.WPUrlUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlideAuthInterceptor @Inject constructor(
    private val authenticationUtils: AuthenticationUtils
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val originalUrl = original.url.toString()

        val upgradedUrl = if (WPUrlUtils.isWordPressCom(originalUrl) && !UrlUtils.isHttps(originalUrl)) {
            UrlUtils.makeHttps(originalUrl)
        } else {
            originalUrl
        }

        val builder = original.newBuilder().url(upgradedUrl)
        authenticationUtils.getAuthHeaders(upgradedUrl).forEach { (name, value) ->
            builder.header(name, value)
        }
        return chain.proceed(builder.build())
    }
}
