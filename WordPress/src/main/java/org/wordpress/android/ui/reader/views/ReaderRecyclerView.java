package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
