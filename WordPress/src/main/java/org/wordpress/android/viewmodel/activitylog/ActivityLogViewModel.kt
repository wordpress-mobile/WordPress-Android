package org.wordpress.android.viewmodel.activitylog

import androidx.core.util.Pair
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP_DAILY
import org.wordpress.android.fluxc.model.JetpackCapability.BACKUP_REALTIME
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status
import org.wordpress.android.fluxc.model.activity.RewindStatusModel.Rewind.Status.RUNNING
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.OnActivityLogFetched
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.ShowBackupDownload
import org.wordpress.android.ui.activitylog.ActivityLogNavigationEvents.ShowRestore
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Footer
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Header
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.Loading
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.SecondaryAction.DOWNLOAD_BACKUP
import org.wordpress.android.ui.activitylog.list.ActivityLogListItem.SecondaryAction.RESTORE
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.jetpack.rewind.RewindStatusService
import org.wordpress.android.ui.jetpack.rewind.RewindStatusService.RewindProgress
import org.wordpress.android.ui.stats.refresh.utils.DateUtils
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BackupFeatureConfig
import org.wordpress.android.util.analytics.ActivityLogTracker
import org.wordpress.android.util.config.ActivityLogFiltersFeatureConfig
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.CAN_LOAD_MORE
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.DONE
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.ActivityLogListStatus.LOADING_MORE
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersHidden
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel.FiltersUiState.FiltersShown
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

const val ACTIVITY_LOG_REWINDABLE_ONLY_KEY = "activity_log_rewindable_only"

private const val DAY_IN_MILLIS = 1000 * 60 * 60 * 24
private const val ONE_SECOND_IN_MILLIS = 1000
private const val TIMEZONE_GMT_0 = "GMT+0"

typealias DateRange = Pair<Long, Long>

class ActivityLogViewModel @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    private val rewindStatusService: RewindStatusService,
    private val resourceProvider: ResourceProvider,
    private val activityLogFiltersFeatureConfig: ActivityLogFiltersFeatureConfig,
    private val backupFeatureConfig: BackupFeatureConfig,
    private val dateUtils: DateUtils,
    private val activityLogTracker: ActivityLogTracker,
    private val jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase,
    @param:Named(UI_THREAD) private val uiDispatcher: CoroutineDispatcher
) : ScopedViewModel(uiDispatcher) {
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

    private val _emptyUiState = MutableLiveData<EmptyUiState>(EmptyUiState.EmptyFilters)
    val emptyUiState: LiveData<EmptyUiState> = _emptyUiState

    private val _showRewindDialog = SingleLiveEvent<ActivityLogListItem>()
    val showRewindDialog: LiveData<ActivityLogListItem>
        get() = _showRewindDialog

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

    private val isRewindProgressItemShown: Boolean
        get() = _events.value?.containsProgressItem() == true

    private val isDone: Boolean
        get() = eventListStatus.value == DONE

    private var fetchActivitiesJob: Job? = null

    private var areActionsEnabled: Boolean = true

    private var lastRewindActivityId: String? = null
    private var lastRewindStatus: Status? = null

    private var currentDateRangeFilter: DateRange? = null
    private var currentActivityTypeFilter: List<ActivityTypeModel> = listOf()

    private val rewindProgressObserver = Observer<RewindProgress> {
        if (it?.activityLogItem?.activityID != lastRewindActivityId || it?.status != lastRewindStatus) {
            lastRewindActivityId = it?.activityLogItem?.activityID
            updateRewindState(it?.status)
        }
    }

    private val rewindAvailableObserver = Observer<Boolean> { isRewindAvailable ->
        if (areActionsEnabled != isRewindAvailable) {
            isRewindAvailable?.let {
                reloadEvents(!isRewindAvailable)
            }
        }
    }

    lateinit var site: SiteModel
    var rewindableOnly: Boolean = false

    fun start(site: SiteModel, rewindableOnly: Boolean) {
        if (isStarted) {
            return
        }
        isStarted = true

        this.site = site
        this.rewindableOnly = rewindableOnly

        rewindStatusService.start(site)
        rewindStatusService.rewindProgress.observeForever(rewindProgressObserver)
        rewindStatusService.rewindAvailable.observeForever(rewindAvailableObserver)

        activityLogStore.getRewindStatusForSite(site)

        reloadEvents(done = true)
        requestEventsUpdate(false)

        showFiltersIfSupported()
    }

    private fun showFiltersIfSupported() {
        when {
            !activityLogFiltersFeatureConfig.isEnabled() -> return
            !site.hasFreePlan -> refreshFiltersUiState()
            else -> {
                launch {
                    jetpackCapabilitiesUseCase.getOrFetchJetpackCapabilities(site.siteId)
                            .find { it == BACKUP || it == BACKUP_DAILY || it == BACKUP_REALTIME }
                            ?.let {
                                refreshFiltersUiState()
                            }
                }
            }
        }
    }

    override fun onCleared() {
        rewindStatusService.rewindAvailable.removeObserver(rewindAvailableObserver)
        rewindStatusService.rewindProgress.removeObserver(rewindProgressObserver)
        rewindStatusService.stop()
        if (currentDateRangeFilter != null || currentActivityTypeFilter.isNotEmpty()) {
            /**
             * Clear cache when filters are not empty. Filters are not retained across sessions, therefore the data is
             * not relevant when the screen is accessed next time.
             */
            activityLogStore.clearActivityLogCache(site)
        }

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
        if (rewindableOnly) {
            if (currentDateRangeFilter != null) {
                _emptyUiState.value = EmptyUiState.Backup.ActiveFilters
            } else {
                _emptyUiState.value = EmptyUiState.Backup.EmptyFilters
            }
        } else {
            if (currentDateRangeFilter != null || currentActivityTypeFilter.isNotEmpty()) {
                _emptyUiState.value = EmptyUiState.ActiveFilters
            } else {
                _emptyUiState.value = EmptyUiState.EmptyFilters
            }
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
                        UiStringText("${it[0].name} (${it[0].count})"),
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

    fun onPullToRefresh() {
        requestEventsUpdate(false)
    }

    fun onItemClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            _showItemDetail.value = item
        }
    }

    // todo: annmarie - Remove once the feature exclusively uses the more menu
    fun onActionButtonClicked(item: ActivityLogListItem) {
        if (item is ActivityLogListItem.Event) {
            _showRewindDialog.value = item
        }
    }

    fun onSecondaryActionClicked(
        secondaryAction: ActivityLogListItem.SecondaryAction,
        item: ActivityLogListItem
    ): Boolean {
        if (item is ActivityLogListItem.Event) {
            val navigationEvent = when (secondaryAction) {
                RESTORE -> {
                    ShowRestore(item)
                }
                DOWNLOAD_BACKUP -> {
                    ShowBackupDownload(item)
                }
            }
            _navigationEvents.value = Event(navigationEvent)
        }
        return true
    }

    fun dateRangePickerClicked() {
        activityLogTracker.trackDateRangeFilterButtonClicked()
        _showDateRangePicker.value = ShowDateRangePicker(initialSelection = currentDateRangeFilter)
    }

    fun onDateRangeSelected(dateRange: DateRange?) {
        val adjustedDateRange = dateRange?.let {
            // adjust time of the end of the date range to 23:59:59
            Pair(dateRange.first, dateRange.second?.let { it + DAY_IN_MILLIS - ONE_SECOND_IN_MILLIS })
        }
        activityLogTracker.trackDateRangeFilterSelected(dateRange)
        currentDateRangeFilter = adjustedDateRange
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onClearDateRangeFilterClicked() {
        activityLogTracker.trackDateRangeFilterCleared()
        currentDateRangeFilter = null
        refreshFiltersUiState()
        requestEventsUpdate(false)
    }

    fun onActivityTypeFilterClicked() {
        activityLogTracker.trackActivityTypeFilterButtonClicked()
        _showActivityTypeFilterDialog.value = ShowActivityTypePicker(
                RemoteId(site.siteId),
                currentActivityTypeFilter.mapNotNull { it.key },
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

    fun onRewindConfirmed(rewindId: String) {
        rewindStatusService.rewind(rewindId, site)
        showRewindStartedMessage()
    }

    fun onScrolledToBottom() {
        requestEventsUpdate(true)
    }

    private fun updateRewindState(status: Status?) {
        lastRewindStatus = status
        if (status == RUNNING && !isRewindProgressItemShown) {
            reloadEvents(disableActions = true, displayProgressItem = true)
        } else if (status != RUNNING && isRewindProgressItemShown) {
            requestEventsUpdate(false)
        }
    }

    private fun reloadEvents(
        disableActions: Boolean = areActionsEnabled,
        displayProgressItem: Boolean = isRewindProgressItemShown,
        done: Boolean = isDone
    ) {
        val eventList = activityLogStore.getActivityLogForSite(
                site = site,
                ascending = false,
                rewindableOnly = rewindableOnly
        )
        val items = mutableListOf<ActivityLogListItem>()
        var moveToTop = false
        val rewindFinished = isRewindProgressItemShown && !displayProgressItem
        if (displayProgressItem) {
            val activityLogModel = rewindStatusService.rewindProgress.value?.activityLogItem
            items.add(Header(resourceProvider.getString(R.string.now)))
            items.add(getRewindProgressItem(activityLogModel))
            moveToTop = eventListStatus.value != LOADING_MORE
        }
        eventList.forEach { model ->
            val currentItem = ActivityLogListItem.Event(model, disableActions, backupFeatureConfig.isEnabled())
            val lastItem = items.lastOrNull() as? ActivityLogListItem.Event
            if (lastItem == null || lastItem.formattedDate != currentItem.formattedDate) {
                items.add(Header(currentItem.formattedDate))
            }
            items.add(currentItem)
        }
        if (eventList.isNotEmpty() && !done) {
            items.add(Loading)
        }
        if (eventList.isNotEmpty() && site.hasFreePlan && done) {
            items.add(Footer)
        }
        areActionsEnabled = !disableActions

        _events.value = items
        if (moveToTop) {
            _moveToTop.call()
        }
        if (rewindFinished) {
            showRewindFinishedMessage()
        }
    }

    private fun List<ActivityLogListItem>.containsProgressItem(): Boolean {
        return this.find { it is ActivityLogListItem.Progress } != null
    }

    private fun getRewindProgressItem(activityLogModel: ActivityLogModel?): ActivityLogListItem.Progress {
        return activityLogModel?.let {
            val rewoundEvent = ActivityLogListItem.Event(
                    model = it,
                    backupFeatureEnabled = backupFeatureConfig.isEnabled()
            )
            ActivityLogListItem.Progress(
                    resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                    resourceProvider.getString(
                            R.string.activity_log_currently_restoring_message,
                            rewoundEvent.formattedDate, rewoundEvent.formattedTime
                    )
            )
        } ?: ActivityLogListItem.Progress(
                resourceProvider.getString(R.string.activity_log_currently_restoring_title),
                resourceProvider.getString(R.string.activity_log_currently_restoring_message_no_dates)
        )
    }

    private fun requestEventsUpdate(loadMore: Boolean) {
        val isLoadingMore = fetchActivitiesJob != null && _eventListStatus.value == ActivityLogListStatus.LOADING_MORE
        val canLoadMore = _eventListStatus.value == CAN_LOAD_MORE
        if (loadMore && (isLoadingMore || !canLoadMore)) {
            // Ignore loadMore request when already loading more items or there are no more items to load
            return
        }
        fetchActivitiesJob?.cancel()
        val newStatus = if (loadMore) LOADING_MORE else ActivityLogListStatus.FETCHING
        _eventListStatus.value = newStatus
        val payload = ActivityLogStore.FetchActivityLogPayload(
                site,
                loadMore,
                currentDateRangeFilter?.first?.let { Date(it) },
                currentDateRangeFilter?.second?.let { Date(it) },
                currentActivityTypeFilter.mapNotNull { it.key }
        )
        fetchActivitiesJob = launch {
            val result = activityLogStore.fetchActivities(payload)
            if (isActive) {
                onActivityLogFetched(result, loadMore)
                fetchActivitiesJob = null
            }
        }
    }

    private fun showRewindStartedMessage() {
        rewindStatusService.rewindingActivity?.let {
            val event = ActivityLogListItem.Event(model = it, backupFeatureEnabled = backupFeatureConfig.isEnabled())
            _showSnackbarMessage.value = resourceProvider.getString(
                    R.string.activity_log_rewind_started_snackbar_message,
                    event.formattedDate,
                    event.formattedTime
            )
        }
    }

    private fun showRewindFinishedMessage() {
        val item = rewindStatusService.rewindingActivity
        if (item != null) {
            val event = ActivityLogListItem.Event(model = item, backupFeatureEnabled = backupFeatureConfig.isEnabled())
            _showSnackbarMessage.value =
                    resourceProvider.getString(
                            R.string.activity_log_rewind_finished_snackbar_message,
                            event.formattedDate,
                            event.formattedTime
                    )
        } else {
            _showSnackbarMessage.value =
                    resourceProvider.getString(R.string.activity_log_rewind_finished_snackbar_message_no_dates)
        }
    }

    private fun onActivityLogFetched(event: OnActivityLogFetched, loadingMore: Boolean) {
        if (event.isError) {
            _eventListStatus.value = ActivityLogListStatus.ERROR
            AppLog.e(AppLog.T.ACTIVITY_LOG, "An error occurred while fetching the Activity log events")
            return
        }

        if (event.rowsAffected > 0) {
            reloadEvents(
                    !rewindStatusService.isRewindAvailable,
                    rewindStatusService.isRewindInProgress,
                    !event.canLoadMore
            )
            if (!loadingMore) {
                moveToTop.call()
            }
            rewindStatusService.requestStatusUpdate()
        }

        if (event.canLoadMore) {
            _eventListStatus.value = ActivityLogListStatus.CAN_LOAD_MORE
        } else {
            _eventListStatus.value = DONE
        }
    }

    data class ShowDateRangePicker(val initialSelection: DateRange?)
    data class ShowActivityTypePicker(
        val siteId: RemoteId,
        val initialSelection: List<String>,
        val dateRange: DateRange?
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

        object EmptyFilters : EmptyUiState() {
            override val emptyScreenTitle = UiStringRes(R.string.activity_log_empty_title)
            override val emptyScreenSubtitle = UiStringRes(R.string.activity_log_empty_subtitle)
        }

        object ActiveFilters : EmptyUiState() {
            override val emptyScreenTitle = UiStringRes(R.string.activity_log_active_filter_empty_title)
            override val emptyScreenSubtitle = UiStringRes(R.string.activity_log_active_filter_empty_subtitle)
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
