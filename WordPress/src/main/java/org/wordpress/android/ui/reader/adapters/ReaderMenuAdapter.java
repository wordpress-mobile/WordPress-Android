package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import org.wordpress.android.R;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.ContextExtensionsKt;

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
        int iconColorRes;
        int iconRes;
        switch (mMenuItems.get(position)) {
            case ITEM_FOLLOW:
                textRes = R.string.reader_btn_follow;
                textColorRes =
                        ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), R.attr.colorPrimary);
                iconColorRes = textColorRes;
                iconRes = R.drawable.ic_reader_follow_white_24dp;
                break;
            case ITEM_UNFOLLOW:
                textRes = R.string.reader_btn_unfollow;
                textColorRes =
                        ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), R.attr.wpColorSuccess);
                iconColorRes = textColorRes;
                iconRes = R.drawable.ic_reader_following_white_24dp;
                break;
            case ITEM_BLOCK:
                textRes = R.string.reader_menu_block_blog;
                textColorRes =
                        ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), R.attr.colorOnSurface);
                iconColorRes = ContextExtensionsKt
                        .getColorResIdFromAttribute(convertView.getContext(), R.attr.wpColorOnSurfaceMedium);
                iconRes = R.drawable.ic_block_white_24dp;
                break;
            case ITEM_NOTIFICATIONS_OFF:
                textRes = R.string.reader_btn_notifications_off;
                textColorRes =
                        ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), R.attr.wpColorSuccess);
                iconColorRes = textColorRes;
                iconRes = R.drawable.ic_bell_white_24dp;
                break;
            case ITEM_NOTIFICATIONS_ON:
                textRes = R.string.reader_btn_notifications_on;
                textColorRes = ContextExtensionsKt
                        .getColorResIdFromAttribute(convertView.getContext(), R.attr.colorOnSurface);
                iconColorRes = ContextExtensionsKt
                        .getColorResIdFromAttribute(convertView.getContext(), R.attr.wpColorOnSurfaceMedium);
                iconRes = R.drawable.ic_bell_white_24dp;
                break;
            case ITEM_SHARE:
                textRes = R.string.reader_btn_share;
                textColorRes =
                        ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), R.attr.colorOnSurface);
                iconColorRes = ContextExtensionsKt
                        .getColorResIdFromAttribute(convertView.getContext(), R.attr.wpColorOnSurfaceMedium);
                iconRes = R.drawable.ic_share_white_24dp;
                break;
            default:
                return convertView;
        }

        holder.mText.setText(textRes);
        holder.mText.setTextColor(AppCompatResources.getColorStateList(convertView.getContext(), textColorRes));
        ColorUtils.INSTANCE.setImageResourceWithTint(holder.mIcon, iconRes, iconColorRes);

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
