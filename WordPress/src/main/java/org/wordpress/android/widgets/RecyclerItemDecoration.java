package org.wordpress.android.widgets;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * simple implementation of RecyclerView dividers
 */
public class RecyclerItemDecoration extends RecyclerView.ItemDecoration {
    private final int mSpacingHorizontal;
    private final int mSpacingVertical;
    private final boolean mSkipFirstItem;

    public RecyclerItemDecoration(int spacingHorizontal, int spacingVertical) {
        super();
        mSpacingHorizontal = spacingHorizontal;
        mSpacingVertical = spacingVertical;
        mSkipFirstItem = false;
    }

    public RecyclerItemDecoration(int spacingHorizontal, int spacingVertical, boolean skipFirstItem) {
        super();
        mSpacingHorizontal = spacingHorizontal;
        mSpacingVertical = spacingVertical;
        mSkipFirstItem = skipFirstItem;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        if (mSkipFirstItem && parent.getChildAdapterPosition(view) == 0) {
            return;
        }
        outRect.set(mSpacingHorizontal, // left
                0,                      // top
                mSpacingHorizontal,     // right
                mSpacingVertical);      // bottom
    }
}