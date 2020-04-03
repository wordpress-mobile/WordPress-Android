package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

/**
 * RecyclerView with setEmptyView method which displays a view when RecyclerView adapter is empty.
 */
public class EmptyViewRecyclerView extends RecyclerView {
    private View mEmptyView;

    private final AdapterDataObserver mObserver = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            toggleEmptyView();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            toggleEmptyView();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            toggleEmptyView();
        }
    };

    public EmptyViewRecyclerView(Context context) {
        super(context);
    }

    public EmptyViewRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyViewRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(Adapter adapterNew) {
        final RecyclerView.Adapter adapterOld = getAdapter();

        if (adapterOld != null) {
            adapterOld.unregisterAdapterDataObserver(mObserver);
        }

        super.setAdapter(adapterNew);

        if (adapterNew != null) {
            adapterNew.registerAdapterDataObserver(mObserver);
        }

        toggleEmptyView();
    }

    @Override
    public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
        final RecyclerView.Adapter adapterOld = getAdapter();

        if (adapterOld != null) {
            adapterOld.unregisterAdapterDataObserver(mObserver);
        }

        super.swapAdapter(adapter, removeAndRecycleExistingViews);

        final RecyclerView.Adapter adapterNew = getAdapter();

        if (adapterNew != null) {
            adapterNew.registerAdapterDataObserver(mObserver);
        }

        toggleEmptyView();
    }

    public void setEmptyView(View emptyView) {
        mEmptyView = emptyView;
        toggleEmptyView();
    }

    private void toggleEmptyView() {
        if (mEmptyView != null && getAdapter() != null) {
            final boolean empty = getAdapter().getItemCount() == 0;
            mEmptyView.setVisibility(empty ? VISIBLE : GONE);
            this.setVisibility(empty ? GONE : VISIBLE);
        }
    }
}
