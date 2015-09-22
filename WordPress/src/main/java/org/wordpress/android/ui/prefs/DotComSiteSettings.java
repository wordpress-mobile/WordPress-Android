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
    // Settings WP.com REST response keys
    public static final String BLOG_TITLE_KEY = "blogname";
    public static final String BLOG_DESC_KEY = "blogdescription";
    public static final String BLOG_LANG_ID_KEY = "lang_id";
    public static final String BLOG_PRIVACY_KEY = "blog_public";
    public static final String BLOG_DEF_CATEGORY_KEY = "default_category";
    public static final String BLOG_DEF_POST_FORMAT_KEY = "default_post_format";

    // Categories WP.com REST response keys
    public static final String ID_KEY = "ID";
    public static final String NAME_KEY = "name";
    public static final String SLUG_KEY = "slug";
    public static final String DESC_KEY = "description";
    public static final String PARENT_ID_KEY = "parent";
    public static final String POST_COUNT_KEY = "post_count";
    public static final String NUM_POSTS_KEY = "found";
    public static final String CATEGORIES_KEY = "categories";

    /**
     * Only instantiated by {@link SiteSettingsInterface}.
     */
    DotComSiteSettings(Activity host, Blog blog, SiteSettingsListener listener) {
        super(host, blog, listener);
    }

    @Override
    public void saveSettings() {
        // Save current settings and attempt to sync remotely
        SiteSettingsTable.saveSettings(mSettings);

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

                        mRemoteSettings.localTableId = mBlog.getLocalTableBlogId();
                        mRemoteSettings.deserializeDotComRestResponse(mBlog, response);
                        mRemoteSettings.language = languageIdToLanguageCode(
                                Integer.toString(mRemoteSettings.languageId));
                        if (!mRemoteSettings.isTheSame(mSettings)) {
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
     * Helper method to create the parameters for the site settings POST request
     *
     * Using undocumented endpoint WPCOM_JSON_API_Site_Settings_Endpoint
     * https://wpcom.trac.automattic.com/browser/trunk/public.api/rest/json-endpoints.php#L1903
     */
    public Map<String, String> serializeDotComParams() {
        Map<String, String> params = new HashMap<>();

        if (mSettings.title!= null && !mSettings.title.equals(mRemoteSettings.title)) {
            params.put(BLOG_TITLE_KEY, mSettings.title);
        }
        if (mSettings.tagline != null && !mSettings.tagline.equals(mRemoteSettings.tagline)) {
            params.put(BLOG_DESC_KEY, mSettings.tagline);
        }
        if (mSettings.languageId != mRemoteSettings.languageId) {
            params.put(BLOG_LANG_ID_KEY, String.valueOf((mSettings.languageId)));
        }
        if (mSettings.privacy != mRemoteSettings.privacy) {
            params.put(BLOG_PRIVACY_KEY, String.valueOf((mSettings.privacy)));
        }
        if (mSettings.defaultCategory != mRemoteSettings.defaultCategory) {
            params.put(BLOG_DEF_CATEGORY_KEY, String.valueOf(mSettings.defaultCategory));
        }
        if (mSettings.defaultPostFormat != null && !mSettings.defaultPostFormat.equals(mRemoteSettings.defaultPostFormat)) {
            params.put(BLOG_DEF_POST_FORMAT_KEY, mSettings.defaultPostFormat);
        }

        return params;
    }

    /**
     * Request a list of post categories for a site via the WordPress REST API.
     */
    private void fetchCategories() {
        WordPress.getRestClientUtilsV1_1().getCategories(Integer.toString(mBlog.getRemoteBlogId()),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Received response to Categories REST request.");

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
