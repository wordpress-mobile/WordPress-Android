package org.wordpress.android.util.analytics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsMetadata;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.AnalyticsTrackerNosara;
import org.wordpress.android.datasets.ReaderPostTable;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.PostImmutableModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.PushAccountSettingsPayload;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.ui.PagePostCreationSourcesDetail;
import org.wordpress.android.ui.posts.EditPostActivity;
import org.wordpress.android.ui.posts.PostListViewLayoutType;
import org.wordpress.android.ui.posts.PostUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.FluxCUtils;
import org.wordpress.android.util.ImageUtils;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.VideoUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.wordpress.android.analytics.AnalyticsTracker.Stat.NEWS_CARD_DIMISSED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.NEWS_CARD_EXTENDED_INFO_REQUESTED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.NEWS_CARD_SHOWN;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_LIKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_ARTICLE_OPENED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_GLOBAL_RELATED_POST_CLICKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_LOCAL_RELATED_POST_CLICKED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.TRAIN_TRACKS_INTERACT;
import static org.wordpress.android.analytics.AnalyticsTracker.Stat.TRAIN_TRACKS_RENDER;
import static org.wordpress.android.ui.PagePostCreationSourcesDetail.CREATED_POST_SOURCE_DETAIL_KEY;
import static org.wordpress.android.ui.posts.EditPostActivity.EXTRA_IS_QUICKPRESS;

public class AnalyticsUtils {
    private static final String BLOG_ID_KEY = "blog_id";
    private static final String POST_ID_KEY = "post_id";
    private static final String COMMENT_ID_KEY = "comment_id";
    private static final String FEED_ID_KEY = "feed_id";
    private static final String FEED_ITEM_ID_KEY = "feed_item_id";
    private static final String SOURCE_BLOG_ID_KEY = "source_blog_id";
    private static final String SOURCE_POST_ID_KEY = "source_post_id";
    private static final String TARGET_BLOG_ID_KEY = "target_blog_id";
    private static final String IS_JETPACK_KEY = "is_jetpack";
    private static final String INTENT_ACTION = "intent_action";
    private static final String INTENT_HOST = "intent_host";
    private static final String INTENT_DATA = "intent_data";
    private static final String INTERCEPTED_URI = "intercepted_uri";
    private static final String INTERCEPTOR_CLASSNAME = "interceptor_classname";
    private static final String NEWS_CARD_ORIGIN = "origin";
    private static final String NEWS_CARD_VERSION = "version";

    public static final String HAS_GUTENBERG_BLOCKS_KEY = "has_gutenberg_blocks";
    public static final String HAS_WP_STORIES_BLOCKS_KEY = "has_wp_stories_blocks";
    public static final String EDITOR_HAS_HW_ACCELERATION_DISABLED_KEY = "editor_has_hw_disabled";
    public static final String EXTRA_CREATION_SOURCE_DETAIL = "creationSourceDetail";

    public enum BlockEditorEnabledSource {
        VIA_SITE_SETTINGS,
        ON_SITE_CREATION,
        ON_BLOCK_POST_OPENING,
        ON_PROGRESSIVE_ROLLOUT_PHASE_1,
        ON_PROGRESSIVE_ROLLOUT_PHASE_2;

        public Map<String, Object> asPropertyMap() {
            Map<String, Object> properties = new HashMap<>();
            properties.put("source", name().toLowerCase(Locale.ROOT));
            return properties;
        }
    }

    public static void trackEditorCreatedPost(String action, Intent intent, SiteModel site, PostImmutableModel post) {
        Map<String, Object> properties = new HashMap<>();
        // Post created from the post list (new post button).
        String normalizedSourceName = "post-list";

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            // Post created with share with WordPress
            normalizedSourceName = "shared-from-external-app";
        }
        if (EditPostActivity.NEW_MEDIA_POST.equals(
                action)) {
            // Post created from the media library
            normalizedSourceName = "media-library";
        }
        if (intent != null && intent.hasExtra(EXTRA_IS_QUICKPRESS)) {
            // Quick press
            normalizedSourceName = "quick-press";
        }
        PostUtils.addPostTypeAndPostFormatToAnalyticsProperties(post, properties);
        properties.put("created_post_source", normalizedSourceName);

        if (intent != null
            && intent.hasExtra(EXTRA_CREATION_SOURCE_DETAIL)
            && normalizedSourceName == "post-list") {
            PagePostCreationSourcesDetail source =
                    (PagePostCreationSourcesDetail) intent.getSerializableExtra(EXTRA_CREATION_SOURCE_DETAIL);
            properties.put(
                    CREATED_POST_SOURCE_DETAIL_KEY,
                    source != null ? source.getLabel() : PagePostCreationSourcesDetail.NO_DETAIL.getLabel()
            );
        } else {
            properties.put(
                    CREATED_POST_SOURCE_DETAIL_KEY,
                    PagePostCreationSourcesDetail.NO_DETAIL.getLabel()
            );
        }

        AnalyticsUtils.trackWithSiteDetails(
                AnalyticsTracker.Stat.EDITOR_CREATED_POST,
                site,
                properties
        );
    }

    public static void updateAnalyticsPreference(Context ctx,
                                                 Dispatcher mDispatcher,
                                                 AccountStore mAccountStore,
                                                 boolean optOut) {
        AnalyticsTracker.setHasUserOptedOut(optOut);
        if (optOut) {
            AnalyticsTracker.clearAllData();
        }
        // Sync with wpcom if a token is available
        if (mAccountStore.hasAccessToken()) {
            mAccountStore.getAccount().setTracksOptOut(optOut);
            PushAccountSettingsPayload payload = new PushAccountSettingsPayload();
            payload.params = new HashMap<>();
            payload.params.put("tracks_opt_out", optOut);
            mDispatcher.dispatch(AccountActionBuilder.newPushSettingsAction(payload));
        }
        // Store the preference locally
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(ctx.getString(R.string.pref_key_send_usage), !optOut);
        editor.apply();
    }

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
        if (siteStore.hasSite()) {
            metadata.setGutenbergEnabled(isGutenbergEnabledOnAnySite(siteStore.getSites()));
        }

        AnalyticsTracker.refreshMetadata(metadata);
    }

    @VisibleForTesting
    protected static boolean isGutenbergEnabledOnAnySite(List<SiteModel> sites) {
        for (SiteModel currentSite : sites) {
            if (SiteUtils.GB_EDITOR_NAME.equals(currentSite.getMobileEditor())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if the siteStore has sites accessed via the WPCom Rest API that are not WPCom sites. This only
     * counts Jetpack sites connected via WPCom Rest API. If there are Jetpack sites in the site store and they're
     * all accessed via XMLRPC, this method returns false.
     */
    static boolean isJetpackUser(SiteStore siteStore) {
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
        // GB is enabled for new users
        metadata.setGutenbergEnabled(true);
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
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack."
                                     + " Tracking analytics without blog info");
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

    public enum QuickActionTrackPropertyValue {
        LIKE {
            public String toString() {
                return "like";
            }
        },
        APPROVE {
            public String toString() {
                return "approve";
            }
        },
        REPLY_TO {
            public String toString() {
                return "reply-to";
            }
        }
    }

    public static void trackQuickActionTouched(QuickActionTrackPropertyValue type,
                                               SiteModel site,
                                               CommentModel comment) {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put("quick_action", type.toString());

        // add available information
        if (site != null) {
            properties.put(BLOG_ID_KEY, site.getSiteId());
            properties.put(IS_JETPACK_KEY, site.isJetpackConnected());
        }

        if (comment != null) {
            properties.put(POST_ID_KEY, comment.getRemotePostId());
            properties.put(COMMENT_ID_KEY, comment.getRemoteCommentId());
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_QUICKACTION_TOUCHED, properties);
        AnalyticsTracker.flush();
    }

    /**
     * Bump Analytics for comment reply, and add blog and comment details into properties.
     *
     * @param isQuickReply Whether is a quick reply or not
     * @param site The site object
     * @param comment The comment object
     */
    public static void trackCommentReplyWithDetails(boolean isQuickReply, SiteModel site,
                                                    CommentModel comment) {
        AnalyticsTracker.Stat stat = isQuickReply ? AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_REPLIED_TO
                : AnalyticsTracker.Stat.NOTIFICATION_REPLIED_TO;
        if (site == null || !SiteUtils.isAccessedViaWPComRest(site)) {
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack."
                                     + " Tracking analytics without blog info");
            AnalyticsTracker.track(stat);
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(BLOG_ID_KEY, site.getSiteId());
        properties.put(IS_JETPACK_KEY, site.isJetpackConnected());
        properties.put(POST_ID_KEY, comment.getRemotePostId());
        properties.put(COMMENT_ID_KEY, comment.getRemoteCommentId());

        AnalyticsTracker.track(stat, properties);
    }


    /**
     * Bump Analytics and add blog_id into properties
     *
     * @param stat The Stat to bump
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
        if (post == null) {
            return;
        }

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

    public static void trackWithReblogDetails(
            AnalyticsTracker.Stat stat,
            long sourceBlogId,
            long sourcePostId,
            long targetSiteId
    ) {
        Map<String, Object> properties = new HashMap<>();

        properties.put(SOURCE_BLOG_ID_KEY, sourceBlogId);
        properties.put(SOURCE_POST_ID_KEY, sourcePostId);
        properties.put(TARGET_BLOG_ID_KEY, targetSiteId);

        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Track when app launched via deep-linking
     *
     * @param stat The Stat to bump
     * @param action The Intent action the app was started with
     * @param host The host if applicable
     * @param data The data URI the app was started with
     */
    public static void trackWithDeepLinkData(AnalyticsTracker.Stat stat, String action, String host, Uri data) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INTENT_ACTION, action);
        properties.put(INTENT_HOST, host);
        properties.put(INTENT_DATA, data != null ? data.toString() : null);

        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Track when app launched via deep-linking but then fell back to external browser
     *
     * @param stat The Stat to bump
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
     * @param stat The Stat to bump
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
     */
    private static void trackRailcarInteraction(AnalyticsTracker.Stat stat, String railcarJson) {
        if (TextUtils.isEmpty(railcarJson)) {
            return;
        }

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
     * Converts the JSON string of a railcar to a properties list using the existing json key names
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
        if (context == null) {
            AppLog.e(AppLog.T.MEDIA, "In order to track media properties Context cannot be null.");
            return properties;
        }
        if (mediaURI == null && TextUtils.isEmpty(path)) {
            AppLog.e(AppLog.T.MEDIA, "In order to track media properties mediaURI and path cannot be both null!!");
            return properties;
        }

        if (mediaURI != null) {
            path = MediaUtils.getRealPathFromURI(context, mediaURI);
        }

        if (TextUtils.isEmpty(path)) {
            return properties;
        }

        // File not found
        File file = new File(path);
        try {
            if (!file.exists()) {
                AppLog.e(AppLog.T.MEDIA,
                         "Can't access the media file. It doesn't exists anymore!! Properties are not being tracked.");
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
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(fileName).toLowerCase(Locale.ROOT);
        properties.put("ext", fileExtension);

        if (!isVideo) {
            int[] dimensions = ImageUtils.getImageSize(Uri.fromFile(file), context);
            double megapixels = dimensions[0] * dimensions[1];
            megapixels = megapixels / 1000000;
            megapixels = Math.floor(megapixels);
            properties.put("megapixels", (int) megapixels);
        } else {
            long videoDurationMS = VideoUtils.getVideoDurationMS(context, file);
            properties.put("duration_secs", (int) videoDurationMS / 1000);
        }

        properties.put("bytes", file.length());

        return properties;
    }

    public static void trackAnalyticsSignIn(AccountStore accountStore, SiteStore siteStore, boolean isWpcomLogin) {
        AnalyticsUtils.refreshMetadata(accountStore, siteStore);
        Map<String, Boolean> properties = new HashMap<>();
        properties.put("dotcom_user", isWpcomLogin); // CHECKSTYLE IGNORE
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_IN, properties);
        if (!isWpcomLogin) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.ADDED_SELF_HOSTED_SITE);
        }
    }

    /**
     * Refreshes analytics metadata and bumps the account created stat.
     *
     * @param username
     * @param email
     */
    public static void trackAnalyticsAccountCreated(String username, String email, Map<String, Object> properties) {
        AnalyticsUtils.refreshMetadataNewUser(username, email);
        // This stat is part of a funnel that provides critical information.  Before
        // making ANY modification to this stat please refer to: p4qSXL-35X-p2
        AnalyticsTracker.track(Stat.CREATED_ACCOUNT, properties);
    }

    public static void trackAnalyticsPostListToggleLayout(PostListViewLayoutType viewLayoutType) {
        Map<String, String> properties = new HashMap<>();
        properties.put("post_list_view_layout_type", viewLayoutType.toString());
        AnalyticsTracker.track(AnalyticsTracker.Stat.POST_LIST_VIEW_LAYOUT_TOGGLED, properties);
    }

    /**
     * Don't use this method directly, use injectable NewsTracker instead.
     */
    public static void trackNewsCardShown(String origin, int version) {
        AnalyticsTracker.track(NEWS_CARD_SHOWN, createNewsCardProperties(origin, version));
    }

    /**
     * Don't use this method directly, use injectable NewsTracker instead.
     */
    public static void trackNewsCardDismissed(String origin, int version) {
        AnalyticsTracker.track(NEWS_CARD_DIMISSED, createNewsCardProperties(origin, version));
    }

    /**
     * Don't use this method directly, use injectable NewsTracker instead.
     */
    public static void trackNewsCardExtendedInfoRequested(String origin, int version) {
        AnalyticsTracker.track(NEWS_CARD_EXTENDED_INFO_REQUESTED, createNewsCardProperties(origin, version));
    }

    private static Map<String, String> createNewsCardProperties(String origin, int version) {
        Map<String, String> properties = new HashMap<>();
        properties.put(NEWS_CARD_ORIGIN, origin);
        properties.put(NEWS_CARD_VERSION, String.valueOf(version));
        return properties;
    }
}
