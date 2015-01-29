package org.wordpress.android.widgets;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

import org.wordpress.android.util.AppLog;

/*
 * custom ViewPager which resolves the "pointer index out of range" bug in the compatibility library
 * https://code.google.com/p/android/issues/detail?id=16836
 * https://code.google.com/p/android/issues/detail?id=18990
 * https://github.com/chrisbanes/PhotoView/issues/31
 *
 */

public class WPMainViewPager extends ViewPager {

    public WPMainViewPager(Context context) {
        super(context);
    }

    public WPMainViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.READER, e);
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        try {
            return super.onTouchEvent(ev);
        } catch (IllegalArgumentException e) {
            AppLog.e(AppLog.T.READER, e);
            return false;
        }
    }
}
