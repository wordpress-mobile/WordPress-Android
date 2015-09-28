package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.text.TextUtils;

import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.CategoryModel;
import org.wordpress.android.models.SiteSettingsModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MapUtils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCException;

import java.util.HashMap;
import java.util.Map;

class SelfHostedSiteSettings extends SiteSettingsInterface {
    // XML-RPC wp.getOptions keys
    private static final String BLOG_URL_KEY = "blog_url";
    private static final String BLOG_TITLE_KEY = "blog_title";
    private static final String BLOG_TAGLINE_KEY = "blog_tagline";
    private static final String BLOG_CATEGORY_ID_KEY = "categoryId";
    private static final String BLOG_CATEGORY_PARENT_ID_KEY = "parentId";
    private static final String BLOG_CATEGORY_DESCRIPTION_KEY = "categoryDescription";
    private static final String BLOG_CATEGORY_NAME_KEY = "categoryName";

    SelfHostedSiteSettings(Activity host, Blog blog, SiteSettingsListener listener) {
        super(host, blog, listener);
    }

    @Override
    protected SiteSettingsInterface init(boolean fetch) {
        super.init(fetch);

        if (mSettings.defaultCategory == 0) {
            mSettings.defaultCategory = siteSettingsPreferences(mActivity).getInt(DEF_CATEGORY_PREF_KEY, 0);
        }
        if (TextUtils.isEmpty(mSettings.defaultPostFormat) || mSettings.defaultPostFormat.equals("0")) {
            mSettings.defaultPostFormat = siteSettingsPreferences(mActivity).getString(DEF_FORMAT_PREF_KEY, "0");
        }

        return this;
    }

    public Map<String, String> serializeSelfHostedParams() {
        Map<String, String> params = new HashMap<>();

        if (mSettings.title != null && !mSettings.title.equals(mRemoteSettings.title)) {
            params.put(BLOG_TITLE_KEY, mSettings.title);
        }
        if (mSettings.tagline != null && !mSettings.tagline.equals(mRemoteSettings.tagline)) {
            params.put(BLOG_TAGLINE_KEY, mSettings.tagline);
        }

        return params;
    }

    @Override
    public void saveSettings() {
        super.saveSettings();

        final Map<String, String> params = serializeSelfHostedParams();
        if (params == null || params.isEmpty()) return;

        XMLRPCCallback callback = new XMLRPCCallback() {
            @Override
            public void onSuccess(long id, final Object result) {
                notifySavedOnUiThread(null);
                mRemoteSettings.copyFrom(mSettings);
            }

            @Override
            public void onFailure(long id, final Exception error) {
                notifySavedOnUiThread(error);
            }
        };
        final Object[] callParams = {
                mBlog.getRemoteBlogId(), mSettings.username, mSettings.password, params
        };

        XMLRPCClientInterface xmlrpcInterface = instantiateInterface();
        if (xmlrpcInterface == null) return;
        xmlrpcInterface.callAsync(callback, ApiHelper.Methods.SET_OPTIONS, callParams);
    }

    /**
     * Request remote site data via XML-RPC.
     */
    @Override
    protected void fetchRemoteData() {
        Object[] params = {mBlog.getRemoteBlogId(), mBlog.getUsername(), mBlog.getPassword()};

        // Need two interfaces or the first call gets aborted
        instantiateInterface().callAsync(mOptionsCallback, ApiHelper.Methods.GET_OPTIONS, params);
        instantiateInterface().callAsync(mCategoriesCallback, ApiHelper.Methods.GET_CATEGORIES, params);
    }

    /**
     * Handles response to fetching self-hosted site categories via XML-RPC.
     */
    private final XMLRPCCallback mCategoriesCallback = new XMLRPCCallback() {
        @Override
        public void onSuccess(long id, Object result) {
            if (result instanceof Object[]) {
                AppLog.d(AppLog.T.API, "Received Categories XML-RPC response.");
                credentialsVerified(true);

                deserializeCategoriesResponse(mRemoteSettings, (Object[]) result);
                mSettings.categories = mRemoteSettings.categories;
                SiteSettingsTable.saveSettings(mSettings);
                notifyUpdatedOnUiThread(null);
            } else {
                // Response is considered an error if we are unable to parse it
                AppLog.w(AppLog.T.API, "Error parsing Categories XML-RPC response: " + result);
                notifyUpdatedOnUiThread(new XMLRPCException("Unknown response object"));
            }
        }

        @Override
        public void onFailure(long id, Exception error) {
            AppLog.w(AppLog.T.API, "Error Categories XML-RPC response: " + error);
            notifyUpdatedOnUiThread(error);
        }
    };

    /**
     * Handles response to fetching self-hosted site options via XML-RPC.
     */
    private final XMLRPCCallback mOptionsCallback = new XMLRPCCallback() {
        @Override
        public void onSuccess(long id, final Object result) {
            if (result instanceof Map) {
                AppLog.d(AppLog.T.API, "Received Options XML-RPC response.");
                credentialsVerified(true);

                deserializeOptionsResponse(mRemoteSettings, (Map) result);
                mSettings.copyFrom(mRemoteSettings);
                SiteSettingsTable.saveSettings(mSettings);
                notifyUpdatedOnUiThread(null);
            } else {
                // Response is considered an error if we are unable to parse it
                AppLog.w(AppLog.T.API, "Error parsing Options XML-RPC response: " + result);
                notifyUpdatedOnUiThread(new XMLRPCException("Unknown response object"));
            }
        }

        @Override
        public void onFailure(long id, final Exception error) {
            AppLog.w(AppLog.T.API, "Error Options XML-RPC response: " + error);
            notifyUpdatedOnUiThread(error);
        }
    };

    /**
     * Sets values from a self-hosted XML-RPC response object.
     */
    private void deserializeOptionsResponse(SiteSettingsModel model, Map response) {
        if (mBlog == null || response == null) return;

        model.username = mBlog.getUsername();
        model.password = mBlog.getPassword();
        model.address = getNestedMapValue(response, BLOG_URL_KEY);
        model.title = getNestedMapValue(response, BLOG_TITLE_KEY);
        model.tagline = getNestedMapValue(response, BLOG_TAGLINE_KEY);
    }

    private void deserializeCategoriesResponse(SiteSettingsModel model, Object[] response) {
        model.categories = new CategoryModel[response.length];

        for (int i = 0; i < response.length; ++i) {
            if (response[i] instanceof Map) {
                Map category = (Map) response[i];
                CategoryModel categoryModel = new CategoryModel();
                categoryModel.id = MapUtils.getMapInt(category, BLOG_CATEGORY_ID_KEY);
                categoryModel.parentId = MapUtils.getMapInt(category, BLOG_CATEGORY_PARENT_ID_KEY);
                categoryModel.description = MapUtils.getMapStr(category, BLOG_CATEGORY_DESCRIPTION_KEY);
                categoryModel.name = MapUtils.getMapStr(category, BLOG_CATEGORY_NAME_KEY);
                model.categories[i] = categoryModel;
            }
        }
    }

    /**
     * Helper method to get a value from a nested Map. Used to parse self-hosted response objects.
     */
    private String getNestedMapValue(Map map, String key) {
        if (map != null && key != null) {
            return MapUtils.getMapStr((Map) map.get(key), "value");
        }

        return "";
    }
}
