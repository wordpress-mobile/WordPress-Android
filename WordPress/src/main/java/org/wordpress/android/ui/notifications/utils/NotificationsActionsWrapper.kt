package org.wordpress.android.ui.notifications.utils

import dagger.Reusable
import org.wordpress.android.models.Note
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Reusable
class NotificationsActionsWrapper @Inject constructor() {
    suspend fun downloadNoteAndUpdateDB(noteId: String): Boolean =
        suspendCoroutine { continuation ->
            NotificationsActions.downloadNoteAndUpdateDB(
                noteId,
                { continuation.resume(true) },
                { continuation.resume(true) })
        }

    fun markNoteAsRead(note: Note?) {
        NotificationsActions.markNoteAsRead(note)
    }
}
