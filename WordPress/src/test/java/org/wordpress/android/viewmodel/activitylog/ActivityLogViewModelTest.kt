package org.wordpress.android.viewmodel.activitylog

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
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
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCHED_ACTIVITIES
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITIES
import org.wordpress.android.fluxc.annotations.action.Action
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
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus
import java.util.Calendar
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var store: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var rewindStatusService: RewindStatusService
    @Mock private lateinit var resourceProvider: ResourceProvider
    private val actionCaptor = argumentCaptor<Action<Any>>()

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
            "",
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
    fun setUp() {
        viewModel = ActivityLogViewModel(dispatcher, store, rewindStatusService, resourceProvider)
        viewModel.site = site
        viewModel.events.observeForever { events.add(it) }
        viewModel.eventListStatus.observeForever { eventListStatuses.add(it) }
        viewModel.showItemDetail.observeForever { itemDetails.add(it) }
        viewModel.showRewindDialog.observeForever { rewindDialogs.add(it) }
        viewModel.showSnackbarMessage.observeForever { snackbarMessages.add(it) }
        viewModel.moveToTop.observeForever { moveToTopEvents.add(it) }

        activityLogList = initializeActivityList()
        whenever(store.getActivityLogForSite(site, false)).thenReturn(activityLogList.toList())
        whenever(store.getRewindStatusForSite(site)).thenReturn(rewindStatusModel)
        whenever(rewindStatusService.rewindProgress).thenReturn(rewindProgress)
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

    private fun expectedActivityList(): List<Event> {
        return activityLogList.mapIndexed { index, activityLogModel ->
            Event(activityLogModel, true).copy(isHeaderVisible = index != 1)
        }
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfCannotLoadMore() {
        val canLoadMore = false
        viewModel.onEventsUpdated(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.onScrolledToBottom()

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun onDataFetchedGoesToTopWhenSomeRowsAffected() {
        assertTrue(moveToTopEvents.isEmpty())

        viewModel.onEventsUpdated(OnActivityLogFetched(10, true, FETCH_ACTIVITIES))

        assertTrue(moveToTopEvents.isNotEmpty())
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

        assertTrue(events.last()?.get(0)?.isHeaderVisible == true)
        assertTrue(events.last()?.get(1)?.isHeaderVisible == false)
        assertTrue(events.last()?.get(2)?.isHeaderVisible == true)
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
    fun loadsNextPageOnScrollToBottom() {
        viewModel.onEventsUpdated(OnActivityLogFetched(10, true, FETCHED_ACTIVITIES))

        viewModel.onScrolledToBottom()

        assertFetchEvents(true)
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
