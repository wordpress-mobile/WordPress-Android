package org.wordpress.android.fluxc.network

import okhttp3.Interceptor
import okhttp3.Response
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

/**
 * Interface to check if tracking network requests is enabled.
 * This is implemented in the app module to access preferences.
 */
interface TrackNetworkRequestsPreference {
    fun isEnabled(): Boolean
}

/**
 * OkHttp interceptor that tracks network requests when enabled for troubleshooting purposes.
 *
 * This interceptor serves as the foundation for HTTP traffic inspection.
 * It will be extended to integrate with Chucker for production debugging.
 */
class TrackNetworkRequestsInterceptor(
    private val preference: TrackNetworkRequestsPreference
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Note: Reading the preference on every request is acceptable because SharedPreferences
        // caches values in memory after initial load. The only costs are:
        // 1. Initial disk read (happens once at app start, regardless of our usage)
        // 2. Memory sync after apply()/commit() (rare, only when user toggles the setting)
        // 3. HashMap lookup (negligible)
        // See: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r1/core/java/android/app/SharedPreferencesImpl.java#345
        if (preference.isEnabled()) {
            AppLog.d(T.API, "TrackNetworkRequestsInterceptor: Intercepted ${request.method} ${request.url}")
        } else {
            AppLog.d(T.API, "TrackNetworkRequestsInterceptor: Skipped (feature disabled)")
        }

        return chain.proceed(request)
    }
}
