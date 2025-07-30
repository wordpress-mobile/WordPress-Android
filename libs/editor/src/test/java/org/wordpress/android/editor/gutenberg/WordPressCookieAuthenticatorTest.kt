package org.wordpress.android.editor.gutenberg

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WordPressCookieAuthenticatorTest {

    @Mock
    private lateinit var mockCookieJar: CookieJar

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var testScope: TestScope
    private lateinit var authenticator: WordPressCookieAuthenticator

    @Before
    fun setUp() {
        okHttpClient = OkHttpClient.Builder()
            .cookieJar(mockCookieJar)
            .build()

        testScope = TestScope()
        coroutineScope = CoroutineScope(StandardTestDispatcher(testScope.testScheduler))
        authenticator = WordPressCookieAuthenticator(okHttpClient, coroutineScope)
    }

    @Test
    fun `authenticateForCookies should return failure when username is blank`() = runTest {
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
    fun `authenticateForCookies should return failure when username is whitespace only`() = runTest {
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
    fun `authenticateForCookies should return failure when bearer token is blank`() = runTest {
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
    fun `authenticateForCookies should return failure when bearer token is whitespace only`() = runTest {
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
    fun `AuthParams data class should store values correctly`() {
        // Given
        val username = "testuser"
        val bearerToken = "test-bearer-token"
        val userAgent = "WordPress/Test-Agent"

        // When
        val params = WordPressCookieAuthenticator.AuthParams(username, bearerToken, userAgent)

        // Then
        assertEquals(username, params.username)
        assertEquals(bearerToken, params.bearerToken)
        assertEquals(userAgent, params.userAgent)
    }

    @Test
    fun `AuthResult Success should contain cookies map`() {
        // Given
        val cookies = mapOf("wordpress_logged_in" to "cookie_value")

        // When
        val result = WordPressCookieAuthenticator.AuthResult.Success(cookies)

        // Then
        assertEquals(cookies, result.cookies)
        assertEquals("cookie_value", result.cookies["wordpress_logged_in"])
    }

    @Test
    fun `AuthResult Failure should contain error message`() {
        // Given
        val errorMessage = "Authentication failed"

        // When
        val result = WordPressCookieAuthenticator.AuthResult.Failure(errorMessage)

        // Then
        assertEquals(errorMessage, result.error)
    }

    @Test
    fun `authenticator should be created successfully`() {
        // Test that the WordPressCookieAuthenticator can be instantiated
        // This indirectly tests that companion object constants are accessible
        // since the class constructor would fail if they weren't properly defined
        val testAuthenticator = WordPressCookieAuthenticator(okHttpClient, coroutineScope)
        assertTrue(testAuthenticator::class.java == WordPressCookieAuthenticator::class.java)
    }

    @Test
    fun `AuthCallback interface can be implemented`() {
        // Test that the callback interface can be implemented and used
        var resultReceived: WordPressCookieAuthenticator.AuthResult? = null
        
        val callback = object : WordPressCookieAuthenticator.AuthCallback {
            override fun onResult(result: WordPressCookieAuthenticator.AuthResult) {
                resultReceived = result
            }
        }
        
        // Simulate calling the callback
        val testResult = WordPressCookieAuthenticator.AuthResult.Failure("Test error")
        callback.onResult(testResult)
        
        // Then verify it worked
        assertEquals(testResult, resultReceived)
    }
}