package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.ui.menus.event.MenuEvents;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;

class MenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private int mPadding;
    private static int ADD_MENU_ITEM_ABOVE_ID = -100;
    private static int ADD_MENU_ITEM_BELOW_ID = -200;
    private static int ADD_MENU_ITEM_TO_CHILDREN_ID = -300;

    private static final int VIEW_TYPE_NORMAL  = 0;
    private static final int VIEW_TYPE_ADDER = 1;

    private boolean mInAddingMode = false;

    private final List<MenuItemModel> mFlattenedMenuItems = new ArrayList<>();

    class MenuItemHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final ImageView imgMenuItemType;
        private final ImageView imgEditIcon;
        private final ImageView imgAddIcon;
        private final ImageView imgArrowLeft;
        private final ImageView imgArrowRight;
        private final WPButton  btnCancelAdd;
        private final ViewGroup containerCancel;
        private final ViewGroup containerButtons;
        private final ViewGroup containerView;

        public MenuItemHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.title);
            imgMenuItemType = (ImageView) view.findViewById(R.id.image_menu_item_type);
            containerView = (ViewGroup) view.findViewById(R.id.layout_container_outter);
            imgEditIcon = (ImageView) view.findViewById(R.id.icon_edit);
            imgAddIcon = (ImageView) view.findViewById(R.id.icon_add);
            imgArrowLeft = (ImageView) view.findViewById(R.id.arrow_left);
            imgArrowRight = (ImageView) view.findViewById(R.id.arrow_right);
            btnCancelAdd = (WPButton) view.findViewById(R.id.cancel_button);
            containerCancel = (ViewGroup) view.findViewById(R.id.layout_cancel_button);
            containerButtons = (ViewGroup) view.findViewById(R.id.layout_buttons);
        }
    }

    class AddItemHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final ImageView imgAddIcon;
        private final ViewGroup containerView;

        public AddItemHolder(View view) {
            super(view);
            txtTitle = (TextView) view.findViewById(R.id.title);
            containerView = (ViewGroup) view.findViewById(R.id.layout_container_outter);
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
        switch (viewType) {
            case VIEW_TYPE_ADDER: {
                View view = mInflater.inflate(R.layout.menu_item_adderitem, null);
                AddItemHolder holder = new AddItemHolder(view);
                view.setTag(holder);
                return holder;
            }
            case VIEW_TYPE_NORMAL:
            default:
            {
                View view = mInflater.inflate(R.layout.menu_item_listitem, null);
                MenuItemHolder holder = new MenuItemHolder(view);
                view.setTag(holder);
                return holder;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mFlattenedMenuItems != null){
            MenuItemModel item = mFlattenedMenuItems.get(position);
            if (item != null) {
                if (item instanceof AddItem) {
                    return VIEW_TYPE_ADDER;
                }
            }
        }
        return VIEW_TYPE_NORMAL;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, final int position) {

        switch (getItemViewType(position)) {
            case VIEW_TYPE_ADDER: {
                final AddItem menuItem = (AddItem) mFlattenedMenuItems.get(position);
                AddItemHolder holder = (AddItemHolder) viewHolder;
                holder.txtTitle.setText(Html.fromHtml(menuItem.name));
                mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.containerView.getLayoutParams();
                p.setMargins(menuItem.flattenedLevel * mPadding,0,0,0);
                holder.containerView.requestLayout();
                holder.imgAddIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO here integrate with @tonyhr59's implementation of item creation
                        ToastUtils.showToast(mContext, "not implemented yet", ToastUtils.Duration.SHORT);
                    }
                });

                break;
            }

            case VIEW_TYPE_NORMAL:
            default:
            {
                final MenuItemModel menuItem = mFlattenedMenuItems.get(position);
                MenuItemHolder holder = (MenuItemHolder) viewHolder;

                holder.txtTitle.setText(Html.fromHtml(menuItem.name));
                mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.containerView.getLayoutParams();
                p.setMargins(menuItem.flattenedLevel * mPadding,0,0,0);
                holder.containerView.requestLayout();
                //TODO: set the correct icon type depending on the menu item type, check with @tonyrh59
                if (menuItem.type != null) {
                    switch (menuItem.type) {
                        case "post":
                            holder.imgMenuItemType.setImageResource(R.drawable.my_site_icon_pages);
                            break;
                    }
                }

                if (mInAddingMode) {

                    holder.containerButtons.setVisibility(View.GONE);
                    if (menuItem.editingMode) {
                        holder.containerCancel.setVisibility(View.VISIBLE);
                    }
                    else {
                        holder.containerCancel.setVisibility(View.GONE);
                    }

                    holder.btnCancelAdd.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mInAddingMode = false;
                            menuItem.editingMode = false;
                            //eliminate now items above and below
                            mFlattenedMenuItems.remove(position + 1);
                            notifyItemRemoved(position + 1);
                            mFlattenedMenuItems.remove(position + 1);
                            notifyItemRemoved(position + 1);
                            mFlattenedMenuItems.remove(position - 1);
                            notifyItemRemoved(position - 1);

                            Handler hdlr = new Handler();
                            hdlr.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            }, 350);

                        }
                    });

                } else {

                    holder.containerButtons.setVisibility(View.VISIBLE);
                    holder.containerCancel.setVisibility(View.GONE);

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

                            //insert items above and below
                            menuItem.editingMode = true;

                            AddItemAbove above = new AddItemAbove(mContext);
                            above.flattenedLevel = menuItem.flattenedLevel;
                            mFlattenedMenuItems.add(position, above);
                            notifyItemInserted(position);

                            AddItemBelow below = new AddItemBelow(mContext);
                            below.flattenedLevel = menuItem.flattenedLevel;
                            if (position == mFlattenedMenuItems.size()-1) {
                                mFlattenedMenuItems.add(below);
                                notifyItemInserted(mFlattenedMenuItems.size()-1);
                            } else {
                                mFlattenedMenuItems.add(position + 2, below);
                                notifyItemInserted(position + 2);
                            }

                            AddItemToChildren toChildren = new AddItemToChildren(mContext);
                            toChildren.flattenedLevel = menuItem.flattenedLevel + 1;
                            if (position == mFlattenedMenuItems.size()-1) {
                                mFlattenedMenuItems.add(toChildren);
                                notifyItemInserted(mFlattenedMenuItems.size() - 1);
                            } else {
                                mFlattenedMenuItems.add(position + 3, toChildren);
                                notifyItemInserted(position + 3);
                            }

                            mInAddingMode = true;

                            EventBus.getDefault().post(new MenuEvents.AddMenuClicked(position));

                            Handler hdlr = new Handler();
                            hdlr.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            }, 350);

                        }
                    });

                    holder.imgArrowLeft.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //if position == 0 we can't move left - the first item always has the left-most level
                            if (position > 0) {
                                if (mFlattenedMenuItems.size() >= (position + 1) && (menuItem.flattenedLevel > 0)) {
                                    MenuItemModel nextItem = null;
                                    if (position == mFlattenedMenuItems.size()-1) {
                                        //last item - special case
                                        if (menuItem.flattenedLevel > 0) {
                                            menuItem.flattenedLevel--;
                                            notifyItemChanged(position);
                                        }
                                    } else {
                                        nextItem = mFlattenedMenuItems.get(position + 1);
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
                        }
                    });

                    holder.imgArrowRight.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //if position == 0 we can't move right - the first item always has the left-most level
                            if (position > 0) {
                                MenuItemModel prevItem = mFlattenedMenuItems.get(position - 1);
                                if ((mFlattenedMenuItems.size() >= position + 1) && (prevItem.flattenedLevel >= menuItem.flattenedLevel)) {
                                    MenuItemModel nextItem = null;
                                    if (position == mFlattenedMenuItems.size()-1) {
                                        //last item - special case, only check prev item
                                        if (prevItem.flattenedLevel >= menuItem.flattenedLevel) {
                                            menuItem.flattenedLevel++;
                                            notifyItemChanged(position);
                                        }
                                    } else {
                                        nextItem = mFlattenedMenuItems.get(position + 1);
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
                        }
                    });

                    MenuItemModel prevItem = (position > 0) ? mFlattenedMenuItems.get(position - 1) : null;
                    holder.imgArrowLeft.setEnabled(menuItem.flattenedLevel > 0);
                    holder.imgArrowRight.setEnabled((position > 0) && (prevItem != null && (prevItem.flattenedLevel >= menuItem.flattenedLevel)));
                }
            }
        }

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

        //now check that we don't have any gaps in between flattenedLevels (all jumps need to be consecutive)
        sanitizeFlattenedLevels();

        //we need no notifyDataSetChanged instead of notifyItemChanged(toPosition);
        // so every item will calculate its enabled/disabled states for arrow left/right
        notifyDataSetChanged();
        return true;
    }

    /**
     * check and fix there are no gaps in between flattenedLevels
     */
    private void sanitizeFlattenedLevels(){
        int lastLevel = 0;
        for (MenuItemModel item : mFlattenedMenuItems) {
            if (item.flattenedLevel > (lastLevel+1)) {
                //sanitize this and the following items
                item.flattenedLevel--;
                lastLevel = item.flattenedLevel;
            }
        }
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


    private abstract class AddItem extends MenuItemModel {
    }

    private class AddItemAbove extends AddItem {
        public AddItemAbove(Context context){
            this.menuId = ADD_MENU_ITEM_ABOVE_ID;
            this.name = context.getString(R.string.menus_add_item_above);
        }
    }

    private class AddItemBelow extends AddItem {
        public AddItemBelow(Context context){
            this.menuId = ADD_MENU_ITEM_BELOW_ID;
            this.name = context.getString(R.string.menus_add_item_below);
        }
    }

    private class AddItemToChildren extends AddItem {
        public AddItemToChildren(Context context){
            this.menuId = ADD_MENU_ITEM_TO_CHILDREN_ID;
            this.name = context.getString(R.string.menus_add_item_to_children);
        }
    }

}
