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
import java.util.Collections;
import java.util.List;

class MenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private int mPadding;

    private final List<MenuItemModel> mFlattenedMenuItems = new ArrayList<>();

    class MenuItemHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final ImageView imgMenuItemType;
        private final ImageView imgEditIcon;
        private final ImageView imgAddIcon;
        private final ImageView imgArrowLeft;
        private final ImageView imgArrowRight;
        private final ViewGroup containerView;

        public MenuItemHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.title);
            imgMenuItemType = (ImageView) view.findViewById(R.id.image_menu_item_type);
            containerView = (ViewGroup) view.findViewById(R.id.layout_container);
            imgEditIcon = (ImageView) view.findViewById(R.id.icon_edit);
            imgAddIcon = (ImageView) view.findViewById(R.id.icon_add);
            imgArrowLeft = (ImageView) view.findViewById(R.id.arrow_left);
            imgArrowRight = (ImageView) view.findViewById(R.id.arrow_right);
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
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {
        final MenuItemModel menuItem = mFlattenedMenuItems.get(position);
        MenuItemHolder holder = (MenuItemHolder) viewHolder;

        holder.txtTitle.setText(Html.fromHtml(menuItem.name));
        mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.containerView.getLayoutParams();
        p.setMargins(menuItem.flattenedLevel * mPadding,0,0,0);
        holder.containerView.requestLayout();
        //TODO: set the correct icon type depending on the menu item type, check with @tonyrh59
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

        holder.imgArrowLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if position == 0 we can't move left - the first item always has the left-most level
                if (position > 0) {
                    if (mFlattenedMenuItems.size() > (position + 1) && (menuItem.flattenedLevel > 0)) {
                        MenuItemModel nextItem = mFlattenedMenuItems.get(position + 1);
                        //if next item is less or equal in level, this item has no children; it's OK to decrement the level
                        if (nextItem.flattenedLevel <= menuItem.flattenedLevel) {
                            menuItem.flattenedLevel--;
                            notifyItemChanged(position);
                        } else {
                            //otherwise if next item has a greater level -> that means it's a child of the current item, so all the children
                            //need to be moved as well
                            int currentLevel = menuItem.flattenedLevel;
                            boolean downTreeBranch = false;
                            for (int i = position; i < mFlattenedMenuItems.size(); i++) {
                                MenuItemModel item = mFlattenedMenuItems.get(i);
                                if (item.flattenedLevel >= currentLevel) {
                                    if (item.flattenedLevel > currentLevel) {
                                        downTreeBranch = true;
                                    } else {
                                        if (downTreeBranch) {
                                            break;
                                        }
                                    }
                                    item.flattenedLevel--;
                                    notifyItemChanged(i);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        holder.imgArrowRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //if position == 0 we can't move right - the first item always has the left-most level
                if (position > 0) {
                    MenuItemModel prevItem = mFlattenedMenuItems.get(position - 1);
                    if (mFlattenedMenuItems.size() >= position + 1 && (prevItem.flattenedLevel >= menuItem.flattenedLevel)) {
                        MenuItemModel nextItem = mFlattenedMenuItems.get(position + 1);
                        //if next item is less or equal in level, it's OK to augment the level
                        if (nextItem.flattenedLevel <= menuItem.flattenedLevel) {
                            menuItem.flattenedLevel++;
                            notifyItemChanged(position);
                        } else {
                            //otherwise if next item has a greater level -> that means it's a child of the current item, so all the children
                            //need to be moved as well
                            int currentLevel = menuItem.flattenedLevel;
                            boolean downTreeBranch = false;
                            for (int i = position; i < mFlattenedMenuItems.size(); i++) {
                                MenuItemModel item = mFlattenedMenuItems.get(i);
                                if (item.flattenedLevel >= currentLevel) {
                                    if (item.flattenedLevel > currentLevel) {
                                        downTreeBranch = true;
                                    } else {
                                        if (downTreeBranch) {
                                            break;
                                        }
                                    }
                                    item.flattenedLevel++;
                                    notifyItemChanged(i);
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        MenuItemModel prevItem = (position > 0) ? mFlattenedMenuItems.get(position - 1) : null;
        holder.imgArrowLeft.setEnabled(menuItem.flattenedLevel > 0);
        holder.imgArrowRight.setEnabled((position > 0) && (prevItem != null && (prevItem.flattenedLevel >= menuItem.flattenedLevel)));

    }

    public MenuItemModel getItem(int position) {
        if (isPositionValid(position)) {
            return mFlattenedMenuItems.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return mFlattenedMenuItems.get(position).itemId;
    }

    @Override
    public int getItemCount() {
        return mFlattenedMenuItems.size();
    }

    private boolean isEmpty() {
        return getItemCount() == 0;
    }

    private boolean isPositionValid(int position) {
        return (position >= 0 && position < mFlattenedMenuItems.size());
    }

    /*
    * menu item operations
     */
    void replaceMenuItems(List<MenuItemModel> menuItems) {
        if (menuItems != null) {
            mFlattenedMenuItems.clear();
            mFlattenedMenuItems.addAll(menuItems);
            notifyDataSetChanged();
        }
    }

    void deleteMenuItem(MenuItemModel menuItem) {
        mFlattenedMenuItems.remove(menuItem);
        notifyDataSetChanged();
    }

    void deleteMenuItem(int index) {
        mFlattenedMenuItems.remove(index);
        notifyDataSetChanged();
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mFlattenedMenuItems, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mFlattenedMenuItems, i, i - 1);
            }
        }

        notifyItemMoved(fromPosition, toPosition);
        return false;
    }

    public boolean onDrop(int fromPosition, int toPosition) {
        //update flattened level for this position depending on the item above
        if (toPosition == 0)
            mFlattenedMenuItems.get(toPosition).flattenedLevel = 0;
        else {
            int previousItemLevel = mFlattenedMenuItems.get(toPosition-1).flattenedLevel;
            mFlattenedMenuItems.get(toPosition).flattenedLevel = previousItemLevel;
        }

        notifyItemChanged(toPosition);
        return true;
    }


    /*
     * clear all menu items
     */
    public void clearMenuItems() {
        if (mFlattenedMenuItems != null){
            mFlattenedMenuItems.clear();
            notifyDataSetChanged();
        }
    }

    public List<MenuItemModel> getCurrentMenuItems(){
        return mFlattenedMenuItems;
    }

}
