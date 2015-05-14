package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class ReaderRecyclerView extends RecyclerView {
    private float mLastMotionY;
    private int mInitialScrollCheckY;
    private boolean mIsMoving;
    private static final int SCROLL_CHECK_DELAY = 100;

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

    // http://stackoverflow.com/a/25227797/1673548
    public boolean canScrollUp() {
        return super.canScrollVertically(-1) || (getChildAt(0) != null && getChildAt(0).getTop() < 0);
    }

    /*
     * returns the vertical scroll position
     */
    public int getVerticalScrollOffset() {
        return super.computeVerticalScrollOffset();
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
                        0,                  // top
                        mSpacingHorizontal, // right
                        mSpacingVertical);  // bottom
        }
    }
}
