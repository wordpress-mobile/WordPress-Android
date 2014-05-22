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
 * WebView descendant used by ReaderPostDetailFragment - uses a custom WebChromeClient to handle
 * displaying fullscreen video and detecting image clicks
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

    void setUrlClickListener(ReaderWebViewUrlClickListener listener) {
        mUrlClickListener = listener;
    }

    private ReaderWebViewUrlClickListener getUrlClickListener() {
        return mUrlClickListener;
    }

    private boolean hasUrlClickListener() {
        return (mUrlClickListener != null);
    }

    void setCustomViewListener(ReaderCustomViewListener listener) {
        mCustomViewListener = listener;
    }

    protected void hideCustomView() {
        if (mReaderChromeClient.isCustomViewShowing()) {
            mReaderChromeClient.onHideCustomView();
        }
    }

    private boolean hasCustomViewListener() {
        return (mCustomViewListener != null);
    }

    private ReaderCustomViewListener getCustomViewListener() {
        return mCustomViewListener;
    }

    private static boolean isValidClickedUrl(String url) {
        return (url != null || url.startsWith("http"));
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

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            AppLog.i(AppLog.T.READER, "onShowCustomView");

            if (mCustomView != null) {
                onHideCustomView();
                return;
            }

            ViewGroup targetView = getTargetView();
            if (targetView == null) {
                return;
            }

            mCustomViewCallback = callback;
            mCustomView = view;

            targetView.addView(mCustomView);
            targetView.setVisibility(View.VISIBLE);
            targetView.bringToFront();

            if (mReaderWebView.hasCustomViewListener()) {
                mReaderWebView.getCustomViewListener().onCustomViewShown();
            }
        }

        @Override
        public void onHideCustomView() {
            AppLog.i(AppLog.T.READER, "onHideCustomView");

            ViewGroup targetView = getTargetView();
            if (mCustomView == null || targetView == null) {
                return;
            }

            mCustomView.setVisibility(View.GONE);
            targetView.removeView(mCustomView);
            targetView.setVisibility(View.GONE);
            mCustomViewCallback.onCustomViewHidden();

            if (mReaderWebView.hasCustomViewListener()) {
                mReaderWebView.getCustomViewListener().onCustomViewHidden();
            }
        }

        boolean isCustomViewShowing() {
            return (mCustomView != null);
        }
    }
}
