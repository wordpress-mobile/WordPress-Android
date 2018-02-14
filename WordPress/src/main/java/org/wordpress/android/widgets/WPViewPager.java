package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.duolingo.open.rtlviewpager.RtlViewPager;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

/**
 * Custom ViewPager which resolves the "pointer index out of range" bug in the compatibility library
 * https://code.google.com/p/android/issues/detail?id=16836
 * https://code.google.com/p/android/issues/detail?id=18990
 * https://github.com/chrisbanes/PhotoView/issues/31
 *
 * The class extends from RtlViewPager - fixes the scroll/swipe direction in the RtL mode.
 */
public class WPViewPager extends RtlViewPager {
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
