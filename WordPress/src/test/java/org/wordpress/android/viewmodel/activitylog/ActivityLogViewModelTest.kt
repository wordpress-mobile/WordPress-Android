package org.wordpress.android.viewmodel.activitylog

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
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
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.RewindStatusService
import org.wordpress.android.ui.activitylog.RewindStatusService.RewindProgress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Footer
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Header
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus
import java.util.Calendar

@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var rewindStatusService: RewindStatusService
    @Mock private lateinit var resourceProvider: ResourceProvider
    private lateinit var actionCaptor: KArgumentCaptor<Action<Any>>

    private var events: MutableList<List<ActivityLogListItem>?> = mutableListOf()
    private var eventListStatuses: MutableList<ActivityLogListStatus?> = mutableListOf()
    private lateinit var activityLogList: List<ActivityLogModel>
    private lateinit var viewModel: ActivityLogViewModel
    private var rewindState = MutableLiveData<RewindProgress>()
    private var rewindAvailable = MutableLiveData<Boolean>()

    @Before
    fun setUp() {
        viewModel = ActivityLogViewModel(dispatcher, store, rewindStatusService, resourceProvider)
        viewModel.site = site
        viewModel.events.observeForever { events.add(it) }
        viewModel.eventListStatus.observeForever { eventListStatuses.add(it) }
        actionCaptor = argumentCaptor()

        activityLogList = initializeActivityList()
        whenever(store.getActivityLogForSite(site, false)).thenReturn(activityLogList.toList())
        whenever(rewindStatusService.rewindProgress).thenReturn(rewindState)
        whenever(rewindStatusService.rewindAvailable).thenReturn(rewindAvailable)
    }

    @Test
    fun onStartEmitsDataFromStoreAndStartsFetching() {
        assertNull(viewModel.events.value)
        assertNull(viewModel.eventListStatus.value)

        viewModel.start(site)

        assertEquals(
                viewModel.events.value,
                expectedActivityList()
        )
        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.FETCHING)

        assertFetchEvents()
        verify(rewindStatusService).start(site)
    }

    @Test
    fun fetchesEventsOnPullToRefresh() {
        viewModel.onPullToRefresh()

        assertFetchEvents()
    }

    @Test
    fun doesNotFetchEventsWhenAlreadyFetching() {
        viewModel.onPullToRefresh()

        reset(dispatcher)

        viewModel.onPullToRefresh()

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCanLoadMore() {
        val canLoadMore = true
        viewModel.onEventsUpdated(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        assertEquals(
                viewModel.events.value,
                expectedActivityList()
        )

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.CAN_LOAD_MORE)
    }

    @Test
    fun onDataFetchedLoadsMoreDataIfCanLoadMore() {
        val canLoadMore = true
        viewModel.onEventsUpdated(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.onScrolledToBottom()

        assertFetchEvents(canLoadMore)
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCannotLoadMore() {
        val canLoadMore = false
        viewModel.onEventsUpdated(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        assertEquals(
                viewModel.events.value,
                expectedActivityList()
        )

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.DONE)
    }

    @Test
    fun onDataFetchedShowsFooterIfCannotLoadMoreAndIsFreeSite() {
        val canLoadMore = false
        whenever(site.hasFreePlan).thenReturn(true)
        viewModel.onEventsUpdated(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        assertEquals(
                viewModel.events.value,
                expectedActivityList(true)
        )

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.DONE)
    }

    private fun expectedActivityList(isLastPageAndFreeSite: Boolean = false): List<ActivityLogListItem> {
        val activityLogListItems = mutableListOf<ActivityLogListItem>()
        val first = Event(activityLogList[0], true)
        val second = Event(activityLogList[1], true)
        val third = Event(activityLogList[2], true)
        activityLogListItems.add(Header(first.formattedDate))
        activityLogListItems.add(first)
        activityLogListItems.add(second)
        activityLogListItems.add(Header(third.formattedDate))
        activityLogListItems.add(third)
        if (isLastPageAndFreeSite) {
            activityLogListItems.add(Footer)
        }
        return activityLogListItems
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfCannotLoadMore() {
        val canLoadMore = false
        viewModel.onEventsUpdated(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.onScrolledToBottom()

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfNoRowsAffected() {
        val canLoadMore = true
        viewModel.onEventsUpdated(OnActivityLogFetched(0, canLoadMore, FETCH_ACTIVITIES))

        verify(store, never()).getActivityLogForSite(site, false)
    }

    @Test
    fun headerIsDisplayedForFirstItemOrWhenDifferentThenPrevious() {
        val canLoadMore = true
        viewModel.onEventsUpdated(OnActivityLogFetched(3, canLoadMore, FETCH_ACTIVITIES))

        assertTrue(events.last()?.get(0) is Header)
        assertTrue(events.last()?.get(3) is Header)
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
