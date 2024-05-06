package org.wordpress.android.analytics;

import android.content.Context;
import android.text.TextUtils;

import com.automattic.android.tracks.TracksClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsTrackerNosara extends Tracker {
    private static final String JETPACK_USER = "jetpack_user";
    private static final String NUMBER_OF_BLOGS = "number_of_blogs";
    private static final String TRACKS_ANON_ID = "nosara_tracks_anon_id";
    @SuppressWarnings("checkstyle:RegexpSingleline")
    private static final String WPCOM_USER = "dotcom_user";
    private static final String IS_GUTENBERG_ENABLED = "gutenberg_enabled";
    private static final String APP_SCHEME = "app_scheme";

    private final String mEventsPrefix;

    private final TracksClient mNosaraClient;

    public AnalyticsTrackerNosara(Context context, String eventsPrefix) throws IllegalArgumentException {
        super(context);
        mEventsPrefix = eventsPrefix;
        mNosaraClient = TracksClient.getClient(context);
    }

    String getAnonIdPrefKey() {
        return TRACKS_ANON_ID;
    }

    @Override
    public void track(AnalyticsTracker.Stat stat) {
        track(stat, null);
    }

    @Override
    @SuppressWarnings("checkstyle:methodlength")
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {
        if (mNosaraClient == null) {
            return;
        }

        if (!isValidEvent(stat)) {
            return;
        }

        String eventName = stat.getEventName();

        Map<String, Object> predefinedEventProperties = new HashMap<>();
        switch (stat) {
            case EDITOR_ADDED_PHOTO_NEW:
            case EDITOR_ADDED_VIDEO_NEW:
                predefinedEventProperties.put("via", "device_camera");
                break;
            case EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY:
            case EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY:
                predefinedEventProperties.put("via", "device_library");
                break;
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
            case EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY:
                predefinedEventProperties.put("via", "media_library");
                break;
            case EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY:
                predefinedEventProperties.put("via", "stock_photos");
                break;
            case EDITOR_ADDED_PHOTO_VIA_MEDIA_EDITOR:
                predefinedEventProperties.put("via", "media_editor");
                break;
            case EDITOR_TAPPED_BLOCKQUOTE:
                predefinedEventProperties.put("button", "blockquote");
                break;
            case EDITOR_TAPPED_BOLD:
                predefinedEventProperties.put("button", "bold");
                break;
            case EDITOR_TAPPED_ELLIPSIS_COLLAPSE:
                predefinedEventProperties.put("button", "overflow_ellipsis");
                predefinedEventProperties.put("action", "made_hidden");
                break;
            case EDITOR_TAPPED_ELLIPSIS_EXPAND:
                predefinedEventProperties.put("button", "overflow_ellipsis");
                predefinedEventProperties.put("action", "made_visible");
                break;
            case EDITOR_TAPPED_HEADING:
                predefinedEventProperties.put("button", "header");
                break;
            case EDITOR_TAPPED_HEADING_1:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h1");
                break;
            case EDITOR_TAPPED_HEADING_2:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h2");
                break;
            case EDITOR_TAPPED_HEADING_3:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h3");
                break;
            case EDITOR_TAPPED_HEADING_4:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h4");
                break;
            case EDITOR_TAPPED_HEADING_5:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h5");
                break;
            case EDITOR_TAPPED_HEADING_6:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "h6");
                break;
            case EDITOR_TAPPED_IMAGE:
                predefinedEventProperties.put("button", "image");
                break;
            case EDITOR_TAPPED_ITALIC:
                predefinedEventProperties.put("button", "italic");
                break;
            case EDITOR_TAPPED_LINK_ADDED:
                predefinedEventProperties.put("button", "link");
                break;
            case EDITOR_TAPPED_LIST:
                predefinedEventProperties.put("button", "list");
                break;
            case EDITOR_TAPPED_LIST_ORDERED:
                predefinedEventProperties.put("button", "ordered_list");
                break;
            case EDITOR_TAPPED_LIST_UNORDERED:
                predefinedEventProperties.put("button", "unordered_list");
                break;
            case EDITOR_TAPPED_NEXT_PAGE:
                predefinedEventProperties.put("button", "next_page");
                break;
            case EDITOR_TAPPED_PARAGRAPH:
                predefinedEventProperties.put("button", "header_selection");
                predefinedEventProperties.put("heading_style", "none");
                break;
            case EDITOR_TAPPED_PREFORMAT:
                predefinedEventProperties.put("button", "preformat");
                break;
            case EDITOR_TAPPED_READ_MORE:
                predefinedEventProperties.put("button", "more");
                break;
            case EDITOR_TAPPED_STRIKETHROUGH:
                predefinedEventProperties.put("button", "strikethrough");
                break;
            case EDITOR_TAPPED_UNDERLINE:
                predefinedEventProperties.put("button", "underline");
                break;
            case EDITOR_TAPPED_HTML:
                predefinedEventProperties.put("button", "html");
                break;
            case EDITOR_TAPPED_ALIGN_LEFT:
                predefinedEventProperties.put("button", "align_left");
                break;
            case EDITOR_TAPPED_ALIGN_CENTER:
                predefinedEventProperties.put("button", "align_center");
                break;
            case EDITOR_TAPPED_ALIGN_RIGHT:
                predefinedEventProperties.put("button", "align_right");
                break;
            case EDITOR_TAPPED_UNDO:
                predefinedEventProperties.put("button", "undo");
                break;
            case EDITOR_TAPPED_REDO:
                predefinedEventProperties.put("button", "redo");
                break;
            case EDITOR_TAPPED_HORIZONTAL_RULE:
                predefinedEventProperties.put("button", "horizontal_rule");
                break;
            case REVISIONS_DETAIL_VIEWED_FROM_LIST:
                predefinedEventProperties.put("source", "list");
                break;
            case REVISIONS_DETAIL_VIEWED_FROM_SWIPE:
                predefinedEventProperties.put("source", "swipe");
                break;
            case REVISIONS_DETAIL_VIEWED_FROM_CHEVRON:
                predefinedEventProperties.put("source", "chevron");
                break;
            case OPENED_POSTS:
                predefinedEventProperties.put("menu_item", "posts");
                break;
            case OPENED_PAGES:
                predefinedEventProperties.put("menu_item", "pages");
                break;
            case OPENED_COMMENTS:
                predefinedEventProperties.put("menu_item", "comments");
                break;
            case OPENED_VIEW_SITE:
                predefinedEventProperties.put("menu_item", "view_site");
                break;
            case OPENED_VIEW_SITE_FROM_HEADER:
                predefinedEventProperties.put("menu_item", "view_site_from_header");
                break;
            case OPENED_VIEW_ADMIN:
                predefinedEventProperties.put("menu_item", "view_admin");
                break;
            case OPENED_MEDIA_LIBRARY:
                predefinedEventProperties.put("menu_item", "media_library");
                break;
            case OPENED_BLOG_SETTINGS:
                predefinedEventProperties.put("menu_item", "site_settings");
                break;
            case OPENED_PLANS:
                predefinedEventProperties.put("menu_item", "plans");
                break;
            case OPENED_SHARING_MANAGEMENT:
                predefinedEventProperties.put("menu_item", "sharing_management");
                break;
            case STATS_PERIOD_DAYS_ACCESSED:
                predefinedEventProperties.put("period", "days");
                break;
            case STATS_PERIOD_WEEKS_ACCESSED:
                predefinedEventProperties.put("period", "weeks");
                break;
            case STATS_PERIOD_MONTHS_ACCESSED:
                predefinedEventProperties.put("period", "months");
                break;
            case STATS_PERIOD_YEARS_ACCESSED:
                predefinedEventProperties.put("period", "years");
                break;
            case NOTIFICATION_QUICK_ACTIONS_LIKED:
            case NOTIFICATION_QUICK_ACTIONS_REPLIED_TO:
            case NOTIFICATION_QUICK_ACTIONS_APPROVED:
            case COMMENT_QUICK_ACTION_LIKED:
            case COMMENT_QUICK_ACTION_REPLIED_TO:
            case COMMENT_QUICK_ACTION_APPROVED:
                predefinedEventProperties.put("is_quick_action", true);
                break;
            case SIGNUP_EMAIL_EPILOGUE_UNCHANGED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_FAILED:
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_TAPPED:
            case SIGNUP_EMAIL_EPILOGUE_VIEWED:
                predefinedEventProperties.put("source", "email");
                break;
            case SIGNUP_SOCIAL_EPILOGUE_UNCHANGED:
            case SIGNUP_SOCIAL_BUTTON_FAILURE:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_FAILED:
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_TAPPED:
            case SIGNUP_SOCIAL_EPILOGUE_VIEWED:
                predefinedEventProperties.put("source", "social");
                break;
            case SIGNUP_SOCIAL_BUTTON_TAPPED:
                predefinedEventProperties.put("source", "google");
                break;
            case READER_POST_SAVED_FROM_OTHER_POST_LIST:
                predefinedEventProperties.put("source", "other_post_list");
                break;
            case READER_POST_SAVED_FROM_SAVED_POST_LIST:
                predefinedEventProperties.put("source", "saved_post_list");
                break;
            case READER_POST_SAVED_FROM_DETAILS:
                predefinedEventProperties.put("source", "post_details");
                break;
            case READER_POST_UNSAVED_FROM_OTHER_POST_LIST:
                predefinedEventProperties.put("source", "other_post_list");
                break;
            case READER_POST_UNSAVED_FROM_SAVED_POST_LIST:
                predefinedEventProperties.put("source", "saved_post_list");
                break;
            case READER_POST_UNSAVED_FROM_DETAILS:
                predefinedEventProperties.put("source", "post_details");
                break;
            case READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST:
                predefinedEventProperties.put("source", "saved_post_list");
                break;
            case READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST:
                predefinedEventProperties.put("source", "other_post_list");
                break;
            case QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED:
                predefinedEventProperties.put("type", "negative");
                break;
            case QUICK_START_TASK_DIALOG_POSITIVE_TAPPED:
                predefinedEventProperties.put("type", "positive");
                break;
            case QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED:
                predefinedEventProperties.put("type", "negative");
                break;
            case QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED:
                predefinedEventProperties.put("type", "positive");
                break;
            case QUICK_START_LIST_CREATE_SITE_TAPPED:
                predefinedEventProperties.put("task_name", "create_site");
                break;
            case QUICK_START_LIST_UPDATE_SITE_TITLE_TAPPED:
                predefinedEventProperties.put("task_name", "update_site_title");
                break;
            case QUICK_START_TYPE_CUSTOMIZE_VIEWED:
                predefinedEventProperties.put("type", "customize");
                break;
            case QUICK_START_TYPE_GROW_VIEWED:
                predefinedEventProperties.put("type", "grow");
                break;
            case QUICK_START_TYPE_GET_TO_KNOW_APP_VIEWED:
                predefinedEventProperties.put("type", "get_to_know_app");
                break;
            case QUICK_START_TYPE_CUSTOMIZE_DISMISSED:
                predefinedEventProperties.put("type", "customize");
                break;
            case QUICK_START_TYPE_GROW_DISMISSED:
                predefinedEventProperties.put("type", "grow");
                break;
            case QUICK_START_TYPE_GET_TO_KNOW_APP_DISMISSED:
                predefinedEventProperties.put("type", "get_to_know_app");
                break;
            case QUICK_START_LIST_CREATE_SITE_SKIPPED:
                predefinedEventProperties.put("task_name", "create_site");
                break;
            case QUICK_START_LIST_UPDATE_SITE_TITLE_SKIPPED:
                predefinedEventProperties.put("task_name", "update_site_title");
                break;
            case QUICK_START_LIST_VIEW_SITE_SKIPPED:
                predefinedEventProperties.put("task_name", "view_site");
                break;
            case QUICK_START_LIST_ADD_SOCIAL_SKIPPED:
                predefinedEventProperties.put("task_name", "share_site");
                break;
            case QUICK_START_LIST_PUBLISH_POST_SKIPPED:
                predefinedEventProperties.put("task_name", "publish_post");
                break;
            case QUICK_START_LIST_FOLLOW_SITE_SKIPPED:
                predefinedEventProperties.put("task_name", "follow_site");
                break;
            case QUICK_START_LIST_UPLOAD_ICON_SKIPPED:
                predefinedEventProperties.put("task_name", "upload_icon");
                break;
            case QUICK_START_LIST_CHECK_STATS_SKIPPED:
                predefinedEventProperties.put("task_name", "check_stats");
                break;
            case QUICK_START_LIST_VIEW_SITE_TAPPED:
                predefinedEventProperties.put("task_name", "view_site");
                break;
            case QUICK_START_LIST_ADD_SOCIAL_TAPPED:
                predefinedEventProperties.put("task_name", "share_site");
                break;
            case QUICK_START_LIST_PUBLISH_POST_TAPPED:
                predefinedEventProperties.put("task_name", "publish_post");
                break;
            case QUICK_START_LIST_FOLLOW_SITE_TAPPED:
                predefinedEventProperties.put("task_name", "follow_site");
                break;
            case QUICK_START_LIST_UPLOAD_ICON_TAPPED:
                predefinedEventProperties.put("task_name", "upload_icon");
                break;
            case QUICK_START_LIST_CHECK_STATS_TAPPED:
                predefinedEventProperties.put("task_name", "check_stats");
                break;
            case QUICK_START_CREATE_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "create_site");
                break;
            case QUICK_START_UPDATE_SITE_TITLE_COMPLETED:
                predefinedEventProperties.put("task_name", "update_site_title");
                break;
            case QUICK_START_VIEW_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "view_site");
                break;
            case QUICK_START_SHARE_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "share_site");
                break;
            case QUICK_START_PUBLISH_POST_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "publish_post");
                break;
            case QUICK_START_FOLLOW_SITE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "follow_site");
                break;
            case QUICK_START_UPLOAD_ICON_COMPLETED:
                predefinedEventProperties.put("task_name", "upload_icon");
                break;
            case QUICK_START_CHECK_STATS_COMPLETED:
                predefinedEventProperties.put("task_name", "check_stats");
                break;
            case QUICK_START_LIST_REVIEW_PAGES_SKIPPED:
            case QUICK_START_LIST_REVIEW_PAGES_TAPPED:
            case QUICK_START_REVIEW_PAGES_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "review_pages");
                break;
            case QUICK_START_LIST_CHECK_NOTIFICATIONS_SKIPPED:
            case QUICK_START_LIST_CHECK_NOTIFICATIONS_TAPPED:
            case QUICK_START_CHECK_NOTIFICATIONS_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "check_notifications");
                break;
            case QUICK_START_LIST_UPLOAD_MEDIA_SKIPPED:
            case QUICK_START_LIST_UPLOAD_MEDIA_TAPPED:
            case QUICK_START_UPLOAD_MEDIA_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "media_upload");
                break;
            case QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED:
                predefinedEventProperties.put("type", "negative");
                break;
            case QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED:
                predefinedEventProperties.put("type", "positive");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA:
                predefinedEventProperties.put("source", "media_upload");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION:
                predefinedEventProperties.put("source", "notification_details");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE:
                predefinedEventProperties.put("source", "publishing_post_or_page");
                break;
            case APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST:
                predefinedEventProperties.put("source", "opening_reader_post");
                break;
            case QUICK_LINK_RIBBON_PAGES_TAPPED:
                predefinedEventProperties.put("button", "pages");
                break;
            case QUICK_LINK_RIBBON_POSTS_TAPPED:
                predefinedEventProperties.put("button", "posts");
                break;
            case QUICK_LINK_RIBBON_MEDIA_TAPPED:
                predefinedEventProperties.put("button", "media");
                break;
            case QUICK_LINK_RIBBON_STATS_TAPPED:
                predefinedEventProperties.put("button", "stats");
                break;
            case WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED:
                predefinedEventProperties.put("button", "create_new_site");
                break;
            case WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED:
                predefinedEventProperties.put("button", "add_self_hosted_site");
                break;
            case FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE:
                predefinedEventProperties.put("source", "app_upgrade");
                break;
            case FEATURE_ANNOUNCEMENT_SHOWN_FROM_APP_SETTINGS:
                predefinedEventProperties.put("source", "app_settings");
                break;
            case FEATURE_ANNOUNCEMENT_CLOSE_DIALOG_BUTTON_TAPPED:
                predefinedEventProperties.put("button", "close_dialog");
                break;
            case FEATURE_ANNOUNCEMENT_FIND_OUT_MORE_TAPPED:
                predefinedEventProperties.put("button", "find_out_more");
                break;
            case READER_ARTICLE_COMMENTED_ON:
                predefinedEventProperties.put("replying_to", "post");
                break;
            case READER_ARTICLE_COMMENT_REPLIED_TO:
                predefinedEventProperties.put("replying_to", "comment");
                break;
        }

        final String user;
        final TracksClient.NosaraUserType userType;
        if (getWordPressComUserName() != null) {
            user = getWordPressComUserName();
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

        // It seems that we're tracking some events with user = null. Make sure we're catching the error here.
        if (user == null) {
            try {
                throw new AnalyticsException("Trying to track analytics with an null user!");
                // TODO add Crash Logging Exception or track this error in Nosara by using a special test user.
            } catch (AnalyticsException e) {
                AppLog.e(AppLog.T.STATS, e);
            }
            return;
        }

        // create the merged JSON Object of properties
        // Properties defined by the user have precedence over the default ones pre-defined at "event level"
        JSONObject propertiesToJSON = null;
        if (properties != null && properties.size() > 0) {
            try {
                propertiesToJSON = new JSONObject(properties);
                for (String key : predefinedEventProperties.keySet()) {
                    try {
                        if (propertiesToJSON.has(key)) {
                            AppLog.w(AppLog.T.STATS,
                                    "The user has defined a property named: '" + key + "' that will override"
                                    + "the same property pre-defined at event level. This may generate unexpected "
                                    + "behavior!!");
                            AppLog.w(AppLog.T.STATS,
                                    "User value: " + propertiesToJSON.get(key)
                                    + " - pre-defined value: "
                                    + predefinedEventProperties.get(key).toString());
                        } else {
                            propertiesToJSON.put(key, predefinedEventProperties.get(key));
                        }
                    } catch (JSONException e) {
                        AppLog.e(AppLog.T.STATS,
                                "Error while merging user-defined properties with pre-defined properties", e);
                    }
                }
            } catch (NullPointerException e) {
                AppLog.e(AppLog.T.STATS, "A property passed to the event " + eventName + " has null key!", e);
            }
        }

        if (propertiesToJSON == null) {
            propertiesToJSON = new JSONObject(predefinedEventProperties);
        }

        if (propertiesToJSON.length() > 0) {
            mNosaraClient.track(mEventsPrefix + eventName, propertiesToJSON, user, userType);
            String jsonString = propertiesToJSON.toString();
            AppLog.i(T.STATS, "\uD83D\uDD35 Tracked: " + eventName + ", Properties: " + jsonString);
        } else {
            mNosaraClient.track(mEventsPrefix + eventName, user, userType);
            AppLog.i(T.STATS, "\uD83D\uDD35 Tracked: " + eventName);
        }
    }


    @Override
    public void endSession() {
        this.flush();
    }

    @Override
    public void flush() {
        if (mNosaraClient == null) {
            return;
        }
        mNosaraClient.flush();
    }

    @Override
    public void refreshMetadata(AnalyticsMetadata metadata) {
        if (mNosaraClient == null) {
            return;
        }

        try {
            JSONObject properties = new JSONObject();
            properties.put(JETPACK_USER, metadata.isJetpackUser());
            properties.put(NUMBER_OF_BLOGS, metadata.getNumBlogs());
            properties.put(WPCOM_USER, metadata.isWordPressComUser());
            properties.put(APP_SCHEME, metadata.getAppScheme());
            // Only add the editor information if it was set before.
            // See: https://github.com/wordpress-mobile/WordPress-Android/pull/10300#discussion_r309145514
            if (metadata.isGutenbergEnabledVariableSet()) {
                properties.put(IS_GUTENBERG_ENABLED, metadata.isGutenbergEnabled());
            }
            mNosaraClient.registerUserProperties(properties);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        // De-anonymize user only when it's WPCOM and we have the username available (might still be waiting for it to
        //  be fetched).
        if (metadata.isUserConnected() && metadata.isWordPressComUser()
            && !TextUtils.isEmpty(metadata.getUsername())) {
            setWordPressComUserName(metadata.getUsername());
            // Re-unify the user
            if (getAnonID() != null) {
                mNosaraClient.trackAliasUser(getWordPressComUserName(), getAnonID(), TracksClient.NosaraUserType.WPCOM);
                clearAnonID();
            }
        } else {
            // Not wpcom connected. Check if anonID is already present
            setWordPressComUserName(null);
            if (getAnonID() == null) {
                generateNewAnonID();
            }
        }
    }


    @Override
    public void clearAllData() {
        super.clearAllData();
        if (mNosaraClient == null) {
            return;
        }
        mNosaraClient.clearUserProperties();
        mNosaraClient.clearQueues();
    }
}
// CHECKSTYLE END IGNORE
