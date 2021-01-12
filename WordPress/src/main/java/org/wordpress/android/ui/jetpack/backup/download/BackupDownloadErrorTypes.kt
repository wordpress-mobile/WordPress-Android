package org.wordpress.android.ui.jetpack.backup.download

enum class BackupDownloadErrorTypes(val id: Int) {
    NetworkUnavailable(0), RemoteRequestFailure(1), GenericFailure(2);
}
