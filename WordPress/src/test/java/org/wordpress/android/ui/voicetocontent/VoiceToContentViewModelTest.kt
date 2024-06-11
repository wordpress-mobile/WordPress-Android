package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.viewmodel.ContextProvider
import kotlin.test.Test

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

    @Mock
    lateinit var prepareVoiceToContentUseCase: PrepareVoiceToContentUseCase

    @Mock
    lateinit var contextProvider: ContextProvider

    private lateinit var viewModel: VoiceToContentViewModel

    // private lateinit var uiState: MutableList<VoiceToContentResult>

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
        // whenever(recordingUseCase.recordingUpdates()).thenReturn(createRecordingUpdateFlow())

        viewModel = VoiceToContentViewModel(
            testDispatcher(),
            voiceToContentFeatureUtils,
            voiceToContentUseCase,
            selectedSiteRepository,
            recordingUseCase,
            contextProvider,
            prepareVoiceToContentUseCase
        )
//        uiState = mutableListOf()
//        viewModel.uiState.observeForever { event ->
//            event?.let { result ->
//                uiState.add(result)
//            }
//        }
    }

    //
//    // Helper function to create a consistent flow
//    private fun createRecordingUpdateFlow() = flow {
//        emit(RecordingUpdate(0, 0, false))
//    }
//
    @Test
    fun `when site is null, then execute posts error state `() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.start()

        verifyNoInteractions(prepareVoiceToContentUseCase)
    }
}
