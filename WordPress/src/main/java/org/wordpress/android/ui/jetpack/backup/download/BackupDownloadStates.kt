package org.wordpress.android.ui.jetpack.backup.download

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.ui.jetpack.backup.download.StateType.COMPLETE
import org.wordpress.android.ui.jetpack.backup.download.StateType.DETAILS
import org.wordpress.android.ui.jetpack.backup.download.StateType.ERROR
import org.wordpress.android.ui.jetpack.backup.download.StateType.PROGRESS
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.CompleteToolbarState
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.ErrorToolbarState
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import java.util.Date

abstract class BackupDownloadUiState(open val type: StateType) {
    abstract val items: List<JetpackListItemState>
    abstract val toolbarState: ToolbarState

    data class ErrorState(
        val errorType: BackupDownloadErrorTypes,
        override val items: List<JetpackListItemState>
    ) : BackupDownloadUiState(ERROR) {
        override val toolbarState: ToolbarState = ErrorToolbarState
    }

    sealed class ContentState(override val type: StateType) : BackupDownloadUiState(type) {
        data class DetailsState(
            val activityLogModel: ActivityLogModel,
            override val items: List<JetpackListItemState>,
            override val type: StateType
        ) : ContentState(DETAILS) {
            override val toolbarState: ToolbarState = DetailsToolbarState
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
    PROGRESS(1),
    COMPLETE(2),
    ERROR(3)
}

sealed class ToolbarState {
    abstract val title: Int

    @DrawableRes
    val icon: Int = R.drawable.ic_close_24px

    object DetailsToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.backup_download_details_page_title
    }

    object ProgressToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.backup_download_progress_page_title
    }

    object CompleteToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.backup_download_complete_page_title
    }

    object ErrorToolbarState : ToolbarState() {
        @StringRes
        override val title: Int = R.string.backup_download_complete_failed_title
    }
}

sealed class BackupDownloadRequestState {
    data class Success(
        val requestRewindId: String,
        val rewindId: String,
        val downloadId: Long?
    ) : BackupDownloadRequestState()

    data class Progress(
        val rewindId: String,
        val progress: Int?,
        val published: Date? = null
    ) : BackupDownloadRequestState()

    data class Complete(
        val rewindId: String,
        val downloadId: Long,
        val url: String?,
        val published: Date? = null,
        val validUntil: Date? = null,
        val isValid: Boolean = false
    ) : BackupDownloadRequestState()

    object Empty : BackupDownloadRequestState()
    sealed class Failure : BackupDownloadRequestState() {
        object NetworkUnavailable : Failure()
        object RemoteRequestFailure : Failure()
        object OtherRequestRunning : Failure()
    }
}
