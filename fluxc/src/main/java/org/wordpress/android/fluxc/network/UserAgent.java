package org.wordpress.android.fluxc.network;

import android.content.Context;
import android.webkit.WebSettings;

import org.wordpress.android.util.PackageUtils;

public class UserAgent {
    private final String mUserAgent;
    private final String mDefaultUserAgent;

    public UserAgent(Context appContext, String appName) {
        // Device's default User-Agent string.
        // E.g.:
        //   "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
        //   AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36"
        mDefaultUserAgent = WebSettings.getDefaultUserAgent(appContext);
        // User-Agent string when making HTTP connections, for both API traffic and WebViews.
        // Appends "wp-android/version" to WebView's default User-Agent string for the webservers
        // to get the full feature list of the browser and serve content accordingly, e.g.:
        //    "Mozilla/5.0 (Linux; Android 6.0; Android SDK built for x86_64 Build/MASTER; wv)
        //    AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/44.0.2403.119 Mobile Safari/537.36
        //    wp-android/4.7"
        mUserAgent = mDefaultUserAgent + " " + appName + "/" + PackageUtils.getVersionName(appContext);
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
}
