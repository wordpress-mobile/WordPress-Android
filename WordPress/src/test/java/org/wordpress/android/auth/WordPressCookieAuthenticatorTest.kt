package org.wordpress.android.auth

import kotlinx.coroutines.CoroutineDispatcher
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
import org.wordpress.android.auth.WordPressCookieAuthenticator
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WordPressCookieAuthenticatorTest {
    @Mock
    private lateinit var mockCookieJar: CookieJar

    private lateinit var okHttpClient: OkHttpClient
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var testScope: TestScope
    private lateinit var authenticator: WordPressCookieAuthenticator

    @Before
    fun setUp() {
        okHttpClient = OkHttpClient.Builder()
            .cookieJar(mockCookieJar)
            .build()

        testScope = TestScope()
        testDispatcher = StandardTestDispatcher(testScope.testScheduler)
        authenticator = WordPressCookieAuthenticator(okHttpClient, testDispatcher)
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
}
