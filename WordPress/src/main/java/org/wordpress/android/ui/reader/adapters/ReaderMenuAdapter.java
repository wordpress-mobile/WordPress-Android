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
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.ContextExtensionsKt;

import java.util.ArrayList;
import java.util.List;

/*
 * adapter for the popup menu that appears when clicking "..." in the reader
 */
public class ReaderMenuAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final List<SecondaryAction> mMenuItems = new ArrayList<>();
    private final UiHelpers mUiHelpers;

    public ReaderMenuAdapter(Context context, @NonNull UiHelpers uiHelpers, @NonNull List<SecondaryAction> menuItems) {
        super();
        mInflater = LayoutInflater.from(context);
        mMenuItems.addAll(menuItems);
        mUiHelpers = uiHelpers;
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
        return mMenuItems.get(position).getType().ordinal();
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

        SecondaryAction item = mMenuItems.get(position);
        String textRes = mUiHelpers.getTextOfUiString(convertView.getContext(), item.getLabel());
        int textColorRes =
                ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), item.getLabelColor());
        int iconColorRes =
                ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), item.getIconColor());
        int iconRes = item.getIconRes();

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
