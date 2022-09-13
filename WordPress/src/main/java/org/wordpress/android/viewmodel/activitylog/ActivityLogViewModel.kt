package org.wordpress.android.viewmodel.activitylog

import androidx.annotation.VisibleForTesting
import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadRequestState
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostDismissBackupDownloadUseCase
import org.wordpress.android.ui.jetpack.common.JetpackBackupDownloadActionState.PROGRESS
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.analytics.ActivityLogTracker
import org.wordpress.android.util.extensions.toFormattedDateString
import org.wordpress.android.util.extensions.toFormattedTimeString
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersHidden
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersShown
import java.util.Date
import javax.inject.Inject

const val ACTIVITY_LOG_REWINDABLE_ONLY_KEY = "activity_log_rewindable_only"

private const val DAY_IN_MILLIS = 1000 * 60 * 60 * 24
private const val ONE_SECOND_IN_MILLIS = 1000
private const val TIMEZONE_GMT_0 = "GMT+0"

typealias DateRange = Pair<Long, Long>

/**
 * It was decided to reuse the 'Activity Log' screen instead of creating a new 'Backup' screen. This was due to the
 * fact that there will be lots of code that would need to be duplicated for the new 'Backup' screen. On the other
 * hand, not much more complexity would be introduced if the 'Activity Log' screen is reused (mainly some 'if/else'
 * code branches here and there).
 *
 * However, should more 'Backup' related additions are added to the 'Activity Log' screen, then it should become a
 * necessity to split those features in separate screens in order not to increase further the complexity of this
 * screen's architecture.
 */
@Suppress("LargeClass")
class ActivityLogViewModel @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    private val getRestoreStatusUseCase: GetRestoreStatusUseCase,
    private val getBackupDownloadStatusUseCase: GetBackupDownloadStatusUseCase,
    private val postDismissBackupDownloadUseCase: PostDismissBackupDownloadUseCase,
    private val resourceProvider: ResourceProvider,
    private val dateUtils: DateUtils,
    private val activityLogTracker: ActivityLogTracker,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase
) : ViewModel() {
    enum class ActivityLogListStatus {
        CAN_LOAD_MORE,
        DONE,
        ERROR,
        FETCHING,
        LOADING_MORE
    }

    private var isStarted = false

    private val _events = MutableLiveData<List<ActivityLogListItem>>()
    val events: LiveData<List<ActivityLogListItem>>
        get() = _events

    private val _eventListStatus = MutableLiveData<ActivityLogListStatus>()
    val eventListStatus: LiveData<ActivityLogListStatus>
        get() = _eventListStatus

    private val _filtersUiState = MutableLiveData<FiltersUiState>(FiltersHidden)
    val filtersUiState: LiveData<FiltersUiState>
        get() = _filtersUiState

    private val _emptyUiState = MutableLiveData<EmptyUiState>(EmptyUiState.ActivityLog.EmptyFilters)
    val emptyUiState: LiveData<EmptyUiState> = _emptyUiState

    private val _showActivityTypeFilterDialog = SingleLiveEvent<ShowActivityTypePicker>()
    val showActivityTypeFilterDialog: LiveData<ShowActivityTypePicker>
        get() = _showActivityTypeFilterDialog

    private val _showDateRangePicker = SingleLiveEvent<ShowDateRangePicker>()
    val showDateRangePicker: LiveData<ShowDateRangePicker>
        get() = _showDateRangePicker

    private val _moveToTop = SingleLiveEvent<Unit>()
    val moveToTop: SingleLiveEvent<Unit>
        get() = _moveToTop

    private val _showItemDetail = SingleLiveEvent<ActivityLogListItem>()
    val showItemDetail: LiveData<ActivityLogListItem>
        get() = _showItemDetail

    private val _showSnackbarMessage = SingleLiveEvent<String>()
    val showSnackbarMessage: LiveData<String>
        get() = _showSnackbarMessage

    private val _navigationEvents =
            MutableLiveData<Event<ActivityLogNavigationEvents>>()
    val navigationEvents: LiveData<Event<ActivityLogNavigationEvents>>
        get() = _navigationEvents

    private val isRestoreProgressItemShown: Boolean
        get() = events.value?.find {
            it is ActivityLogListItem.Progress &&
                    it.progressType == ActivityLogListItem.Progress.Type.RESTORE
        } != null

    private val isBackupDownloadProgressItemShown: Boolean
        get() = events.value?.find {
            it is ActivityLogListItem.Progress &&
                    it.progressType == ActivityLogListItem.Progress.Type.BACKUP_DOWNLOAD
        } != null

    private val isDone: Boolean
        get() = eventListStatus.value == ActivityLogListStatus.DONE

    private var fetchActivitiesJob: Job? = null
    private var restoreStatusJob: Job? = null
    private var backupDownloadStatusJob: Job? = null

    private var currentDateRangeFilter: DateRange? = null
    private var currentActivityTypeFilter: List<ActivityTypeModel> = listOf()

    lateinit var site: SiteModel
    var rewindableOnly: Boolean = false

    private var currentRestoreEvent = RestoreEvent(false)
    private var currentBackupDownloadEvent = BackupDownloadEvent(displayProgress = false, displayNotice = false)

    fun start(site: SiteModel, rewindableOnly: Boolean) {
        if (isStarted) {
            return
        }
        isStarted = true

        this.site = site
        this.rewindableOnly = rewindableOnly

        reloadEvents(true)
        requestEventsUpdate(false)

        showFiltersIfSupported()
    }

    @Suppress("LongMethod", "ComplexMethod")
    @VisibleForTesting
    fun reloadEvents(
        done: Boolean = isDone,
        restoreEvent: RestoreEvent = currentRestoreEvent,
        backupDownloadEvent: BackupDownloadEvent = currentBackupDownloadEvent
    ) {
        currentRestoreEvent = restoreEvent
        currentBackupDownloadEvent = backupDownloadEvent
        val eventList = activityLogStore.getActivityLogForSite(
                site = site,
                ascending = false,
                rewindableOnly = rewindableOnly
        )
        val items = mutableListOf<ActivityLogListItem>()
        var moveToTop = false
        val withRestoreProgressItem = restoreEvent.displayProgress && !restoreEvent.isCompleted
        val withBackupDownloadProgressItem = backupDownloadEvent.displayProgress && !backupDownloadEvent.isCompleted
        val withBackupDownloadNoticeItem = backupDownloadEvent.displayNotice
        if (withRestoreProgressItem || withBackupDownloadProgressItem) {
            items.add(ActivityLogListItem.Header(resourceProvider.getString(R.string.now)))
            moveToTop = eventListStatus.value != ActivityLogListStatus.LOADING_MORE
        }
        if (withRestoreProgressItem) {
            items.add(getRestoreProgressItem(restoreEvent.rewindId, restoreEvent.published))
        }
        if (withBackupDownloadProgressItem) {
            items.add(getBackupDownloadProgressItem(backupDownloadEvent.rewindId, backupDownloadEvent.published))
        }
        if (withBackupDownloadNoticeItem) {
            getBackupDownloadNoticeItem(backupDownloadEvent)?.let {
                items.add(it)
                moveToTop = true
            }
        }
        eventList.forEach { model ->
            val currentItem = ActivityLogListItem.Event(
                    model = model,
                    rewindDisabled = withRestoreProgressItem || withBackupDownloadProgressItem,
                    isRestoreHidden = restoreEvent.isRestoreHidden
            )
            val lastItem = items.lastOrNull() as? ActivityLogListItem.Event
            if (lastItem == null || lastItem.formattedDate != currentItem.formattedDate) {
                items.add(ActivityLogListItem.Header(currentItem.formattedDate))
            }
            items.add(currentItem)
        }
        if (eventList.isNotEmpty() && !done) {
            items.add(ActivityLogListItem.Loading)
        }
        if (eventList.isNotEmpty() && site.hasFreePlan && done) {
            items.add(ActivityLogListItem.Footer)
        }

        _events.value = items
        if (moveToTop) {
            _moveToTop.call()
        }
        if (restoreEvent.isCompleted) {
            showRestoreFinishedMessage(restoreEvent.rewindId, restoreEvent.published)
            currentRestoreEvent = RestoreEvent(false)
        }
    }

    private fun getRestoreProgressItem(rewindId: String?, published: Date?): ActivityLogListItem.Progress {
        val rewindDate = published ?: rewindId?.let { activityLogStore.getActivityLogItemByRewindId(it)?.published }
        return rewindDate?.let {
            ActivityLogListItem.Progress(
                    resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                    resourceProvider.getString(
                            R.string.activity_log_currently_restoring_message,
                            rewindDate.toFormattedDateString(),
                            rewindDate.toFormattedTimeString()
                    ),
                    ActivityLogListItem.Progress.Type.RESTORE
            )
        } ?: ActivityLogListItem.Progress(
                resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                resourceProvider.getString(R.string.activity_log_currently_restoring_message_no_dates),
                ActivityLogListItem.Progress.Type.RESTORE
        )
    }

    private fun getBackupDownloadProgressItem(rewindId: String?, published: Date?): ActivityLogListItem.Progress {
        val rewindDate = published ?: rewindId?.let { activityLogStore.getActivityLogItemByRewindId(it)?.published }
        return rewindDate?.let {
            ActivityLogListItem.Progress(
                    resourceProvider.getString(R.string.activity_log_currently_backing_up_title),
                    resourceProvider.getString(
                            R.string.activity_log_currently_backing_up_message,
                            rewindDate.toFormattedDateString(), rewindDate.toFormattedTimeString()
                    ),
                    ActivityLogListItem.Progress.Type.BACKUP_DOWNLOAD
            )
        } ?: ActivityLogListItem.Progress(
                resourceProvider.getString(R.string.activity_log_currently_backing_up_title),
                resourceProvider.getString(R.string.activity_log_currently_backing_up_message_no_dates),
                ActivityLogListItem.Progress.Type.BACKUP_DOWNLOAD
        )
    }

    private fun getBackupDownloadNoticeItem(backupDownloadEvent: BackupDownloadEvent): ActivityLogListItem.Notice? {
        val rewindDate = backupDownloadEvent.published
                ?: backupDownloadEvent.rewindId?.let { activityLogStore.getActivityLogItemByRewindId(it)?.published }
        return rewindDate?.let {
            ActivityLogListItem.Notice(
                    label = resourceProvider.getString(
                            R.string.activity_log_backup_download_notice_description_with_two_params,
                            rewindDate.toFormattedDateString(), rewindDate.toFormattedTimeString()
                    ),
                    primaryAction = { onDownloadBackupFileClicked(backupDownloadEvent.url as String) },
                    secondaryAction = { dismissNoticeClicked(backupDownloadEvent.downloadId as Long) }
            )
        }
    }

    private fun showRestoreFinishedMessage(rewindId: String?, published: Date?) {
        val rewindDate = published ?: rewindId?.let { activityLogStore.getActivityLogItemByRewindId(it)?.published }
        if (rewindDate != null) {
            _showSnackbarMessage.value =
                    resourceProvider.getString(
                            R.string.activity_log_rewind_finished_snackbar_message,
                            rewindDate.toFormattedDateString(),
                            rewindDate.toFormattedTimeString()
                    )
        } else {
            _showSnackbarMessage.value =
                    resourceProvider.getString(R.string.activity_log_rewind_finished_snackbar_message_no_dates)
        }
    }

    private fun showBackupDownloadFinishedMessage(rewindId: String?) {
        val rewindDate = rewindId?.let { activityLogStore.getActivityLogItemByRewindId(it)?.published }
        if (rewindDate != null) {
            _showSnackbarMessage.value =
                    resourceProvider.getString(
                            R.string.activity_log_backup_finished_snackbar_message,
                            rewindDate.toFormattedDateString(),
                            rewindDate.toFormattedTimeString()
                    )
        } else {
            _showSnackbarMessage.value =
                    resourceProvider.getString(R.string.activity_log_backup_finished_snackbar_message_no_dates)
        }
    }

    private fun requestEventsUpdate(
        loadMore: Boolean,
        restoreEvent: RestoreEvent = currentRestoreEvent,
        backupDownloadEvent: BackupDownloadEvent = currentBackupDownloadEvent
    ) {
        val isLoadingMore = fetchActivitiesJob != null && eventListStatus.value == ActivityLogListStatus.LOADING_MORE
        val canLoadMore = eventListStatus.value == ActivityLogListStatus.CAN_LOAD_MORE
        if (loadMore && (isLoadingMore || !canLoadMore)) {
            // Ignore loadMore request when already loading more items or there are no more items to load
            return
        }
        fetchActivitiesJob?.cancel()
        val newStatus = if (loadMore) ActivityLogListStatus.LOADING_MORE else ActivityLogListStatus.FETCHING
        _eventListStatus.value = newStatus
        val payload = ActivityLogStore.FetchActivityLogPayload(
                site,
                loadMore,
                currentDateRangeFilter?.first?.let { Date(it) },
                currentDateRangeFilter?.second?.let { Date(it) },
                currentActivityTypeFilter.map { it.key }
        )
        fetchActivitiesJob = viewModelScope.launch {
            val result = activityLogStore.fetchActivities(payload)
            if (isActive) {
                onActivityLogFetched(result, loadMore, restoreEvent, backupDownloadEvent)
                fetchActivitiesJob = null
            }
        }
    }

    private fun onActivityLogFetched(
        event: OnActivityLogFetched,
        loadingMore: Boolean,
        restoreEvent: RestoreEvent,
        backupDownloadEvent: BackupDownloadEvent
    ) {
        if (event.isError) {
            _eventListStatus.value = ActivityLogListStatus.ERROR
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Activity log events")
            return
        }

        if (event.rowsAffected > 0) {
            reloadEvents(
                    done = !event.canLoadMore,
                    restoreEvent = restoreEvent,
                    backupDownloadEvent = backupDownloadEvent
            )
            if (!loadingMore) {
                moveToTop.call()
            }
            if (!restoreEvent.isCompleted) queryRestoreStatus()
            if (!backupDownloadEvent.isCompleted) queryBackupDownloadStatus()
        }

        if (event.canLoadMore) {
            _eventListStatus.value = ActivityLogListStatus.CAN_LOAD_MORE
        } else {
            _eventListStatus.value = ActivityLogListStatus.DONE
        }
    }

    private fun showFiltersIfSupported() {
        when {
            !site.hasFreePlan -> refreshFiltersUiState()
            else -> {
                viewModelScope.launch {
                    val purchasedProducts = jetpackCapabilitiesUseCase.getCachedJetpackPurchasedProducts(site.siteId)
                    if (purchasedProducts.backup) {
                        refreshFiltersUiState()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        if (currentDateRangeFilter != null || currentActivityTypeFilter.isNotEmpty()) {
            /**
             * Clear cache when filters are not empty. Filters are not retained across sessions, therefore the data is
             * not relevant when the screen is accessed next time.
             */
            activityLogStore.clearActivityLogCache(site)
        }
        jetpackCapabilitiesUseCase.clear()

        super.onCleared()
    }

    private fun refreshFiltersUiState() {
        val (activityTypeLabel, activityTypeLabelContentDescription) = createActivityTypeFilterLabel()
        val (dateRangeLabel, dateRangeLabelContentDescription) = createDateRangeFilterLabel()
        _filtersUiState.value = FiltersShown(
                dateRangeLabel,
                dateRangeLabelContentDescription,
                activityTypeLabel,
                activityTypeLabelContentDescription,
                currentDateRangeFilter?.let { ::onClearDateRangeFilterClicked },
                currentActivityTypeFilter.takeIf { it.isNotEmpty() }?.let { ::onClearActivityTypeFilterClicked }
        )
        refreshEmptyUiState()
    }

    private fun refreshEmptyUiState() {
        if (rewindableOnly) {
            refreshBackupEmptyUiState()
        } else {
            refreshActivityLogEmptyUiState()
        }
    }

    private fun refreshBackupEmptyUiState() {
        if (currentDateRangeFilter != null) {
            _emptyUiState.value = EmptyUiState.Backup.ActiveFilters
        } else {
            _emptyUiState.value = EmptyUiState.Backup.EmptyFilters
        }
    }

    private fun refreshActivityLogEmptyUiState() {
        if (currentDateRangeFilter != null || currentActivityTypeFilter.isNotEmpty()) {
            _emptyUiState.value = EmptyUiState.ActivityLog.ActiveFilters
        } else {
            _emptyUiState.value = EmptyUiState.ActivityLog.EmptyFilters
        }
    }

    private fun createDateRangeFilterLabel(): kotlin.Pair<UiString, UiString> {
        return currentDateRangeFilter?.let {
            val label = UiStringText(
                    dateUtils.formatDateRange(requireNotNull(it.first), requireNotNull(it.second), TIMEZONE_GMT_0)
            )
            kotlin.Pair(label, label)
        } ?: kotlin.Pair(
                UiStringRes(R.string.activity_log_date_range_filter_label),
                UiStringRes(R.string.activity_log_date_range_filter_label_content_description)
        )
    }

    private fun createActivityTypeFilterLabel(): kotlin.Pair<UiString, UiString> {
        return currentActivityTypeFilter.takeIf { it.isNotEmpty() }?.let {
            if (it.size == 1) {
                kotlin.Pair(
                        UiStringText(it[0].name),
                        UiStringResWithParams(
                                R.string.activity_log_activity_type_filter_single_item_selected_content_description,
                                listOf(UiStringText(it[0].name), UiStringText(it[0].count.toString()))
                        )
                )
            } else {
                kotlin.Pair(
                        UiStringResWithParams(
                                R.string.activity_log_activity_type_filter_active_label,
                                listOf(UiStringText("${it.size}"))
                        ),
                        UiStringResWithParams(
                                R.string.activity_log_activity_type_filter_multiple_items_selected_content_description,
                                listOf(UiStringText("${it.size}"))
                        )
                )
            }
        } ?: kotlin.Pair(
                UiStringRes(R.string.activity_log_activity_type_filter_label),
                UiStringRes(R.string.activity_log_activity_type_filter_no_item_selected_content_description)
        )
    }

    fun onScrolledToBottom() {
        requestEventsUpdate(true)
    }

    fun onPullToRefresh() {
        requestEventsUpdate(false)
    }

    fun onItemClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            _showItemDetail.value = item
        }
    }

    fun onSecondaryActionClicked(
        secondaryAction: ActivityLogListItem.SecondaryAction,
        item: ActivityLogListItem
    ): Boolean {
        if (item is ActivityLogListItem.Event) {
            val navigationEvent = when (secondaryAction) {
                ActivityLogListItem.SecondaryAction.RESTORE -> {
                    ActivityLogNavigationEvents.ShowRestore(item)
                }
                ActivityLogListItem.SecondaryAction.DOWNLOAD_BACKUP -> {
                    ActivityLogNavigationEvents.ShowBackupDownload(item)
                }
            }
            _navigationEvents.value = Event(navigationEvent)
        }
        return true
    }

    private fun onDownloadBackupFileClicked(url: String) {
        activityLogTracker.trackDownloadBackupDownloadButtonClicked(rewindableOnly)
        _navigationEvents.value = Event(ActivityLogNavigationEvents.DownloadBackupFile(url))
    }
    /**
    Reload events first to remove the notice item, as it shows progress to the user. Then
    trigger the dismiss (this is an optimistic call). If the dismiss fails it will show
    again on the next reload.
    */
    private fun dismissNoticeClicked(downloadId: Long) {
        activityLogTracker.trackDownloadBackupDismissButtonClicked(rewindableOnly)
        reloadEvents(backupDownloadEvent = BackupDownloadEvent(displayNotice = false, displayProgress = false))
        viewModelScope.launch { postDismissBackupDownloadUseCase.dismissBackupDownload(downloadId, site) }
    }

    fun dateRangePickerClicked() {
        activityLogTracker.trackDateRangeFilterButtonClicked(rewindableOnly)
        _showDateRangePicker.value = ShowDateRangePicker(initialSelection = currentDateRangeFilter)
    }

    fun onDateRangeSelected(dateRange: DateRange?) {
        val adjustedDateRange = dateRange?.let {
            // adjust time of the end of the date range to 23:59:59
            Pair(dateRange.first, dateRange.second?.let { it + DAY_IN_MILLIS - ONE_SECOND_IN_MILLIS })
        }
        activityLogTracker.trackDateRangeFilterSelected(dateRange, rewindableOnly)
        currentDateRangeFilter = adjustedDateRange
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onClearDateRangeFilterClicked() {
        activityLogTracker.trackDateRangeFilterCleared(rewindableOnly)
        currentDateRangeFilter = null
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onActivityTypeFilterClicked() {
        activityLogTracker.trackActivityTypeFilterButtonClicked()
        _showActivityTypeFilterDialog.value = ShowActivityTypePicker(
                RemoteId(site.siteId),
                currentActivityTypeFilter.map { it.key },
                currentDateRangeFilter
        )
    }

    fun onActivityTypesSelected(selectedTypes: List<ActivityTypeModel>) {
        activityLogTracker.trackActivityTypeFilterSelected(selectedTypes)
        currentActivityTypeFilter = selectedTypes
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onClearActivityTypeFilterClicked() {
        activityLogTracker.trackActivityTypeFilterCleared()
        currentActivityTypeFilter = listOf()
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onQueryRestoreStatus(rewindId: String, restoreId: Long) {
        queryRestoreStatus(restoreId)
        showRestoreStartedMessage(rewindId)
    }

    private fun queryRestoreStatus(restoreId: Long? = null) {
        restoreStatusJob?.cancel()
        restoreStatusJob = viewModelScope.launch {
            getRestoreStatusUseCase.getRestoreStatus(site, restoreId)
                    .collect { handleRestoreStatus(it) }
        }
    }

    private fun handleRestoreStatus(state: RestoreRequestState) {
        when (state) {
            is RestoreRequestState.Multisite -> handleRestoreStatusForMultisite()
            is RestoreRequestState.Progress -> handleRestoreStatusForProgress(state)
            is RestoreRequestState.Complete -> handleRestoreStatusForComplete(state)
            else -> Unit // Do nothing
        }
    }

    private fun handleRestoreStatusForMultisite() {
        reloadEvents(
                restoreEvent = RestoreEvent(
                        displayProgress = false,
                        isRestoreHidden = true
                )
        )
    }

    private fun handleRestoreStatusForProgress(state: RestoreRequestState.Progress) {
        if (!isRestoreProgressItemShown) {
            reloadEvents(
                    restoreEvent = RestoreEvent(
                            displayProgress = true,
                            isCompleted = false,
                            rewindId = state.rewindId,
                            published = state.published
                    )
            )
        }
    }

    private fun handleRestoreStatusForComplete(state: RestoreRequestState.Complete) {
        if (isRestoreProgressItemShown) {
            requestEventsUpdate(
                    loadMore = false,
                    restoreEvent = RestoreEvent(
                            displayProgress = false,
                            isCompleted = true,
                            rewindId = state.rewindId,
                            published = state.published
                    )
            )
        }
    }

    private fun showRestoreStartedMessage(rewindId: String) {
        activityLogStore.getActivityLogItemByRewindId(rewindId)?.published?.let {
            _showSnackbarMessage.value = resourceProvider.getString(
                    R.string.activity_log_rewind_started_snackbar_message,
                    it.toFormattedDateString(),
                    it.toFormattedTimeString()
            )
        }
    }

    fun onQueryBackupDownloadStatus(rewindId: String, downloadId: Long, actionState: Int) {
        queryBackupDownloadStatus(downloadId)

        if (actionState == PROGRESS.id) {
            showBackupDownloadStartedMessage(rewindId)
        } else {
            showBackupDownloadFinishedMessage(rewindId)
        }
    }

    private fun queryBackupDownloadStatus(downloadId: Long? = null) {
        backupDownloadStatusJob?.cancel()
        backupDownloadStatusJob = viewModelScope.launch {
            getBackupDownloadStatusUseCase.getBackupDownloadStatus(site, downloadId)
                    .collect { state -> handleBackupDownloadStatus(state) }
        }
    }

    private fun handleBackupDownloadStatus(state: BackupDownloadRequestState) {
        when (state) {
            is BackupDownloadRequestState.Progress -> handleBackupDownloadStatusForProgress(state)
            is BackupDownloadRequestState.Complete -> handleBackupDownloadStatusForComplete(state)
            else -> Unit // Do nothing
        }
    }

    private fun handleBackupDownloadStatusForProgress(state: BackupDownloadRequestState.Progress) {
        if (!isBackupDownloadProgressItemShown) {
            reloadEvents(
                    backupDownloadEvent = BackupDownloadEvent(
                            displayProgress = true,
                            displayNotice = false,
                            isCompleted = false,
                            rewindId = state.rewindId,
                            published = state.published
                    )
            )
        }
    }

    private fun handleBackupDownloadStatusForComplete(state: BackupDownloadRequestState.Complete) {
        val backupDownloadEvent = BackupDownloadEvent(
                displayProgress = false,
                displayNotice = state.isValid,
                isCompleted = true,
                rewindId = state.rewindId,
                published = state.published,
                url = state.url,
                validUntil = state.validUntil,
                downloadId = state.downloadId
        )

        if (isBackupDownloadProgressItemShown) {
            requestEventsUpdate(loadMore = false, backupDownloadEvent = backupDownloadEvent)
        } else {
            reloadEvents(backupDownloadEvent = backupDownloadEvent)
        }
    }

    private fun showBackupDownloadStartedMessage(rewindId: String) {
        activityLogStore.getActivityLogItemByRewindId(rewindId)?.published?.let {
            _showSnackbarMessage.value = resourceProvider.getString(
                    R.string.activity_log_backup_started_snackbar_message,
                    it.toFormattedDateString(),
                    it.toFormattedTimeString()
            )
        }
    }

    data class ShowDateRangePicker(val initialSelection: DateRange?)
    data class ShowActivityTypePicker(
        val siteId: RemoteId,
        val initialSelection: List<String>,
        val dateRange: DateRange?
    )

    data class RestoreEvent(
        val displayProgress: Boolean,
        val isRestoreHidden: Boolean = false,
        val isCompleted: Boolean = false,
        val rewindId: String? = null,
        val published: Date? = null
    )

    data class BackupDownloadEvent(
        val displayProgress: Boolean,
        val displayNotice: Boolean,
        val isCompleted: Boolean = false,
        val rewindId: String? = null,
        val published: Date? = null,
        val downloadId: Long? = null,
        val url: String? = null,
        val validUntil: Date? = null
    )

    sealed class FiltersUiState(val visibility: Boolean) {
        object FiltersHidden : FiltersUiState(visibility = false)

        data class FiltersShown(
            val dateRangeLabel: UiString,
            val dateRangeLabelContentDescription: UiString,
            val activityTypeLabel: UiString,
            val activityTypeLabelContentDescription: UiString,
            val onClearDateRangeFilterClicked: (() -> Unit)?,
            val onClearActivityTypeFilterClicked: (() -> Unit)?
        ) : FiltersUiState(visibility = true)
    }

    sealed class EmptyUiState {
        abstract val emptyScreenTitle: UiString
        abstract val emptyScreenSubtitle: UiString

        object ActivityLog {
            object EmptyFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.activity_log_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.activity_log_empty_subtitle)
            }

            object ActiveFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.activity_log_active_filter_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.activity_log_active_filter_empty_subtitle)
            }
        }

        object Backup {
            object EmptyFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.backup_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.backup_empty_subtitle)
            }

            object ActiveFilters : EmptyUiState() {
                override val emptyScreenTitle = UiStringRes(R.string.backup_active_filter_empty_title)
                override val emptyScreenSubtitle = UiStringRes(R.string.backup_active_filter_empty_subtitle)
            }
        }
    }
}
