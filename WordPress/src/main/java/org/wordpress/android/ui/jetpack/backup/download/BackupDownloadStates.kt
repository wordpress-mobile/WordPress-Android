package org.wordpress.android.ui.jetpack.backup.download

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.ui.jetpack.backup.download.StateType.COMPLETE
import org.wordpress.android.ui.jetpack.backup.download.StateType.DETAILS
import org.wordpress.android.ui.jetpack.backup.download.StateType.ERROR
import org.wordpress.android.ui.jetpack.backup.download.StateType.PROGRESS
import org.wordpress.android.ui.jetpack.common.JetpackListItemState

abstract class BackupDownloadUiState(open val type: StateType) {
    abstract val items: List<JetpackListItemState>
    abstract val toolbarState: ToolbarState

    data class ErrorState(
        val errorType: BackupDownloadErrorTypes,
        override val items: List<JetpackListItemState>,
        override val toolbarState: ToolbarState
    ) : BackupDownloadUiState(ERROR)

    sealed class ContentState(override val type: StateType) : BackupDownloadUiState(type) {
        data class DetailsState(
            val activityLogModel: ActivityLogModel,
            override val items: List<JetpackListItemState>,
            override val toolbarState: ToolbarState,
            override val type: StateType
        ) : ContentState(DETAILS)

        data class ProgressState(
            override val items: List<JetpackListItemState>,
            override val toolbarState: ToolbarState,
            override val type: StateType
        ) : ContentState(PROGRESS)

        data class CompleteState(
            override val items: List<JetpackListItemState>,
            override val toolbarState: ToolbarState,
            override val type: StateType
        ) : ContentState(COMPLETE)
    }
}

enum class StateType(val id: Int) {
    DETAILS(0),
    PROGRESS(1),
    COMPLETE(2),
    ERROR(3)
}

sealed class ToolbarState {
    abstract val title: Int
    abstract val icon: Int

    data class DetailsToolbarState(
        @StringRes override val title: Int = R.string.backup_download_details_page_title,
        @DrawableRes override val icon: Int = R.drawable.ic_arrow_back
    ) : ToolbarState()

    data class ProgressToolbarState(
        @StringRes override val title: Int = R.string.backup_download_progress_page_title,
        @DrawableRes override val icon: Int = R.drawable.ic_close_24px
    ) : ToolbarState()

    data class CompleteToolbarState(
        @StringRes override val title: Int = R.string.backup_download_complete_page_title,
        @DrawableRes override val icon: Int = R.drawable.ic_close_24px
    ) : ToolbarState()

    data class ErrorToolbarState(
        @StringRes override val title: Int = R.string.backup_download_complete_failed_title,
        @DrawableRes override val icon: Int = R.drawable.ic_close_24px
    ) : ToolbarState()
}

sealed class BackupDownloadRequestState {
    data class Success(
        val requestRewindId: String,
        val rewindId: String,
        val downloadId: Long?
    ) : BackupDownloadRequestState()
    data class Progress(val rewindId: String, val progress: Int?) : BackupDownloadRequestState()
    data class Complete(val rewindId: String, val downloadId: Long, val url: String?) :
            BackupDownloadRequestState()
    sealed class Failure : BackupDownloadRequestState() {
        object NetworkUnavailable : Failure()
        object RemoteRequestFailure : Failure()
        object OtherRequestRunning : Failure()
    }
}
