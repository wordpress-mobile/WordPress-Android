package org.wordpress.android.ui.notifications;

import com.android.volley.VolleyError;

import org.wordpress.android.models.Note;

import java.util.List;

public class NotificationEvents {
    public static class NotificationsChanged {}
    public static class NoteModerationFailed {}
    public static class NoteModerationStatusChanged {
        final boolean mIsModerating;
        final String mNoteId;
        public NoteModerationStatusChanged(String noteId, boolean isModerating) {
            mNoteId = noteId;
            mIsModerating = isModerating;
        }
    }
    public static class NoteVisibilityChanged {
        final boolean mIsHidden;
        final String mNoteId;
        public NoteVisibilityChanged(String noteId, boolean isHidden) {
            mNoteId = noteId;
            mIsHidden = isHidden;
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
