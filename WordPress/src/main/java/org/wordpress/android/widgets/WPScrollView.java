package org.wordpress.android.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * ScrollView which reports when user has scrolled up or down, and when scrolling has completed
 */
public class WPScrollView extends ScrollView {
    private float mLastMotionY;
    private int mInitialScrollCheckY;
    private boolean mIsMoving;
    private static final int SCROLL_CHECK_DELAY = 100;

    public interface OnScrollDirectionListener {
        public void onScrollUp();
        public void onScrollDown();
        public void onScrollCompleted();
    }
    private OnScrollDirectionListener mOnScrollDirectionListener;

    public WPScrollView(Context context) {
        super(context);
    }

    public WPScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WPScrollView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnScrollDirectionListener(OnScrollDirectionListener listener) {
        mOnScrollDirectionListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mOnScrollDirectionListener != null) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;

            switch (action) {
                case MotionEvent.ACTION_MOVE :
                    if (mIsMoving) {
                        int yDiff = (int) (event.getY() - mLastMotionY);
                        if (yDiff < 0) {
                            mOnScrollDirectionListener.onScrollDown();
                        } else if (yDiff > 0) {
                            mOnScrollDirectionListener.onScrollUp();
                        }
                        mLastMotionY = event.getY();
                    } else {
                        mIsMoving = true;
                        mLastMotionY = event.getY();
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    if (mIsMoving) {
                        mIsMoving = false;
                        startScrollCheck();
                    }
                    break;

                default :
                    mIsMoving = false;
                    break;
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
                if (mOnScrollDirectionListener != null) {
                    mOnScrollDirectionListener.onScrollCompleted();
                }
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
