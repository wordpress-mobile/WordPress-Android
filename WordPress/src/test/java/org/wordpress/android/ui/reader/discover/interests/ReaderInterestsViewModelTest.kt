package org.wordpress.android.ui.reader.discover.interests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
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
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.InterestUiState
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
    fun `interests ui correctly loaded if non empty interests received from repo on start`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()

            // Then
            assertThat(requireNotNull(viewModel.uiState.value).interests).isEqualTo(mockInterests)
            assertThat(requireNotNull(viewModel.uiState.value).interestsUiState[0])
                    .isInstanceOf(InterestUiState::class.java)
            assertThat(requireNotNull(viewModel.uiState.value).interestsUiState[0].title)
                    .isEqualTo(mockInterests[0].tagTitle)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `interests ui not updated if no tags received from repo on start`() =
        coroutineScope.runBlockingTest {
            // Given
            whenever(readerTagRepository.getInterests()).thenReturn(ReaderTagList())

            // When
            initViewModel()

            // Then
            assertThat(viewModel.uiState.value).isNull()
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `done button enabled if at least one interest selected`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()
            viewModel.onInterestAtIndexToggled(index = 0, isChecked = true)

            // Then
            assertThat(requireNotNull(viewModel.uiState.value).interestsUiState.filter { it.isChecked }.size)
                    .isGreaterThanOrEqualTo(1)
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                    .isInstanceOf(DoneButtonEnabledUiState::class.java)
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.titleRes)
                    .isEqualTo(R.string.reader_btn_done)
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.enabled)
                    .isEqualTo(true)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `done button disabled if the only selected interest is deselected`() =
        coroutineScope.runBlockingTest {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(mockInterests)

            // When
            initViewModel()
            // Select an interest
            viewModel.onInterestAtIndexToggled(index = 0, isChecked = true)
            // Then
            assertThat(requireNotNull(viewModel.uiState.value).interestsUiState.filter { it.isChecked }.size)
                    .isGreaterThanOrEqualTo(1)
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                    .isInstanceOf(DoneButtonEnabledUiState::class.java)

            // Deselect the only selected interest
            viewModel.onInterestAtIndexToggled(index = 0, isChecked = false)
            // Then
            assertThat(requireNotNull(viewModel.uiState.value).interestsUiState.filter { it.isChecked }.size)
                    .isEqualTo(0)
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                    .isInstanceOf(DoneButtonDisabledUiState::class.java)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `navigate to discover on done button click`() =
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
