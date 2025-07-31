package org.wordpress.android.auth

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper
import org.wordpress.android.util.AppLog
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.core.content.edit

/**
 * Utility class for WordPress.com cookie authentication.
 * Handles Bearer token authentication to retrieve session cookies.
 * Uses WPAndroid's configured OkHttpClient with proper cookie handling.
 * Implements automatic caching to avoid unnecessary network requests.
 */
@Singleton
class WordPressCookieAuthenticator @Inject constructor(
    @Named("regular") private val okHttpClient: OkHttpClient,
    @Named("IO_THREAD") private val ioDispatcher: CoroutineDispatcher,
    private val preferenceUtils: PreferenceUtilsWrapper
) {
    companion object {
        private const val WP_LOGIN_URL = "https://wordpress.com/wp-login.php"
        private const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"
        private const val WPCOM_COOKIE_CACHE_PREFIX = "WPCOM_COOKIE_CACHE_"
        private const val COOKIE_CACHE_EXPIRATION_HOURS = 24 * 30 * 6 // 6 months (approximately)
        private const val MILLIS_PER_SECOND = 1000L
        private const val SECONDS_PER_HOUR = 3600L
    }

    private val gson: Gson by lazy {
        GsonBuilder().create()
    }

    /**
     * Data class for cached cookie information
     */
    private data class CachedCookieData(
        val cookies: Map<String, String>,
        val expirationTimestamp: Long
    )

    /**
     * Data class for authentication parameters
     */
    data class AuthParams(
        val username: String,
        val bearerToken: String,
        val userAgent: String
    )

    /**
     * Data class for validated authentication parameters (internal use)
     */
    private data class ValidatedAuthParams(
        val username: String,
        val bearerToken: String,
        val userAgent: String
    )

    /**
     * Data class for authentication result
     */
    sealed class AuthResult {
        data class Success(val cookies: Map<String, String>) : AuthResult()
        data class Failure(val error: String) : AuthResult()
    }

    /**
     * Authenticates with WordPress.com and retrieves session cookies using coroutines.
     * This is the preferred method for Kotlin callers.
     * Automatically handles caching to avoid unnecessary network requests.
     *
     * @param params Authentication parameters including username, Bearer token, and user agent
     * @return AuthResult containing either success with cookies or failure with error message
     */
    suspend fun authenticateForCookies(params: AuthParams): AuthResult = withContext(ioDispatcher) {
        // Validate parameters
        if (params.username.isBlank()) {
            return@withContext AuthResult.Failure("Username cannot be empty")
        }

        if (params.bearerToken.isBlank()) {
            return@withContext AuthResult.Failure("Bearer token cannot be empty")
        }

        AppLog.d(
            AppLog.T.API,
            "Starting WordPress.com account cookie authentication for user: ${params.username}"
        )

        // Check cache first
        val cachedCookies = getCachedCookies(params.username)
        if (cachedCookies != null) {
            AppLog.d(
                AppLog.T.API,
                "WordPress.com account cookie cache hit for user: ${params.username}"
            )
            return@withContext AuthResult.Success(cachedCookies)
        }

        AppLog.d(
            AppLog.T.API,
            "WordPress.com account cookie cache miss for user: ${params.username}, making network request"
        )

        // Create validated parameters after validation checks pass
        val validatedParams = ValidatedAuthParams(
            username = params.username,
            bearerToken = params.bearerToken,
            userAgent = params.userAgent
        )

        return@withContext performAuthenticationRequest(validatedParams)
    }

    /**
     * Performs the actual authentication request and handles caching of successful results.
     * Accepts only validated parameters, ensuring all validation has been completed.
     */
    private suspend fun performAuthenticationRequest(validatedParams: ValidatedAuthParams): AuthResult {
        try {
            // Build form body with authentication parameters
            val formBody = FormBody.Builder()
                .add("log", validatedParams.username)
                .add("rememberme", "true")
                .build()

            // Build HTTP request matching curl command
            val request = Request.Builder()
                .url(WP_LOGIN_URL)
                .post(formBody)
                .addHeader("Content-Type", CONTENT_TYPE_FORM)
                .addHeader("Authorization", "Bearer ${validatedParams.bearerToken}")
                .addHeader("User-Agent", validatedParams.userAgent)
                .build()

            AppLog.d(
                AppLog.T.API,
                "Making POST account cookie authentication request to: $WP_LOGIN_URL"
            )
            AppLog.d(AppLog.T.API, "Request form body: log=${validatedParams.username}&rememberme=true")
            AppLog.d(AppLog.T.API, "Request User-Agent: ${validatedParams.userAgent}")
            AppLog.d(AppLog.T.API, "Request Content-Type: $CONTENT_TYPE_FORM")

            // Execute request and wait for response
            val response = executeRequest(request)
            return response.use {
                val result = handleAuthResponse(response)

                // Cache successful results
                if (result is AuthResult.Success) {
                    cacheCookies(validatedParams.username, result.cookies)
                    AppLog.d(
                        AppLog.T.API,
                        "WordPress.com account cookies cached for user: ${validatedParams.username}"
                    )
                }

                result
            }
        } catch (e: IOException) {
            AppLog.e(
                AppLog.T.API,
                "WordPress.com account cookie authentication error: ${e.message}"
            )
            return AuthResult.Failure("Authentication error: ${e.message}")
        }
    }

    /**
     * Executes HTTP request using coroutines with proper cancellation support
     */
    private suspend fun executeRequest(request: Request): Response =
        suspendCancellableCoroutine { continuation ->
            val call = okHttpClient.newCall(request)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    }
                }
            })

            continuation.invokeOnCancellation {
                call.cancel()
            }
        }

    /**
     * Handles the authentication response and extracts cookies
     */
    private fun handleAuthResponse(response: Response): AuthResult {
        AppLog.d(AppLog.T.API, "Account cookie auth response code: ${response.code}")
        AppLog.d(AppLog.T.API, "Account cookie auth response message: ${response.message}")
        AppLog.d(AppLog.T.API, "Response headers: ${response.headers}")

        if (!response.isSuccessful) {
            AppLog.w(
                AppLog.T.API,
                "WordPress.com account cookie authentication unsuccessful: HTTP ${response.code}"
            )
            return AuthResult.Failure("HTTP error: ${response.code}")
        }

        // Log all Set-Cookie headers for debugging
        val allCookieHeaders = response.headers("Set-Cookie")
        AppLog.d(AppLog.T.API, "Total Set-Cookie headers: ${allCookieHeaders.size}")
        for (cookieHeader in allCookieHeaders) {
            AppLog.d(AppLog.T.API, "Raw cookie header: $cookieHeader")
        }

        // Extract authentication cookies from CookieJar instead of headers
        val cookies = extractAuthenticationCookiesFromJar(response.request.url)

        return if (cookies.isEmpty()) {
            AppLog.w(AppLog.T.API, "No WordPress.com account cookies found in response")
            AuthResult.Failure("No authentication cookies received")
        } else {
            AppLog.d(
                AppLog.T.API,
                "Successfully retrieved ${cookies.size} WordPress.com account cookies"
            )
            for (cookieName in cookies.keys) {
                AppLog.d(AppLog.T.API, "Received WordPress.com account cookie: $cookieName")
            }
            AuthResult.Success(cookies)
        }
    }

    /**
     * Extracts WordPress authentication cookies from the CookieJar
     */
    private fun extractAuthenticationCookiesFromJar(url: HttpUrl): Map<String, String> {
        val cookies = mutableMapOf<String, String>()

        // Get all cookies from the CookieJar for the request URL
        val jarCookies = okHttpClient.cookieJar.loadForRequest(url)
        AppLog.d(AppLog.T.API, "Total cookies in jar for ${url.host}: ${jarCookies.size}")

        for (cookie in jarCookies) {
            cookies[cookie.name] = cookie.value
            AppLog.d(
                AppLog.T.API,
                "Extracted WordPress.com account cookie from jar: ${cookie.name}"
            )
        }

        return cookies
    }

    /**
     * Gets cached cookies for a user if they exist and are not expired
     */
    private fun getCachedCookies(username: String): Map<String, String>? {
        val cacheKey = getCacheKey(username)
        val cachedDataJson = preferenceUtils.getFluxCPreferences().getString(cacheKey, null)

        if (cachedDataJson == null) {
            AppLog.d(
                AppLog.T.API,
                "No cached WordPress.com account cookie data found for user: $username"
            )
            return null
        }

        return try {
            val cachedData = gson.fromJson(cachedDataJson, CachedCookieData::class.java)
            val currentTime = System.currentTimeMillis() / MILLIS_PER_SECOND

            if (currentTime >= cachedData.expirationTimestamp) {
                AppLog.d(
                    AppLog.T.API,
                    "Cached WordPress.com account cookies expired for user: $username, clearing cache"
                )
                clearCachedCookies(username)
                null
            } else {
                AppLog.d(
                    AppLog.T.API,
                    "Valid cached WordPress.com account cookies found for user: $username"
                )
                cachedData.cookies
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            AppLog.w(
                AppLog.T.API,
                "Failed to parse cached WordPress.com account cookie data for " +
                        "user: $username, clearing cache: ${e.message}"
            )
            clearCachedCookies(username)
            null
        }
    }

    /**
     * Caches cookies for a user with expiration timestamp
     */
    private fun cacheCookies(username: String, cookies: Map<String, String>) {
        val cacheKey = getCacheKey(username)
        val currentTime = System.currentTimeMillis() / MILLIS_PER_SECOND
        val expirationTime = currentTime + (COOKIE_CACHE_EXPIRATION_HOURS * SECONDS_PER_HOUR)

        val cachedData = CachedCookieData(
            cookies = cookies,
            expirationTimestamp = expirationTime
        )

        val cachedDataJson = gson.toJson(cachedData)
        preferenceUtils.getFluxCPreferences().edit {
            putString(cacheKey, cachedDataJson)
        }

        AppLog.d(
            AppLog.T.API,
            "Cached ${cookies.size} WordPress.com account cookies for user: $username, expires at: $expirationTime"
        )
    }

    /**
     * Clears cached cookies for a specific user
     */
    fun clearCachedCookies(username: String) {
        val cacheKey = getCacheKey(username)
        preferenceUtils.getFluxCPreferences().edit {
            remove(cacheKey)
        }

        AppLog.d(AppLog.T.API, "Cleared cached WordPress.com account cookies for user: $username")
    }

    /**
     * Clears all cached cookies (useful for logout)
     */
    fun clearAllCachedCookies() {
        val preferences = preferenceUtils.getFluxCPreferences()
        val editor = preferences.edit()
        val allKeys = preferences.all.keys

        var clearedCount = 0
        for (key in allKeys) {
            if (key.startsWith(WPCOM_COOKIE_CACHE_PREFIX)) {
                editor.remove(key)
                clearedCount++
            }
        }

        editor.apply()
        AppLog.d(AppLog.T.API, "Cleared $clearedCount cached WordPress.com account cookie entries")
    }

    /**
     * Generates cache key for a user
     */
    private fun getCacheKey(username: String): String {
        return WPCOM_COOKIE_CACHE_PREFIX + username
    }
}
