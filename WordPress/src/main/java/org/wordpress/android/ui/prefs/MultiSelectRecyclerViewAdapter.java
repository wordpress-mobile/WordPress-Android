package org.wordpress.android.ui.prefs;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;

import java.util.List;

/**
 * RecyclerView.Adapter for selecting multiple list items with simple layout (TextView + divider).
 */
public class MultiSelectRecyclerViewAdapter extends RecyclerView.Adapter<MultiSelectRecyclerViewAdapter.ItemHolder> {
    private final List<String> mItems;
    private final SparseBooleanArray mItemsSelected;

    MultiSelectRecyclerViewAdapter(List<String> items) {
        this.mItems = items;
        this.mItemsSelected = new SparseBooleanArray();
    }

    class ItemHolder extends RecyclerView.ViewHolder {
        private final View mContainer;
        private final TextView mText;

        ItemHolder(View view) {
            super(view);
            mContainer = view.findViewById(R.id.container);
            mText = view.findViewById(R.id.text);
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void onBindViewHolder(final ItemHolder holder, int position) {
        String item = getItem(holder.getAdapterPosition());
        holder.mText.setText(item);
        holder.mContainer.setSelected(isItemSelected(position));
    }

    @NotNull @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int type) {
        return new ItemHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.wp_simple_list_item_1, parent, false));
    }

    public String getItem(int position) {
        return mItems.get(position);
    }

    SparseBooleanArray getItemsSelected() {
        return mItemsSelected;
    }

    private boolean isItemSelected(int position) {
        String item = getItem(position);
        return item != null && mItemsSelected.get(position);
    }

    void removeItemsSelected() {
        mItemsSelected.clear();
        notifyDataSetChanged();
    }

    void setItemSelected(int position) {
        if (!mItemsSelected.get(position)) {
            mItemsSelected.put(position, true);
        }

        notifyItemChanged(position);
    }

    void toggleItemSelected(int position) {
        if (!mItemsSelected.get(position)) {
            mItemsSelected.put(position, true);
        } else {
            mItemsSelected.delete(position);
        }

        notifyItemChanged(position);
    }
}
