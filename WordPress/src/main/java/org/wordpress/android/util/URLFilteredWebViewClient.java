package org.wordpress.android.util;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.wordpress.android.WordPress;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * WebViewClient that adds the ability of restrict URL loading (navigation) to a list of allowed URLs.
 * Generally used to disable links and navigation in admin pages.
 */
public class URLFilteredWebViewClient extends WebViewClient {
    private Set<String> mAllowedURLs = new LinkedHashSet<>();
    private int mLinksDisabledMessageResId = org.wordpress.android.R.string.preview_screen_links_disabled;

    public URLFilteredWebViewClient() {
    }

    public URLFilteredWebViewClient(String url) {
        mAllowedURLs.add(url);
    }

    public URLFilteredWebViewClient(Collection<String> urls) {
        if (urls == null || urls.size() == 0) {
            AppLog.w(AppLog.T.UTILS, "No valid URLs passed to URLFilteredWebViewClient! HTTP Links in the"
                                     + " page are NOT disabled, and ALL URLs could be loaded by the user!!");
            return;
        }
        mAllowedURLs.addAll(urls);
    }

    protected boolean isAllURLsAllowed() {
        return mAllowedURLs.size() == 0;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        // Found a bug on some pages where there is an incorrect
        // auto-redirect to file:///android_asset/webkit/.
        if (url.equals("file:///android_asset/webkit/")) {
            return true;
        }

        if (isAllURLsAllowed() || mAllowedURLs.contains(url)) {
            view.loadUrl(url);
        } else {
            // show "links are disabled" message.
            Context ctx = WordPress.getContext();
            Toast.makeText(ctx, ctx.getText(mLinksDisabledMessageResId), Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    public void setLinksDisabledMessageResId(int resId) {
        mLinksDisabledMessageResId = resId;
    }
}
