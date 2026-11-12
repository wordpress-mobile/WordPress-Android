package org.wordpress.android.ui.notifications.utils

import android.content.Context
import org.wordpress.android.push.NativeNotificationsUtils
import javax.inject.Inject

class PendingDraftsNotificationsUtilsWrapper
@Inject constructor() {
    fun cancelPendingDraftAlarms(context: Context, postId: Int) =
        PendingDraftsNotificationsUtils.cancelPendingDraftAlarms(
            context,
            postId
        )

    /** Removes a pending-draft reminder already on screen for [postId]. */
    fun dismissPendingDraftNotification(context: Context, postId: Int) =
        NativeNotificationsUtils.dismissNotification(
            PendingDraftsNotificationsUtils.makePendingDraftNotificationId(postId),
            context
        )

    fun scheduleNextNotifications(context: Context, postId: Int, dateLocallyChanged: String) =
        PendingDraftsNotificationsUtils.scheduleNextNotifications(
            context,
            postId,
            dateLocallyChanged
        )
}
