package org.wordpress.android.ui.reader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;

/*
 * WebView descendant used by ReaderPostDetailFragment - handles
 * displaying fullscreen video and detecting url/image clicks
 */
class ReaderWebView extends WebView {

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

    private ReaderWebChromeClient mReaderChromeClient;
    private ReaderCustomViewListener mCustomViewListener;
    private ReaderWebViewUrlClickListener mUrlClickListener;

    public ReaderWebView(Context context) {
        super(context);
        init();
    }

    public ReaderWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReaderWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (!isInEditMode()) {
            mReaderChromeClient = new ReaderWebChromeClient(this);
            this.setWebChromeClient(mReaderChromeClient);

            this.setWebViewClient(new ReaderWebViewClient(this));
            this.setOnTouchListener(mOnTouchListener);
            this.getSettings().setUserAgentString(WordPress.getUserAgent());
        }
    }

    private ReaderWebViewUrlClickListener getUrlClickListener() {
        return mUrlClickListener;
    }

    void setUrlClickListener(ReaderWebViewUrlClickListener listener) {
        mUrlClickListener = listener;
    }

    private boolean hasUrlClickListener() {
        return (mUrlClickListener != null);
    }

    void setCustomViewListener(ReaderCustomViewListener listener) {
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

    boolean isCustomViewShowing() {
        return mReaderChromeClient.isCustomViewShowing();
    }

    void hideCustomView() {
        if (isCustomViewShowing()) {
            mReaderChromeClient.onHideCustomView();
        }
    }
    /*
     * detect when an image is tapped
     */
    private final OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    HitTestResult hr = ((WebView) view).getHitTestResult();
                    if (hr != null && (hr.getType() == HitTestResult.IMAGE_TYPE || hr.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                        String imageUrl = hr.getExtra();
                        if (isValidClickedUrl(imageUrl) && mUrlClickListener != null) {
                            return mUrlClickListener.onImageUrlClick(imageUrl, view, (int) event.getX(), (int) event.getY());
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                default:
                    return false;
            }
        }
    };

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
            // show the webView now that it has loaded (ReaderPostDetailFragment may have hidden it)
            if (view.getVisibility() != View.VISIBLE) {
                view.setVisibility(View.VISIBLE);
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
