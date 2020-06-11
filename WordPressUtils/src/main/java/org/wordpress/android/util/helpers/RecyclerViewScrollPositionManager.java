package org.wordpress.android.util.helpers;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class RecyclerViewScrollPositionManager {
    private static final String RV_POSITION = "rv_position";
    private static final String RV_OFFSET = "rv_offset";
    private int mRVPosition = 0;
    private int mRVOffset = 0;

    public void onSaveInstanceState(Bundle outState, RecyclerView recyclerView) {
        // make sure the layout manager is assigned to the RecyclerView
        // also take into account this needs to be a LinearLayoutManager, otherwise ClassCastException occurs
        outState.putInt(RV_POSITION,
                        ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition());
        View firstItemView = recyclerView.getChildAt(0);
        int offset = (firstItemView == null) ? 0 : (firstItemView.getTop() - recyclerView.getPaddingTop());
        outState.putInt(RV_OFFSET, offset);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        mRVPosition = savedInstanceState.getInt(RV_POSITION);
        mRVOffset = savedInstanceState.getInt(RV_OFFSET);
    }

    public void restoreScrollOffset(RecyclerView recyclerView) {
        if (mRVPosition > 0 || mRVOffset > 0) {
            ((LinearLayoutManager) recyclerView.getLayoutManager())
                    .scrollToPositionWithOffset(mRVPosition, mRVOffset);
        }
        mRVPosition = 0;
        mRVOffset = 0;
    }
}
