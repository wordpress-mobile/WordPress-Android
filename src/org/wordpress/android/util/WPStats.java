package org.wordpress.android.util;

import org.json.JSONObject;

import java.util.List;
import java.util.Vector;

public class WPStats {

    public enum Stat {
        APPLICATION_OPENED,
        APPLICATION_CLOSED,
        THEMES_ACCESSED_THEMES_BROWSER,
        THEMES_CHANGED_THEME,
        READER_ACCESSED,
        READER_OPENED_ARTICLE,
        READER_LIKED_ARTICLE,
        READER_REBLOGGED_ARTICLE,
        READER_INFINITE_SCROLL,
        READER_FOLLOWED_READER_TAG,
        READER_UNFOLLOWED_READER_TAG,
        READER_LOADED_TAG,
        READER_LOADED_FRESHLY_PRESSED,
        READER_COMMENTED_ON_ARTICLE,
        STATS_ACCESSED,
        EDITOR_CREATED_POST,
        EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY,
        EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY,
        EDITOR_UPDATED_POST,
        EDITOR_PUBLISHED_POST,
        EDITOR_PUBLISHED_POST_WITH_PHOTO,
        EDITOR_PUBLISHED_POST_WITH_VIDEO,
        EDITOR_PUBLISHED_POST_WITH_CATEGORIES,
        EDITOR_PUBLISHED_POST_WITH_TAGS,
        NOTIFICATIONS_ACCESSED,
        NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS,
        NOTIFICATION_PERFORMED_ACTION,
        NOTIFICATION_REPLIED_TO,
        NOTIFICATION_APPROVED,
        NOTIFICATION_TRASHED,
        NOTIFICATION_FLAGGED_AS_SPAM,
        OPENED_POSTS,
        OPENED_PAGES,
        OPENED_COMMENTS,
        OPENED_VIEW_SITE,
        OPENED_VIEW_ADMIN,
        OPENED_MEDIA_LIBRARY,
        OPENED_SETTINGS,
        CREATED_ACCOUNT,
        CREATED_SITE,
        SHARED_ITEM,
        SHARED_ITEM_VIA_EMAIL,
        SHARED_ITEM_VIA_SMS,
        SHARED_ITEM_VIA_TWITTER,
        SHARED_ITEM_VIA_FACEBOOK,
        SENT_ITEM_TO_INSTAPAPER,
        SENT_ITEM_TO_POCKET,
        SENT_ITEM_TO_GOOGLE_PLUS,
    };

    public interface Tracker {
        public void track(Stat stat);
        public void track(Stat stat, JSONObject properties);
        public void beginSession();
        public void endSession();
    }

    private static final List<Tracker> trackers = new Vector<Tracker>();

    public static void registerTracker(Tracker tracker) {
        if (tracker != null) {
            trackers.add(tracker);
        }
    }

    public static void track(Stat stat) {
        for(Tracker tracker : trackers) {
            tracker.track(stat);
        }
    }

    public static void track(Stat stat, JSONObject properties) {
        for(Tracker tracker : trackers) {
            tracker.track(stat, properties);
        }
    }

    public static void beginSession() {
        for(Tracker tracker : trackers) {
            tracker.beginSession();
        }
    }

    public static void endSession() {
        for(Tracker tracker : trackers) {
            tracker.endSession();
        }
    }
}

