package org.wordpress.android.ui.notifications;

public class NotificationEvents {
    public static class SimperiumNotAuthorized {}
    public static class NotificationsChanged {}
    public static class NoteModerationFailed {}
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
    public static class NotificationsSettingsStatusChanged {
        String mMessage;
        public NotificationsSettingsStatusChanged(String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }
    }
}
