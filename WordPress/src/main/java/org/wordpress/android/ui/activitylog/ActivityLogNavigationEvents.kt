package org.wordpress.android.ui.activitylog

import org.wordpress.android.ui.activitylog.list.ActivityLogListItem

sealed class ActivityLogNavigationEvents {
    data class ShowBackupDownload(val event: ActivityLogListItem.Event) : ActivityLogNavigationEvents()
    data class ShowRestore(val event: ActivityLogListItem.Event) : ActivityLogNavigationEvents()
    data class DownloadBackupFile(val url: String) : ActivityLogNavigationEvents()
}
