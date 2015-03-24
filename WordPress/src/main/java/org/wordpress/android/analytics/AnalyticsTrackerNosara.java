package org.wordpress.android.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.automattic.android.tracks.TracksClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

import java.util.Map;
import java.util.UUID;

public class AnalyticsTrackerNosara implements AnalyticsTracker.Tracker {
    public static final String LOGTAG = "AnalyticsTrackerNosara";

    private static final String JETPACK_USER = "jetpack_user";
    private static final String MIXPANEL_NUMBER_OF_BLOGS = "number_of_blogs";

    private static final String EVENTS_PREFIX = "wpandroid_";

    private String mAnonID = null;
    private String mWpcomUserName = null;

    private TracksClient mNosaraClient;

    public AnalyticsTrackerNosara(Context ctx) {
        if (null == ctx) {
            mNosaraClient = null;
            return;
        }
        mNosaraClient = TracksClient.getClient(ctx);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat) {
        track(stat, null);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {
        if (mNosaraClient == null) {
            return;
        }

        String eventName;

        switch (stat) {
            case APPLICATION_STARTED:
                eventName = "application_started";
                break;
            case APPLICATION_OPENED:
                eventName = "application_opened";
                break;
            case APPLICATION_CLOSED:
                eventName = "application_closed";
                break;
            case THEMES_ACCESSED_THEMES_BROWSER:
                eventName = "themes_accessed_theme_browser";
                break;
            case THEMES_CHANGED_THEME:
                eventName = "themes_changed_theme";
                break;
            case THEMES_PREVIEWED_SITE:
                eventName = "themes_previewed_theme_for_site";
                break;
            case READER_ACCESSED:
                eventName = "reader_accessed";
                break;
            case READER_OPENED_ARTICLE:
                eventName = "reader_opened_article";
                break;
            case READER_LIKED_ARTICLE:
                eventName = "reader_liked_article";
                break;
            case READER_REBLOGGED_ARTICLE:
                eventName = "reader_reblogged_article";
                break;
            case READER_INFINITE_SCROLL:
                eventName = "reader_infinite_scroll_performed";
                break;
            case READER_FOLLOWED_READER_TAG:
                eventName = "reader_followed_reader_tag";
                break;
            case READER_UNFOLLOWED_READER_TAG:
                eventName = "reader_unfollowed_reader_tag";
                break;
            case READER_LOADED_TAG:
                eventName = "reader_loaded_tag";
                break;
            case READER_LOADED_FRESHLY_PRESSED:
                eventName = "reader_loaded_freshly_pressed";
                break;
            case READER_COMMENTED_ON_ARTICLE:
                eventName = "reader_commented_on_article";
                break;
            case READER_FOLLOWED_SITE:
                eventName = "reader_followed_site";
                break;
            case READER_BLOCKED_BLOG:
                eventName = "reader_blocked_blog";
                break;
            case READER_BLOG_PREVIEW:
                eventName = "reader_blog_preview";
                break;
            case READER_TAG_PREVIEW:
                eventName = "reader_tag_preview";
                break;
            case EDITOR_CREATED_POST:
                eventName = "editor_created_post";
                break;
            case EDITOR_SAVED_DRAFT:
                eventName = "editor_saved_draft";
                break;
            case EDITOR_CLOSED_POST:
                eventName = "editor_closed";
                break;
            case EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY:
                eventName = "editor_added_photo_via_local_library";
                break;
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                eventName = "editor_added_photo_via_wp_media_library";
                break;
            case EDITOR_PUBLISHED_POST:
                eventName = "editor_published_post";
                break;
            case EDITOR_UPDATED_POST:
                eventName = "editor_update_post";
                break;
            case EDITOR_SCHEDULED_POST:
                eventName = "editor_scheduled_post";
                break;
            case EDITOR_PUBLISHED_POST_WITH_PHOTO:
                eventName = "editor_published_post_with_photos";
                break;
            case EDITOR_PUBLISHED_POST_WITH_VIDEO:
                eventName = "editor_published_post_with_videos";
                break;
            case EDITOR_PUBLISHED_POST_WITH_CATEGORIES:
                eventName = "editor_published_post_with_categories";
                break;
            case EDITOR_PUBLISHED_POST_WITH_TAGS:
                eventName = "editor_published_post_with_tags";
                break;
            case EDITOR_TAPPED_BLOCKQUOTE:
                eventName = "editor_tapped_blockquote_button";
                break;
            case EDITOR_TAPPED_BOLD:
                eventName = "editor_tapped_bold_button";
                break;
            case EDITOR_TAPPED_IMAGE:
                eventName = "editor_tapped_image_button";
                break;
            case EDITOR_TAPPED_ITALIC:
                eventName = "editor_tapped_italic_button";
                break;
            case EDITOR_TAPPED_LINK:
                eventName = "editor_tapped_link_button";
                break;
            case EDITOR_TAPPED_MORE:
                eventName = "editor_tapped_more_button";
                break;
            case EDITOR_TAPPED_STRIKETHROUGH:
                eventName = "editor_tapped_strikethrough_button";
                break;
            case EDITOR_TAPPED_UNDERLINE:
                eventName = "editor_tapped_underline_button";
                break;
            case NOTIFICATIONS_ACCESSED:
                eventName = "notifications_accessed";
                break;
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                eventName = "notifications_opened_notification_details";
                break;
            case NOTIFICATION_APPROVED:
                eventName = "notifications_approved";
                break;
            case NOTIFICATION_UNAPPROVED:
                eventName = "notifications_unapproved";
                break;
            case NOTIFICATION_REPLIED_TO:
                eventName = "notifications_replied_to";
                break;
            case NOTIFICATION_TRASHED:
                eventName = "notifications_trashed";
                break;
            case NOTIFICATION_FLAGGED_AS_SPAM:
                eventName = "notifications_flagged_as_spam";
                break;
            case NOTIFICATION_LIKED:
                eventName = "notifications_liked_comment";
                break;
            case NOTIFICATION_UNLIKED:
                eventName = "notifications_unliked_comment";
                break;
            case OPENED_POSTS:
                eventName = "site_menu_opened_posts";
                break;
            case OPENED_PAGES:
                eventName = "site_menu_opened_pages";
                break;
            case OPENED_COMMENTS:
                eventName = "site_menu_opened_comments";
                break;
            case OPENED_VIEW_SITE:
                eventName = "site_menu_opened_view_site";
                break;
            case OPENED_VIEW_ADMIN:
                eventName = "site_menu_opened_view_admin";
                break;
            case OPENED_MEDIA_LIBRARY:
                eventName = "site_menu_opened_media_library";
                break;
            case OPENED_SETTINGS:
                eventName = "site_menu_opened_settings";
                break;
            case CREATED_ACCOUNT:
                eventName = "created_account";
                break;
            case SHARED_ITEM:
                eventName = "shared_item";
                break;
            case ADDED_SELF_HOSTED_SITE:
                eventName = "added_self_hosted_blog";
                break;
            case SIGNED_IN:
                eventName = "signed_in";
                break;
            case SIGNED_INTO_JETPACK:
                eventName = "signed_into_jetpack";
                break;
            case PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN:
                eventName = "signed_into_jetpack_from_stats_screen";
                break;
            case STATS_ACCESSED:
                eventName = "stats_accessed";
                break;
            case STATS_VIEW_ALL_ACCESSED:
                eventName = "stats_view_all_accessed";
                break;
            case STATS_SINGLE_POST_ACCESSED:
                eventName = "stats_single_post_accessed";
                break;
            case STATS_OPENED_WEB_VERSION:
                eventName = "stats_opened_web_version_accessed";
                break;
            case STATS_TAPPED_BAR_CHART:
                eventName = "stats_tapped_bar_chart";
                break;
            case STATS_SCROLLED_TO_BOTTOM:
                eventName = "stats_scrolled_to_bottom";
                break;
            case STATS_SELECTED_INSTALL_JETPACK:
                eventName = "stats_selected_install_jetpack";
                break;
            case PUSH_NOTIFICATION_RECEIVED:
                eventName = "push_notification_received";
                break;
            case SUPPORT_OPENED_HELPSHIFT_SCREEN:
                eventName = "support_opened_helpshift_screen";
                break;
            case LOGIN_FAILED:
                eventName = "login_failed_login";
                break;
            case LOGIN_FAILED_TO_GUESS_XMLRPC:
                eventName = "login_failed_to_guess_xmlrpc";
                break;
            default:
                eventName = null;
                break;
        }


        if (eventName == null) {
            Log.w(LOGTAG, "There is NO match for the event " + stat.name() + "stat");
            return;
        }

        if (shouldGenerateAnonID()) {
            mAnonID = getNewAnonID();
        }

        final String user = mAnonID != null ? mAnonID : mWpcomUserName;
        TracksClient.NosaraUserType userType = mAnonID != null ?
                TracksClient.NosaraUserType.ANON :
                TracksClient.NosaraUserType.WPCOM;

        if (properties != null && properties.size() > 0) {
            JSONObject propertiesToJSON = new JSONObject(properties);
            mNosaraClient.track(EVENTS_PREFIX + eventName, propertiesToJSON, user, userType);
        } else {
            mNosaraClient.track(EVENTS_PREFIX + eventName, user, userType);
        }
    }

    @Override
    public void beginSession() {
        if (mNosaraClient == null) {
            return;
        }

        refreshMetadata();
    }

    private boolean shouldGenerateAnonID() {
        return (mAnonID == null && mWpcomUserName == null);
    }

    private String getNewAnonID() {
        String uuid = UUID.randomUUID().toString();
        String[] uuidSplitted = uuid.split("-");
        StringBuilder builder = new StringBuilder();
        for (String currentPart : uuidSplitted) {
            builder.append(currentPart);
        }
        uuid = builder.toString();
        Log.d(LOGTAG, "anon UUID generato " + uuid);
        return uuid;
    }

    @Override
    public void endSession() {
        if (mNosaraClient == null) {
            return;
        }
        mNosaraClient.flush();
    }

    @Override
    public void refreshMetadata() {
        if (mNosaraClient == null) {
            return;
        }

        boolean connected = WordPress.hasDotComToken(WordPress.getContext());
        if (connected) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
            String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            mWpcomUserName = username;
            // Re-unify the user
            if (mAnonID != null) {
                mNosaraClient.trackAliasUser(mWpcomUserName, mAnonID);
                mAnonID = null;
            }
        } else {
            // Not wpcom connected. Check if mAnon is already present
            mWpcomUserName = null;
            if (shouldGenerateAnonID()) {
                mAnonID = getNewAnonID();
            }
        }

        boolean jetpackUser = WordPress.wpDB.hasAnyJetpackBlogs();
        int numBlogs = WordPress.wpDB.getVisibleAccounts().size();
        try {
            JSONObject properties = new JSONObject();
            properties.put(JETPACK_USER, jetpackUser);
            properties.put(MIXPANEL_NUMBER_OF_BLOGS, numBlogs);
            mNosaraClient.registerUserProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }
    }

    @Override
    public void clearAllData() {
        if (mNosaraClient == null) {
            return;
        }
        mNosaraClient.clearUserProperties();

        // Reset the anon token here
        mAnonID = null;
        mWpcomUserName = null;
    }

    @Override
    public void registerPushNotificationToken(String regId) {
        return;
    }
}