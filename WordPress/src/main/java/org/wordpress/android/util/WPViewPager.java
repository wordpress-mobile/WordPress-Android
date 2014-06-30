package org.wordpress.android.util;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class WPViewPager extends ViewPager {
    private boolean enabled;
    private int mPreviousPage;

    public WPViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = false;
        mPreviousPage = 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onInterceptTouchEvent(event);
        }

        return false;
    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void setCurrentItem(int currentItem) {
        mPreviousPage = getCurrentItem();
        super.setCurrentItem(currentItem);
    }

    public int getPreviousPage() {
        return mPreviousPage;
    }
}
