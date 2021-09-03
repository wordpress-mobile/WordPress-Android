package org.wordpress.android.ui.activitylog.detail

const val DOCUMENTATION_PAGE_URL = "https://jetpack.com/support/backup/"

sealed class ActivityLogDetailNavigationEvents {
    data class ShowBackupDownload(val model: ActivityLogDetailModel) : ActivityLogDetailNavigationEvents()
    data class ShowRestore(val model: ActivityLogDetailModel) : ActivityLogDetailNavigationEvents()
    data class ShowDocumentationPage(val url: String = DOCUMENTATION_PAGE_URL) : ActivityLogDetailNavigationEvents()
}
