package org.wordpress.android.util.helpers;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;

public interface RecyclerViewScrollPositionSaver {
    void onSaveInstanceState(Bundle outState, RecyclerView recyclerView);
    void onRestoreInstanceState(Bundle savedInstanceState);
    void restoreScrollOffset(RecyclerView recyclerView);
}
