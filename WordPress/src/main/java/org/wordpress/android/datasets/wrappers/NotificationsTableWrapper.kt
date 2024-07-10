package org.wordpress.android.datasets.wrappers

import dagger.Reusable
import org.wordpress.android.datasets.NotificationsTable
import org.wordpress.android.models.Note
import javax.inject.Inject

@Reusable
class NotificationsTableWrapper @Inject constructor() {
    fun saveNote(note: Note): Boolean = NotificationsTable.saveNote(note)

    fun saveNotes(notes: List<Note>, clearBeforeSaving: Boolean) {
        NotificationsTable.saveNotes(notes, clearBeforeSaving)
    }
}
