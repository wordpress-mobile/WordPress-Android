package org.wordpress.android.fluxc.network

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.chuckerteam.chucker.api.RetentionManager
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * Retention period options for tracked network requests.
 *
 * IMPORTANT: Do not modify existing int values as they are persisted to SharedPreferences.
 * To add new options, append them with new int values. If existing values must change,
 * implement a preference migration.
 */
enum class NetworkRequestsRetentionPeriod(val value: Int) {
    ONE_HOUR(0),
    ONE_DAY(1),
    ONE_WEEK(2),
    FOREVER(3);

    fun toChuckerPeriod(): RetentionManager.Period = when (this) {
        ONE_HOUR -> RetentionManager.Period.ONE_HOUR
        ONE_DAY -> RetentionManager.Period.ONE_DAY
        ONE_WEEK -> RetentionManager.Period.ONE_WEEK
        FOREVER -> RetentionManager.Period.FOREVER
    }

    companion object {
        fun fromInt(value: Int): NetworkRequestsRetentionPeriod =
            entries.find { it.value == value } ?: ONE_HOUR
    }
}

/**
 * Interface to check tracking preferences.
 * This is implemented in the app module to access preferences.
 */
interface TrackNetworkRequestsPreference {
    fun isEnabled(): Boolean
    fun getRetentionPeriod(): NetworkRequestsRetentionPeriod
}

/**
 * OkHttp interceptor that tracks network requests when enabled for troubleshooting purposes.
 *
 * This interceptor wraps Chucker's ChuckerInterceptor and only delegates to it when the
 * feature is enabled via [TrackNetworkRequestsPreference]. When disabled, requests pass
 * through without any logging or inspection.
 *
 * @param context Application context for Chucker initialization
 * @param preference Provides the enabled/disabled state and retention period from app preferences
 */
class TrackNetworkRequestsInterceptor(
    private val context: Context,
    private val preference: TrackNetworkRequestsPreference
) : Interceptor {
    @Volatile
    private var chuckerInterceptor: ChuckerInterceptor? = null

    @Volatile
    private var currentRetentionPeriod: NetworkRequestsRetentionPeriod? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        // Note: Reading the preference on every request is acceptable because SharedPreferences
        // caches values in memory after initial load. The only costs are:
        // 1. Initial disk read (happens once at app start, regardless of our usage)
        // 2. Memory sync after apply()/commit() (rare, only when user toggles the setting)
        // 3. HashMap lookup (negligible)
        // See: https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-14.0.0_r1/core/java/android/app/SharedPreferencesImpl.java#345
        return if (preference.isEnabled()) {
            val chucker = getOrCreateChuckerInterceptor()
            if (shouldRedactRequestBody(chain.request())) {
                chucker.intercept(RedactedBodyChain(chain))
            } else {
                chucker.intercept(chain)
            }
        } else {
            chain.proceed(chain.request())
        }
    }

    private fun shouldRedactRequestBody(request: Request): Boolean {
        val pathSegments = request.url.pathSegments
        return REDACTED_BODY_PATHS.any { it in pathSegments }
    }

    private fun getOrCreateChuckerInterceptor(): ChuckerInterceptor {
        val desiredRetention = preference.getRetentionPeriod()
        val currentInterceptor = chuckerInterceptor

        // Recreate interceptor if retention period changed
        if (currentInterceptor == null || currentRetentionPeriod != desiredRetention) {
            synchronized(this) {
                // Double-check after acquiring lock
                if (chuckerInterceptor == null || currentRetentionPeriod != desiredRetention) {
                    chuckerInterceptor = createChuckerInterceptor(desiredRetention)
                    currentRetentionPeriod = desiredRetention
                }
            }
        }
        return chuckerInterceptor!!
    }

    private fun createChuckerInterceptor(retention: NetworkRequestsRetentionPeriod): ChuckerInterceptor {
        val collector = ChuckerCollector(
            context = context,
            showNotification = false,
            retentionPeriod = retention.toChuckerPeriod()
        )
        return ChuckerInterceptor.Builder(context)
            .collector(collector)
            .maxContentLength(MAX_CONTENT_LENGTH)
            .redactHeaders(SENSITIVE_HEADERS)
            .alwaysReadResponseBody(false)
            .build()
    }

    /**
     * A [Interceptor.Chain] wrapper that presents a redacted request body
     * to Chucker for logging purposes, while ensuring the actual network
     * call uses the original request with the real body intact.
     *
     * This achieves selective logging redaction without affecting the
     * actual HTTP request/response.
     */
    private class RedactedBodyChain(
        private val delegate: Interceptor.Chain
    ) : Interceptor.Chain {
        private val redactedRequest: Request by lazy {
            val original = delegate.request()
            // Falls back to plain text if original has no body;
            // the redacted placeholder still needs a content type.
            val contentType = original.body?.contentType()
                ?: "text/plain".toMediaType()
            val redactedBody = REDACTED_BODY_PLACEHOLDER
                .toRequestBody(contentType)
            original.newBuilder().method(original.method, redactedBody).build()
        }

        override fun request(): Request = redactedRequest

        override fun proceed(request: Request): Response {
            return delegate.proceed(delegate.request())
        }

        override fun connection(): Connection? = delegate.connection()
        override fun call(): Call = delegate.call()
        override fun connectTimeoutMillis(): Int =
            delegate.connectTimeoutMillis()
        override fun withConnectTimeout(
            timeout: Int,
            unit: TimeUnit
        ): Interceptor.Chain =
            RedactedBodyChain(delegate.withConnectTimeout(timeout, unit))
        override fun readTimeoutMillis(): Int =
            delegate.readTimeoutMillis()
        override fun withReadTimeout(
            timeout: Int,
            unit: TimeUnit
        ): Interceptor.Chain =
            RedactedBodyChain(delegate.withReadTimeout(timeout, unit))
        override fun writeTimeoutMillis(): Int =
            delegate.writeTimeoutMillis()
        override fun withWriteTimeout(
            timeout: Int,
            unit: TimeUnit
        ): Interceptor.Chain =
            RedactedBodyChain(delegate.withWriteTimeout(timeout, unit))
    }

    companion object {
        private val SENSITIVE_HEADERS = setOf(
            "Authorization",
            "Cookie",
            "Set-Cookie",
            "X-WP-Nonce"
        )

        private const val MAX_CONTENT_LENGTH = 250_000L

        private const val REDACTED_BODY_PLACEHOLDER =
            "[Body redacted — contains sensitive information]"

        private val REDACTED_BODY_PATHS = listOf(
            "xmlrpc.php",
            "wp-login.php"
        )
    }
}
