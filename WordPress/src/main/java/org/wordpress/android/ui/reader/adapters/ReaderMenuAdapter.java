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
    public static final int ITEM_NOTIFICATIONS_OFF = 3;
    public static final int ITEM_NOTIFICATIONS_ON = 4;
    public static final int ITEM_SHARE = 5;

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
            convertView = mInflater.inflate(R.layout.reader_popup_menu_item, parent, false);
            holder = new ReaderMenuHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ReaderMenuHolder) convertView.getTag();
        }

        int textRes;
        int textColorRes;
        int iconRes;
        switch (mMenuItems.get(position)) {
            case ITEM_FOLLOW:
                textRes = R.string.reader_btn_follow;
                textColorRes = R.color.reader_follow;
                iconRes = R.drawable.ic_reader_follow_blue_medium_24dp;
                break;
            case ITEM_UNFOLLOW:
                textRes = R.string.reader_btn_unfollow;
                textColorRes = R.color.reader_following;
                iconRes = R.drawable.ic_reader_following_alert_green_24dp;
                break;
            case ITEM_BLOCK:
                textRes = R.string.reader_menu_block_blog;
                textColorRes = R.color.grey_dark;
                iconRes = R.drawable.ic_block_grey_dark_24dp;
                break;
            case ITEM_NOTIFICATIONS_OFF:
                textRes = R.string.reader_btn_notifications_off;
                textColorRes = R.color.reader_notifications_off;
                iconRes = R.drawable.ic_bell_alert_green_24dp;
                break;
            case ITEM_NOTIFICATIONS_ON:
                textRes = R.string.reader_btn_notifications_on;
                textColorRes = R.color.reader_notifications_on;
                iconRes = R.drawable.ic_bell_grey_dark_24dp;
                break;
            case ITEM_SHARE:
                textRes = R.string.reader_btn_share;
                textColorRes = R.color.grey_dark;
                iconRes = R.drawable.ic_share_grey_dark_24dp;
                break;
            default:
                return convertView;
        }

        holder.mText.setText(textRes);
        holder.mText.setTextColor(convertView.getContext().getResources().getColor(textColorRes));

        if (iconRes != 0) {
            holder.mIcon.setImageResource(iconRes);
        }

        return convertView;
    }

    class ReaderMenuHolder {
        private final TextView mText;
        private final ImageView mIcon;

        ReaderMenuHolder(View view) {
            mText = view.findViewById(R.id.text);
            mIcon = view.findViewById(R.id.image);
        }
    }
}
