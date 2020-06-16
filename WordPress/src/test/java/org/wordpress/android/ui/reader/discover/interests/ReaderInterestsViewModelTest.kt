package org.wordpress.android.ui.reader.discover.interests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonDisabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonEnabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonHiddenUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.InterestUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadFailedUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadFailedUiState.ContentLoadFailedConnectionErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadSuccessUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class ReaderInterestsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private lateinit var viewModel: ReaderInterestsViewModel

    @Mock lateinit var readerTagRepository: ReaderTagRepository
    @Mock lateinit var networkUtils: NetworkUtilsWrapper

    @Before
    fun setUp() {
        viewModel = ReaderInterestsViewModel(readerTagRepository, networkUtils)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `progress bar shown on start hides on successful data load`() {
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // Pause dispatcher so we can verify progress bar initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(true)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(false)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `title hidden on start become visible on successful data load`() {
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // Pause dispatcher so we can verify title initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(false)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(true)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `subtitle hidden on start become visible on successful data load`() {
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // Pause dispatcher so we can verify subtitle initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).subtitleVisible).isEqualTo(false)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(viewModel.uiState.value).isInstanceOf(ContentLoadSuccessUiState::class.java)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `interests correctly shown on successful data load`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()

            // Then
            assertThat(viewModel.uiState.value).isInstanceOf(ContentLoadSuccessUiState::class.java)
            assertThat(requireNotNull(viewModel.uiState.value as ContentLoadSuccessUiState).interests)
                .isEqualTo(mockInterests)

            val uiState = requireNotNull(viewModel.uiState.value) as ContentLoadSuccessUiState
            assertThat(uiState.interests).isEqualTo(mockInterests)
            assertThat(uiState.interestsUiState[0]).isInstanceOf(InterestUiState::class.java)
            assertThat(uiState.interestsUiState[0].title).isEqualTo(mockInterests[0].tagTitle)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `done button hidden on start switches to disabled state when tags received from repo`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // Pause dispatcher so we can verify done button initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                .isInstanceOf(DoneButtonHiddenUiState::class.java)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                .isInstanceOf(DoneButtonDisabledUiState::class.java)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `interest selected if onInterestAtIndexToggled invoked on a deselected interest`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)
            val selectedIndex = 0

            // When
            initViewModel()
            viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = true)

            // Then
            assertThat(requireNotNull(viewModel.uiState.value as ContentLoadSuccessUiState)
                .interestsUiState[selectedIndex].isChecked)
                .isEqualTo(true)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `interest deselected if onInterestAtIndexToggled invoked on a selected interest`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)
            val selectedIndex = 0

            // When
            initViewModel()
            viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = true)
            viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = false)

            // Then
            assertThat(requireNotNull(viewModel.uiState.value as ContentLoadSuccessUiState)
                .interestsUiState[selectedIndex].isChecked)
                .isEqualTo(false)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `done button shown in enabled state if an interest is in selected state`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()
            viewModel.onInterestAtIndexToggled(index = 0, isChecked = true)

            // Then
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                .isInstanceOf(DoneButtonEnabledUiState::class.java)
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.titleRes)
                .isEqualTo(R.string.reader_btn_done)
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.enabled)
                .isEqualTo(true)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `done button shown in disabled state if no interests are in selected state`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()

            // Then
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                .isInstanceOf(DoneButtonDisabledUiState::class.java)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `navigation to discover triggered on done button click`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()
            viewModel.onDoneButtonClick()

            // Then
            assertThat(requireNotNull(viewModel.navigateToDiscover.value).peekContent()).isNotNull
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `selected interests saved on done button click`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)
            val selectInterestAtIndex = 2

            // When
            initViewModel()
            viewModel.onInterestAtIndexToggled(index = selectInterestAtIndex, isChecked = true)
            viewModel.onDoneButtonClick()

            // Then
            verify(readerTagRepository, times(1)).saveInterests(eq(listOf(mockInterests[selectInterestAtIndex])))
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `no network error shown when internet access not available`() =
        coroutineScope.runBlockingTest {
            // Given
            whenever(networkUtils.isNetworkAvailable()).thenReturn(false)

            // When
            initViewModel()

            // Then
            assertThat(viewModel.uiState.value).isInstanceOf(ContentLoadFailedConnectionErrorUiState::class.java)
            val contentLoadFailedUiState = requireNotNull(viewModel.uiState.value as ContentLoadFailedUiState)
            assertThat(contentLoadFailedUiState.fullscreenErrorLayoutVisible).isEqualTo(true)
            assertThat(contentLoadFailedUiState.titleResId).isEqualTo(R.string.no_network_message)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `data loading triggered on retry`() =
        coroutineScope.runBlockingTest {
            // Given
            val uiStates = mutableListOf<UiState>()
            viewModel.uiState.observeForever {
                uiStates.add(it)
            }
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            viewModel.onRetryButtonClick()

            // Then
            assertThat(uiStates.size).isEqualTo(2)
            assertThat(uiStates[0]).isInstanceOf(LoadingUiState::class.java)
        }

    private fun initViewModel() = viewModel.start()

    private fun getMockInterests() =
        ReaderTagList().apply {
            for (c in 'A'..'Z')
                (add(
                    ReaderTag(
                        c.toString(), c.toString(), c.toString(),
                        "https://public-api.wordpress.com/rest/v1.2/read/tags/$c/posts",
                        ReaderTagType.DEFAULT
                    )
                ))
        }
}
