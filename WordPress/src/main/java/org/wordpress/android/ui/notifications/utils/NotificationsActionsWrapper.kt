package org.wordpress.android.ui.notifications.utils

import dagger.Reusable
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.models.Note
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Reusable
class NotificationsActionsWrapper @Inject constructor(
    private val notificationStore: NotificationStore
) {
    suspend fun downloadNoteAndUpdateDB(noteId: String): Boolean =
        suspendCoroutine { continuation ->
            NotificationsActions.downloadNoteAndUpdateDB(
                noteId,
                { continuation.resume(true) },
                { continuation.resume(true) })
        }

    @Suppress("TooGenericExceptionCaught")
    suspend fun markNoteAsRead(notes: List<Note>): NotificationStore.OnNotificationChanged? {
        val noteIds = notes.map {
            try {
                it.id.toLong()
            } catch (ex: Exception) {
                // id might be empty
                AppLog.e(AppLog.T.NOTIFS, "Error parsing note id: ${it.id}", ex)
                -1L
            }
        }.filter { it != -1L }
        if (noteIds.isEmpty()) return null

        return notificationStore.markNotificationsRead(
            NotificationStore.MarkNotificationsReadPayload(noteIds.map { NotificationModel(remoteNoteId = it) })
        )
    }
}
