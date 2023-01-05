package org.wordpress.android.ui.bloggingprompts.promptslist

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.ui.bloggingprompts.promptslist.mapper.BloggingPromptsListItemModelMapper
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase.Result.Failure
import org.wordpress.android.ui.bloggingprompts.promptslist.usecase.FetchBloggingPromptsListUseCase.Result.Success
import org.wordpress.android.util.NetworkUtilsWrapper
import java.util.Date

@ExperimentalCoroutinesApi
class BloggingPromptsListViewModelTest : BaseUnitTest() {
    @Mock private lateinit var fetchBloggingPromptsListUseCase: FetchBloggingPromptsListUseCase
    @Mock private lateinit var itemMapper: BloggingPromptsListItemModelMapper
    @Mock private lateinit var tracker: BloggingPromptsListAnalyticsTracker
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    lateinit var viewModel: BloggingPromptsListViewModel

    @Before
    fun setUp() {
        whenever(itemMapper.toUiModel(argThat { id == UI_PROMPT.id })).thenReturn(UI_PROMPT)

        viewModel = BloggingPromptsListViewModel(
                fetchBloggingPromptsListUseCase,
                itemMapper,
                tracker,
                networkUtilsWrapper,
                testDispatcher()
        )
    }

    @Test
    fun `when start, then track screen seen event`() = test {
        viewModel.start()
        verify(tracker).trackScreenShown()
    }

    @Test
    fun `given successful fetch, when start, then update uiState to Loading then Content`() = test {
        whenever(fetchBloggingPromptsListUseCase.execute()).doSuspendableAnswer {
            delay(10)
            Success(listOf(DOMAIN_PROMPT))
        }
        mockNetworkAvailability(true)

        assertThat(viewModel.uiStateFlow.value).isEqualTo(BloggingPromptsListViewModel.UiState.None)

        viewModel.start()

        assertThat(viewModel.uiStateFlow.value).isEqualTo(BloggingPromptsListViewModel.UiState.Loading)

        advanceUntilIdle()

        val result = viewModel.uiStateFlow.value
        assertThat(result).isInstanceOf(BloggingPromptsListViewModel.UiState.Content::class.java)
        assertThat((result as BloggingPromptsListViewModel.UiState.Content).content).isEqualTo(listOf(UI_PROMPT))
    }

    @Test
    fun `given unsuccessful fetch, when start, then update uiState to Loading then FetchError`() = test {
        whenever(fetchBloggingPromptsListUseCase.execute()).doSuspendableAnswer {
            delay(10)
            Failure
        }
        mockNetworkAvailability(true)

        assertThat(viewModel.uiStateFlow.value).isEqualTo(BloggingPromptsListViewModel.UiState.None)

        viewModel.start()

        assertThat(viewModel.uiStateFlow.value).isEqualTo(BloggingPromptsListViewModel.UiState.Loading)

        advanceUntilIdle()

        val result = viewModel.uiStateFlow.value
        assertThat(result).isInstanceOf(BloggingPromptsListViewModel.UiState.FetchError::class.java)
    }

    @Test
    fun `given network unavailable, when start, then update uiState to Loading then FetchError`() = test {
        mockNetworkAvailability(false)

        assertThat(viewModel.uiStateFlow.value).isEqualTo(BloggingPromptsListViewModel.UiState.None)

        viewModel.start()

        assertThat(viewModel.uiStateFlow.value).isEqualTo(BloggingPromptsListViewModel.UiState.NetworkError)
    }

    private fun mockNetworkAvailability(isNetworkAvailable: Boolean) {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(isNetworkAvailable)
    }

    companion object {
        private val DOMAIN_PROMPT = BloggingPromptModel(
                id = 123,
                text = "Text",
                title = "Title",
                content = "Content",
                date = Date(1671678000000), // December 22, 2022
                isAnswered = true,
                attribution = "Attribution",
                respondentsCount = 321,
                respondentsAvatarUrls = emptyList(),
        )

        private val UI_PROMPT = BloggingPromptsListItemModel(
                id = 123,
                text = "Text",
                date = Date(1671678000000), // December 22, 2022
                formattedDate = "Dec 22",
                isAnswered = true,
                answersCount = 321,
        )
    }
}
