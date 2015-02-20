package org.wordpress.android.ui.reader.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/*
 * WebView descendant used by ReaderPostDetailFragment - handles
 * displaying fullscreen video and detecting url/image clicks
 */
public class ReaderWebView extends WebView {

    public interface ReaderWebViewUrlClickListener {
        public boolean onUrlClick(String url);
        public boolean onImageUrlClick(String imageUrl, View view, int x, int y);
    }

    public interface ReaderCustomViewListener {
        public void onCustomViewShown();
        public void onCustomViewHidden();
        public ViewGroup onRequestCustomView();
        public ViewGroup onRequestContentView();
    }

    public interface ReaderWebViewPageFinishedListener {
        public void onPageFinished(WebView view, String url);
    }

    private ReaderWebChromeClient mReaderChromeClient;
    private ReaderCustomViewListener mCustomViewListener;
    private ReaderWebViewUrlClickListener mUrlClickListener;
    private ReaderWebViewPageFinishedListener mPageFinishedListener;

    private boolean mIsDestroyed;
    private static String mToken;

    public ReaderWebView(Context context) {
        super(context);
        init(context);
    }



    @Override
    public void destroy() {
        mIsDestroyed = true;
        super.destroy();
    }

    public boolean isDestroyed() {
        return mIsDestroyed;
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
        if (!isInEditMode()) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            mToken = settings.getString(WordPress.ACCESS_TOKEN_PREFERENCE, "");

            mReaderChromeClient = new ReaderWebChromeClient(this);
            this.setWebChromeClient(mReaderChromeClient);
            this.setWebViewClient(new ReaderWebViewClient(this));
            this.getSettings().setUserAgentString(WordPress.getUserAgent());
            // Lollipop disables third-party cookies by default, but we need them in order
            // to support authenticated images
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);
            }
        }
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

    private static boolean isValidClickedUrl(String url) {
        // only return true for http(s) urls so we avoid file: and data: clicks
        return (url != null && url.startsWith("http"));
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
     * detect when an image is tapped
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP && mUrlClickListener != null) {
            HitTestResult hr = getHitTestResult();
            if (hr != null && (hr.getType() == HitTestResult.IMAGE_TYPE
                            || hr.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                String imageUrl = hr.getExtra();
                if (isValidClickedUrl(imageUrl) ) {
                    return mUrlClickListener.onImageUrlClick(
                            imageUrl,
                            this,
                            (int) event.getX(),
                            (int) event.getY());
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
            if (view.getVisibility() == View.VISIBLE
                    && mReaderWebView.hasUrlClickListener()
                    && isValidClickedUrl(url)) {
                return mReaderWebView.getUrlClickListener().onUrlClick(url);
            } else {
                return false;
            }
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            // Intercept requests and add the WP.com authorization header
            if (!TextUtils.isEmpty(mToken) && isImageUrl(url)) {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                httpGet.setHeader("Authorization", "Bearer " + mToken);
                try {
                    HttpResponse httpResponse = client.execute(httpGet);
                    InputStream responseInputStream = httpResponse.getEntity().getContent();
                    return new WebResourceResponse(httpResponse.getEntity().getContentType().toString(), "UTF-8", responseInputStream);
                } catch (IOException e) {
                    AppLog.e(AppLog.T.READER, "Invalid reader detail request: " + e.getMessage());
                }
            }

            return super.shouldInterceptRequest(view, url);
        }
    }

    private static boolean isImageUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;

        String lowerCaseUrl = url.toLowerCase();

        return lowerCaseUrl.endsWith("jpg") || url.endsWith("jpeg") || url.endsWith("gif") ||
                url.endsWith("png");
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

            mReaderWebView.onPause();
        }

        boolean isCustomViewShowing() {
            return (mCustomView != null);
        }
    }
}
