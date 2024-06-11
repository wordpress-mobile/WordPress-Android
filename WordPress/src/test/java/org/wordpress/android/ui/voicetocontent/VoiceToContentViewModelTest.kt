package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Before
import org.mockito.Mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.audio.RecordingUpdate
import org.wordpress.android.viewmodel.ContextProvider
import kotlin.test.Test

@ExperimentalCoroutinesApi
class VoiceToContentViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var voiceToContentFeatureUtils: VoiceToContentFeatureUtils

    @Mock
    lateinit var voiceToContentUseCase: VoiceToContentUseCase

    @Mock
    lateinit var recordingUseCase: RecordingUseCase

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var prepareVoiceToContentUseCase: PrepareVoiceToContentUseCase

    @Mock
    lateinit var contextProvider: ContextProvider

    private lateinit var viewModel: VoiceToContentViewModel

    private var uiStateChanges = mutableListOf<VoiceToContentUiState>()
    private val uiState
        get() = viewModel.state.value

    private fun <T> testUiStateChanges(
        block: suspend CoroutineScope.() -> T
    ) {
        test {
            uiStateChanges.clear()
            val job = launch(testDispatcher()) {
                viewModel.state.toList(uiStateChanges)
            }
            this.block()
            job.cancel()
        }
    }
    /* private val jetpackAIAssistantFeature = JetpackAIAssistantFeature(
        hasFeature = true,
        isOverLimit = false,
        requestsCount = 0,
        requestsLimit = 0,
        usagePeriod = null,
        siteRequireUpgrade = true,
        upgradeType = "upgradeType",
        currentTier = null,
        nextTier = null,
        tierPlans = emptyList(),
        tierPlansEnabled = false,
        costs = null
    )*/

    @Before
    fun setup() {
        // Mock the recording updates to return a non-null flow before ViewModel instantiation
        whenever(recordingUseCase.recordingUpdates()).thenReturn(createRecordingUpdateFlow())

        viewModel = VoiceToContentViewModel(
            testDispatcher(),
            voiceToContentFeatureUtils,
            voiceToContentUseCase,
            selectedSiteRepository,
            recordingUseCase,
            contextProvider,
            prepareVoiceToContentUseCase
        )
    }

    // Helper function to create a consistent flow
    private fun createRecordingUpdateFlow() = flow {
        emit(RecordingUpdate(0, 0, false))
    }

    @Test
    fun `when site is null, then execute posts error state `() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.start()

        verifyNoInteractions(prepareVoiceToContentUseCase)
    }
}
