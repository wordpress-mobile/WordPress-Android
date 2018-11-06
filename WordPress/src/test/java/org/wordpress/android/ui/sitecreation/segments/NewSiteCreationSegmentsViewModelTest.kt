package org.wordpress.android.ui.sitecreation.segments

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.experimental.Dispatchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentsError
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.UiState
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationSegmentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var mFetchSegmentsUseCase: FetchSegmentsUseCase
    private val firstModel = OnSegmentsFetched(
            listOf(
                    VerticalSegmentModel(
                            "dummyTitle",
                            "dummySubtitle",
                            "http://dummy.com",
                            123
                    )
            )
    )
    private val secondDummyEvent = OnSegmentsFetched(
            listOf(
                    VerticalSegmentModel(
                            "dummyTitle",
                            "dummySubtitle",
                            "http://dummy.com",
                            999
                    )
            )
    )
    private val errorEvent = OnSegmentsFetched(emptyList(), FetchSegmentsError(GENERIC_ERROR, "dummyError"))
    private lateinit var viewModel: NewSiteCreationSegmentsViewModel

    @Mock private lateinit var uiStateObserver: Observer<UiState>

    @Before
    fun setUp() {
        viewModel = NewSiteCreationSegmentsViewModel(
                dispatcher,
                mFetchSegmentsUseCase,
                Dispatchers.Unconfined,
                Dispatchers.Unconfined
        )
        val uiStateObservable = viewModel.uiState
        uiStateObservable.observeForever(uiStateObserver)
    }

    @Test
    fun onStartFetchesCategories() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModel)
        viewModel.start()

        assert(viewModel.uiState.value!!.data == firstModel.segmentList)
    }

    @Test
    fun onRetryFetchesCategories() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModel)
        viewModel.start()

        assert(viewModel.uiState.value!!.data == firstModel.segmentList)

        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(secondDummyEvent)
        viewModel.onRetryClicked()

        assert(viewModel.uiState.value!!.data == secondDummyEvent.segmentList)
    }

    @Test
    fun fetchCategoriesChangesStateToProgress() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModel)
        viewModel.start()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(
                    UiState(
                            showProgress = true,
                            showHeader = true
                    )
            )
            verify(uiStateObserver).onChanged(
                    UiState(
                            showHeader = true,
                            showList = true,
                            data = firstModel.segmentList
                    )
            )
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun onErrorEventChangesStateToError() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(
                    UiState(
                            showProgress = true,
                            showHeader = true
                    )
            )
            verify(uiStateObserver).onChanged(UiState(showError = true))
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun onSuccessfulRetryRemovesErrorState() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(secondDummyEvent)
        viewModel.onRetryClicked()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(
                    UiState(
                            showProgress = true,
                            showHeader = true
                    )
            )
            verify(uiStateObserver).onChanged(
                    UiState(
                            showError = true
                    )
            )
            verify(uiStateObserver).onChanged(
                    UiState(
                            showProgress = true,
                            showHeader = true
                    )
            )
            verify(uiStateObserver).onChanged(
                    UiState(
                            showHeader = true,
                            showList = true,
                            data = secondDummyEvent.segmentList
                    )
            )
            verifyNoMoreInteractions()
        }
    }
}
