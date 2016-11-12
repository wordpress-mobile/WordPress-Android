package org.wordpress.android.ui.prefs;

import android.app.Activity;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class DotComSiteSettings extends SiteSettingsInterface {
    // WP.com REST keys used in response to a settings GET and POST request
    public static final String LANGUAGE_ID_KEY = "lang_id";
    public static final String PRIVACY_KEY = "blog_public";
    public static final String URL_KEY = "URL";
    public static final String DEF_CATEGORY_KEY = "default_category";
    public static final String DEF_POST_FORMAT_KEY = "default_post_format";
    public static final String RELATED_POSTS_ALLOWED_KEY = "jetpack_relatedposts_allowed";
    public static final String RELATED_POSTS_ENABLED_KEY = "jetpack_relatedposts_enabled";
    public static final String RELATED_POSTS_HEADER_KEY = "jetpack_relatedposts_show_headline";
    public static final String RELATED_POSTS_IMAGES_KEY = "jetpack_relatedposts_show_thumbnails";
    public static final String ALLOW_COMMENTS_KEY = "default_comment_status";
    public static final String SEND_PINGBACKS_KEY = "default_pingback_flag";
    public static final String RECEIVE_PINGBACKS_KEY = "default_ping_status";
    public static final String CLOSE_OLD_COMMENTS_KEY = "close_comments_for_old_posts";
    public static final String CLOSE_OLD_COMMENTS_DAYS_KEY = "close_comments_days_old";
    public static final String THREAD_COMMENTS_KEY = "thread_comments";
    public static final String THREAD_COMMENTS_DEPTH_KEY = "thread_comments_depth";
    public static final String PAGE_COMMENTS_KEY = "page_comments";
    public static final String PAGE_COMMENT_COUNT_KEY = "comments_per_page";
    public static final String COMMENT_SORT_ORDER_KEY = "comment_order";
    public static final String COMMENT_MODERATION_KEY = "comment_moderation";
    public static final String REQUIRE_IDENTITY_KEY = "require_name_email";
    public static final String REQUIRE_USER_ACCOUNT_KEY = "comment_registration";
    public static final String WHITELIST_KNOWN_USERS_KEY = "comment_whitelist";
    public static final String MAX_LINKS_KEY = "comment_max_links";
    public static final String MODERATION_KEYS_KEY = "moderation_keys";
    public static final String BLACKLIST_KEYS_KEY = "blacklist_keys";

    // WP.com REST keys used to GET certain site settings
    public static final String GET_TITLE_KEY = "name";
    public static final String GET_DESC_KEY = "description";

    // WP.com REST keys used to POST updates to site settings
    private static final String SET_TITLE_KEY = "blogname";
    private static final String SET_DESC_KEY = "blogdescription";

    // JSON response keys
    private static final String SETTINGS_KEY = "settings";
    private static final String UPDATED_KEY = "updated";

    // WP.com REST keys used in response to a categories GET request
    private static final String CAT_ID_KEY = "ID";
    private static final String CAT_NAME_KEY = "name";
    private static final String CAT_SLUG_KEY = "slug";
    private static final String CAT_DESC_KEY = "description";
    private static final String CAT_PARENT_ID_KEY = "parent";
    private static final String CAT_POST_COUNT_KEY = "post_count";
    private static final String CAT_NUM_POSTS_KEY = "found";
    private static final String CATEGORIES_KEY = "categories";

    /**
     * Only instantiated by {@link SiteSettingsInterface}.
     */
    DotComSiteSettings(Activity host, Blog blog, SiteSettingsListener listener) {
        super(host, blog, listener);
    }

    @Override
    public void saveSettings() {
        super.saveSettings();

        final Map<String, String> params = serializeDotComParams();
        if (params == null || params.isEmpty()) return;

        WordPress.getRestClientUtils().setGeneralSiteSettings(
                String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Site Settings saved remotely");
                        notifySavedOnUiThread(null);
                        mRemoteSettings.copyFrom(mSettings);

                        if (response != null) {
                            JSONObject updated = response.optJSONObject(UPDATED_KEY);
                            if (updated == null) return;
                            HashMap<String, Object> properties = new HashMap<>();
                            Iterator<String> keys = updated.keys();
                            while (keys.hasNext()) {
                                String currentKey = keys.next();
                                Object currentValue = updated.opt(currentKey);
                                if (currentValue != null) {
                                    properties.put(SAVED_ITEM_PREFIX + currentKey, currentValue);
                                }
                            }
                            AnalyticsUtils.trackWithCurrentBlogDetails(
                                    AnalyticsTracker.Stat.SITE_SETTINGS_SAVED_REMOTELY, properties);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error POSTing site settings changes: " + error);
                        notifySavedOnUiThread(error);
                    }
                }, params);
    }

    /**
     * Request remote site data via the WordPress REST API.
     */
    @Override
    protected void fetchRemoteData() {
        fetchCategories();
        WordPress.getRestClientUtils().getGeneralSettings(
                String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Received response to Settings REST request.");
                        credentialsVerified(true);

                        mRemoteSettings.localTableId = mBlog.getRemoteBlogId();
                        deserializeDotComRestResponse(mBlog, response);
                        if (!mRemoteSettings.equals(mSettings)) {
                            // postFormats setting is not returned by this api call so copy it over
                            final Map<String, String> currentPostFormats = mSettings.postFormats;

                            mSettings.copyFrom(mRemoteSettings);

                            mSettings.postFormats = currentPostFormats;

                            SiteSettingsTable.saveSettings(mSettings);
                            notifyUpdatedOnUiThread(null);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error response to Settings REST request: " + error);
                        notifyUpdatedOnUiThread(error);
                    }
                });
    }

    /**
     * Sets values from a .com REST response object.
     */
    public void deserializeDotComRestResponse(Blog blog, JSONObject response) {
        if (blog == null || response == null) return;
        JSONObject settingsObject = response.optJSONObject(SETTINGS_KEY);

        mRemoteSettings.username = blog.getUsername();
        mRemoteSettings.password = blog.getPassword();
        mRemoteSettings.address = response.optString(URL_KEY, "");
        mRemoteSettings.title = response.optString(GET_TITLE_KEY, "");
        mRemoteSettings.tagline = response.optString(GET_DESC_KEY, "");
        mRemoteSettings.languageId = settingsObject.optInt(LANGUAGE_ID_KEY, -1);
        mRemoteSettings.privacy = settingsObject.optInt(PRIVACY_KEY, -2);
        mRemoteSettings.defaultCategory = settingsObject.optInt(DEF_CATEGORY_KEY, 0);
        mRemoteSettings.defaultPostFormat = settingsObject.optString(DEF_POST_FORMAT_KEY, "0");
        mRemoteSettings.language = languageIdToLanguageCode(Integer.toString(mRemoteSettings.languageId));
        mRemoteSettings.allowComments = settingsObject.optBoolean(ALLOW_COMMENTS_KEY, true);
        mRemoteSettings.sendPingbacks = settingsObject.optBoolean(SEND_PINGBACKS_KEY, false);
        mRemoteSettings.receivePingbacks = settingsObject.optBoolean(RECEIVE_PINGBACKS_KEY, true);
        mRemoteSettings.shouldCloseAfter = settingsObject.optBoolean(CLOSE_OLD_COMMENTS_KEY, false);
        mRemoteSettings.closeCommentAfter = settingsObject.optInt(CLOSE_OLD_COMMENTS_DAYS_KEY, 0);
        mRemoteSettings.shouldThreadComments = settingsObject.optBoolean(THREAD_COMMENTS_KEY, false);
        mRemoteSettings.threadingLevels = settingsObject.optInt(THREAD_COMMENTS_DEPTH_KEY, 0);
        mRemoteSettings.shouldPageComments = settingsObject.optBoolean(PAGE_COMMENTS_KEY, false);
        mRemoteSettings.commentsPerPage = settingsObject.optInt(PAGE_COMMENT_COUNT_KEY, 0);
        mRemoteSettings.commentApprovalRequired = settingsObject.optBoolean(COMMENT_MODERATION_KEY, false);
        mRemoteSettings.commentsRequireIdentity = settingsObject.optBoolean(REQUIRE_IDENTITY_KEY, false);
        mRemoteSettings.commentsRequireUserAccount = settingsObject.optBoolean(REQUIRE_USER_ACCOUNT_KEY, true);
        mRemoteSettings.commentAutoApprovalKnownUsers = settingsObject.optBoolean(WHITELIST_KNOWN_USERS_KEY, false);
        mRemoteSettings.maxLinks = settingsObject.optInt(MAX_LINKS_KEY, 0);
        mRemoteSettings.holdForModeration = new ArrayList<>();
        mRemoteSettings.blacklist = new ArrayList<>();

        String modKeys = settingsObject.optString(MODERATION_KEYS_KEY, "");
        if (modKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.holdForModeration, modKeys.split("\n"));
        }
        String blacklistKeys = settingsObject.optString(BLACKLIST_KEYS_KEY, "");
        if (blacklistKeys.length() > 0) {
            Collections.addAll(mRemoteSettings.blacklist, blacklistKeys.split("\n"));
        }

        if (settingsObject.optString(COMMENT_SORT_ORDER_KEY, "").equals("asc")) {
            mRemoteSettings.sortCommentsBy = ASCENDING_SORT;
        } else {
            mRemoteSettings.sortCommentsBy = DESCENDING_SORT;
        }

        if (settingsObject.optBoolean(RELATED_POSTS_ALLOWED_KEY, false)) {
            mRemoteSettings.showRelatedPosts = settingsObject.optBoolean(RELATED_POSTS_ENABLED_KEY, false);
            mRemoteSettings.showRelatedPostHeader = settingsObject.optBoolean(RELATED_POSTS_HEADER_KEY, false);
            mRemoteSettings.showRelatedPostImages = settingsObject.optBoolean(RELATED_POSTS_IMAGES_KEY, false);
        }
    }

    /**
     * Helper method to create the parameters for the site settings POST request
     *
     * Using undocumented endpoint WPCOM_JSON_API_Site_Settings_Endpoint
     * https://wpcom.trac.automattic.com/browser/trunk/public.api/rest/json-endpoints.php#L1903
     */
    public Map<String, String> serializeDotComParams() {
        Map<String, String> params = new HashMap<>();

        if (mSettings.title!= null && !mSettings.title.equals(mRemoteSettings.title)) {
            params.put(SET_TITLE_KEY, mSettings.title);
        }
        if (mSettings.tagline != null && !mSettings.tagline.equals(mRemoteSettings.tagline)) {
            params.put(SET_DESC_KEY, mSettings.tagline);
        }
        if (mSettings.languageId != mRemoteSettings.languageId) {
            params.put(LANGUAGE_ID_KEY, String.valueOf((mSettings.languageId)));
        }
        if (mSettings.privacy != mRemoteSettings.privacy) {
            params.put(PRIVACY_KEY, String.valueOf((mSettings.privacy)));
        }
        if (mSettings.defaultCategory != mRemoteSettings.defaultCategory) {
            params.put(DEF_CATEGORY_KEY, String.valueOf(mSettings.defaultCategory));
        }
        if (mSettings.defaultPostFormat != null && !mSettings.defaultPostFormat.equals(mRemoteSettings.defaultPostFormat)) {
            params.put(DEF_POST_FORMAT_KEY, mSettings.defaultPostFormat);
        }
        if (mSettings.showRelatedPosts != mRemoteSettings.showRelatedPosts ||
                mSettings.showRelatedPostHeader != mRemoteSettings.showRelatedPostHeader ||
                mSettings.showRelatedPostImages != mRemoteSettings.showRelatedPostImages) {
            params.put(RELATED_POSTS_ENABLED_KEY, String.valueOf(mSettings.showRelatedPosts));
            params.put(RELATED_POSTS_HEADER_KEY, String.valueOf(mSettings.showRelatedPostHeader));
            params.put(RELATED_POSTS_IMAGES_KEY, String.valueOf(mSettings.showRelatedPostImages));
        }
        if (mSettings.allowComments != mRemoteSettings.allowComments) {
            params.put(ALLOW_COMMENTS_KEY, String.valueOf(mSettings.allowComments));
        }
        if (mSettings.sendPingbacks != mRemoteSettings.sendPingbacks) {
            params.put(SEND_PINGBACKS_KEY, String.valueOf(mSettings.sendPingbacks));
        }
        if (mSettings.receivePingbacks != mRemoteSettings.receivePingbacks) {
            params.put(RECEIVE_PINGBACKS_KEY, String.valueOf(mSettings.receivePingbacks));
        }
        if (mSettings.commentApprovalRequired != mRemoteSettings.commentApprovalRequired) {
            params.put(COMMENT_MODERATION_KEY, String.valueOf(mSettings.commentApprovalRequired));
        }
        if (mSettings.closeCommentAfter != mRemoteSettings.closeCommentAfter
                || mSettings.shouldCloseAfter != mRemoteSettings.shouldCloseAfter) {
            params.put(CLOSE_OLD_COMMENTS_KEY, String.valueOf(mSettings.shouldCloseAfter));
            params.put(CLOSE_OLD_COMMENTS_DAYS_KEY, String.valueOf(mSettings.closeCommentAfter));
        }
        if (mSettings.sortCommentsBy != mRemoteSettings.sortCommentsBy) {
            if (mSettings.sortCommentsBy == ASCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "asc");
            } else if (mSettings.sortCommentsBy == DESCENDING_SORT) {
                params.put(COMMENT_SORT_ORDER_KEY, "desc");
            }
        }
        if (mSettings.threadingLevels != mRemoteSettings.threadingLevels
                || mSettings.shouldThreadComments != mRemoteSettings.shouldThreadComments) {
            params.put(THREAD_COMMENTS_KEY, String.valueOf(mSettings.shouldThreadComments));
            params.put(THREAD_COMMENTS_DEPTH_KEY, String.valueOf(mSettings.threadingLevels));
        }
        if (mSettings.commentsPerPage != mRemoteSettings.commentsPerPage
                || mSettings.shouldPageComments != mRemoteSettings.shouldPageComments) {
            params.put(PAGE_COMMENTS_KEY, String.valueOf(mSettings.shouldPageComments));
            params.put(PAGE_COMMENT_COUNT_KEY, String.valueOf(mSettings.commentsPerPage));
        }
        if (mSettings.commentsRequireIdentity != mRemoteSettings.commentsRequireIdentity) {
            params.put(REQUIRE_IDENTITY_KEY, String.valueOf(mSettings.commentsRequireIdentity));
        }
        if (mSettings.commentsRequireUserAccount != mRemoteSettings.commentsRequireUserAccount) {
            params.put(REQUIRE_USER_ACCOUNT_KEY, String.valueOf(mSettings.commentsRequireUserAccount));
        }
        if (mSettings.commentAutoApprovalKnownUsers != mRemoteSettings.commentAutoApprovalKnownUsers) {
            params.put(WHITELIST_KNOWN_USERS_KEY, String.valueOf(mSettings.commentAutoApprovalKnownUsers));
        }
        if (mSettings.maxLinks != mRemoteSettings.maxLinks) {
            params.put(MAX_LINKS_KEY, String.valueOf(mSettings.maxLinks));
        }
        if (mSettings.holdForModeration != null && !mSettings.holdForModeration.equals(mRemoteSettings.holdForModeration)) {
            StringBuilder builder = new StringBuilder();
            for (String key : mSettings.holdForModeration) {
                builder.append(key);
                builder.append("\n");
            }
            if (builder.length() > 1) {
                params.put(MODERATION_KEYS_KEY, builder.substring(0, builder.length() - 1));
            } else {
                params.put(MODERATION_KEYS_KEY, "");
            }
        }
        if (mSettings.blacklist != null && !mSettings.blacklist.equals(mRemoteSettings.blacklist)) {
            StringBuilder builder = new StringBuilder();
            for (String key : mSettings.blacklist) {
                builder.append(key);
                builder.append("\n");
            }
            if (builder.length() > 1) {
                params.put(BLACKLIST_KEYS_KEY, builder.substring(0, builder.length() - 1));
            } else {
                params.put(BLACKLIST_KEYS_KEY, "");
            }
        }

        return params;
    }

    /**
     * Request a list of post categories for a site via the WordPress REST API.
     */
    private void fetchCategories() {
        WordPress.getRestClientUtilsV1_1().getCategories(String.valueOf(mBlog.getRemoteBlogId()),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Received response to Categories REST request.");
                        credentialsVerified(true);

                        CategoryModel[] models = deserializeJsonRestResponse(response);
                        if (models == null) return;

                        SiteSettingsTable.saveCategories(models);
                        mRemoteSettings.categories = models;
                        mSettings.categories = models;
                        notifyUpdatedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(AppLog.T.API, "Error fetching WP.com categories:" + error);
                    }
                });
    }

    private CategoryModel deserializeCategoryFromJson(JSONObject category) throws JSONException {
        if (category == null) return null;

        CategoryModel model = new CategoryModel();
        model.id = category.getInt(CAT_ID_KEY);
        model.name = category.getString(CAT_NAME_KEY);
        model.slug = category.getString(CAT_SLUG_KEY);
        model.description = category.getString(CAT_DESC_KEY);
        model.parentId = category.getInt(CAT_PARENT_ID_KEY);
        model.postCount = category.getInt(CAT_POST_COUNT_KEY);

        return model;
    }

    private CategoryModel[] deserializeJsonRestResponse(JSONObject response) {
        try {
            int num = response.getInt(CAT_NUM_POSTS_KEY);
            JSONArray categories = response.getJSONArray(CATEGORIES_KEY);
            CategoryModel[] models = new CategoryModel[num];

            for (int i = 0; i < num; ++i) {
                JSONObject category = categories.getJSONObject(i);
                models[i] = deserializeCategoryFromJson(category);
            }

            AppLog.d(AppLog.T.API, "Successfully fetched WP.com categories");

            return models;
        } catch (JSONException exception) {
            AppLog.d(AppLog.T.API, "Error parsing WP.com categories response:" + response);
            return null;
        }
    }
}
