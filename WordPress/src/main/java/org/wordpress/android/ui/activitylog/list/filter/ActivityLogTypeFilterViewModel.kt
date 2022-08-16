package org.wordpress.android.ui.activitylog.list.filter

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.fluxc.store.ActivityLogStore
import org.wordpress.android.fluxc.store.ActivityLogStore.FetchActivityTypesPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Error
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.FullscreenLoading
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel
import org.wordpress.android.viewmodel.activitylog.DateRange
import java.util.Date
import javax.inject.Inject
import javax.inject.Named

class ActivityLogTypeFilterViewModel @Inject constructor(
    private val activityLogStore: ActivityLogStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var remoteSiteId: RemoteId
    private lateinit var parentViewModel: ActivityLogViewModel
    private lateinit var initialSelection: List<String>
    private var dateRange: DateRange? = null

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    private val _dismissDialog = MutableLiveData<Event<Unit>>()
    val dismissDialog: LiveData<Event<Unit>> = _dismissDialog

    fun start(
        remoteSiteId: RemoteId,
        parentViewModel: ActivityLogViewModel,
        dateRange: DateRange?,
        initialSelection: List<String>
    ) {
        if (isStarted) return
        isStarted = true
        this.remoteSiteId = remoteSiteId
        this.parentViewModel = parentViewModel
        this.initialSelection = initialSelection
        this.dateRange = dateRange
        fetchAvailableActivityTypes()
    }

    private fun fetchAvailableActivityTypes() {
        launch {
            _uiState.value = FullscreenLoading
            val response = activityLogStore.fetchActivityTypes(
                    FetchActivityTypesPayload(
                            remoteSiteId.value,
                            dateRange?.first?.let { Date(it) },
                            dateRange?.second?.let { Date(it) }
                    )
            )
            if (response.isError) {
                _uiState.value = buildErrorUiState()
            } else {
                _uiState.value = buildContentUiState(response.activityTypeModels)
            }
        }
    }

    private fun buildErrorUiState() =
            Error.ConnectionError(Action(UiStringRes(R.string.retry)).apply { action = ::onRetryClicked })

    private suspend fun buildContentUiState(activityTypes: List<ActivityTypeModel>): UiState {
        return withContext(bgDispatcher) {
            if (activityTypes.isEmpty()) { return@withContext Error.NoActivitiesError }

            val headerListItem = ListItemUiState.SectionHeader(
                    UiStringRes(R.string.activity_log_activity_type_filter_header)
            )
            val activityTypeListItems: List<ListItemUiState.ActivityType> = activityTypes
                    .map {
                        ListItemUiState.ActivityType(
                                id = it.key,
                                title = UiStringText("${it.name} (${it.count})"),
                                onClick = { onItemClicked(it.key) },
                                checked = initialSelection.contains(it.key)
                        )
                    }
            Content(
                    listOf(headerListItem) + activityTypeListItems,
                    primaryAction = Action(label = UiStringRes(R.string.activity_log_activity_type_filter_apply))
                            .apply { action = { onApplyClicked(activityTypes) } },
                    secondaryAction = Action(label = UiStringRes(R.string.activity_log_activity_type_filter_clear))
                            .apply { action = ::onClearClicked }
            )
        }
    }

    private fun onItemClicked(itemId: String) {
        (_uiState.value as? Content)?.let { content ->
            val updatedList = content.items.map { itemUiState ->
                if (itemUiState is ListItemUiState.ActivityType && itemUiState.id == itemId) {
                    itemUiState.copy(checked = !itemUiState.checked)
                } else {
                    itemUiState
                }
            }
            _uiState.postValue(content.copy(items = updatedList))
        }
    }

    private fun onApplyClicked(activityTypes: List<ActivityTypeModel>) {
        val selectedModels = getSelectedActivityTypeIds().mapNotNull { key ->
            activityTypes.find { model -> model.key == key }
        }
        parentViewModel.onActivityTypesSelected(selectedModels)
        _dismissDialog.value = Event(Unit)
    }

    private fun onRetryClicked() {
        fetchAvailableActivityTypes()
    }

    private fun onClearClicked() {
        (_uiState.value as? Content)?.let {
            _uiState.value = it.copy(items = getAllActivityTypeItemsUnchecked(it.items))
        }
    }

    private fun getAllActivityTypeItemsUnchecked(listItemUiStates: List<ListItemUiState>): List<ListItemUiState> =
            listItemUiStates.map { item ->
                if (item is ListItemUiState.ActivityType) {
                    item.copy(checked = false)
                } else {
                    item
                }
            }

    private fun getSelectedActivityTypeIds(): List<String> =
            (_uiState.value as Content).items
                    .filterIsInstance(ListItemUiState.ActivityType::class.java)
                    .filter { it.checked }
                    .map { it.id }

    sealed class UiState {
        open val contentVisibility = false
        open val loadingVisibility = false
        open val errorVisibility = false

        object FullscreenLoading : UiState() {
            override val loadingVisibility: Boolean = true
            val loadingText: UiString = UiStringRes(R.string.loading)
        }

        sealed class Error : UiState() {
            override val errorVisibility = true
            abstract val image: Int
            abstract val title: UiString
            abstract val subtitle: UiString
            open val buttonText: UiString? = null
            open val retryAction: Action? = null

            data class ConnectionError(override val retryAction: Action) : Error() {
                @DrawableRes override val image = R.drawable.img_illustration_cloud_off_152dp
                override val title: UiString = UiStringRes(R.string.activity_log_activity_type_error_title)
                override val subtitle: UiString = UiStringRes(R.string.activity_log_activity_type_error_subtitle)
                override val buttonText: UiString = UiStringRes(R.string.retry)
            }

            object NoActivitiesError : Error() {
                @DrawableRes override val image = R.drawable.img_illustration_empty_results_216dp
                override val title = UiStringRes(R.string.activity_log_activity_type_empty_title)
                override val subtitle = UiStringRes(R.string.activity_log_activity_type_empty_subtitle)
            }
        }

        data class Content(
            val items: List<ListItemUiState>,
            val primaryAction: Action,
            val secondaryAction: Action
        ) : UiState() {
            override val contentVisibility = true
        }
    }

    sealed class ListItemUiState {
        data class SectionHeader(
            val title: UiString
        ) : ListItemUiState()

        data class ActivityType(
            val id: String,
            val title: UiString,
            val checked: Boolean = false,
            val onClick: (() -> Unit)
        ) : ListItemUiState()
    }

    @Suppress("DataClassShouldBeImmutable")
    data class Action(val label: UiString) {
        lateinit var action: (() -> Unit)
    }
}
