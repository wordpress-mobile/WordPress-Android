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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ApplicationPasswordDialogViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    @Mock
    lateinit var appLogWrapper: AppLogWrapper

    @Mock
    lateinit var experimentalFeatures: ExperimentalFeatures

    private lateinit var viewModel: ApplicationPasswordDialogViewModel

    private val testAuthUrl = "https://example.com/wp-admin/authorize-application.php"
    private val testCompleteAuthUrl = "https://example.com/wp-admin/authorize-application.php?" +
        "app_name=android-jetpack-client&success_url=jetpack://app-pass-authorize"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ApplicationPasswordDialogViewModel(
            applicationPasswordLoginHelper,
            appLogWrapper,
            experimentalFeatures
        )
    }

    @Test
    fun `onDialogConfirmed with valid URL processes successfully and emits NavigateToLogin`() = runTest {
        // Given
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(testAuthUrl)))
            .thenReturn(testCompleteAuthUrl)

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.isLoading.test {
                // Initially not loading
                assertFalse(awaitItem())

                viewModel.onDialogConfirmed(testAuthUrl)

                // Should become loading
                assertTrue(awaitItem())

                // Should stop loading
                assertFalse(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            // Should emit navigation event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordDialogViewModel.NavigationEvent.NavigateToLogin(testCompleteAuthUrl),
                navigationEvent
            )

            verify(applicationPasswordLoginHelper, times(1)).getAuthorizationUrlComplete(eq(testAuthUrl))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDialogConfirmed with empty authenticationUrl emits ShowError without calling helper`() = runTest {
        // When & Then
        viewModel.navigationEvent.test {
            viewModel.onDialogConfirmed("")

            // Should emit error event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordDialogViewModel.NavigationEvent.ShowError,
                navigationEvent
            )

            // Should NOT call the helper since we return early
            verify(applicationPasswordLoginHelper, times(0)).getAuthorizationUrlComplete(any())
            // Should log a warning about empty URL
            verify(appLogWrapper, times(1)).w(any(), any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDialogConfirmed with helper returning empty URL emits ShowError`() = runTest {
        // Given
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(testAuthUrl)))
            .thenReturn("")

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.isLoading.test {
                // Initially not loading
                assertFalse(awaitItem())

                viewModel.onDialogConfirmed(testAuthUrl)

                // Should become loading
                assertTrue(awaitItem())

                // Should stop loading
                assertFalse(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            // Should emit error event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordDialogViewModel.NavigationEvent.ShowError,
                navigationEvent
            )

            verify(applicationPasswordLoginHelper, times(1)).getAuthorizationUrlComplete(eq(testAuthUrl))
            verify(appLogWrapper, times(1)).e(any(), any())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onDialogConfirmed with helper throwing exception emits ShowError`() = runTest {
        // Given
        val testException = RuntimeException("API discovery failed")
        whenever(applicationPasswordLoginHelper.getAuthorizationUrlComplete(eq(testAuthUrl)))
            .doThrow(testException)

        // When & Then
        viewModel.navigationEvent.test {
            viewModel.isLoading.test {
                // Initially not loading
                assertFalse(awaitItem())

                viewModel.onDialogConfirmed(testAuthUrl)

                // Should become loading
                assertTrue(awaitItem())

                // Should stop loading even when exception occurs
                assertFalse(awaitItem())

                cancelAndIgnoreRemainingEvents()
            }

            // Should emit error event
            val navigationEvent = awaitItem()
            assertEquals(
                ApplicationPasswordDialogViewModel.NavigationEvent.ShowError,
                navigationEvent
            )

            verify(applicationPasswordLoginHelper, times(1)).getAuthorizationUrlComplete(eq(testAuthUrl))
            verify(appLogWrapper, times(1)).e(any(), any())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
