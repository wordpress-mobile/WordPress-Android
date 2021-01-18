package org.wordpress.android.ui.jetpack.restore

sealed class RestoreRequestState {
    data class Success(
        val requestRewindId: String,
        val rewindId: String,
        val restoreId: Long?
    ) : RestoreRequestState()
    data class Progress(val rewindId: String, val progress: Int?) : RestoreRequestState()
    data class Complete(val rewindId: String, val restoreId: Long) :
            RestoreRequestState()
    sealed class Failure : RestoreRequestState() {
        object NetworkUnavailable : Failure()
        object RemoteRequestFailure : Failure()
        object OtherRequestRunning : Failure()
    }
}
