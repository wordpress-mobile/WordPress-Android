package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.ListView;

/**
 * ListView which reports scroll changes
 */
public class WPListView extends ListView {

    private ViewTreeObserver.OnScrollChangedListener mScrollChangedListener;

    public WPListView(Context context) {
        super(context);
    }

    public WPListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WPListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnScrollChangedListener(ViewTreeObserver.OnScrollChangedListener listener) {
        mScrollChangedListener = listener;
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        // note that onScrollChanged occurs quite often, so make sure to optimize the listener
        // to avoid unnecessary layout/calculation
        if (mScrollChangedListener != null) {
            mScrollChangedListener.onScrollChanged();
        }
    }
}
