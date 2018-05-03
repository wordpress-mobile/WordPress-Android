package org.wordpress.android.viewmodel.activitylog

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITIES
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityError
import org.wordpress.android.fluxc.store.ActivityLogStore.ActivityLogErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus

@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    private val actionCaptor = argumentCaptor<Action<Any>>()

    private var events: MutableList<List<ActivityLogModel>?> = mutableListOf()
    private var eventListStatuses: MutableList<ActivityLogListStatus?> = mutableListOf()
    private val activityLog = listOf<ActivityLogModel>(mock())
    private lateinit var viewModel: ActivityLogViewModel

    @Before
    fun setUp() {
        viewModel = ActivityLogViewModel(dispatcher, store)
        viewModel.site = site
        viewModel.events.observeForever { events.add(it) }
        viewModel.eventListStatus.observeForever { eventListStatuses.add(it) }
        whenever(store.getActivityLogForSite(site, false)).thenReturn(activityLog)
    }

    @Test
    fun onStartEmitsDataFromStoreAndStartsFetching() {
        assertNull(viewModel.events.value)
        assertNull(viewModel.eventListStatus.value)

        viewModel.start()

        assertEquals(viewModel.events.value, activityLog)
        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.FETCHING)

        assertFetchEvents()
    }

    @Test
    fun fetchesEventsOnPullToRefresh() {
        viewModel.pullToRefresh()

        assertFetchEvents()
    }

    @Test
    fun doesNotFetchEventsWhenAlreadyFetching() {
        viewModel.pullToRefresh()

        reset(dispatcher)

        viewModel.pullToRefresh()

        verify(dispatcher, never()).dispatch(any())
    }

    @Test(expected = NoSuchFieldError::class)
    fun onFetchingErrorPostsErrorState() {
        viewModel.onActivityLogFetched(OnActivityLogFetched(ActivityError(GENERIC_ERROR), FETCH_ACTIVITIES))

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.ERROR)
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCanLoadMore() {
        val canLoadMore = true
        viewModel.onActivityLogFetched(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        assertEquals(viewModel.events.value, activityLog)

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.CAN_LOAD_MORE)
    }

    @Test
    fun onDataFetchedLoadsMoreDataIfCanLoadMore() {
        val canLoadMore = true
        viewModel.onActivityLogFetched(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.loadMore()

        assertFetchEvents(canLoadMore)
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCannotLoadMore() {
        val canLoadMore = false
        viewModel.onActivityLogFetched(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        assertEquals(viewModel.events.value, activityLog)

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.DONE)
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfCannotLoadMore() {
        val canLoadMore = false
        viewModel.onActivityLogFetched(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.loadMore()

        verify(dispatcher, never()).dispatch(any())
    }

    private fun assertFetchEvents(canLoadMore: Boolean = false) {
        verify(dispatcher).dispatch(actionCaptor.capture())

        val action = actionCaptor.firstValue
        assertEquals(action.type, FETCH_ACTIVITIES)
        assertTrue(action.payload is FetchActivityLogPayload)
        (action.payload as? FetchActivityLogPayload)?.apply {
            assertEquals(this.loadMore, canLoadMore)
            assertEquals(this.site, site)
        }
    }
}
