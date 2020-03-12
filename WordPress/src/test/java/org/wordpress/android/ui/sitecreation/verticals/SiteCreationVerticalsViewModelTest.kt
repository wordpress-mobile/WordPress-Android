package org.wordpress.android.ui.sitecreation.verticals

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.firstValue
import com.nhaarman.mockitokotlin2.lastValue
import com.nhaarman.mockitokotlin2.secondValue
import com.nhaarman.mockitokotlin2.thirdValue
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptError
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentPromptUseCase
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenProgressUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper

private const val SEGMENT_ID = 1L
private const val EMPTY_STRING = ""

private const val DUMMY_HINT = "dummyHint"
private const val DUMMY_TITLE = "dummyTitle"
private const val DUMMY_SUBTITLE = "dummySubtitle"

private const val FIRST_MODEL_QUERY = "success_1"

private val SUCCESSFUL_HEADER_PROMPT_FETCHED = OnSegmentPromptFetched(
        SEGMENT_ID,
        SegmentPromptModel(DUMMY_TITLE, DUMMY_SUBTITLE, DUMMY_HINT), error = null
)
private val FAILED_HEADER_PROMPT_FETCHED = OnSegmentPromptFetched(
        SEGMENT_ID,
        null,
        FetchSegmentPromptError(GENERIC_ERROR, message = null)
)

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SiteCreationVerticalsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchSegmentsPromptUseCase: FetchSegmentPromptUseCase
    @Mock private lateinit var uiStateObserver: Observer<VerticalsUiState>
    @Mock private lateinit var clearBtnObserver: Observer<Unit>
    @Mock private lateinit var verticalSelectedObserver: Observer<String?>
    @Mock private lateinit var skipBtnClickedObservable: Observer<Unit>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper

    private lateinit var viewModel: SiteCreationVerticalsViewModel

    @Before
    fun setUp() {
        viewModel = SiteCreationVerticalsViewModel(
                networkUtils,
                dispatcher,
                fetchSegmentsPromptUseCase,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        viewModel.uiState.observeForever(uiStateObserver)
        viewModel.clearBtnClicked.observeForever(clearBtnObserver)
        viewModel.verticalSelected.observeForever(verticalSelectedObserver)
        viewModel.skipBtnClicked.observeForever(skipBtnClickedObservable)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    private fun <T> testWithSuccessResponses(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID))
                    .thenReturn(SUCCESSFUL_HEADER_PROMPT_FETCHED)
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
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        verifyHeaderAndSkipButtonHidden(viewModel.uiState)
    }

    @Test
    fun verifyHeaderAndSkipBtnShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        viewModel.updateQuery(EMPTY_STRING)
        verifyHeaderAndSkipButtonVisible(viewModel.uiState)
    }

    @Test
    fun verifySearchInputShownOnNonEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        verifyNonEmptySearchInputVisible(viewModel.uiState)
    }

    @Test
    fun verifySearchInputShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        viewModel.updateQuery(EMPTY_STRING)
        verifyEmptySearchInputVisible(viewModel.uiState)
    }

    @Test
    fun verifyClearSearchNotShownAfterPromptFetched() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        assertThat((viewModel.uiState.value!! as VerticalsContentUiState).searchInputUiState.showClearButton).isFalse()
    }

    @Test
    fun verifyClearSearchShownOnNonEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        assertThat((viewModel.uiState.value!! as VerticalsContentUiState).searchInputUiState.showClearButton).isTrue()
    }

    @Test
    fun verifyClearSearchNotShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        viewModel.updateQuery(EMPTY_STRING)
        assertThat((viewModel.uiState.value!! as VerticalsContentUiState).searchInputUiState.showClearButton).isFalse()
    }

    @Test
    fun verifySearchProgressNotShownAfterPromptFetched() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        assertThat((viewModel.uiState.value!! as VerticalsContentUiState).searchInputUiState.showProgress).isFalse()
    }

    @Test
    fun verifySearchProgressNotShownOnEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        assertThat((viewModel.uiState.value!! as VerticalsContentUiState).searchInputUiState.showProgress).isFalse()
    }

    @Test
    fun verifySearchProgressNotShownOnNonEmptyQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        viewModel.updateQuery(EMPTY_STRING)
        assertThat((viewModel.uiState.value!! as VerticalsContentUiState).searchInputUiState.showProgress).isFalse()
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
        verifyGenericFullscreenErrorShown(viewModel.uiState.value!! as VerticalsFullscreenErrorUiState)
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
    fun verifyFullscreenProgressShownWhenFetchingPrompt() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(2)).onChanged(captor.capture())

        verifyFullscreenProgressShown(captor.firstValue as VerticalsFullscreenProgressUiState)
        verifyEmptySearchInputVisible(captor.secondValue as VerticalsContentUiState)
    }

    @Test
    fun verifyFullscreenProgressShownOnRetry() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID)).thenReturn(FAILED_HEADER_PROMPT_FETCHED)
        viewModel.start(SEGMENT_ID)
        viewModel.onFetchSegmentsPromptRetry()

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(4)).onChanged(captor.capture())

        verifyFullscreenProgressShown(captor.firstValue as VerticalsFullscreenProgressUiState)
        verifyGenericFullscreenErrorShown(captor.secondValue as VerticalsFullscreenErrorUiState)
        verifyFullscreenProgressShown(captor.thirdValue as VerticalsFullscreenProgressUiState)
        verifyGenericFullscreenErrorShown(captor.lastValue as VerticalsFullscreenErrorUiState)
    }

    @Test
    fun verifyOnSkipIsPropagated() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.onSkipStepBtnClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(skipBtnClickedObservable).onChanged(captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
    }

    @Test
    fun verifyOnHelpClickedPropagated() = testWithSuccessResponses {
        viewModel.onHelpClicked()
        val captor = ArgumentCaptor.forClass(Unit::class.java)
        verify(onHelpClickedObserver).onChanged(captor.capture())

        assertThat(captor.allValues.size).isEqualTo(1)
    }

    @Test
    fun verifyNoConnectionErrorShownOnFetchPrompt() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        viewModel.start(SEGMENT_ID)

        verifyNoConnectionFullscreenErrorShown(viewModel.uiState.value!! as VerticalsFullscreenErrorUiState)
    }

    private fun verifyEmptySearchInputVisible(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyEmptySearchInputVisible(uiStateLiveData.value!! as VerticalsContentUiState)
    }

    private fun verifyEmptySearchInputVisible(uiState: VerticalsContentUiState) {
        assertThat(uiState.searchInputUiState.showProgress).isFalse()
        assertThat(uiState.searchInputUiState.showClearButton).isFalse()
        assertThat(uiState.searchInputUiState.hint).isEqualTo(UiStringText(DUMMY_HINT))
    }

    private fun verifyNonEmptySearchInputVisible(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyNonEmptySearchInputVisible(uiStateLiveData.value!! as VerticalsContentUiState)
    }

    private fun verifyNonEmptySearchInputVisible(uiState: VerticalsContentUiState) {
        assertThat(uiState.searchInputUiState.showProgress).isFalse()
        assertThat(uiState.searchInputUiState.showClearButton).isTrue()
        assertThat(uiState.searchInputUiState.hint).isEqualTo(UiStringText(DUMMY_HINT))
    }

    private fun verifyHeaderAndSkipButtonVisible(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyHeaderAndSkipButtonVisible(uiStateLiveData.value!! as VerticalsContentUiState)
    }

    private fun verifyHeaderAndSkipButtonVisible(uiState: VerticalsContentUiState) {
        assertThat(uiState.showSkipButton).isTrue()
        assertThat(uiState.headerUiState).isNotNull()
        assertThat(uiState.headerUiState!!.title).isEqualTo(UiStringText(DUMMY_TITLE))
        assertThat(uiState.headerUiState!!.subtitle).isEqualTo(UiStringText(DUMMY_SUBTITLE))
    }

    private fun verifyHeaderAndSkipButtonHidden(uiStateLiveData: LiveData<VerticalsUiState>) {
        verifyHeaderAndSkipButtonHidden(uiStateLiveData.value!! as VerticalsContentUiState)
    }

    private fun verifyHeaderAndSkipButtonHidden(uiState: VerticalsContentUiState) {
        assertThat(uiState.showSkipButton).isFalse()
        assertThat(uiState.headerUiState).isNull()
    }

    private fun verifyGenericFullscreenErrorShown(uiState: VerticalsFullscreenErrorUiState) {
        assertThat(uiState).isEqualTo(VerticalsUiState.VerticalsFullscreenErrorUiState.VerticalsGenericErrorUiState)
    }

    private fun verifyNoConnectionFullscreenErrorShown(uiState: VerticalsFullscreenErrorUiState) {
        assertThat(uiState).isEqualTo(VerticalsUiState.VerticalsFullscreenErrorUiState.VerticalsConnectionErrorUiState)
    }

    private fun verifyFullscreenProgressShown(uiState: VerticalsFullscreenProgressUiState) {
        assertThat(uiState).isEqualTo(VerticalsUiState.VerticalsFullscreenProgressUiState)
    }
}
