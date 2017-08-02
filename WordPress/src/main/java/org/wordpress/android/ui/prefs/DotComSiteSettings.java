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
import org.wordpress.android.fluxc.model.SiteModel;
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
    private static final String LANGUAGE_ID_KEY = "lang_id";
    private static final String PRIVACY_KEY = "blog_public";
    private static final String URL_KEY = "URL";
    private static final String DEF_CATEGORY_KEY = "default_category";
    private static final String DEF_POST_FORMAT_KEY = "default_post_format";
    private static final String RELATED_POSTS_ALLOWED_KEY = "jetpack_relatedposts_allowed";
    private static final String RELATED_POSTS_ENABLED_KEY = "jetpack_relatedposts_enabled";
    private static final String RELATED_POSTS_HEADER_KEY = "jetpack_relatedposts_show_headline";
    private static final String RELATED_POSTS_IMAGES_KEY = "jetpack_relatedposts_show_thumbnails";
    private static final String ALLOW_COMMENTS_KEY = "default_comment_status";
    private static final String SEND_PINGBACKS_KEY = "default_pingback_flag";
    private static final String RECEIVE_PINGBACKS_KEY = "default_ping_status";
    private static final String CLOSE_OLD_COMMENTS_KEY = "close_comments_for_old_posts";
    private static final String CLOSE_OLD_COMMENTS_DAYS_KEY = "close_comments_days_old";
    private static final String THREAD_COMMENTS_KEY = "thread_comments";
    private static final String THREAD_COMMENTS_DEPTH_KEY = "thread_comments_depth";
    private static final String PAGE_COMMENTS_KEY = "page_comments";
    private static final String PAGE_COMMENT_COUNT_KEY = "comments_per_page";
    private static final String COMMENT_SORT_ORDER_KEY = "comment_order";
    private static final String COMMENT_MODERATION_KEY = "comment_moderation";
    private static final String REQUIRE_IDENTITY_KEY = "require_name_email";
    private static final String REQUIRE_USER_ACCOUNT_KEY = "comment_registration";
    private static final String WHITELIST_KNOWN_USERS_KEY = "comment_whitelist";
    private static final String MAX_LINKS_KEY = "comment_max_links";
    private static final String MODERATION_KEYS_KEY = "moderation_keys";
    private static final String BLACKLIST_KEYS_KEY = "blacklist_keys";
    private static final String SHARING_LABEL_KEY = "sharing_label";
    private static final String SHARING_BUTTON_STYLE_KEY = "sharing_button_style";
    private static final String SHARING_REBLOGS_DISABLED_KEY = "disabled_reblogs";
    private static final String SHARING_LIKES_DISABLED_KEY = "disabled_likes";
    private static final String SHARING_COMMENT_LIKES_KEY = "jetpack_comment_likes_enabled";
    private static final String TWITTER_USERNAME_KEY = "twitter_via";
    private static final String JP_MONITOR_EMAIL_NOTES_KEY = "email_notifications";
    private static final String JP_MONITOR_WP_NOTES_KEY = "wp_note_notifications";
    private static final String JP_PROTECT_WHITELIST_KEY = "jetpack_protect_whitelist";

    // WP.com REST keys used to GET certain site settings
    private static final String GET_TITLE_KEY = "name";
    private static final String GET_DESC_KEY = "description";

    // WP.com REST keys used to POST updates to site settings
    private static final String SET_TITLE_KEY = "blogname";
    private static final String SET_DESC_KEY = "blogdescription";

    // WP.com REST keys used in response to a categories GET request
    private static final String CAT_ID_KEY = "ID";
    private static final String CAT_NAME_KEY = "name";
    private static final String CAT_SLUG_KEY = "slug";
    private static final String CAT_DESC_KEY = "description";
    private static final String CAT_PARENT_ID_KEY = "parent";
    private static final String CAT_POST_COUNT_KEY = "post_count";
    private static final String CAT_NUM_POSTS_KEY = "found";
    private static final String CATEGORIES_KEY = "categories";
    private static final String DEFAULT_SHARING_BUTTON_STYLE = "icon-only";

    /**
     * Only instantiated by {@link SiteSettingsInterface}.
     */
    DotComSiteSettings(Activity host, SiteModel site, SiteSettingsListener listener) {
        super(host, site, listener);
    }

    @Override
    public void saveSettings() {
        super.saveSettings();

        // save any Jetpack changes
        if (mSite.isJetpackConnected()) {
            pushJetpackSettings();
        }

        try {
            final JSONObject jsonParams = serializeDotComParamsToJSONObject();
            // skip network requests if there are no changes
            if (jsonParams.length() <= 0) {
                return;
            }
            WordPress.getRestClientUtils().setGeneralSiteSettings(
                    mSite.getSiteId(), new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            AppLog.d(AppLog.T.API, "Site Settings saved remotely");
                            notifySavedOnUiThread(null);
                            mRemoteSettings.copyFrom(mSettings);

                            if (response != null) {
                                JSONObject updated = response.optJSONObject("updated");
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
                                AnalyticsUtils.trackWithSiteDetails(
                                        AnalyticsTracker.Stat.SITE_SETTINGS_SAVED_REMOTELY, mSite, properties);
                            }
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error POSTing site settings changes: " + error);
                            notifySavedOnUiThread(error);
                        }
                    }, jsonParams);
        } catch (JSONException exception) {
            AppLog.w(AppLog.T.API, "Error serializing settings changes: " + exception);
            notifySavedOnUiThread(exception);
        }
    }

    /**
     * Request remote site data via the WordPress REST API.
     */
    @Override
    protected void fetchRemoteData() {
        fetchCategories();

        WordPress.getRestClientUtils().getGeneralSettings(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Received response to Settings REST request.");
                        credentialsVerified(true);

                        mRemoteSettings.localTableId = mSite.getId();
                        deserializeDotComRestResponse(mSite, response);
                        if (!mRemoteSettings.equals(mSettings)) {
                            // postFormats setting is not returned by this api call so copy it over
                            final Map<String, String> currentPostFormats = mSettings.postFormats;

                            // Local settings
                            boolean location = mSettings.location;
                            boolean optimizedImage = mSettings.optimizedImage;
                            int maxImageWidth = mSettings.maxImageWidth;
                            int imageQualitySetting = mSettings.imageQualitySetting;
                            boolean optimizedVideo = mSettings.optimizedVideo;
                            int maxVideoWidth = mSettings.maxVideoWidth;
                            int videoEncoderBitrate = mSettings.videoEncoderBitrate;

                            mSettings.copyFrom(mRemoteSettings);

                            mSettings.postFormats = currentPostFormats;
                            mSettings.location = location;
                            mSettings.optimizedImage = optimizedImage;
                            mSettings.maxImageWidth = maxImageWidth;
                            mSettings.imageQualitySetting = imageQualitySetting;
                            mSettings.optimizedVideo = optimizedVideo;
                            mSettings.maxVideoWidth = maxVideoWidth;
                            mSettings.videoEncoderBitrate = videoEncoderBitrate;
                            mJpSettings.jetpackProtectWhitelist.clear();
                            mJpSettings.jetpackProtectWhitelist.addAll(mRemoteJpSettings.jetpackProtectWhitelist);

                            SiteSettingsTable.saveSettings(mSettings);
                            notifyUpdatedOnUiThread(null);
                        }

                        if (mSite.isJetpackConnected()) {
                            fetchJetpackSettings();
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

    /** Request a list of post categories for a site via the WordPress REST API. */
    private void fetchCategories() {
        // TODO: Replace with FluxC (GET_CATEGORIES + TaxonomyStore.getCategoriesForSite())
        WordPress.getRestClientUtilsV1_1().getCategories(mSite.getSiteId(),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received site Categories");
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

    private void fetchJetpackSettings() {
        fetchJetpackMonitorSettings();
        fetchJetpackProtectSettings();
        fetchJetpackSsoSettings();
    }

    private void pushJetpackSettings() {
        pushJetpackMonitorSettings();
        pushJetpackProtectSettings();
        pushJetpackSsoSettings();
    }

    private void fetchJetpackProtectSettings() {
        WordPress.getRestClientUtils().getJetpackModule(
                mSite.getSiteId(), "protect", new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received Jetpack Protect module");
                        mRemoteJpSettings.jetpackProtectEnabled = response.optBoolean("active");
                        mJpSettings.jetpackProtectEnabled = mRemoteJpSettings.jetpackProtectEnabled;
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack Protect module: " + error);
                    }
                });
    }

    private void fetchJetpackSsoSettings() {
        WordPress.getRestClientUtils().getJetpackModule(
                mSite.getSiteId(), "sso", new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received Jetpack SSO module");
                        mRemoteJpSettings.ssoActive = response.optBoolean("active");
                        mJpSettings.ssoActive = response.optBoolean("active");
                        if (mJpSettings.ssoActive) {
                            fetchJetpackSsoModuleSettings();
                        }
                        notifyUpdatedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack SSO module: " + error);
                    }
                });
    }

    private void fetchJetpackSsoModuleSettings() {
        WordPress.getRestClientUtils().getJetpackSsoMatchByEmailOption(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received Jetpack SSO module match emails option");
                        JSONArray options = response.optJSONArray("options");
                        if (options != null && options.length() > 0) {
                            try {
                                JSONObject ssoValue = options.getJSONObject(0);
                                if (ssoValue != null && ssoValue.optString("option_name", null) != null) {
                                    mRemoteJpSettings.ssoMatchEmail = ssoValue.optString("option_value", "0").equals("1");
                                    mJpSettings.ssoMatchEmail = mRemoteJpSettings.ssoMatchEmail;
                                }
                            } catch (JSONException e) {
                                AppLog.e(AppLog.T.API, "Error reading REST response to SSO settings fetch: " + e);
                            }
                            notifyUpdatedOnUiThread(null);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack SSO module match emails option: " + error);
                    }
                });
        WordPress.getRestClientUtils().getJetpackSsoTwoStepOption(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received Jetpack SSO module 2FA option");
                        mRemoteJpSettings.ssoRequireTwoFactor = response.optBoolean("option_value");
                        mJpSettings.ssoRequireTwoFactor = mRemoteJpSettings.ssoRequireTwoFactor;
                            notifyUpdatedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack SSO module 2FA option: " + error);
                    }
                });
    }

    private void fetchJetpackMonitorSettings() {
        WordPress.getRestClientUtils().getJetpackMonitor(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received Jetpack Monitor module");
                        mRemoteJpSettings.monitorActive = response.optBoolean("active");
                        mJpSettings.monitorActive = mRemoteJpSettings.monitorActive;
                        notifyUpdatedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack Monitor module: " + error);
                    }
                });

        WordPress.getRestClientUtils().getJetpackSettings(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.v(AppLog.T.API, "Received Jetpack Monitor module options");
                        mRemoteJpSettings.localTableId = mSite.getId();
                        deserializeJetpackRestResponse(mSite, response);
                        mJpSettings.localTableId = mRemoteJpSettings.localTableId;
                        mJpSettings.emailNotifications = mRemoteJpSettings.emailNotifications;
                        mJpSettings.wpNotifications = mRemoteJpSettings.wpNotifications;
                        SiteSettingsTable.saveJpSettings(mJpSettings);
                        notifyUpdatedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error fetching Jetpack Monitor module options: " + error);
                        notifyUpdatedOnUiThread(error);
                    }
                });
    }

    private void pushJetpackProtectSettings() {
        WordPress.getRestClientUtils().setJetpackProtect(
                mSite.getSiteId(), mJpSettings.jetpackProtectEnabled, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mRemoteJpSettings.jetpackProtectEnabled = response.optBoolean("active");
                        mJpSettings.jetpackProtectEnabled = mRemoteJpSettings.jetpackProtectEnabled;
                        String status = mJpSettings.jetpackProtectEnabled ? "activated" : "deactivated";
                        AppLog.d(AppLog.T.API, "Jetpack Protect module " + status);
                        notifySavedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error updating Jetpack Protect module: " + error);
                    }
                });
    }

    private void pushJetpackMonitorSettings() {
        WordPress.getRestClientUtils().setJetpackMonitor(
                mSite.getSiteId(), mJpSettings.monitorActive, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        mRemoteJpSettings.monitorActive = response.optBoolean("active");
                        mJpSettings.monitorActive = mRemoteJpSettings.monitorActive;
                        String status = mJpSettings.jetpackProtectEnabled ? "activated" : "deactivated";
                        AppLog.d(AppLog.T.API, "Jetpack Monitor module " + status);
                        notifySavedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error updating Jetpack Monitor module: " + error);
                        notifySavedOnUiThread(error);
                    }
                });

        final Map<String, String> params = serializeJetpackParams();
        if (params == null || params.isEmpty()) return;

        WordPress.getRestClientUtils().setJetpackSettings(
                mSite.getSiteId(), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Jetpack Monitor module options updated");
                        mRemoteJpSettings.emailNotifications = mJpSettings.emailNotifications;
                        mRemoteJpSettings.wpNotifications = mJpSettings.wpNotifications;
                        notifySavedOnUiThread(null);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error updating Jetpack Monitor module options: " + error);
                        notifySavedOnUiThread(error);
                    }
                }, params);
    }

    private void pushJetpackSsoSettings() {
        if (mJpSettings.ssoActive != mRemoteJpSettings.ssoActive) {
            WordPress.getRestClientUtils().setJetpackSso(
                    mSite.getSiteId(), mJpSettings.ssoActive, new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            mRemoteJpSettings.ssoActive = response.optBoolean("active");
                            mJpSettings.ssoActive = mRemoteJpSettings.ssoActive;
                            String status = mJpSettings.ssoActive ? "activated" : "deactivated";
                            AppLog.d(AppLog.T.API, "Jetpack SSO module " + status);
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error updating Jetpack SSO module: " + error);
                        }
                    });
        }

        if (mJpSettings.ssoRequireTwoFactor != mRemoteJpSettings.ssoRequireTwoFactor) {
            WordPress.getRestClientUtilsV1_1().setJetpackSsoTwoStepOption(
                    mSite.getSiteId(), mJpSettings.ssoRequireTwoFactor, new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            mRemoteJpSettings.ssoRequireTwoFactor = response.optBoolean("option_value");
                            mJpSettings.ssoRequireTwoFactor = mRemoteJpSettings.ssoRequireTwoFactor;
                            AppLog.d(AppLog.T.API, "Jetpack SSO module 2FA option updated");
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error updating Jetpack SSO module 2FA option: " + error);
                        }
                    });
        }

        if (mJpSettings.ssoMatchEmail != mRemoteJpSettings.ssoMatchEmail) {
            WordPress.getRestClientUtilsV1_1().setJetpacSsoMatchEmailOption(
                    mSite.getSiteId(), mJpSettings.ssoMatchEmail, new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            AppLog.d(AppLog.T.API, "Jetpack SSO module match email option updated");
                            mRemoteJpSettings.ssoMatchEmail = response.optBoolean("option_value");
                            mJpSettings.ssoMatchEmail = mRemoteJpSettings.ssoMatchEmail;
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.w(AppLog.T.API, "Error updating Jetpack SSO module match email option: " + error);
                        }
                    });
        }
    }

    /**
     * Sets values from a .com REST response object.
     */
    private void deserializeDotComRestResponse(SiteModel site, JSONObject response) {
        if (site == null || response == null) return;
        JSONObject settingsObject = response.optJSONObject("settings");

        mRemoteSettings.username = site.getUsername();
        mRemoteSettings.password = site.getPassword();
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
        mRemoteSettings.sharingLabel = settingsObject.optString(SHARING_LABEL_KEY, "");
        mRemoteSettings.sharingButtonStyle = settingsObject.optString(SHARING_BUTTON_STYLE_KEY, DEFAULT_SHARING_BUTTON_STYLE);
        mRemoteSettings.allowCommentLikes = settingsObject.optBoolean(SHARING_COMMENT_LIKES_KEY, false);
        mRemoteSettings.twitterUsername = settingsObject.optString(TWITTER_USERNAME_KEY, "");

        boolean reblogsDisabled = settingsObject.optBoolean(SHARING_REBLOGS_DISABLED_KEY, false);
        boolean likesDisabled = settingsObject.optBoolean(SHARING_LIKES_DISABLED_KEY, false);
        mRemoteSettings.allowReblogButton = !reblogsDisabled;
        mRemoteSettings.allowLikeButton = !likesDisabled;

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

        JSONObject jetpackProtectWhitelist = settingsObject.optJSONObject(JP_PROTECT_WHITELIST_KEY);
        if (jetpackProtectWhitelist != null) {
            JSONArray whitelistItems = jetpackProtectWhitelist.optJSONArray("local");
            if (whitelistItems != null) {
                for (int i = 0; i < whitelistItems.length(); ++i) {
                    String item = whitelistItems.optString(i, "");
                    if (!item.isEmpty() && !mRemoteJpSettings.jetpackProtectWhitelist.contains(item)) {
                        mRemoteJpSettings.jetpackProtectWhitelist.add(item);
                    }
                }
            }
        }

        if (settingsObject.optBoolean(RELATED_POSTS_ALLOWED_KEY, false)) {
            mRemoteSettings.showRelatedPosts = settingsObject.optBoolean(RELATED_POSTS_ENABLED_KEY, false);
            mRemoteSettings.showRelatedPostHeader = settingsObject.optBoolean(RELATED_POSTS_HEADER_KEY, false);
            mRemoteSettings.showRelatedPostImages = settingsObject.optBoolean(RELATED_POSTS_IMAGES_KEY, false);
        }
    }

    /**
     * Need to use JSONObject's instead of HashMap<String, String> to serialize array values (Jetpack Whitelist)
     */
    private JSONObject serializeDotComParamsToJSONObject() throws JSONException {
        JSONObject params = new JSONObject();

        if (mSettings.title!= null && !mSettings.title.equals(mRemoteSettings.title)) {
            params.put(SET_TITLE_KEY, mSettings.title);
        }
        if (mSettings.tagline != null && !mSettings.tagline.equals(mRemoteSettings.tagline)) {
            params.put(SET_DESC_KEY, mSettings.tagline);
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

        if (mSettings.sharingLabel != null && !mSettings.sharingLabel.equals(mRemoteSettings.sharingLabel)) {
            params.put(SHARING_LABEL_KEY, String.valueOf(mSettings.sharingLabel));
        }

        if (mSettings.sharingButtonStyle != null && !mSettings.sharingButtonStyle.equals(mRemoteSettings.sharingButtonStyle)) {
            params.put(SHARING_BUTTON_STYLE_KEY, mSettings.sharingButtonStyle);
        }

        if (mSettings.allowReblogButton != mRemoteSettings.allowReblogButton) {
            params.put(SHARING_REBLOGS_DISABLED_KEY, String.valueOf(!mSettings.allowReblogButton));
        }

        if (mSettings.allowLikeButton != mRemoteSettings.allowLikeButton) {
            params.put(SHARING_LIKES_DISABLED_KEY, String.valueOf(!mSettings.allowLikeButton));
        }

        if (mSettings.allowCommentLikes != mRemoteSettings.allowCommentLikes) {
            params.put(SHARING_COMMENT_LIKES_KEY, String.valueOf(mSettings.allowCommentLikes));
        }

        if (mSettings.twitterUsername != null && !mSettings.twitterUsername.equals(mRemoteSettings.twitterUsername)) {
            params.put(TWITTER_USERNAME_KEY, mSettings.twitterUsername);
        }

        if (!mJpSettings.whitelistMatches(mRemoteJpSettings.jetpackProtectWhitelist)) {
            JSONArray protectWhitelist = new JSONArray(mJpSettings.jetpackProtectWhitelist);
            params.put(JP_PROTECT_WHITELIST_KEY, protectWhitelist);
        }

        return params;
    }

    private void deserializeJetpackRestResponse(SiteModel site, JSONObject response) {
        if (site == null || response == null) return;
        JSONObject settingsObject = response.optJSONObject("settings");
        mRemoteJpSettings.emailNotifications = settingsObject.optBoolean(JP_MONITOR_EMAIL_NOTES_KEY, false);
        mRemoteJpSettings.wpNotifications = settingsObject.optBoolean(JP_MONITOR_WP_NOTES_KEY, false);
    }

    private Map<String, String> serializeJetpackParams() {
        Map<String, String> params = new HashMap<>();
        params.put(JP_MONITOR_EMAIL_NOTES_KEY, String.valueOf(mJpSettings.emailNotifications));
        params.put(JP_MONITOR_WP_NOTES_KEY, String.valueOf(mJpSettings.wpNotifications));
        return params;
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
