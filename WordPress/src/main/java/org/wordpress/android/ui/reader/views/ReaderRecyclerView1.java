package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import org.wordpress.android.ui.FilteredRecyclerView;

public class ReaderRecyclerView1 extends FilteredRecyclerView {

    public ReaderRecyclerView1(Context context) {
        super(context);
    }

    public ReaderRecyclerView1(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ReaderRecyclerView1(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
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

    /*
     * returns true if the first post is still visible in the RecyclerView - will return
     * false if the first post is scrolled out of view, or if the list is empty
     */
    public boolean isFirstPostVisible() {
        if (mRecyclerView == null
                || mRecyclerView.getLayoutManager() == null) {
            return false;
        }

        View child = mRecyclerView.getLayoutManager().getChildAt(0);
        return (child != null && mRecyclerView.getLayoutManager().getPosition(child) == 0);
    }


}
