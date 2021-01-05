package org.wordpress.android.ui.jetpack.backup.download

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
