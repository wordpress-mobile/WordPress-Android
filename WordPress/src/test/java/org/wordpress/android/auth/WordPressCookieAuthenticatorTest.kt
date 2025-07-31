package org.wordpress.android.auth

import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.utils.PreferenceUtils.PreferenceUtilsWrapper
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WordPressCookieAuthenticatorTest {
    @Mock
    private lateinit var mockCookieJar: CookieJar

    @Mock
    private lateinit var mockPreferenceUtils: PreferenceUtilsWrapper

    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor

    @Mock
    private lateinit var mockOkHttpClient: OkHttpClient

    @Mock
    private lateinit var mockCall: Call
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var testScope: TestScope
    private lateinit var authenticator: WordPressCookieAuthenticator

    @Before
    fun setUp() {
        testScope = TestScope()
        testDispatcher = StandardTestDispatcher(testScope.testScheduler)

        // Mock SharedPreferences chain
        whenever(mockPreferenceUtils.getFluxCPreferences()).thenReturn(mockSharedPreferences)
        whenever(mockSharedPreferences.edit()).thenReturn(mockEditor)

        // Mock OkHttpClient to return our mock call
        whenever(mockOkHttpClient.newCall(any())).thenReturn(mockCall)
        whenever(mockOkHttpClient.cookieJar).thenReturn(mockCookieJar)

        authenticator = WordPressCookieAuthenticator(mockOkHttpClient, testDispatcher, mockPreferenceUtils)
    }

    @Test
    fun `authenticateForCookies should return failure when username is blank`() = testScope.runTest {
        // Given
        val params = WordPressCookieAuthenticator.AuthParams(
            username = "",
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
        assertEquals("Username cannot be empty", result.error)
    }

    @Test
    fun `authenticateForCookies should return failure when username is whitespace only`() = testScope.runTest {
        // Given
        val params = WordPressCookieAuthenticator.AuthParams(
            username = "   ",
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
        assertEquals("Username cannot be empty", result.error)
    }

    @Test
    fun `authenticateForCookies should return failure when bearer token is blank`() = testScope.runTest {
        // Given
        val params = WordPressCookieAuthenticator.AuthParams(
            username = "testuser",
            bearerToken = "",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
        assertEquals("Bearer token cannot be empty", result.error)
    }

    @Test
    fun `authenticateForCookies should return failure when bearer token is whitespace only`() = testScope.runTest {
        // Given
        val params = WordPressCookieAuthenticator.AuthParams(
            username = "testuser",
            bearerToken = "   ",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
        assertEquals("Bearer token cannot be empty", result.error)
    }

    @Test
    fun `authenticateForCookies should return cached cookies when cache is valid`() = testScope.runTest {
        // Given
        val username = "testuser"
        val cachedCookies = mapOf("wordpress_logged_in_123" to "cookie_value")
        val currentTime = System.currentTimeMillis() / 1000
        val futureTime = currentTime + (3 * 60 * 60) // 3 hours in future
        val cachedData =
            "{\"cookies\":{\"wordpress_logged_in_123\":\"cookie_value\"},\"expirationTimestamp\":$futureTime}"

        whenever(mockSharedPreferences.getString("WPCOM_COOKIE_CACHE_$username", null))
            .thenReturn(cachedData)

        val params = WordPressCookieAuthenticator.AuthParams(
            username = username,
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Success)
        assertEquals(cachedCookies, result.cookies)
    }

    @Test
    fun `authenticateForCookies should clear expired cache`() = testScope.runTest {
        // Given
        val username = "testuser"
        val currentTime = System.currentTimeMillis() / 1000
        val pastTime = currentTime - (1 * 60 * 60) // 1 hour in past (expired)
        val expiredCachedData =
            "{\"cookies\":{\"wordpress_logged_in_123\":\"expired_value\"},\"expirationTimestamp\":$pastTime}"

        whenever(mockSharedPreferences.getString("WPCOM_COOKIE_CACHE_$username", null))
            .thenReturn(expiredCachedData)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        // Mock network request to simulate expired cache handling
        val mockResponse = createMockResponse(200, "<html>Login page</html>")
        setupMockHttpCall(mockResponse)
        whenever(mockCookieJar.loadForRequest(any())).thenReturn(emptyList())

        val params = WordPressCookieAuthenticator.AuthParams(
            username = username,
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        authenticator.authenticateForCookies(params)

        // Then - Should clear expired cache
        verify(mockEditor).remove("WPCOM_COOKIE_CACHE_$username")
        verify(mockEditor).apply()
    }

    @Test
    fun `authenticateForCookies should handle corrupted cache gracefully`() = testScope.runTest {
        // Given
        val username = "testuser"
        val corruptedData = "invalid-json-data"

        whenever(mockSharedPreferences.getString("WPCOM_COOKIE_CACHE_$username", null))
            .thenReturn(corruptedData)
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        // Mock network request for corrupted cache scenario
        val mockResponse = createMockResponse(200, "<html>Login page</html>")
        setupMockHttpCall(mockResponse)
        whenever(mockCookieJar.loadForRequest(any())).thenReturn(emptyList())

        val params = WordPressCookieAuthenticator.AuthParams(
            username = username,
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then - Should clear corrupted cache
        verify(mockEditor).remove("WPCOM_COOKIE_CACHE_$username")
        verify(mockEditor).apply()

        // Should make network request after clearing cache
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
    }

    @Test
    fun `authenticateForCookies should return no cache initially`() = testScope.runTest {
        // Given
        val username = "testuser"

        // No cached data initially
        whenever(mockSharedPreferences.getString("WPCOM_COOKIE_CACHE_$username", null))
            .thenReturn(null)

        // Mock network request for no cache scenario
        val mockResponse = createMockResponse(200, "<html>Login page</html>")
        setupMockHttpCall(mockResponse)
        whenever(mockCookieJar.loadForRequest(any())).thenReturn(emptyList())

        val params = WordPressCookieAuthenticator.AuthParams(
            username = username,
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then - Should attempt network request since no cache
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
        assertEquals("No authentication cookies received", result.error)
    }

    @Test
    fun `clearAllCachedCookies should remove all cookie cache entries`() {
        // Given
        val allKeys = setOf(
            "WPCOM_COOKIE_CACHE_user1",
            "WPCOM_COOKIE_CACHE_user2",
            "OTHER_PREF_KEY",
            "WPCOM_COOKIE_CACHE_user3"
        )
        whenever(mockSharedPreferences.all).thenReturn(allKeys.associateWith { "value" })
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        // When
        authenticator.clearAllCachedCookies()

        // Then - Should only remove WPCOM_COOKIE_CACHE_ prefixed keys
        verify(mockEditor).remove("WPCOM_COOKIE_CACHE_user1")
        verify(mockEditor).remove("WPCOM_COOKIE_CACHE_user2")
        verify(mockEditor).remove("WPCOM_COOKIE_CACHE_user3")
        verify(mockEditor, never()).remove("OTHER_PREF_KEY")
        verify(mockEditor).apply()
    }

    @Test
    fun `clearCachedCookies should remove specific user cache`() {
        // Given
        val username = "testuser"
        whenever(mockEditor.remove(any())).thenReturn(mockEditor)

        // When
        authenticator.clearCachedCookies(username)

        // Then
        verify(mockEditor).remove("WPCOM_COOKIE_CACHE_$username")
        verify(mockEditor).apply()
    }

    private fun createMockResponse(code: Int, body: String): Response {
        val request = Request.Builder()
            .url("https://wordpress.com/wp-login.php")
            .build()

        return Response.Builder()
            .request(request)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(body.toResponseBody())
            .build()
    }

    private fun setupMockHttpCall(mockResponse: Response) {
        whenever(mockCall.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onResponse(mockCall, mockResponse)
            null
        }
    }

    @Test
    fun `authenticateForCookies should successfully authenticate and cache cookies`() = testScope.runTest {
        // Given
        val username = "testuser"
        val expectedCookies = mapOf("wordpress_logged_in_123" to "auth_cookie_value")

        // No cached data initially
        whenever(mockSharedPreferences.getString("WPCOM_COOKIE_CACHE_$username", null))
            .thenReturn(null)
        whenever(mockEditor.putString(any(), any())).thenReturn(mockEditor)

        // Mock successful network response with cookies
        val mockResponse = createMockResponse(200, "<html>Success</html>")
        setupMockHttpCall(mockResponse)

        // Mock cookie jar to return authentication cookies
        val mockCookie = org.mockito.kotlin.mock<okhttp3.Cookie>()
        whenever(mockCookie.name).thenReturn("wordpress_logged_in_123")
        whenever(mockCookie.value).thenReturn("auth_cookie_value")
        whenever(mockCookieJar.loadForRequest(any())).thenReturn(listOf(mockCookie))

        val params = WordPressCookieAuthenticator.AuthParams(
            username = username,
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Success)
        assertEquals(expectedCookies, result.cookies)

        // Verify cookies were cached
        verify(mockEditor).putString(eq("WPCOM_COOKIE_CACHE_$username"), any())
        verify(mockEditor).apply()
    }

    @Test
    fun `authenticateForCookies should handle HTTP error response`() = testScope.runTest {
        // Given
        val username = "testuser"

        // No cached data initially
        whenever(mockSharedPreferences.getString("WPCOM_COOKIE_CACHE_$username", null))
            .thenReturn(null)

        // Mock HTTP error response
        val mockResponse = createMockResponse(401, "<html>Unauthorized</html>")
        setupMockHttpCall(mockResponse)

        val params = WordPressCookieAuthenticator.AuthParams(
            username = username,
            bearerToken = "invalid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
        assertEquals("HTTP error: 401", result.error)
    }

    @Test
    fun `authenticateForCookies should handle network failure`() = testScope.runTest {
        // Given
        val username = "testuser"

        // No cached data initially
        whenever(mockSharedPreferences.getString("WPCOM_COOKIE_CACHE_$username", null))
            .thenReturn(null)

        // Mock network failure
        whenever(mockCall.enqueue(any())).thenAnswer { invocation ->
            val callback = invocation.getArgument<Callback>(0)
            callback.onFailure(mockCall, java.io.IOException("Network error"))
            null
        }

        val params = WordPressCookieAuthenticator.AuthParams(
            username = username,
            bearerToken = "valid-token",
            userAgent = "test-agent"
        )

        // When
        val result = authenticator.authenticateForCookies(params)

        // Then
        assertTrue(result is WordPressCookieAuthenticator.AuthResult.Failure)
        assertEquals("Authentication error: Network error", result.error)
    }
}
