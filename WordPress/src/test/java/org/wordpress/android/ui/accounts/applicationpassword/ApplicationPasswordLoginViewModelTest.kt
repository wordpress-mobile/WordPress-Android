package org.wordpress.android.ui.accounts.applicationpassword

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ApplicationPasswordLoginViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var applicationPasswordLoginHelper: ApplicationPasswordLoginHelper

    private lateinit var viewModel: ApplicationPasswordLoginViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        viewModel = ApplicationPasswordLoginViewModel(testDispatcher(), applicationPasswordLoginHelper)
    }

    @Test
    fun `valid rawData stores credentials and emits true`() = runTest {
        // Given
        val rawData = "some valid raw data"
        whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData))
            .thenReturn(true)

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertTrue(finishedEvent, "onFinishedEvent should emit true")
            verify(applicationPasswordLoginHelper, times(1))
                .storeApplicationPasswordCredentialsFrom(rawData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `valid rawData storing fails emits true`() = runTest {
        // Given
        val rawData = "some invalid raw data"
        whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData))
            .thenReturn(false)

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertFalse(finishedEvent, "onFinishedEvent should emit false")
            verify(applicationPasswordLoginHelper, times(1))
                .storeApplicationPasswordCredentialsFrom(rawData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty rawData does not store credentials and emits true`()= runTest {
        // Given
        val rawData = ""

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertFalse(finishedEvent, "onFinishedEvent should emit false")
            verify(applicationPasswordLoginHelper, times(0))
                .storeApplicationPasswordCredentialsFrom(rawData)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `storeCredentials throws exception emits true and logs error`() = runTest {
        // Given
        val rawData = "some invalid raw data"
        whenever(applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData))
            .thenThrow(RuntimeException())

        // When
        viewModel.onFinishedEvent.test {
            viewModel.setupSite(rawData)

            // Then
            val finishedEvent = awaitItem()
            assertFalse(finishedEvent, "onFinishedEvent should emit false")
            verify(applicationPasswordLoginHelper, times(1))
                .storeApplicationPasswordCredentialsFrom(rawData)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
