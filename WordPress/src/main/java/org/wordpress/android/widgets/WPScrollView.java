package org.wordpress.android.widgets;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * ScrollView which reports when user has scrolled up or down, and when scrolling has completed
 */
public class WPScrollView extends ScrollView {

    public interface ScrollDirectionListener {
        void onScrollUp(float distanceY);
        void onScrollDown(float distanceY);
        void onScrollCompleted();
    }

    private ScrollDirectionListener mScrollDirectionListener;
    private final GestureDetectorCompat mDetector;

    private static final int SCROLL_CHECK_DELAY = 250;
    private int mInitialScrollCheckY;

    public WPScrollView(Context context) {
        this(context, null);
    }

    public WPScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WPScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mDetector = new GestureDetectorCompat(context, new ScrollGestureListener());
    }

    public void setScrollDirectionListener(ScrollDirectionListener listener) {
        mScrollDirectionListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScrollDirectionListener != null) {
            mDetector.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    private void startScrollCheck() {
        mInitialScrollCheckY = getScrollY();
        post(mScrollTask);
    }

    private final Runnable mScrollTask = new Runnable() {
        @Override
        public void run() {
            if (mInitialScrollCheckY == getScrollY()) {
                mScrollDirectionListener.onScrollCompleted();
            } else {
                mInitialScrollCheckY = getScrollY();
                postDelayed(mScrollTask, SCROLL_CHECK_DELAY);
            }
        }
    };

    public boolean canScrollUp() {
        return canScrollVertically(-1);
    }
    public boolean canScrollDown() {
        return canScrollVertically(1);
    }

    private class ScrollGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (distanceY < 0) {
                mScrollDirectionListener.onScrollUp(distanceY);
                startScrollCheck();
            } else if (distanceY > 0) {
                mScrollDirectionListener.onScrollDown(distanceY);
                startScrollCheck();
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }
}
