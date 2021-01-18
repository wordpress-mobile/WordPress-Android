package org.wordpress.android.ui.jetpack.restore

enum class RestoreErrorTypes(val id: Int) {
    NetworkUnavailable(0), RemoteRequestFailure(1), GenericFailure(2), OtherRequestRunning(3);
}
