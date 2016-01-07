package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
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
    private int mInitialScrollCheckY;
    private static final int SCROLL_CHECK_DELAY = 250;

    public WPScrollView(Context context) {
        this(context, null);
    }

    public WPScrollView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WPScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setScrollDirectionListener(ScrollDirectionListener listener) {
        mScrollDirectionListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScrollDirectionListener != null
                && event.getActionMasked() == MotionEvent.ACTION_MOVE
                && event.getHistorySize() > 0) {
            float initialY = event.getHistoricalY(event.getHistorySize() - 1);
            float distanceY = initialY - event.getY();
            if (distanceY < 0) {
                mScrollDirectionListener.onScrollUp(distanceY);
                startScrollCheck();
            } else if (distanceY > 0) {
                mScrollDirectionListener.onScrollDown(distanceY);
                startScrollCheck();
            }
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
}
