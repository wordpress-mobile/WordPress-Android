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
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class LoginSiteApplicationPasswordViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    private lateinit var viewModel: LoginSiteApplicationPasswordViewModel

    // Test dispatcher for coroutines
    private val testDispatcher = StandardTestDispatcher(TestCoroutineScheduler())

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = LoginSiteApplicationPasswordViewModel(applicationPasswordLoginHelper)
    }

    @Test
    fun `Given ViewModel, when calling runApiDiscovery, then `() = test {
        // Given
        val siteUrl = "https.example.com"
        val expectedDiscoveryUrl = "https://example.com/wp-json/wp/v2/application-passwords/authorization"
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl))
            .thenReturn(expectedDiscoveryUrl)

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
}
