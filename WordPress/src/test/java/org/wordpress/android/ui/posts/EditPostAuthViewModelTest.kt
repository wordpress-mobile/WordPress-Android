package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.auth.WordPressCookieAuthenticator
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.network.rest.wpcom.site.PrivateAtomicCookie
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.model.AccountModel
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class EditPostAuthViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var wordPressCookieAuthenticator: WordPressCookieAuthenticator

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var userAgent: UserAgent

    @Mock
    private lateinit var account: AccountModel

    private lateinit var viewModel: EditPostAuthViewModel

    private fun createMockPrivateAtomicCookie(
        exists: Boolean = true,
        isExpired: Boolean = false,
        name: String = "atomic_cookie",
        value: String = "cookie_value",
        domain: String = ".example.wordpress.com"
    ): PrivateAtomicCookie {
        val mockCookie = org.mockito.kotlin.mock<PrivateAtomicCookie>()
        whenever(mockCookie.exists()).thenReturn(exists)
        if (exists && !isExpired) {
            whenever(mockCookie.isExpired()).thenReturn(isExpired)
            whenever(mockCookie.getName()).thenReturn(name)
            whenever(mockCookie.getValue()).thenReturn(value)
            whenever(mockCookie.getDomain()).thenReturn(domain)
        }
        return mockCookie
    }

    @Before
    fun setUp() {
        whenever(accountStore.account).thenReturn(account)
        whenever(account.userName).thenReturn("testuser")
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(userAgent.webViewUserAgent).thenReturn("WordPress/Test")

        viewModel = EditPostAuthViewModel(
            testDispatcher(),
            wordPressCookieAuthenticator,
            accountStore,
            userAgent
        )
    }

    @Test
    fun `fetchWpComCookies posts Loading then Success states`() = test {
        // Given
        val stateChanges = mutableListOf<EditPostAuthViewModel.WpComCookieAuthState>()
        val expectedCookies = mapOf("wordpress_logged_in_123" to "cookie_value")

        whenever(wordPressCookieAuthenticator.authenticateForCookies(org.mockito.kotlin.any()))
            .thenReturn(WordPressCookieAuthenticator.AuthResult.Success(expectedCookies))

        // Observe state changes
        viewModel.wpComCookieAuthState.observeForever { state ->
            stateChanges.add(state)
        }

        // When
        viewModel.fetchWpComCookies()
        advanceUntilIdle()

        // Then - Should have seen Loading then Success states
        assertTrue(stateChanges.size >= 2)
        assertTrue(stateChanges[stateChanges.size - 2] is EditPostAuthViewModel.WpComCookieAuthState.Loading)
        val finalState = stateChanges.last()
        assertTrue(finalState is EditPostAuthViewModel.WpComCookieAuthState.Success)
        assertEquals(expectedCookies, finalState.cookies)
    }

    @Test
    fun `fetchWpComCookies transitions to Success on successful authentication`() = test {
        // Given
        val expectedCookies = mapOf("wordpress_logged_in_123" to "cookie_value")
        whenever(wordPressCookieAuthenticator.authenticateForCookies(org.mockito.kotlin.any()))
            .thenReturn(WordPressCookieAuthenticator.AuthResult.Success(expectedCookies))

        // When
        viewModel.fetchWpComCookies()
        advanceUntilIdle()

        // Then
        val state = viewModel.wpComCookieAuthState.value
        assertTrue(state is EditPostAuthViewModel.WpComCookieAuthState.Success)
        assertEquals(expectedCookies, state.cookies)
    }

    @Test
    fun `fetchWpComCookies transitions to Error on authentication failure`() = test {
        // Given
        val errorMessage = "Authentication failed"
        whenever(wordPressCookieAuthenticator.authenticateForCookies(org.mockito.kotlin.any()))
            .thenReturn(WordPressCookieAuthenticator.AuthResult.Failure(errorMessage))

        // When
        viewModel.fetchWpComCookies()
        advanceUntilIdle()

        // Then
        val state = viewModel.wpComCookieAuthState.value
        assertTrue(state is EditPostAuthViewModel.WpComCookieAuthState.Error)
        assertEquals(errorMessage, state.message)
    }

    @Test
    fun `getCookiesForPrivateSites returns empty for non-private sites`() = test {
        // Given
        val siteModel = SiteModel().apply {
            setIsWPCom(true)
            setIsPrivate(false)
        }

        // When
        val cookies = viewModel.getCookiesForPrivateSites(siteModel, createMockPrivateAtomicCookie())

        // Then
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `getCookiesForPrivateSites returns empty for non-WPCom sites`() = test {
        // Given
        val siteModel = SiteModel().apply {
            setIsWPCom(false)
            setIsPrivate(true)
        }

        // When
        val cookies = viewModel.getCookiesForPrivateSites(siteModel, createMockPrivateAtomicCookie())

        // Then
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `getCookiesForPrivateSites returns atomic cookies for atomic sites`() = test {
        // Given
        val siteModel = SiteModel().apply {
            setIsWPCom(true)
            setIsWPComAtomic(true)
            setIsPrivate(true)
            url = "https://example.wordpress.com"
        }
        val atomicCookie = createMockPrivateAtomicCookie()

        // When
        val cookies = viewModel.getCookiesForPrivateSites(siteModel, atomicCookie)

        // Then
        assertEquals(1, cookies.size)
        assertTrue(cookies.containsKey(siteModel.url))
        assertTrue(cookies[siteModel.url]?.contains("atomic_cookie=cookie_value") == true)
    }

    @Test
    fun `getCookiesForPrivateSites returns empty for atomic sites when cookie does not exist`() = test {
        // Given
        val siteModel = SiteModel().apply {
            setIsWPCom(true)
            setIsWPComAtomic(true)
            setIsPrivate(true)
        }
        val atomicCookie = createMockPrivateAtomicCookie(exists = false)

        // When
        val cookies = viewModel.getCookiesForPrivateSites(siteModel, atomicCookie)

        // Then
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `getCookiesForPrivateSites returns wpcom cookies for simple private sites after authentication`() =
        test {
            // Given
            val siteModel = SiteModel().apply {
                setIsWPCom(true)
                setIsWPComAtomic(false)
                setIsPrivate(true)
                url = "https://example.wordpress.com"
            }
            val expectedCookies = mapOf("wordpress_logged_in_123" to "cookie_value")
            whenever(wordPressCookieAuthenticator.authenticateForCookies(org.mockito.kotlin.any()))
                .thenReturn(WordPressCookieAuthenticator.AuthResult.Success(expectedCookies))

            // When - First authenticate
            viewModel.fetchWpComCookies()
            advanceUntilIdle()

            // Then - Get cookies should return formatted cookies
            val cookies = viewModel.getCookiesForPrivateSites(siteModel, createMockPrivateAtomicCookie())
            assertEquals(1, cookies.size)
            assertTrue(cookies.containsKey(siteModel.url))
            assertTrue(cookies[siteModel.url]?.contains("wordpress_logged_in_123=cookie_value") == true)
            assertTrue(cookies[siteModel.url]?.contains("domain=.wordpress.com") == true)
        }

    @Test
    fun `getCookiesForPrivateSites returns empty for simple sites before authentication`() = test {
        // Given
        val siteModel = SiteModel().apply {
            setIsWPCom(true)
            setIsWPComAtomic(false)
            setIsPrivate(true)
        }

        // When - Get cookies without authentication
        val cookies = viewModel.getCookiesForPrivateSites(siteModel, createMockPrivateAtomicCookie())

        // Then
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `getCookiesForPrivateSites returns empty for simple sites after authentication failure`() = test {
        // Given
        val siteModel = SiteModel().apply {
            setIsWPCom(true)
            setIsWPComAtomic(false)
            setIsPrivate(true)
        }
        whenever(wordPressCookieAuthenticator.authenticateForCookies(org.mockito.kotlin.any()))
            .thenReturn(WordPressCookieAuthenticator.AuthResult.Failure("Auth failed"))

        // When
        viewModel.fetchWpComCookies()
        advanceUntilIdle()
        val cookies = viewModel.getCookiesForPrivateSites(siteModel, createMockPrivateAtomicCookie())

        // Then
        assertTrue(cookies.isEmpty())
    }

    @Test
    fun `getCookiesForPrivateSites filters non-wordpress_logged_in cookies`() = test {
        // Given
        val siteModel = SiteModel().apply {
            setIsWPCom(true)
            setIsWPComAtomic(false)
            setIsPrivate(true)
            url = "https://example.wordpress.com"
        }
        val mixedCookies = mapOf(
            "some_other_cookie" to "other_value",
            "wordpress_logged_in_123" to "auth_value",
            "another_cookie" to "another_value"
        )
        whenever(wordPressCookieAuthenticator.authenticateForCookies(org.mockito.kotlin.any()))
            .thenReturn(WordPressCookieAuthenticator.AuthResult.Success(mixedCookies))

        // When
        viewModel.fetchWpComCookies()
        advanceUntilIdle()
        val cookies = viewModel.getCookiesForPrivateSites(siteModel, createMockPrivateAtomicCookie())

        // Then - Only wordpress_logged_in cookie should be included
        assertEquals(1, cookies.size)
        assertTrue(cookies[siteModel.url]?.contains("wordpress_logged_in_123=auth_value") == true)
        assertTrue(cookies[siteModel.url]?.contains("some_other_cookie") == false)
    }
}
