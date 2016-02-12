package org.wordpress.android.util.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
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

    public void saveToPreferences(Context context, String uniqueId) {
        saveScrollOffset();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        Editor editor = settings.edit();
        editor.putInt("scroll-position-manager-index-" + uniqueId, mListViewScrollStateIndex);
        editor.putInt("scroll-position-manager-offset-" + uniqueId, mListViewScrollStateOffset);
        editor.putInt("scroll-position-manager-selected-position-" + uniqueId, mSelectedPosition);
        editor.apply();
    }

    public void restoreFromPreferences(Context context, String uniqueId) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        mListViewScrollStateIndex = settings.getInt("scroll-position-manager-index-" + uniqueId, 0);
        mListViewScrollStateOffset = settings.getInt("scroll-position-manager-offset-" + uniqueId, 0);
        mSelectedPosition = settings.getInt("scroll-position-manager-selected-position-" + uniqueId, 0);
        restoreScrollOffset();
    }
}
