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
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;

import java.util.List;

public class MenusSpinner extends Spinner {
    private String mTitle;
    private int mIconResource;

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

    public void setItems(List<?> items) {
        if (items != null && items.size() > 0) {
            if (items.get(0) instanceof MenuLocationModel) {
                setAdapter(new MenuLocationsSpinnerAdapter((List<MenuLocationModel>)items));
            }
            else if (items.get(0) instanceof MenuModel) {
                setAdapter(new MenusSpinnerAdapter((List<MenuModel>)items));
            }
            else {
                setAdapter(null);
            }
        } else {
            setAdapter(null);
        }
    }

    public interface MenusSpinnerAdapterInterface extends SpinnerAdapter {
        public boolean hasItems();
        public boolean isValidPosition(int position);
        public String getItemDisplayName(int position);
    }

    private abstract class MenuBaseSpinnerAdapter implements MenusSpinnerAdapterInterface {
        public static final int EMPTY_VIEW_TYPE = 0;
        public static final int NORMAL_VIEW_TYPE = 1;

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.menu_spinner_item, null);
            }

            if (!isValidPosition(position)) return convertView;

            TextView nameView = (TextView) convertView.findViewById(R.id.menu_spinner_item_name);
            nameView.setText(getItemDisplayName(position));

            ImageView iconView = (ImageView) convertView.findViewById(R.id.menu_spinner_item_icon);
            iconView.setVisibility(position == getSelectedItemPosition() ? VISIBLE : INVISIBLE);

            return convertView;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.menu_spinner, null);
            }

            if (mIconResource > 0) {
                ImageView iconView = (ImageView) convertView.findViewById(R.id.menu_spinner_icon);
                iconView.setBackgroundResource(mIconResource);
            }

            TextView titleView = (TextView) convertView.findViewById(R.id.menu_spinner_title);
            titleView.setText(String.format(mTitle, getCount()));

            TextView nameView = (TextView) convertView.findViewById(R.id.menu_spinner_name);
            nameView.setText(getItemDisplayName(position));

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


        @Override
        public boolean hasStableIds() {
            return false;
        }

    }


    private class MenuLocationsSpinnerAdapter extends MenuBaseSpinnerAdapter {

        private final List<MenuLocationModel> mMenuLocationItems;

        public MenuLocationsSpinnerAdapter(List<MenuLocationModel> items) {
            super();
            mMenuLocationItems = items;
        }

        @Override
        public boolean hasItems() {
            return mMenuLocationItems != null && mMenuLocationItems.size() > 0;
        }

        public boolean isValidPosition(int position) {
            return mMenuLocationItems != null && position >= 0 && position < mMenuLocationItems.size();
        }

        @Override
        public String getItemDisplayName(int position) {
            MenuLocationModel item =  (MenuLocationModel) getItem(position);
            if (item != null && item.name != null) {
                return item.name;
            } else {
                return ""; //empty name set on purpose
            }
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public int getCount() {
            return hasItems() ? mMenuLocationItems.size() : 1;
        }

        @Override
        public Object getItem(int position) {
            if (!isValidPosition(position)) return null;
            return mMenuLocationItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEmpty() {
            return !hasItems();
        }
    }


    private class MenusSpinnerAdapter extends MenuBaseSpinnerAdapter {

        private final List<MenuModel> mMenuItems;

        public MenusSpinnerAdapter(List<MenuModel> items) {
            super();
            mMenuItems = items;
        }

        @Override
        public boolean hasItems() {
            return mMenuItems != null && mMenuItems.size() > 0;
        }

        public boolean isValidPosition(int position) {
            return mMenuItems != null && position >= 0 && position < mMenuItems.size();
        }

        @Override
        public String getItemDisplayName(int position) {
            MenuLocationModel item =  (MenuLocationModel) getItem(position);
            if (item != null && item.name != null) {
                return item.name;
            } else {
                return ""; //empty name set on purpose
            }
        }

        @Override
        public void registerDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver observer) {
        }

        @Override
        public int getCount() {
            return hasItems() ? mMenuItems.size() : 1;
        }

        @Override
        public Object getItem(int position) {
            if (!isValidPosition(position)) return null;
            return mMenuItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEmpty() {
            return !hasItems();
        }
    }



}
