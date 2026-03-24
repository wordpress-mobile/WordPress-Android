package org.wordpress.android.ui.accounts.login.applicationpassword

import android.content.Context
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.util.BuildConfigWrapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ApplicationPasswordAutoAuthDialogViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var context: Context

    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    private lateinit var viewModel: ApplicationPasswordAutoAuthDialogViewModel

    private val testSite = SiteModel().apply {
        url = "https://example.com"
        username = "testuser"
        password = "testpass123"
    }

    private val testAuthUrl = "https://example.com/wp-admin/authorize-application.php"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ApplicationPasswordAutoAuthDialogViewModel(
            context,
            wpApiClientProvider,
            applicationPasswordLoginHelper,
            buildConfigWrapper,
            appLogWrapper,
        )
    }

    @Test
    fun `createApplicationPassword with exception during API call falls back to manual login`() = runTest {
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(testSite.url))
            .thenReturn(testAuthUrl)
        val testException = RuntimeException("API client creation failed")
        whenever(wpApiClientProvider.getWpApiClientCookiesNonceAuthentication(eq(testSite)))
            .doThrow(testException)

        viewModel.navigationEvent.test {
            viewModel.isLoading.test {
                assertFalse(awaitItem())

                viewModel.createApplicationPassword(testSite)

                assertTrue(awaitItem())
                assertFalse(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.FallbackToManualLogin(testAuthUrl),
                navigationEvent
            )

            verify(appLogWrapper, times(1)).e(any(), any())
            verify(applicationPasswordLoginHelper, never()).storeApplicationPasswordCredentialsFrom(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword with blank username falls back to manual login`() = runTest {
        val invalidSite = SiteModel().apply {
            url = "https://example.com"
            username = ""
            password = "testpass123"
        }
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(invalidSite.url))
            .thenReturn(testAuthUrl)

        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.FallbackToManualLogin(testAuthUrl),
                navigationEvent
            )

            verify(appLogWrapper, times(1)).e(any(), any())
            verify(wpApiClientProvider, never()).getWpApiClientCookiesNonceAuthentication(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword with blank password falls back to manual login`() = runTest {
        val invalidSite = SiteModel().apply {
            url = "https://example.com"
            username = "testuser"
            password = ""
        }
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(invalidSite.url))
            .thenReturn(testAuthUrl)

        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.FallbackToManualLogin(testAuthUrl),
                navigationEvent
            )

            verify(appLogWrapper, times(1)).e(any(), any())
            verify(wpApiClientProvider, never()).getWpApiClientCookiesNonceAuthentication(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword emits Error when fallback also fails`() = runTest {
        val invalidSite = SiteModel().apply {
            url = "https://example.com"
            username = ""
            password = "testpass123"
        }
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(any()))
            .doThrow(RuntimeException("Failed to get auth URL"))

        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Error,
                navigationEvent
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}
