package org.wordpress.android.ui.notifications.utils

import android.content.Context
import javax.inject.Inject

class PendingDraftsNotificationsUtilsWrapper
@Inject constructor() {
    fun cancelPendingDraftAlarms(context: Context, postId: Int) =
        PendingDraftsNotificationsUtils.cancelPendingDraftAlarms(
            context,
            postId
        )

    fun scheduleNextNotifications(context: Context, postId: Int, dateLocallyChanged: String) =
        PendingDraftsNotificationsUtils.scheduleNextNotifications(
            context,
            postId,
            dateLocallyChanged
        )
}
