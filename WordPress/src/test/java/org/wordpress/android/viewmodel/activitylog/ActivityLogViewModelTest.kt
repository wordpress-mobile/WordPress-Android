package org.wordpress.android.viewmodel.activitylog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.util.Pair
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.action.ActivityLogAction.FETCH_ACTIVITIES
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.State.ACTIVE
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.ShowBackupDownload
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.ShowRestore
import org.wordpress.android.ui.jetpack.rewind.RewindStatusService
import org.wordpress.android.ui.jetpack.rewind.RewindStatusService.RewindProgress
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Event
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Footer
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Header
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Icon.DEFAULT
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Loading
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.SecondaryAction.DOWNLOAD_BACKUP
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.SecondaryAction.RESTORE
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BackupFeatureConfig
import org.wordpress.android.util.config.ActivityLogFiltersFeatureConfig
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersShown
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ShowDateRangePicker
import java.util.Calendar
import java.util.Date

private const val DATE_1_IN_MILLIS = 1578614400000L // 2020-01-10T00:00:00+00:00
private const val DATE_2_IN_MILLIS = 1578787200000L // 2020-01-12T00:00:00+00:00

private const val TIMEZONE_UTC = "UTC"
private const val ONE_DAY_WITHOUT_SECOND_IN_MILLIS = 1000 * 60 * 60 * 24 - 1000

@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()
    @Mock private lateinit var store: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var rewindStatusService: RewindStatusService
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var activityLogFiltersFeatureConfig: ActivityLogFiltersFeatureConfig
    @Mock private lateinit var backupFeatureConfig: BackupFeatureConfig
    @Mock private lateinit var dateUtils: DateUtils
    private lateinit var fetchActivityLogCaptor: KArgumentCaptor<FetchActivityLogPayload>
    private lateinit var formatDateRangeTimezoneCaptor: KArgumentCaptor<String>

    private var events: MutableList<List<ActivityLogListItem>?> = mutableListOf()
    private var itemDetails: MutableList<ActivityLogListItem?> = mutableListOf()
    private var rewindDialogs: MutableList<ActivityLogListItem?> = mutableListOf()
    private var eventListStatuses: MutableList<ActivityLogListStatus?> = mutableListOf()
    private var snackbarMessages: MutableList<String?> = mutableListOf()
    private var moveToTopEvents: MutableList<Unit?> = mutableListOf()
    private var navigationEvents:
            MutableList<org.wordpress.android.viewmodel.Event<ActivityLogNavigationEvents?>> = mutableListOf()
    private var showDateRangePickerEvents: MutableList<ShowDateRangePicker> = mutableListOf()
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
            null
    )

    val event = ActivityLogListItem.Event(
            "activityId",
            "",
            ",",
            null,
            null,
            true,
            null,
            Date(),
            true,
            DEFAULT,
            false
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
        viewModel = ActivityLogViewModel(
                store,
                rewindStatusService,
                resourceProvider,
                activityLogFiltersFeatureConfig,
                backupFeatureConfig,
                dateUtils,
                Dispatchers.Unconfined
        )
        viewModel.site = site
        viewModel.events.observeForever { events.add(it) }
        viewModel.eventListStatus.observeForever { eventListStatuses.add(it) }
        viewModel.showItemDetail.observeForever { itemDetails.add(it) }
        viewModel.showRewindDialog.observeForever { rewindDialogs.add(it) }
        viewModel.showSnackbarMessage.observeForever { snackbarMessages.add(it) }
        viewModel.moveToTop.observeForever { moveToTopEvents.add(it) }
        viewModel.navigationEvents.observeForever { navigationEvents.add(it) }
        viewModel.showDateRangePicker.observeForever { showDateRangePickerEvents.add(it) }
        fetchActivityLogCaptor = argumentCaptor()
        formatDateRangeTimezoneCaptor = argumentCaptor()

        activityLogList = initializeActivityList()
        whenever(store.getActivityLogForSite(site, false)).thenReturn(activityLogList.toList())
        whenever(store.getRewindStatusForSite(site)).thenReturn(rewindStatusModel)
        whenever(rewindStatusService.rewindProgress).thenReturn(rewindProgress)
        whenever(rewindStatusService.rewindAvailable).thenReturn(rewindAvailable)
        whenever(store.fetchActivities(anyOrNull())).thenReturn(mock())
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
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

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
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        reset(store)
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.onScrolledToBottom()

        assertFetchEvents(canLoadMore)
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCannotLoadMore() = runBlocking {
        val canLoadMore = false
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

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
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

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
        val first = Event(activityLogList[0], true, false)
        val second = Event(activityLogList[1], true, false)
        val third = Event(activityLogList[2], true, false)
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
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(1, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        reset(store)

        viewModel.onScrolledToBottom()

        verify(store, never()).fetchActivities(anyOrNull())
    }

    @Test
    fun onDataFetchedGoesToTopWhenSomeRowsAffected() = runBlocking {
        assertTrue(moveToTopEvents.isEmpty())

        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(10, true, FETCH_ACTIVITIES))

        viewModel.start(site)

        assertTrue(moveToTopEvents.isNotEmpty())
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfNoRowsAffected() = runBlocking<Unit> {
        val canLoadMore = true

        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(0, canLoadMore, FETCH_ACTIVITIES))

        viewModel.start(site)

        verify(store).getActivityLogForSite(site, false)
    }

    @Test
    fun headerIsDisplayedForFirstItemOrWhenDifferentThenPrevious() = runBlocking {
        val canLoadMore = true
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(3, canLoadMore, FETCH_ACTIVITIES))

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
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(10, true, FETCH_ACTIVITIES))

        viewModel.start(site)
        reset(store)
        whenever(store.fetchActivities(anyOrNull())).thenReturn(OnActivityLogFetched(10, true, FETCH_ACTIVITIES))

        viewModel.onScrolledToBottom()

        assertFetchEvents(true)
    }

    @Test
    fun filtersAreNotVisibleWhenFiltersFeatureFlagIsDisabled() = runBlocking {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.start(site)

        assertEquals(false, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun filtersAreVisibleWhenFiltersFeatureFlagIsEnabled() = runBlocking {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.start(site)

        assertEquals(true, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun onActivityTypeFilterClickShowsActivityTypeFilter() {
        viewModel.onActivityTypeFilterClicked()

        assertNotNull(viewModel.showActivityTypeFilterDialog.value)
    }

    @Test
    fun onActivityTypeFilterClickRemoteSiteIdIsPassed() {
        viewModel.onActivityTypeFilterClicked()

        assertEquals(RemoteId(site.siteId), viewModel.showActivityTypeFilterDialog.value!!.siteId)
    }

    @Test
    fun onActivityTypeFilterClickPreviouslySelectedTypesPassed() {
        val selectedItems = listOf(
                Pair("backup", UiStringText("Backups and Restores") as UiString),
                Pair("user", UiStringText("Users") as UiString)
        )
        viewModel.onActivityTypesSelected(selectedItems)

        viewModel.onActivityTypeFilterClicked()

        assertEquals(selectedItems.map { it.first }, viewModel.showActivityTypeFilterDialog.value!!.initialSelection)
    }

    @Test
    fun onSecondaryActionClickRestoreNavigationEventIsShowRestore() {
        viewModel.onSecondaryActionClicked(RESTORE, event)

        Assertions.assertThat(navigationEvents.last().peekContent()).isInstanceOf(ShowRestore::class.java)
    }

    @Test
    fun onSecondaryActionClickDownloadBackupNavigationEventIsShowBackupDownload() {
        viewModel.onSecondaryActionClicked(DOWNLOAD_BACKUP, event)

        Assertions.assertThat(navigationEvents.last().peekContent()).isInstanceOf(ShowBackupDownload::class.java)
    }

    @Test
    fun dateRangeFilterClearActionShownWhenFilterNotEmpty() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")
        val dateRange = Pair(10L, 20L)

        viewModel.onDateRangeSelected(dateRange)

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked
        Assertions.assertThat(action != null).isTrue
    }

    @Test
    fun dateRangeFilterClearActionHiddenWhenFilterEmpty() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        viewModel.onDateRangeSelected(null)

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked
        Assertions.assertThat(action == null).isTrue
    }

    @Test
    fun onDateRangeFilterClearActionClickClearActionDisappears() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")
        viewModel.onDateRangeSelected(Pair(10L, 20L))

        (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked!!.invoke()

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked
        Assertions.assertThat(action == null).isTrue
    }

    @Test
    fun basicDateRangeLabelShownWhenFilterEmpty() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.onDateRangeSelected(null)

        Assertions.assertThat((viewModel.filtersUiState.value as FiltersShown).dateRangeLabel)
                .isEqualTo(UiStringRes(R.string.activity_log_date_range_filter_label))
    }

    @Test
    fun dateRangeLabelWithDatesShownWhenFilterNotEmpty() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(10L, 20L))

        Assertions.assertThat((viewModel.filtersUiState.value as FiltersShown).dateRangeLabel)
                .isEqualTo(UiStringText("TEST"))
    }

    @Test
    fun dateRangeLabelFormattingUsesUTCTimezone() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(
                dateUtils.formatDateRange(
                        anyOrNull(),
                        anyOrNull(),
                        formatDateRangeTimezoneCaptor.capture()
                )
        ).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(10L, 20L))

        Assertions.assertThat(formatDateRangeTimezoneCaptor.firstValue)
                .isEqualTo(TIMEZONE_UTC)
    }

    @Test
    fun dateRangeEndTimestampGetsAdjustedToEndOfDay() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(
                dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())
        ).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(DATE_1_IN_MILLIS, DATE_2_IN_MILLIS))
        viewModel.dateRangePickerClicked()

        Assertions.assertThat(showDateRangePickerEvents[0].initialSelection).isEqualTo(
                Pair(DATE_1_IN_MILLIS, DATE_2_IN_MILLIS + ONE_DAY_WITHOUT_SECOND_IN_MILLIS)
        )
    }



    @Test
    fun activityTypeFilterClearActionShownWhenFilterNotEmpty() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        viewModel.onActivityTypesSelected(
                listOf(Pair("backup", UiStringText("Backups and Restores") as UiString))
        )

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked
        Assertions.assertThat(action != null).isTrue
    }

    @Test
    fun activityTypeFilterClearActionHiddenWhenFilterEmpty() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        viewModel.onActivityTypesSelected(listOf())

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked
        Assertions.assertThat(action == null).isTrue
    }

    @Test
    fun onActivityTypeFilterClearActionClickClearActionDisappears() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        viewModel.onActivityTypesSelected(
                listOf(Pair("backup", UiStringText("Backups and Restores") as UiString))
        )

        (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked!!.invoke()

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked
        Assertions.assertThat(action == null).isTrue
    }

    @Test
    fun basicActivityTypeLabelShownWhenFilterEmpty() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        viewModel.onActivityTypesSelected(listOf())

        Assertions.assertThat((viewModel.filtersUiState.value as FiltersShown).activityTypeLabel)
                .isEqualTo(UiStringRes(R.string.activity_log_activity_type_filter_label))
    }

    @Test
    fun activityTypeLabelWithNameShownWhenFilterHasOneItem() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        val activityTypeName = "Backups and Restores"
        viewModel.onActivityTypesSelected(
                listOf(Pair("backup", UiStringText(activityTypeName) as UiString))
        )

        Assertions.assertThat((viewModel.filtersUiState.value as FiltersShown).activityTypeLabel)
                .isEqualTo(UiStringText(activityTypeName))
    }

    @Test
    fun activityTypeLabelWithCountShownWhenFilterHasMoreThanOneItem() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        viewModel.onActivityTypesSelected(
                listOf(
                        Pair("backup", UiStringText("Backups and Restores") as UiString),
                        Pair("user", UiStringText("Users") as UiString)
                )
        )

        Assertions.assertThat((viewModel.filtersUiState.value as FiltersShown).activityTypeLabel)
                .isEqualTo(
                        UiStringResWithParams(
                                R.string.activity_log_activity_type_filter_active_label,
                                listOf(UiStringText("2"))
                        )
                )
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
        val activity = ActivityLogModel(
                "", "", null, "", "", "",
                "", true, "", birthday.time
        )
        list.add(activity)
        list.add(activity.copy())

        birthday.set(1987, 5, 26)
        list.add(activity.copy(published = birthday.time))

        return list
    }
}
