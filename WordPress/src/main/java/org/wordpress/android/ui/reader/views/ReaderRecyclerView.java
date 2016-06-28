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
}
