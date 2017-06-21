package org.wordpress.android.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.Html;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.analytics.AnalyticsMetadata;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTrackerNosara;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderPost;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_LIKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_OPENED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_GLOBAL_RELATED_POST_CLICKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_LOCAL_RELATED_POST_CLICKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.TRAIN_TRACKS_INTERACT;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.TRAIN_TRACKS_RENDER;

public class AnalyticsUtils {
    private static String BLOG_ID_KEY = "blog_id";
    private static String POST_ID_KEY = "post_id";
    private static String COMMENT_ID_KEY = "comment_id";
    private static String FEED_ID_KEY = "feed_id";
    private static String FEED_ITEM_ID_KEY = "feed_item_id";
    private static String IS_JETPACK_KEY = "is_jetpack";
    private static String INTENT_ACTION = "intent_action";
    private static String INTENT_DATA = "intent_data";
    private static String INTERCEPTED_URI = "intercepted_uri";
    private static String INTERCEPTOR_CLASSNAME = "interceptor_classname";

    /**
     * Utility methods to refresh metadata.
     */
    public static void refreshMetadata(AccountStore accountStore, SiteStore siteStore) {
        AnalyticsMetadata metadata = new AnalyticsMetadata();

        metadata.setUserConnected(FluxCUtils.isSignedInWPComOrHasWPOrgSite(accountStore, siteStore));
        metadata.setWordPressComUser(accountStore.hasAccessToken());
        metadata.setJetpackUser(isJetpackUser(siteStore));
        metadata.setNumBlogs(siteStore.getSitesCount());
        metadata.setUsername(accountStore.getAccount().getUserName());
        metadata.setEmail(accountStore.getAccount().getEmail());

        AnalyticsTracker.refreshMetadata(metadata);
    }

    /***
     * @return true if the siteStore has sites accessed via the WPCom Rest API that are not WPCom sites. This only
     * counts Jetpack sites connected via WPCom Rest API. If there are Jetpack sites in the site store and they're
     * all accessed via XMLRPC, this method returns false.
     */
    private static boolean isJetpackUser(SiteStore siteStore) {
        return siteStore.getSitesAccessedViaWPComRestCount() - siteStore.getWPComSitesCount() > 0;
    }

    public static void refreshMetadataNewUser(String username, String email) {
        AnalyticsMetadata metadata = new AnalyticsMetadata();
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
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat The Stat to bump
     * @param site The site object
     *
     */
    public static void trackWithSiteDetails(AnalyticsTracker.Stat stat, SiteModel site) {
        trackWithSiteDetails(stat, site, null);
    }

    /**
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat The Stat to bump
     * @param site The site object
     * @param properties Properties to attach to the event
     */
    public static void trackWithSiteDetails(AnalyticsTracker.Stat stat, SiteModel site,
                                            Map<String, Object> properties) {
        if (site == null || !SiteUtils.isAccessedViaWPComRest(site)) {
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack. Tracking analytics without blog info");
            AnalyticsTracker.track(stat, properties);
            return;
        }

        if (SiteUtils.isAccessedViaWPComRest(site)) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(BLOG_ID_KEY, site.getSiteId());
            properties.put(IS_JETPACK_KEY, site.isJetpackConnected());
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
     * @param stat   The Stat to bump
     * @param blogID The REMOTE blog ID.
     */
    public static void trackWithSiteId(AnalyticsTracker.Stat stat, long blogID) {
        Map<String, Object> properties = new HashMap<>();
        if (blogID != 0) {
            properties.put(BLOG_ID_KEY, blogID);
        }
        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Bump Analytics for a reader post
     *
     * @param stat The Stat to bump
     * @param post The reader post to track
     */
    public static void trackWithReaderPostDetails(AnalyticsTracker.Stat stat, ReaderPost post) {
        if (post == null) return;

        // wpcom/jetpack posts should pass: feed_id, feed_item_id, blog_id, post_id, is_jetpack
        // RSS pass should pass: feed_id, feed_item_id, is_jetpack
        Map<String, Object> properties = new HashMap<>();
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

    /**
     * Track when a railcar item has been rendered
     *
     * @param railcarJson The JSON string of the railcar
     *
     */
    public static void trackWithReaderPostDetails(AnalyticsTracker.Stat stat, long blogId, long postId) {
        trackWithReaderPostDetails(stat, ReaderPostTable.getBlogPost(blogId, postId, true));
    }

    public static void trackWithBlogPostDetails(AnalyticsTracker.Stat stat, long blogId, long postId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(BLOG_ID_KEY, blogId);
        properties.put(POST_ID_KEY, postId);

        AnalyticsTracker.track(stat, properties);
    }

    public static void trackWithBlogPostDetails(AnalyticsTracker.Stat stat, String blogId, String postId, int
            commentId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(BLOG_ID_KEY, blogId);
        properties.put(POST_ID_KEY, postId);
        properties.put(COMMENT_ID_KEY, commentId);

        AnalyticsTracker.track(stat, properties);
    }

    public static void trackWithFeedPostDetails(AnalyticsTracker.Stat stat, long feedId, long feedItemId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(FEED_ID_KEY, feedId);
        properties.put(FEED_ITEM_ID_KEY, feedItemId);

        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Track when app launched via deep-linking
     *
     * @param stat   The Stat to bump
     * @param action The Intent action the app was started with
     * @param data   The data URI the app was started with
     */
    public static void trackWithDeepLinkData(AnalyticsTracker.Stat stat, String action, Uri data) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INTENT_ACTION, action);
        properties.put(INTENT_DATA, data != null ? data.toString() : null);

        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Track when app launched via deep-linking but then fell back to external browser
     *
     * @param stat           The Stat to bump
     * @param interceptedUri The fallback URI the app was started with
     */
    public static void trackWithInterceptedUri(AnalyticsTracker.Stat stat, String interceptedUri) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INTERCEPTED_URI, interceptedUri);

        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Track when app launched via deep-linking but then fell back to external browser
     *
     * @param stat                 The Stat to bump
     * @param interceptorClassname The name of the class that handles the intercept by default
     */
    public static void trackWithDefaultInterceptor(AnalyticsTracker.Stat stat, String interceptorClassname) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INTERCEPTOR_CLASSNAME, interceptorClassname);

        AnalyticsTracker.track(stat, properties);
    }

    /**
   * Track when a railcar item has been rendered
   *
   * @param railcarJson The JSON string of the railcar
   *
   */
    public static void trackRailcarRender(String railcarJson) {
        if (TextUtils.isEmpty(railcarJson)) {
            return;
        }

        AnalyticsTracker.track(TRAIN_TRACKS_RENDER, railcarJsonToProperties(railcarJson));
    }

    /**
     * Track when a railcar item has been interacted with
     *
     * @param stat The event that caused the interaction
     * @param railcarJson The JSON string of the railcar
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
                || stat == READER_GLOBAL_RELATED_POST_CLICKED
                || stat == READER_LOCAL_RELATED_POST_CLICKED;
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

    public static Map<String, Object> getMediaProperties(Context context, boolean isVideo, Uri mediaURI, String path) {
        Map<String, Object> properties = new HashMap<>();
        if(context == null) {
            AppLog.e(AppLog.T.MEDIA, "In order to track media properties Context cannot be null.");
            return properties;
        }
        if(mediaURI == null && TextUtils.isEmpty(path)) {
            AppLog.e(AppLog.T.MEDIA, "In order to track media properties mediaURI and path cannot be both null!!");
            return properties;
        }

        if(mediaURI != null) {
            path = MediaUtils.getRealPathFromURI(context, mediaURI);
        }

        if (TextUtils.isEmpty(path) ) {
            return  properties;
        }

        // File not found
        File file = new File(path);
        try {
            if (!file.exists()) {
                AppLog.e(AppLog.T.MEDIA, "Can't access the media file. It doesn't exists anymore!! Properties are not being tracked.");
                return properties;
            }

            if (file.lastModified() > 0L) {
                long ageMS = System.currentTimeMillis() - file.lastModified();
                properties.put("age_ms", ageMS);
            }
        } catch (SecurityException e) {
            AppLog.e(AppLog.T.MEDIA, "Can't access the media file. Properties are not being tracked.", e);
            return properties;
        }

        String mimeType = MediaUtils.getMediaFileMimeType(file);
        properties.put("mime", mimeType);

        String fileName = MediaUtils.getMediaFileName(file, mimeType);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase();
        properties.put("ext", fileExtension);

        if (!isVideo) {
            int[] dimensions = ImageUtils.getImageSize(Uri.fromFile(file), context);
            double megapixels = dimensions[0] * dimensions[1];
            megapixels = megapixels / 1000000;
            megapixels = Math.floor(megapixels);
            properties.put("megapixels", (int) megapixels);
        } else {
            long videoDurationMS = VideoUtils.getVideoDurationMS(context, file);
            properties.put("duration_secs", (int)videoDurationMS/1000);
        }

        properties.put("bytes", file.length());

        return  properties;
    }
}
