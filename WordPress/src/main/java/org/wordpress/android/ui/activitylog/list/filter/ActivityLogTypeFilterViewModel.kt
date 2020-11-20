package org.wordpress.android.ui.activitylog.list.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.immutableListOf
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.ListItemUiState.SectionHeader
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Action
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.Content
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.FullscreenLoading
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ActivityLogTypeFilterViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        if (isStarted) return
        isStarted = true

        _uiState.value = FullscreenLoading
        fetchAvailableActivityTypes()
    }

    private fun fetchAvailableActivityTypes() {
        launch {
            // TODO malinjir initiate the fetch
            onActivityTypesFetched(immutableListOf(DummyActivityType, DummyActivityType, DummyActivityType))
        }
    }

    private suspend fun onActivityTypesFetched(activityTypes: List<DummyActivityType>) {
        _uiState.value = buildContentUiState(activityTypes)
    }

    private suspend fun buildContentUiState(activityTypes: List<DummyActivityType>): Content {
        return withContext(bgDispatcher) {
            // TODO malinjir replace the hardcoded header title
            val headerListItem = SectionHeader(UiStringText("Test"))
            // TODO malinjir replace "it.toString()" with activity type name
            val activityTypeListItems: List<ListItemUiState.ActivityType> = activityTypes
                    .map {
                        ListItemUiState.ActivityType(title = UiStringText(it.toString()))
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

    private fun onApplyClicked() {
        // TODO malinjir save and dismiss
    }

    private fun onClearClicked() {
        (_uiState.value as? Content)?.let { it ->
            _uiState.value = it.copy(items = uncheckAllActivityTypeItems(it))
        }
    }

    private fun uncheckAllActivityTypeItems(it: Content): List<ListItemUiState> {
        return it.items.map { item ->
            if (item is ListItemUiState.ActivityType) {
                item.copy(checked = false)
            } else {
                item
            }
        }
    }

    sealed class UiState {
        open val loadingVisibility = false
        open val items: List<ListItemUiState>? = null
        open val primaryAction: Action? = null
        open val secondaryAction: Action? = null

        object FullscreenLoading : UiState() {
            override val loadingVisibility: Boolean = true
        }

        data class Content(
            override val items: List<ListItemUiState>,
            override val primaryAction: Action,
            override val secondaryAction: Action
        ) : UiState()

        data class Action(val label: UiString) {
            var action: (() -> Unit)? = null
        }
    }

    sealed class ListItemUiState {
        data class SectionHeader(
            val title: UiString
        ) : ListItemUiState()

        data class ActivityType(
            val title: UiString,
            val checked: Boolean = false
        ) : ListItemUiState()
    }

    object DummyActivityType
}
