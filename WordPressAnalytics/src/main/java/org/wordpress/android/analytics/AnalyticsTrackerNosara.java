package org.wordpress.android.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.automattic.android.tracks.TracksClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnalyticsTrackerNosara implements AnalyticsTracker.Tracker {

    private static final String JETPACK_USER = "jetpack_user";
    private static final String NUMBER_OF_BLOGS = "number_of_blogs";
    private static final String TRACKS_ANON_ID = "nosara_tracks_anon_id";

    private static final String EVENTS_PREFIX = "wpandroid_";

    private String mWpcomUserName = null;
    private String mAnonID = null; // do not access this variable directly. Use methods.

    private TracksClient mNosaraClient;
    private Context mContext;

    public AnalyticsTrackerNosara(Context context) {
        if (null == context) {
            mNosaraClient = null;
            return;
        }
        mContext = context;
        mNosaraClient = TracksClient.getClient(context);
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

        Map<String, Object> predefinedEventProperties = new HashMap<String, Object>();
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
                eventName = "themes_theme_browser_accessed";
                break;
            case THEMES_CHANGED_THEME:
                eventName = "themes_theme_changed";
                break;
            case THEMES_PREVIEWED_SITE:
                eventName = "themes_theme_for_site_previewed";
                break;
            case READER_ACCESSED:
                eventName = "reader_accessed";
                break;
            case READER_OPENED_ARTICLE:
                eventName = "reader_article_opened";
                break;
            case READER_LIKED_ARTICLE:
                eventName = "reader_article_liked";
                break;
            case READER_REBLOGGED_ARTICLE:
                eventName = "reader_article_reblogged";
                break;
            case READER_INFINITE_SCROLL:
                eventName = "reader_infinite_scroll_performed";
                break;
            case READER_FOLLOWED_READER_TAG:
                eventName = "reader_reader_tag_followed";
                break;
            case READER_UNFOLLOWED_READER_TAG:
                eventName = "reader_reader_tag_unfollowed";
                break;
            case READER_LOADED_TAG:
                eventName = "reader_tag_loaded";
                break;
            case READER_LOADED_FRESHLY_PRESSED:
                eventName = "reader_freshly_pressed_loaded";
                break;
            case READER_COMMENTED_ON_ARTICLE:
                eventName = "reader_article_commented_on";
                break;
            case READER_FOLLOWED_SITE:
                eventName = "reader_site_followed";
                break;
            case READER_BLOCKED_BLOG:
                eventName = "reader_blog_blocked";
                break;
            case READER_BLOG_PREVIEW:
                eventName = "reader_blog_previewed";
                break;
            case READER_TAG_PREVIEW:
                eventName = "reader_tag_previewed";
                break;
            case EDITOR_CREATED_POST:
                eventName = "editor_post_created";
                break;
            case EDITOR_SAVED_DRAFT:
                eventName = "editor_draft_saved";
                break;
            case EDITOR_CLOSED_POST:
                eventName = "editor_closed";
                break;
            case EDITOR_ADDED_PHOTO_VIA_LOCAL_LIBRARY:
                eventName = "editor_photo_added";
                predefinedEventProperties.put("via", "local_library");
                break;
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                eventName = "editor_photo_added";
                predefinedEventProperties.put("via", "wp_media_library");
                break;
            case EDITOR_PUBLISHED_POST:
                eventName = "editor_post_published";
                break;
            case EDITOR_UPDATED_POST:
                eventName = "editor_post_updated";
                break;
            case EDITOR_SCHEDULED_POST:
                eventName = "editor_post_scheduled";
                break;
            case EDITOR_PUBLISHED_POST_WITH_PHOTO:
                eventName = "editor_post_published";
                predefinedEventProperties.put("with_photos", true);
                break;
            case EDITOR_PUBLISHED_POST_WITH_VIDEO:
                eventName = "editor_post_published";
                predefinedEventProperties.put("with_videos", true);
                break;
            case EDITOR_PUBLISHED_POST_WITH_CATEGORIES:
                eventName = "editor_post_published";
                predefinedEventProperties.put("with_categories", true);
                break;
            case EDITOR_PUBLISHED_POST_WITH_TAGS:
                eventName = "editor_post_published";
                predefinedEventProperties.put("with_tags", true);
                break;
            case EDITOR_TAPPED_BLOCKQUOTE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "blockquote");
                break;
            case EDITOR_TAPPED_BOLD:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "bold");
                break;
            case EDITOR_TAPPED_IMAGE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "image");
                break;
            case EDITOR_TAPPED_ITALIC:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "italic");
                break;
            case EDITOR_TAPPED_LINK:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "link");
                break;
            case EDITOR_TAPPED_MORE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "more");
                break;
            case EDITOR_TAPPED_STRIKETHROUGH:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "strikethrough");
                break;
            case EDITOR_TAPPED_UNDERLINE:
                eventName = "editor_button_tapped";
                predefinedEventProperties.put("button", "underline");
                break;
            case NOTIFICATIONS_ACCESSED:
                eventName = "notifications_accessed";
                break;
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                eventName = "notifications_notification_details_opened";
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
                eventName = "notifications_comment_liked";
                break;
            case NOTIFICATION_UNLIKED:
                eventName = "notifications_comment_unliked";
                break;
            case OPENED_POSTS:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "posts");
                break;
            case OPENED_PAGES:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "pages");
                break;
            case OPENED_COMMENTS:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "media_library");
                break;
            case OPENED_VIEW_SITE:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "view_site");
                break;
            case OPENED_VIEW_ADMIN:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "view_admin");
                break;
            case OPENED_MEDIA_LIBRARY:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "media_library");
                break;
            case OPENED_SETTINGS:
                eventName = "site_menu_opened";
                predefinedEventProperties.put("menu_item", "settings");
                break;
            case CREATED_ACCOUNT:
                eventName = "account_created";
                break;
            case SHARED_ITEM:
                eventName = "item_shared";
                break;
            case ADDED_SELF_HOSTED_SITE:
                eventName = "self_hosted_blog_added";
                break;
            case SIGNED_IN:
                eventName = "signed_in";
                break;
            case SIGNED_INTO_JETPACK:
                eventName = "signed_into_jetpack";
                break;
            case PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN:
                eventName = "stats_screen_signed_into_jetpack";
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
                eventName = "stats_web_version_accessed";
                break;
            case STATS_TAPPED_BAR_CHART:
                eventName = "stats_bar_chart_tapped";
                break;
            case STATS_SCROLLED_TO_BOTTOM:
                eventName = "stats_scrolled_to_bottom";
                break;
            case STATS_SELECTED_INSTALL_JETPACK:
                eventName = "stats_install_jetpack_selected";
                break;
            case PUSH_NOTIFICATION_RECEIVED:
                eventName = "push_notification_received";
                break;
            case SUPPORT_OPENED_HELPSHIFT_SCREEN:
                eventName = "support_helpshift_screen_opened";
                break;
            case SUPPORT_SENT_REPLY_TO_SUPPORT_MESSAGE:
                eventName = "support_reply_to_support_message_sent";
                break;
            case LOGIN_FAILED:
                eventName = "login_failed_to_login";
                break;
            case LOGIN_FAILED_TO_GUESS_XMLRPC:
                eventName = "login_failed_to_guess_xmlrpc";
                break;
            default:
                eventName = null;
                break;
        }

        if (eventName == null) {
            AppLog.w(AppLog.T.STATS, "There is NO match for the event " + stat.name() + "stat");
            return;
        }

        final String user;
        final TracksClient.NosaraUserType userType;
        if (mWpcomUserName != null) {
            user = mWpcomUserName;
            userType = TracksClient.NosaraUserType.WPCOM;
        } else {
            // This is just a security checks since the anonID is already available here.
            // refresh metadata is called on login/logout/startup and it loads/generates the anonId when necessary.
            if (getAnonID() == null) {
                user = generateNewAnonID();
            } else {
                user = getAnonID();
            }
            userType = TracksClient.NosaraUserType.ANON;
        }


        // create the merged JSON Object of properties
        // Properties defined by the user have precedence over the default ones pre-defined at "event level"
        final JSONObject propertiesToJSON;
        if (properties != null && properties.size() > 0) {
            propertiesToJSON = new JSONObject(properties);
            for (String key : predefinedEventProperties.keySet()) {
                try {
                    if (propertiesToJSON.has(key)) {
                        AppLog.w(AppLog.T.STATS, "The user has defined a property named: '" + key + "' that will override" +
                                "the same property pre-defined at event level. This may generate unexpected behavior!!");
                        AppLog.w(AppLog.T.STATS, "User value: " + propertiesToJSON.get(key).toString() + " - pre-defined value: " +
                                predefinedEventProperties.get(key).toString());
                    } else {
                        propertiesToJSON.put(key, predefinedEventProperties.get(key));
                    }
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, "Error while merging user-defined properties with pre-defined properties", e);
                }
            }
        } else{
            propertiesToJSON = new JSONObject(predefinedEventProperties);
        }

        if (propertiesToJSON.length() > 0) {
            mNosaraClient.track(EVENTS_PREFIX + eventName, propertiesToJSON, user, userType);
        } else {
            mNosaraClient.track(EVENTS_PREFIX + eventName, user, userType);
        }
    }

    private void clearAnonID() {
        mAnonID = null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (preferences.contains(TRACKS_ANON_ID)) {
            final SharedPreferences.Editor editor = preferences.edit();
            editor.remove(TRACKS_ANON_ID);
            editor.commit();
        }
    }

    private String getAnonID() {
        if (mAnonID == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            mAnonID = preferences.getString(TRACKS_ANON_ID, null);
        }
        return mAnonID;
    }

    private String generateNewAnonID() {
        String uuid = UUID.randomUUID().toString();
        String[] uuidSplitted = uuid.split("-");
        StringBuilder builder = new StringBuilder();
        for (String currentPart : uuidSplitted) {
            builder.append(currentPart);
        }
        uuid = builder.toString();
        AppLog.d(AppLog.T.STATS, "New anon ID generated: " + uuid);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(TRACKS_ANON_ID, uuid);
        editor.commit();

        mAnonID = uuid;
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
    public void refreshMetadata(boolean isUserConnected, boolean isWordPressComUser, boolean isJetpackUser,
                                int sessionCount, int numBlogs, int versionCode, String username, String email) {
        if (mNosaraClient == null) {
            return;
        }

        if (isUserConnected && isWordPressComUser) {
            mWpcomUserName = username;
            // Re-unify the user
            if (getAnonID() != null) {
                mNosaraClient.trackAliasUser(mWpcomUserName, getAnonID());
                clearAnonID();
            }
        } else {
            // Not wpcom connected. Check if anonID is already present
            mWpcomUserName = null;
            if (getAnonID() == null) {
                generateNewAnonID();
            }
        }

        try {
            JSONObject properties = new JSONObject();
            properties.put(JETPACK_USER, isJetpackUser);
            properties.put(NUMBER_OF_BLOGS, numBlogs);
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
        // Reset the anon ID here
        clearAnonID();
        mWpcomUserName = null;
    }

    @Override
    public void registerPushNotificationToken(String regId) {
        return;
    }
}
