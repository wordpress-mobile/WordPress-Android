package org.wordpress.android.ui.activitylog.list.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.FullscreenLoading
import org.wordpress.android.ui.activitylog.list.filter.DummyActivityTypesProvider.DummyActivityType
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ActivityLogTypeFilterViewModel @Inject constructor(
    private val dummyActivityTypesProvider: DummyActivityTypesProvider,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    private lateinit var remoteSiteId: RemoteId

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start(remoteSiteId: RemoteId) {
        if (isStarted) return
        isStarted = true
        this.remoteSiteId = remoteSiteId

        fetchAvailableActivityTypes()
    }

    private fun fetchAvailableActivityTypes() {
        launch {
            _uiState.value = FullscreenLoading
            val response = dummyActivityTypesProvider.fetchAvailableActivityTypes(remoteSiteId.value)
            if (response.isError) {
                _uiState.value = buildErrorUiState()
            } else {
                _uiState.value = buildContentUiState(response.activityTypes)
            }
        }
    }

    private fun buildErrorUiState() =
            UiState.Error(Action(UiStringRes(R.string.retry)).apply { action = ::onRetryClicked })

    private suspend fun buildContentUiState(activityTypes: List<DummyActivityType>): Content {
        return withContext(bgDispatcher) {
            // TODO malinjir replace the hardcoded header title
            val headerListItem = ListItemUiState.SectionHeader(UiStringText("Test"))
            // TODO malinjir replace "it.toString()" with activity type name
            val activityTypeListItems: List<ListItemUiState.ActivityType> = activityTypes
                    .map {
                        ListItemUiState.ActivityType(title = UiStringText(it.toString()))
                                .apply { onClick = ::onItemClicked }
                    }
            Content(
                    listOf(headerListItem) + activityTypeListItems,
                    primaryAction = Action(label = UiStringRes(R.string.activity_log_activity_type_filter_apply))
                            .apply { action = ::onApplyClicked },
                    secondaryAction = Action(label = UiStringRes(R.string.activity_log_activity_type_filter_clear))
                            .apply { action = ::onClearClicked }
            )
        }
    }

    // TODO malinjir pass an id instead of the UiState
    private fun onItemClicked(activityType: ListItemUiState.ActivityType) {
        (_uiState.value as? Content)?.let { it ->
            val updatedList = it.items.map {
                if (it == activityType) {
                    activityType
                            .copy(checked = !activityType.checked)
                            .apply { onClick = activityType.onClick }
                } else {
                    it
                }
            }
            _uiState.postValue(it.copy(items = updatedList))
        }

    }

    private fun onApplyClicked() {
    }

    private fun onRetryClicked() {
        fetchAvailableActivityTypes()
    }

    private fun onClearClicked() {
        (_uiState.value as? Content)?.let { it ->
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

    sealed class UiState {
        open val contentVisibility = false
        open val loadingVisibility = false
        open val errorVisibility = false

        object FullscreenLoading : UiState() {
            override val loadingVisibility: Boolean = true
            val loadingText: UiString = UiStringRes(R.string.loading)
        }

        data class Error(val retryAction: Action) : UiState() {
            override val errorVisibility = true
            // TODO malinjir replace strings according to design
            val errorTitle: UiString = UiStringRes(R.string.error)
            val errorSubtitle: UiString = UiStringRes(R.string.hpp_retry_error)
            val errorButtonText: UiString = UiStringRes(R.string.retry)
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
            val title: UiString,
            val checked: Boolean = false
        ) : ListItemUiState() {
            lateinit var onClick: ((ActivityType) -> Unit)
        }
    }

    data class Action(val label: UiString) {
        lateinit var action: (() -> Unit)
    }
}
