package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.List;

class MenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private int mPadding;

    private final List<MenuItemModel> mMenuItems = new ArrayList<>();

    class MenuItemHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final ImageView imgMenuItemType;
        private final ImageView imgEditIcon;
        private final ImageView imgAddIcon;
        private final ViewGroup containerView;

        public MenuItemHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.title);
            imgMenuItemType = (ImageView) view.findViewById(R.id.image_menu_item_type);
            containerView = (ViewGroup) view.findViewById(R.id.layout_container);
            imgEditIcon = (ImageView) view.findViewById(R.id.icon_edit);
            imgAddIcon = (ImageView) view.findViewById(R.id.icon_add);
        }
    }

    MenuItemAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.menu_item_listitem, null);
        MenuItemHolder holder = new MenuItemHolder(view);
        view.setTag(holder);
        return holder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
        MenuItemModel menuItem = mMenuItems.get(position);
        MenuItemHolder holder = (MenuItemHolder) viewHolder;

        holder.txtTitle.setText(Html.fromHtml(menuItem.name));
        mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
        holder.containerView.setPadding(menuItem.flattenedLevel * mPadding,0,0,0);
        //TODO: set the correct icon type depending on the menu item type
        switch (menuItem.type) {
            case "post":
                holder.imgMenuItemType.setImageResource(R.drawable.my_site_icon_pages);
                break;
        }

        holder.imgEditIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO implement edit
                ToastUtils.showToast(mContext, "not implemented yet", ToastUtils.Duration.SHORT);
            }
        });

        holder.imgAddIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO implement add item
                ToastUtils.showToast(mContext, "not implemented yet", ToastUtils.Duration.SHORT);
            }
        });
    }

    public MenuItemModel getItem(int position) {
        if (isPositionValid(position)) {
            return mMenuItems.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return mMenuItems.get(position).itemId;
    }

    @Override
    public int getItemCount() {
        return mMenuItems.size();
    }

    private boolean isEmpty() {
        return getItemCount() == 0;
    }

    private boolean isPositionValid(int position) {
        return (position >= 0 && position < mMenuItems.size());
    }

    /*
    * menu item operations
     */
    void replaceMenuItems(List<MenuItemModel> menuItems) {
        if (menuItems != null) {
            mMenuItems.clear();
            mMenuItems.addAll(menuItems);
            notifyDataSetChanged();
        }
    }

    void deleteMenuItem(MenuItemModel menuItem) {
        mMenuItems.remove(menuItem);
        notifyDataSetChanged();
    }

    /*
     * clear all menu items
     */
    void clearMenuItems() {
        if (mMenuItems != null){
            mMenuItems.clear();
            notifyDataSetChanged();
        }
    }

}
