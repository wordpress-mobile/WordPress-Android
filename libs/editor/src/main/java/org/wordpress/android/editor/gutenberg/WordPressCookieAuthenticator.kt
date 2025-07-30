package org.wordpress.android.editor.gutenberg

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.HttpUrl
import java.net.CookieHandler
import java.net.CookieManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Utility class for WordPress.com cookie authentication.
 * Handles Bearer token authentication to retrieve session cookies.
 * Supports both Kotlin coroutines and Java callback interfaces.
 * Uses WPAndroid's configured OkHttpClient with proper cookie handling.
 */
class WordPressCookieAuthenticator @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val WP_LOGIN_URL = "https://wordpress.com/wp-login.php"
        private const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"
    }

    /**
     * Data class for authentication parameters
     */
    data class AuthParams(
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
     * Callback interface for authentication results (for Java interoperability)
     */
    interface AuthCallback {
        fun onResult(result: AuthResult)
    }

    /**
     * Authenticates with WordPress.com and retrieves session cookies using coroutines.
     * This is the preferred method for Kotlin callers.
     *
     * @param params Authentication parameters including username, Bearer token, and user agent
     * @return AuthResult containing either success with cookies or failure with error message
     */
    suspend fun authenticateForCookies(params: AuthParams): AuthResult = withContext(Dispatchers.IO) {
        // Validate parameters
        if (params.username.isBlank()) {
            return@withContext AuthResult.Failure("Username cannot be empty")
        }

        if (params.bearerToken.isBlank()) {
            return@withContext AuthResult.Failure("Bearer token cannot be empty")
        }

        AppLog.d(T.EDITOR, "Starting WordPress.com cookie authentication for user: ${params.username}")

        try {
            // Build form body with authentication parameters
            val formBody = FormBody.Builder()
                .add("log", params.username)
                .add("rememberme", "true")
                .build()

            // Build HTTP request matching curl command
            val request = Request.Builder()
                .url(WP_LOGIN_URL)
                .post(formBody)
                .addHeader("Content-Type", CONTENT_TYPE_FORM)
                .addHeader("Authorization", "Bearer ${params.bearerToken}")
                .addHeader("User-Agent", params.userAgent)
                .build()

            AppLog.d(T.EDITOR, "Making POST cookie authentication request to: $WP_LOGIN_URL")
            AppLog.d(T.EDITOR, "Request form body: log=${params.username}&rememberme=true")
            AppLog.d(T.EDITOR, "Request User-Agent: ${params.userAgent}")
            AppLog.d(T.EDITOR, "Request Content-Type: $CONTENT_TYPE_FORM")

            // Execute request and wait for response
            val response = executeRequest(request)
            response.use {
                handleAuthResponse(response)
            }
        } catch (e: IOException) {
            AppLog.e(T.EDITOR, "WordPress.com cookie authentication error: ${e.message}")
            AuthResult.Failure("Authentication error: ${e.message}")
        }
    }

    /**
     * Java-friendly callback-based version for backwards compatibility.
     * This method can be called from Java code and delegates to the suspend function.
     *
     * @param params Authentication parameters including username, Bearer token, and user agent
     * @param callback Callback to receive the authentication result
     */
    fun authenticateForCookies(params: AuthParams, callback: AuthCallback) {
        coroutineScope.launch {
            val result = authenticateForCookies(params)
            callback.onResult(result)
        }
    }

    /**
     * Executes HTTP request using coroutines with proper cancellation support
     */
    private suspend fun executeRequest(request: Request): Response = suspendCancellableCoroutine { continuation ->
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
        AppLog.d(T.EDITOR, "Cookie auth response code: ${response.code}")
        AppLog.d(T.EDITOR, "Cookie auth response message: ${response.message}")
        AppLog.d(T.EDITOR, "Response headers: ${response.headers}")

        if (!response.isSuccessful) {
            AppLog.w(T.EDITOR, "WordPress.com cookie authentication unsuccessful: HTTP ${response.code}")
            return AuthResult.Failure("HTTP error: ${response.code}")
        }

        // Log all Set-Cookie headers for debugging
        val allCookieHeaders = response.headers("Set-Cookie")
        AppLog.d(T.EDITOR, "Total Set-Cookie headers: ${allCookieHeaders.size}")
        for (cookieHeader in allCookieHeaders) {
            AppLog.d(T.EDITOR, "Raw cookie header: $cookieHeader")
        }

        // Extract authentication cookies from CookieJar instead of headers
        val cookies = extractAuthenticationCookiesFromJar(response.request.url)

        return if (cookies.isEmpty()) {
            AppLog.w(T.EDITOR, "No authentication cookies found in response")
            AuthResult.Failure("No authentication cookies received")
        } else {
            AppLog.d(T.EDITOR, "Successfully retrieved ${cookies.size} authentication cookies")
            for (cookieName in cookies.keys) {
                AppLog.d(T.EDITOR, "Received cookie: $cookieName")
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
        AppLog.d(T.EDITOR, "Total cookies in jar for ${url.host}: ${jarCookies.size}")

        for (cookie in jarCookies) {
            cookies[cookie.name] = cookie.value
            AppLog.d(T.EDITOR, "Extracted auth cookie from jar: ${cookie.name}")
        }

        return cookies
    }
}

/**
 * Creates an OkHttpClient with proper cookie handling for WordPress authentication
 */
private fun createDefaultOkHttpClient(): OkHttpClient {
    // Create CookieManager similar to ReleaseNetworkModule setup
    val cookieManager = CookieManager()
    CookieHandler.setDefault(cookieManager)
    val cookieJar: CookieJar = JavaNetCookieJar(cookieManager)

    // Create OkHttpClient with proper cookie handling
    return OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

private const val DEFAULT_TIMEOUT_SECONDS = 15L
