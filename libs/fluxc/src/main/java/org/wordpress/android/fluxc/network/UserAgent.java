package org.wordpress.android.fluxc.network;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.webkit.WebView;

public class UserAgent {
    private static final String USER_AGENT_APPNAME = "wp-android";
    private String mUserAgent;
    private String mDefaultUserAgent;

    public UserAgent(Context appContext) {
        // Device's default User-Agent string.
        // E.g.:
        //   "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
        //   AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36"
        mDefaultUserAgent = new WebView(appContext).getSettings().getUserAgentString();
        // User-Agent string when making HTTP connections, for both API traffic and WebViews.
        // Appends "wp-android/version" to WebView's default User-Agent string for the webservers
        // to get the full feature list of the browser and serve content accordingly, e.g.:
        //    "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
        //    AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36
        //    wp-android/4.7"
        // Note that app versions prior to 4.7 simply used "wp-android" as the user agent
        mUserAgent = mDefaultUserAgent + " " + USER_AGENT_APPNAME + "/" + getVersionName(appContext);
    }

    public String getUserAgent() {
        return mUserAgent;
    }

    public String getDefaultUserAgent() {
        return mDefaultUserAgent;
    }

    @Override
    public String toString() {
        return getUserAgent();
    }

    // TODO: reuse util methods
    private PackageInfo getPackageInfo(Context context) {
        try {
            PackageManager manager = context.getPackageManager();
            return manager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private String getVersionName(Context context) {
        PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo != null) {
            return packageInfo.versionName;
        }
        return "0";
    }
}
