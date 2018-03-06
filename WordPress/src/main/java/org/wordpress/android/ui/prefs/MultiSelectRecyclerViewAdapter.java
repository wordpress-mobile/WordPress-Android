package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.List;

/**
 * RecyclerView.Adapter for selecting multiple list items with simple layout (TextView + divider).
 */
public class MultiSelectRecyclerViewAdapter extends RecyclerView.Adapter<MultiSelectRecyclerViewAdapter.ItemHolder> {
    private final List<String> mItems;
    private final SparseBooleanArray mItemsSelected;
    private final int mSelectedColor;
    private final int mUnselectedColor;

    public MultiSelectRecyclerViewAdapter(Context context, List<String> items) {
        this.mSelectedColor = ContextCompat.getColor(context, R.color.white);
        this.mUnselectedColor = ContextCompat.getColor(context, R.color.transparent);
        this.mItems = items;
        this.mItemsSelected = new SparseBooleanArray();
    }

    public class ItemHolder extends RecyclerView.ViewHolder {
        private final LinearLayout mContainer;
        private final TextView mText;

        public ItemHolder(View view) {
            super(view);
            mContainer = (LinearLayout) view.findViewById(R.id.container);
            mText = (TextView) view.findViewById(R.id.text);
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
        holder.mContainer.setBackgroundColor(
                isItemSelected(position)
                        ? mSelectedColor
                        : mUnselectedColor);
    }

    @Override
    public ItemHolder onCreateViewHolder(ViewGroup parent, int type) {
        return new ItemHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.wp_simple_list_item_1, parent, false));
    }

    public String getItem(int position) {
        return mItems.get(position);
    }

    public SparseBooleanArray getItemsSelected() {
        return mItemsSelected;
    }

    private boolean isItemSelected(int position) {
        String item = getItem(position);
        return item != null && mItemsSelected.get(position);
    }

    public void removeItemsSelected() {
        mItemsSelected.clear();
        notifyDataSetChanged();
    }

    public void setItemSelected(int position) {
        if (!mItemsSelected.get(position)) {
            mItemsSelected.put(position, true);
        }

        notifyItemChanged(position);
    }

    public void toggleItemSelected(int position) {
        if (!mItemsSelected.get(position)) {
            mItemsSelected.put(position, true);
        } else {
            mItemsSelected.delete(position);
        }

        notifyItemChanged(position);
    }
}
