package org.wordpress.android.ui.accounts.login.applicationpassword

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.fluxc.store.SiteStore.SiteError
import org.wordpress.android.fluxc.store.SiteStore.SiteErrorType
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class LoginSiteApplicationPasswordViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    private lateinit var fetchConnectSiteInfoUseCase: FetchConnectSiteInfoUseCase

    private lateinit var viewModel: LoginSiteApplicationPasswordViewModel

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = LoginSiteApplicationPasswordViewModel(
            applicationPasswordLoginHelper,
            fetchConnectSiteInfoUseCase
        )
    }

    @Test
    fun `Given ViewModel, when calling runApiDiscovery, then `() = test {
        // Given
        val siteUrl = "https.example.com"
        val expectedDiscoveryUrl = "https://example.com/wp-json/wp/v2/application-passwords/authorization"
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Authorized(expectedDiscoveryUrl))

        // A collector for the discoveryURL SharedFlow
        var collectedUrl: String? = null
        val job = launch { // Launch in the testScope to collect emissions
            viewModel.discoveryURL.first { url -> // first will collect the first emission and cancel
                collectedUrl = url
                true
            }
        }

        // When
        viewModel.runApiDiscovery(siteUrl)
        // Advance past the initial loading state set and the coroutine launch
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(applicationPasswordLoginHelper).getAuthorizationUrlComplete(siteUrl)
        assertEquals(expectedDiscoveryUrl, collectedUrl)
        assertEquals(false, viewModel.loadingStateFlow.value)
        verify(fetchConnectSiteInfoUseCase, never()).fetchConnectSiteInfo(siteUrl)

        job.cancel() // Clean up the collector job
    }

    @Test
    fun `Given discovery fails on a WP_com site, when running discovery, then wpComDetected emits`() = test {
        // Given
        val siteUrl = "https://example.com"
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Failed("error"))
        whenever(fetchConnectSiteInfoUseCase.fetchConnectSiteInfo(siteUrl))
            .thenReturn(ConnectSiteInfoPayload(url = siteUrl, isWPCom = true))

        var detectedUrl: String? = null
        val job = launch {
            viewModel.wpComDetected.first { url ->
                detectedUrl = url
                true
            }
        }

        // When
        viewModel.runApiDiscovery(siteUrl)
        advanceUntilIdle()

        // Then
        assertEquals(siteUrl, detectedUrl)
        assertNull(viewModel.errorMessage.value)
        assertEquals(false, viewModel.loadingStateFlow.value)

        job.cancel()
    }

    @Test
    fun `Given discovery fails on a non-WP_com site, when running discovery, then generic error shown`() = test {
        // Given
        val siteUrl = "https://example.com"
        val errorMessage = "not supported"
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Failed(errorMessage))
        whenever(fetchConnectSiteInfoUseCase.fetchConnectSiteInfo(siteUrl))
            .thenReturn(ConnectSiteInfoPayload(url = siteUrl, isWPCom = false))

        var wpComDetected = false
        val wpComJob = launch { viewModel.wpComDetected.first { wpComDetected = true; true } }

        var collectedUrl: String? = null
        val discoveryJob = launch { viewModel.discoveryURL.first { collectedUrl = it; true } }

        // When
        viewModel.runApiDiscovery(siteUrl)
        advanceUntilIdle()

        // Then
        assertEquals(errorMessage, viewModel.errorMessage.value)
        assertEquals("", collectedUrl)
        assertEquals(false, wpComDetected)

        wpComJob.cancel()
        discoveryJob.cancel()
    }

    @Test
    fun `Given discovery fails and connect-site-info errors, when running discovery, then generic error shown`() =
        test {
            // Given
            val siteUrl = "https://example.com"
            val errorMessage = "not supported"
            whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
                .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Failed(errorMessage))
            whenever(fetchConnectSiteInfoUseCase.fetchConnectSiteInfo(siteUrl))
                .thenReturn(ConnectSiteInfoPayload(siteUrl, SiteError(SiteErrorType.GENERIC_ERROR)))

            var wpComDetected = false
            val wpComJob = launch { viewModel.wpComDetected.first { wpComDetected = true; true } }

            var collectedUrl: String? = null
            val discoveryJob = launch { viewModel.discoveryURL.first { collectedUrl = it; true } }

            // When
            viewModel.runApiDiscovery(siteUrl)
            advanceUntilIdle()

            // Then
            assertEquals(errorMessage, viewModel.errorMessage.value)
            assertEquals("", collectedUrl)
            assertEquals(false, wpComDetected)

            wpComJob.cancel()
            discoveryJob.cancel()
        }

    @Test
    fun `Given discovery fails and connect-site-info times out, when running discovery, then generic error shown`() =
        test {
            // Given
            val siteUrl = "https://example.com"
            val errorMessage = "not supported"
            whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
                .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Failed(errorMessage))
            whenever(fetchConnectSiteInfoUseCase.fetchConnectSiteInfo(siteUrl))
                .doSuspendableAnswer { awaitCancellation() }

            var wpComDetected = false
            val wpComJob = launch { viewModel.wpComDetected.first { wpComDetected = true; true } }

            var collectedUrl: String? = null
            val discoveryJob = launch { viewModel.discoveryURL.first { collectedUrl = it; true } }

            // When
            viewModel.runApiDiscovery(siteUrl)
            advanceUntilIdle()

            // Then
            assertEquals(errorMessage, viewModel.errorMessage.value)
            assertEquals("", collectedUrl)
            assertEquals(false, wpComDetected)
            assertEquals(false, viewModel.loadingStateFlow.value)

            wpComJob.cancel()
            discoveryJob.cancel()
        }
}
