package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction;
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.SecondaryAction;
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType;
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
    private final List<ReaderPostCardAction> mMenuItems = new ArrayList<>();
    private final UiHelpers mUiHelpers;

    public ReaderMenuAdapter(Context context, @NonNull UiHelpers uiHelpers,
                             @NonNull List<ReaderPostCardAction> menuItems) {
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

        ReaderPostCardAction cardAction = mMenuItems.get(position);
        if (cardAction.getType() == ReaderPostCardActionType.SPACER_NO_ACTION) {
            holder.mText.setVisibility(View.INVISIBLE);
            holder.mIcon.setVisibility(View.INVISIBLE);
            holder.mContainer.setClickable(false);
        } else {
            SecondaryAction item = (SecondaryAction) cardAction;
            CharSequence textRes = mUiHelpers.getTextOfUiString(convertView.getContext(), item.getLabel());
            int textColorRes =
                    ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), item.getLabelColor());
            int iconColorRes =
                    ContextExtensionsKt.getColorResIdFromAttribute(convertView.getContext(), item.getIconColor());
            int iconRes = item.getIconRes();

            holder.mText.setText(textRes);
            holder.mText.setTextColor(AppCompatResources.getColorStateList(convertView.getContext(), textColorRes));
            ColorUtils.INSTANCE.setImageResourceWithTint(holder.mIcon, iconRes, iconColorRes);
        }

        return convertView;
    }

    class ReaderMenuHolder {
        private final TextView mText;
        private final ImageView mIcon;
        private final LinearLayout mContainer;

        ReaderMenuHolder(View view) {
            mText = view.findViewById(R.id.text);
            mIcon = view.findViewById(R.id.image);
            mContainer = view.findViewById(R.id.reader_popup_menu_item_container);
        }
    }
}
