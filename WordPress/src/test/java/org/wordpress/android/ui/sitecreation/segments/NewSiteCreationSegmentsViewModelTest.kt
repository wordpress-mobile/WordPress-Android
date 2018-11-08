package org.wordpress.android.ui.sitecreation.segments

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.whenever
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
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
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.ItemUiState.SegmentUiState
import org.wordpress.android.ui.sitecreation.segments.NewSiteCreationSegmentsViewModel.UiState
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationSegmentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var mFetchSegmentsUseCase: FetchSegmentsUseCase
    private val firstModel =
            VerticalSegmentModel(
                    "dummyTitle",
                    "dummySubtitle",
                    "http://dummy.com",
                    123
            )
    private val secondModel =
            VerticalSegmentModel(
                    "dummyTitle",
                    "dummySubtitle",
                    "http://dummy.com",
                    999
            )

    private val progressState = UiState(false, true, listOf(HeaderUiState, ProgressUiState))
    private val errorState = UiState(true, false, emptyList())
    private val headerAndFirstItemState = UiState(
            false, true,
            listOf(
                    HeaderUiState,
                    SegmentUiState(
                            firstModel.segmentId,
                            firstModel.title,
                            firstModel.subtitle,
                            firstModel.iconUrl,
                            false
                    )
            )
    )
    private val headerAndSecondItemState = UiState(
            false, true,
            listOf(
                    HeaderUiState,
                    SegmentUiState(
                            secondModel.segmentId,
                            secondModel.title,
                            secondModel.subtitle,
                            secondModel.iconUrl,
                            false
                    )
            )
    )

    private val firstModelEvent = OnSegmentsFetched(listOf(firstModel))
    private val secondModelEvent = OnSegmentsFetched(listOf(secondModel))
    private val firstAndSecondModelEvent = OnSegmentsFetched(listOf(firstModel, secondModel))
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
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModelEvent)
        viewModel.start()

        assertTrue(viewModel.uiState.value!! == headerAndFirstItemState)
    }

    @Test
    fun onRetryFetchesCategories() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModelEvent)
        viewModel.start()

        assertTrue(viewModel.uiState.value!! == headerAndFirstItemState)

        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(secondModelEvent)
        viewModel.onRetryClicked()

        assertTrue(viewModel.uiState.value!! == headerAndSecondItemState)
    }

    @Test
    fun fetchCategoriesChangesStateToProgress() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstModelEvent)
        viewModel.start()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(progressState)
            verify(uiStateObserver).onChanged(headerAndFirstItemState)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun onErrorEventChangesStateToError() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(progressState)
            verify(uiStateObserver).onChanged(errorState)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun onSuccessfulRetryRemovesErrorState() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(errorEvent)
        viewModel.start()
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(secondModelEvent)
        viewModel.onRetryClicked()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(progressState)
            verify(uiStateObserver).onChanged(errorState)
            verify(uiStateObserver).onChanged(progressState)
            verify(uiStateObserver).onChanged(headerAndSecondItemState)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyLastItemDoesntShowDivider() = test {
        whenever(mFetchSegmentsUseCase.fetchCategories()).thenReturn(firstAndSecondModelEvent)
        viewModel.start()

        val items = viewModel.uiState.value!!.items
        assertFalse((items[items.size-1] as SegmentUiState).showDivider)
    }
}
