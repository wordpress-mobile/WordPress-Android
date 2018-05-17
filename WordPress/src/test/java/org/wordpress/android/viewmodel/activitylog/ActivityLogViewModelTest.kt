package org.wordpress.android.viewmodel.activitylog

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
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
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.models.networkresource.ListState
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    private val actionCaptor = argumentCaptor<Action<Any>>()

    private var events: MutableList<ListState<ActivityLogListItemViewModel>?> = mutableListOf()
    private lateinit var activityLogList: List<ActivityLogModel>
    private lateinit var viewModel: ActivityLogViewModel

    @Before
    fun setUp() {
        viewModel = ActivityLogViewModel(dispatcher, store)
        viewModel.site = site
        viewModel.events.observeForever { events.add(it) }

        activityLogList = initializeActivityList()
        whenever(store.getActivityLogForSite(site, false)).thenReturn(activityLogList.toList())
    }

    @Test
    fun onStartEmitsDataFromStoreAndStartsFetching() {
        assertTrue(viewModel.eventListState.data.isEmpty())

        viewModel.start(site)

        assertEquals(
                viewModel.eventListState.data,
                activityLogList.map { ActivityLogListItemViewModel.fromDomainModel(it) }
        )
        assertTrue(viewModel.eventListState.isFetchingFirstPage())

        assertFetchEvents()
    }

    @Test
    fun fetchesEventsOnPullToRefresh() {
        viewModel.start(site)

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

    @Test
    fun onDataFetchedPostsDataAndChangesStateThatItCanLoadMore() {
        val canLoadMore = true
        viewModel.onActivityLogFetched(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        assertEquals(
                viewModel.eventListState.data,
                activityLogList.map { ActivityLogListItemViewModel.fromDomainModel(it) }
        )

        assertTrue(viewModel.eventListState.shouldFetch(canLoadMore))
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

        assertEquals(
                viewModel.eventListState.data,
                activityLogList.map { ActivityLogListItemViewModel.fromDomainModel(it) }
        )

        assertTrue(viewModel.eventListState is ListState.Success<*> &&
                viewModel.eventListState.shouldFetch(true) == canLoadMore)
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfCannotLoadMore() {
        val canLoadMore = false
        viewModel.onActivityLogFetched(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.loadMore()

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfNoRowsAffected() {
        val canLoadMore = true
        viewModel.onActivityLogFetched(OnActivityLogFetched(0, canLoadMore, FETCH_ACTIVITIES))

        verify(store, never()).getActivityLogForSite(site, false)
    }

    @Test
    fun headerIsDisplayedForFirstItemOrWhenDifferentThenPrevious() {
        val canLoadMore = true
        viewModel.onActivityLogFetched(OnActivityLogFetched(3, canLoadMore, FETCH_ACTIVITIES))

        assertTrue(events.last()?.data?.get(0)?.isHeaderVisible(null) == true)
        assertTrue(events.last()?.data?.get(1)?.isHeaderVisible(events.last()?.data?.get(0)) == false)
        assertTrue(events.last()?.data?.get(2)?.isHeaderVisible(events.last()?.data?.get(1)) == true)
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

    private fun initializeActivityList(): List<ActivityLogModel> {
        val birthday = Calendar.getInstance()
        birthday.set(1985, 8, 27)

        val list = mutableListOf<ActivityLogModel>()
        val activity = ActivityLogModel("", "", "", "", "", "",
                "", true, "", birthday.time)
        list.add(activity)
        list.add(activity.copy())

        birthday.set(1987, 5, 26)
        list.add(activity.copy(published = birthday.time))

        return list
    }
}
