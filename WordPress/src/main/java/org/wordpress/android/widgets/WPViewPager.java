package org.wordpress.android.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

/**
 * Custom ViewPager which resolves the "pointer index out of range" bug in the compatibility library
 * https://code.google.com/p/android/issues/detail?id=16836
 * https://code.google.com/p/android/issues/detail?id=18990
 * https://github.com/chrisbanes/PhotoView/issues/31
 */
public class WPViewPager extends ViewPager {
    private boolean mPagingEnabled = true;

    public WPViewPager(Context context) {
        super(context);
    }

    public WPViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mPagingEnabled) {
            try {
                return super.onInterceptTouchEvent(ev);
            } catch (IllegalArgumentException e) {
                AppLog.e(T.UTILS, e);
            }
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility") // we are not detecting tap events, so can ignore this one
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mPagingEnabled) {
            try {
                return super.onTouchEvent(ev);
            } catch (IllegalArgumentException e) {
                AppLog.e(AppLog.T.UTILS, e);
            }
        }
        return false;
    }

    public void setPagingEnabled(boolean pagingEnabled) {
        mPagingEnabled = pagingEnabled;
    }
}
