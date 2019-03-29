package org.wordpress.android.ui.sitecreation.verticals

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
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
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentPromptError
import org.wordpress.android.fluxc.store.VerticalStore.FetchVerticalsError
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentPromptUseCase
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsCustomModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsFetchSuggestionsErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenErrorUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsUiState.VerticalsFullscreenProgressUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper

private const val SEGMENT_ID = 1L
private const val EMPTY_STRING = ""

private const val DUMMY_HINT = "dummyHint"
private const val DUMMY_TITLE = "dummyTitle"
private const val DUMMY_SUBTITLE = "dummySubtitle"
private const val DUMMY_SLUG = "dummySlug"

private const val FIRST_MODEL_QUERY = "success_1"
private const val SECOND_MODEL_QUERY = "success_2"
private const val CUSTOM_CATEGORY_MODEL_QUERY = "success_custom_category"
private const val ERROR_MODEL_QUERY = "error"

private const val FIRST_MODEL_NAME = "firstModel"
private const val FIRST_MODEL_ID = "1"
private const val SECOND_MODEL_NAME = "secondModel"
private const val SECOND_MODEL_ID = "2"
private const val CUSTOM_CATEGORY_MODEL_NAME = "customCategoryModel"
private const val CUSTOM_CATEGORY_MODEL_ID = "3"

private val SUCCESSFUL_HEADER_PROMPT_FETCHED = OnSegmentPromptFetched(
        SEGMENT_ID,
        SegmentPromptModel(DUMMY_TITLE, DUMMY_SUBTITLE, DUMMY_HINT), error = null
)
private val FAILED_HEADER_PROMPT_FETCHED = OnSegmentPromptFetched(
        SEGMENT_ID,
        null,
        FetchSegmentPromptError(GENERIC_ERROR, message = null)
)

private val FIRST_MODEL_ON_VERTICALS_FETCHED = OnVerticalsFetched(
        FIRST_MODEL_QUERY,
        listOf(VerticalModel(
                name = FIRST_MODEL_NAME,
                verticalId = FIRST_MODEL_ID,
                isUserInputVertical = false,
                verticalSlug = DUMMY_SLUG
        )),
        error = null
)
private val SECOND_MODEL_ON_VERTICALS_FETCHED = OnVerticalsFetched(
        SECOND_MODEL_QUERY,
        listOf(VerticalModel(
                name = SECOND_MODEL_NAME,
                verticalId = SECOND_MODEL_ID,
                isUserInputVertical = false,
                verticalSlug = DUMMY_SLUG
        )),
        error = null
)
private val CUSTOM_CATEGORY_MODEL_ON_VERTICALS_FETCHED = OnVerticalsFetched(
        CUSTOM_CATEGORY_MODEL_QUERY,
        listOf(VerticalModel(
                name = CUSTOM_CATEGORY_MODEL_NAME,
                verticalId = CUSTOM_CATEGORY_MODEL_ID,
                isUserInputVertical = true,
                verticalSlug = DUMMY_SLUG
        )),
        error = null
)
private val ERROR_ON_VERTICALS_FETCHED = OnVerticalsFetched(
        ERROR_MODEL_QUERY,
        emptyList(),
        FetchVerticalsError(GENERIC_ERROR, message = null)
)

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationVerticalsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchVerticalsUseCase: FetchVerticalsUseCase
    @Mock lateinit var fetchSegmentsPromptUseCase: FetchSegmentPromptUseCase
    @Mock lateinit var tracker: NewSiteCreationTracker
    @Mock private lateinit var uiStateObserver: Observer<VerticalsUiState>
    @Mock private lateinit var clearBtnObserver: Observer<Unit>
    @Mock private lateinit var verticalSelectedObserver: Observer<String?>
    @Mock private lateinit var skipBtnClickedObservable: Observer<Unit>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>
    @Mock private lateinit var networkUtils: NetworkUtilsWrapper

    private lateinit var viewModel: NewSiteCreationVerticalsViewModel

    @Before
    fun setUp() {
        viewModel = NewSiteCreationVerticalsViewModel(
                networkUtils,
                dispatcher,
                fetchSegmentsPromptUseCase,
                fetchVerticalsUseCase,
                tracker,
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
            whenever(fetchVerticalsUseCase.fetchVerticals(FIRST_MODEL_QUERY))
                    .thenReturn(FIRST_MODEL_ON_VERTICALS_FETCHED)
            whenever(fetchVerticalsUseCase.fetchVerticals(SECOND_MODEL_QUERY))
                    .thenReturn(SECOND_MODEL_ON_VERTICALS_FETCHED)
            whenever(fetchVerticalsUseCase.fetchVerticals(CUSTOM_CATEGORY_MODEL_QUERY))
                    .thenReturn(CUSTOM_CATEGORY_MODEL_ON_VERTICALS_FETCHED)
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
    fun verifyItemShownOnUpdateQuery() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        verifyModelShown(viewModel.uiState, FIRST_MODEL_ID, FIRST_MODEL_NAME)
    }

    @Test
    fun verifyItemShownOnMultipleQueryUpdates() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        viewModel.updateQuery(SECOND_MODEL_QUERY)
        verifyModelShown(viewModel.uiState, SECOND_MODEL_ID, SECOND_MODEL_NAME)
    }

    @Test
    fun verifyCustomCategoryItemHasValidType() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(CUSTOM_CATEGORY_MODEL_QUERY)
        assertThat((viewModel.uiState.value!! as VerticalsContentUiState).items[0])
                .isInstanceOf(VerticalsCustomModelUiState::class.java)
    }

    @Test
    fun verifyCustomCategoryItemShown() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(CUSTOM_CATEGORY_MODEL_QUERY)
        verifyCustomCategoryModelShown(viewModel.uiState, CUSTOM_CATEGORY_MODEL_ID, CUSTOM_CATEGORY_MODEL_NAME)
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
    fun verifyRetrySuggestionsItemShownOnFailedSuggestionsRequest() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID))
                .thenReturn(SUCCESSFUL_HEADER_PROMPT_FETCHED)
        whenever(fetchVerticalsUseCase.fetchVerticals(any())).thenReturn(ERROR_ON_VERTICALS_FETCHED)
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)
        verifyUnknownErrorRetryItemShown(viewModel.uiState.value!! as VerticalsContentUiState)
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
    fun verifySearchInputProgressShownOnFetchingSuggestions() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(4)).onChanged(captor.capture())

        verifyFullscreenProgressShown(captor.firstValue as VerticalsFullscreenProgressUiState)
        verifyEmptySearchInputVisible(captor.secondValue as VerticalsContentUiState)
        verifySearchInputWithProgressVisible(captor.thirdValue as VerticalsContentUiState)
        verifyNonEmptySearchInputVisible(captor.lastValue as VerticalsContentUiState)
    }

    @Test
    fun verifySearchInputProgressShownOnSuggestionsRetry() = test {
        whenever(fetchSegmentsPromptUseCase.fetchSegmentsPrompt(SEGMENT_ID))
                .thenReturn(SUCCESSFUL_HEADER_PROMPT_FETCHED)
        whenever(fetchVerticalsUseCase.fetchVerticals(ERROR_ON_VERTICALS_FETCHED.searchQuery))
                .thenReturn(ERROR_ON_VERTICALS_FETCHED)
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(ERROR_MODEL_QUERY)

        // invoke retry
        ((viewModel.uiState.value!! as VerticalsContentUiState).items[0] as VerticalsFetchSuggestionsErrorUiState)
                .onItemTapped!!.invoke()

        val captor = ArgumentCaptor.forClass(VerticalsUiState::class.java)
        verify(uiStateObserver, times(6)).onChanged(captor.capture())

        // [0] Fullscreen Progress
        verifyFullscreenProgressShown(captor.allValues[0] as VerticalsFullscreenProgressUiState)
        // [1] Header + Empty Input
        verifyHeaderAndSkipButtonVisible(captor.allValues[1] as VerticalsContentUiState)
        // [2] Input Progress
        verifySearchInputWithProgressVisible(captor.allValues[2] as VerticalsContentUiState)
        // [3] Input Error
        verifyUnknownErrorRetryItemShown(captor.allValues[3] as VerticalsContentUiState)
        // [4] Input Progress
        verifySearchInputWithProgressVisible(captor.allValues[4] as VerticalsContentUiState)
    }

    @Test
    fun verifyOnVerticalSelectedIsPropagated() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        viewModel.updateQuery(FIRST_MODEL_QUERY)

        (viewModel.uiState.value!! as VerticalsContentUiState).items[0].onItemTapped!!.invoke()

        val selectedVerticalCaptor = ArgumentCaptor.forClass(String::class.java)
        verify(verticalSelectedObserver).onChanged(selectedVerticalCaptor.capture())

        assertThat(selectedVerticalCaptor.allValues.size).isEqualTo(1)
        assertThat(selectedVerticalCaptor.lastValue).isEqualTo(FIRST_MODEL_ID)
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

    @Test
    fun verifyErrorShownOnFetchVerticals() = testWithSuccessResponses {
        viewModel.start(SEGMENT_ID)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        viewModel.updateQuery(FIRST_MODEL_QUERY)

        verifyNoConnectionRetryItemShown(viewModel.uiState.value!! as VerticalsContentUiState)
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

    private fun verifySearchInputWithProgressVisible(uiState: VerticalsContentUiState) {
        assertThat(uiState.searchInputUiState.showProgress).isTrue()
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

    private fun verifyModelShown(uiStateLiveData: LiveData<VerticalsUiState>, id: String, title: String) {
        val uiState = uiStateLiveData.value!! as VerticalsContentUiState
        verifyModelShown(uiState, id, title)
    }

    private fun verifyModelShown(uiState: VerticalsContentUiState, id: String, title: String) {
        assertThat((uiState.items[0] as VerticalsModelUiState).id).isEqualTo(id)
        assertThat((uiState.items[0] as VerticalsModelUiState).title).isEqualTo(title)
    }

    private fun verifyCustomCategoryModelShown(
        uiStateLiveData: LiveData<VerticalsUiState>,
        id: String,
        title: String
    ) {
        val uiState = uiStateLiveData.value!! as VerticalsContentUiState
        assertThat((uiState.items[0] as VerticalsCustomModelUiState).id).isEqualTo(id)
        assertThat((uiState.items[0] as VerticalsCustomModelUiState).title).isEqualTo(title)
    }

    private fun verifyUnknownErrorRetryItemShown(uiState: VerticalsContentUiState) {
        assertThat(uiState.items[0]).isInstanceOf(VerticalsFetchSuggestionsErrorUiState::class.java)
        assertThat((uiState.items[0] as VerticalsFetchSuggestionsErrorUiState).messageResId)
                .isEqualTo(R.string.site_creation_fetch_suggestions_error_unknown)
    }

    private fun verifyNoConnectionRetryItemShown(uiState: VerticalsContentUiState) {
        assertThat(uiState.items[0]).isInstanceOf(VerticalsFetchSuggestionsErrorUiState::class.java)
        assertThat((uiState.items[0] as VerticalsFetchSuggestionsErrorUiState).messageResId)
                .isEqualTo(R.string.site_creation_fetch_suggestions_error_no_connection)
    }
}
