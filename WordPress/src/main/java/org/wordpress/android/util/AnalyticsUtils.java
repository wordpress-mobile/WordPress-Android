package org.wordpress.android.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;

import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTrackerMixpanel;
import org.wordpress.android.models.AccountHelper;

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
        boolean isUserConnected = AccountHelper.isSignedIn();
        boolean isWordPressComUser = AccountHelper.isSignedInWordPressDotCom();
        boolean isJetpackUser = AccountHelper.isJetPackUser();
        int numBlogs = WordPress.wpDB.getVisibleBlogs().size();
        int versionCode = PackageUtils.getVersionCode(WordPress.getContext());
        AnalyticsTracker.refreshMetadata(isUserConnected, isWordPressComUser, isJetpackUser, sessionCount, numBlogs,
                versionCode, username, email);
    }

    /**
     * Utility method to refresh mixpanel metadata.
     */
    public static void refreshMetadata() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext());
        int sessionCount = preferences.getInt(AnalyticsTrackerMixpanel.SESSION_COUNT, 0);
        boolean isUserConnected = AccountHelper.isSignedIn();
        boolean isWordPressComUser = AccountHelper.isSignedInWordPressDotCom();
        boolean isJetpackUser = AccountHelper.isJetPackUser();
        int numBlogs = WordPress.wpDB.getVisibleBlogs().size();
        int versionCode = PackageUtils.getVersionCode(WordPress.getContext());
        String username = AccountHelper.getDefaultAccount().getUserName();
        String email = AccountHelper.getDefaultAccount().getEmail();
        AnalyticsTracker.refreshMetadata(isUserConnected, isWordPressComUser, isJetpackUser, sessionCount, numBlogs,
                versionCode, username, email);
    }

    public static int getWordCount(String content) {
        String text = Html.fromHtml(content.replaceAll("<img[^>]*>", "")).toString();
        return text.split("\\s+").length;
    }
}
