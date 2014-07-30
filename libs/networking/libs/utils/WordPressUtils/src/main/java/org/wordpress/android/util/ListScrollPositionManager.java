package org.wordpress.android.util;

import android.view.View;
import android.widget.ListView;

public class ListScrollPositionManager {
    private int mSelectedPosition;
    private int mListViewScrollStateIndex;
    private int mListViewScrollStateOffset;
    private ListView mListView;
    private boolean mSetSelection;

    public ListScrollPositionManager(ListView listView, boolean setSelection) {
        mListView = listView;
        mSetSelection = setSelection;
    }

    public void saveScrollOffset() {
        mListViewScrollStateIndex = mListView.getFirstVisiblePosition();
        View view = mListView.getChildAt(0);
        mListViewScrollStateOffset = 0;
        if (view != null) {
            mListViewScrollStateOffset = view.getTop();
        }
        if (mSetSelection) {
            mSelectedPosition = mListView.getCheckedItemPosition();
        }
    }

    public void restoreScrollOffset() {
        mListView.setSelectionFromTop(mListViewScrollStateIndex, mListViewScrollStateOffset);
        if (mSetSelection) {
            mListView.setItemChecked(mSelectedPosition, true);
        }
    }
}
