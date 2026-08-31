package org.wordpress.android.ui.accounts.login.applicationpassword

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.viewmodel.ResourceProvider
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class LoginSiteApplicationPasswordViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: LoginSiteApplicationPasswordViewModel

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = LoginSiteApplicationPasswordViewModel(applicationPasswordLoginHelper, resourceProvider)
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

        job.cancel() // Clean up the collector job
    }

    @Test
    fun `Given a WP_com site, when running discovery, then wpComDetected emits the url`() = test {
        // Given
        val siteUrl = "https://example.com"
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.WpComSite)

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
    fun `Given discovery fails, when running discovery, then the library message is shown as-is`() = test {
        // Given
        val siteUrl = "https://example.com"
        val errorMessage = "Found a site but failed to read its API configuration."
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
            .thenReturn(ApplicationPasswordLoginHelper.DiscoveryResult.Failed(errorMessage))

        var wpComDetected = false
        val wpComJob = launch { viewModel.wpComDetected.first { wpComDetected = true; true } }

        var collectedUrl: String? = null
        val discoveryJob = launch { viewModel.discoveryURL.first { collectedUrl = it; true } }

        // When
        viewModel.runApiDiscovery(siteUrl)
        advanceUntilIdle()

        // Then — the specific reason survives, and no URL is emitted for the UI to misread as a
        // failure signal (that empty emission is what used to overwrite this message).
        assertEquals(errorMessage, viewModel.errorMessage.value)
        assertNull(collectedUrl)
        assertEquals(false, wpComDetected)

        wpComJob.cancel()
        discoveryJob.cancel()
    }

    @Test
    fun `Given a private site, when running discovery, then the private-site explanation is shown`() = test {
        // The library reports a private site as a generic "couldn't read the API configuration",
        // which reads as though the site is broken. Name the actual cause instead.
        val siteUrl = "https://example.com"
        val privateSiteMessage = "This site is private, so we can't read its settings to sign you in."
        whenever(resourceProvider.getString(R.string.application_password_private_site_error))
            .thenReturn(privateSiteMessage)
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
            .thenReturn(
                ApplicationPasswordLoginHelper.DiscoveryResult.Failed(
                    userFacingMessage = "Found a site but failed to read its API configuration.",
                    reason = ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason.PrivateSite,
                )
            )

        viewModel.runApiDiscovery(siteUrl)
        advanceUntilIdle()

        assertEquals(privateSiteMessage, viewModel.errorMessage.value)
        assertEquals(false, viewModel.loadingStateFlow.value)
    }
}
