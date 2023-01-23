package org.wordpress.android.ui.bloggingprompts.promptslist

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.ActionEvent
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListViewModel.UiState
import org.wordpress.android.ui.bloggingprompts.promptslist.mapper.BloggingPromptsListItemModelMapper
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase.Result.Failure
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase.Result.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.config.BloggingPromptsEnhancementsFeatureConfig
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class BloggingPromptsListViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var fetchBloggingPromptsListUseCase: FetchBloggingPromptsListUseCase

    @Mock
    private lateinit var itemMapper: BloggingPromptsListItemModelMapper

    @Mock
    private lateinit var tracker: BloggingPromptsListAnalyticsTracker

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var bloggingPromptsEnhancementsFeatureConfig: BloggingPromptsEnhancementsFeatureConfig

    lateinit var viewModel: BloggingPromptsListViewModel

    @Before
    fun setUp() {
        whenever(itemMapper.toUiModel(argThat { id == BloggingPromptsListFixtures.UI_MODEL.id }))
            .thenReturn(BloggingPromptsListFixtures.UI_MODEL)

        viewModel = BloggingPromptsListViewModel(
            fetchBloggingPromptsListUseCase,
            itemMapper,
            tracker,
            networkUtilsWrapper,
            testDispatcher(),
            bloggingPromptsEnhancementsFeatureConfig,
        )
    }

    @Test
    fun `when start, then track screen seen event`() = test {
        viewModel.onScreenShown()
        verify(tracker).trackScreenShown()
    }

    @Test
    fun `given successful fetch, when start, then update uiState to Loading then Content`() = test {
        whenever(fetchBloggingPromptsListUseCase.execute()).doSuspendableAnswer {
            delay(10)
            Success(listOf(BloggingPromptsListFixtures.DOMAIN_MODEL))
        }
        mockNetworkAvailability(true)

        assertThat(viewModel.uiStateFlow.value).isEqualTo(UiState.None)

        viewModel.onScreenShown()

        assertThat(viewModel.uiStateFlow.value).isEqualTo(UiState.Loading)

        advanceUntilIdle()

        val result = viewModel.uiStateFlow.value
        assertThat(result).isInstanceOf(UiState.Content::class.java)
        assertThat((result as UiState.Content).content)
            .isEqualTo(listOf(BloggingPromptsListFixtures.UI_MODEL))
    }

    @Test
    fun `given unsuccessful fetch, when start, then update uiState to Loading then FetchError`() = test {
        whenever(fetchBloggingPromptsListUseCase.execute()).doSuspendableAnswer {
            delay(10)
            Failure
        }
        mockNetworkAvailability(true)

        assertThat(viewModel.uiStateFlow.value).isEqualTo(UiState.None)

        viewModel.onScreenShown()

        assertThat(viewModel.uiStateFlow.value).isEqualTo(UiState.Loading)

        advanceUntilIdle()

        val result = viewModel.uiStateFlow.value
        assertThat(result).isInstanceOf(UiState.FetchError::class.java)
    }

    @Test
    fun `given network unavailable, when start, then update uiState to Loading then FetchError`() = test {
        mockNetworkAvailability(false)

        assertThat(viewModel.uiStateFlow.value).isEqualTo(UiState.None)

        viewModel.onScreenShown()

        assertThat(viewModel.uiStateFlow.value).isEqualTo(UiState.NetworkError)
    }

    @Test
    fun `given list item is clicked, then track analytics event`() {
        viewModel.onPromptListItemClicked(BloggingPromptsListFixtures.UI_MODEL)
        verify(tracker).trackItemClicked()
    }

    @Test
    fun `given list item is clicked and enhancements feature flag is ENABLED, then open editor screen`() = test {
        val promptListItem = BloggingPromptsListFixtures.UI_MODEL
        mockBloggingPromptsEnhancementsFeatureFlagEnabled(true)

        val result = ArrayList<ActionEvent>()
        val job = launch {
            viewModel.actionEvents.collectLatest {
                result.add(it)
            }
        }

        viewModel.onPromptListItemClicked(promptListItem)
        assertThat(result.first()).isEqualTo(ActionEvent.OpenEditor(promptListItem.id))

        job.cancel()
    }

    @Test
    fun `given list item is clicked and enhancements feature flag is DISABLED, should NOT open editor screen`() =
        test {
            val promptListItem = BloggingPromptsListFixtures.UI_MODEL
            mockBloggingPromptsEnhancementsFeatureFlagEnabled(false)

            val result = ArrayList<ActionEvent>()
            val job = launch {
                viewModel.actionEvents.collectLatest {
                    result.add(it)
                }
            }

            viewModel.onPromptListItemClicked(promptListItem)
            assertTrue(result.isEmpty())

            job.cancel()
        }

    private fun mockNetworkAvailability(isNetworkAvailable: Boolean) {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(isNetworkAvailable)
    }

    private fun mockBloggingPromptsEnhancementsFeatureFlagEnabled(isEnabled: Boolean) {
        whenever(bloggingPromptsEnhancementsFeatureConfig.isEnabled()).thenReturn(isEnabled)
    }
}
