package org.wordpress.android.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog.T;

public class AnalyticsUtils {
    /**
     * Utility method to refresh mixpanel metadata.
     *
     * @param username WordPress.com username
     * @param email WordPress.com email address
     */
    public static void refreshMetadata(String username, String email) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0);
        boolean isUserConnected = WordPress.isSignedIn(WordPress.getContext());
        boolean isJetpackUser = WordPress.wpDB.hasAnyJetpackBlogs();
        int numBlogs = WordPress.wpDB.getVisibleAccounts().size();
        int versionCode = PackageUtils.getVersionCode(WordPress.getContext());

        retrieveAndSaveEmailAddressIfApplicable();
        AnalyticsTracker.refreshMetadata(isUserConnected, isJetpackUser, sessionCount, numBlogs, versionCode,
                username, email);
    }

    /**
     * Utility method to refresh mixpanel metadata.
     */
    public static void refreshMetadata() {
        // retrieve email address if user is logged in
        retrieveAndSaveEmailAddressIfApplicable();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0);
        boolean isUserConnected = WordPress.isSignedIn(WordPress.getContext());
        boolean isJetpackUser = WordPress.wpDB.hasAnyJetpackBlogs();
        int numBlogs = WordPress.wpDB.getVisibleAccounts().size();
        int versionCode = PackageUtils.getVersionCode(WordPress.getContext());
        String username = preferences.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
        String email = AppPrefs.getMixpanelUserEmail();
        AnalyticsTracker.refreshMetadata(isUserConnected, isJetpackUser, sessionCount, numBlogs, versionCode,
                username, email);
    }

    /**
     * Fetch user email address with the REST api, will be used later to fill Mixpanel metadata.
     */
    private static void retrieveAndSaveEmailAddressIfApplicable() {
        // Once the email address is bound to a mixpanel profile, we don't need to set (and get it) a second time.
        if (AppPrefs.getMixpanelUserEmail() != null) {
            return;
        }
        RestRequest.Listener listener = new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                try {
                    if (jsonObject != null && !TextUtils.isEmpty(jsonObject.getString("email"))) {
                        String email = jsonObject.getString("email");
                        AppPrefs.setMixpanelUserEmail(email);
                    }
                } catch (JSONException e) {
                    AppLog.e(T.UTILS, "Can't get email field from json response: " + jsonObject);
                }
            }
        };
        RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(T.UTILS, volleyError);
            }
        };

        String path = "/me";
        WordPress.getRestClientUtils().get(path, listener, errorListener);
    }
}
