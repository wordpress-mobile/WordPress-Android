package org.wordpress.android.ui.prefs;

import android.app.Activity;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.util.AppLog;

import java.util.HashMap;
import java.util.Map;

class DotComSiteSettings extends SiteSettingsInterface {
    // WP.com REST keys used in response to a settings GET request
    public static final String GET_TITLE_KEY = "name";
    public static final String GET_DESC_KEY = "description";
    public static final String GET_LANGUAGE_ID_KEY = "lang_id";
    public static final String GET_PRIVACY_KEY = "blog_public";
    public static final String GET_URL_KEY = "URL";
    public static final String GET_DEF_CATEGORY_KEY = "default_category";
    public static final String GET_DEF_POST_FORMAT_KEY = "default_post_format";

    // WP.com REST keys used to POST updates to site settings
    private static final String SET_TITLE_KEY = "blogname";
    private static final String SET_DESC_KEY = "blogdescription";
    private static final String SET_LANG_ID_KEY = GET_LANGUAGE_ID_KEY;
    private static final String SET_PRIVACY_KEY = GET_PRIVACY_KEY;
    private static final String SET_DEF_CATEGORY_KEY = GET_DEF_CATEGORY_KEY;
    private static final String SET_DEF_POST_FORMAT_KEY = GET_DEF_POST_FORMAT_KEY;

    // WP.com REST keys used in response to a categories GET request
    private static final String ID_KEY = "ID";
    private static final String NAME_KEY = "name";
    private static final String SLUG_KEY = "slug";
    private static final String DESC_KEY = "description";
    private static final String PARENT_ID_KEY = "parent";
    private static final String POST_COUNT_KEY = "post_count";
    private static final String NUM_POSTS_KEY = "found";
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
                            mSettings.copyFrom(mRemoteSettings);
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
        JSONObject settingsObject = response.optJSONObject("settings");

        mRemoteSettings.username = blog.getUsername();
        mRemoteSettings.password = blog.getPassword();
        mRemoteSettings.address = response.optString(GET_URL_KEY, "");
        mRemoteSettings.title = response.optString(GET_TITLE_KEY, "");
        mRemoteSettings.tagline = response.optString(GET_DESC_KEY, "");
        mRemoteSettings.languageId = settingsObject.optInt(GET_LANGUAGE_ID_KEY, -1);
        mRemoteSettings.privacy = settingsObject.optInt(GET_PRIVACY_KEY, -2);
        mRemoteSettings.defaultCategory = settingsObject.optInt(GET_DEF_CATEGORY_KEY, 0);
        mRemoteSettings.defaultPostFormat = settingsObject.optString(GET_DEF_POST_FORMAT_KEY, "0");
        mRemoteSettings.language = languageIdToLanguageCode(Integer.toString(mRemoteSettings.languageId));
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
            params.put(SET_LANG_ID_KEY, String.valueOf((mSettings.languageId)));
        }
        if (mSettings.privacy != mRemoteSettings.privacy) {
            params.put(SET_PRIVACY_KEY, String.valueOf((mSettings.privacy)));
        }
        if (mSettings.defaultCategory != mRemoteSettings.defaultCategory) {
            params.put(SET_DEF_CATEGORY_KEY, String.valueOf(mSettings.defaultCategory));
        }
        if (mSettings.defaultPostFormat != null && !mSettings.defaultPostFormat.equals(mRemoteSettings.defaultPostFormat)) {
            params.put(SET_DEF_POST_FORMAT_KEY, mSettings.defaultPostFormat);
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
        model.id = category.getInt(ID_KEY);
        model.name = category.getString(NAME_KEY);
        model.slug = category.getString(SLUG_KEY);
        model.description = category.getString(DESC_KEY);
        model.parentId = category.getInt(PARENT_ID_KEY);
        model.postCount = category.getInt(POST_COUNT_KEY);

        return model;
    }

    private CategoryModel[] deserializeJsonRestResponse(JSONObject response) {
        try {
            int num = response.getInt(NUM_POSTS_KEY);
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