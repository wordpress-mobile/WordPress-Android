package org.wordpress.android.datasets

import dagger.Reusable
import org.wordpress.android.models.Note
import javax.inject.Inject

@Reusable
class NotificationsTableWrapper @Inject constructor() {

    fun getNotificationById(noteID: String): Note? =
            NotificationsTable.getNoteById(noteID)
}
