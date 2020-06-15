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
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentInitialUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentLoadSuccessUiState
import org.wordpress.android.ui.reader.repository.ReaderTagRepository

@RunWith(MockitoJUnitRunner::class)
class ReaderInterestsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private lateinit var viewModel: ReaderInterestsViewModel

    @Mock lateinit var readerTagRepository: ReaderTagRepository

    @Before
    fun setUp() {
        viewModel = ReaderInterestsViewModel(readerTagRepository)
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
    fun `title, subtitles hidden on start become visible on successful data load`() {
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // Pause dispatcher so we can verify title, subtitles initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(false)
            assertThat(requireNotNull(viewModel.uiState.value).subtitleVisible).isEqualTo(false)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(true)
            assertThat(requireNotNull(viewModel.uiState.value).subtitleVisible).isEqualTo(true)
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `content correctly loaded if non empty interests received from repo on start`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()

            // Then
            assertThat(requireNotNull(viewModel.uiState.value).interests).isEqualTo(mockInterests)
            assertThat(viewModel.uiState.value).isInstanceOf(ContentLoadSuccessUiState::class.java)

            val uiState = requireNotNull(viewModel.uiState.value) as ContentLoadSuccessUiState
            assertThat(uiState.interestTags).isEqualTo(mockInterests)
            assertThat(uiState.interestTagsUiState[0]).isInstanceOf(InterestUiState::class.java)
            assertThat(uiState.interestTagsUiState[0].title).isEqualTo(mockInterests[0].tagTitle)
            assertThat(uiState.doneBtnUiState).isEqualTo(DoneButtonDisabledUiState)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `content not updated if no tags received from repo on start`() =
        coroutineScope.runBlockingTest {
            // Given
            whenever(readerTagRepository.getInterests()).thenReturn(ReaderTagList())

            // When
            initViewModel()

            // Then
            assertThat(viewModel.uiState.value).isEqualTo(ContentInitialUiState)
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
            assertThat(requireNotNull(viewModel.uiState.value).interestsUiState[selectedIndex].isChecked)
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
            assertThat(requireNotNull(viewModel.uiState.value).interestsUiState[selectedIndex].isChecked)
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
