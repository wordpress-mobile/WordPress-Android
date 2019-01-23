package org.wordpress.android.viewmodel.activitylog

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITIES
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.RewindStatusService
import org.wordpress.android.ui.activitylog.RewindStatusService.RewindProgress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Footer
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Header
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Loading
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus
import java.util.Calendar
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var store: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var rewindStatusService: RewindStatusService
    @Mock private lateinit var resourceProvider: ResourceProvider
    private lateinit var fetchActivityLogCaptor: KArgumentCaptor<FetchActivityLogPayload>

    private var events: MutableList<List<ActivityLogListItem>?> = mutableListOf()
    private var itemDetails: MutableList<ActivityLogListItem?> = mutableListOf()
    private var rewindDialogs: MutableList<ActivityLogListItem?> = mutableListOf()
    private var eventListStatuses: MutableList<ActivityLogListStatus?> = mutableListOf()
    private var snackbarMessages: MutableList<String?> = mutableListOf()
    private var moveToTopEvents: MutableList<Unit?> = mutableListOf()
    private lateinit var activityLogList: List<ActivityLogModel>
    private lateinit var viewModel: ActivityLogViewModel
    private var rewindProgress = MutableLiveData<RewindProgress>()
    private var rewindAvailable = MutableLiveData<Boolean>()

    private val rewindStatusModel = RewindStatusModel(
            ACTIVE,
            null,
            Date(),
            true,
            null,
            null)

    val event = ActivityLogListItem.Event(
            "activityId",
            "",
            ",",
            null,
            null,
            true,
            null,
            Date(),
            true
    )
    val activity = ActivityLogModel(
            "activityId",
            "",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            Date(),
            null
            )

    @Before
    fun setUp() = runBlocking<Unit> {
        viewModel = ActivityLogViewModel(store, rewindStatusService, resourceProvider, Dispatchers.Unconfined)
        viewModel.site = site
        viewModel.events.observeForever { events.add(it) }
        viewModel.eventListStatus.observeForever { eventListStatuses.add(it) }
        viewModel.showItemDetail.observeForever { itemDetails.add(it) }
        viewModel.showRewindDialog.observeForever { rewindDialogs.add(it) }
        viewModel.showSnackbarMessage.observeForever { snackbarMessages.add(it) }
        viewModel.moveToTop.observeForever { moveToTopEvents.add(it) }
        fetchActivityLogCaptor = argumentCaptor()

        activityLogList = initializeActivityList()
        whenever(store.getActivityLogForSite(site, false)).thenReturn(activityLogList.toList())
        whenever(store.getRewindStatusForSite(site)).thenReturn(rewindStatusModel)
        whenever(rewindStatusService.rewindProgress).thenReturn(rewindProgress)
        whenever(rewindStatusService.rewindAvailable).thenReturn(rewindAvailable)
        whenever(store.fetchActivities(any())).thenReturn(mock())
    }

    @Test
    fun onStartEmitsDataFromStoreAndStartsFetching() = runBlocking {
        assertNull(viewModel.events.value)
        assertTrue(eventListStatuses.isEmpty())

        viewModel.start(site)

        assertEquals(
                viewModel.events.value,
                expectedActivityList()
        )
        assertEquals(eventListStatuses[0], ActivityLogListStatus.FETCHING)
        assertEquals(eventListStatuses[1], ActivityLogListStatus.DONE)

        assertFetchEvents()
        verify(rewindStatusService).start(site)
    }

    @Test
    fun fetchesEventsOnPullToRefresh() = runBlocking {
        viewModel.onPullToRefresh()

        assertFetchEvents()
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCanLoadMore() = runBlocking {
        val canLoadMore = true
        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        assertEquals(
                viewModel.events.value,
                expectedActivityList(false, canLoadMore)
        )

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.CAN_LOAD_MORE)
    }

    @Test
    fun onDataFetchedLoadsMoreDataIfCanLoadMore() = runBlocking {
        val canLoadMore = true
        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        reset(store)

        viewModel.onScrolledToBottom()

        assertFetchEvents(canLoadMore)
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCannotLoadMore() = runBlocking {
        val canLoadMore = false
        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        assertEquals(
                viewModel.events.value,
                expectedActivityList()
        )

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.DONE)
    }

    @Test
    fun onDataFetchedShowsFooterIfCannotLoadMoreAndIsFreeSite() = runBlocking {
        val canLoadMore = false
        whenever(site.hasFreePlan).thenReturn(true)
        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        assertEquals(
                viewModel.events.value,
                expectedActivityList(true)
        )

        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.DONE)
    }

    private fun expectedActivityList(isLastPageAndFreeSite: Boolean = false, canLoadMore: Boolean = false):
            List<ActivityLogListItem> {
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
        if (canLoadMore) {
            activityLogListItems.add(Loading)
        }
        return activityLogListItems
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfCannotLoadMore() = runBlocking<Unit> {
        val canLoadMore = false
        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        reset(store)

        viewModel.onScrolledToBottom()

        verify(store, never()).fetchActivities(any())
    }

    @Test
    fun onDataFetchedGoesToTopWhenSomeRowsAffected() = runBlocking {
        assertTrue(moveToTopEvents.isEmpty())

        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(10, true, FETCH_ACTIVITIES))

        viewModel.start(site)

        assertTrue(moveToTopEvents.isNotEmpty())
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfNoRowsAffected() = runBlocking<Unit> {
        val canLoadMore = true

        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(0, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        verify(store).getActivityLogForSite(site, false)
    }

    @Test
    fun headerIsDisplayedForFirstItemOrWhenDifferentThenPrevious() = runBlocking {
        val canLoadMore = true
        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(3, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        assertTrue(events.last()?.get(0) is Header)
        assertTrue(events.last()?.get(3) is Header)
    }

    @Test
    fun onItemClickShowsItemDetail() {
        assertTrue(itemDetails.isEmpty())

        viewModel.onItemClicked(event)

        assertEquals(itemDetails.firstOrNull(), event)
    }

    @Test
    fun onActionButtonClickShowsRewindDialog() {
        assertTrue(rewindDialogs.isEmpty())

        viewModel.onActionButtonClicked(event)

        assertEquals(rewindDialogs.firstOrNull(), event)
    }

    @Test
    fun onRewindConfirmedTriggersRewindOperation() {
        viewModel.start(site)
        val rewindId = "rewindId"

        viewModel.onRewindConfirmed(rewindId)

        verify(rewindStatusService).rewind(rewindId, site)
    }

    @Test
    fun onRewindConfirmedShowsRewindStartedMessage() {
        assertTrue(snackbarMessages.isEmpty())
        whenever(rewindStatusService.rewindingActivity).thenReturn(activity)
        val snackBarMessage = "snackBar message"
        whenever(resourceProvider.getString(any(), any(), any())).thenReturn(snackBarMessage)

        viewModel.onRewindConfirmed("rewindId")

        assertEquals(snackbarMessages.firstOrNull(), snackBarMessage)
    }

    @Test
    fun loadsNextPageOnScrollToBottom() = runBlocking {
        whenever(store.fetchActivities(any())).thenReturn(OnActivityLogFetched(10, true, FETCH_ACTIVITIES))

        viewModel.start(site)
        reset(store)

        viewModel.onScrolledToBottom()

        assertFetchEvents(true)
    }

    private suspend fun assertFetchEvents(canLoadMore: Boolean = false) {
        verify(store).fetchActivities(fetchActivityLogCaptor.capture())

        fetchActivityLogCaptor.lastValue.apply {
            assertEquals(this.loadMore, canLoadMore)
            assertEquals(this.site, site)
        }
    }

    private fun initializeActivityList(): List<ActivityLogModel> {
        val birthday = Calendar.getInstance()
        birthday.set(1985, 8, 27)

        val list = mutableListOf<ActivityLogModel>()
        val activity = ActivityLogModel("", "", null, "", "", "",
                "", true, "", birthday.time)
        list.add(activity)
        list.add(activity.copy())

        birthday.set(1987, 5, 26)
        list.add(activity.copy(published = birthday.time))

        return list
    }
}
