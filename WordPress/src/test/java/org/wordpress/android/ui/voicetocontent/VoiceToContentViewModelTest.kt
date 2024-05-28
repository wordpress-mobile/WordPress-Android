package org.wordpress.android.ui.voicetocontent

import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.wordpress.android.fluxc.model.jetpackai.JetpackAIAssistantFeature
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIAssistantFeatureResponse
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class VoiceToContentViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var voiceToContentFeatureUtils: VoiceToContentFeatureUtils

    @Mock
    lateinit var voiceToContentUseCase: VoiceToContentUseCase

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var jetpackAIStore: JetpackAIStore

    private lateinit var viewModel: VoiceToContentViewModel

    private lateinit var uiState: MutableList<VoiceToContentResult>

    private val jetpackAIAssistantFeature = JetpackAIAssistantFeature(
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
    )
    @Before
    fun setup() {
        viewModel = VoiceToContentViewModel(
            testDispatcher(),
            voiceToContentFeatureUtils,
            voiceToContentUseCase,
            selectedSiteRepository,
            jetpackAIStore
        )

        uiState = mutableListOf()
        viewModel.uiState.observeForever { event ->
            event?.let { result ->
                uiState.add(result)
            }
        }
    }

    @Test
    fun `when site is null, then execute posts error state `() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)

        viewModel.execute()

        val expectedState = VoiceToContentResult(isError = true)
        assertThat(uiState.first()).isEqualTo(expectedState)
    }

    @Test
        fun `when voice to content is enabled, then execute invokes use case `() = test {
            val site = SiteModel().apply { id = 1 }
            whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
            whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(true)
            whenever(jetpackAIStore.fetchJetpackAIAssistantFeature(site))
                .thenReturn(JetpackAIAssistantFeatureResponse.Success(jetpackAIAssistantFeature))

            viewModel.execute()

            verify(voiceToContentUseCase).execute(site)
    }

    @Test
    fun `when voice to content is disabled, then execute does not invoke use case `() = test {
        val site = SiteModel().apply { id = 1 }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(voiceToContentFeatureUtils.isVoiceToContentEnabled()).thenReturn(false)

        viewModel.execute()

        verifyNoInteractions(voiceToContentUseCase)
    }
}
