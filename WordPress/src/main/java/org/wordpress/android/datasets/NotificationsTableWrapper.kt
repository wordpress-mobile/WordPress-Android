package org.wordpress.android.datasets

import dagger.Reusable
import org.wordpress.android.fluxc.model.CommentModel
import javax.inject.Inject

@Reusable
class NotificationsTableWrapper @Inject constructor() {
    fun getNotificationById(noteID: String): CommentModel? =
            NotificationsTable.getNoteById(noteID)?.buildComment()
}
