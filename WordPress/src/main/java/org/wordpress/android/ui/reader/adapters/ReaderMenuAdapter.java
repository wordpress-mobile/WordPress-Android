package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;

import java.util.ArrayList;
import java.util.List;

/*
 * adapter for the popup menu that appears when clicking "..." in the reader
 */
public class ReaderMenuAdapter extends BaseAdapter {

    private final LayoutInflater mInflater;
    private final List<Integer> mMenuItems = new ArrayList<>();

    public static final int ITEM_FOLLOW = 0;
    public static final int ITEM_UNFOLLOW = 1;
    public static final int ITEM_BLOCK = 2;

    public ReaderMenuAdapter(Context context, @NonNull List<Integer> menuItems) {
        super();
        mInflater = LayoutInflater.from(context);
        mMenuItems.addAll(menuItems);
    }

    @Override
    public int getCount() {
        return mMenuItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mMenuItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mMenuItems.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ReaderMenuHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.popup_menu_item, parent, false);
            holder = new ReaderMenuHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ReaderMenuHolder) convertView.getTag();
        }

        int textRes;
        int iconRes;
        switch (mMenuItems.get(position)) {
            case ITEM_FOLLOW:
                textRes = R.string.reader_btn_follow;
                iconRes = R.drawable.reader_follow;
                break;
            case ITEM_UNFOLLOW:
                textRes = R.string.reader_btn_unfollow;
                iconRes = R.drawable.reader_following;
                break;
            case ITEM_BLOCK:
                textRes = R.string.reader_menu_block_blog;
                iconRes = 0;
                break;
            default:
                return convertView;
        }


        holder.text.setText(textRes);
        if (iconRes != 0) {
            holder.icon.setImageResource(iconRes);
        }

        return convertView;
    }

    class ReaderMenuHolder {
        private final TextView text;
        private final ImageView icon;

        ReaderMenuHolder(View view) {
            text = (TextView) view.findViewById(R.id.text);
            icon = (ImageView) view.findViewById(R.id.image);
        }
    }
}
