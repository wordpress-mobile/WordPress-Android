package org.wordpress.android.fluxc.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import okhttp3.Interceptor
import okhttp3.Response

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
 * This interceptor wraps Chucker's ChuckerInterceptor and only delegates to it when the
 * feature is enabled via [TrackNetworkRequestsPreference]. When disabled, requests pass
 * through without any logging or inspection.
 *
 * @param context Application context for Chucker initialization
 * @param preference Provides the enabled/disabled state from app preferences
 */
class TrackNetworkRequestsInterceptor(
    context: Context,
    private val preference: TrackNetworkRequestsPreference
) : Interceptor {
    private val chuckerInterceptor: Interceptor = createChuckerInterceptor(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        // Note: Reading the preference on every request is acceptable because SharedPreferences
        // caches values in memory after initial load. The only costs are:
        // 1. Initial disk read (happens once at app start, regardless of our usage)
        // 2. Memory sync after apply()/commit() (rare, only when user toggles the setting)
        // 3. HashMap lookup (negligible)
        // See: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r1/core/java/android/app/SharedPreferencesImpl.java#345
        return if (preference.isEnabled()) {
            chuckerInterceptor.intercept(chain)
        } else {
            chain.proceed(chain.request())
        }
    }

    companion object {
        private val SENSITIVE_HEADERS = setOf(
            "Authorization",
            "Cookie",
            "Set-Cookie",
            "X-WP-Nonce"
        )

        private fun createChuckerInterceptor(context: Context): ChuckerInterceptor {
            val collector = ChuckerCollector(
                context = context,
                showNotification = false,
                retentionPeriod = RetentionManager.Period.ONE_HOUR
            )
            return ChuckerInterceptor.Builder(context)
                .collector(collector)
                .maxContentLength(MAX_CONTENT_LENGTH)
                .redactHeaders(SENSITIVE_HEADERS)
                .alwaysReadResponseBody(false)
                .build()
        }

        private const val MAX_CONTENT_LENGTH = 250_000L
    }
}
