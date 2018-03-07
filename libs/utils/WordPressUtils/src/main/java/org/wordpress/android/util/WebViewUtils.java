package org.wordpress.android.util;

import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;

public class WebViewUtils {
    public static void clearCookiesAsync() {
        clearCookiesAsync(null);
    }

    public static void clearCookiesAsync(ValueCallback<Boolean> callback) {
        CookieManager cookieManager = CookieManager.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(callback);
        } else {
            // noinspection deprecation
            cookieManager.removeAllCookie();
        }
    }
}
