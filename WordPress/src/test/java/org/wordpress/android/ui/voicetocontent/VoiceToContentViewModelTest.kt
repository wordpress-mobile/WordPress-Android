package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class VoiceToContentViewModelTest : BaseUnitTest() {
//    @Mock
//    lateinit var voiceToContentFeatureUtils: VoiceToContentFeatureUtils
//
//    @Mock
//    lateinit var voiceToContentUseCase: VoiceToContentUseCase
//
//    @Mock
//    lateinit var recordingUseCase: RecordingUseCase
//
//    @Mock
//    lateinit var selectedSiteRepository: SelectedSiteRepository
//
//    @Mock
//    lateinit var jetpackAIStore: JetpackAIStore
//
//    private lateinit var viewModel: VoiceToContentViewModel
//
//    private lateinit var uiState: MutableList<VoiceToContentResult>

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

//    @Before
//    fun setup() {
//        // Mock the recording updates to return a non-null flow before ViewModel instantiation
//        whenever(recordingUseCase.recordingUpdates()).thenReturn(createRecordingUpdateFlow())
//
//        viewModel = VoiceToContentViewModel(
//            testDispatcher(),
//            voiceToContentFeatureUtils,
//            voiceToContentUseCase,
//            selectedSiteRepository,
//            jetpackAIStore,
//            recordingUseCase
//        )
//
//        uiState = mutableListOf()
//        viewModel.uiState.observeForever { event ->
//            event?.let { result ->
//                uiState.add(result)
//            }
//        }
//    }
//
//    // Helper function to create a consistent flow
//    private fun createRecordingUpdateFlow() = flow {
//        emit(RecordingUpdate(0, 0, false))
//    }
//
//    @Test
//    fun `when site is null, then execute posts error state `() = test {
//        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
//        val dummyFile = File("dummy_path")
//        viewModel.executeVoiceToContent(dummyFile)
//
//        val expectedState = VoiceToContentResult(isError = true)
//        assertThat(uiState.first()).isEqualTo(expectedState)
//    }
//
//   /* @Test
//    fun `when voice to content is enabled, then execute invokes use case `() = test {
//        val site = SiteModel().apply { id = 1 }
//        val dummyFile = File("dummy_path")
//
//        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
//        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(true)
//        whenever(jetpackAIStore.fetchJetpackAIAssistantFeature(site))
//            .thenReturn(JetpackAIAssistantFeatureResponse.Success(jetpackAIAssistantFeature))
//
//        viewModel.executeVoiceToContent(dummyFile)
//
//        verify(voiceToContentUseCase).execute(site, dummyFile)
//    }*/
//
////    @Test
////    fun `when voice to content is disabled, then executeVoiceToContent does not invoke use case`() = runTest {
////        val site = SiteModel().apply { id = 1 }
////        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
////        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(false)
////        val dummyFile = File("dummy_path")
////
////        viewModel.executeVoiceToContent(dummyFile)
////
////        verifyNoInteractions(voiceToContentUseCase)
////    }
//
//    @Test
//    fun `when startRecording is called, then recordingUseCase starts recording`() {
//        viewModel.startRecording()
//
//        verify(recordingUseCase).startRecording(any())
//    }
}


