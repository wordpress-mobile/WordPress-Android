package org.wordpress.android.ui.notifications;

import androidx.annotation.NonNull;

import com.android.volley.VolleyError;

import org.wordpress.android.models.Note;

import java.util.List;

public class NotificationEvents {
    public static class NotificationsChanged {
        public final boolean hasUnseenNotes;
        NotificationsChanged() {
            this.hasUnseenNotes = false;
        }
        public NotificationsChanged(boolean hasUnseenNotes) {
            this.hasUnseenNotes = hasUnseenNotes;
        }
    }

    public static class NoteLikeOrModerationStatusChanged {
        public final String noteId;

        public NoteLikeOrModerationStatusChanged(String noteId) {
            this.noteId = noteId;
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
        public final boolean hasUnseenNotes;

        public NotificationsUnseenStatus(boolean hasUnseenNotes) {
            this.hasUnseenNotes = hasUnseenNotes;
        }
    }

    public static class NotificationsRefreshCompleted {
        public final List<Note> notes;

        public NotificationsRefreshCompleted(List<Note> notes) {
            this.notes = notes;
        }
    }

    public static class NotificationsRefreshError {
        public VolleyError error;

        public NotificationsRefreshError(VolleyError error) {
            this.error = error;
        }

        public NotificationsRefreshError() {}
    }

    public static class OnNoteCommentLikeChanged {
        public final Note note;
        public final boolean liked;

        public OnNoteCommentLikeChanged(@NonNull Note note, boolean liked) {
            this.note = note;
            this.liked = liked;
        }
    }

    public static class OnNotePostLikeChanged {
        public final Note note;
        public final boolean liked;

        public OnNotePostLikeChanged(@NonNull Note note, boolean liked) {
            this.note = note;
            this.liked = liked;
        }
    }
}
