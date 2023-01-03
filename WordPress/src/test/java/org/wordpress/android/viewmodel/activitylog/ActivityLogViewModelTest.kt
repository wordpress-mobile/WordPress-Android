package org.wordpress.android.viewmodel.activitylog

import androidx.core.util.Pair
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.DownloadBackupFile
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Notice
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Progress.Type.BACKUP_DOWNLOAD
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Progress.Type.RESTORE
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostDismissBackupDownloadUseCase
import org.wordpress.android.ui.jetpack.common.JetpackBackupDownloadActionState
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import org.wordpress.android.ui.stats.refresh.utils.StatsDateUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.ActivityLogTracker
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.BackupDownloadEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.EmptyUiState
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersShown
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.RestoreEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ShowDateRangePicker
import java.util.Calendar
import java.util.Date

private const val DATE_1_IN_MILLIS = 1578614400000L // 2020-01-10T00:00:00+00:00
private const val DATE_2_IN_MILLIS = 1578787200000L // 2020-01-12T00:00:00+00:00

private const val TIMEZONE_GMT_0 = "GMT+0"
private const val ONE_DAY_WITHOUT_SECOND_IN_MILLIS = 1000 * 60 * 60 * 24 - 1000

private const val SITE_ID = 1L

private const val NOW = "Now"
private const val RESTORE_STARTED = "Your site is being restored\nRestoring to date time"
private const val RESTORING_CURRENTLY = "Currently restoring your site"
private const val RESTORING_DATE_TIME = "Restoring to date time"
private const val RESTORING_NO_DATE = "Restore in progress"
private const val RESTORED_DATE_TIME = "Your site has been successfully restored\nRestored to date time"
private const val RESTORED_NO_DATE = "Your site has been successfully restored"

private const val BACKUP_STARTED = "Your site is being backed up\nBacking up from date time"
private const val BACKING_UP_CURRENTLY = "Creating downloadable backup"
private const val BACKING_UP_DATE_TIME = "Backing up site from date time"
private const val BACKING_UP_NO_DATE = "Backing up site"
private const val BACKED_UP_DATE_TIME = "Your site has been successfully backed up\nBacked up from date time"
private const val BACKED_UP_NO_DATE = "Your site has been successfully backed up"
private const val BACKUP_NOTICE = "We successfully created a backup of your site from date time"

private const val REWIND_ID = "rewindId"
private const val RESTORE_ID = 123456789L
private const val DOWNLOAD_URL = "downloadUrl"
private const val DOWNLOAD_IS_VALID = true
private const val DOWNLOAD_ID = 987654321L
private val DOWNLOAD_PUBLISHED = Date()
private val DOWNLOAD_VALID_UNTIL = Date()

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var store: ActivityLogStore

    @Mock
    private lateinit var site: SiteModel

    @Mock
    private lateinit var getRestoreStatusUseCase: GetRestoreStatusUseCase

    @Mock
    private lateinit var getBackupDownloadStatusUseCase: GetBackupDownloadStatusUseCase

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var mStatsDateUtils: StatsDateUtils

    @Mock
    private lateinit var activityLogTracker: ActivityLogTracker

    @Mock
    private lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase

    @Mock
    private lateinit var postDismissBackupDownloadUseCase: PostDismissBackupDownloadUseCase

    private lateinit var fetchActivityLogCaptor: KArgumentCaptor<FetchActivityLogPayload>
    private lateinit var formatDateRangeTimezoneCaptor: KArgumentCaptor<String>
    private lateinit var viewModel: ActivityLogViewModel

    private var events: MutableList<List<ActivityLogListItem>?> = mutableListOf()
    private var itemDetails: MutableList<ActivityLogListItem?> = mutableListOf()
    private var eventListStatuses: MutableList<ActivityLogListStatus?> = mutableListOf()
    private var snackbarMessages: MutableList<String?> = mutableListOf()
    private var moveToTopEvents: MutableList<Unit?> = mutableListOf()
    private var navigationEvents: MutableList<Event<ActivityLogNavigationEvents?>> = mutableListOf()
    private var showDateRangePickerEvents: MutableList<ShowDateRangePicker> = mutableListOf()

    private val activityList = listOf(firstActivity(), secondActivity(), thirdActivity())
    private val rewindableOnly = false

    @Before
    fun setUp() = test {
        viewModel = ActivityLogViewModel(
            store,
            getRestoreStatusUseCase,
            getBackupDownloadStatusUseCase,
            postDismissBackupDownloadUseCase,
            resourceProvider,
            mStatsDateUtils,
            activityLogTracker,
            jetpackCapabilitiesUseCase
        )
        viewModel.site = site
        viewModel.rewindableOnly = rewindableOnly

        viewModel.events.observeForever { events.add(it) }
        viewModel.eventListStatus.observeForever { eventListStatuses.add(it) }
        viewModel.showItemDetail.observeForever { itemDetails.add(it) }
        viewModel.showSnackbarMessage.observeForever { snackbarMessages.add(it) }
        viewModel.moveToTop.observeForever { moveToTopEvents.add(it) }
        viewModel.navigationEvents.observeForever { navigationEvents.add(it) }
        viewModel.showDateRangePicker.observeForever { showDateRangePickerEvents.add(it) }

        fetchActivityLogCaptor = argumentCaptor()
        formatDateRangeTimezoneCaptor = argumentCaptor()

        whenever(store.getActivityLogForSite(site, false, rewindableOnly)).thenReturn(activityList.toList())
        whenever(store.fetchActivities(anyOrNull())).thenReturn(mock())
        whenever(site.hasFreePlan).thenReturn(false)
        whenever(site.siteId).thenReturn(SITE_ID)
        whenever(jetpackCapabilitiesUseCase.getCachedJetpackPurchasedProducts(anyLong()))
            .thenReturn(JetpackPurchasedProducts(scan = false, backup = false))
    }

    @Test
    fun onStartEmitsDataFromStoreAndStartsFetching() = test {
        assertNull(viewModel.events.value)
        assertTrue(eventListStatuses.isEmpty())

        viewModel.start(site, rewindableOnly)

        assertEquals(viewModel.events.value, expectedActivityList(rewindDisabled = false))
        assertEquals(eventListStatuses[0], ActivityLogListStatus.FETCHING)
        assertEquals(eventListStatuses[1], ActivityLogListStatus.DONE)
        assertFetchEvents()
    }

    @Test
    fun fetchesEventsOnPullToRefresh() = test {
        viewModel.onPullToRefresh()

        assertFetchEvents()
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCanLoadMore() = test {
        val canLoadMore = true
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(1, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.start(site, rewindableOnly)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = canLoadMore
            )
        )
        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.CAN_LOAD_MORE)
    }

    @Test
    fun onDataFetchedLoadsMoreDataIfCanLoadMore() = test {
        val canLoadMore = true
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(1, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))
        viewModel.start(site, rewindableOnly)
        reset(store)
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(1, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.onScrolledToBottom()

        assertFetchEvents(canLoadMore)
    }

    @Test
    fun onDataFetchedPostsDataAndChangesStatusIfCannotLoadMore() = test {
        val canLoadMore = false
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(1, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.start(site, rewindableOnly)

        assertEquals(viewModel.events.value, expectedActivityList(rewindDisabled = false))
        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.DONE)
    }

    @Test
    fun onDataFetchedShowsFooterIfCannotLoadMoreAndIsFreeSite() = test {
        val canLoadMore = false
        whenever(site.hasFreePlan).thenReturn(true)
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(1, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.start(site, rewindableOnly)

        assertEquals(viewModel.events.value, expectedActivityList(rewindDisabled = false, isLastPageAndFreeSite = true))
        assertEquals(viewModel.eventListStatus.value, ActivityLogListStatus.DONE)
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfCannotLoadMore() = test {
        val canLoadMore = false
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(1, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))
        viewModel.start(site, rewindableOnly)
        reset(store)

        viewModel.onScrolledToBottom()

        verify(store, never()).fetchActivities(anyOrNull())
    }

    @Test
    fun onDataFetchedGoesToTopWhenSomeRowsAffected() = test {
        assertTrue(moveToTopEvents.isEmpty())
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(10, true, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.start(site, rewindableOnly)

        assertTrue(moveToTopEvents.isNotEmpty())
    }

    @Test
    fun onDataFetchedDoesNotLoadMoreDataIfNoRowsAffected() = test {
        val canLoadMore = true
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(0, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.start(site, rewindableOnly)

        verify(store).getActivityLogForSite(site, rewindableOnly)
    }

    @Test
    fun headerIsDisplayedForFirstItemOrWhenDifferentThenPrevious() = test {
        val canLoadMore = true
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(3, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.start(site, rewindableOnly)

        assertTrue(events.last()?.get(0) is ActivityLogListItem.Header)
        assertTrue(events.last()?.get(3) is ActivityLogListItem.Header)
    }

    @Test
    fun onItemClickShowsItemDetail() {
        val event = event()
        assertTrue(itemDetails.isEmpty())

        viewModel.onItemClicked(event)

        assertEquals(itemDetails.firstOrNull(), event)
    }

    @Test
    fun loadsNextPageOnScrollToBottom() = test {
        val canLoadMore = true
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(10, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))
        viewModel.start(site, rewindableOnly)
        reset(store)
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(10, canLoadMore, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.onScrolledToBottom()

        assertFetchEvents(canLoadMore)
    }

    @Test
    fun filtersAreVisibleWhenSiteOnPaidPlan() {
        whenever(site.hasFreePlan).thenReturn(false)

        viewModel.start(site, rewindableOnly)

        assertEquals(true, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun filtersAreNotVisibleWhenSiteOnFreePlan() {
        whenever(site.hasFreePlan).thenReturn(true)

        viewModel.start(site, rewindableOnly)

        assertEquals(false, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun filtersAreVisibleWhenSiteOnFreePlanButHasPurchasedBackupProduct() = test {
        whenever(site.hasFreePlan).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getCachedJetpackPurchasedProducts(SITE_ID))
            .thenReturn(JetpackPurchasedProducts(scan = false, backup = true))

        viewModel.start(site, rewindableOnly)

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

        assertEquals(RemoteId(SITE_ID), viewModel.showActivityTypeFilterDialog.value!!.siteId)
    }

    @Test
    fun onActivityTypeFilterClickPreviouslySelectedTypesPassed() {
        val selectedItems = listOf(
            ActivityTypeModel("user", "User", 10),
            ActivityTypeModel("backup", "Backup", 5)
        )
        viewModel.onActivityTypesSelected(selectedItems)

        viewModel.onActivityTypeFilterClicked()

        assertEquals(selectedItems.map { it.key }, viewModel.showActivityTypeFilterDialog.value!!.initialSelection)
    }

    @Test
    fun onSecondaryActionClickRestoreNavigationEventIsShowRestore() {
        viewModel.onSecondaryActionClicked(ActivityLogListItem.SecondaryAction.RESTORE, event())

        assertThat(navigationEvents.last().peekContent())
            .isInstanceOf(ActivityLogNavigationEvents.ShowRestore::class.java)
    }

    @Test
    fun onSecondaryActionClickDownloadBackupNavigationEventIsShowBackupDownload() {
        viewModel.onSecondaryActionClicked(ActivityLogListItem.SecondaryAction.DOWNLOAD_BACKUP, event())

        assertThat(navigationEvents.last().peekContent())
            .isInstanceOf(ActivityLogNavigationEvents.ShowBackupDownload::class.java)
    }

    @Test
    fun dateRangeTrackDateRangeFilterSelectedEventFired() {
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")
        val dateRange = Pair(10L, 20L)

        viewModel.onDateRangeSelected(dateRange)

        verify(activityLogTracker).trackDateRangeFilterSelected(dateRange, rewindableOnly)
    }

    @Test
    fun dateRangeFilterClearActionShownWhenFilterNotEmpty() {
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")
        val dateRange = Pair(10L, 20L)

        viewModel.onDateRangeSelected(dateRange)

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked
        assertThat(action != null).isTrue
    }

    @Test
    fun dateRangeFilterClearActionHiddenWhenFilterEmpty() {
        viewModel.onDateRangeSelected(null)

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked
        assertThat(action == null).isTrue
    }

    @Test
    fun onDateRangeFilterClearActionClickClearActionDisappears() {
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")
        viewModel.onDateRangeSelected(Pair(10L, 20L))

        (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked!!.invoke()

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearDateRangeFilterClicked
        assertThat(action == null).isTrue
    }

    @Test
    fun basicDateRangeLabelShownWhenFilterEmpty() {
        viewModel.onDateRangeSelected(null)

        assertThat((viewModel.filtersUiState.value as FiltersShown).dateRangeLabel)
            .isEqualTo(UiStringRes(R.string.activity_log_date_range_filter_label))
    }

    @Test
    fun dateRangeLabelWithDatesShownWhenFilterNotEmpty() {
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(10L, 20L))

        assertThat((viewModel.filtersUiState.value as FiltersShown).dateRangeLabel).isEqualTo(UiStringText("TEST"))
    }

    @Test
    fun dateRangeLabelFormattingUsesGMT0Timezone() {
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), formatDateRangeTimezoneCaptor.capture()))
            .thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(10L, 20L))

        assertThat(formatDateRangeTimezoneCaptor.firstValue).isEqualTo(TIMEZONE_GMT_0)
    }

    @Test
    fun dateRangeTrackDateRangeFilterButtonClickedEventFired() {
        viewModel.dateRangePickerClicked()

        verify(activityLogTracker).trackDateRangeFilterButtonClicked(rewindableOnly)
    }

    @Test
    fun dateRangeEndTimestampGetsAdjustedToEndOfDay() {
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(DATE_1_IN_MILLIS, DATE_2_IN_MILLIS))
        viewModel.dateRangePickerClicked()

        assertThat(showDateRangePickerEvents[0].initialSelection)
            .isEqualTo(Pair(DATE_1_IN_MILLIS, DATE_2_IN_MILLIS + ONE_DAY_WITHOUT_SECOND_IN_MILLIS))
    }

    @Test
    fun activityTypeFilterClearActionShownWhenFilterNotEmpty() {
        viewModel.onActivityTypesSelected(listOf(ActivityTypeModel("user", "User", 10)))

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked
        assertThat(action != null).isTrue
    }

    @Test
    fun activityTypeFilterClearActionHiddenWhenFilterEmpty() {
        viewModel.onActivityTypesSelected(listOf())

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked
        assertThat(action == null).isTrue
    }

    @Test
    fun onActivityTypeFilterClearActionClickClearActionDisappears() {
        viewModel.onActivityTypesSelected(listOf(ActivityTypeModel("user", "User", 10)))

        (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked!!.invoke()

        val action = (viewModel.filtersUiState.value as FiltersShown).onClearActivityTypeFilterClicked
        assertThat(action == null).isTrue
    }

    @Test
    fun basicActivityTypeLabelShownWhenFilterEmpty() {
        viewModel.onActivityTypesSelected(listOf())

        assertThat((viewModel.filtersUiState.value as FiltersShown).activityTypeLabel)
            .isEqualTo(UiStringRes(R.string.activity_log_activity_type_filter_label))
    }

    @Test
    fun activityTypeLabelWithNameShownWhenFilterHasOneItem() {
        val activityTypeName = "Backups and Restores"
        val activityTypeCount = 5
        viewModel.onActivityTypesSelected(listOf(ActivityTypeModel("backup", activityTypeName, activityTypeCount)))

        assertThat((viewModel.filtersUiState.value as FiltersShown).activityTypeLabel)
            .isEqualTo(UiStringText(activityTypeName))
    }

    @Test
    fun activityTypeLabelWithCountShownWhenFilterHasMoreThanOneItem() {
        viewModel.onActivityTypesSelected(
            listOf(
                ActivityTypeModel("user", "User", 10),
                ActivityTypeModel("backup", "Backup", 5)
            )
        )

        val params = listOf(UiStringText("2"))
        assertThat((viewModel.filtersUiState.value as FiltersShown).activityTypeLabel)
            .isEqualTo(UiStringResWithParams(R.string.activity_log_activity_type_filter_active_label, params))
    }

    @Test
    fun dateRangeTrackDateRangeFilterClearedEventFired() {
        viewModel.onClearDateRangeFilterClicked()

        verify(activityLogTracker).trackDateRangeFilterCleared(rewindableOnly)
    }

    @Test
    fun verifyActivityLogEmptyScreenTextsWhenFiltersAreEmpty() {
        viewModel.onClearDateRangeFilterClicked()
        viewModel.onClearActivityTypeFilterClicked()

        assertThat(viewModel.emptyUiState.value).isEqualTo(EmptyUiState.ActivityLog.EmptyFilters)
    }

    @Test
    fun verifyActivityLogEmptyScreenTextsWhenDateRangeFilterSet() {
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(1L, 2L))

        assertThat(viewModel.emptyUiState.value).isEqualTo(EmptyUiState.ActivityLog.ActiveFilters)
    }

    @Test
    fun verifyActivityLogEmptyScreenTextsWhenActivityTypeFilterSet() {
        viewModel.onActivityTypesSelected(listOf(ActivityTypeModel("user", "User", 10)))

        assertThat(viewModel.emptyUiState.value).isEqualTo(EmptyUiState.ActivityLog.ActiveFilters)
    }

    @Test
    fun verifyBackupEmptyScreenTextsWhenFilterIsEmpty() {
        viewModel.rewindableOnly = true

        viewModel.onClearDateRangeFilterClicked()

        assertThat(viewModel.emptyUiState.value).isEqualTo(EmptyUiState.Backup.EmptyFilters)
    }

    @Test
    fun verifyBackupEmptyScreenTextsWhenDateRangeFilterSet() {
        viewModel.rewindableOnly = true
        whenever(mStatsDateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(1L, 2L))

        assertThat(viewModel.emptyUiState.value).isEqualTo(EmptyUiState.Backup.ActiveFilters)
    }

    /* RELOAD EVENTS - RESTORE */

    @Test
    fun `given no restore progress item, when reloading events, then the menu items are visible`() {
        val displayRestoreProgressItem = false

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = displayRestoreProgressItem)
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = displayRestoreProgressItem,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = displayRestoreProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given no restore progress item, when reloading events, then item is not visible`() {
        val displayRestoreProgressItem = false

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = displayRestoreProgressItem)
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = displayRestoreProgressItem,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = displayRestoreProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given no restore progress item, when reloading events, then move to top is not triggered`() {
        val displayRestoreProgressItem = false

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = displayRestoreProgressItem)
        )

        assertTrue(moveToTopEvents.isEmpty())
    }

    @Test
    fun `given restore progress item, when reloading events, then the menu items are not visible`() {
        val displayRestoreProgressItem = true
        val displayRestoreProgressWithDate = false
        initRestoreProgressMocks(displayRestoreProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = displayRestoreProgressItem)
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = displayRestoreProgressItem,
                restoreProgressWithDate = displayRestoreProgressWithDate,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = displayRestoreProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given restore progress item with date, when reloading events, then item is visible with date`() {
        val displayRestoreProgressItem = true
        val displayRestoreProgressWithDate = true
        initRestoreProgressMocks(displayRestoreProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = displayRestoreProgressItem, rewindId = REWIND_ID)
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = displayRestoreProgressItem,
                restoreProgressWithDate = displayRestoreProgressWithDate,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = displayRestoreProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given restore progress item without date, when reloading events, then item is visible without date`() {
        val displayRestoreProgressItem = true
        val displayRestoreProgressWithDate = false
        initRestoreProgressMocks(displayRestoreProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = displayRestoreProgressItem)
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = displayRestoreProgressItem,
                restoreProgressWithDate = displayRestoreProgressWithDate,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = displayRestoreProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given restore progress item, when reloading events, then move to top is triggered`() {
        initRestoreProgressMocks()

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = true, rewindId = REWIND_ID)
        )

        assertTrue(moveToTopEvents.isNotEmpty())
    }

    @Test
    fun `given restore finished with date, when reloading events, then show restore finished message with date`() {
        val date = activity().published
        initRestoreProgressFinishedMocks(date, true)

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(
                displayProgress = false,
                isCompleted = true,
                rewindId = REWIND_ID,
                published = date
            )
        )

        assertEquals(snackbarMessages.firstOrNull(), RESTORED_DATE_TIME)
    }

    @Test
    fun `given restore finished without date, when reloading events, then show restore finished msg without date`() {
        val date = null
        initRestoreProgressFinishedMocks(date, false)

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(
                displayProgress = false,
                isCompleted = true,
                rewindId = REWIND_ID,
                published = date
            )
        )

        assertEquals(snackbarMessages.firstOrNull(), RESTORED_NO_DATE)
    }

    /* RELOAD EVENTS - BACKUP DOWNLOAD */

    @Test
    fun `given no backup progress item, when reloading events, then the menu items are visible`() {
        val displayBackupProgressItem = false
        val displayNoticeItem = false

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem
            )
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = displayBackupProgressItem,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = displayBackupProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given no backup progress item, when reloading events, then item is not visible`() {
        val displayBackupProgressItem = false
        val displayNoticeItem = false

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem
            )
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = displayBackupProgressItem,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = displayBackupProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given no backup progress item, when reloading events, then move to top is not triggered`() {
        val displayBackupProgressItem = false
        val displayNoticeItem = false

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem
            )
        )

        assertTrue(moveToTopEvents.isEmpty())
    }

    @Test
    fun `given backup progress item, when reloading events, then the menu items are not visible`() {
        val displayBackupProgressItem = true
        val displayBackupProgressWithDate = false
        val displayNoticeItem = false
        initBackupProgressMocks(displayBackupProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem
            )
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = displayBackupProgressItem,
                backupProgressWithDate = displayBackupProgressWithDate,
                emptyList = false,
                rewindDisabled = displayBackupProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given backup progress item with date, when reloading events, then item is visible with date`() {
        val displayBackupProgressItem = true
        val displayBackupProgressWithDate = true
        val displayNoticeItem = false
        initBackupProgressMocks(displayBackupProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem,
                rewindId = REWIND_ID
            )
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = displayBackupProgressItem,
                backupProgressWithDate = displayBackupProgressWithDate,
                emptyList = false,
                rewindDisabled = displayBackupProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given backup progress item without date, when reloading events, then item is visible without date`() {
        val displayBackupProgressItem = true
        val displayBackupProgressWithDate = false
        val displayNoticeItem = false
        initBackupProgressMocks(displayBackupProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem
            )
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = displayBackupProgressItem,
                backupProgressWithDate = displayBackupProgressWithDate,
                emptyList = false,
                rewindDisabled = displayBackupProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given backup progress item, when reloading events, then move to top is triggered`() {
        initBackupProgressMocks()

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = true,
                displayNotice = false,
                rewindId = REWIND_ID
            )
        )

        assertTrue(moveToTopEvents.isNotEmpty())
    }

    @Test
    fun `given backup complete, when reloading events, then move to top is triggered`() {
        initBackupDownloadCompleteMocks()

        viewModel.reloadEvents(done = false, backupDownloadEvent = backupDownloadCompleteEvent())

        assertTrue(moveToTopEvents.isNotEmpty())
    }

    @Test
    fun `given backup finished, when reloading events, then list contains notice item`() {
        initBackupDownloadCompleteMocks()

        viewModel.reloadEvents(done = false, backupDownloadEvent = backupDownloadCompleteEvent())

        assertThat(events.first()?.first()).isInstanceOf(Notice::class.java)
    }

    @Test
    fun `given notice shown, when download clicked, then a navigationEvent is posted`() {
        initBackupDownloadCompleteMocks()

        viewModel.reloadEvents(done = false, backupDownloadEvent = backupDownloadCompleteEvent())

        val notice = events.first()?.filterIsInstance<Notice>()
        notice?.first()?.primaryAction?.invoke()
        assertThat(navigationEvents.last().peekContent()).isInstanceOf(DownloadBackupFile::class.java)
    }

    @Test
    fun `given notice shown, when dismiss clicked, then item is removed from list`() {
        initBackupDownloadCompleteMocks()

        viewModel.reloadEvents(done = false, backupDownloadEvent = backupDownloadCompleteEvent())

        events.first()
            ?.filterIsInstance<Notice>()
            ?.first()
            ?.secondaryAction
            ?.invoke()
        assertThat(events.last()?.filterIsInstance<Notice>()).isEmpty()
    }

    @Test
    fun `given notice shown, when dismiss clicked, then dismiss button tapped event is tracked`() {
        initBackupDownloadCompleteMocks()

        viewModel.reloadEvents(done = false, backupDownloadEvent = backupDownloadCompleteEvent())
        events.first()
            ?.filterIsInstance<Notice>()
            ?.first()
            ?.secondaryAction
            ?.invoke()

        verify(activityLogTracker).trackDownloadBackupDismissButtonClicked(rewindableOnly)
    }

    @Test
    fun `given notice shown, when download clicked, then download button tapped event is tracked`() {
        initBackupDownloadCompleteMocks()

        viewModel.reloadEvents(done = false, backupDownloadEvent = backupDownloadCompleteEvent())
        events.first()?.filterIsInstance<Notice>()?.first()?.primaryAction?.invoke()

        verify(activityLogTracker).trackDownloadBackupDownloadButtonClicked(rewindableOnly)
    }

    @Test
    fun `given backup progress item, when reloading events, then the notice banner is not visible`() {
        val displayBackupProgressItem = true
        val displayBackupProgressWithDate = true
        val displayNoticeItem = false
        initBackupProgressMocks(displayBackupProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem,
                rewindId = REWIND_ID
            )
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = displayBackupProgressItem,
                backupProgressWithDate = displayBackupProgressWithDate,
                emptyList = false,
                rewindDisabled = displayBackupProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    /* RELOAD EVENTS - RESTORE AND BACKUP DOWNLOAD */

    @Test
    fun `given restore and backup progress items, when reloading events, then both items are visible`() {
        val displayRestoreProgressItem = true
        val displayRestoreProgressWithDate = true
        val displayBackupProgressItem = true
        val displayBackupProgressWithDate = true
        val displayNoticeItem = false
        initRestoreProgressMocks(displayRestoreProgressWithDate)
        initBackupProgressMocks(displayBackupProgressWithDate)

        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(
                displayProgress = displayRestoreProgressItem,
                rewindId = REWIND_ID
            ),
            backupDownloadEvent = BackupDownloadEvent(
                displayProgress = displayBackupProgressItem,
                displayNotice = displayNoticeItem,
                rewindId = REWIND_ID
            )
        )

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = displayRestoreProgressItem,
                restoreProgressWithDate = displayRestoreProgressWithDate,
                displayBackupProgress = displayBackupProgressItem,
                backupProgressWithDate = displayBackupProgressWithDate,
                emptyList = false,
                rewindDisabled = displayRestoreProgressItem || displayBackupProgressItem,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    /* RELOAD EVENTS - DONE */

    @Test
    fun `given not done and the event list is empty, when reloading events, then the loading item is not visible`() {
        whenever(store.getActivityLogForSite(site, false, rewindableOnly)).thenReturn(emptyList())
        val done = false

        viewModel.reloadEvents(done)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = true,
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = false,
                withFooter = false
            )
        )
    }

    @Test
    fun `given not done and the event list is not empty, when reloading events, then the loading item is visible`() {
        whenever(store.getActivityLogForSite(site, false, rewindableOnly)).thenReturn(activityList.toList())
        val done = false

        viewModel.reloadEvents(done)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given done and the event list is empty, when reloading events, then the loading item is not visible`() {
        whenever(store.getActivityLogForSite(site, false, rewindableOnly)).thenReturn(emptyList())
        val done = true

        viewModel.reloadEvents(done)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = true,
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = false,
                withFooter = false
            )
        )
    }

    @Test
    fun `given done and the event list is not empty, when reloading events, then the loading item is not visible`() {
        whenever(store.getActivityLogForSite(site, false, rewindableOnly)).thenReturn(activityList.toList())
        val done = true

        viewModel.reloadEvents(done)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = false,
                withFooter = false
            )
        )
    }

    @Test
    fun `given done a not empty event list and free plan, when reloading events, then the footer item is visible`() {
        whenever(site.hasFreePlan).thenReturn(true)
        whenever(store.getActivityLogForSite(site, false, rewindableOnly)).thenReturn(activityList.toList())
        val done = true

        viewModel.reloadEvents(done)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = false,
                withFooter = true
            )
        )
    }

    /* QUERY RESTORE STATUS */

    @Test
    fun `when query restore status, then trigger get restore status`() = test {
        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        verify(getRestoreStatusUseCase).getRestoreStatus(site, RESTORE_ID)
    }

    @Test
    fun `given status is multisite, when query restore status, then reload events for multisite`() = test {
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID))
            .thenReturn(flow { emit(RestoreRequestState.Multisite) })
        initRestoreProgressMocks()

        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                emptyList = false,
                rewindDisabled = false,
                isRestoreHidden = true,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given status is a progress, when query restore status, then reload events for progress`() = test {
        val progress = RestoreRequestState.Progress(REWIND_ID, 50)
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID)).thenReturn(flow { emit(progress) })
        initRestoreProgressMocks()

        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = true,
                restoreProgressWithDate = true,
                emptyList = false,
                rewindDisabled = true,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given status is a complete, when query restore status, then request events update for complete`() = test {
        val progress = RestoreRequestState.Progress(REWIND_ID, 50)
        val complete = RestoreRequestState.Complete(REWIND_ID, RESTORE_ID)
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID))
            .thenReturn(flow { emit(progress); emit(complete) })
        initRestoreProgressMocks()
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(10, false, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                emptyList = false,
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = false,
                withFooter = false
            )
        )
    }

    @Test
    fun `given status is something else, when query restore status, then do not trigger anything`() = test {
        val success = RestoreRequestState.Success(REWIND_ID, REWIND_ID, RESTORE_ID)
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID)).thenReturn(flow { emit(success) })

        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        assertNull(viewModel.events.value)
    }

    @Test
    fun `when query restore status, then show restore started message`() {
        whenever(store.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(activity())
        whenever(resourceProvider.getString(eq(R.string.activity_log_rewind_started_snackbar_message), any(), any()))
            .thenReturn(RESTORE_STARTED)

        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        assertEquals(snackbarMessages.firstOrNull(), RESTORE_STARTED)
    }

    /* QUERY BACKUP DOWNLOAD STATUS */

    @Test
    fun `when query backup status, then trigger get backup download status`() = test {
        viewModel.onQueryBackupDownloadStatus(REWIND_ID, DOWNLOAD_ID, JetpackBackupDownloadActionState.PROGRESS.id)

        verify(getBackupDownloadStatusUseCase).getBackupDownloadStatus(site, DOWNLOAD_ID)
    }

    @Test
    fun `given status is a progress, when query backup status, then reload events for progress`() = test {
        val progress = BackupDownloadRequestState.Progress(REWIND_ID, 50)
        whenever(getBackupDownloadStatusUseCase.getBackupDownloadStatus(site, DOWNLOAD_ID))
            .thenReturn(flow { emit(progress) })
        initBackupProgressMocks()

        viewModel.onQueryBackupDownloadStatus(REWIND_ID, DOWNLOAD_ID, JetpackBackupDownloadActionState.PROGRESS.id)

        assertEquals(
            viewModel.events.value,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = true,
                backupProgressWithDate = true,
                emptyList = false,
                rewindDisabled = true,
                isLastPageAndFreeSite = false,
                canLoadMore = true,
                withFooter = false
            )
        )
    }

    @Test
    fun `given status is a complete, when query backup status, then request events update for complete`() = test {
        val progress = BackupDownloadRequestState.Progress(REWIND_ID, 50)
        val complete = BackupDownloadRequestState.Complete(
            REWIND_ID,
            DOWNLOAD_ID,
            DOWNLOAD_URL,
            DOWNLOAD_PUBLISHED,
            DOWNLOAD_VALID_UNTIL,
            DOWNLOAD_IS_VALID
        )
        whenever(getBackupDownloadStatusUseCase.getBackupDownloadStatus(site, DOWNLOAD_ID))
            .thenReturn(flow { emit(progress); emit(complete) })
        initBackupProgressMocks()
        initBackupDownloadCompleteMocks()
        whenever(store.fetchActivities(anyOrNull()))
            .thenReturn(OnActivityLogFetched(10, false, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.onQueryBackupDownloadStatus(REWIND_ID, DOWNLOAD_ID, JetpackBackupDownloadActionState.COMPLETE.id)

        assertThat(viewModel.events.value?.first() as? Notice).isNotNull
        val events = viewModel.events.value?.filterNot { it is Notice }
        assertEquals(
            events,
            expectedActivityList(
                displayRestoreProgress = false,
                restoreProgressWithDate = false,
                displayBackupProgress = false,
                backupProgressWithDate = false,
                emptyList = false,
                rewindDisabled = false,
                isLastPageAndFreeSite = false,
                canLoadMore = false,
                withFooter = false
            )
        )
    }

    @Test
    fun `given status is something else, when query backup status, then do not trigger anything`() = test {
        val success = BackupDownloadRequestState.Success(REWIND_ID, REWIND_ID, DOWNLOAD_ID)
        whenever(getBackupDownloadStatusUseCase.getBackupDownloadStatus(site, DOWNLOAD_ID))
            .thenReturn(flow { emit(success) })

        viewModel.onQueryBackupDownloadStatus(REWIND_ID, DOWNLOAD_ID, JetpackBackupDownloadActionState.CANCEL.id)

        assertNull(viewModel.events.value)
    }

    @Test
    fun `when query backup status in progress, then show backup download started message`() {
        whenever(store.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(activity())
        whenever(resourceProvider.getString(eq(R.string.activity_log_backup_started_snackbar_message), any(), any()))
            .thenReturn(BACKUP_STARTED)

        viewModel.onQueryBackupDownloadStatus(REWIND_ID, DOWNLOAD_ID, JetpackBackupDownloadActionState.PROGRESS.id)

        assertEquals(snackbarMessages.firstOrNull(), BACKUP_STARTED)
    }

    @Test
    fun `given published date, when query backup status complete, then show dates backup finished message`() {
        whenever(store.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(activity())
        whenever(resourceProvider.getString(eq(R.string.activity_log_backup_finished_snackbar_message), any(), any()))
            .thenReturn(BACKED_UP_DATE_TIME)

        viewModel.onQueryBackupDownloadStatus(REWIND_ID, DOWNLOAD_ID, JetpackBackupDownloadActionState.COMPLETE.id)

        assertEquals(snackbarMessages.firstOrNull(), BACKED_UP_DATE_TIME)
    }

    @Test
    fun `given no published date, when query backup status complete, then show no dates backup finished message`() {
        whenever(store.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(null)
        whenever(resourceProvider.getString(eq(R.string.activity_log_backup_finished_snackbar_message_no_dates)))
            .thenReturn(BACKED_UP_NO_DATE)

        viewModel.onQueryBackupDownloadStatus(REWIND_ID, DOWNLOAD_ID, JetpackBackupDownloadActionState.COMPLETE.id)

        assertEquals(snackbarMessages.firstOrNull(), BACKED_UP_NO_DATE)
    }

    /* PRIVATE */

    private fun firstActivity() = activity()

    private fun secondActivity() = activity(rewindable = false)

    private fun thirdActivity() = activity(published = activityPublishedTime(1987, 5, 26))

    private fun activity(
        rewindable: Boolean = true,
        published: Date = activityPublishedTime(1985, 8, 27)
    ) = ActivityLogModel(
        activityID = "activityId",
        summary = "",
        content = null,
        name = "",
        type = "",
        gridicon = "",
        status = "",
        rewindable = rewindable,
        rewindID = "",
        published = published,
        actor = null
    )

    private fun activityPublishedTime(year: Int, month: Int, date: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, date)
        return calendar.time
    }

    private fun expectedActivityList(
        displayRestoreProgress: Boolean = false,
        restoreProgressWithDate: Boolean = false,
        displayBackupProgress: Boolean = false,
        backupProgressWithDate: Boolean = false,
        emptyList: Boolean = false,
        rewindDisabled: Boolean = true,
        isRestoreHidden: Boolean = false,
        isLastPageAndFreeSite: Boolean = false,
        canLoadMore: Boolean = false,
        withFooter: Boolean = false
    ): List<ActivityLogListItem> {
        val list = mutableListOf<ActivityLogListItem>()
        if (displayRestoreProgress || displayBackupProgress) {
            list.add(ActivityLogListItem.Header(NOW))
        }
        if (displayRestoreProgress) {
            if (restoreProgressWithDate) {
                list.add(ActivityLogListItem.Progress(RESTORING_CURRENTLY, RESTORING_DATE_TIME, RESTORE))
            } else {
                list.add(ActivityLogListItem.Progress(RESTORING_CURRENTLY, RESTORING_NO_DATE, RESTORE))
            }
        }
        if (displayBackupProgress) {
            if (backupProgressWithDate) {
                list.add(ActivityLogListItem.Progress(BACKING_UP_CURRENTLY, BACKING_UP_DATE_TIME, BACKUP_DOWNLOAD))
            } else {
                list.add(ActivityLogListItem.Progress(BACKING_UP_CURRENTLY, BACKING_UP_NO_DATE, BACKUP_DOWNLOAD))
            }
        }
        if (!emptyList) {
            firstItem(rewindDisabled, isRestoreHidden).let {
                list.add(ActivityLogListItem.Header(it.formattedDate))
                list.add(it)
            }
            list.add(secondItem(rewindDisabled, isRestoreHidden))
            thirdItem(rewindDisabled, isRestoreHidden).let {
                list.add(ActivityLogListItem.Header(it.formattedDate))
                list.add(it)
            }
        }
        if (isLastPageAndFreeSite) {
            list.add(ActivityLogListItem.Footer)
        }
        if (canLoadMore) {
            list.add(ActivityLogListItem.Loading)
        }

        if (withFooter) {
            list.add(ActivityLogListItem.Footer)
        }
        return list
    }

    private fun firstItem(rewindDisabled: Boolean, isRestoreHidden: Boolean) = ActivityLogListItem.Event(
        model = activityList[0],
        rewindDisabled = rewindDisabled,
        isRestoreHidden = isRestoreHidden
    )

    private fun secondItem(rewindDisabled: Boolean, isRestoreHidden: Boolean) = ActivityLogListItem.Event(
        model = activityList[1],
        rewindDisabled = rewindDisabled,
        isRestoreHidden = isRestoreHidden
    )

    private fun thirdItem(rewindDisabled: Boolean, isRestoreHidden: Boolean) = ActivityLogListItem.Event(
        model = activityList[2],
        rewindDisabled = rewindDisabled,
        isRestoreHidden = isRestoreHidden
    )

    private suspend fun assertFetchEvents(canLoadMore: Boolean = false) {
        verify(store).fetchActivities(fetchActivityLogCaptor.capture())

        fetchActivityLogCaptor.lastValue.apply {
            assertEquals(canLoadMore, loadMore)
            assertEquals(this@ActivityLogViewModelTest.site, site)
        }
    }

    private fun event() = ActivityLogListItem.Event(
        activityId = "activityId",
        title = "",
        description = ",",
        gridIcon = null,
        eventStatus = null,
        isRewindable = true,
        rewindId = null,
        date = Date(),
        isButtonVisible = true,
        buttonIcon = ActivityLogListItem.Icon.DEFAULT,
        isRestoreHidden = false
    )

    private fun backupDownloadCompleteEvent() = BackupDownloadEvent(
        displayProgress = false,
        displayNotice = true,
        isCompleted = true,
        rewindId = REWIND_ID,
        published = activity().published,
        url = "www.wordpress.com",
        validUntil = activity().published,
        downloadId = 10L
    )

    private fun initRestoreProgressMocks(displayProgressWithDate: Boolean = true) {
        if (displayProgressWithDate) {
            whenever(store.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(activity())
        }
        whenever(resourceProvider.getString(R.string.now)).thenReturn(NOW)
        whenever(resourceProvider.getString(R.string.activity_log_currently_restoring_title))
            .thenReturn(RESTORING_CURRENTLY)
        if (displayProgressWithDate) {
            whenever(resourceProvider.getString(eq(R.string.activity_log_currently_restoring_message), any(), any()))
                .thenReturn(RESTORING_DATE_TIME)
        } else {
            whenever(resourceProvider.getString(R.string.activity_log_currently_restoring_message_no_dates))
                .thenReturn(RESTORING_NO_DATE)
        }
    }

    private fun initBackupProgressMocks(displayProgressWithDate: Boolean = true) {
        if (displayProgressWithDate) {
            whenever(store.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(activity())
        }
        whenever(resourceProvider.getString(R.string.now)).thenReturn(NOW)
        whenever(resourceProvider.getString(R.string.activity_log_currently_backing_up_title))
            .thenReturn(BACKING_UP_CURRENTLY)
        if (displayProgressWithDate) {
            whenever(resourceProvider.getString(eq(R.string.activity_log_currently_backing_up_message), any(), any()))
                .thenReturn(BACKING_UP_DATE_TIME)
        } else {
            whenever(resourceProvider.getString(R.string.activity_log_currently_backing_up_message_no_dates))
                .thenReturn(BACKING_UP_NO_DATE)
        }
    }

    private fun initRestoreProgressFinishedMocks(date: Date?, displayProgressWithDate: Boolean) {
        initRestoreProgressMocks(displayProgressWithDate)
        viewModel.reloadEvents(
            done = false,
            restoreEvent = RestoreEvent(displayProgress = true, rewindId = REWIND_ID)
        )
        if (date != null) {
            whenever(
                resourceProvider.getString(
                    eq(R.string.activity_log_rewind_finished_snackbar_message),
                    any(),
                    any()
                )
            ).thenReturn(RESTORED_DATE_TIME)
        } else {
            whenever(resourceProvider.getString(R.string.activity_log_rewind_finished_snackbar_message_no_dates))
                .thenReturn(RESTORED_NO_DATE)
        }
    }

    private fun initBackupDownloadCompleteMocks() {
        whenever(
            resourceProvider.getString(
                eq(R.string.activity_log_backup_download_notice_description_with_two_params),
                any(),
                any()
            )
        ).thenReturn(BACKUP_NOTICE)
        whenever(
            resourceProvider.getString(
                eq(R.string.activity_log_backup_finished_snackbar_message),
                any(),
                any()
            )
        ).thenReturn(BACKED_UP_DATE_TIME)
    }
}
