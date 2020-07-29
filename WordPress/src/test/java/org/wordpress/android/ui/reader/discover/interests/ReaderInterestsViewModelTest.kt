package org.wordpress.android.ui.reader.discover.interests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
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
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonDisabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonEnabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonHiddenUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.InterestUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.ConnectionErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.RequestFailedErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.InitialLoadingUiState
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel

private const val CURRENT_LANGUAGE = "en"
@RunWith(MockitoJUnitRunner::class)
class ReaderInterestsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private lateinit var viewModel: ReaderInterestsViewModel
    @Mock lateinit var parentViewModel: ReaderViewModel

    @Mock lateinit var readerTagRepository: ReaderTagRepository

    @Before
    fun setUp() {
        viewModel = ReaderInterestsViewModel(readerTagRepository)
    }

    @ExperimentalCoroutinesApi
    @Test
    fun `getUserTags invoked on start`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // When
            initViewModel()

            // Then
            verify(readerTagRepository, times(1)).getUserTags()
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `getInterests invoked if empty user tags received from repo`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // When
            initViewModel()

            // Then
            verify(readerTagRepository, times(1)).getInterests()
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `close reader screen triggered if non empty user tags are received from repo`() =
        testWithNonEmptyUserTags {
            // When
            initViewModel()

            // Then
            verify(parentViewModel, times(1)).onCloseReaderInterests()
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `progress bar shown on start hides on successful interests data load`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // Pause dispatcher so we can verify progress bar initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(true)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(false)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `title hidden on start become visible on successful interests data load`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // Pause dispatcher so we can verify title initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(false)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(true)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `subtitle hidden on start become visible on successful interests data load`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // Pause dispatcher so we can verify subtitle initial state
            coroutineScope.pauseDispatcher()

            // Trigger data load
            initViewModel()

            assertThat(requireNotNull(viewModel.uiState.value).subtitleVisible).isEqualTo(false)

            // Resume pending coroutines execution
            coroutineScope.resumeDispatcher()

            assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `interests correctly shown on successful interests data load`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // When
            initViewModel()

            // Then
            assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
            assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).interests)
                .isEqualTo(mockInterests)

            val uiState = requireNotNull(viewModel.uiState.value) as ContentUiState
            assertThat(uiState.interests).isEqualTo(mockInterests)
            assertThat(uiState.interestsUiState[0]).isInstanceOf(InterestUiState::class.java)
            assertThat(uiState.interestsUiState[0].title).isEqualTo(mockInterests[0].tagTitle)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `done button hidden on start switches to disabled state when interests tags received from repo`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

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
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
            val selectedIndex = 0

            // When
            initViewModel()
            viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = true)

            // Then
            assertThat(requireNotNull(viewModel.uiState.value as ContentUiState)
                .interestsUiState[selectedIndex].isChecked)
                .isEqualTo(true)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `interest deselected if onInterestAtIndexToggled invoked on a selected interest`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
            val selectedIndex = 0

            // When
            initViewModel()
            viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = true)
            viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = false)

            // Then
            assertThat(requireNotNull(viewModel.uiState.value as ContentUiState)
                .interestsUiState[selectedIndex].isChecked)
                .isEqualTo(false)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `done button shown in enabled state if an interest is in selected state`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

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
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // When
            initViewModel()

            // Then
            assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                .isInstanceOf(DoneButtonDisabledUiState::class.java)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `close reader interests screen triggered when interests are saved successfully`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
            whenever(readerTagRepository.saveInterests(any())).thenReturn(Success)

            // When
            initViewModel()
            viewModel.onDoneButtonClick()

            // Then
            verify(parentViewModel, times(1)).onCloseReaderInterests()
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `selected interests saved on done button click`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
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
    fun `interests data loading triggered on retry`() =
        testWithEmptyUserTags {
            // Given
            val uiStates = mutableListOf<UiState>()
            viewModel.uiState.observeForever {
                uiStates.add(it)
            }
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))

            // When
            viewModel.onRetryButtonClick()

            // Then
            assertThat(uiStates.size).isEqualTo(2)
            assertThat(uiStates[0]).isInstanceOf(InitialLoadingUiState::class.java)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `error layout is shown on interests load error`() =
        testWithEmptyUserTags {
            // Given
            whenever(readerTagRepository.getInterests()).thenReturn(NetworkUnavailable)

            // When
            initViewModel()

            // Then
            val contentLoadFailedUiState = requireNotNull(viewModel.uiState.value) as ConnectionErrorUiState
            assertThat(contentLoadFailedUiState.errorLayoutVisible).isEqualTo(true)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `network error shown when internet access not available on interests load`() =
        testWithEmptyUserTags {
            // Given
            whenever(readerTagRepository.getInterests()).thenReturn(NetworkUnavailable)

            // When
            initViewModel()

            // Then
            assertThat(viewModel.uiState.value).isInstanceOf(ConnectionErrorUiState::class.java)
            val errorUiState = requireNotNull(viewModel.uiState.value) as ConnectionErrorUiState
            assertThat(errorUiState.titleResId).isEqualTo(R.string.no_network_message)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `request failed error shown on load interests remote request failure`() =
        testWithEmptyUserTags {
            // Given
            whenever(readerTagRepository.getInterests()).thenReturn(RemoteRequestFailure)

            // When
            initViewModel()

            // Then
            assertThat(viewModel.uiState.value).isInstanceOf(RequestFailedErrorUiState::class.java)
            val errorUiState = requireNotNull(viewModel.uiState.value) as RequestFailedErrorUiState
            assertThat(errorUiState.titleResId).isEqualTo(R.string.reader_error_request_failed_title)
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `snackbar is shown on save interests error`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
            whenever(readerTagRepository.saveInterests(any())).thenReturn(NetworkUnavailable)

            // When
            initViewModel()
            viewModel.onDoneButtonClick()

            // Then
            assertThat(viewModel.snackbarEvents.value).isNotNull
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `snackbar is not shown when interests are saved successfully`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
            whenever(readerTagRepository.saveInterests(any())).thenReturn(Success)

            // When
            initViewModel()
            viewModel.onDoneButtonClick()

            // Then
            assertThat(viewModel.snackbarEvents.value).isNull()
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `network error shown when internet access not available on save interests`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
            whenever(readerTagRepository.saveInterests(any())).thenReturn(NetworkUnavailable)

            // When
            initViewModel()
            viewModel.onDoneButtonClick()

            // Then
            assertThat(requireNotNull(viewModel.snackbarEvents.value).peekContent())
                    .isEqualTo(SnackbarMessageHolder(R.string.no_network_message))
        }

    @ExperimentalCoroutinesApi
    @Test
    fun `request failed error shown on save interests remote request failure`() =
        testWithEmptyUserTags {
            // Given
            val mockInterests = getMockInterests()
            whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(mockInterests))
            whenever(readerTagRepository.saveInterests(any())).thenReturn(RemoteRequestFailure)

            // When
            initViewModel()
            viewModel.onDoneButtonClick()

            // Then
            assertThat(requireNotNull(viewModel.snackbarEvents.value).peekContent())
                .isEqualTo(SnackbarMessageHolder(R.string.reader_error_request_failed_title))
        }

    private fun initViewModel() = viewModel.start(
        parentViewModel = parentViewModel,
        currentLanguage = CURRENT_LANGUAGE
    )

    @ExperimentalCoroutinesApi
    private fun <T> testWithEmptyUserTags(block: suspend CoroutineScope.() -> T) {
        coroutineScope.runBlockingTest {
            whenever(readerTagRepository.getUserTags()).thenReturn(ReaderTagList())
            block()
        }
    }

    @ExperimentalCoroutinesApi
    private fun <T> testWithNonEmptyUserTags(block: suspend CoroutineScope.() -> T) {
        coroutineScope.runBlockingTest {
            val nonEmptyUserTags = ReaderTagList().apply {
                this.add(mock())
                this.add(mock())
            }
            whenever(readerTagRepository.getUserTags()).thenReturn(nonEmptyUserTags)
            block()
        }
    }

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
