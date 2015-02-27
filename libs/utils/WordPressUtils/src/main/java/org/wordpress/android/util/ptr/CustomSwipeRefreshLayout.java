package org.wordpress.android.util.ptr;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class CustomSwipeRefreshLayout extends SwipeRefreshLayout {
    public CustomSwipeRefreshLayout(Context context) {
        super(context);
    }

    public CustomSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try{
            return super.onTouchEvent(event);
        } catch(IllegalArgumentException e) {
            AppLog.e(T.UTILS, e);
            return true;
        }
    }
}
