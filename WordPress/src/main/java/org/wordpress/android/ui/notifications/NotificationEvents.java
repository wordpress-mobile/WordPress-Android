package org.wordpress.android.ui.notifications;

import com.android.volley.VolleyError;

import org.wordpress.android.models.Note;

import java.util.List;

public class NotificationEvents {
    public static class NotificationsChanged {
        final public boolean hasUnseenNotes;
        public NotificationsChanged() {
            this.hasUnseenNotes = false;
        }
        public NotificationsChanged(boolean hasUnseenNotes) {
            this.hasUnseenNotes = hasUnseenNotes;
        }
    }
    public static class NoteLikeStatusChanged {
        final String noteId;
        public NoteLikeStatusChanged(String noteId) {
            this.noteId = noteId;
        }
    }
    public static class NoteVisibilityChanged {
        final boolean isHidden;
        final String noteId;
        public NoteVisibilityChanged(String noteId, boolean isHidden) {
            this.noteId = noteId;
            this.isHidden = isHidden;
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
        final public boolean hasUnseenNotes;
        public NotificationsUnseenStatus(boolean hasUnseenNotes) {
            this.hasUnseenNotes = hasUnseenNotes;
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
