package org.wordpress.android.util.helpers;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;


public class RecyclerViewScrollPositionManager implements RecyclerViewScrollPositionSaver {
    private static final String RV_POSITION = "rv_position";
    private static final String RV_OFFSET = "rv_offset";
    private int mRVPosition = 0;
    private int mRVOffset = 0;

    @Override
    public void onSaveInstanceState(Bundle outState, RecyclerView recyclerView) {
        outState.putInt(RV_POSITION,
                ((LinearLayoutManager)recyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        View firstItemView = recyclerView.getChildAt(0);
        int offset = (firstItemView == null) ? 0 : (firstItemView.getTop() - recyclerView.getPaddingTop());
        outState.putInt(RV_OFFSET, offset);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mRVPosition = savedInstanceState.getInt(RV_POSITION);
        mRVOffset = savedInstanceState.getInt(RV_OFFSET);
    }

    @Override
    public void restoreScrollOffset(RecyclerView recyclerView) {
        ((LinearLayoutManager)recyclerView.getLayoutManager())
                .scrollToPositionWithOffset(mRVPosition, mRVOffset);
    }
}
