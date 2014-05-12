package org.wordpress.android.util.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class AnalyticsTracker {
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
        READER_FOLLOWED_SITE,
        READER_LOADED_TAG,
        READER_LOADED_FRESHLY_PRESSED,
        READER_COMMENTED_ON_ARTICLE,
        STATS_ACCESSED,
        EDITOR_CREATED_POST,
        EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY,
        EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY,
        EDITOR_UPDATED_POST,
        EDITOR_SCHEDULED_POST,
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
        ADDED_SELF_HOSTED_SITE,
        SIGNED_INTO_JETPACK,
        PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN,
        STATS_SELECTED_INSTALL_JETPACK,
    }

    public interface Tracker {
        void track(Stat stat);
        void track(Stat stat, Map<String, ?> properties);
        void beginSession();
        void endSession();
        void clearAllData();
    }

    private static final List<Tracker> TRACKERS = new ArrayList<Tracker>();

    private AnalyticsTracker() {
    }

    public static void registerTracker(Tracker tracker) {
        if (tracker != null) {
            TRACKERS.add(tracker);
        }
    }

    public static void track(Stat stat) {
        for (Tracker tracker : TRACKERS) {
            tracker.track(stat);
        }
    }

    public static void track(Stat stat, Map<String, ?> properties) {
        for (Tracker tracker : TRACKERS) {
            tracker.track(stat, properties);
        }
    }

    public static void beginSession() {
        for (Tracker tracker : TRACKERS) {
            tracker.beginSession();
        }
    }

    public static void endSession() {
        for (Tracker tracker : TRACKERS) {
            tracker.endSession();
        }
    }

    public static void clearAllData() {
        for (Tracker tracker : TRACKERS) {
            tracker.clearAllData();
        }
    }
}

