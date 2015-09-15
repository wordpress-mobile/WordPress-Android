package org.wordpress.android.ui.prefs;

import android.app.Activity;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.util.AppLog;

import java.util.Map;

class DotComSiteSettings extends SiteSettingsInterface {

    DotComSiteSettings(Activity host, Blog blog, SiteSettingsListener listener) {
        super(host, blog, listener);
    }

    @Override
    public void saveSettings() {
        // Save current settings and attempt to sync remotely
        SiteSettingsTable.saveSettings(mSettings);

        final Map<String, String> params =
                mSettings.serializeDotComParams(mRemoteSettings);
        if (params == null || params.isEmpty()) return;

        WordPress.getRestClientUtils().setGeneralSiteSettings(
                String.valueOf(mBlog.getRemoteBlogId()), new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(AppLog.T.API, "Site Settings saved remotely");
                        saveOnUiThread(null, mSettings);
                        mRemoteSettings.copyFrom(mSettings);
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error POSTing site settings changes: " + error);
                        saveOnUiThread(error, null);
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
                        mRemoteSettings.language = convertLanguageIdToLanguageCode(
                                Integer.toString(mRemoteSettings.languageId));
                        if (!mRemoteSettings.isTheSame(mSettings)) {
                            updateOnUiThread(null, mRemoteSettings);
                            mSettings.copyFrom(mRemoteSettings);
                            SiteSettingsTable.saveSettings(mSettings);
                        }
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.w(AppLog.T.API, "Error response to Settings REST request: " + error);
                        updateOnUiThread(error, null);
                    }
                });
    }

    /**
     * Request a list of post categories for a site via the WordPress REST API.
     */
    private void fetchCategories() {
        WordPress.getRestClientUtilsV1_1().getCategories(Integer.toString(mBlog.getRemoteBlogId()),
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        CategoryModel[] models = CategoryModel.deserializeFromDotComRestResponse(response);
                        if (models == null) return;

                        SiteSettingsTable.saveCategories(models);
                        mRemoteSettings.categories = models;
                        mSettings.categories = models;
                        updateOnUiThread(null, mSettings);

                        AppLog.d(AppLog.T.API, "Successfully fetched WP.com categories");
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(AppLog.T.API, "Error fetching WP.com categories:" + error);
                    }
                });
    }
}
