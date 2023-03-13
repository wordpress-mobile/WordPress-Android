package org.wordpress.android.ui.sitecreation.progress

import android.os.Bundle
import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atMost
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.notNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.sitecreation.RESULT_NOT_IN_LOCAL_DB
import org.wordpress.android.ui.sitecreation.SERVICE_ERROR
import org.wordpress.android.ui.sitecreation.SERVICE_SUCCESS
import org.wordpress.android.ui.sitecreation.SiteCreationResult
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotCreated
import org.wordpress.android.ui.sitecreation.SiteCreationResult.NotInLocalDb
import org.wordpress.android.ui.sitecreation.SiteCreationState
import org.wordpress.android.ui.sitecreation.URL
import org.wordpress.android.ui.sitecreation.domains.DomainModel
import org.wordpress.android.ui.sitecreation.misc.SiteCreationTracker
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Error.ConnectionError
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Error.GenericError
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.SiteProgressUiState.Loading
import org.wordpress.android.ui.sitecreation.progress.SiteProgressViewModel.StartServiceData
import org.wordpress.android.ui.sitecreation.theme.defaultTemplateSlug
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.UrlUtilsWrapper
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteProgressViewModelTest : BaseUnitTest() {
    private var networkUtils = mock<NetworkUtilsWrapper>()
    private var urlUtils = mock<UrlUtilsWrapper>()
    private var tracker = mock<SiteCreationTracker>()

    private val uiStateObserver = mock<Observer<SiteProgressUiState>>()
    private val startServiceObserver = mock<Observer<StartServiceData>>()
    private val onHelpClickedObserver = mock<Observer<Unit>>()
    private val onCancelWizardClickedObserver = mock<Observer<SiteCreationResult>>()
    private val onSiteCreationCompletedObserver = mock<Observer<SiteCreationResult>>()

    private val bundle = mock<Bundle>()

    private lateinit var viewModel: SiteProgressViewModel

    @Before
    fun setUp() {
        viewModel = SiteProgressViewModel(
            networkUtils = networkUtils,
            urlUtils = urlUtils,
            tracker = tracker,
            bgDispatcher = testDispatcher(),
            mainDispatcher = testDispatcher()
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.startCreateSiteService.observeForever(startServiceObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        viewModel.onCancelWizardClicked.observeForever(onCancelWizardClickedObserver)
        viewModel.onSiteCreationCompleted.observeForever(onSiteCreationCompletedObserver)

        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
        whenever(urlUtils.removeScheme(URL)).thenReturn(URL)
    }

    @Test
    fun `on start shows progress`() = test {
        startViewModel()
        assertIs<SiteProgressUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows progress when restoring from SiteNotCreated`() {
        startViewModel(NotCreated)
        assertIs<SiteProgressUiState>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows progress when restoring from SiteNotInLocalDb`() {
        startViewModel(RESULT_NOT_IN_LOCAL_DB)
        assertIs<Loading>(viewModel.uiState.value)
    }

    @Test
    fun `on start emits service event`() = test {
        startViewModel()
        assertNotNull(viewModel.startCreateSiteService.value)
    }

    @Test
    fun `on start emits service event when restoring from SiteNotCreated`() {
        startViewModel(NotCreated)
        assertNotNull(viewModel.startCreateSiteService.value)
    }

    @Test
    fun `on start emits completion when restoring from NotInLocalDb`() {
        startViewModel(RESULT_NOT_IN_LOCAL_DB)
        verify(onSiteCreationCompletedObserver).onChanged(isA<NotInLocalDb>())
    }

    @Test
    fun `on start shows error when network is not available`() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        startViewModel()
        advanceUntilIdle()
        assertIs<ConnectionError>(viewModel.uiState.value)
    }

    @Test
    fun `on start shows first loading text without animation`() = test {
        startViewModel()
        verify(uiStateObserver).onChanged(check<Loading> { !it.animate })
    }

    @Test
    fun `on start changes the loading text with delayed animation`() = test {
        startViewModel()
        advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
        verify(uiStateObserver).onChanged(check<Loading> { it.animate })
    }

    @Test
    fun `on start changes the loading text with delayed animation only 4 times`() = test {
        startViewModel()
        val captor = argumentCaptor<Loading>()
        (1..9).forEach {
            verify(uiStateObserver, atMost(it)).onChanged(captor.capture())
            advanceTimeBy(LOADING_STATE_TEXT_ANIMATION_DELAY)
        }
        assertThat(captor.allValues.distinctBy { it.loadingTextResId }).hasSize(4)
    }

    @Test
    fun `on retry click emits service event with the previous result`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        viewModel.retry()
        assertEquals(viewModel.startCreateSiteService.value?.previousState, SERVICE_ERROR.payload)
    }

    @Test
    fun `on help click is propagate`() {
        startViewModel()
        viewModel.onHelpClicked()
        verify(onHelpClickedObserver).onChanged(isNull())
    }

    @Test
    fun `on cancel wizard click is propagated`() {
        startViewModel()
        viewModel.onCancelWizardClicked()
        assertEquals(viewModel.onCancelWizardClicked.value, NotCreated)
    }

    @Test
    fun `on write to bundle saves result`() {
        startViewModel()
        viewModel.writeToBundle(bundle)
        verify(bundle).putParcelable(eq(KEY_RESULT), notNull())
    }

    @Test
    fun `on service success emits completion`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_SUCCESS)
        verify(onSiteCreationCompletedObserver).onChanged(isA<NotInLocalDb>())
    }

    @Test
    fun `on service failure shows generic error`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        verify(uiStateObserver).onChanged(eq(GenericError))
    }

    @Test
    fun `on service failure shows error`() {
        startViewModel()
        viewModel.onSiteCreationServiceStateUpdated(SERVICE_ERROR)
        assertIs<GenericError>(viewModel.uiState.value)
    }

    // region Helpers

    private fun startViewModel(restoredState: SiteCreationResult? = null) {
        viewModel.start(
            SiteCreationState(
                segmentId = 1,
                siteDesign = defaultTemplateSlug,
                domain = DomainModel(URL, true, "", 1)
            ),
            bundle.apply {
                restoredState?.let { whenever(getParcelable<SiteCreationResult>(KEY_RESULT)).thenReturn(it) }
            }
        )
    }

    // endregion
}
