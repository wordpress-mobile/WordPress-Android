package org.wordpress.android.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.ListView;

/**
 * ListView which reports scroll changes and offers a few additional properties
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

    /*
     * returns true if the listView can scroll up/down vertically - always returns true prior to ICS
     * because canScrollVertically() requires API 14
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public boolean canScrollUp() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            return true;
        return canScrollVertically(-1);
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public boolean canScrollDown() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            return true;
        return canScrollVertically(1);
    }
}
