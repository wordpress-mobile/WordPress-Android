package org.wordpress.android.util;

import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.List;

/**
 * WebViewClient that adds the ability of restrict URL loading (navigation) to a list of allowed URLs.
 * Generally used to disable links and navigation in admin pages.
 */
public class URLFilteredWebViewClient extends WebViewClient {
    ArrayList<String> allowedURLs = new ArrayList<>();

    public URLFilteredWebViewClient() {
    }

    public URLFilteredWebViewClient(String url) {
       allowedURLs.add(url);
    }

    public URLFilteredWebViewClient(List<String> urls) {
        if (urls == null || urls.size() == 0) {
            AppLog.w(AppLog.T.UTILS, "No valid URLs passed to URLFilteredWebViewClient! " +
                    "HTTP Links in the page are NOT disabled, and ALL URLs could be loaded by the user!!");
            return;
        }
        allowedURLs.addAll(urls);
    }

    protected boolean isAllURLsAllowed() {
        return allowedURLs.size() == 0;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Found a bug on some pages where there is an incorrect
        // auto-redirect to file:///android_asset/webkit/.
        if (url.equals("file:///android_asset/webkit/")) {
            return true;
        }

        if (isAllURLsAllowed() || allowedURLs.contains(url)) {
            view.loadUrl(url);
        }
        return true;
    }
}