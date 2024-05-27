package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.audio.RecordingUpdate
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class VoiceToContentViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var voiceToContentFeatureUtils: VoiceToContentFeatureUtils

    @Mock
    lateinit var voiceToContentUseCase: VoiceToContentUseCase

    @Mock
    lateinit var recordingUseCase: RecordingUseCase

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    private lateinit var viewModel: VoiceToContentViewModel

    private lateinit var uiState: MutableList<VoiceToContentResult>

    @Before
    fun setup() {
        // Mock the recording updates to return a non-null flow before ViewModel instantiation
        whenever(recordingUseCase.recordingUpdates()).thenReturn(createRecordingUpdateFlow())

        viewModel = VoiceToContentViewModel(
            testDispatcher(),
            voiceToContentFeatureUtils,
            voiceToContentUseCase,
            selectedSiteRepository,
            recordingUseCase
        )

        uiState = mutableListOf()
        viewModel.uiState.observeForever { event ->
            event?.let { result ->
                uiState.add(result)
            }
        }
    }

    // Helper function to create a consistent flow
    private fun createRecordingUpdateFlow() = flow {
        emit(RecordingUpdate(0, 0, false))
        // You can add more emits to simulate different recording states if needed
    }

    @Test
    fun `when voice to content is enabled, then executeVoiceToContent invokes use case`() = runTest {
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(true)
        val dummyFile = File("dummy_path")

        viewModel.executeVoiceToContent(dummyFile)

        verify(voiceToContentUseCase).execute(site, dummyFile)
    }

    @Test
    fun `when voice to content is disabled, then executeVoiceToContent does not invoke use case`() = runTest {
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(false)
        val dummyFile = File("dummy_path")

        viewModel.executeVoiceToContent(dummyFile)

        verifyNoInteractions(voiceToContentUseCase)
    }

    @Test
    fun `when stopRecording is called and file is null, then posts error state`() = runTest {
        whenever(recordingUseCase.stopRecording()).thenReturn(null)

        viewModel.stopRecording()

        val expectedState = VoiceToContentResult(isError = true)
        assertThat(uiState.first()).isEqualTo(expectedState)
    }

    @Test
    fun `when startRecording is called, then recordingUseCase starts recording`() {
        viewModel.startRecording()

        verify(recordingUseCase).startRecording()
    }

    @Test
    fun `when stopRecording is called and file is not null, then executeVoiceToContent is called`() = runTest {
        val dummyFile = File("dummy_path")
        val site = SiteModel().apply { id = 1 }
        whenever(recordingUseCase.stopRecording()).thenReturn(dummyFile)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(true)

        viewModel.stopRecording()

        verify(voiceToContentUseCase).execute(site, dummyFile)
    }
}

