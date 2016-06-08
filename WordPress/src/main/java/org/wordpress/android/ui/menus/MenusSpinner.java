package org.wordpress.android.ui.menus;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.NameInterface;

import java.util.List;

public class MenusSpinner extends Spinner {
    private String mTitle;
    private int mIconResource;
    private MenusSpinnerAdapter mAdapter;

    public MenusSpinner(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.MenusSpinner);
        for (int i = 0; i < array.getIndexCount(); ++i) {
            int index = array.getIndex(i);
            if (index == R.styleable.MenusSpinner_spinnerTitle) {
                mTitle = array.getString(index);
            } else if (index == R.styleable.MenusSpinner_spinnerIcon) {
                mIconResource = array.getResourceId(index, -1);
            }
        }
        array.recycle();
    }

    public <T extends NameInterface> void setItems(List<T> items, int displayBaseCount) {
        if (items != null && items.size() > 0) {
            mAdapter = new MenusSpinnerAdapter<>(items, displayBaseCount);
            setAdapter(mAdapter);
        } else {
            setAdapter(null);
        }
    }

    public <T extends NameInterface> List<T> getItems(){
        if (mAdapter != null)
            return mAdapter.getItems();
        return null;
    }

    private class MenusSpinnerAdapter<T extends NameInterface> implements SpinnerAdapter {
        private static final int EMPTY_VIEW_TYPE = 0;
        private static final int NORMAL_VIEW_TYPE = 1;

        private final List<T> mItems;
        private int mDisplayBaseCount; //count starts from this value

        public MenusSpinnerAdapter(List<T> items, int displayBaseCount) {
            super();
            mItems = items;
            mDisplayBaseCount = displayBaseCount;
        }

        public boolean hasItems() {
            return mItems != null && mItems.size() > 0;
        }

        public boolean isValidPosition(int position) {
            return mItems != null && position >= 0 && position < mItems.size();
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.menu_spinner_item, null);
            }

            if (!isValidPosition(position)) return convertView;

            TextView nameView = (TextView) convertView.findViewById(R.id.menu_spinner_item_name);
            nameView.setText(mItems.get(position).getName());

            ImageView iconView = (ImageView) convertView.findViewById(R.id.menu_spinner_item_icon);
            iconView.setVisibility(position == getSelectedItemPosition() ? VISIBLE : INVISIBLE);

            return convertView;
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public int getCount() {
            return hasItems() ? mItems.size() : 1;
        }

        @Override
        public Object getItem(int position) {
            if (!isValidPosition(position)) return null;
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.menu_spinner, null);
            }

            if (mIconResource > 0) {
                ImageView iconView = (ImageView) convertView.findViewById(R.id.menu_spinner_icon);
                iconView.setImageResource(mIconResource);
            }

            TextView titleView = (TextView) convertView.findViewById(R.id.menu_spinner_title);
            titleView.setText(String.format(mTitle, getCount() + mDisplayBaseCount));

            if (hasItems() && position < mItems.size()) {
                TextView nameView = (TextView) convertView.findViewById(R.id.menu_spinner_name);
                nameView.setText(mItems.get(position).getName());
            }

            return convertView;
        }

        @Override
        public int getItemViewType(int position) {
            return hasItems() ? NORMAL_VIEW_TYPE : EMPTY_VIEW_TYPE;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return !hasItems();
        }

        public List<T> getItems(){
            return mItems;
        }
    }
}
