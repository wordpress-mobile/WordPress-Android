package org.wordpress.android.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.ArrayList;

public class MenuDrawerAdapter extends ArrayAdapter<MenuDrawerItem> {

    public MenuDrawerAdapter(Context context) {
        super(context, R.layout.menu_drawer_row, R.id.menu_row_title, new ArrayList<MenuDrawerItem>());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view = super.getView(position, convertView, parent);
        final TextView txtTitle = (TextView) view.findViewById(R.id.menu_row_title);
        final TextView txtBadge = (TextView) view.findViewById(R.id.menu_row_badge);
        final ImageView imgIcon = (ImageView) view.findViewById(R.id.menu_row_icon);

        final MenuDrawerItem item = getItem(position);
        boolean isSelected = item.isSelected();

        txtTitle.setText(item.getTitleRes());
        txtTitle.setSelected(isSelected);
        imgIcon.setImageResource(item.getIconRes());
        txtBadge.setVisibility(View.GONE);

        if (isSelected) {
            view.setBackgroundResource(R.color.md__background_selected);
        } else {
            view.setBackgroundResource(R.drawable.md_list_selector);
        }

        // allow the drawer item to configure the view
        item.configureView(view);

        return view;
    }
}
