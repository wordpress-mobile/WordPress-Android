package org.wordpress.android.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.MenuDrawerItems.DrawerItem;

public class MenuDrawerAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final MenuDrawerItems mItems = new MenuDrawerItems();

    public MenuDrawerAdapter(Context context) {
        super();
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void refresh() {
        mItems.refresh();
        notifyDataSetChanged();
    }

    public boolean hasSelectedItem(Context context) {
        return mItems.hasSelectedItem(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final DrawerViewHolder holder;
        if (convertView == null || !(convertView.getTag() instanceof DrawerViewHolder)) {
            convertView = mInflater.inflate(R.layout.menu_drawer_row, parent, false);
            holder = new DrawerViewHolder(convertView);
        } else {
            holder = (DrawerViewHolder) convertView.getTag();
        }

        DrawerItem item = mItems.get(position);
        int badgeCount = item.getBadgeCount();

        holder.txtTitle.setText(item.getTitleResId());
        holder.imgIcon.setImageResource(item.getIconResId());

        if (badgeCount > 0) {
            holder.txtBadge.setVisibility(View.VISIBLE);
            holder.txtBadge.setText(String.valueOf(badgeCount));
        } else {
            holder.txtBadge.setVisibility(View.GONE);
        }

        if (item.isSelected(parent.getContext())) {
            convertView.setBackgroundResource(R.color.md__background_selected);
        } else {
            convertView.setBackgroundResource(0);
        }

        return convertView;
    }

    private static class DrawerViewHolder {
        final TextView txtTitle;
        final TextView txtBadge;
        final ImageView imgIcon;

        DrawerViewHolder(View view) {
            txtTitle = (TextView) view.findViewById(R.id.menu_row_title);
            txtBadge = (TextView) view.findViewById(R.id.menu_row_badge);
            imgIcon = (ImageView) view.findViewById(R.id.menu_row_icon);
        }
    }
}
