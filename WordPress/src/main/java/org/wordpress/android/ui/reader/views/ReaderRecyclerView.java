package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import org.wordpress.android.R;

/*
 * default recycler view used for reader cards
 */
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
            addItemDecoration(new DividerItemDecoration(context));
        }
    }

    private class DividerItemDecoration extends RecyclerView.ItemDecoration {
        private final int mSpacingHorizontal;
        private final int mSpacingVertical;

        DividerItemDecoration(Context context) {
            super();
            mSpacingHorizontal = context.getResources().getDimensionPixelSize(R.dimen.reader_card_spacing);
            mSpacingVertical = context.getResources().getDimensionPixelSize(R.dimen.reader_card_spacing_vertical);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.set(mSpacingHorizontal, // left
                        mSpacingVertical,   // top
                        mSpacingHorizontal, // right
                        0);                 // bottom
        }
    }

}
