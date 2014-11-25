package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;

public class ReaderPostRecyclerView extends RecyclerView {
    private static final int GRID_SPAN_COUNT = 2;

    public ReaderPostRecyclerView(Context context) {
        super(context);
        initialize(context);
    }

    public ReaderPostRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }

    public ReaderPostRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }

    /*
     * this works around a bug in the default canScrollVertically() that causes it
     * to return false prematurely when checking whether the view can scroll up
     * http://stackoverflow.com/a/25227797/1673548
     */
    @Override
    public boolean canScrollVertically(int direction) {
        if (direction < 0) {
            boolean original = super.canScrollVertically(direction);
            return !original && getChildAt(0) != null && getChildAt(0).getTop() < 0 || original;
        }
        return super.canScrollVertically(direction);
    }

    private void initialize(Context context) {
        if (!isInEditMode()) {
            ItemAnimator animator = new DefaultItemAnimator();
            animator.setSupportsChangeAnimations(true);
            setItemAnimator(animator);

            boolean isGridView;
            int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
            switch(screenSize) {
                case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                    // always use a grid when running a large tablet
                    isGridView = true;
                    break;
                case Configuration.SCREENLAYOUT_SIZE_LARGE:
                    // use a grid on other tablets when in landscape
                    isGridView = DisplayUtils.isLandscape(context);
                    break;
                case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                    // use a grid on normal displays when in landscape if they're xhdpi+
                    float density = context.getResources().getDisplayMetrics().density;
                    isGridView = DisplayUtils.isLandscape(context) && (density >= 2.0f);
                    break;
                default:
                    // skip grid and use a standard list for all other displays
                    isGridView = false;
                    break;
            }

            if (isGridView) {
                setLayoutManager(new StaggeredGridLayoutManager(GRID_SPAN_COUNT, StaggeredGridLayoutManager.VERTICAL));
            } else {
                setLayoutManager(new LinearLayoutManager(context));
            }

            addItemDecoration(new DividerItemDecoration(context, isGridView));
        }
    }

    private class DividerItemDecoration extends RecyclerView.ItemDecoration {
        private final int mSpacing;
        private final int mHalfSpacing;
        private final boolean mIsGridView;

        DividerItemDecoration(Context context, boolean isGridView) {
            super();
            mIsGridView = isGridView;
            mSpacing = context.getResources().getDimensionPixelSize(R.dimen.reader_card_spacing);
            mHalfSpacing = (mSpacing / 2);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (mIsGridView) {
                outRect.set(mHalfSpacing, mHalfSpacing, mHalfSpacing, mHalfSpacing);
            } else {
                outRect.set(mSpacing, mSpacing, mSpacing, 0);
            }
        }
    }

}
