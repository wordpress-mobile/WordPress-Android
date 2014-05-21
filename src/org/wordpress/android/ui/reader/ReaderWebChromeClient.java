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
    }

    private final ViewGroup mTargetView;
    private View mCustomView;
    private CustomViewCallback mCustomViewCallback;
    private ReaderCustomViewListener mCustomViewListener;

    protected ReaderWebChromeClient(ViewGroup TargetView, ReaderCustomViewListener listener) {
        mTargetView = TargetView;
    }

    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        AppLog.i(AppLog.T.READER, "onShowCustomView");

        if (mCustomView != null) {
            onHideCustomView();
            return;
        }

        mCustomViewCallback = callback;
        mCustomView = view;
        mTargetView.addView(mCustomView);
        mTargetView.setVisibility(View.VISIBLE);
        mTargetView.bringToFront();

        if (mCustomViewListener != null) {
            mCustomViewListener.onCustomViewShown();
        }
    }

    @Override
    public void onHideCustomView() {
        AppLog.i(AppLog.T.READER, "onHideCustomView");

        if (mCustomView == null) {
            return;
        }

        mCustomView.setVisibility(View.GONE);
        mTargetView.removeView(mCustomView);
        mTargetView.setVisibility(View.GONE);
        mCustomViewCallback.onCustomViewHidden();

        if (mCustomViewListener != null) {
            mCustomViewListener.onCustomViewHidden();
        }
    }

    protected boolean inCustomView() {
        return (mCustomView != null);
    }
}