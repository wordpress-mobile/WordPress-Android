package org.wordpress.android.util;

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsMetadata;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.analytics.AnalyticsTrackerNosara;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.ReaderPost;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_LIKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_OPENED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_RELATED_POST_CLICKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.TRAIN_TRACKS_INTERACT;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.TRAIN_TRACKS_RENDER;

public class AnalyticsUtils {
    private static String BLOG_ID_KEY = "blog_id";
    private static String POST_ID_KEY = "post_id";
    private static String FEED_ID_KEY = "feed_id";
    private static String FEED_ITEM_ID_KEY = "feed_item_id";
    private static String IS_JETPACK_KEY = "is_jetpack";
    private static String INTENT_ACTION = "intent_action";
    private static String INTENT_DATA = "intent_data";

    /**
     * Utility method to refresh mixpanel metadata.
     *
     * @param username WordPress.com username
     * @param email WordPress.com email address
     */
    public static void refreshMetadata(String username, String email) {
        AnalyticsMetadata metadata = new AnalyticsMetadata();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());

        metadata.setSessionCount(preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0));
        metadata.setUserConnected(AccountHelper.isSignedIn());
        metadata.setWordPressComUser(AccountHelper.isSignedInWordPressDotCom());
        metadata.setJetpackUser(AccountHelper.isJetPackUser());
        metadata.setNumBlogs(WordPress.wpDB.getNumBlogs());
        metadata.setUsername(username);
        metadata.setEmail(email);

        AnalyticsTracker.refreshMetadata(metadata);
    }

    /**
     * Utility method to refresh mixpanel metadata.
     */
    public static void refreshMetadata() {
        AnalyticsMetadata metadata = new AnalyticsMetadata();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());

        metadata.setSessionCount(preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0));
        metadata.setUserConnected(AccountHelper.isSignedIn());
        metadata.setWordPressComUser(AccountHelper.isSignedInWordPressDotCom());
        metadata.setJetpackUser(AccountHelper.isJetPackUser());
        metadata.setNumBlogs(WordPress.wpDB.getNumBlogs());
        metadata.setUsername(AccountHelper.getDefaultAccount().getUserName());
        metadata.setEmail(AccountHelper.getDefaultAccount().getEmail());

        AnalyticsTracker.refreshMetadata(metadata);
    }

    public static int getWordCount(String content) {
        String text = Html.fromHtml(content.replaceAll("<img[^>]*>", "")).toString();
        return text.split("\\s+").length;
    }

    /**
     * Bump Analytics for the passed Stat and CURRENT blog details into properties.
     *
     * @param stat The Stat to bump
     *
     */
    public static void trackWithCurrentBlogDetails(AnalyticsTracker.Stat stat) {
        trackWithCurrentBlogDetails(stat, null);
    }

    /**
     * Bump Analytics for the passed Stat and CURRENT blog details into properties.
     *
     * @param stat The Stat to bump
     * @param properties Properties to attach to the event
     *
     */
    public static void trackWithCurrentBlogDetails(AnalyticsTracker.Stat stat, Map<String, Object> properties) {
        trackWithBlogDetails(stat, WordPress.getCurrentBlog(), properties);
    }

    /**
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat The Stat to bump
     * @param blog The blog object
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Blog blog) {
        trackWithBlogDetails(stat, blog, null);
    }

    /**
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat The Stat to bump
     * @param blog The blog object
     * @param properties Properties to attach to the event
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Blog blog, Map<String, Object> properties) {
        if (blog == null || (!blog.isDotcomFlag() && !blog.isJetpackPowered())) {
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack. Tracking analytics without blog info");
            AnalyticsTracker.track(stat, properties);
            return;
        }

        String blogID = blog.getDotComBlogId();
        if (blogID != null) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(BLOG_ID_KEY, blogID);
            properties.put(IS_JETPACK_KEY, blog.isJetpackPowered());
        } else {
            // When the blog ID is null here does mean the blog is not hosted on wpcom.
            // It may be a Jetpack blog still in synch for options, or a self-hosted.
            // In both of these cases skip adding blog details into properties.
        }

        if (properties == null) {
            AnalyticsTracker.track(stat);
        } else {
            AnalyticsTracker.track(stat, properties);
        }
    }

    /**
     * Bump Analytics and add blog_id into properties
     *
     * @param stat The Stat to bump
     * @param blogID The REMOTE blog ID.
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Long blogID) {
        Map<String, Object> properties =  new HashMap<>();
        if (blogID != null) {
            properties.put(BLOG_ID_KEY, blogID);
        }
        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Bump Analytics and add blog_id into properties
     *
     * @param stat The Stat to bump
     * @param blogID The REMOTE blog ID.
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, String blogID) {
        try {
            Long remoteID = Long.parseLong(blogID);
            trackWithBlogDetails(stat, remoteID);
        } catch (NumberFormatException err) {
            AnalyticsTracker.track(stat);
        }
    }

    /**
     * Bump Analytics for a reader post
     *
     * @param stat The Stat to bump
     * @param post The reader post to track
     *
     */
    public static void trackWithReaderPostDetails(AnalyticsTracker.Stat stat, ReaderPost post) {
        if (post == null) return;

        // wpcom/jetpack posts should pass: feed_id, feed_item_id, blog_id, post_id, is_jetpack
        // RSS pass should pass: feed_id, feed_item_id, is_jetpack
        Map<String, Object> properties =  new HashMap<>();
        if (post.isWP() || post.isJetpack) {
            properties.put(BLOG_ID_KEY, post.blogId);
            properties.put(POST_ID_KEY, post.postId);
        }
        properties.put(FEED_ID_KEY, post.feedId);
        properties.put(FEED_ITEM_ID_KEY, post.feedItemId);
        properties.put(IS_JETPACK_KEY, post.isJetpack);

        AnalyticsTracker.track(stat, properties);

        // record a railcar interact event if the post has a railcar and this can be tracked
        // as an interaction
        if (canTrackRailcarInteraction(stat) && post.hasRailcar()) {
            trackRailcarInteraction(stat, post.getRailcarJson());
        }
    }

    public static void trackWithReaderPostDetails(AnalyticsTracker.Stat stat, long blogId, long postId) {
        trackWithReaderPostDetails(stat, ReaderPostTable.getBlogPost(blogId, postId, true));
    }

    public static void trackWithBlogPostDetails(AnalyticsTracker.Stat stat, long blogId, long postId) {
        Map<String, Object> properties =  new HashMap<>();
        properties.put(BLOG_ID_KEY, blogId);
        properties.put(POST_ID_KEY, postId);

        AnalyticsTracker.track(stat, properties);
    }

    public static void trackWithFeedPostDetails(AnalyticsTracker.Stat stat, long feedId, long feedItemId) {
        Map<String, Object> properties =  new HashMap<>();
        properties.put(FEED_ID_KEY, feedId);
        properties.put(FEED_ITEM_ID_KEY, feedItemId);

        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Track when app launched via deep-linking
     *
     * @param stat The Stat to bump
     * @param action The Intent action the app was started with
     * @param data The data URI the app was started with
     *
     */
    public static void trackWithDeepLinkData(AnalyticsTracker.Stat stat, String action, Uri data) {
        Map<String, Object> properties =  new HashMap<>();
        properties.put(INTENT_ACTION, action);
        properties.put(INTENT_DATA, data != null ? data.toString() : null);

        AnalyticsTracker.track(stat, properties);
    }

  /**
   * Track when a railcar item has been rendered
   *
   * @param post The JSON string of the railcar
   *
   */
    public static void trackRailcarRender(String railcarJson) {
        if (TextUtils.isEmpty(railcarJson)) return;

        AnalyticsTracker.track(TRAIN_TRACKS_RENDER, railcarJsonToProperties(railcarJson));
    }

    /**
     * Track when a railcar item has been interacted with
     *
     * @param stat The event that caused the interaction
     * @param post The JSON string of the railcar
     *
     */
    private static void trackRailcarInteraction(AnalyticsTracker.Stat stat, String railcarJson) {
        if (TextUtils.isEmpty(railcarJson)) return;

        Map<String, Object> properties = railcarJsonToProperties(railcarJson);
        properties.put("action", AnalyticsTrackerNosara.getEventNameForStat(stat));
        AnalyticsTracker.track(TRAIN_TRACKS_INTERACT, properties);
    }

    /**
     * @param stat The event that would cause the interaction
     * @return True if the passed stat event can be recorded as a railcar interaction
     */
    private static boolean canTrackRailcarInteraction(AnalyticsTracker.Stat stat) {
        return stat == READER_ARTICLE_LIKED
                || stat == READER_ARTICLE_OPENED
                || stat == READER_SEARCH_RESULT_TAPPED
                || stat == READER_ARTICLE_COMMENTED_ON
                || stat == READER_RELATED_POST_CLICKED;
    }

    /*
     *  Converts the JSON string of a railcar to a properties list using the existing json key names
     */
    private static Map<String, Object> railcarJsonToProperties(@NonNull String railcarJson) {
        Map<String, Object> properties = new HashMap<>();
        try {
            JSONObject jsonRailcar = new JSONObject(railcarJson);
            Iterator<String> iter = jsonRailcar.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object value = jsonRailcar.get(key);
                properties.put(key, value);
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.READER, e);
        }

        return properties;
    }

}
