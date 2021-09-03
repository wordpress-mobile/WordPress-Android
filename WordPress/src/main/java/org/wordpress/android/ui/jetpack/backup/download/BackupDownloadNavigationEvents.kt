package org.wordpress.android.ui.jetpack.backup.download

sealed class BackupDownloadNavigationEvents {
    data class ShareLink(val url: String) : BackupDownloadNavigationEvents()
    data class DownloadFile(val url: String) : BackupDownloadNavigationEvents()
}
