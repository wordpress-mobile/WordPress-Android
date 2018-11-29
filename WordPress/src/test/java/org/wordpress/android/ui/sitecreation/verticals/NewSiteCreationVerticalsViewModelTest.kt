package org.wordpress.android.ui.sitecreation.verticals

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.firstValue
import com.nhaarman.mockito_kotlin.lastValue
import com.nhaarman.mockito_kotlin.secondValue
import com.nhaarman.mockito_kotlin.thirdValue
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptError
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsError
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentPromptUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsContentState.CONTENT
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState

private const val SEGMENT_ID = 1L
private const val ZERO_DELAY = 0
private const val EMPTY_STRING = ""

private const val DUMMY_HINT = "dummyHint"
private const val DUMMY_TITLE = "dummyTitle"
private const val DUMMY_SUBTITLE = "dummySubtitle"

private const val FIRST_MODEL_QUERY = "success_1"
private const val SECOND_MODEL_QUERY = "success_2"
private const val ERROR_MODEL_QUERY = "error"

private const val FIRST_MODEL_NAME = "firstModel"
private const val FIRST_MODEL_ID = "1"
private const val SECOND_MODEL_NAME = "secondModel"
private const val SECOND_MODEL_ID = "2"

private val SUCCESSFUL_HEADER_PROMPT_FETCHED = OnSegmentPromptFetched(
        SEGMENT_ID,
        SegmentPromptModel(DUMMY_TITLE, DUMMY_SUBTITLE, DUMMY_HINT), null
)
private val FAILED_HEADER_PROMPT_FETCHED = OnSegmentPromptFetched(
        SEGMENT_ID,
        null,
        FetchSegmentPromptError(GENERIC_ERROR, null)
)

private val FIRST_MODEL_ON_VERTICALS_FETCHED = OnVerticalsFetched(
        FIRST_MODEL_QUERY,
        listOf(VerticalModel(FIRST_MODEL_NAME, FIRST_MODEL_ID)),
        null
)
private val SECOND_MODEL_ON_VERTICALS_FETCHED = OnVerticalsFetched(
        SECOND_MODEL_QUERY,
        listOf(VerticalModel(SECOND_MODEL_NAME, SECOND_MODEL_ID)),
        null
)
private val ERROR_ON_VERTICALS_FETCHED = OnVerticalsFetched(
        ERROR_MODEL_QUERY,
        emptyList(),
        FetchVerticalsError(GENERIC_ERROR, null)
)

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationVerticalsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchVerticalsUseCase: FetchVerticalsUseCase
    @Mock lateinit var fetchSegmentsPromptUseCase: FetchSegmentPromptUseCase
    @Mock private lateinit var uiStateObserver: Observer<VerticalsUiState>
    @Mock private lateinit var clearBtnObserver: Observer<Void>
    @Mock private lateinit var verticalSelectedObserver: Observer<String?>

    private lateinit var viewModel: NewSiteCreationVerticalsViewModel

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
        viewModel.verticalSelected.observeForever(verticalSelectedObserver)
    }

    private fun <T> testWithSuccessResponses(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID))
                    .thenReturn(SUCCESSFUL_HEADER_PROMPT_FETCHED)

            whenever(fetchVerticalsUseCase.fetchVerticals(FIRST_MODEL_QUERY))
                    .thenReturn(FIRST_MODEL_ON_VERTICALS_FETCHED)

            whenever(fetchVerticalsUseCase.fetchVerticals(SECOND_MODEL_QUERY))
                    .thenReturn(SECOND_MODEL_ON_VERTICALS_FETCHED)
            block()
        }
    }

    @Test
    fun verifyHeaderAndSkipBtnShownAfterPromptFetched() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        verifyHeaderAndSkipButtonVisible(viewModel.uiState)
    }

    @Test
    fun verifyInputShownAfterPromptFetched() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        verifyEmptySearchInputVisible(viewModel.uiState)
    }

    @Test
    fun verifyHeaderAndSkipBtnNotShownOnNonEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        verifyHeaderAndSkipButtonHidden(viewModel.uiState)
    }

    @Test
    fun verifyHeaderAndSkipBtnShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        viewModel.updateQuery(EMPTY_STRING, ZERO_DELAY)
        verifyHeaderAndSkipButtonVisible(viewModel.uiState)
    }

    @Test
    fun verifySearchInputShownOnNonEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        verifyNonEmptySearchInputVisible(viewModel.uiState)
    }

    @Test
    fun verifySearchInputShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        viewModel.updateQuery(EMPTY_STRING, ZERO_DELAY)
        verifyEmptySearchInputVisible(viewModel.uiState)
    }

    @Test
    fun verifyClearSearchNotShownAfterPromptFetched() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        assertThat(viewModel.uiState.value!!.searchInputState.showClearButton).isFalse()
    }

    @Test
    fun verifyClearSearchShownOnNonEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        assertThat(viewModel.uiState.value!!.searchInputState.showClearButton).isTrue()
    }

    @Test
    fun verifyClearSearchNotShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        viewModel.updateQuery(EMPTY_STRING, ZERO_DELAY)
        assertThat(viewModel.uiState.value!!.searchInputState.showClearButton).isFalse()
    }

    @Test
    fun verifySearchProgressNotShownAfterPromptFetched() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        assertThat(viewModel.uiState.value!!.searchInputState.showProgress).isFalse()
    }

    @Test
    fun verifySearchProgressNotShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        assertThat(viewModel.uiState.value!!.searchInputState.showProgress).isFalse()
    }

    @Test
    fun verifySearchProgressNotShownOnNonEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        viewModel.updateQuery(EMPTY_STRING, ZERO_DELAY)
        assertThat(viewModel.uiState.value!!.searchInputState.showProgress).isFalse()
    }

    @Test
    fun verifyItemShownOnUpdateQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        verifyModelShown(viewModel.uiState, FIRST_MODEL_ID, FIRST_MODEL_NAME)
    }

    @Test
    fun verifyItemShownOnMultipleQueryUpdates() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        viewModel.updateQuery(SECOND_MODEL_QUERY, ZERO_DELAY)
        verifyModelShown(viewModel.uiState, SECOND_MODEL_ID, SECOND_MODEL_NAME)
    }

    @Test
    fun verifyOnClearBtnClickedPropagated() = test {
        viewModel.onClearTextBtnClicked()
        verify(clearBtnObserver).onChanged(anyOrNull())
    }

    @Test
    fun verifyFullscreenErrorShownOnFailedHeaderInfoRequest() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID)).thenReturn(FAILED_HEADER_PROMPT_FETCHED)
        viewModel.start(SEGMENT_ID)
        verifyFullscreenErrorShown(viewModel.uiState.value!!)
    }

    @Test
    fun verifyRetryWorksOnFullScreenError() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID)).thenReturn(FAILED_HEADER_PROMPT_FETCHED)
        viewModel.start(SEGMENT_ID)
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID))
                .thenReturn(SUCCESSFUL_HEADER_PROMPT_FETCHED)
        viewModel.onFetchSegmentsPromptRetry()

        verifyHeaderAndSkipButtonVisible(viewModel.uiState)
        verifyEmptySearchInputVisible(viewModel.uiState)
    }

    @Test
    fun verifyRetrySuggestionsItemShownOnFailedSuggestionsRequest() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID))
                .thenReturn(SUCCESSFUL_HEADER_PROMPT_FETCHED)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(ERROR_ON_VERTICALS_FETCHED)
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)
        verifyRetrySuggestionItemShown(viewModel.uiState)
    }

    @Test
    fun verifyFullscreenProgressShownWhenFetchingPrompt() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())

        verifyFullscreenProgressShown(captor.firstValue)
        verifyEmptySearchInputVisible(captor.secondValue)
    }

    @Test
    fun verifyFullscreenProgressShownOnRetry() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID)).thenReturn(FAILED_HEADER_PROMPT_FETCHED)
        viewModel.start(SEGMENT_ID)
        viewModel.onFetchSegmentsPromptRetry()

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(4)).onChanged(captor.capture())

        verifyFullscreenProgressShown(captor.firstValue)
        verifyFullscreenErrorShown(captor.secondValue)
        verifyFullscreenProgressShown(captor.thirdValue)
        verifyFullscreenErrorShown(captor.lastValue)
    }

    @Test
    fun verifySearchInputProgressShownOnFetchingSuggestions() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(4)).onChanged(captor.capture())

        verifyFullscreenProgressShown(captor.firstValue)
        verifyEmptySearchInputVisible(captor.secondValue)
        verifySearchInputWithProgressVisible(captor.thirdValue)
        verifyNonEmptySearchInputVisible(captor.lastValue)
    }

    @Test
    fun verifySearchInputProgressShownOnSuggestionsRetry() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID))
                .thenReturn(SUCCESSFUL_HEADER_PROMPT_FETCHED)
        whenever(fetchVerticalsUseCase.fetchVerticals(ERROR_ON_VERTICALS_FETCHED.searchQuery))
                .thenReturn(ERROR_ON_VERTICALS_FETCHED)
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(ERROR_MODEL_QUERY, ZERO_DELAY)

        // invoke retry
        (viewModel.uiState.value!!.items[0] as VerticalsFetchSuggestionsErrorUiState).onItemTapped!!.invoke()

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(5)).onChanged(captor.capture())

        // 1. Fullscreen Progress
        // 2. Header + Empty Input
        // 3. Input Progress
        // 4. Input Error

        // the last item is the 'inProgress' state as the result will never be returned
        // since we can't set throttle delay to 0 in onItemTapped
        verifySearchInputWithProgressVisible(captor.lastValue)
    }

    @Test
    fun verifyOnVerticalSelectedIsPropagated() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY, ZERO_DELAY)

        viewModel.uiState.value!!.items[0].onItemTapped!!.invoke()

        val selectedVerticalCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(verticalSelectedObserver).onChanged(selectedVerticalCaptor.capture())

        assertThat(selectedVerticalCaptor.allValues.size == 1).isTrue()
        assertThat(selectedVerticalCaptor.lastValue).isEqualTo(FIRST_MODEL_ID)
    }

    @Test
    fun verifyOnSkipIsPropagated() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.onSkipStepBtnClicked()
        val captor = ArgumentCaptor.forClass(String::class.java)
        verify(verticalSelectedObserver).onChanged(captor.capture())

        assertThat(captor.allValues.size == 1).isTrue()
        assertThat(captor.lastValue).isNull()
    }

    private fun verifyEmptySearchInputVisible(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyEmptySearchInputVisible(uiStateLiveData.value!!)
    }

    private fun verifyEmptySearchInputVisible(uiState: VerticalsUiState) {
        assertThat(uiState.contentState == CONTENT).isTrue()
        assertThat(uiState.searchInputState.isVisible).isTrue()
        assertThat(uiState.searchInputState.showProgress).isFalse()
        assertThat(uiState.searchInputState.showClearButton).isFalse()
        assertThat(uiState.searchInputState.hint).isEqualTo(DUMMY_HINT)
    }

    private fun verifyNonEmptySearchInputVisible(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyNonEmptySearchInputVisible(uiStateLiveData.value!!)
    }

    private fun verifyNonEmptySearchInputVisible(uiState: VerticalsUiState) {
        assertThat(uiState.contentState == CONTENT).isTrue()
        assertThat(uiState.searchInputState.isVisible).isTrue()
        assertThat(uiState.searchInputState.showProgress).isFalse()
        assertThat(uiState.searchInputState.showClearButton).isTrue()
        assertThat(uiState.searchInputState.hint).isEqualTo(DUMMY_HINT)
    }

    private fun verifySearchInputWithProgressVisible(uiState: VerticalsUiState) {
        assertThat(uiState.contentState == CONTENT).isTrue()
        assertThat(uiState.searchInputState.isVisible).isTrue()
        assertThat(uiState.searchInputState.showProgress).isTrue()
        assertThat(uiState.searchInputState.showClearButton).isTrue()
        assertThat(uiState.searchInputState.hint).isEqualTo(DUMMY_HINT)
    }

    private fun verifyHeaderAndSkipButtonVisible(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyHeaderAndSkipButtonVisible(uiStateLiveData.value!!)
    }

    private fun verifyHeaderAndSkipButtonVisible(uiState: VerticalsUiState) {
        assertThat(uiState.contentState == CONTENT).isTrue()
        assertThat(uiState.showSkipButton).isTrue()
        assertThat(uiState.headerUiState.isVisible).isTrue()
        assertThat(uiState.headerUiState.title).isEqualTo(DUMMY_TITLE)
        assertThat(uiState.headerUiState.subtitle).isEqualTo(DUMMY_SUBTITLE)
    }

    private fun verifyHeaderAndSkipButtonHidden(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyHeaderAndSkipButtonHidden(uiStateLiveData.value!!)
    }

    private fun verifyHeaderAndSkipButtonHidden(uiState: VerticalsUiState) {
        assertThat(uiState.contentState == CONTENT).isTrue()
        assertThat(uiState.showSkipButton).isFalse()
        assertThat(uiState.headerUiState.isVisible).isFalse()
    }

    private fun verifyFullscreenErrorShown(uiState: VerticalsUiState) {
        assertThat(uiState).isEqualTo(VerticalsUiState.VerticalsFullscreenErrorUiState)
    }

    private fun verifyFullscreenProgressShown(uiState: VerticalsUiState) {
        assertThat(uiState).isEqualTo(VerticalsUiState.VerticalsFullscreenProgressUiState)
    }

    private fun verifyModelShown(uiStateLiveData: LiveData<VerticalsUiState>, id: String, title: String) {
        val uiState = uiStateLiveData.value!!
        assertThat((uiState.items[0] as VerticalsModelUiState).id).isEqualTo(id)
        assertThat((uiState.items[0] as VerticalsModelUiState).title).isEqualTo(title)
    }

    private fun verifyRetrySuggestionItemShown(uiStateLiveData: LiveData<VerticalsUiState>) {
        assertThat(uiStateLiveData.value!!.items[0])
                .isInstanceOf(VerticalsFetchSuggestionsErrorUiState::class.java)
    }
}
