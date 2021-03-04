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

    private static final String EVENTS_PREFIX = "wpandroid_";

    private final TracksClient mNosaraClient;

    public AnalyticsTrackerNosara(Context context) throws IllegalArgumentException {
        super(context);
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

        String eventName = getEventNameForStat(stat);
        if (eventName == null) {
            AppLog.w(AppLog.T.STATS, "There is NO match for the event " + stat.name() + "stat");
            return;
        }

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
            case QUICK_START_MIGRATION_DIALOG_POSITIVE_TAPPED:
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
            case QUICK_START_TYPE_CUSTOMIZE_DISMISSED:
                predefinedEventProperties.put("type", "customize");
                break;
            case QUICK_START_TYPE_GROW_DISMISSED:
                predefinedEventProperties.put("type", "grow");
                break;
            case QUICK_START_LIST_CUSTOMIZE_COLLAPSED:
                predefinedEventProperties.put("type", "customize");
                break;
            case QUICK_START_LIST_GROW_COLLAPSED:
                predefinedEventProperties.put("type", "grow");
                break;
            case QUICK_START_LIST_CUSTOMIZE_EXPANDED:
                predefinedEventProperties.put("type", "customize");
                break;
            case QUICK_START_LIST_GROW_EXPANDED:
                predefinedEventProperties.put("type", "grow");
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
            case QUICK_START_LIST_EXPLORE_PLANS_SKIPPED:
                predefinedEventProperties.put("task_name", "explore_plans");
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
            case QUICK_START_LIST_EXPLORE_PLANS_TAPPED:
                predefinedEventProperties.put("task_name", "explore_plans");
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
            case QUICK_START_EXPLORE_PLANS_COMPLETED:
                predefinedEventProperties.put("task_name", "explore_plans");
                break;
            case QUICK_START_LIST_EDIT_HOMEPAGE_SKIPPED:
            case QUICK_START_LIST_EDIT_HOMEPAGE_TAPPED:
            case QUICK_START_EDIT_HOMEPAGE_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "edit_homepage");
                break;
            case QUICK_START_LIST_REVIEW_PAGES_SKIPPED:
            case QUICK_START_LIST_REVIEW_PAGES_TAPPED:
            case QUICK_START_REVIEW_PAGES_TASK_COMPLETED:
                predefinedEventProperties.put("task_name", "review_pages");
                break;
            case QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED:
                predefinedEventProperties.put("type", "negative");
                break;
            case QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED:
                predefinedEventProperties.put("type", "positive");
                break;
            case QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED:
                predefinedEventProperties.put("type", "neutral");
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
            case QUICK_ACTION_STATS_TAPPED:
                predefinedEventProperties.put("button", "stats");
                break;
            case QUICK_ACTION_PAGES_TAPPED:
                predefinedEventProperties.put("button", "pages");
                break;
            case QUICK_ACTION_POSTS_TAPPED:
                predefinedEventProperties.put("button", "posts");
                break;
            case QUICK_ACTION_MEDIA_TAPPED:
                predefinedEventProperties.put("button", "media");
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
                                    "User value: " + propertiesToJSON.get(key).toString()
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
            mNosaraClient.track(EVENTS_PREFIX + eventName, propertiesToJSON, user, userType);
            String jsonString = propertiesToJSON.toString();
            AppLog.i(T.STATS, "\uD83D\uDD35 Tracked: " + eventName + ", Properties: " + jsonString);
        } else {
            mNosaraClient.track(EVENTS_PREFIX + eventName, user, userType);
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

    @SuppressWarnings("checkstyle:methodlength")
    public static String getEventNameForStat(AnalyticsTracker.Stat stat) {
        if (!isValidEvent(stat)) {
            return null;
        }

        switch (stat) {
            case APPLICATION_OPENED:
                // This stat is part of a funnel that provides critical information.  Before
                // making ANY modification to this stat please refer to: p4qSXL-35X-p2
                return "application_opened";
            case APPLICATION_CLOSED:
                return "application_closed";
            case APPLICATION_INSTALLED:
                return "application_installed";
            case APPLICATION_UPGRADED:
                return "application_upgraded";
            case READER_ACCESSED:
                return "reader_accessed";
            case READER_ARTICLE_COMMENTED_ON:
            case READER_ARTICLE_COMMENT_REPLIED_TO:
                return "reader_article_commented_on";
            case READER_ARTICLE_COMMENTS_OPENED:
                return "reader_article_comments_opened";
            case READER_ARTICLE_COMMENT_LIKED:
                return "reader_article_comment_liked";
            case READER_ARTICLE_COMMENT_UNLIKED:
                return "reader_article_comment_unliked";
            case READER_ARTICLE_DETAIL_LIKED:
                return "reader_article_detail_liked";
            case READER_ARTICLE_DETAIL_UNLIKED:
                return "reader_article_detail_unliked";
            case READER_ARTICLE_LIKED:
                return "reader_article_liked";
            case READER_ARTICLE_REBLOGGED:
                return "reader_article_reblogged";
            case READER_ARTICLE_DETAIL_REBLOGGED:
                return "reader_article_detail_reblogged";
            case READER_ARTICLE_OPENED:
                return "reader_article_opened";
            case READER_ARTICLE_UNLIKED:
                return "reader_article_unliked";
            case READER_ARTICLE_RENDERED:
                return "reader_article_rendered";
            case READER_BLOG_BLOCKED:
                return "reader_blog_blocked";
            case READER_BLOG_FOLLOWED:
                return "reader_site_followed";
            case READER_BLOG_PREVIEWED:
                return "reader_blog_previewed";
            case READER_BLOG_UNFOLLOWED:
                return "reader_site_unfollowed";
            case READER_SUGGESTED_SITE_VISITED:
                return "reader_suggested_site_visited";
            case READER_SUGGESTED_SITE_TOGGLE_FOLLOW:
                return "reader_suggested_site_toggle_follow";
            case READER_ARTICLE_VISITED:
                return "reader_article_visited";
            case READER_DISCOVER_VIEWED:
                return "reader_discover_viewed";
            case READER_INFINITE_SCROLL:
                return "reader_infinite_scroll_performed";
            case READER_LIST_FOLLOWED:
                return "reader_list_followed";
            case READER_LIST_LOADED:
                return "reader_list_loaded";
            case READER_LIST_PREVIEWED:
                return "reader_list_previewed";
            case READER_LIST_UNFOLLOWED:
                return "reader_list_unfollowed";
            case READER_TAG_FOLLOWED:
                return "reader_reader_tag_followed";
            case READER_TAG_LOADED:
                return "reader_tag_loaded";
            case READER_P2_SHOWN:
                return "reader_p2_shown";
            case READER_A8C_SHOWN:
                return "reader_a8c_shown";
            case READER_TAG_PREVIEWED:
                return "reader_tag_previewed";
            case READER_SEARCH_LOADED:
                return "reader_search_loaded";
            case READER_SEARCH_PERFORMED:
                return "reader_search_performed";
            case READER_SEARCH_RESULT_TAPPED:
                return "reader_searchcard_clicked";
            case READER_TAG_UNFOLLOWED:
                return "reader_reader_tag_unfollowed";
            case READER_GLOBAL_RELATED_POST_CLICKED:
                return "reader_related_post_from_other_site_clicked";
            case READER_LOCAL_RELATED_POST_CLICKED:
                return "reader_related_post_from_same_site_clicked";
            case READER_VIEWPOST_INTERCEPTED:
                return "reader_viewpost_intercepted";
            case READER_BLOG_POST_INTERCEPTED:
                return "reader_blog_post_intercepted";
            case READER_FEED_POST_INTERCEPTED:
                return "reader_feed_post_intercepted";
            case READER_WPCOM_BLOG_POST_INTERCEPTED:
                return "reader_wpcom_blog_post_intercepted";
            case READER_SIGN_IN_INITIATED:
                return "reader_sign_in_initiated";
            case READER_WPCOM_SIGN_IN_NEEDED:
                return "reader_wpcom_sign_in_needed";
            case READER_USER_UNAUTHORIZED:
                return "reader_user_unauthorized";
            case READER_POST_SAVED_FROM_OTHER_POST_LIST:
            case READER_POST_SAVED_FROM_SAVED_POST_LIST:
            case READER_POST_SAVED_FROM_DETAILS:
                return "reader_post_saved";
            case READER_POST_UNSAVED_FROM_OTHER_POST_LIST:
            case READER_POST_UNSAVED_FROM_SAVED_POST_LIST:
            case READER_POST_UNSAVED_FROM_DETAILS:
                return "reader_post_unsaved";
            case READER_SAVED_POST_OPENED_FROM_SAVED_POST_LIST:
            case READER_SAVED_POST_OPENED_FROM_OTHER_POST_LIST:
                return "reader_saved_post_opened";
            case READER_SITE_SHARED:
                return "reader_site_shared";
            case EDITOR_CREATED_POST:
                return "editor_post_created";
            case EDITOR_SAVED_DRAFT:
                return "editor_draft_saved";
            case EDITOR_EDITED_IMAGE:
                return "editor_image_edited";
            case EDITOR_AZTEC_ENABLED:
                return "editor_aztec_enabled";
            case EDITOR_AZTEC_TOGGLED_OFF:
                return "editor_aztec_toggled_off";
            case EDITOR_AZTEC_TOGGLED_ON:
                return "editor_aztec_toggled_on";
            case EDITOR_UPLOAD_MEDIA_FAILED:
                return "editor_upload_media_failed";
            case EDITOR_UPLOAD_MEDIA_RETRIED:
                return "editor_upload_media_retried";
            case EDITOR_CLOSED:
                return "editor_closed";
            case EDITOR_SESSION_START:
                return "editor_session_start";
            case EDITOR_SESSION_SWITCH_EDITOR:
                return "editor_session_switch_editor";
            case EDITOR_SESSION_TEMPLATE_APPLY:
                return "editor_session_template_apply";
            case EDITOR_SESSION_END:
                return "editor_session_end";
            case EDITOR_GUTENBERG_ENABLED:
                return "gutenberg_enabled";
            case EDITOR_GUTENBERG_DISABLED:
                return "gutenberg_disabled";
            case POST_LIST_ACCESS_ERROR:
                return "post_list_access_error";
            case POST_LIST_BUTTON_PRESSED:
                return "post_list_button_pressed";
            case POST_LIST_ITEM_SELECTED:
                return "post_list_item_selected";
            case POST_LIST_AUTHOR_FILTER_CHANGED:
                return "post_list_author_filter_changed";
            case POST_LIST_TAB_CHANGED:
                return "post_list_tab_changed";
            case POST_LIST_VIEW_LAYOUT_TOGGLED:
                return "post_list_view_layout_toggled";
            case POST_LIST_SEARCH_ACCESSED:
                return "post_list_search_accessed";
            case EDITOR_OPENED:
                return "editor_opened";
            case EDITOR_ADDED_PHOTO_NEW:
                return "editor_photo_added";
            case EDITOR_ADDED_PHOTO_VIA_DEVICE_LIBRARY:
                return "editor_photo_added";
            case EDITOR_ADDED_PHOTO_VIA_WP_MEDIA_LIBRARY:
                return "editor_photo_added";
            case EDITOR_ADDED_PHOTO_VIA_MEDIA_EDITOR:
                return "editor_photo_added";
            case EDITOR_ADDED_VIDEO_NEW:
                return "editor_video_added";
            case EDITOR_ADDED_VIDEO_VIA_DEVICE_LIBRARY:
                return "editor_video_added";
            case EDITOR_ADDED_VIDEO_VIA_WP_MEDIA_LIBRARY:
                return "editor_video_added";
            case EDITOR_ADDED_PHOTO_VIA_STOCK_MEDIA_LIBRARY:
                return "editor_photo_added";
            case EDITOR_ADDED_FILE_VIA_LIBRARY:
                return "editor_file_added";
            case EDITOR_ADDED_AUDIO_FILE_VIA_LIBRARY:
                return "editor_audio_file_added";
            case MEDIA_PHOTO_OPTIMIZED:
                return "media_photo_optimized";
            case MEDIA_PHOTO_OPTIMIZE_ERROR:
                return "media_photo_optimize_error";
            case MEDIA_VIDEO_OPTIMIZED:
                return "media_video_optimized";
            case MEDIA_VIDEO_OPTIMIZE_ERROR:
                return "media_video_optimize_error";
            case MEDIA_VIDEO_CANT_OPTIMIZE:
                return "media_video_cant_optimize";
            case EDITOR_PUBLISHED_POST:
                return "editor_post_published";
            case EDITOR_POST_PUBLISH_TAPPED:
                return "editor_post_publish_tapped";
            case EDITOR_POST_SCHEDULE_CHANGED:
                return "editor_post_schedule_changed";
            case EDITOR_POST_VISIBILITY_CHANGED:
                return "editor_post_visibility_changed";
            case EDITOR_POST_TAGS_CHANGED:
                return "editor_post_tags_changed";
            case EDITOR_POST_PUBLISH_NOW_TAPPED:
                return "editor_post_publish_now_tapped";
            case EDITOR_POST_PASSWORD_CHANGED:
                return "editor_post_password_changed";
            case EDITOR_UPDATED_POST:
                return "editor_post_updated";
            case EDITOR_SCHEDULED_POST:
                return "editor_post_scheduled";
            case EDITOR_POST_CATEGORIES_ADDED:
                return "editor_post_categories_added";
            case EDITOR_POST_FORMAT_CHANGED:
                return "editor_post_format_changed";
            case EDITOR_POST_SLUG_CHANGED:
                return "editor_post_slug_changed";
            case EDITOR_POST_EXCERPT_CHANGED:
                return "editor_post_excerpt_changed";
            case EDITOR_TAPPED_BLOCKQUOTE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_BOLD:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ELLIPSIS_COLLAPSE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ELLIPSIS_EXPAND:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_1:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_2:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_3:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_4:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_5:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HEADING_6:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HORIZONTAL_RULE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_IMAGE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ITALIC:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LINK_ADDED:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LIST:
                return "editor_button_tapped";
            case EDITOR_TAPPED_READ_MORE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_NEXT_PAGE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_PARAGRAPH:
                return "editor_button_tapped";
            case EDITOR_TAPPED_PREFORMAT:
                return "editor_button_tapped";
            case EDITOR_TAPPED_STRIKETHROUGH:
                return "editor_button_tapped";
            case EDITOR_TAPPED_UNDERLINE:
                return "editor_button_tapped";
            case EDITOR_TAPPED_HTML:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LIST_ORDERED:
                return "editor_button_tapped";
            case EDITOR_TAPPED_LIST_UNORDERED:
                return "editor_button_tapped";
            case EDITOR_TAPPED_ALIGN_LEFT:
            case EDITOR_TAPPED_ALIGN_CENTER:
            case EDITOR_TAPPED_ALIGN_RIGHT:
                return "editor_button_tapped";
            case EDITOR_TAPPED_UNDO:
            case EDITOR_TAPPED_REDO:
                return "editor_button_tapped";
            case REVISIONS_LIST_VIEWED:
                return "revisions_list_viewed";
            case REVISIONS_DETAIL_VIEWED_FROM_LIST:
            case REVISIONS_DETAIL_VIEWED_FROM_SWIPE:
            case REVISIONS_DETAIL_VIEWED_FROM_CHEVRON:
                return "revisions_detail_viewed";
            case REVISIONS_DETAIL_CANCELLED:
                return "revisions_detail_cancelled";
            case REVISIONS_REVISION_LOADED:
                return "revisions_revision_loaded";
            case REVISIONS_LOAD_UNDONE:
                return "revisions_load_undone";
            case FOLLOWED_BLOG_NOTIFICATIONS_READER_ENABLED:
                return "followed_blog_notifications_reader_enabled";
            case FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_OFF:
                return "followed_blog_notifications_reader_menu_off";
            case FOLLOWED_BLOG_NOTIFICATIONS_READER_MENU_ON:
                return "followed_blog_notifications_reader_menu_on";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_OFF:
                return "followed_blog_notifications_settings_off";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_ON:
                return "followed_blog_notifications_settings_on";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_OFF:
                return "followed_blog_notifications_settings_email_off";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_ON:
                return "followed_blog_notifications_settings_email_on";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_INSTANTLY:
                return "followed_blog_notifications_settings_email_instantly";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_DAILY:
                return "followed_blog_notifications_settings_email_daily";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_EMAIL_WEEKLY:
                return "followed_blog_notifications_settings_email_weekly";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_COMMENTS_OFF:
                return "followed_blog_notifications_settings_comments_off";
            case FOLLOWED_BLOG_NOTIFICATIONS_SETTINGS_COMMENTS_ON:
                return "followed_blog_notifications_settings_comments_on";
            case NOTIFICATIONS_DISABLED:
                return "notifications_disabled";
            case NOTIFICATIONS_ENABLED:
                return "notifications_enabled";
            case NOTIFICATIONS_ACCESSED:
                return "notifications_accessed";
            case NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS:
                return "notifications_notification_details_opened";
            case NOTIFICATION_APPROVED:
            case NOTIFICATION_QUICK_ACTIONS_APPROVED:
                return "notifications_approved";
            case NOTIFICATION_UNAPPROVED:
                return "notifications_unapproved";
            case NOTIFICATIONS_MISSING_SYNC_WARNING:
                return "notifications_missing_sync_warning";
            case NOTIFICATION_REPLIED_TO:
            case NOTIFICATION_QUICK_ACTIONS_REPLIED_TO:
                return "notifications_replied_to";
            case NOTIFICATION_TRASHED:
                return "notifications_trashed";
            case NOTIFICATION_FLAGGED_AS_SPAM:
                return "notifications_flagged_as_spam";
            case NOTIFICATION_SWIPE_PAGE_CHANGED:
                return "notifications_swipe_page_changed";
            case NOTIFICATION_PENDING_DRAFTS_TAPPED:
                return "notifications_pending_drafts_tapped";
            case NOTIFICATION_PENDING_DRAFTS_IGNORED:
                return "notifications_pending_drafts_ignored";
            case NOTIFICATION_PENDING_DRAFTS_DISMISSED:
                return "notifications_pending_drafts_dismissed";
            case NOTIFICATION_PENDING_DRAFTS_SETTINGS_ENABLED:
                return "notifications_pending_drafts_settings_enabled";
            case NOTIFICATION_PENDING_DRAFTS_SETTINGS_DISABLED:
                return "notifications_pending_drafts_settings_disabled";
            case NOTIFICATION_LIKED:
            case NOTIFICATION_QUICK_ACTIONS_LIKED:
                return "notifications_comment_liked";
            case NOTIFICATION_QUICK_ACTIONS_QUICKACTION_TOUCHED:
                return "quick_action_touched";
            case NOTIFICATION_UNLIKED:
                return "notifications_comment_unliked";
            case NOTIFICATION_UPLOAD_MEDIA_SUCCESS_WRITE_POST:
                return "notifications_upload_media_success_write_post";
            case NOTIFICATION_UPLOAD_POST_ERROR_RETRY:
                return "notifications_upload_post_error_retry";
            case NOTIFICATION_UPLOAD_MEDIA_ERROR_RETRY:
                return "notifications_upload_media_error_retry";
            case NOTIFICATION_RECEIVED_PROCESSING_START:
                return "notifications_received_processing_start";
            case NOTIFICATION_RECEIVED_PROCESSING_END:
                return "notifications_received_processing_end";
            case NOTIFICATION_SHOWN:
                return "notification_shown";
            case NOTIFICATION_TAPPED:
                return "notification_tapped";
            case NOTIFICATION_DISMISSED:
                return "notification_dismissed";
            case OPENED_POSTS:
                return "site_menu_opened";
            case OPENED_PAGES:
                return "site_menu_opened";
            case OPENED_COMMENTS:
                return "site_menu_opened";
            case OPENED_VIEW_SITE:
                return "site_menu_opened";
            case OPENED_VIEW_SITE_FROM_HEADER:
                return "site_menu_opened";
            case OPENED_VIEW_ADMIN:
                return "site_menu_opened";
            case OPENED_MEDIA_LIBRARY:
                return "site_menu_opened";
            case OPENED_BLOG_SETTINGS:
                return "site_menu_opened";
            case OPENED_ACCOUNT_SETTINGS:
                return "account_settings_opened";
            case ACCOUNT_SETTINGS_CHANGE_USERNAME_SUCCEEDED:
                return "account_settings_change_username_succeeded";
            case ACCOUNT_SETTINGS_CHANGE_USERNAME_FAILED:
                return "account_settings_change_username_failed";
            case ACCOUNT_SETTINGS_CHANGE_USERNAME_SUGGESTIONS_FAILED:
                return "account_settings_change_username_suggestions_failed";
            case OPENED_APP_SETTINGS:
                return "app_settings_opened";
            case OPENED_MY_PROFILE:
                return "my_profile_opened";
            case OPENED_PEOPLE_MANAGEMENT:
                return "people_management_list_opened";
            case OPENED_PERSON:
                return "people_management_details_opened";
            case OPENED_PLUGIN_DETAIL:
                return "plugin_detail_opened";
            case OPENED_PLUGIN_DIRECTORY:
                return "plugin_directory_opened";
            case OPENED_PLUGIN_LIST:
                return "plugin_list_opened";
            case OPENED_PLANS:
                return "site_menu_opened";
            case OPENED_SHARING_MANAGEMENT:
                return "site_menu_opened";
            case OPENED_SHARING_BUTTON_MANAGEMENT:
                return "sharing_buttons_opened";
            case CREATE_ACCOUNT_INITIATED:
                return "account_create_initiated";
            case CREATE_ACCOUNT_EMAIL_EXISTS:
                return "account_create_email_exists";
            case CREATE_ACCOUNT_USERNAME_EXISTS:
                return "account_create_username_exists";
            case CREATE_ACCOUNT_FAILED:
                return "account_create_failed";
            case CREATED_ACCOUNT:
                // This stat is part of a funnel that provides critical information.  Before
                // making ANY modification to this stat please refer to: p4qSXL-35X-p2
                return "account_created";
            case SHARED_ITEM:
                return "item_shared";
            case SHARED_ITEM_READER:
                return "item_shared_reader";
            case ADDED_SELF_HOSTED_SITE:
                return "self_hosted_blog_added";
            case SIGNED_IN:
                return "signed_in";
            case SIGNED_INTO_JETPACK:
                return "signed_into_jetpack";
            case INSTALL_JETPACK_SELECTED:
                return "install_jetpack_selected";
            case INSTALL_JETPACK_CANCELLED:
                return "install_jetpack_canceled";
            case INSTALL_JETPACK_COMPLETED:
                return "install_jetpack_completed";
            case INSTALL_JETPACK_REMOTE_START:
                return "install_jetpack_remote_start";
            case INSTALL_JETPACK_REMOTE_COMPLETED:
                return "install_jetpack_remote_completed";
            case INSTALL_JETPACK_REMOTE_FAILED:
                return "install_jetpack_remote_failed";
            case INSTALL_JETPACK_REMOTE_CONNECT:
                return "install_jetpack_remote_connect";
            case INSTALL_JETPACK_REMOTE_LOGIN:
                return "install_jetpack_remote_login";
            case INSTALL_JETPACK_REMOTE_RESTART:
                return "install_jetpack_remote_restart";
            case INSTALL_JETPACK_REMOTE_START_MANUAL_FLOW:
                return "install_jetpack_remote_start_manual_flow";
            case INSTALL_JETPACK_REMOTE_ALREADY_INSTALLED:
                return "install_jetpack_remote_already_installed";
            case CONNECT_JETPACK_SELECTED:
                return "connect_jetpack_selected";
            case CONNECT_JETPACK_FAILED:
                return "connect_jetpack_failed";
            case ACCOUNT_LOGOUT:
                return "account_logout";
            case STATS_ACCESSED:
                return "stats_accessed";
            case STATS_ACCESS_ERROR:
                return "stats_access_error";
            case STATS_INSIGHTS_ACCESSED:
                return "stats_insights_accessed";
            case STATS_INSIGHTS_MANAGEMENT_HINT_DISMISSED:
                return "stats_insights_management_hint_dismissed";
            case STATS_INSIGHTS_MANAGEMENT_HINT_CLICKED:
                return "stats_insights_management_hint_clicked";
            case STATS_INSIGHTS_MANAGEMENT_ACCESSED:
                return "stats_insights_management_accessed";
            case STATS_INSIGHTS_TYPE_MOVED_UP:
                return "stats_insights_type_moved_up";
            case STATS_INSIGHTS_TYPE_MOVED_DOWN:
                return "stats_insights_type_moved_down";
            case STATS_INSIGHTS_TYPE_REMOVED:
                return "stats_insights_type_removed";
            case STATS_INSIGHTS_MANAGEMENT_SAVED:
                return "stats_insights_management_saved";
            case STATS_INSIGHTS_MANAGEMENT_TYPE_ADDED:
                return "stats_insights_management_type_added";
            case STATS_INSIGHTS_MANAGEMENT_TYPE_REMOVED:
                return "stats_insights_management_type_removed";
            case STATS_INSIGHTS_MANAGEMENT_TYPE_REORDERED:
                return "stats_insights_management_type_reordered";
            case STATS_PERIOD_DAYS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_WEEKS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_MONTHS_ACCESSED:
                return "stats_period_accessed";
            case STATS_PERIOD_YEARS_ACCESSED:
                return "stats_period_accessed";
            case STATS_VIEW_ALL_ACCESSED:
                return "stats_view_all_accessed";
            case STATS_PREVIOUS_DATE_TAPPED:
                return "stats_previous_date_tapped";
            case STATS_NEXT_DATE_TAPPED:
                return "stats_next_date_tapped";
            case STATS_FOLLOWERS_VIEW_MORE_TAPPED:
                return "stats_followers_view_more_tapped";
            case STATS_TAGS_AND_CATEGORIES_VIEW_MORE_TAPPED:
                return "stats_tags_and_categories_view_more_tapped";
            case STATS_PUBLICIZE_VIEW_MORE_TAPPED:
                return "stats_publicize_view_more_tapped";
            case STATS_POSTS_AND_PAGES_VIEW_MORE_TAPPED:
                return "stats_posts_and_pages_view_more_tapped";
            case STATS_POSTS_AND_PAGES_ITEM_TAPPED:
                return "stats_posts_and_pages_item_tapped";
            case STATS_REFERRERS_VIEW_MORE_TAPPED:
                return "stats_referrers_view_more_tapped";
            case STATS_REFERRERS_ITEM_TAPPED:
                return "stats_referrers_item_tapped";
            case STATS_REFERRERS_ITEM_LONG_PRESSED:
                return "stats_referrers_item_long_pressed";
            case STATS_REFERRERS_ITEM_MARKED_AS_SPAM:
                return "stats_referrers_item_marked_as_spam";
            case STATS_REFERRERS_ITEM_MARKED_AS_NOT_SPAM:
                return "stats_referrers_item_marked_as_not_spam";
            case STATS_CLICKS_VIEW_MORE_TAPPED:
                return "stats_clicks_view_more_tapped";
            case STATS_COUNTRIES_VIEW_MORE_TAPPED:
                return "stats_countries_view_more_tapped";
            case STATS_OVERVIEW_BAR_CHART_TAPPED:
                return "stats_overview_bar_chart_tapped";
            case STATS_OVERVIEW_ERROR:
                return "stats_overview_error";
            case STATS_VIDEO_PLAYS_VIEW_MORE_TAPPED:
                return "stats_video_plays_view_more_tapped";
            case STATS_VIDEO_PLAYS_VIDEO_TAPPED:
                return "stats_video_plays_video_tapped";
            case STATS_SEARCH_TERMS_VIEW_MORE_TAPPED:
                return "stats_search_terms_view_more_tapped";
            case STATS_AUTHORS_VIEW_MORE_TAPPED:
                return "stats_authors_view_more_tapped";
            case STATS_FILE_DOWNLOADS_VIEW_MORE_TAPPED:
                return "stats_file_downloads_view_more_tapped";
            case STATS_LATEST_POST_SUMMARY_ADD_NEW_POST_TAPPED:
                return "stats_latest_post_summary_add_new_post_tapped";
            case STATS_LATEST_POST_SUMMARY_SHARE_POST_TAPPED:
                return "stats_latest_post_summary_share_post_tapped";
            case STATS_LATEST_POST_SUMMARY_VIEW_POST_DETAILS_TAPPED:
                return "stats_latest_post_summary_view_post_details_tapped";
            case STATS_LATEST_POST_SUMMARY_POST_ITEM_TAPPED:
                return "stats_latest_post_summary_post_item_tapped";
            case STATS_TAGS_AND_CATEGORIES_VIEW_TAG_TAPPED:
                return "stats_tags_and_categories_view_tag_tapped";
            case STATS_AUTHORS_VIEW_POST_TAPPED:
                return "stats_authors_view_post_tapped";
            case STATS_CLICKS_ITEM_TAPPED:
                return "stats_clicks_item_tapped";
            case STATS_SINGLE_POST_ACCESSED:
                return "stats_single_post_accessed";
            case STATS_TAPPED_BAR_CHART:
                return "stats_bar_chart_tapped";
            case STATS_OVERVIEW_TYPE_TAPPED:
                return "stats_overview_type_tapped";
            case STATS_DETAIL_POST_TAPPED:
                return "stats_detail_post_tapped";
            case STATS_SCROLLED_TO_BOTTOM:
                return "stats_scrolled_to_bottom";
            case STATS_WIDGET_ADDED:
                return "stats_widget_added";
            case STATS_WIDGET_REMOVED:
                return "stats_widget_removed";
            case STATS_WIDGET_TAPPED:
                return "stats_widget_tapped";
            case PUSH_NOTIFICATION_RECEIVED:
                return "push_notification_received";
            case PUSH_NOTIFICATION_TAPPED:
                return "push_notification_alert_tapped";
            case UNIFIED_LOGIN_STEP:
                return "unified_login_step";
            case UNIFIED_LOGIN_INTERACTION:
                return "unified_login_interaction";
            case UNIFIED_LOGIN_FAILURE:
                return "unified_login_failure";
            case LOGIN_ACCESSED:
                return "login_accessed";
            case LOGIN_MAGIC_LINK_EXITED:
                return "login_magic_link_exited";
            case LOGIN_MAGIC_LINK_FAILED:
                return "login_magic_link_failed";
            case LOGIN_MAGIC_LINK_OPENED:
                return "login_magic_link_opened";
            case LOGIN_MAGIC_LINK_REQUESTED:
                return "login_magic_link_requested";
            case LOGIN_MAGIC_LINK_SUCCEEDED:
                return "login_magic_link_succeeded";
            case LOGIN_FAILED:
                return "login_failed_to_login";
            case LOGIN_FAILED_TO_GUESS_XMLRPC:
                return "login_failed_to_guess_xmlrpc";
            case LOGIN_INSERTED_INVALID_URL:
                return "login_inserted_invalid_url";
            case LOGIN_AUTOFILL_CREDENTIALS_FILLED:
                return "login_autofill_credentials_filled";
            case LOGIN_AUTOFILL_CREDENTIALS_UPDATED:
                return "login_autofill_credentials_updated";
            case LOGIN_PROLOGUE_PAGED:
                return "login_prologue_paged";
            case LOGIN_PROLOGUE_PAGED_JETPACK:
                return "login_prologue_paged_jetpack";
            case LOGIN_PROLOGUE_PAGED_NOTIFICATIONS:
                return "login_prologue_paged_notifications";
            case LOGIN_PROLOGUE_PAGED_POST:
                return "login_prologue_paged_post";
            case LOGIN_PROLOGUE_PAGED_READER:
                return "login_prologue_paged_reader";
            case LOGIN_PROLOGUE_PAGED_STATS:
                return "login_prologue_paged_stats";
            case LOGIN_PROLOGUE_VIEWED:
                return "login_prologue_viewed";
            case LOGIN_EMAIL_FORM_VIEWED:
                return "login_email_form_viewed";
            case LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED:
                return "login_magic_link_open_email_client_viewed";
            case LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED:
                return "login_magic_link_open_email_client_clicked";
            case LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED:
                return "login_magic_link_request_form_viewed";
            case LOGIN_PASSWORD_FORM_VIEWED:
                return "login_password_form_viewed";
            case LOGIN_URL_FORM_VIEWED:
                return "login_url_form_viewed";
            case LOGIN_URL_HELP_SCREEN_VIEWED:
                return "login_url_help_screen_viewed";
            case LOGIN_CONNECTED_SITE_INFO_REQUESTED:
                return "login_connected_site_info_requested";
            case LOGIN_CONNECTED_SITE_INFO_FAILED:
                return "login_connected_site_info_failed";
            case LOGIN_CONNECTED_SITE_INFO_SUCCEEDED:
                return "login_connected_site_info_succeeded";
            case LOGIN_USERNAME_PASSWORD_FORM_VIEWED:
                return "login_username_password_form_viewed";
            case LOGIN_TWO_FACTOR_FORM_VIEWED:
                return "login_two_factor_form_viewed";
            case LOGIN_EPILOGUE_VIEWED:
                return "login_epilogue_viewed";
            case LOGIN_FORGOT_PASSWORD_CLICKED:
                return "login_forgot_password_clicked";
            case LOGIN_SOCIAL_BUTTON_CLICK:
                return "login_social_button_click";
            case LOGIN_SOCIAL_BUTTON_FAILURE:
                return "login_social_button_failure";
            case LOGIN_SOCIAL_CONNECT_SUCCESS:
                return "login_social_connect_success";
            case LOGIN_SOCIAL_CONNECT_FAILURE:
                return "login_social_connect_failure";
            case LOGIN_SOCIAL_SUCCESS:
                return "login_social_success";
            case LOGIN_SOCIAL_FAILURE:
                return "login_social_failure";
            case LOGIN_SOCIAL_2FA_NEEDED:
                return "login_social_2fa_needed";
            case LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING:
                return "login_social_accounts_need_connecting";
            case LOGIN_SOCIAL_ERROR_UNKNOWN_USER:
                return "login_social_error_unknown_user";
            case LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE:
                return "login_wpcom_background_service_update";
            case PAGES_SET_PARENT_CHANGES_SAVED:
                return "site_pages_set_parent_changes_saved";
            case PAGES_ADD_PAGE:
                return "site_pages_add_page";
            case PAGES_TAB_PRESSED:
                return "site_pages_tabs_pressed";
            case PAGES_OPTIONS_PRESSED:
                return "site_pages_options_pressed";
            case PAGES_SEARCH_ACCESSED:
                return "site_pages_search_accessed";
            case SIGNUP_BUTTON_TAPPED:
                // This stat is part of a funnel that provides critical information.  Before
                // making ANY modification to this stat please refer to: p4qSXL-35X-p2
                return "signup_button_tapped";
            case SIGNUP_EMAIL_BUTTON_TAPPED:
                return "signup_email_button_tapped";
            case SIGNUP_EMAIL_EPILOGUE_GRAVATAR_CROPPED:
                return "signup_email_epilogue_gravatar_cropped:";
            case SIGNUP_EMAIL_EPILOGUE_GRAVATAR_GALLERY_PICKED:
                return "signup_email_epilogue_gallery_picked";
            case SIGNUP_EMAIL_EPILOGUE_GRAVATAR_SHOT_NEW:
                return "signup_email_epilogue_shot_new";
            case SIGNUP_EMAIL_EPILOGUE_UNCHANGED:
                return "signup_epilogue_unchanged";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
                return "signup_epilogue_update_display_name_failed";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
                return "signup_epilogue_update_display_name_succeeded";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_FAILED:
                return "signup_epilogue_update_username_failed";
            case SIGNUP_EMAIL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
                return "signup_epilogue_update_username_succeeded";
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
                return "signup_epilogue_username_suggestions_failed";
            case SIGNUP_EMAIL_EPILOGUE_USERNAME_TAPPED:
                return "signup_epilogue_username_tapped";
            case SIGNUP_EMAIL_EPILOGUE_VIEWED:
                return "signup_epilogue_viewed";
            case SIGNUP_SOCIAL_BUTTON_TAPPED:
                return "signup_social_button_tapped";
            case SIGNUP_TERMS_OF_SERVICE_TAPPED:
                return "signup_terms_of_service_tapped";
            case SIGNUP_CANCELED:
                return "signup_canceled";
            case SIGNUP_EMAIL_TO_LOGIN:
                return "signup_email_to_login";
            case SIGNUP_MAGIC_LINK_FAILED:
                return "signup_magic_link_failed";
            case SIGNUP_MAGIC_LINK_OPENED:
                return "signup_magic_link_opened";
            case SIGNUP_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED:
                return "signup_magic_link_open_email_client_clicked";
            case SIGNUP_MAGIC_LINK_SENT:
                return "signup_magic_link_sent";
            case SIGNUP_MAGIC_LINK_SUCCEEDED:
                return "signup_magic_link_succeeded";
            case SIGNUP_SOCIAL_ACCOUNTS_NEED_CONNECTING:
                return "signup_social_accounts_need_connecting";
            case SIGNUP_SOCIAL_BUTTON_FAILURE:
                return "signup_social_button_failure";
            case SIGNUP_SOCIAL_EPILOGUE_UNCHANGED:
                return "signup_epilogue_unchanged";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_FAILED:
                return "signup_epilogue_update_display_name_failed";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_DISPLAY_NAME_SUCCEEDED:
                return "signup_epilogue_update_display_name_succeeded";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_FAILED:
                return "signup_epilogue_update_username_failed";
            case SIGNUP_SOCIAL_EPILOGUE_UPDATE_USERNAME_SUCCEEDED:
                return "signup_epilogue_update_username_succeeded";
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED:
                return "signup_epilogue_username_suggestions_failed";
            case SIGNUP_SOCIAL_EPILOGUE_USERNAME_TAPPED:
                return "signup_epilogue_username_tapped";
            case SIGNUP_SOCIAL_EPILOGUE_VIEWED:
                return "signup_epilogue_viewed";
            case SIGNUP_SOCIAL_SUCCESS:
                return "signup_social_success";
            case SIGNUP_SOCIAL_TO_LOGIN:
                return "signup_social_to_login";
            case ENHANCED_SITE_CREATION_ACCESSED:
                return "enhanced_site_creation_accessed";
            case ENHANCED_SITE_CREATION_SEGMENTS_VIEWED:
                return "enhanced_site_creation_segments_viewed";
            case ENHANCED_SITE_CREATION_SEGMENTS_SELECTED:
                return "enhanced_site_creation_segments_selected";
            case ENHANCED_SITE_CREATION_DOMAINS_ACCESSED:
                return "enhanced_site_creation_domains_accessed";
            case ENHANCED_SITE_CREATION_DOMAINS_SELECTED:
                return "enhanced_site_creation_domains_selected";
            case ENHANCED_SITE_CREATION_SUCCESS_LOADING:
                return "enhanced_site_creation_success_loading";
            case ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_VIEWED:
                return "enhanced_site_creation_success_preview_viewed";
            case ENHANCED_SITE_CREATION_SUCCESS_PREVIEW_LOADED:
                return "enhanced_site_creation_success_preview_loaded";
            case ENHANCED_SITE_CREATION_PREVIEW_OK_BUTTON_TAPPED:
                return "enhanced_site_creation_preview_ok_button_tapped";
            case ENHANCED_SITE_CREATION_EXITED:
                return "enhanced_site_creation_exited";
            case ENHANCED_SITE_CREATION_ERROR_SHOWN:
                return "enhanced_site_creation_error_shown";
            case ENHANCED_SITE_CREATION_BACKGROUND_SERVICE_UPDATED:
                return "enhanced_site_creation_background_service_updated";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_VIEWED:
                return "enhanced_site_creation_site_design_viewed";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_THUMBNAIL_MODE_BUTTON_TAPPED:
                return "enhanced_site_creation_site_design_thumbnail_mode_button_tapped";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_SELECTED:
                return "enhanced_site_creation_site_design_selected";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_SKIPPED:
                return "enhanced_site_creation_site_design_skipped";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_VIEWED:
                return "enhanced_site_creation_site_design_preview_viewed";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_MODE_BUTTON_TAPPED:
                return "enhanced_site_creation_site_design_preview_mode_button_tapped";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_MODE_CHANGED:
                return "enhanced_site_creation_site_design_preview_mode_changed";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_LOADING:
                return "enhanced_site_creation_site_design_preview_loading";
            case ENHANCED_SITE_CREATION_SITE_DESIGN_PREVIEW_LOADED:
                return "enhanced_site_creation_site_design_preview_loaded";
            case LAYOUT_PICKER_PREVIEW_MODE_CHANGED:
                return "layout_picker_preview_mode_changed";
            case LAYOUT_PICKER_THUMBNAIL_MODE_BUTTON_TAPPED:
                return "layout_picker_thumbnail_mode_button_tapped";
            case LAYOUT_PICKER_PREVIEW_MODE_BUTTON_TAPPED:
                return "layout_picker_preview_mode_button_tapped";
            case LAYOUT_PICKER_PREVIEW_LOADING:
                return "layout_picker_preview_loading";
            case LAYOUT_PICKER_PREVIEW_LOADED:
                return "layout_picker_preview_loaded";
            case LAYOUT_PICKER_PREVIEW_VIEWED:
                return "layout_picker_preview_viewed";
            case LAYOUT_PICKER_ERROR_SHOWN:
                return "layout_picker_error_shown";
            case FILTER_CHANGED:
                return "filter_changed";
            case SITE_CREATED:
                // This stat is part of a funnel that provides critical information.  Before
                // making ANY modification to this stat please refer to: p4qSXL-35X-p2
                return "site_created";
            case PERSON_REMOVED:
                return "people_management_person_removed";
            case PERSON_UPDATED:
                return "people_management_person_updated";
            case PUSH_AUTHENTICATION_APPROVED:
                return "push_authentication_approved";
            case PUSH_AUTHENTICATION_EXPIRED:
                return "push_authentication_expired";
            case PUSH_AUTHENTICATION_FAILED:
                return "push_authentication_failed";
            case PUSH_AUTHENTICATION_IGNORED:
                return "push_authentication_ignored";
            case NOTIFICATION_SETTINGS_LIST_OPENED:
                return "notification_settings_list_opened";
            case NOTIFICATION_SETTINGS_STREAMS_OPENED:
                return "notification_settings_streams_opened";
            case NOTIFICATION_SETTINGS_DETAILS_OPENED:
                return "notification_settings_details_opened";
            case NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_DISABLED:
                return "notification_settings_app_notifications_disabled";
            case NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_ENABLED:
                return "notification_settings_app_notifications_enabled";
            case NOTIFICATION_TAPPED_SEGMENTED_CONTROL:
                return "notification_tapped_segmented_control";
            case ME_ACCESSED:
                return "me_tab_accessed";
            case ME_GRAVATAR_TAPPED:
                return "me_gravatar_tapped";
            case ME_GRAVATAR_SHOT_NEW:
                return "me_gravatar_shot_new";
            case ME_GRAVATAR_GALLERY_PICKED:
                return "me_gravatar_gallery_picked";
            case ME_GRAVATAR_CROPPED:
                return "me_gravatar_cropped";
            case ME_GRAVATAR_UPLOADED:
                return "me_gravatar_uploaded";
            case ME_GRAVATAR_UPLOAD_UNSUCCESSFUL:
                return "me_gravatar_upload_unsuccessful";
            case ME_GRAVATAR_UPLOAD_EXCEPTION:
                return "me_gravatar_upload_exception";
            case MY_SITE_ACCESSED:
                return "my_site_tab_accessed";
            case MY_SITE_ICON_TAPPED:
                return "my_site_icon_tapped";
            case MY_SITE_ICON_REMOVED:
                return "my_site_icon_removed";
            case MY_SITE_ICON_SHOT_NEW:
                return "my_site_icon_shot_new";
            case MY_SITE_ICON_GALLERY_PICKED:
                return "my_site_icon_gallery_picked";
            case MY_SITE_ICON_CROPPED:
                return "my_site_icon_cropped";
            case MY_SITE_ICON_UPLOADED:
                return "my_site_icon_uploaded";
            case MY_SITE_ICON_UPLOAD_UNSUCCESSFUL:
                return "my_site_icon_upload_unsuccessful";
            case THEMES_ACCESSED_THEMES_BROWSER:
                return "themes_theme_browser_accessed";
            case THEMES_ACCESSED_SEARCH:
                return "themes_search_accessed";
            case THEMES_CHANGED_THEME:
                return "themes_theme_changed";
            case THEMES_PREVIEWED_SITE:
                return "themes_theme_for_site_previewed";
            case THEMES_DEMO_ACCESSED:
                return "themes_demo_accessed";
            case THEMES_CUSTOMIZE_ACCESSED:
                return "themes_customize_accessed";
            case THEMES_SUPPORT_ACCESSED:
                return "themes_support_accessed";
            case THEMES_DETAILS_ACCESSED:
                return "themes_details_accessed";
            case ACCOUNT_SETTINGS_LANGUAGE_CHANGED:
                return "account_settings_language_changed";
            case SITE_SETTINGS_ACCESSED:
                return "site_settings_accessed";
            case SITE_SETTINGS_ACCESSED_MORE_SETTINGS:
                return "site_settings_more_settings_accessed";
            case SITE_SETTINGS_ADDED_LIST_ITEM:
                return "site_settings_added_list_item";
            case SITE_SETTINGS_DELETED_LIST_ITEMS:
                return "site_settings_deleted_list_items";
            case SITE_SETTINGS_HINT_TOAST_SHOWN:
                return "site_settings_hint_toast_shown";
            case SITE_SETTINGS_LEARN_MORE_CLICKED:
                return "site_settings_learn_more_clicked";
            case SITE_SETTINGS_LEARN_MORE_LOADED:
                return "site_settings_learn_more_loaded";
            case SITE_SETTINGS_SAVED_REMOTELY:
                return "site_settings_saved_remotely";
            case SITE_SETTINGS_START_OVER_ACCESSED:
                return "site_settings_start_over_accessed";
            case SITE_SETTINGS_START_OVER_CONTACT_SUPPORT_CLICKED:
                return "site_settings_start_over_contact_support_clicked";
            case SITE_SETTINGS_EXPORT_SITE_ACCESSED:
                return "site_settings_export_site_accessed";
            case SITE_SETTINGS_EXPORT_SITE_REQUESTED:
                return "site_settings_export_site_requested";
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_OK:
                return "site_settings_export_site_response_ok";
            case SITE_SETTINGS_EXPORT_SITE_RESPONSE_ERROR:
                return "site_settings_export_site_response_error";
            case SITE_SETTINGS_DELETE_SITE_ACCESSED:
                return "site_settings_delete_site_accessed";
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_REQUESTED:
                return "site_settings_delete_site_purchases_requested";
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOWN:
                return "site_settings_delete_site_purchases_shown";
            case SITE_SETTINGS_DELETE_SITE_PURCHASES_SHOW_CLICKED:
                return "site_settings_delete_site_purchases_show_clicked";
            case SITE_SETTINGS_DELETE_SITE_REQUESTED:
                return "site_settings_delete_site_requested";
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_OK:
                return "site_settings_delete_site_response_ok";
            case SITE_SETTINGS_DELETE_SITE_RESPONSE_ERROR:
                return "site_settings_delete_site_response_error";
            case SITE_SETTINGS_OPTIMIZE_IMAGES_CHANGED:
                return "site_settings_optimize_images_changed";
            case SITE_SETTINGS_JETPACK_SECURITY_SETTINGS_VIEWED:
                return "jetpack_settings_viewed";
            case SITE_SETTINGS_JETPACK_WHITELISTED_IPS_VIEWED:
                return "jetpack_whitelisted_ips_viewed";
            case SITE_SETTINGS_JETPACK_WHITELISTED_IPS_CHANGED:
                return "jetpack_whitelisted_ips_changed";
            case ABTEST_START:
                return "abtest_start";
            case FEATURE_FLAG_SET:
                return "feature_flag_set";
            case EXPERIMENT_VARIANT_SET:
                return "experiment_variant_set";
            case TRAIN_TRACKS_RENDER:
                return "traintracks_render";
            case TRAIN_TRACKS_INTERACT:
                return "traintracks_interact";
            case DEEP_LINKED:
                return "deep_linked";
            case DEEP_LINKED_FALLBACK:
                return "deep_linked_fallback";
            case DEEP_LINK_NOT_DEFAULT_HANDLER:
                return "deep_link_not_default_handler";
            case MEDIA_LIBRARY_ADDED_PHOTO:
                return "media_library_photo_added";
            case MEDIA_LIBRARY_ADDED_VIDEO:
                return "media_library_video_added";
            case MEDIA_UPLOAD_STARTED:
                return "media_service_upload_started";
            case MEDIA_UPLOAD_ERROR:
                return "media_service_upload_response_error";
            case MEDIA_UPLOAD_SUCCESS:
                return "media_service_upload_response_ok";
            case MEDIA_UPLOAD_CANCELED:
                return "media_service_upload_canceled";
            case MEDIA_PICKER_OPEN_CAPTURE_MEDIA:
                return "media_picker_capture_media_opened";
            case MEDIA_PICKER_OPEN_SYSTEM_PICKER:
                return "media_picker_open_system_picker";
            case MEDIA_PICKER_OPEN_DEVICE_LIBRARY:
                return "media_picker_device_library_opened";
            case MEDIA_PICKER_OPEN_WP_MEDIA:
                return "media_picker_wordpress_library_opened";
            case MEDIA_PICKER_OPEN_STOCK_LIBRARY:
                return "media_picker_open_stock_library";
            case MEDIA_PICKER_OPEN_GIF_LIBRARY:
                return "media_picker_open_gif_library";
            case MEDIA_PICKER_OPEN_WP_STORIES_CAPTURE:
                return "media_picker_stories_capture_opened";
            case MEDIA_PICKER_OPEN_FOR_STORIES:
                return "media_picker_open_for_stories";
            case MEDIA_PICKER_RECENT_MEDIA_SELECTED:
                return "media_picker_recent_media_selected";
            case MEDIA_PICKER_PREVIEW_OPENED:
                return "media_picker_preview_opened";
            case MEDIA_PICKER_SEARCH_EXPANDED:
                return "media_picker_search_expanded";
            case MEDIA_PICKER_SEARCH_COLLAPSED:
                return "media_picker_search_collapsed";
            case MEDIA_PICKER_SEARCH_TRIGGERED:
                return "media_picker_search_triggered";
            case MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN:
                return "media_picker_show_permissions_screen";
            case MEDIA_PICKER_ITEM_SELECTED:
                return "media_picker_item_selected";
            case MEDIA_PICKER_ITEM_UNSELECTED:
                return "media_picker_item_unselected";
            case MEDIA_PICKER_SELECTION_CLEARED:
                return "media_picker_selection_cleared";
            case MEDIA_PICKER_OPENED:
                return "media_picker_opened";
            case APP_PERMISSION_GRANTED:
                return "app_permission_granted";
            case APP_PERMISSION_DENIED:
                return "app_permission_denied";
            case SHARE_TO_WP_SUCCEEDED:
                return "share_to_wp_succeeded";
            case PLUGIN_ACTIVATED:
                return "plugin_activated";
            case PLUGIN_AUTOUPDATE_ENABLED:
                return "plugin_autoupdate_enabled";
            case PLUGIN_AUTOUPDATE_DISABLED:
                return "plugin_autoupdate_disabled";
            case PLUGIN_DEACTIVATED:
                return "plugin_deactivated";
            case PLUGIN_INSTALLED:
                return "plugin_installed";
            case PLUGIN_REMOVED:
                return "plugin_removed";
            case PLUGIN_SEARCH_PERFORMED:
                return "plugin_search_performed";
            case PLUGIN_UPDATED:
                return "plugin_updated";
            case STOCK_MEDIA_ACCESSED:
                return "stock_media_accessed";
            case STOCK_MEDIA_SEARCHED:
                return "stock_media_searched";
            case STOCK_MEDIA_UPLOADED:
                return "stock_media_uploaded";
            case GIF_PICKER_SEARCHED:
                return "gif_picker_searched";
            case GIF_PICKER_ACCESSED:
                return "gif_picker_accessed";
            case GIF_PICKER_DOWNLOADED:
                return "gif_picker_downloaded";
            case SHORTCUT_STATS_CLICKED:
                return "shortcut_stats_clicked";
            case SHORTCUT_NOTIFICATIONS_CLICKED:
                return "shortcut_notifications_clicked";
            case SHORTCUT_NEW_POST_CLICKED:
                return "shortcut_new_post_clicked";
            case AUTOMATED_TRANSFER_CONFIRM_DIALOG_SHOWN:
                return "automated_transfer_confirm_dialog_shown";
            case AUTOMATED_TRANSFER_CONFIRM_DIALOG_CANCELLED:
                return "automated_transfer_confirm_dialog_cancelled";
            case AUTOMATED_TRANSFER_CHECK_ELIGIBILITY:
                return "automated_transfer_check_eligibility";
            case AUTOMATED_TRANSFER_NOT_ELIGIBLE:
                return "automated_transfer_not_eligible";
            case AUTOMATED_TRANSFER_INITIATE:
                return "automated_transfer_initiate";
            case AUTOMATED_TRANSFER_INITIATED:
                return "automated_transfer_initiated";
            case AUTOMATED_TRANSFER_INITIATION_FAILED:
                return "automated_transfer_initiation_failed";
            case AUTOMATED_TRANSFER_STATUS_COMPLETE:
                return "automated_transfer_status_complete";
            case AUTOMATED_TRANSFER_STATUS_FAILED:
                return "automated_transfer_status_failed";
            case AUTOMATED_TRANSFER_FLOW_COMPLETE:
                return "automated_transfer_flow_complete";
            case AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASED:
                return "automated_transfer_custom_domain_purchased";
            case AUTOMATED_TRANSFER_CUSTOM_DOMAIN_PURCHASE_FAILED:
                return "automated_transfer_custom_domain_purchase_failed";
            case PUBLICIZE_SERVICE_CONNECTED:
                return "publicize_service_connected";
            case PUBLICIZE_SERVICE_DISCONNECTED:
                return "publicize_service_disconnected";
            case ACTIVITY_LOG_LIST_OPENED:
                return "activity_log_list_opened";
            case ACTIVITY_LOG_DETAIL_OPENED:
                return "activity_log_detail_opened";
            case ACTIVITY_LOG_REWIND_STARTED:
                return "activity_log_rewind_started";
            case ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_BUTTON_TAPPED:
                return "activitylog_filterbar_range_button_tapped";
            case ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_BUTTON_TAPPED:
                return "activitylog_filterbar_type_button_tapped";
            case ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_SELECTED:
                return "activitylog_filterbar_select_range";
            case ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_SELECTED:
                return "activitylog_filterbar_select_type";
            case ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_RESET:
                return "activitylog_filterbar_reset_range";
            case ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_RESET:
                return "activitylog_filterbar_reset_type";
            case JETPACK_BACKUP_LIST_OPENED:
                return "jetpack_backup_list_opened";
            case JETPACK_BACKUP_REWIND_STARTED:
                return "jetpack_backup_rewind_started";
            case JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_BUTTON_TAPPED:
                return "jetpack_backup_filterbar_range_button_tapped";
            case JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_SELECTED:
                return "jetpack_backup_filterbar_select_range";
            case JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_RESET:
                return "jetpack_backup_filterbar_reset_range";
            case JETPACK_SCAN_ACCESSED:
                return "jetpack_scan_accessed";
            case JETPACK_SCAN_HISTORY_ACCESSED:
                return "jetpack_scan_history_accessed";
            case JETPACK_SCAN_HISTORY_FILTER:
                return "jetpack_scan_history_filter";
            case JETPACK_SCAN_THREAT_LIST_ITEM_TAPPED:
                return "jetpack_scan_threat_list_item_tapped";
            case JETPACK_SCAN_THREAT_CODEABLE_ESTIMATE_TAPPED:
                return "jetpack_scan_threat_codeable_estimate_tapped";
            case JETPACK_SCAN_RUN_TAPPED:
                return "jetpack_scan_run_tapped";
            case JETPACK_SCAN_IGNORE_THREAT_DIALOG_OPEN:
                return "jetpack_scan_ignorethreat_dialogopen";
            case JETPACK_SCAN_THREAT_IGNORE_TAPPED:
                return "jetpack_scan_threat_ignore_tapped";
            case JETPACK_SCAN_FIX_THREAT_DIALOG_OPEN:
                return "jetpack_scan_fixthreat_dialogopen";
            case JETPACK_SCAN_THREAT_FIX_TAPPED:
                return "jetpack_scan_threat_fix_tapped";
            case JETPACK_SCAN_ALL_THREATS_OPEN:
                return "jetpack_scan_allthreats_open";
            case JETPACK_SCAN_ALL_THREATS_FIX_TAPPED:
                return "jetpack_scan_allthreats_fix_tapped";
            case JETPACK_SCAN_ERROR:
                return "jetpack_scan_error";
            case SUPPORT_HELP_CENTER_VIEWED:
                return "support_help_center_viewed";
            case SUPPORT_NEW_REQUEST_VIEWED:
                return "support_new_request_viewed";
            case SUPPORT_TICKET_LIST_VIEWED:
                return "support_ticket_list_viewed";
            case SUPPORT_OPENED:
                return "support_opened";
            case SUPPORT_IDENTITY_FORM_VIEWED:
                return "support_identity_form_viewed";
            case SUPPORT_IDENTITY_SET:
                return "support_identity_set";
            case QUICK_START_TASK_DIALOG_VIEWED:
                return "quick_start_task_dialog_viewed";
            case QUICK_START_TASK_DIALOG_NEGATIVE_TAPPED:
            case QUICK_START_TASK_DIALOG_POSITIVE_TAPPED:
                return "quick_start_task_dialog_button_tapped";
            case QUICK_START_MIGRATION_DIALOG_VIEWED:
                return "quick_start_migration_dialog_viewed";
            case QUICK_START_MIGRATION_DIALOG_POSITIVE_TAPPED:
                return "quick_start_migration_dialog_button_tapped";
            case QUICK_START_REMOVE_DIALOG_NEGATIVE_TAPPED:
            case QUICK_START_REMOVE_DIALOG_POSITIVE_TAPPED:
                return "quick_start_remove_dialog_button_tapped";
            case QUICK_START_TYPE_CUSTOMIZE_DISMISSED:
            case QUICK_START_TYPE_GROW_DISMISSED:
                return "quick_start_type_dismissed";
            case QUICK_START_LIST_CUSTOMIZE_COLLAPSED:
            case QUICK_START_LIST_GROW_COLLAPSED:
                return "quick_start_list_collapsed";
            case QUICK_START_LIST_CUSTOMIZE_EXPANDED:
            case QUICK_START_LIST_GROW_EXPANDED:
                return "quick_start_list_expanded";
            case QUICK_START_TYPE_CUSTOMIZE_VIEWED:
            case QUICK_START_TYPE_GROW_VIEWED:
                return "quick_start_list_viewed";
            case QUICK_START_LIST_CREATE_SITE_SKIPPED:
            case QUICK_START_LIST_UPDATE_SITE_TITLE_SKIPPED:
            case QUICK_START_LIST_VIEW_SITE_SKIPPED:
            case QUICK_START_LIST_ADD_SOCIAL_SKIPPED:
            case QUICK_START_LIST_PUBLISH_POST_SKIPPED:
            case QUICK_START_LIST_FOLLOW_SITE_SKIPPED:
            case QUICK_START_LIST_UPLOAD_ICON_SKIPPED:
            case QUICK_START_LIST_CHECK_STATS_SKIPPED:
            case QUICK_START_LIST_EXPLORE_PLANS_SKIPPED:
            case QUICK_START_LIST_EDIT_HOMEPAGE_SKIPPED:
            case QUICK_START_LIST_REVIEW_PAGES_SKIPPED:
                return "quick_start_list_item_skipped";
            case QUICK_START_LIST_CREATE_SITE_TAPPED:
            case QUICK_START_LIST_UPDATE_SITE_TITLE_TAPPED:
            case QUICK_START_LIST_VIEW_SITE_TAPPED:
            case QUICK_START_LIST_ADD_SOCIAL_TAPPED:
            case QUICK_START_LIST_PUBLISH_POST_TAPPED:
            case QUICK_START_LIST_FOLLOW_SITE_TAPPED:
            case QUICK_START_LIST_UPLOAD_ICON_TAPPED:
            case QUICK_START_LIST_CHECK_STATS_TAPPED:
            case QUICK_START_LIST_EXPLORE_PLANS_TAPPED:
            case QUICK_START_LIST_EDIT_HOMEPAGE_TAPPED:
            case QUICK_START_LIST_REVIEW_PAGES_TAPPED:
                return "quick_start_list_item_tapped";
            case QUICK_START_CREATE_SITE_TASK_COMPLETED:
            case QUICK_START_UPDATE_SITE_TITLE_COMPLETED:
            case QUICK_START_VIEW_SITE_TASK_COMPLETED:
            case QUICK_START_SHARE_SITE_TASK_COMPLETED:
            case QUICK_START_PUBLISH_POST_TASK_COMPLETED:
            case QUICK_START_FOLLOW_SITE_TASK_COMPLETED:
            case QUICK_START_UPLOAD_ICON_COMPLETED:
            case QUICK_START_CHECK_STATS_COMPLETED:
            case QUICK_START_EXPLORE_PLANS_COMPLETED:
            case QUICK_START_EDIT_HOMEPAGE_TASK_COMPLETED:
            case QUICK_START_REVIEW_PAGES_TASK_COMPLETED:
                return "quick_start_task_completed";
            case QUICK_START_ALL_TASKS_COMPLETED:
                return "quick_start_all_tasks_completed";
            case QUICK_START_REQUEST_VIEWED:
                return "quick_start_request_dialog_viewed";
            case QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED:
            case QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED:
            case QUICK_START_REQUEST_DIALOG_NEUTRAL_TAPPED:
                return "quick_start_request_dialog_button_tapped";
            case QUICK_START_NOTIFICATION_DISMISSED:
                return "quick_start_notification_dismissed";
            case QUICK_START_NOTIFICATION_SENT:
                return "quick_start_notification_sent";
            case QUICK_START_NOTIFICATION_TAPPED:
                return "quick_start_notification_tapped";
            case QUICK_START_HIDE_CARD_TAPPED:
                return "quick_start_hide_card_tapped";
            case QUICK_START_REMOVE_CARD_TAPPED:
                return "quick_start_remove_card_tapped";
            case INSTALLATION_REFERRER_OBTAINED:
                return "installation_referrer_obtained";
            case INSTALLATION_REFERRER_FAILED:
                return "installation_referrer_failed";
            case OPENED_PAGE_PARENT:
                return "page_parent_opened";
            case GUTENBERG_WARNING_CONFIRM_DIALOG_SHOWN:
                return "gutenberg_warning_confirm_dialog_shown";
            case GUTENBERG_WARNING_CONFIRM_DIALOG_YES_TAPPED:
                return "gutenberg_warning_confirm_dialog_yes_tapped";
            case GUTENBERG_WARNING_CONFIRM_DIALOG_CANCEL_TAPPED:
                return "gutenberg_warning_confirm_dialog_cancel_tapped";
            case GUTENBERG_WARNING_CONFIRM_DIALOG_DONT_SHOW_AGAIN_CHECKED:
                return "gutenberg_warning_confirm_dialog_dont_show_again_checked";
            case GUTENBERG_WARNING_CONFIRM_DIALOG_DONT_SHOW_AGAIN_UNCHECKED:
                return "gutenberg_warning_confirm_dialog_dont_show_again_unchecked";
            case GUTENBERG_WARNING_CONFIRM_DIALOG_LEARN_MORE_TAPPED:
                return "gutenberg_warning_confirm_dialog_learn_more_tapped";
            case APP_REVIEWS_SAW_PROMPT:
                return "app_reviews_saw_prompt";
            case APP_REVIEWS_CANCELLED_PROMPT:
                return "app_reviews_cancelled_prompt";
            case APP_REVIEWS_RATED_APP:
                return "app_reviews_rated_app";
            case APP_REVIEWS_DECLINED_TO_RATE_APP:
                return "app_reviews_declined_to_rate_apt";
            case APP_REVIEWS_DECIDED_TO_RATE_LATER:
                return "app_reviews_decided_to_rate_later";
            case APP_REVIEWS_EVENT_INCREMENTED_BY_UPLOADING_MEDIA:
            case APP_REVIEWS_EVENT_INCREMENTED_BY_CHECKING_NOTIFICATION:
            case APP_REVIEWS_EVENT_INCREMENTED_BY_PUBLISHING_POST_OR_PAGE:
            case APP_REVIEWS_EVENT_INCREMENTED_BY_OPENING_READER_POST:
                return "app_reviews_significant_event_incremented";
            case DOMAIN_CREDIT_PROMPT_SHOWN:
                return "domain_credit_prompt_shown";
            case DOMAIN_CREDIT_REDEMPTION_TAPPED:
                return "domain_credit_redemption_tapped";
            case DOMAIN_CREDIT_REDEMPTION_SUCCESS:
                return "domain_credit_redemption_success";
            case DOMAIN_CREDIT_SUGGESTION_QUERIED:
                return "domain_credit_suggestion_queried";
            case DOMAIN_CREDIT_NAME_SELECTED:
                return "domain_credit_name_selected";
            case QUICK_ACTION_STATS_TAPPED:
            case QUICK_ACTION_PAGES_TAPPED:
            case QUICK_ACTION_POSTS_TAPPED:
            case QUICK_ACTION_MEDIA_TAPPED:
                return "quick_action_tapped";
            case AUTO_UPLOAD_POST_INVOKED:
                return "auto_upload_post_invoked";
            case AUTO_UPLOAD_PAGE_INVOKED:
                return "auto_upload_page_invoked";
            case UNPUBLISHED_REVISION_DIALOG_SHOWN:
                return "unpublished_revision_dialog_shown";
            case UNPUBLISHED_REVISION_DIALOG_LOAD_LOCAL_VERSION_CLICKED:
                return "unpublished_revision_dialog_load_local_version_clicked";
            case UNPUBLISHED_REVISION_DIALOG_LOAD_UNPUBLISHED_VERSION_CLICKED:
                return "unpublished_revision_dialog_load_unpublished_version_clicked";
            case WELCOME_NO_SITES_INTERSTITIAL_SHOWN:
                return "welcome_no_sites_interstitial_shown";
            case WELCOME_NO_SITES_INTERSTITIAL_CREATE_NEW_SITE_TAPPED:
            case WELCOME_NO_SITES_INTERSTITIAL_ADD_SELF_HOSTED_SITE_TAPPED:
                return "welcome_no_sites_interstitial_button_tapped";
            case WELCOME_NO_SITES_INTERSTITIAL_DISMISSED:
                return "welcome_no_sites_interstitial_dismissed";
            case FEATURED_IMAGE_SET_CLICKED_POST_SETTINGS:
                return "featured_image_set_clicked_post_settings";
            case FEATURED_IMAGE_PICKED_POST_SETTINGS:
                return "featured_image_picked_post_settings";
            case FEATURED_IMAGE_UPLOAD_CANCELED_POST_SETTINGS:
                return "featured_image_upload_canceled_post_settings";
            case FEATURED_IMAGE_UPLOAD_RETRY_CLICKED_POST_SETTINGS:
                return "featured_image_upload_retry_clicked_post_settings";
            case FEATURED_IMAGE_REMOVE_CLICKED_POST_SETTINGS:
                return "featured_image_remove_clicked_post_settings";
            case MEDIA_EDITOR_SHOWN:
                return "media_editor_shown";
            case MEDIA_EDITOR_USED:
                return "media_editor_used";
            case STORY_SAVE_SUCCESSFUL:
                return "story_save_successful";
            case STORY_SAVE_ERROR:
                return "story_save_error";
            case STORY_POST_SAVE_LOCALLY:
                return "story_post_save_locally";
            case STORY_POST_SAVE_REMOTELY:
                return "story_post_save_remotely";
            case STORY_SAVE_ERROR_SNACKBAR_MANAGE_TAPPED:
                return "story_post_error_snackbar_manage_tapped";
            case STORY_POST_PUBLISH_TAPPED:
                return "story_post_publish_tapped";
            case STORY_TEXT_CHANGED:
                return "story_text_changed";
            case STORY_INTRO_SHOWN:
                return "story_intro_shown";
            case STORY_INTRO_DISMISSED:
                return "story_intro_dismissed";
            case STORY_INTRO_CREATE_STORY_BUTTON_TAPPED:
                return "story_intro_create_story_button_tapped";
            case STORY_BLOCK_ADD_MEDIA_TAPPED:
                return "story_block_add_media_tapped";
            case FEATURE_ANNOUNCEMENT_SHOWN_ON_APP_UPGRADE:
            case FEATURE_ANNOUNCEMENT_SHOWN_FROM_APP_SETTINGS:
                return "feature_announcement_shown";
            case FEATURE_ANNOUNCEMENT_FIND_OUT_MORE_TAPPED:
            case FEATURE_ANNOUNCEMENT_CLOSE_DIALOG_BUTTON_TAPPED:
                return "feature_announcement_button_tapped";
            case OPENED_PLANS_COMPARISON:
                return "plans_compare";
            case PAGES_LIST_AUTHOR_FILTER_CHANGED:
                return "pages_list_author_filter_changed";
            case EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_SHOWN:
                return "gutenberg_unsupported_block_webview_shown";
            case EDITOR_GUTENBERG_UNSUPPORTED_BLOCK_WEBVIEW_CLOSED:
                return "gutenberg_unsupported_block_webview_closed";
            case PREPUBLISHING_BOTTOM_SHEET_OPENED:
                return "prepublishing_bottom_sheet_opened";
            case PREPUBLISHING_BOTTOM_SHEET_DISMISSED:
                return "prepublishing_bottom_sheet_dismissed";
            case SELECT_INTERESTS_SHOWN:
                return "select_interests_shown";
            case SELECT_INTERESTS_PICKED:
                return "select_interests_picked";
            case READER_FOLLOWING_SHOWN:
                return "reader_following_shown";
            case READER_LIKED_SHOWN:
                return "reader_liked_shown";
            case READER_SAVED_LIST_SHOWN:
                return "reader_saved_list_shown";
            case READER_CUSTOM_TAB_SHOWN:
                return "reader_custom_tab_shown";
            case READER_DISCOVER_SHOWN:
                return "reader_discover_shown";
            case READER_DISCOVER_PAGINATED:
                return "reader_discover_paginated";
            case READER_DISCOVER_TOPIC_TAPPED:
                return "reader_discover_topic_tapped";
            case READER_POST_CARD_TAPPED:
                return "reader_post_card_tapped";
            case READER_PULL_TO_REFRESH:
                return "reader_pull_to_refresh";
            case POST_CARD_MORE_TAPPED:
                return "post_card_more_tapped";
            case READER_ARTICLE_DETAIL_MORE_TAPPED:
                return "reader_article_detail_more_tapped";
            case READER_CHIPS_MORE_TOGGLED:
                return "reader_chips_more_toggled";
            case ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL:
                return "encrypted_logging_upload_successful";
            case ENCRYPTED_LOGGING_UPLOAD_FAILED:
                return "encrypted_logging_upload_failed";
            case READER_POST_REPORTED:
                return "reader_post_reported";
            case SUGGESTION_SESSION_FINISHED:
                return "suggestion_session_finished";
            case COMMENT_APPROVED:
            case COMMENT_QUICK_ACTION_APPROVED:
                return "comment_approved";
            case COMMENT_UNAPPROVED:
                return "comment_unapproved";
            case COMMENT_SPAMMED:
                return "comment_spammed";
            case COMMENT_UNSPAMMED:
                return "comment_unspammed";
            case COMMENT_LIKED:
            case COMMENT_QUICK_ACTION_LIKED:
                return "comment_liked";
            case COMMENT_UNLIKED:
                return "comment_unliked";
            case COMMENT_TRASHED:
                return "comment_trashed";
            case COMMENT_UNTRASHED:
                return "comment_untrashed";
            case COMMENT_REPLIED_TO:
            case COMMENT_QUICK_ACTION_REPLIED_TO:
                return "comment_replied_to";
            case COMMENT_EDITED:
                return "comment_edited";
            case COMMENT_VIEWED:
                return "comment_viewed";
            case COMMENT_DELETED:
                return "comment_deleted";
            case COMMENT_FOLLOW_CONVERSATION:
                return "comment_follow_conversation";
            case COMMENT_BATCH_APPROVED:
                return "comment_batch_approved";
            case COMMENT_BATCH_UNAPPROVED:
                return "comment_batch_unapproved";
            case COMMENT_BATCH_SPAMMED:
                return "comment_batch_spammed";
            case COMMENT_BATCH_TRASHED:
                return "comment_batch_trashed";
            case COMMENT_BATCH_DELETED:
                return "comment_batch_deleted";
            case COMMENT_EDITOR_OPENED:
                return "comment_editor_opened";
            case READER_POST_MARKED_AS_SEEN:
                return "reader_mark_as_seen";
            case READER_POST_MARKED_AS_UNSEEN:
                return "reader_mark_as_unseen";
            case JETPACK_RESTORE_OPENED:
                return "jetpack_restore_opened";
            case JETPACK_RESTORE_CONFIRMED:
                return "jetpack_restore_confirmed";
            case JETPACK_RESTORE_ERROR:
                return "jetpack_restore_error";
            case JETPACK_BACKUP_DOWNLOAD_OPENED:
                return "jetpack_backup_download_opened";
            case JETPACK_BACKUP_DOWNLOAD_CONFIRMED:
                return "jetpack_backup_download_confirmed";
            case JETPACK_BACKUP_DOWNLOAD_ERROR:
                return "jetpack_backup_download_error";
            case JETPACK_BACKUP_DOWNLOAD_FILE_DOWNLOAD_TAPPED:
                return "jetpack_backup_download_file_download_tapped";
            case JETPACK_BACKUP_DOWNLOAD_SHARE_LINK_TAPPED:
                return "jetpack_backup_download_share_link_tapped";
            case MY_SITE_CREATE_SHEET_SHOWN:
                return "my_site_create_sheet_shown";
            case MY_SITE_CREATE_SHEET_ACTION_TAPPED:
                return "my_site_create_sheet_action_tapped";
            case POST_LIST_CREATE_SHEET_SHOWN:
                return "post_list_create_sheet_shown";
            case POST_LIST_CREATE_SHEET_ACTION_TAPPED:
                return "post_list_create_sheet_action_tapped";
            case INVITE_LINKS_GET_STATUS:
                return "invite_links_get_status";
            case INVITE_LINKS_GENERATE:
                return "invite_links_generate";
            case INVITE_LINKS_DISABLE:
                return "invite_links_disable";
            case INVITE_LINKS_SHARE:
                return "invite_links_share";
            case JETPACK_BACKUP_DOWNLOAD_FILE_NOTICE_DOWNLOAD_TAPPED:
                return "jetpack_backup_download_file_notice_download_tapped";
            case JETPACK_BACKUP_DOWNLOAD_FILE_NOTICE_DISMISSED_TAPPED:
                return "jetpack_backup_download_file_notice_dismissed_tapped";
            case ACTIVITY_LOG_DOWNLOAD_FILE_NOTICE_DOWNLOAD_TAPPED:
                return "activity_log_download_file_notice_download_tapped";
            case ACTIVITY_LOG_DOWNLOAD_FILE_NOTICE_DISMISSED_TAPPED:
                return "activity_log_download_file_notice_dismissed_tapped";
        }
        return null;
    }
}
// CHECKSTYLE END IGNORE
