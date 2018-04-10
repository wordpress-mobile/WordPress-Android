package org.wordpress.android.ui.stats;

import android.content.Context;
import android.util.AttributeSet;

import org.wordpress.android.widgets.WPNestedScrollView;

public class NestedScrollViewExt extends WPNestedScrollView {
    private ScrollViewListener mScrollViewListener = null;

    public NestedScrollViewExt(Context context) {
        super(context);
    }

    public NestedScrollViewExt(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public NestedScrollViewExt(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setScrollViewListener(ScrollViewListener scrollViewListener) {
        this.mScrollViewListener = scrollViewListener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mScrollViewListener != null) {
            mScrollViewListener.onScrollChanged(this, l, t, oldl, oldt);
        }
    }

    public interface ScrollViewListener {
        void onScrollChanged(NestedScrollViewExt scrollView,
                             int x, int y, int oldx, int oldy);
    }
}

