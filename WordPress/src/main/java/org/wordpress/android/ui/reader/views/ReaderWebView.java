package org.wordpress.android.ui.reader.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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
import org.wordpress.android.ui.WPWebView;
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
public class ReaderWebView extends WPWebView {
    public interface ReaderWebViewUrlClickListener {
        @SuppressWarnings("SameReturnValue")
        boolean onUrlClick(String url);

        boolean onPageJumpClick(String pageJump);

        boolean onImageUrlClick(String imageUrl, View view, int x, int y);

        boolean onFileDownloadClick(String fileUrl);
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

    /**
     * Timeout in milliseconds for read / connect timeouts
     */
    private static final int TIMEOUT_MS = 30000;

    private ReaderWebChromeClient mReaderChromeClient;
    private ReaderCustomViewListener mCustomViewListener;
    private ReaderWebViewUrlClickListener mUrlClickListener;
    private ReaderWebViewPageFinishedListener mPageFinishedListener;

    private static String mToken;
    private static boolean mIsPrivatePost;
    private static boolean mBlogSchemeIsHttps;

    private boolean mIsDestroyed;
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
        setBackgroundColor(Color.TRANSPARENT);
        if (!isInEditMode()) {
            mToken = mAccountStore.getAccessToken();

            mReaderChromeClient = new ReaderWebChromeClient(this);
            this.setWebChromeClient(mReaderChromeClient);
            this.setWebViewClient(new ReaderWebViewClient(this));
            this.getSettings().setUserAgentString(WordPress.getUserAgent());
            this.getSettings().setMediaPlaybackRequiresUserGesture(false);

            // Enable third-party cookies since they are disabled by default;
            // we need third-party cookies to support authenticated images
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
            this.setDownloadListener(
                    (url, userAgent, contentDisposition, mimetype, contentLength) -> {
                        if (hasUrlClickListener()) {
                            mUrlClickListener.onFileDownloadClick(url);
                        }
                    });
        }
    }

    @Override
    public void destroy() {
        mIsDestroyed = true;
        super.destroy();
    }

    public boolean isDestroyed() {
        return mIsDestroyed;
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

    private boolean hasUrlClickListener() {
        return (mUrlClickListener != null);
    }

    private ReaderWebViewPageFinishedListener getPageFinishedListener() {
        return mPageFinishedListener;
    }

    public void setPageFinishedListener(ReaderWebViewPageFinishedListener listener) {
        mPageFinishedListener = listener;
    }

    private boolean hasPageFinishedListener() {
        return (mPageFinishedListener != null);
    }

    public void setCustomViewListener(ReaderCustomViewListener listener) {
        mCustomViewListener = listener;
    }

    private boolean hasCustomViewListener() {
        return (mCustomViewListener != null);
    }

    private ReaderCustomViewListener getCustomViewListener() {
        return mCustomViewListener;
    }

    public void setIsPrivatePost(boolean isPrivatePost) {
        mIsPrivatePost = isPrivatePost;
    }

    public void setBlogSchemeIsHttps(boolean blogSchemeIsHttps) {
        mBlogSchemeIsHttps = blogSchemeIsHttps;
    }

    private static boolean isValidClickedUrl(String url) {
        // only return true for http(s) urls so we avoid file: and data: clicks
        return (url != null && (url.startsWith("http") || url.startsWith("wordpress:")));
    }

    public boolean isCustomViewShowing() {
        return mReaderChromeClient.isCustomViewShowing();
    }

    public void hideCustomView() {
        if (isCustomViewShowing()) {
            mReaderChromeClient.onHideCustomView();
        }
    }

    /*
     * detect when a link is tapped
     */
    @SuppressLint("ClickableViewAccessibility") // works as is
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && mUrlClickListener != null) {
            HitTestResult hr = getHitTestResult();
            if (hr != null) {
                if (isValidClickedUrl(hr.getExtra())) {
                    if (UrlUtils.isImageUrl(hr.getExtra())) {
                        return mUrlClickListener.onImageUrlClick(
                                hr.getExtra(),
                                this,
                                (int) event.getX(),
                                (int) event.getY());
                    } else {
                        return mUrlClickListener.onUrlClick(hr.getExtra());
                    }
                } else {
                    String pageJump = UrlUtils.getPageJumpOrNull(hr.getExtra());
                    if (null != pageJump) {
                        return mUrlClickListener.onPageJumpClick(pageJump);
                    }
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
            if (mReaderWebView.hasPageFinishedListener()) {
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
                   && mReaderWebView.hasUrlClickListener()
                   && isValidClickedUrl(url)
                   && mReaderWebView.getUrlClickListener().onUrlClick(url);
        }

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            URL imageUrl = null;
            if (mIsPrivatePost && mBlogSchemeIsHttps && UrlUtils.isImageUrl(url)) {
                try {
                    imageUrl = new URL(UrlUtils.makeHttps(url));
                } catch (MalformedURLException e) {
                    AppLog.e(AppLog.T.READER, e);
                }
            }
            // Intercept requests for private images and add the WP.com authorization header
            if (imageUrl != null && WPUrlUtils.safeToAddWordPressComAuthToken(imageUrl)
                && !TextUtils.isEmpty(mToken)) {
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
            if (mReaderWebView.hasCustomViewListener()) {
                return mReaderWebView.getCustomViewListener().onRequestCustomView();
            } else {
                return null;
            }
        }

        /*
         * request the view that should be hidden when showing fullscreen video
         */
        private ViewGroup getContentView() {
            if (mReaderWebView.hasCustomViewListener()) {
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

            if (mReaderWebView.hasCustomViewListener()) {
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
            if (mReaderWebView.hasCustomViewListener()) {
                mReaderWebView.getCustomViewListener().onCustomViewHidden();
            }

            mCustomView = null;
            mCustomViewCallback = null;
        }

        boolean isCustomViewShowing() {
            return (mCustomView != null);
        }
    }
}
