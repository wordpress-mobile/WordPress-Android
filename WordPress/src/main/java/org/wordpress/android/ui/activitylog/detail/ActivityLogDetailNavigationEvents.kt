package org.wordpress.android.ui.activitylog.detail

sealed class ActivityLogDetailNavigationEvents {
    data class ShowRewindDialog(val model: ActivityLogDetailModel) : ActivityLogDetailNavigationEvents()
}
