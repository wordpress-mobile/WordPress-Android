package org.wordpress.android.ui.notifications;

import com.android.volley.VolleyError;

import org.wordpress.android.models.Note;

import java.util.List;

public class NotificationEvents {
    public static class NotificationsChanged {
        final public boolean unseenNotesEh;
        public NotificationsChanged() {
            this.unseenNotesEh = false;
        }
        public NotificationsChanged(boolean unseenNotesEh) {
            this.unseenNotesEh = unseenNotesEh;
        }
    }
    public static class NoteModerationFailed {}
    public static class NoteModerationStatusChanged {
        final boolean moderatingEh;
        final String noteId;
        public NoteModerationStatusChanged(String noteId, boolean moderatingEh) {
            this.noteId = noteId;
            this.moderatingEh = moderatingEh;
        }
    }
    public static class NoteLikeStatusChanged {
        final String noteId;
        public NoteLikeStatusChanged(String noteId) {
            this.noteId = noteId;
        }
    }
    public static class NoteVisibilityChanged {
        final boolean hiddenEh;
        final String noteId;
        public NoteVisibilityChanged(String noteId, boolean hiddenEh) {
            this.noteId = noteId;
            this.hiddenEh = hiddenEh;
        }
    }
    public static class NotificationsSettingsStatusChanged {
        final String mMessage;
        public NotificationsSettingsStatusChanged(String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }
    }
    public static class NotificationsUnseenStatus {
        final public boolean unseenNotesEh;
        public NotificationsUnseenStatus(boolean unseenNotesEh) {
            this.unseenNotesEh = unseenNotesEh;
        }
    }
    public static class NotificationsRefreshCompleted {
        final List<Note> notes;
        public NotificationsRefreshCompleted(List<Note> notes) {
            this.notes = notes;
        }
    }
    public static class NotificationsRefreshError {
        VolleyError error;
        public NotificationsRefreshError(VolleyError error) {
            this.error = error;
        }
        public NotificationsRefreshError() {
        }
    }
}
