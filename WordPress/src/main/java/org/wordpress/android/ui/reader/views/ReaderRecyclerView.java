package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import org.wordpress.android.widgets.ScrollDirectionListener;

public class ReaderRecyclerView extends RecyclerView {
    private float mLastMotionY;
    private int mInitialScrollCheckY;
    private boolean mIsMoving;
    private static final int SCROLL_CHECK_DELAY = 100;
    private ScrollDirectionListener mScrollDirectionListener;

    public ReaderRecyclerView(Context context) {
        super(context);
        initialize(context);
    }

    public ReaderRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ReaderRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    private void initialize(Context context) {
        if (!isInEditMode()) {
            setLayoutManager(new LinearLayoutManager(context));
        }
    }

    public void setScrollDirectionListener(ScrollDirectionListener listener) {
        mScrollDirectionListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScrollDirectionListener != null) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;

            switch (action) {
                case MotionEvent.ACTION_MOVE :
                    if (mIsMoving) {
                        int yDiff = (int) (event.getY() - mLastMotionY);
                        if (yDiff < 0) {
                            mScrollDirectionListener.onScrollDown();
                        } else if (yDiff > 0) {
                            mScrollDirectionListener.onScrollUp();
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
                if (mScrollDirectionListener != null) {
                    mScrollDirectionListener.onScrollCompleted();
                }
            } else {
                mInitialScrollCheckY = getScrollY();
                postDelayed(mScrollTask, SCROLL_CHECK_DELAY);
            }
        }
    };

    // http://stackoverflow.com/a/25227797/1673548
    public boolean canScrollUp() {
        return super.canScrollVertically(-1) || (getChildAt(0) != null && getChildAt(0).getTop() < 0);
    }

    /**
     * dividers for reader cards
     */
    public static class ReaderItemDecoration extends RecyclerView.ItemDecoration {
        private final int mSpacingHorizontal;
        private final int mSpacingVertical;

        public ReaderItemDecoration(int spacingHorizontal, int spacingVertical) {
            super();
            mSpacingHorizontal = spacingHorizontal;
            mSpacingVertical = spacingVertical;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(mSpacingHorizontal, // left
                        mSpacingVertical,   // top
                        mSpacingHorizontal, // right
                        0);                 // bottom
        }
    }
}
