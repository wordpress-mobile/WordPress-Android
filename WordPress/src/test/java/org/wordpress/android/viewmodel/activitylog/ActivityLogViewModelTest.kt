package org.wordpress.android.viewmodel.activitylog

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.util.Pair
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.fluxc.action.ActivityLogAction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityLogPayload
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.test
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase.JetpackPurchasedProducts
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import org.wordpress.android.ui.jetpack.restore.usecases.PostRestoreUseCase
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.ActivityLogTracker
import org.wordpress.android.util.config.ActivityLogFiltersFeatureConfig
import org.wordpress.android.util.config.BackupDownloadFeatureConfig
import org.wordpress.android.util.config.RestoreFeatureConfig
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus
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
private const val RESTORED_DATE_TIME = "Your site has been successfully restored\\nRestored to date time"
private const val RESTORED_NO_DATE = "Your site has been successfully restored"

private const val REWIND_ID = "rewindId"
private const val RESTORE_ID = 123456789L

@RunWith(MockitoJUnitRunner::class)
class ActivityLogViewModelTest {
    @Rule @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var store: ActivityLogStore
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var postRestoreUseCase: PostRestoreUseCase
    @Mock private lateinit var getRestoreStatusUseCase: GetRestoreStatusUseCase
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var activityLogFiltersFeatureConfig: ActivityLogFiltersFeatureConfig
    @Mock private lateinit var backupDownloadFeatureConfig: BackupDownloadFeatureConfig
    @Mock private lateinit var dateUtils: DateUtils
    @Mock private lateinit var activityLogTracker: ActivityLogTracker
    @Mock private lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
    @Mock private lateinit var restoreFeatureConfig: RestoreFeatureConfig

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
                postRestoreUseCase,
                getRestoreStatusUseCase,
                resourceProvider,
                activityLogFiltersFeatureConfig,
                backupDownloadFeatureConfig,
                dateUtils,
                activityLogTracker,
                jetpackCapabilitiesUseCase,
                restoreFeatureConfig
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
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(anyLong()))
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
    fun onActionButtonClickShowsRewindDialog() {
        viewModel.onActionButtonClicked(event())

        assertThat(navigationEvents.last().peekContent())
                .isInstanceOf(ActivityLogNavigationEvents.ShowRewindDialog::class.java)
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
    fun filtersAreNotVisibleWhenFiltersFeatureFlagIsDisabled() = test {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.start(site, rewindableOnly)

        assertEquals(false, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun filtersAreVisibleWhenFiltersFeatureFlagIsEnabled() = test {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.start(site, rewindableOnly)

        assertEquals(true, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun filtersAreVisibleWhenSiteOnPaidPlan() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(site.hasFreePlan).thenReturn(false)

        viewModel.start(site, rewindableOnly)

        assertEquals(true, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun filtersAreNotVisibleWhenSiteOnFreePlan() {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(site.hasFreePlan).thenReturn(true)

        viewModel.start(site, rewindableOnly)

        assertEquals(false, viewModel.filtersUiState.value!!.visibility)
    }

    @Test
    fun filtersAreVisibleWhenSiteOnFreePlanButHasPurchasedBackupProduct() = test {
        whenever(activityLogFiltersFeatureConfig.isEnabled()).thenReturn(true)
        whenever(site.hasFreePlan).thenReturn(true)
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(SITE_ID))
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
    fun onSecondaryActionClickRestoreNavigationEventIsShowRewindDialog() {
        viewModel.onSecondaryActionClicked(ActivityLogListItem.SecondaryAction.RESTORE, event())

        assertThat(navigationEvents.last().peekContent())
                .isInstanceOf(ActivityLogNavigationEvents.ShowRewindDialog::class.java)
    }

    @Test
    fun onSecondaryActionClickDownloadBackupNavigationEventIsShowBackupDownload() {
        viewModel.onSecondaryActionClicked(ActivityLogListItem.SecondaryAction.DOWNLOAD_BACKUP, event())

        assertThat(navigationEvents.last().peekContent())
                .isInstanceOf(ActivityLogNavigationEvents.ShowBackupDownload::class.java)
    }

    @Test
    fun dateRangeFilterClearActionShownWhenFilterNotEmpty() {
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")
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
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")
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
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(10L, 20L))

        assertThat((viewModel.filtersUiState.value as FiltersShown).dateRangeLabel).isEqualTo(UiStringText("TEST"))
    }

    @Test
    fun dateRangeLabelFormattingUsesGMT0Timezone() {
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), formatDateRangeTimezoneCaptor.capture()))
                .thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(10L, 20L))

        assertThat(formatDateRangeTimezoneCaptor.firstValue).isEqualTo(TIMEZONE_GMT_0)
    }

    @Test
    fun dateRangeEndTimestampGetsAdjustedToEndOfDay() {
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

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
    fun verifyActivityLogEmptyScreenTextsWhenFiltersAreEmpty() {
        viewModel.onClearDateRangeFilterClicked()
        viewModel.onClearActivityTypeFilterClicked()

        assertThat(viewModel.emptyUiState.value).isEqualTo(EmptyUiState.ActivityLog.EmptyFilters)
    }

    @Test
    fun verifyActivityLogEmptyScreenTextsWhenDateRangeFilterSet() {
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

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
        whenever(dateUtils.formatDateRange(anyOrNull(), anyOrNull(), anyOrNull())).thenReturn("TEST")

        viewModel.onDateRangeSelected(Pair(1L, 2L))

        assertThat(viewModel.emptyUiState.value).isEqualTo(EmptyUiState.Backup.ActiveFilters)
    }

    /* RELOAD EVENTS */

    @Test
    fun `given no progress item, when reloading events, then the menu items are visible`() {
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = false,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = false,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
                        isLastPageAndFreeSite = false,
                        canLoadMore = true,
                        withFooter = false
                )
        )
    }

    @Test
    fun `given no progress item, when reloading events, then the progress item is not visible`() {
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = false,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = false,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
                        isLastPageAndFreeSite = false,
                        canLoadMore = true,
                        withFooter = false
                )
        )
    }

    @Test
    fun `given no progress item, when reloading events, then move to top is not triggered`() {
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = false,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertTrue(moveToTopEvents.isEmpty())
    }

    @Test
    fun `given progress item, when reloading events, then the menu items are not visible`() {
        val displayProgressItem = true
        val displayProgressWithDate = false
        initProgressMocks(displayProgressWithDate)

        viewModel.reloadEvents(
                done = false,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = displayProgressWithDate,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
                        isLastPageAndFreeSite = false,
                        canLoadMore = true,
                        withFooter = false
                )
        )
    }

    @Test
    fun `given progress item with date, when reloading events, then the progress item is visible with date`() {
        val displayProgressItem = true
        val displayProgressWithDate = true
        initProgressMocks(displayProgressWithDate)

        viewModel.reloadEvents(
                done = false,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem, rewindId = REWIND_ID)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = displayProgressWithDate,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
                        isLastPageAndFreeSite = false,
                        canLoadMore = true,
                        withFooter = false
                )
        )
    }

    @Test
    fun `given progress item without date, when reloading events, then the progress item is visible without date`() {
        val displayProgressItem = true
        val displayProgressWithDate = false
        initProgressMocks(displayProgressWithDate)

        viewModel.reloadEvents(
                done = false,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = displayProgressWithDate,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
                        isLastPageAndFreeSite = false,
                        canLoadMore = true,
                        withFooter = false
                )
        )
    }

    @Test
    fun `given progress item, when reloading events, then move to top is triggered`() {
        initProgressMocks()

        viewModel.reloadEvents(
                done = false,
                restoreEvent = RestoreEvent(displayProgress = true, rewindId = REWIND_ID)
        )

        assertTrue(moveToTopEvents.isNotEmpty())
    }

    @Test
    fun `given restore finished with date, when reloading events, then show restore finished message with date`() {
        val date = activity().published
        initProgressFinishedMocks(date, true)

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
        initProgressFinishedMocks(date, false)

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

    @Test
    fun `given not done and the event list is empty, when reloading events, then the loading item is not visible`() {
        whenever(store.getActivityLogForSite(site, false, rewindableOnly)).thenReturn(emptyList())
        val done = false
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = done,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = false,
                        emptyList = true,
                        rewindDisabled = displayProgressItem,
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
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = done,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = false,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
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
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = done,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = false,
                        emptyList = true,
                        rewindDisabled = displayProgressItem,
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
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = done,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = false,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
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
        val displayProgressItem = false

        viewModel.reloadEvents(
                done = done,
                restoreEvent = RestoreEvent(displayProgress = displayProgressItem)
        )

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = displayProgressItem,
                        progressWithDate = false,
                        emptyList = false,
                        rewindDisabled = displayProgressItem,
                        isLastPageAndFreeSite = false,
                        canLoadMore = false,
                        withFooter = true
                )
        )
    }

    /* RESTORE CONFIRMED */

    @Test
    fun `when restore confirmed, then trigger post restore request`() = test {
        viewModel.onRestoreConfirmed(REWIND_ID)

        verify(postRestoreUseCase).postRestoreRequest(REWIND_ID, site)
    }

    @Test
    fun `given restore requests is a success, when restore confirmed, then do trigger get restore status`() = test {
        val success = RestoreRequestState.Success(REWIND_ID, REWIND_ID, RESTORE_ID)
        whenever(postRestoreUseCase.postRestoreRequest(REWIND_ID, site)).thenReturn(success)

        viewModel.onRestoreConfirmed(REWIND_ID)

        verify(getRestoreStatusUseCase).getRestoreStatus(site, RESTORE_ID)
    }

    @Test
    fun `given restore requests is something else, when restore confirmed, then do not trigger anything`() = test {
        val progress = RestoreRequestState.Progress(REWIND_ID, 50)
        whenever(postRestoreUseCase.postRestoreRequest(REWIND_ID, site)).thenReturn(progress)

        viewModel.onRestoreConfirmed(REWIND_ID)

        verify(getRestoreStatusUseCase, times(0)).getRestoreStatus(site, RESTORE_ID)
    }

    @Test
    fun `given restore status is a progress, when restore confirmed, then reload events for progress`() = test {
        val success = RestoreRequestState.Success(REWIND_ID, REWIND_ID, RESTORE_ID)
        whenever(postRestoreUseCase.postRestoreRequest(REWIND_ID, site)).thenReturn(success)
        val progress = RestoreRequestState.Progress(REWIND_ID, 50)
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID)).thenReturn(flow { emit(progress) })
        initProgressMocks()

        viewModel.onRestoreConfirmed(REWIND_ID)

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = true,
                        progressWithDate = true,
                        emptyList = false,
                        rewindDisabled = true,
                        isLastPageAndFreeSite = false,
                        canLoadMore = true,
                        withFooter = false
                )
        )
    }

    @Test
    fun `given restore status is a complete, when restore confirmed, then request events update for complete`() = test {
        val success = RestoreRequestState.Success(REWIND_ID, REWIND_ID, RESTORE_ID)
        whenever(postRestoreUseCase.postRestoreRequest(REWIND_ID, site)).thenReturn(success)
        val progress = RestoreRequestState.Progress(REWIND_ID, 50)
        val complete = RestoreRequestState.Complete(REWIND_ID, RESTORE_ID)
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID))
                .thenReturn(flow { emit(progress); emit(complete) })
        initProgressMocks()
        whenever(store.fetchActivities(anyOrNull()))
                .thenReturn(OnActivityLogFetched(10, false, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.onRestoreConfirmed(REWIND_ID)

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = false,
                        progressWithDate = false,
                        emptyList = false,
                        rewindDisabled = false,
                        isLastPageAndFreeSite = false,
                        canLoadMore = false,
                        withFooter = false
                )
        )
    }

    @Test
    fun `given restore status is something else, when restore confirmed, then do not trigger anything`() = test {
        val success = RestoreRequestState.Success(REWIND_ID, REWIND_ID, RESTORE_ID)
        whenever(postRestoreUseCase.postRestoreRequest(REWIND_ID, site)).thenReturn(success)
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID)).thenReturn(flow { emit(success) })

        viewModel.onRestoreConfirmed(REWIND_ID)

        assertNull(viewModel.events.value)
    }

    @Test
    fun `when restore confirmed, then show restore started message`() {
        whenever(store.getActivityLogItemByRewindId(REWIND_ID)).thenReturn(activity())
        whenever(resourceProvider.getString(eq(R.string.activity_log_rewind_started_snackbar_message), any(), any()))
                .thenReturn(RESTORE_STARTED)

        viewModel.onRestoreConfirmed(REWIND_ID)

        assertEquals(snackbarMessages.firstOrNull(), RESTORE_STARTED)
    }

    /* QUERY RESTORE STATUS */

    @Test
    fun `when query restore statues, then trigger get restore status`() = test {
        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        verify(getRestoreStatusUseCase).getRestoreStatus(site, RESTORE_ID)
    }

    @Test
    fun `given status is a progress, when query restore status, then reload events for progress`() = test {
        val progress = RestoreRequestState.Progress(REWIND_ID, 50)
        whenever(getRestoreStatusUseCase.getRestoreStatus(site, RESTORE_ID)).thenReturn(flow { emit(progress) })
        initProgressMocks()

        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = true,
                        progressWithDate = true,
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
        initProgressMocks()
        whenever(store.fetchActivities(anyOrNull()))
                .thenReturn(OnActivityLogFetched(10, false, ActivityLogAction.FETCH_ACTIVITIES))

        viewModel.onQueryRestoreStatus(REWIND_ID, RESTORE_ID)

        assertEquals(
                viewModel.events.value,
                expectedActivityList(
                        displayProgress = false,
                        progressWithDate = false,
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
        displayProgress: Boolean = false,
        progressWithDate: Boolean = false,
        emptyList: Boolean = false,
        rewindDisabled: Boolean = true,
        isLastPageAndFreeSite: Boolean = false,
        canLoadMore: Boolean = false,
        withFooter: Boolean = false
    ): List<ActivityLogListItem> {
        val list = mutableListOf<ActivityLogListItem>()
        if (displayProgress) {
            list.add(ActivityLogListItem.Header(NOW))
            if (progressWithDate) {
                list.add(ActivityLogListItem.Progress(RESTORING_CURRENTLY, RESTORING_DATE_TIME))
            } else {
                list.add(ActivityLogListItem.Progress(RESTORING_CURRENTLY, RESTORING_NO_DATE))
            }
        }
        if (!emptyList) {
            firstItem(rewindDisabled).let {
                list.add(ActivityLogListItem.Header(it.formattedDate))
                list.add(it)
            }
            list.add(secondItem(rewindDisabled))
            thirdItem(rewindDisabled).let {
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

    private fun firstItem(rewindDisabled: Boolean) = ActivityLogListItem.Event(
            model = activityList[0],
            rewindDisabled = rewindDisabled,
            backupDownloadFeatureEnabled = false,
            restoreFeatureEnabled = false
    )

    private fun secondItem(rewindDisabled: Boolean) = ActivityLogListItem.Event(
            model = activityList[1],
            rewindDisabled = rewindDisabled,
            backupDownloadFeatureEnabled = false,
            restoreFeatureEnabled = false
    )

    private fun thirdItem(rewindDisabled: Boolean) = ActivityLogListItem.Event(
            model = activityList[2],
            rewindDisabled = rewindDisabled,
            backupDownloadFeatureEnabled = false,
            restoreFeatureEnabled = false
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
            isProgressBarVisible = false
    )

    private fun initProgressMocks(displayProgressWithDate: Boolean = true) {
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

    private fun initProgressFinishedMocks(date: Date?, displayProgressWithDate: Boolean) {
        initProgressMocks(displayProgressWithDate)
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
}
