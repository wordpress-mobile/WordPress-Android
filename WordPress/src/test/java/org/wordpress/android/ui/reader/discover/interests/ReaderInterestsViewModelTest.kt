package org.wordpress.android.ui.reader.discover.interests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.R
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.test
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsFragment.EntryPoint
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonDisabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonEnabledUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.DoneButtonUiState.DoneButtonHiddenUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ContentUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.ConnectionErrorUiState
import org.wordpress.android.ui.reader.discover.interests.ReaderInterestsViewModel.UiState.ErrorUiState.RequestFailedErrorUiState
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.NetworkUnavailable
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Error.RemoteRequestFailure
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.Success
import org.wordpress.android.ui.reader.repository.ReaderRepositoryCommunication.SuccessWithData
import org.wordpress.android.ui.reader.repository.ReaderTagRepository
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel
import org.wordpress.android.ui.utils.UiString.UiStringRes

private const val CURRENT_LANGUAGE = "en"

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderInterestsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    private lateinit var viewModel: ReaderInterestsViewModel
    @Mock lateinit var parentViewModel: ReaderViewModel

    @Mock lateinit var readerTagRepository: ReaderTagRepository
    @Mock lateinit var readerTracker: ReaderTracker

    @Before
    fun setUp() {
        viewModel = ReaderInterestsViewModel(readerTagRepository, readerTracker)
    }

    @Test
    fun `getUserTags invoked on start`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // When
                initViewModel()

                // Then
                verify(readerTagRepository, times(1)).getUserTags()
            }

    @Test
    fun `getInterests invoked if empty user tags received from repo`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // When
                initViewModel()

                // Then
                verify(readerTagRepository, times(1)).getInterests()
            }

    @Test
    fun `discover close reader screen triggered if non empty user tags are received from repo`() =
            testWithNonEmptyUserTags {
                // When
                initViewModel(EntryPoint.DISCOVER)

                // Then
                verify(parentViewModel, times(1)).onCloseReaderInterests()
            }

    @Test
    fun `settings does not close reader screen triggered if non empty user tags are received from repo`() =
            testWithNonEmptyUserTags {
                // When
                initViewModel(EntryPoint.SETTINGS)

                // Then
                verify(parentViewModel, times(0)).onCloseReaderInterests()
            }

    @Test
    fun `progress bar shown on start hides on successful interests data load`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // Pause dispatcher so we can verify progress bar initial state
                coroutineScope.pauseDispatcher()

                // Trigger data load
                initViewModel()

                assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(true)

                // Resume pending coroutines execution
                coroutineScope.resumeDispatcher()

                assertThat(requireNotNull(viewModel.uiState.value).progressBarVisible).isEqualTo(false)
            }

    @Test
    fun `title hidden on start become visible on successful interests data load`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // Pause dispatcher so we can verify title initial state
                coroutineScope.pauseDispatcher()

                // Trigger data load
                initViewModel()

                assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(false)

                // Resume pending coroutines execution
                coroutineScope.resumeDispatcher()

                assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isEqualTo(true)
            }

    @Test
    fun `interests correctly shown on successful interests data load without excluded interests`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getUserTags()).thenReturn(SuccessWithData(ReaderTagList()))
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // When
                initViewModel()

                // Then
                assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
                assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).interests)
                        .isEqualTo(interests)

                val uiState = requireNotNull(viewModel.uiState.value) as ContentUiState
                assertThat(uiState.interests).isEqualTo(interests)
                assertThat(uiState.interestsUiState[0]).isInstanceOf(TagUiState::class.java)
                assertThat(uiState.interestsUiState[0].title).isEqualTo(interests[0].tagTitle)
            }

    @Test
    fun `interests correctly shown on successful interests data load with excluded interests`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                val excludedInterests = getExcludedInterests()
                whenever(readerTagRepository.getUserTags()).thenReturn(SuccessWithData(excludedInterests))
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // When
                initViewModel(
                        entryPoint = EntryPoint.SETTINGS
                )

                // Then
                interests.removeAll(excludedInterests)
                assertThat(viewModel.uiState.value).isInstanceOf(ContentUiState::class.java)
                assertThat(requireNotNull(viewModel.uiState.value as ContentUiState).interests)
                        .isEqualTo(interests)

                val uiState = requireNotNull(viewModel.uiState.value) as ContentUiState
                assertThat(uiState.interests).isEqualTo(interests)
                assertThat(uiState.interestsUiState[0]).isInstanceOf(TagUiState::class.java)
                assertThat(uiState.interestsUiState[0].title).isEqualTo(interests[0].tagTitle)
            }

    @Test
    fun `discover title shown on start when interests tags received from repo`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // Pause dispatcher so we can verify done button initial state
                coroutineScope.pauseDispatcher()

                // Trigger data load
                initViewModel(EntryPoint.DISCOVER)

                // Resume pending coroutines execution
                coroutineScope.resumeDispatcher()

                assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isTrue
            }

    @Test
    fun `settings title hidden on start when interests tags received from repo`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // Pause dispatcher so we can verify done button initial state
                coroutineScope.pauseDispatcher()

                // Trigger data load
                initViewModel(EntryPoint.SETTINGS)

                // Resume pending coroutines execution
                coroutineScope.resumeDispatcher()

                assertThat(requireNotNull(viewModel.uiState.value).titleVisible).isFalse
            }

    @Test
    fun `discover done button hidden on start switches to disabled state when interests tags received from repo`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // Pause dispatcher so we can verify done button initial state
                coroutineScope.pauseDispatcher()

                // Trigger data load
                initViewModel(EntryPoint.DISCOVER)

                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                        .isInstanceOf(DoneButtonHiddenUiState::class.java)

                // Resume pending coroutines execution
                coroutineScope.resumeDispatcher()

                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                        .isInstanceOf(DoneButtonDisabledUiState::class.java)
                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.titleRes)
                        .isEqualTo(R.string.reader_btn_select_few_interests)
            }

    @Test
    fun `settings done button hidden on start switches to disabled state when interests tags received from repo`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // Pause dispatcher so we can verify done button initial state
                coroutineScope.pauseDispatcher()

                // Trigger data load
                initViewModel(EntryPoint.SETTINGS)

                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                        .isInstanceOf(DoneButtonHiddenUiState::class.java)

                // Resume pending coroutines execution
                coroutineScope.resumeDispatcher()

                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                        .isInstanceOf(DoneButtonDisabledUiState::class.java)
                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.titleRes)
                        .isEqualTo(R.string.reader_btn_done)
            }

    @Test
    fun `interest selected if onInterestAtIndexToggled invoked on a deselected interest`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                val selectedIndex = 0

                // When
                initViewModel()
                viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = true)

                // Then
                assertThat(
                        requireNotNull(viewModel.uiState.value as ContentUiState)
                                .interestsUiState[selectedIndex].isChecked
                )
                        .isEqualTo(true)
            }

    @Test
    fun `interest deselected if onInterestAtIndexToggled invoked on a selected interest`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                val selectedIndex = 0

                // When
                initViewModel()
                viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = true)
                viewModel.onInterestAtIndexToggled(index = selectedIndex, isChecked = false)

                // Then
                assertThat(
                        requireNotNull(viewModel.uiState.value as ContentUiState)
                                .interestsUiState[selectedIndex].isChecked
                )
                        .isEqualTo(false)
            }

    @Test
    fun `when interest tag is toggled, then complete follow site quick start task if needed is invoked`() =
            testWithEmptyUserTags {
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                val selectInterestAtIndex = 2

                initViewModel()
                viewModel.onInterestAtIndexToggled(index = selectInterestAtIndex, isChecked = true)

                // Then
                verify(parentViewModel).completeQuickStartFollowSiteTaskIfNeeded()
            }

    @Test
    fun `done button shown in enabled state if an interest is in selected state`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

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

    @Test
    fun `discover done button shown in disabled state if no interests are in selected state`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // When
                initViewModel(EntryPoint.DISCOVER)

                // Then
                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                        .isInstanceOf(DoneButtonDisabledUiState::class.java)
                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.titleRes)
                        .isEqualTo(R.string.reader_btn_select_few_interests)
            }

    @Test
    fun `settings done button shown in disabled state if no interests are in selected state`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))

                // When
                initViewModel(EntryPoint.SETTINGS)

                // Then
                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState)
                        .isInstanceOf(DoneButtonDisabledUiState::class.java)
                assertThat(requireNotNull(viewModel.uiState.value).doneButtonUiState.titleRes)
                        .isEqualTo(R.string.reader_btn_done)
            }

    @Test
    fun `discover close reader interests screen triggered when interests are saved successfully`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                whenever(readerTagRepository.saveInterests(any())).thenReturn(Success)

                // When
                initViewModel(EntryPoint.DISCOVER)
                viewModel.onDoneButtonClick()

                // Then
                verify(parentViewModel, times(1)).onCloseReaderInterests()
            }

    @Test
    fun `settings close reader interests screen triggered when interests are saved successfully`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                whenever(readerTagRepository.saveInterests(any())).thenReturn(Success)

                // When
                initViewModel(EntryPoint.SETTINGS)
                viewModel.onDoneButtonClick()

                // Then
                assertThat(viewModel.closeReaderInterests.value).isNotNull
            }

    @Test
    fun `selected interests saved on done button click`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                val selectInterestAtIndex = 2

                // When
                initViewModel()
                viewModel.onInterestAtIndexToggled(index = selectInterestAtIndex, isChecked = true)
                viewModel.onDoneButtonClick()

                // Then
                verify(readerTagRepository, times(1)).saveInterests(eq(listOf(interests[selectInterestAtIndex])))
            }

    @Test
    fun `get interests triggered on retry`() =
            testWithEmptyUserTags {
                // When
                viewModel.onRetryButtonClick()

                // Then
                verify(readerTagRepository, times(1)).getInterests()
            }

    @Test
    fun `get user tags re-triggered on retry if user tags request had failed earlier`() =
            testWithFailedUserTags {
                // When
                initViewModel()
                viewModel.onRetryButtonClick()

                // Then
                verify(readerTagRepository, times(2)).getUserTags()
            }

    @Test
    fun `get user tags not re-triggered on retry if user tags request had not failed earlier`() =
            testWithEmptyUserTags {
                // When
                initViewModel()
                viewModel.onRetryButtonClick()

                // Then
                verify(readerTagRepository, times(1)).getUserTags()
            }

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
                assertThat(errorUiState.titleRes).isEqualTo(R.string.no_network_message)
            }

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
                assertThat(errorUiState.titleRes).isEqualTo(R.string.reader_error_request_failed_title)
            }

    @Test
    fun `snackbar is shown on save interests error`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                whenever(readerTagRepository.saveInterests(any())).thenReturn(NetworkUnavailable)

                // When
                initViewModel()
                viewModel.onDoneButtonClick()

                // Then
                assertThat(viewModel.snackbarEvents.value).isNotNull
            }

    @Test
    fun `snackbar is not shown when interests are saved successfully`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                whenever(readerTagRepository.saveInterests(any())).thenReturn(Success)

                // When
                initViewModel()
                viewModel.onDoneButtonClick()

                // Then
                assertThat(viewModel.snackbarEvents.value).isNull()
            }

    @Test
    fun `network error shown when internet access not available on save interests`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                whenever(readerTagRepository.saveInterests(any())).thenReturn(NetworkUnavailable)

                // When
                initViewModel()
                viewModel.onDoneButtonClick()

                // Then
                assertThat(requireNotNull(viewModel.snackbarEvents.value).peekContent())
                        .isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            }

    @Test
    fun `request failed error shown on save interests remote request failure`() =
            testWithEmptyUserTags {
                // Given
                val interests = getInterests()
                whenever(readerTagRepository.getInterests()).thenReturn(SuccessWithData(interests))
                whenever(readerTagRepository.saveInterests(any())).thenReturn(RemoteRequestFailure)

                // When
                initViewModel()
                viewModel.onDoneButtonClick()

                // Then
                assertThat(requireNotNull(viewModel.snackbarEvents.value).peekContent())
                        .isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.reader_error_request_failed_title)))
            }

    @Test
    fun `discover close reader screen on back button click`() {
        // When
        initViewModel(EntryPoint.DISCOVER)

        viewModel.onBackButtonClick()

        // Then
        verify(parentViewModel, times(1)).onCloseReaderInterests()
    }

    @Test
    fun `settings close reader screen on back button click`() {
        // When
        initViewModel(EntryPoint.SETTINGS)

        viewModel.onBackButtonClick()

        // Then
        assertThat(viewModel.closeReaderInterests.value).isNotNull
    }

    private fun initViewModel(
        entryPoint: EntryPoint = EntryPoint.DISCOVER
    ) = viewModel.start(
            entryPoint = entryPoint,
            currentLanguage = CURRENT_LANGUAGE,
            parentViewModel = parentViewModel
    )

    private fun <T> testWithEmptyUserTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerTagRepository.getUserTags()).thenReturn(SuccessWithData(ReaderTagList()))
            block()
        }
    }

    private fun <T> testWithFailedUserTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerTagRepository.getUserTags()).thenReturn(NetworkUnavailable)
            block()
        }
    }

    private fun <T> testWithNonEmptyUserTags(block: suspend CoroutineScope.() -> T) {
        test {
            val nonEmptyUserTags = ReaderTagList().apply {
                this.add(mock())
                this.add(mock())
            }
            whenever(readerTagRepository.getUserTags()).thenReturn(SuccessWithData(nonEmptyUserTags))
            block()
        }
    }

    private fun getInterests() =
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

    private fun getExcludedInterests() =
            ReaderTagList().apply {
                for (c in 'A'..'C')
                    (add(
                            ReaderTag(
                                    c.toString(), c.toString(), c.toString(),
                                    "https://public-api.wordpress.com/rest/v1.2/read/tags/$c/posts",
                                    ReaderTagType.DEFAULT
                            )
                    ))
            }
}
