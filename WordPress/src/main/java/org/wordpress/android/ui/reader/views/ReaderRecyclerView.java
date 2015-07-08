package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

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
}
