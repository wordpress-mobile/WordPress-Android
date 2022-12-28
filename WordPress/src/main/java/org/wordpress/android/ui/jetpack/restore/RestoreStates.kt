package org.wordpress.android.ui.jetpack.restore

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.restore.StateType.COMPLETE
import org.wordpress.android.ui.jetpack.restore.StateType.DETAILS
import org.wordpress.android.ui.jetpack.restore.StateType.ERROR
import org.wordpress.android.ui.jetpack.restore.StateType.PROGRESS
import org.wordpress.android.ui.jetpack.restore.StateType.WARNING
import org.wordpress.android.ui.jetpack.restore.ToolbarState.CompleteToolbarState
import org.wordpress.android.ui.jetpack.restore.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.restore.ToolbarState.ErrorToolbarState
import org.wordpress.android.ui.jetpack.restore.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.restore.ToolbarState.WarningToolbarState
import java.util.Date

abstract class RestoreUiState(open val type: StateType) {
    abstract val items: List<JetpackListItemState>
    abstract val toolbarState: ToolbarState

    data class ErrorState(
        val errorType: RestoreErrorTypes,
        override val items: List<JetpackListItemState>
    ) : RestoreUiState(ERROR) {
        override val toolbarState: ToolbarState = ErrorToolbarState
    }

    sealed class ContentState(override val type: StateType) : RestoreUiState(type) {
        data class DetailsState(
            val activityLogModel: ActivityLogModel,
            override val items: List<JetpackListItemState>,
            override val type: StateType
        ) : ContentState(DETAILS) {
            override val toolbarState: ToolbarState = DetailsToolbarState
        }

        data class WarningState(
            override val items: List<JetpackListItemState>,
            override val type: StateType
        ) : ContentState(WARNING) {
            override val toolbarState: ToolbarState = WarningToolbarState
        }

        data class ProgressState(
            override val items: List<JetpackListItemState>,
            override val type: StateType
        ) : ContentState(PROGRESS) {
            override val toolbarState: ToolbarState = ProgressToolbarState
        }

        data class CompleteState(
            override val items: List<JetpackListItemState>,
            override val type: StateType
        ) : ContentState(COMPLETE) {
            override val toolbarState: ToolbarState = CompleteToolbarState
        }
    }
}

enum class StateType(val id: Int) {
    DETAILS(0),
    WARNING(1),
    PROGRESS(2),
    COMPLETE(3),
    ERROR(4)
}

sealed class ToolbarState {
    abstract val title: Int
    @DrawableRes
    val icon: Int = R.drawable.ic_close_24px

    object DetailsToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.restore_details_page_title
    }

    object WarningToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.restore_warning_page_title
    }

    object ProgressToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.restore_progress_page_title
    }

    object CompleteToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.restore_complete_page_title
    }

    object ErrorToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.restore_complete_failed_title
    }
}

sealed class RestoreRequestState {
    data class Success(
        val requestRewindId: String,
        val rewindId: String,
        val restoreId: Long?
    ) : RestoreRequestState()

    data class Progress(
        val rewindId: String,
        val progress: Int?,
        val message: String? = null,
        val currentEntry: String? = null,
        val published: Date? = null
    ) : RestoreRequestState()

    data class Complete(
        val rewindId: String,
        val restoreId: Long,
        val published: Date? = null
    ) : RestoreRequestState()

    object Multisite : RestoreRequestState()

    object Empty : RestoreRequestState()

    data class AwaitingCredentials(val isAwaitingCredentials: Boolean) : RestoreRequestState()

    sealed class Failure : RestoreRequestState() {
        object NetworkUnavailable : Failure()
        object RemoteRequestFailure : Failure()
        object OtherRequestRunning : Failure()
    }
}
