package org.wordpress.android.ui.notifications;

import com.android.volley.VolleyError;

import org.wordpress.android.models.Note;

import java.util.List;

/**
 * NotificationEvents.java class is responsible for notifying the user
 * if the Event's details are changed using NotificationsChanged class.
 */

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

    /**
     * Furthermore, if someone likes a post, the  user has notification about
     * this activity.
     */

    public static class NoteLikeOrModerationStatusChanged {
        public final String noteId;

        public NoteLikeOrModerationStatusChanged(String noteId) {
            this.noteId = noteId;
        }
    }

    /**
     * NotificationsSettingsStatusChanged let the user to select whether wants to have
     * notifications or not.
     */

    public static class NotificationsSettingsStatusChanged {
        final String mMessage;

        public NotificationsSettingsStatusChanged(String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }
    }

    /**
     * NotificationsUnseenStatus reveals to the user that there are unseen notifications.
     */

    public static class NotificationsUnseenStatus {
        public final boolean hasUnseenNotes;

        public NotificationsUnseenStatus(boolean hasUnseenNotes) {
            this.hasUnseenNotes = hasUnseenNotes;
        }
    }

    /**
     *
     * Contributed to issuee #7037 about Notification Marked as unread
     *
     * @copyright 2018 Huo Team
     *
     * This class is created to let the user mark a watched notification as watched using
     *
     *
     */

    public static class NotificationsMarkedAsUnread {

        public final boolean hasReadNotes;
        public final boolean hasUnseenNotes;

        /**
         *
         * Contributed to issuee #7037 about Notification Marked as unread
         * @copyright 2018 Huo Team
         *
         * This class is created to let the user mark a watched notification as watched using
         * @param hasReadNotes
         *
         * @param hasUnseenNotes is helping to switch status of hasReadNotes if the user clicks
         *                       on this icon.
         *
         */
        public NotificationsMarkedAsUnread(boolean hasUnseenNotes, boolean hasReadNotes){
            this.hasUnseenNotes = hasUnseenNotes;
            if (!(hasUnseenNotes)) this.hasReadNotes = true;
            else this.hasReadNotes = false;
        }
    }

    /**
     *
     * NotificationsRefreshCompleted erases the notification symbol from red to default
     * as well as it appears the last user's notifications.
     *
     */

    public static class NotificationsRefreshCompleted {
        public final List<Note> notes;

        public NotificationsRefreshCompleted(List<Note> notes) {
            this.notes = notes;
        }
    }

    /**
     *
     * The user is being informed that due to an unexpected reason (internet
     * malfunction, device CPU overload e.t.c) the notification cannot be performed
     * and respond with an appropriate message.
     *
     */

    public static class NotificationsRefreshError {
        public VolleyError error;

        public NotificationsRefreshError(VolleyError error) {
            this.error = error;
        }

        public NotificationsRefreshError() {}
    }
}
