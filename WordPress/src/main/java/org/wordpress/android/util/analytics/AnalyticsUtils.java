package org.wordpress.android.util.analytics;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.text.HtmlCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsMetadata;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.analytics.AnalyticsTrackerNosara;
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

import static org.wordpress.android.ui.PagePostCreationSourcesDetail.CREATED_POST_SOURCE_DETAIL_KEY;
import static org.wordpress.android.ui.posts.EditPostActivity.EXTRA_IS_QUICKPRESS;

public class AnalyticsUtils {
    private static final String BLOG_ID_KEY = "blog_id";
    private static final String POST_ID_KEY = "post_id";
    private static final String FOLLOW_KEY = "follow";
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
    private static final String INTERCEPTOR_CLASSNAME = "interceptor_classname";
    private static final String NEWS_CARD_ORIGIN = "origin";
    private static final String NEWS_CARD_VERSION = "version";
    private static final String SITE_TYPE_KEY = "site_type";
    private static final String COMMENT_ACTION_SOURCE = "source";
    private static final String SOURCE_KEY = "source";
    private static final String URL_KEY = "url";
    private static final String SOURCE_INFO_KEY = "source_info";
    private static final String LIST_TYPE_KEY = "list_type";
    private static final String IS_STORAGE_SETTINGS_RESOLVED_KEY = "is_storage_settings_resolved";
    private static final String PAGE_KEY = "page";
    private static final String PER_PAGE_KEY = "per_page";
    private static final String CAUSE_OF_ISSUE_KEY = "cause_of_issue";

    public static final String HAS_GUTENBERG_BLOCKS_KEY = "has_gutenberg_blocks";
    public static final String HAS_WP_STORIES_BLOCKS_KEY = "has_wp_stories_blocks";
    public static final String EDITOR_HAS_HW_ACCELERATION_DISABLED_KEY = "editor_has_hw_disabled";
    public static final String EXTRA_CREATION_SOURCE_DETAIL = "creationSourceDetail";
    public static final String PROMPT_ID = "prompt_id";

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
        String scheme = BuildConfig.DEBUG ? "debug" : BuildConfig.FLAVOR;
        metadata.setAppScheme(scheme);
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
        String text = HtmlCompat.fromHtml(
                content.replaceAll("<img[^>]*>", ""),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ).toString();
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
     * @param stat       The Stat to bump
     * @param site       The site object
     * @param properties Properties to attach to the event
     */
    public static void trackWithSiteDetails(AnalyticsTrackerWrapper analyticsTrackerWrapper,
                                            AnalyticsTracker.Stat stat,
                                            @Nullable SiteModel site,
                                            @Nullable Map<String, Object> properties) {
        if (site == null || !SiteUtils.isAccessedViaWPComRest(site)) {
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack."
                                     + " Tracking analytics without blog info");
            if (properties == null) {
                analyticsTrackerWrapper.track(stat);
            } else {
                analyticsTrackerWrapper.track(stat, properties);
            }
            return;
        }

        if (SiteUtils.isAccessedViaWPComRest(site)) {
            if (properties == null) {
                properties = new HashMap<>();
            }
            properties.put(BLOG_ID_KEY, site.getSiteId());
            properties.put(IS_JETPACK_KEY, site.isJetpackConnected());
            properties.put(SITE_TYPE_KEY, AnalyticsSiteType.toStringFromSiteModel(site));
        }

        if (properties == null) {
            analyticsTrackerWrapper.track(stat);
        } else {
            analyticsTrackerWrapper.track(stat, properties);
        }
    }

    /**
     * Bump Analytics for the passed Stat and add blog details into properties.
     *
     * @param stat       The Stat to bump
     * @param site       The site object
     * @param properties Properties to attach to the event
     */
    public static void trackWithSiteDetails(AnalyticsTracker.Stat stat,
                                            @Nullable SiteModel site,
                                            @Nullable Map<String, Object> properties) {
        trackWithSiteDetails(new AnalyticsTrackerWrapper(), stat, site, properties);
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
            properties.put(SITE_TYPE_KEY, AnalyticsSiteType.toStringFromSiteModel(site));
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
     * @param site         The site object
     * @param comment      The comment object
     * @param source       The source of the comment action
     */
    public static void trackCommentReplyWithDetails(boolean isQuickReply, SiteModel site,
                                                    CommentModel comment, AnalyticsCommentActionSource source) {
        AnalyticsTracker.Stat legacyTracker = null;
        if (source == AnalyticsCommentActionSource.NOTIFICATIONS) {
            legacyTracker = isQuickReply ? AnalyticsTracker.Stat.NOTIFICATION_QUICK_ACTIONS_REPLIED_TO
                    : AnalyticsTracker.Stat.NOTIFICATION_REPLIED_TO;
        }

        AnalyticsTracker.Stat stat = isQuickReply
                ? AnalyticsTracker.Stat.COMMENT_QUICK_ACTION_REPLIED_TO
                : AnalyticsTracker.Stat.COMMENT_REPLIED_TO;
        if (site == null || !SiteUtils.isAccessedViaWPComRest(site)) {
            AppLog.w(AppLog.T.STATS, "The passed blog obj is null or it's not a wpcom or Jetpack."
                                     + " Tracking analytics without blog info");
            AnalyticsTracker.track(stat);

            if (legacyTracker != null) {
                AnalyticsTracker.track(legacyTracker);
            }
            return;
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put(BLOG_ID_KEY, site.getSiteId());
        properties.put(IS_JETPACK_KEY, site.isJetpackConnected());
        properties.put(POST_ID_KEY, comment.getRemotePostId());
        properties.put(COMMENT_ID_KEY, comment.getRemoteCommentId());
        properties.put(SITE_TYPE_KEY, AnalyticsSiteType.toStringFromSiteModel(site));
        properties.put(COMMENT_ACTION_SOURCE, source.toString());

        AnalyticsTracker.track(stat, properties);

        if (legacyTracker != null) {
            AnalyticsTracker.track(legacyTracker, properties);
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

    public static void trackWithReaderPostDetails(AnalyticsTracker.Stat stat,
                                                  @Nullable ReaderPost post,
                                                  @NonNull Map<String, Object> properties) {
        if (post == null) {
            return;
        }

        // wpcom/jetpack posts should pass: feed_id, feed_item_id, blog_id, post_id, is_jetpack
        // RSS pass should pass: feed_id, feed_item_id, is_jetpack
        if (post.isWP() || post.isJetpack) {
            properties.put(BLOG_ID_KEY, post.blogId);
            properties.put(POST_ID_KEY, post.postId);
        }
        properties.put(FOLLOW_KEY, post.isFollowedByCurrentUser);
        properties.put(FEED_ID_KEY, post.feedId);
        properties.put(FEED_ITEM_ID_KEY, post.feedItemId);
        properties.put(IS_JETPACK_KEY, post.isJetpack);
        properties.put(SITE_TYPE_KEY, AnalyticsSiteType.toStringFromReaderPost(post));

        AnalyticsTracker.track(stat, properties);

        // record a railcar interact event if the post has a railcar and this can be tracked
        // as an interaction
        if (canTrackRailcarInteraction(stat) && post.hasRailcar()) {
            trackRailcarInteraction(stat, post.getRailcarJson());
        }
    }

    public static void trackWithBlogPostDetails(AnalyticsTracker.Stat stat, long blogId, long postId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(BLOG_ID_KEY, blogId);
        properties.put(POST_ID_KEY, postId);

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
     * @param stat   The Stat to bump
     * @param action The Intent action the app was started with
     * @param host   The host if applicable
     * @param data   The data URI the app was started with
     */
    public static void trackWithDeepLinkData(AnalyticsTracker.Stat stat, String action, String host, Uri data) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INTENT_ACTION, action);
        properties.put(INTENT_HOST, host);
        properties.put(INTENT_DATA, data != null ? data.toString() : null);

        AnalyticsTracker.track(stat, properties);
    }

    /**
     * Track when app launched via deep-linking
     *
     * @param stat   The Stat to bump
     * @param action The Intent action the app was started with
     * @param host   The host if applicable
     * @param source The source of deeplink (EMAIL, LINK or BANNER)
     * @param url    The deeplink URL stripped of sensitive data
     * @param sourceInfo    Any additional source info
     */
    public static void trackWithDeepLinkData(AnalyticsTracker.Stat stat, String action, String host, String source,
                                             String url, @Nullable String sourceInfo) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(INTENT_ACTION, action);
        properties.put(INTENT_HOST, host);
        properties.put(SOURCE_KEY, source);
        properties.put(URL_KEY, url);
        if (sourceInfo != null) {
            properties.put(SOURCE_INFO_KEY, sourceInfo);
        }

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
     */
    public static void trackRailcarRender(String railcarJson) {
        if (TextUtils.isEmpty(railcarJson)) {
            return;
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.TRAIN_TRACKS_RENDER, railcarJsonToProperties(railcarJson));
    }

    /**
     * Track when a railcar item has been interacted with
     *
     * @param stat        The event that caused the interaction
     * @param railcarJson The JSON string of the railcar
     */
    private static void trackRailcarInteraction(AnalyticsTracker.Stat stat, String railcarJson) {
        if (TextUtils.isEmpty(railcarJson)) {
            return;
        }

        Map<String, Object> properties = railcarJsonToProperties(railcarJson);
        properties.put("action", AnalyticsTrackerNosara.getEventNameForStat(stat));
        AnalyticsTracker.track(AnalyticsTracker.Stat.TRAIN_TRACKS_INTERACT, properties);
    }

    /**
     * @param stat The event that would cause the interaction
     * @return True if the passed stat event can be recorded as a railcar interaction
     */
    private static boolean canTrackRailcarInteraction(AnalyticsTracker.Stat stat) {
        return stat == AnalyticsTracker.Stat.READER_ARTICLE_LIKED
               || stat == AnalyticsTracker.Stat.READER_ARTICLE_OPENED
               || stat == AnalyticsTracker.Stat.READER_SEARCH_RESULT_TAPPED
               || stat == AnalyticsTracker.Stat.READER_ARTICLE_COMMENTED_ON
               || stat == AnalyticsTracker.Stat.READER_GLOBAL_RELATED_POST_CLICKED
               || stat == AnalyticsTracker.Stat.READER_LOCAL_RELATED_POST_CLICKED;
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
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATED_ACCOUNT, properties);
    }

    public static void trackAnalyticsPostListToggleLayout(PostListViewLayoutType viewLayoutType) {
        Map<String, String> properties = new HashMap<>();
        properties.put("post_list_view_layout_type", viewLayoutType.toString());
        AnalyticsTracker.track(AnalyticsTracker.Stat.POST_LIST_VIEW_LAYOUT_TOGGLED, properties);
    }

    private static Map<String, String> createNewsCardProperties(String origin, int version) {
        Map<String, String> properties = new HashMap<>();
        properties.put(NEWS_CARD_ORIGIN, origin);
        properties.put(NEWS_CARD_VERSION, String.valueOf(version));
        return properties;
    }

    public static void trackLoginProloguePages(int page) {
        Map<String, Integer> properties = new HashMap<>();
        properties.put("page_number", page);
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED, properties);
    }

    @VisibleForTesting
    protected enum AnalyticsSiteType {
        BLOG {
            public String toString() {
                return "blog";
            }
        },
        P2 {
            public String toString() {
                return "p2";
            }
        };

        static AnalyticsSiteType fromSiteModel(SiteModel siteModel) {
            if (siteModel.isWpForTeamsSite()) {
                return P2;
            }

            return BLOG;
        }

        static AnalyticsSiteType fromReaderPost(ReaderPost readerPost) {
            if (readerPost.isP2orA8C()) {
                return P2;
            }

            return BLOG;
        }

        static String toStringFromSiteModel(SiteModel siteModel) {
            return fromSiteModel(siteModel).toString();
        }

        static String toStringFromReaderPost(ReaderPost readerPost) {
            return fromReaderPost(readerPost).toString();
        }
    }

    public enum AnalyticsCommentActionSource {
        NOTIFICATIONS {
            public String toString() {
                return "notifications";
            }
        },
        SITE_COMMENTS {
            public String toString() {
                return "site_comments";
            }
        },
        READER {
            public String toString() {
                return "reader";
            }
        }
    }

    public static void trackCommentActionWithSiteDetails(AnalyticsTracker.Stat stat,
                                                         AnalyticsCommentActionSource actionSource, SiteModel site) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(COMMENT_ACTION_SOURCE, actionSource.toString());

        AnalyticsUtils.trackWithSiteDetails(stat, site, properties);
    }

    public static void trackCommentActionWithReaderPostDetails(AnalyticsTracker.Stat stat,
                                                               AnalyticsCommentActionSource actionSource,
                                                               @Nullable ReaderPost post) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(COMMENT_ACTION_SOURCE, actionSource.toString());

        AnalyticsUtils.trackWithReaderPostDetails(stat, post, properties);
    }

    public static void trackFollowCommentsWithReaderPostDetails(
            AnalyticsTracker.Stat stat,
            long blogId,
            long postId,
            @Nullable ReaderPost post,
            @NonNull Map<String, Object> properties
    ) {
        if (post != null) {
            AnalyticsUtils.trackWithReaderPostDetails(stat, post, properties);
        } else {
            AppLog.w(AppLog.T.READER, "The passed post obj is null. Tracking analytics without post details info");

            properties.put(BLOG_ID_KEY, blogId);
            properties.put(POST_ID_KEY, postId);

            AnalyticsTracker.track(stat, properties);
        }
    }

    public static void trackInviteLinksAction(
            AnalyticsTracker.Stat stat,
            @Nullable SiteModel site,
            @Nullable Map<String, Object> properties
    ) {
        AnalyticsUtils.trackWithSiteDetails(stat, site, properties);
    }

    public static void trackUserProfileShown(String source) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_KEY, source);

        AnalyticsTracker.track(Stat.USER_PROFILE_SHEET_SHOWN, properties);
    }

    public static void trackUserProfileSiteShown() {
        AnalyticsTracker.track(Stat.USER_PROFILE_SHEET_SITE_SHOWN);
    }

    public static void trackBlogPreviewedByUrl(String source) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_KEY, source);

        AnalyticsTracker.track(Stat.BLOG_URL_PREVIEWED, properties);
    }

    public static void trackLikeListOpened(String source, String listType) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_KEY, source);
        properties.put(LIST_TYPE_KEY, listType);

        AnalyticsTracker.track(Stat.LIKE_LIST_OPENED, properties);
    }

    public static void trackLikeListFetchedMore(String source,
                                                String listType,
                                                int nextPage,
                                                int perPage) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_KEY, source);
        properties.put(LIST_TYPE_KEY, listType);
        properties.put(PAGE_KEY, nextPage);
        properties.put(PER_PAGE_KEY, perPage);

        AnalyticsTracker.track(Stat.LIKE_LIST_FETCHED_MORE, properties);
    }

    public static void trackStorageWarningDialogEvent(Stat stat, String source, Boolean isStorageSettingsResolved) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_KEY, source);
        properties.put(IS_STORAGE_SETTINGS_RESOLVED_KEY, isStorageSettingsResolved ? "true" : "false");

        AnalyticsTracker.track(stat, properties);
    }

    public enum RecommendAppSource {
        ME("me"),
        ABOUT("about");

        private final String mSourceName;

        RecommendAppSource(String sourceName) {
            this.mSourceName = sourceName;
        }
    }

    public static void trackRecommendAppEngaged(RecommendAppSource source) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_KEY, source.mSourceName);

        AnalyticsTracker.track(Stat.RECOMMEND_APP_ENGAGED, properties);
    }

    public static void trackRecommendAppFetchFailed(RecommendAppSource source, String error) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(SOURCE_KEY, source.mSourceName);
        properties.put(CAUSE_OF_ISSUE_KEY, error);

        AnalyticsTracker.track(Stat.RECOMMEND_APP_CONTENT_FETCH_FAILED, properties);
    }

    public static void trackBlockEditorEvent(String event, SiteModel site, Map<String, Object> properties) {
        if (event.equals("editor_block_inserted")) {
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_BLOCK_INSERTED, site, properties);
        }

        if (event.equals("editor_block_moved")) {
            AnalyticsUtils.trackWithSiteDetails(Stat.EDITOR_BLOCK_MOVED, site, properties);
        }
    }
}
