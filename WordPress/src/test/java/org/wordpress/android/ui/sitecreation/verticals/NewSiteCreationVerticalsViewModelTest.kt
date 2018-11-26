package org.wordpress.android.ui.sitecreation.verticals

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.experimental.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptError
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsError
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentPromptUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsContentState.CONTENT
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsContentState.FULLSCREEN_ERROR
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsHeaderUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsSearchInputUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationVerticalsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchVerticalsUseCase: FetchVerticalsUseCase
    @Mock lateinit var fetchSegmentsPromptUseCase: FetchSegmentPromptUseCase

    private lateinit var viewModel: NewSiteCreationVerticalsViewModel

    @Mock private lateinit var uiStateObserver: Observer<VerticalsUiState>
    @Mock private lateinit var clearBtnObserver: Observer<Void>

    private val dummySearchInputHint = "dummyHint"
    private val dummySearchInputTitle = "dummyTitle"
    private val dummySearchInputSubtitle = "dummySubtitle"
    private val successHeaderInfoEvent = OnSegmentPromptFetched(
            1L,
            SegmentPromptModel(
                    dummySearchInputTitle,
                    dummySearchInputSubtitle,
                    dummySearchInputHint
            ), null
    )
    private val errorHeaderInfoEvent = OnSegmentPromptFetched(
            1L,
            null,
            FetchSegmentPromptError(GENERIC_ERROR, null)
    )
    private val firstModel = VerticalModel("firstModel", "1")
    private val secondModel = VerticalModel("secondModel", "2")
    private val headerAndEmptyInputState = VerticalsUiState.VerticalsContentUiState(
            showSkipButton = true,
            headerUiState = VerticalsHeaderUiState.Visible(dummySearchInputTitle, dummySearchInputSubtitle),
            searchInputState = VerticalsSearchInputUiState.Visible(dummySearchInputHint, false, false),
            items = listOf()
    )
    private val fetchingSuggestionsState = VerticalsUiState.VerticalsContentUiState(
            showSkipButton = false,
            headerUiState = VerticalsHeaderUiState.Hidden,
            searchInputState = VerticalsSearchInputUiState.Visible(dummySearchInputHint, true, true),
            items = listOf()
    )

    private val fetchingSuggestionsFailedState = VerticalsUiState.VerticalsContentUiState(
            showSkipButton = false,
            headerUiState = VerticalsHeaderUiState.Hidden,
            searchInputState = VerticalsSearchInputUiState.Visible(dummySearchInputHint, false, true),
            items = listOf(
                    VerticalsFetchSuggestionsErrorUiState(
                            R.string.site_creation_fetch_suggestions_failed,
                            R.string.button_retry
                    )
            )
    )

    private val firstModelDisplayedState = VerticalsUiState.VerticalsContentUiState(
            showSkipButton = false,
            headerUiState = VerticalsHeaderUiState.Hidden,
            searchInputState = VerticalsSearchInputUiState.Visible(dummySearchInputHint, false, true),
            items = listOf(VerticalsModelUiState(firstModel.verticalId, firstModel.name, false))
    )

    private val secondModelDisplayedState = VerticalsUiState.VerticalsContentUiState(
            showSkipButton = false,
            headerUiState = VerticalsHeaderUiState.Hidden,
            searchInputState = VerticalsSearchInputUiState.Visible(dummySearchInputHint, false, true),
            items = listOf(VerticalsModelUiState(secondModel.verticalId, secondModel.name, false))
    )

    private val firstModelEvent = OnVerticalsFetched("a", listOf(firstModel), null)
    private val secondModelEvent = OnVerticalsFetched("b", listOf(secondModel), null)
    private val fetchSuggestionsFailedEvent = OnVerticalsFetched(
            "c",
            emptyList(),
            FetchVerticalsError(VerticalErrorType.GENERIC_ERROR, null)
    )

    @Before
    fun setUp() {
        viewModel = NewSiteCreationVerticalsViewModel(
                dispatcher,
                fetchSegmentsPromptUseCase,
                fetchVerticalsUseCase,
                Dispatchers.Unconfined,
                Dispatchers.Unconfined
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.clearBtnClicked.observeForever(clearBtnObserver)
    }

    @Test
    fun verifyHeaderInfoFetchedOnStart() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        viewModel.start(1L)
        assertTrue(viewModel.uiState.value!!.contentState == CONTENT)
    }

    @Test
    fun verifyFullscreenErrorShownOnFailedHeaderInfoRequest() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(errorHeaderInfoEvent)
        viewModel.start(1L)
        assertTrue(viewModel.uiState.value!!.contentState == FULLSCREEN_ERROR)
    }

    @Test
    fun verifyRetryWorksOnFullScreenError() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(errorHeaderInfoEvent)
        viewModel.start(1L)
        assertTrue(viewModel.uiState.value!!.contentState == FULLSCREEN_ERROR)

        viewModel.onFetchSegmentsPromptRetry()
        assertTrue(viewModel.uiState.value!!.contentState == FULLSCREEN_ERROR)

        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        viewModel.onFetchSegmentsPromptRetry()
        assertTrue(viewModel.uiState.value!!.contentState == CONTENT)
    }

    @Test
    fun verifyOnClearBtnClickedPropagated() = test {
        viewModel.onClearTextBtnClicked()
        inOrder(clearBtnObserver)
                .verify(clearBtnObserver).onChanged(anyOrNull())
    }

    @Test
    fun verifyHeaderShownInInitialState() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        viewModel.start(1L)
        assertTrue(viewModel.uiState.value!!.headerUiState.isVisible)
    }


    @Test
    fun verifyHeaderShownOnEmptyQuery() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(firstModelEvent)
        viewModel.start(1L)
        viewModel.updateQuery("a", 0)
        viewModel.updateQuery("", 0)
        assertTrue(viewModel.uiState.value!!.headerUiState.isVisible)
    }

    @Test
    fun verifyHeaderNotShownOnNonEmptyQuery() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(argThat { isNotEmpty() })).thenReturn(firstModelEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(argThat { isEmpty() })).thenThrow(IllegalArgumentException())
        viewModel.start(1L)
        viewModel.updateQuery("a", 0)
        assertFalse(viewModel.uiState.value!!.headerUiState.isVisible)
    }

    @Test
    fun verifyInputShownOnHeaderInfoFetched() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        viewModel.start(1L)
        val inputState = viewModel.uiState.value!!.searchInputState
        assertFalse(inputState.showProgress)
        assertFalse(inputState.showClearButton)
        assertEquals(dummySearchInputHint, inputState.hint)
    }

    @Test
    fun verifyClearSearchNotShownOnEmptyQuery() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        viewModel.start(1L)
        val searchState = viewModel.uiState.value!!.searchInputState
        assertFalse(searchState.showClearButton)
    }

    @Test
    fun verifyClearSearchShownOnNonEmptyQuery() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(argThat { isNotEmpty() })).thenReturn(firstModelEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(argThat { isEmpty() })).thenThrow(IllegalArgumentException())
        viewModel.start(1L)
        viewModel.updateQuery("abc", 0)
        val searchState = viewModel.uiState.value!!.searchInputState
        assertTrue(searchState.showClearButton)
    }

    @Test
    fun verifySearchProgressNotShownOnHeaderInfoFetched() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        viewModel.start(1L)
        val searchState = viewModel.uiState.value!!.searchInputState
        assertFalse(searchState.showProgress)
    }

    @Test
    fun verifyStatesAfterUpdatingQuery() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(firstModelEvent)
        viewModel.start(1L)
        viewModel.updateQuery("a", delay = 0)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(secondModelEvent)
        viewModel.updateQuery("ab", delay = 0)

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(headerAndEmptyInputState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verify(uiStateObserver).onChanged(firstModelDisplayedState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verify(uiStateObserver).onChanged(secondModelDisplayedState)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyRetryItemShownOnFailedSuggestionsRequest() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(fetchSuggestionsFailedEvent)
        viewModel.start(1L)
        viewModel.updateQuery("a", delay = 0)
        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(headerAndEmptyInputState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsFailedState)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyRetryItemReFetchesSuggestions() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(1L)).thenReturn(successHeaderInfoEvent)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(fetchSuggestionsFailedEvent)
        viewModel.start(1L)
        viewModel.updateQuery("a", delay = 0)

        val errorUiState = viewModel.uiState.value!!.items[0] as VerticalsFetchSuggestionsErrorUiState
        errorUiState.onItemTapped.invoke()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(headerAndEmptyInputState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verify(uiStateObserver).onChanged(fetchingSuggestionsFailedState)
            // we verify just the 'inProgress' state as the result will never be returned
            // since we can't set throttle delay to 0 in onItemTapped
            verify(uiStateObserver).onChanged(fetchingSuggestionsState)
            verifyNoMoreInteractions()
        }
    }
}
