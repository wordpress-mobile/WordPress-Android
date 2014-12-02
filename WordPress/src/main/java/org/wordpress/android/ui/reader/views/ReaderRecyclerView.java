package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class ReaderRecyclerView extends RecyclerView {

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
            ItemAnimator animator = new DefaultItemAnimator();
            animator.setSupportsChangeAnimations(true);
            setItemAnimator(animator);
            setLayoutManager(new LinearLayoutManager(context));
        }
    }

    /**
     * dividers for reader post cards
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
