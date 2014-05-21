package org.wordpress.android.ui.reader;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.reader.ReaderWebChromeClient.ReaderCustomViewListener;

public class ReaderWebView extends WebView
        implements ReaderCustomViewListener {

    public interface ReaderWebViewImageClickListener {
        public boolean onImageClick(String imageUrl, View view, int x, int y);
    }

    private ReaderCustomViewListener mCustomViewListener;
    private ReaderWebViewImageClickListener mImageClickListener;

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
            this.setWebChromeClient(mReaderChromeClient);
            this.setOnTouchListener(mOnTouchListener);
            this.getSettings().setUserAgentString(WordPress.getUserAgent());
        }
    }

    void setCustomViewListener(ReaderCustomViewListener listener) {
        mCustomViewListener = listener;
    }

    void setImageClickListener(ReaderWebViewImageClickListener listener) {
        mImageClickListener = listener;
    }

    protected void hideCustomView() {
        mReaderChromeClient.onHideCustomView();
    }

    @Override
    public void onCustomViewShown() {
        if (mCustomViewListener != null) {
            mCustomViewListener.onCustomViewShown();
        }
    }

    @Override
    public void onCustomViewHidden() {
        if (mCustomViewListener != null) {
            mCustomViewListener.onCustomViewHidden();
        }
    }

    @Override
    public ViewGroup onRequestCustomView() {
        if (mCustomViewListener != null) {
            return mCustomViewListener.onRequestCustomView();
        } else {
            return null;
        }
    }

    private final OnTouchListener mOnTouchListener = new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    // detect when an image is tapped and shows it in the photo viewer activity
                    HitTestResult hr = ((WebView) view).getHitTestResult();
                    if (hr != null && (hr.getType() == HitTestResult.IMAGE_TYPE || hr.getType() == HitTestResult.SRC_IMAGE_ANCHOR_TYPE)) {
                        String imageUrl = hr.getExtra();
                        if (imageUrl != null && imageUrl.startsWith("http") && mImageClickListener != null) {
                            return mImageClickListener.onImageClick(imageUrl, view, (int) event.getX(), (int) event.getY());
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

    private final ReaderWebChromeClient mReaderChromeClient =
            new ReaderWebChromeClient(new ReaderCustomViewListener() {
                @Override
                public ViewGroup onRequestCustomView() {
                    return (mCustomViewListener != null ? mCustomViewListener.onRequestCustomView() : null);
                }

                @Override
                public void onCustomViewShown() {
                    if (mCustomViewListener != null) {
                        mCustomViewListener.onCustomViewShown();
                    }
                }

                @Override
                public void onCustomViewHidden() {
                    if (mCustomViewListener != null) {
                        mCustomViewListener.onCustomViewHidden();
                    }
                }
            });
}
