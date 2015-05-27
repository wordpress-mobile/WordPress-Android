package org.wordpress.android.ui.notifications;

import org.wordpress.android.models.CommentStatus;

public class NotificationEvents {
    public static class SimperiumNotAuthorized {}
    public static class NotificationsChanged {}
    public static class NoteModerationStatusChanged {
        boolean mIsModerating;
        String mNoteId;
        public NoteModerationStatusChanged(String noteId, boolean isModerating) {
            mNoteId = noteId;
            mIsModerating = isModerating;
        }
    }
    public static class NoteVisibilityChanged {
        boolean mIsHidden;
        String mNoteId;
        public NoteVisibilityChanged(String noteId, boolean isHidden) {
            mNoteId = noteId;
            mIsHidden = isHidden;
        }
    }
    public static class NoteModerationFailed {
        CommentStatus mStatus;
        String mNoteId;
        public NoteModerationFailed(String noteId, CommentStatus status) {
            mNoteId = noteId;
            mStatus = status;
        }
    }
}
