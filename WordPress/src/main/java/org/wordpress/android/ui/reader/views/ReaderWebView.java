package org.wordpress.android.ui.reader.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPUrlUtils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

/*
 * WebView descendant used by ReaderPostDetailFragment - handles
 * displaying fullscreen video and detecting url/image clicks
 */
public class ReaderWebView extends WebView {

    public interface ReaderWebViewUrlClickListener {
        @SuppressWarnings("SameReturnValue")
        boolean onUrlClick(String url);
        boolean onImageUrlClick(String imageUrl, View view, int x, int y);
    }

    public interface ReaderCustomViewListener {
        void onCustomViewShown();
        void onCustomViewHidden();
        ViewGroup onRequestCustomView();
        ViewGroup onRequestContentView();
    }

    public interface ReaderWebViewPageFinishedListener {
        void onPageFinished(WebView view, String url);
    }

    /** Timeout in milliseconds for read / connect timeouts */
    private static final int TIMEOUT_MS = 30000;

    private ReaderWebChromeClient mReaderChromeClient;
    private ReaderCustomViewListener mCustomViewListener;
    private ReaderWebViewUrlClickListener mUrlClickListener;
    private ReaderWebViewPageFinishedListener mPageFinishedListener;

    private static String mToken;
    private static boolean mPrivatePostEh;
    private static boolean mBlogSchemeIsHttps;

    private boolean mDestroyedEh;
    @Inject AccountStore mAccountStore;

    public ReaderWebView(Context context) {
        super(context);
        init(context);
    }

    public ReaderWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ReaderWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    @SuppressLint("NewApi")
    private void init(Context context) {
        ((WordPress) context.getApplicationContext()).component().inject(this);

        if (!isInEditMode()) {
            mToken = mAccountStore.getAccessToken();

            mReaderChromeClient = new ReaderWebChromeClient(this);
            this.setWebChromeClient(mReaderChromeClient);
            this.setWebViewClient(new ReaderWebViewClient(this));
            this.getSettings().setUserAgentString(WordPress.getUserAgent());

            // Adjust content font size on APIs 19 and below as those do not do it automatically.
            //  If fontScale is close to 1, just let it be 1.
            final float fontScale = getResources().getConfiguration().fontScale;
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT && ((int) (fontScale * 10000)) != 10000) {

                this.getSettings().setDefaultFontSize((int) (this.getSettings().getDefaultFontSize() * fontScale));
                this.getSettings().setDefaultFixedFontSize(
                        (int) (this.getSettings().getDefaultFixedFontSize() * fontScale));
            }

            // Lollipop disables third-party cookies by default, but we need them in order
            // to support authenticated images
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
            }
        }
    }

    @Override
    public void destroy() {
        mDestroyedEh = true;
        super.destroy();
    }

    public boolean destroyedEh() {
        return mDestroyedEh;
    }


    public void clearContent() {
        loadUrl("about:blank");
    }

    private ReaderWebViewUrlClickListener getUrlClickListener() {
        return mUrlClickListener;
    }

    public void setUrlClickListener(ReaderWebViewUrlClickListener listener) {
        mUrlClickListener = listener;
    }

    private boolean urlClickListenerEh() {
        return (mUrlClickListener != null);
    }

    private ReaderWebViewPageFinishedListener getPageFinishedListener() {
        return mPageFinishedListener;
    }

    public void setPageFinishedListener(ReaderWebViewPageFinishedListener listener) {
        mPageFinishedListener = listener;
    }

    private boolean pageFinishedListenerEh() {
        return (mPageFinishedListener != null);
    }

    public void setCustomViewListener(ReaderCustomViewListener listener) {
        mCustomViewListener = listener;
    }

    private boolean customViewListenerEh() {
        return (mCustomViewListener != null);
    }

    private ReaderCustomViewListener getCustomViewListener() {
        return mCustomViewListener;
    }

    public void setIsPrivatePost(boolean privatePostEh) {
        mPrivatePostEh = privatePostEh;
    }

    public void setBlogSchemeIsHttps(boolean blogSchemeIsHttps) {
        mBlogSchemeIsHttps = blogSchemeIsHttps;
    }

    private static boolean validClickedUrlEh(String url) {
        // only return true for http(s) urls so we avoid file: and data: clicks
        return (url != null && (url.startsWith("http") || url.startsWith("wordpress:")));
    }

    public boolean customViewShowingEh() {
        return mReaderChromeClient.customViewShowingEh();
    }

    public void hideCustomView() {
        if (customViewShowingEh()) {
            mReaderChromeClient.onHideCustomView();
        }
    }

    /*
     * detect when a link is tapped
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && mUrlClickListener != null) {
            HitTestResult hr = getHitTestResult();
            if (hr != null && validClickedUrlEh(hr.getExtra())) {
                if (UrlUtils.imageUrlEh(hr.getExtra())) {
                    return mUrlClickListener.onImageUrlClick(
                            hr.getExtra(),
                            this,
                            (int) event.getX(),
                            (int) event.getY());
                } else {
                    return mUrlClickListener.onUrlClick(hr.getExtra());
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private static class ReaderWebViewClient extends WebViewClient {
        private final ReaderWebView mReaderWebView;

        ReaderWebViewClient(ReaderWebView readerWebView) {
            if (readerWebView == null) {
                throw new IllegalArgumentException("ReaderWebViewClient requires readerWebView");
            }
            mReaderWebView = readerWebView;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (mReaderWebView.pageFinishedListenerEh()) {
                mReaderWebView.getPageFinishedListener().onPageFinished(view, url);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            // fire the url click listener, but only do this when webView has
            // loaded (is visible) - have seen some posts containing iframes
            // automatically try to open urls (without being clicked)
            // before the page has loaded
            return view.getVisibility() == View.VISIBLE
                    && mReaderWebView.urlClickListenerEh()
                    && validClickedUrlEh(url)
                    && mReaderWebView.getUrlClickListener().onUrlClick(url);
        }

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            URL imageUrl  = null;
            if (mPrivatePostEh && mBlogSchemeIsHttps && UrlUtils.imageUrlEh(url)) {
                try {
                    imageUrl = new URL(UrlUtils.makeHttps(url));
                } catch (MalformedURLException e) {
                    AppLog.e(AppLog.T.READER, e);
                }
            }
            // Intercept requests for private images and add the WP.com authorization header
            if (imageUrl != null && WPUrlUtils.safeToAddWordPressComAuthToken(imageUrl) &&
                    !TextUtils.isEmpty(mToken)) {
                try {
                    HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                    conn.setRequestProperty("Authorization", "Bearer " + mToken);
                    conn.setReadTimeout(TIMEOUT_MS);
                    conn.setConnectTimeout(TIMEOUT_MS);
                    conn.setRequestProperty("User-Agent", WordPress.getUserAgent());
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    return new WebResourceResponse(conn.getContentType(),
                            conn.getContentEncoding(),
                            conn.getInputStream());
                } catch (IOException e) {
                    AppLog.e(AppLog.T.READER, e);
                }
            }

            return super.shouldInterceptRequest(view, url);
        }
    }

    private static class ReaderWebChromeClient extends WebChromeClient {
        private final ReaderWebView mReaderWebView;
        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;

        ReaderWebChromeClient(ReaderWebView readerWebView) {
            if (readerWebView == null) {
                throw new IllegalArgumentException("ReaderWebChromeClient requires readerWebView");
            }
            mReaderWebView = readerWebView;
        }

        /*
         * request the view that will host the fullscreen video
         */
        private ViewGroup getTargetView() {
            if (mReaderWebView.customViewListenerEh()) {
                return mReaderWebView.getCustomViewListener().onRequestCustomView();
            } else {
                return null;
            }
        }

        /*
         * request the view that should be hidden when showing fullscreen video
         */
        private ViewGroup getContentView() {
            if (mReaderWebView.customViewListenerEh()) {
                return mReaderWebView.getCustomViewListener().onRequestContentView();
            } else {
                return null;
            }
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            AppLog.i(AppLog.T.READER, "onShowCustomView");

            if (mCustomView != null) {
                AppLog.w(AppLog.T.READER, "customView already showing");
                onHideCustomView();
                return;
            }

            // hide the post detail content
            ViewGroup contentView = getContentView();
            if (contentView != null) {
                contentView.setVisibility(View.INVISIBLE);
            }

            // show the full screen view
            ViewGroup targetView = getTargetView();
            if (targetView != null) {
                targetView.addView(view);
                targetView.setVisibility(View.VISIBLE);
            }

            if (mReaderWebView.customViewListenerEh()) {
                mReaderWebView.getCustomViewListener().onCustomViewShown();
            }

            mCustomView = view;
            mCustomViewCallback = callback;
        }

        @Override
        public void onHideCustomView() {
            AppLog.i(AppLog.T.READER, "onHideCustomView");

            if (mCustomView == null) {
                AppLog.w(AppLog.T.READER, "customView does not exist");
                return;
            }

            // hide the target view
            ViewGroup targetView = getTargetView();
            if (targetView != null) {
                targetView.removeView(mCustomView);
                targetView.setVisibility(View.GONE);
            }

            // redisplay the post detail content
            ViewGroup contentView = getContentView();
            if (contentView != null) {
                contentView.setVisibility(View.VISIBLE);
            }

            if (mCustomViewCallback != null) {
                mCustomViewCallback.onCustomViewHidden();
            }
            if (mReaderWebView.customViewListenerEh()) {
                mReaderWebView.getCustomViewListener().onCustomViewHidden();
            }

            mCustomView = null;
            mCustomViewCallback = null;
        }

        boolean customViewShowingEh() {
            return (mCustomView != null);
        }
    }
}
