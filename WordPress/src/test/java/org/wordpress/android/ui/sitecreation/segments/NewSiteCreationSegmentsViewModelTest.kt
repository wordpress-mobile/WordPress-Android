package org.wordpress.android.ui.sitecreation.segments

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.Observer
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.InternalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalSegmentModel
import org.wordpress.android.fluxc.store.VerticalStore.FetchSegmentsError
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.fluxc.store.VerticalStore.VerticalErrorType.GENERIC_ERROR
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.misc.NewSiteCreationTracker
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.HeaderUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.ProgressUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsItemUiState.SegmentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsContentUiState
import org.wordpress.android.ui.sitecreation.segments.SegmentsUiState.SegmentsErrorUiState
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper

private const val FIRST_MODEL_TITLE = "first_title"
private const val FIRST_MODEL_SUBTITLE = "first_subtitle"
private const val FIRST_MODEL_ICON_URL = "http://first_url.com"
private const val FIRST_MODEL_ICON_COLOR = "first_icon_color"
private const val FIRST_MODEL_SEGMENT_ID = 1L
private const val FIRST_MODEL_IS_MOBILE_SEGMENT = true

private const val SECOND_MODEL_TITLE = "second_title"
private const val SECOND_MODEL_SUBTITLE = "second_subtitle"
private const val SECOND_MODEL_ICON_URL = "http://second_url.com"
private const val SECOND_MODEL_ICON_COLOR = "second_icon_color"
private const val SECOND_MODEL_SEGMENT_ID = 2L
private const val SECOND_MODEL_IS_MOBILE_SEGMENT = true

private const val THIRD_MODEL_TITLE = "third_title"
private const val THIRD_MODEL_SUBTITLE = "third_subtitle"
private const val THIRD_MODEL_ICON_URL = "http://third_url.com"
private const val THIRD_MODEL_ICON_COLOR = "third_icon_color"
private const val THIRD_MODEL_SEGMENT_ID = 3L
private const val THIRD_MODEL_IS_MOBILE_SEGMENT = false

private const val ERROR_MESSAGE = "dummy_error_message"

private val FIRST_MODEL =
        VerticalSegmentModel(
                title = FIRST_MODEL_TITLE,
                subtitle = FIRST_MODEL_SUBTITLE,
                iconUrl = FIRST_MODEL_ICON_URL,
                iconColor = FIRST_MODEL_ICON_COLOR,
                segmentId = FIRST_MODEL_SEGMENT_ID,
                isMobileSegment = FIRST_MODEL_IS_MOBILE_SEGMENT
        )

private val SECOND_MODEL =
        VerticalSegmentModel(
                title = SECOND_MODEL_TITLE,
                subtitle = SECOND_MODEL_SUBTITLE,
                iconUrl = SECOND_MODEL_ICON_URL,
                iconColor = SECOND_MODEL_ICON_COLOR,
                segmentId = SECOND_MODEL_SEGMENT_ID,
                isMobileSegment = SECOND_MODEL_IS_MOBILE_SEGMENT
        )

private val THIRD_MODEL =
        VerticalSegmentModel(
                title = THIRD_MODEL_TITLE,
                subtitle = THIRD_MODEL_SUBTITLE,
                iconUrl = THIRD_MODEL_ICON_URL,
                iconColor = THIRD_MODEL_ICON_COLOR,
                segmentId = THIRD_MODEL_SEGMENT_ID,
                isMobileSegment = THIRD_MODEL_IS_MOBILE_SEGMENT
        )

private val PROGRESS_STATE = SegmentsContentUiState(listOf(HeaderUiState, ProgressUiState))
private val HEADER_AND_FIRST_ITEM_STATE = SegmentsContentUiState(
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
private val HEADER_AND_SECOND_ITEM_STATE = SegmentsContentUiState(
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
private val FIRST_SECOND_AND_THIRD_MODEL_EVENT = OnSegmentsFetched(listOf(FIRST_MODEL, SECOND_MODEL, THIRD_MODEL))
private val ERROR_EVENT = OnSegmentsFetched(emptyList(), FetchSegmentsError(GENERIC_ERROR, ERROR_MESSAGE))

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class NewSiteCreationSegmentsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var fetchSegmentsUseCase: FetchSegmentsUseCase
    @Mock lateinit var networkUtils: NetworkUtilsWrapper

    private lateinit var viewModel: NewSiteCreationSegmentsViewModel

    @Mock private lateinit var tracker: NewSiteCreationTracker
    @Mock private lateinit var uiStateObserver: Observer<SegmentsUiState>
    @Mock private lateinit var segmentSelectedObserver: Observer<Long>
    @Mock private lateinit var onHelpClickedObserver: Observer<Unit>

    @Before
    fun setUp() {
        viewModel = NewSiteCreationSegmentsViewModel(
                networkUtils,
                dispatcher,
                fetchSegmentsUseCase,
                tracker,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
        viewModel.segmentsUiState.observeForever(uiStateObserver)
        viewModel.segmentSelected.observeForever(segmentSelectedObserver)
        viewModel.onHelpClicked.observeForever(onHelpClickedObserver)
        whenever(networkUtils.isNetworkAvailable()).thenReturn(true)
    }

    @Test
    fun onStartFetchesCategories() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_MODEL_EVENT)
        viewModel.start()

        assertTrue(viewModel.segmentsUiState.value!! == HEADER_AND_FIRST_ITEM_STATE)
    }

    @Test
    fun onlyMobileSegmentsAreShown() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_SECOND_AND_THIRD_MODEL_EVENT)
        viewModel.start()

        // Find the segment items
        val items = (viewModel.segmentsUiState.value!! as SegmentsContentUiState).items
        val segmentUiStates = items.mapNotNull { it as? SegmentUiState }

        // Verify that there are only 2 segments items and third segment is filtered out since it's not a mobile segment
        assertThat(segmentUiStates.size, `is`(2))
        assertNull(segmentUiStates.find { it.segmentId == THIRD_MODEL_SEGMENT_ID })
    }

    @Test
    fun onRetryFetchesCategories() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_MODEL_EVENT)
        viewModel.start()

        assertTrue(viewModel.segmentsUiState.value!! == HEADER_AND_FIRST_ITEM_STATE)

        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(SECOND_MODEL_EVENT)
        viewModel.onRetryClicked()

        assertTrue(viewModel.segmentsUiState.value!! == HEADER_AND_SECOND_ITEM_STATE)
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
            verify(uiStateObserver).onChanged(SegmentsErrorUiState.SegmentsGenericErrorUiState)
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
            verify(uiStateObserver).onChanged(SegmentsErrorUiState.SegmentsGenericErrorUiState)
            verify(uiStateObserver).onChanged(PROGRESS_STATE)
            verify(uiStateObserver).onChanged(HEADER_AND_SECOND_ITEM_STATE)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyLastItemDoesNotShowDivider() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_AND_SECOND_MODEL_EVENT)
        viewModel.start()

        val items = (viewModel.segmentsUiState.value!! as SegmentsContentUiState).items
        assertFalse((items[items.size - 1] as SegmentUiState).showDivider)
    }

    @Test
    fun verifyOnSegmentSelectedIsPropagated() = test {
        whenever(fetchSegmentsUseCase.fetchCategories()).thenReturn(FIRST_MODEL_EVENT)
        viewModel.start()
        ((viewModel.segmentsUiState.value!! as SegmentsContentUiState).items[1] as SegmentUiState)
                .onItemTapped!!.invoke()

        inOrder(segmentSelectedObserver).apply {
            verify(segmentSelectedObserver).onChanged(FIRST_MODEL_SEGMENT_ID)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyNoConnectionErrorShown() = test {
        whenever(networkUtils.isNetworkAvailable()).thenReturn(false)
        viewModel.start()

        inOrder(uiStateObserver).apply {
            verify(uiStateObserver).onChanged(PROGRESS_STATE)
            verify(uiStateObserver).onChanged(SegmentsErrorUiState.SegmentsConnectionErrorUiState)
            verifyNoMoreInteractions()
        }
    }

    @Test
    fun verifyOnHelpClickedPropagated() = test {
        viewModel.onHelpClicked()
        inOrder(onHelpClickedObserver).apply {
            verify(onHelpClickedObserver).onChanged(anyOrNull())
            verifyNoMoreInteractions()
        }
    }
}
