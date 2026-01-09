package org.wordpress.android.ui.accounts.login.applicationpassword

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
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.BuildConfigWrapper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ApplicationPasswordAutoAuthDialogViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var experimentalFeatures: ExperimentalFeatures

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
            wpApiClientProvider,
            applicationPasswordLoginHelper,
            buildConfigWrapper,
            appLogWrapper,
            experimentalFeatures
        )
    }

    @Test
    fun `createApplicationPassword enables experimental feature at start`() = runTest {
        // Given
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE))
            .thenReturn(false)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(any()))
            .thenReturn(testAuthUrl)
        val testException = RuntimeException("API client creation failed")
        whenever(wpApiClientProvider.getWpApiClientCookiesNonceAuthentication(eq(testSite)))
            .doThrow(testException)

        // When
        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(testSite)
            cancelAndIgnoreRemainingEvents()
        }

        // Then
        verify(experimentalFeatures).setEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, true)
    }

    @Test
    fun `createApplicationPassword does not enable experimental feature if already enabled`() = runTest {
        // Given
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE))
            .thenReturn(true)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(any()))
            .thenReturn(testAuthUrl)
        val testException = RuntimeException("API client creation failed")
        whenever(wpApiClientProvider.getWpApiClientCookiesNonceAuthentication(eq(testSite)))
            .doThrow(testException)

        // When
        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(testSite)
            cancelAndIgnoreRemainingEvents()
        }

        // Then
        verify(experimentalFeatures, never()).setEnabled(any(), any())
    }

    @Test
    fun `createApplicationPassword with exception during API call falls back to manual login`() = runTest {
        // Given
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE))
            .thenReturn(true)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(testSite.url))
            .thenReturn(testAuthUrl)
        val testException = RuntimeException("API client creation failed")
        whenever(wpApiClientProvider.getWpApiClientCookiesNonceAuthentication(eq(testSite)))
            .doThrow(testException)

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.isLoading.test {
                // Initially not loading
                assertFalse(awaitItem())

                viewModel.createApplicationPassword(testSite)

                // Should become loading
                assertTrue(awaitItem())

                // Should stop loading even when exception occurs
                assertFalse(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            // Should emit fallback event with auth URL
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.FallbackToManualLogin(testAuthUrl),
                navigationEvent
            )

            // Should log error with exception message
            verify(appLogWrapper, times(1)).e(any(), any())

            // Should NOT store credentials
            verify(applicationPasswordLoginHelper, never()).storeApplicationPasswordCredentialsFrom(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword with blank username falls back to manual login`() = runTest {
        // Given
        val invalidSite = SiteModel().apply {
            url = "https://example.com"
            username = ""
            password = "testpass123"
        }
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE))
            .thenReturn(true)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(invalidSite.url))
            .thenReturn(testAuthUrl)

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            // Should emit fallback event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.FallbackToManualLogin(testAuthUrl),
                navigationEvent
            )

            // Should log error
            verify(appLogWrapper, times(1)).e(any(), any())

            // Should NOT make API call
            verify(wpApiClientProvider, never()).getWpApiClientCookiesNonceAuthentication(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword with blank password falls back to manual login`() = runTest {
        // Given
        val invalidSite = SiteModel().apply {
            url = "https://example.com"
            username = "testuser"
            password = ""
        }
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE))
            .thenReturn(true)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(invalidSite.url))
            .thenReturn(testAuthUrl)

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            // Should emit fallback event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.FallbackToManualLogin(testAuthUrl),
                navigationEvent
            )

            // Should log error
            verify(appLogWrapper, times(1)).e(any(), any())

            // Should NOT make API call
            verify(wpApiClientProvider, never()).getWpApiClientCookiesNonceAuthentication(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword emits Error when fallback also fails`() = runTest {
        // Given
        val invalidSite = SiteModel().apply {
            url = "https://example.com"
            username = ""
            password = "testpass123"
        }
        whenever(experimentalFeatures.isEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE))
            .thenReturn(true)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(any()))
            .doThrow(RuntimeException("Failed to get auth URL"))

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            // Should emit error event when fallback also fails
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Error,
                navigationEvent
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}
