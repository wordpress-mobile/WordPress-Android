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

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ApplicationPasswordAutoAuthDialogViewModel(
            wpApiClientProvider,
            applicationPasswordLoginHelper,
            buildConfigWrapper,
            appLogWrapper
        )
    }

    @Test
    fun `createApplicationPassword with exception during API call emits Error`() = runTest {
        // Given
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

            // Should emit error event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Error,
                navigationEvent
            )

            // Should log error with exception message
            verify(appLogWrapper, times(1)).e(any(), any())

            // Should NOT store credentials
            verify(applicationPasswordLoginHelper, times(0)).storeApplicationPasswordCredentialsFrom(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword with blank username emits Error`() = runTest {
        // Given
        val invalidSite = testSite.apply { username = "" }

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            // Should emit error event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Error,
                navigationEvent
            )

            // Should log error
            verify(appLogWrapper, times(1)).e(any(), any())

            // Should NOT make API call
            verify(wpApiClientProvider, times(0)).getWpApiClientCookiesNonceAuthentication(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `createApplicationPassword with blank password emits Error`() = runTest {
        // Given
        val invalidSite = testSite.apply { password = "" }

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.createApplicationPassword(invalidSite)

            // Should emit error event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordAutoAuthDialogViewModel.NavigationEvent.Error,
                navigationEvent
            )

            // Should log error
            verify(appLogWrapper, times(1)).e(any(), any())

            // Should NOT make API call
            verify(wpApiClientProvider, times(0)).getWpApiClientCookiesNonceAuthentication(any())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
