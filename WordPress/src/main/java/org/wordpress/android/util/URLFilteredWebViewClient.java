package org.wordpress.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
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
    public interface URLWebViewClientListener {
        void onPageLoaded();
        void onError();
    }

    private Set<String> mAllowedURLs = new LinkedHashSet<>();
    private int mLinksDisabledMessageResId = org.wordpress.android.R.string.preview_screen_links_disabled;
    private boolean mWebResourceError;
    private URLWebViewClientListener mListener;

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

    public void setListener(URLWebViewClientListener listener) {
        mListener = listener;
    }

    protected boolean isAllURLsAllowed() {
        return mAllowedURLs.size() == 0;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mWebResourceError = false;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (mListener != null && !mWebResourceError) {
            mListener.onPageLoaded();
        }
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        super.onReceivedError(view, request, error);
        mWebResourceError = true;
        if (mListener != null) {
            mListener.onError();
        }
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
