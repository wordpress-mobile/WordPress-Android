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

private const val FIRST_MODEL_TITLE = "first_title"
private const val FIRST_MODEL_SUBTITLE = "first_subtitle"
private const val FIRST_MODEL_ICON_URL = "http://first_url.com"
private const val FIRST_MODEL_ICON_COLOR = "first_icon_color"
private const val FIRST_MODEL_SEGMENT_ID = 1L

private const val SECOND_MODEL_TITLE = "second_title"
private const val SECOND_MODEL_SUBTITLE = "second_subtitle"
private const val SECOND_MODEL_ICON_URL = "http://second_url.com"
private const val SECOND_MODEL_ICON_COLOR = "second_icon_color"
private const val SECOND_MODEL_SEGMENT_ID = 2L

private const val ERROR_MESSAGE = "dummy_error_message"

private val FIRST_MODEL =
        VerticalSegmentModel(
                FIRST_MODEL_TITLE,
                FIRST_MODEL_SUBTITLE,
                FIRST_MODEL_ICON_URL,
                FIRST_MODEL_ICON_COLOR,
                FIRST_MODEL_SEGMENT_ID
        )

private val SECOND_MODEL =
        VerticalSegmentModel(
                SECOND_MODEL_TITLE,
                SECOND_MODEL_SUBTITLE,
                SECOND_MODEL_ICON_URL,
                SECOND_MODEL_ICON_COLOR,
                SECOND_MODEL_SEGMENT_ID
        )

private val PROGRESS_STATE = UiState(false, true, listOf(HeaderUiState, ProgressUiState))
private val ERROR_STATE = UiState(true, false, emptyList())
private val HEADER_AND_FIRST_ITEM_STATE = UiState(
        false, true,
        listOf(
                HeaderUiState,
                SegmentUiState(
                        FIRST_MODEL.segmentId,
                        FIRST_MODEL.title,
                        FIRST_MODEL.subtitle,
                        FIRST_MODEL.iconUrl,
                        FIRST_MODEL_ICON_COLOR,
                        false
                )
        )
)
private val HEADER_AND_SECOND_ITEM_STATE = UiState(
        false, true,
        listOf(
                HeaderUiState,
                SegmentUiState(
                        SECOND_MODEL.segmentId,
                        SECOND_MODEL.title,
                        SECOND_MODEL.subtitle,
                        SECOND_MODEL.iconUrl,
                        SECOND_MODEL.iconColor,
                        false
                )
        )
)

private val FIRST_MODEL_EVENT = OnSegmentsFetched(listOf(FIRST_MODEL))
private val SECOND_MODEL_EVENT = OnSegmentsFetched(listOf(SECOND_MODEL))
private val FIRST_AND_SECOND_MODEL_EVENT = OnSegmentsFetched(listOf(FIRST_MODEL, SECOND_MODEL))
private val ERROR_EVENT = OnSegmentsFetched(emptyList(), FetchSegmentsError(GENERIC_ERROR, ERROR_MESSAGE))

@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationSegmentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchSegmentsUseCase: FetchSegmentsUseCase

    private lateinit var viewModel: NewSiteCreationSegmentsViewModel

    @Mock private lateinit var uiStateObserver: Observer<UiState>

    @Before
    fun setUp() {
        viewModel = NewSiteCreationSegmentsViewModel(
                dispatcher,
                fetchSegmentsUseCase,
                Dispatchers.Unconfined,
                Dispatchers.Unconfined
        )
        viewModel.uiState.observeForever(uiStateObserver)
    }

    @Test
    fun onStartFetchesCategories() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_MODEL_EVENT)
        viewModel.start()

        assertTrue(viewModel.uiState.value!! == HEADER_AND_FIRST_ITEM_STATE)
    }

    @Test
    fun onRetryFetchesCategories() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_MODEL_EVENT)
        viewModel.start()

        assertTrue(viewModel.uiState.value!! == HEADER_AND_FIRST_ITEM_STATE)

        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(SECOND_MODEL_EVENT)
        viewModel.onRetryClicked()

        assertTrue(viewModel.uiState.value!! == HEADER_AND_SECOND_ITEM_STATE)
    }

    @Test
    fun fetchCategoriesChangesStateToProgress() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_MODEL_EVENT)
        viewModel.start()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(PROGRESS_STATE)
            verify(uiStateObserver).onChanged(HEADER_AND_FIRST_ITEM_STATE)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun onErrorEventChangesStateToError() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(ERROR_EVENT)
        viewModel.start()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(PROGRESS_STATE)
            verify(uiStateObserver).onChanged(ERROR_STATE)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun onSuccessfulRetryRemovesErrorState() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(ERROR_EVENT)
        viewModel.start()
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(SECOND_MODEL_EVENT)
        viewModel.onRetryClicked()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(PROGRESS_STATE)
            verify(uiStateObserver).onChanged(ERROR_STATE)
            verify(uiStateObserver).onChanged(PROGRESS_STATE)
            verify(uiStateObserver).onChanged(HEADER_AND_SECOND_ITEM_STATE)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyLastItemDoesNotShowDivider() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_AND_SECOND_MODEL_EVENT)
        viewModel.start()

        val items = viewModel.uiState.value!!.items
        assertFalse((items[items.size - 1] as SegmentUiState).showDivider)
    }
}
