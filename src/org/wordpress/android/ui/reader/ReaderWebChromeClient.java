package org.wordpress.android.ui.reader;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;

import org.wordpress.android.util.AppLog;

/*
 * WebChrome client used by ReaderPostDetailFragment to support playing full-screen video
 * http://stackoverflow.com/a/16199649/1673548
 */
class ReaderWebChromeClient extends WebChromeClient {

    public interface ReaderCustomViewListener {
        public void onCustomViewShown();
        public void onCustomViewHidden();
        public ViewGroup onRequestCustomView();
    }

    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    private ReaderCustomViewListener mCustomViewListener;

    protected ReaderWebChromeClient(ReaderCustomViewListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("ReaderWebChromeClient requires a listener");
        }
        mCustomViewListener = listener;
    }

    /*
     * request the view that will host the fullscreen video
     */
    private ViewGroup getTargetView() {
        return mCustomViewListener.onRequestCustomView();
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

        mCustomViewListener.onCustomViewShown();
    }

    @Override
    public void onHideCustomView() {
        AppLog.i(AppLog.T.READER, "onHideCustomView");

        if (mCustomView == null) {
            return;
        }

        ViewGroup targetView = getTargetView();
        if (targetView == null) {
            return;
        }

        mCustomView.setVisibility(View.GONE);
        targetView.removeView(mCustomView);
        targetView.setVisibility(View.GONE);
        mCustomViewCallback.onCustomViewHidden();

        mCustomViewListener.onCustomViewHidden();
    }

    protected boolean inCustomView() {
        return (mCustomView != null);
    }
}