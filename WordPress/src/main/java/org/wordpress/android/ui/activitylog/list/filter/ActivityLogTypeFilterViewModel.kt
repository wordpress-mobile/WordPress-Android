package org.wordpress.android.ui.activitylog.list.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.activitylog.list.filter.ActivityLogTypeFilterViewModel.UiState.FullscreenLoading
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ActivityLogTypeFilterViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false

    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        if (isStarted) return
        isStarted = true

        _uiState.value = FullscreenLoading
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
}
