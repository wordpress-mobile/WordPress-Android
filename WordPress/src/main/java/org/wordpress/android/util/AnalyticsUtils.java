package org.wordpress.android.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsMetadata;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.SiteStore;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsUtils {
    private static String BLOG_ID_KEY = "blog_id";
    private static String POST_ID_KEY = "post_id";
    private static String FEED_ID_KEY = "feed_id";
    private static String FEED_ITEM_ID_KEY = "feed_item_id";
    private static String IS_JETPACK_KEY = "is_jetpack";

    /**
     * Utility methods to refresh Tracks and Mixpanel metadata.
     */
    public static void refreshMetadata(AccountStore accountStore, SiteStore siteStore) {
        AnalyticsMetadata metadata = new AnalyticsMetadata();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());

        metadata.setSessionCount(preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0));
        metadata.setUserConnected(WPStoreUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore));
        metadata.setWordPressComUser(accountStore.hasAccessToken());
        metadata.setJetpackUser(siteStore.hasJetpackSite());
        metadata.setNumBlogs(siteStore.getSitesCount());
        metadata.setUsername(accountStore.getAccount().getUserName());
        metadata.setEmail(accountStore.getAccount().getEmail());

        AnalyticsTracker.refreshMetadata(metadata);
    }

    public static void refreshMetadataNewUser(String username, String email) {
        AnalyticsMetadata metadata = new AnalyticsMetadata();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        metadata.setSessionCount(preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0));
        metadata.setUserConnected(true);
        metadata.setWordPressComUser(true);
        metadata.setJetpackUser(false);
        metadata.setNumBlogs(1);
        metadata.setUsername(username);
        metadata.setEmail(email);
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
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, SiteModel site) {
        trackWithBlogDetails(stat, site, null);
    }

    // TODO: STORES: do nothing
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Blog site) {}
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, Blog site, Map<String, Object> properties) {}

    /**
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat The Stat to bump
     * @param blog The blog object
     * @param properties Properties to attach to the event
     *
     */
    public static void trackWithBlogDetails(AnalyticsTracker.Stat stat, SiteModel site, Map<String, Object>
            properties) {
        if (site == null || (!site.isWPCom() && !site.isJetpack())) {
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack. Tracking analytics without blog info");
            AnalyticsTracker.track(stat, properties);
            return;
        }

        if (site.isWPCom()) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(BLOG_ID_KEY, site.getSiteId());
            properties.put(IS_JETPACK_KEY, site.isJetpack());
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
    }
}
