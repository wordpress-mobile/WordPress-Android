package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

public class ReaderPostRecyclerView extends RecyclerView {

    public ReaderPostRecyclerView(Context context) {
        super(context);
        initialize();
    }

    public ReaderPostRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public ReaderPostRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    private void initialize() {
        if (!isInEditMode()) {
            ItemAnimator animator = new DefaultItemAnimator();
            animator.setSupportsChangeAnimations(true);
            setItemAnimator(animator);
        }
    }
}
