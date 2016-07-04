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
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory;
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory.ITEM_TYPE;
import org.wordpress.android.widgets.WPButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.event.EventBus;

public class MenuItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface MenuItemInteractions {
        void onItemClick(MenuItemModel item, int position);
        void onCancelClick();
        void onAddClick(int position, ItemAddPosition where);
    }

    public enum ItemAddPosition {
        ZERO,
        ABOVE,
        BELOW,
        TO_CHILDREN
    }

    private final LayoutInflater mInflater;
    private final Context mContext;
    private int mPadding;
    private static int ADD_MENU_ITEM_ALONE_ID = -50;
    private static int ADD_MENU_ITEM_ABOVE_ID = -100;
    private static int ADD_MENU_ITEM_BELOW_ID = -200;
    private static int ADD_MENU_ITEM_TO_CHILDREN_ID = -300;

    private static final int VIEW_TYPE_NORMAL  = 0;
    private static final int VIEW_TYPE_ADDER = 1;

    private boolean mInAddingMode = false;
    private int mAddingModeStarterPosition = -1;

    private final List<MenuItemModel> mFlattenedMenuItems = new ArrayList<>();

    private MenuItemInteractions mListener;

    MenuItemAdapter(Context context, MenuItemInteractions listener) {
        mListener = listener;
        mInflater = LayoutInflater.from(context);
        mContext = context;
        mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case VIEW_TYPE_ADDER: {
                View view = mInflater.inflate(R.layout.menu_item_adderitem, parent, false);
                AddItemHolder holder = new AddItemHolder(view);
                view.setTag(holder);
                return holder;
            }
            case VIEW_TYPE_NORMAL:
            default:
            {
                View view = mInflater.inflate(R.layout.menu_item_listitem, parent, false);
                MenuItemHolder holder = new MenuItemHolder(view);
                view.setTag(holder);
                return holder;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mFlattenedMenuItems.size() == 0) return VIEW_TYPE_ADDER;

        MenuItemModel item = mFlattenedMenuItems.get(position);
        if (item != null) {
            if (item instanceof AddItem) {
                return VIEW_TYPE_ADDER;
            }
        }
        return VIEW_TYPE_NORMAL;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, int position) {
        switch (getItemViewType(position)) {
            case VIEW_TYPE_ADDER: {
                final AddItem menuItem = (AddItem) mFlattenedMenuItems.get(position);
                final int adderPosition = position;
                AddItemHolder holder = (AddItemHolder) viewHolder;
                holder.txtTitle.setText(Html.fromHtml(menuItem.name));
                mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.containerView.getLayoutParams();
                p.setMargins(menuItem.flattenedLevel * mPadding,0,0,0);
                holder.containerView.requestLayout();

                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        menuItem.editingMode = false;
                        mInAddingMode = false;
                        if (isEmptyList()) {
                            mAddingModeStarterPosition = 0;
                            mListener.onAddClick(0, ItemAddPosition.ZERO);
                        } else {
                            //mAddingModeStarterPosition plus one as at this very moment we have the 3 adder control buttons in our list
                            triggerAddControlRemoveAnimation(mAddingModeStarterPosition + 1, true);
                            mListener.onAddClick(mAddingModeStarterPosition, getWhereToAddMenuItem(mAddingModeStarterPosition + 1, adderPosition));
                            Handler hdlr = new Handler();
                            hdlr.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                    }
                            }, 350);
                        }
                    }
                };

                holder.imgAddIcon.setOnClickListener(clickListener);
                holder.containerView.setOnClickListener(clickListener);

                break;
            }
            case VIEW_TYPE_NORMAL:
            default:
            {
                final MenuItemModel menuItem = mFlattenedMenuItems.get(position);
                final int itemPosition = position;
                MenuItemHolder holder = (MenuItemHolder) viewHolder;

                holder.txtTitle.setText(Html.fromHtml(menuItem.name));
                mPadding = mContext.getResources().getDimensionPixelOffset(R.dimen.margin_medium);
                ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) holder.containerView.getLayoutParams();
                p.setMargins(menuItem.flattenedLevel * mPadding,0,0,0);
                holder.containerView.requestLayout();
                holder.containerView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onItemClick(menuItem, itemPosition);
                    }
                });
                ITEM_TYPE itemType = ITEM_TYPE.typeForString(menuItem.calculatedType != null ? menuItem.calculatedType : menuItem.type);
                if (itemType != ITEM_TYPE.NULL) {
                    int itemRes = MenuItemEditorFactory.getIconDrawableRes(itemType);
                    if (itemRes != -1) {
                        holder.imgMenuItemType.setImageResource(itemRes);
                    } else {
                        holder.imgMenuItemType.setImageResource(0);
                    }
                } else {
                    holder.imgMenuItemType.setImageResource(0);
                }

                if (mInAddingMode) {
                    holder.containerButtons.setVisibility(View.GONE);
                    if (menuItem.editingMode) {
                        holder.containerCancel.setVisibility(View.VISIBLE);
                    } else {
                        holder.containerCancel.setVisibility(View.GONE);
                    }

                    holder.btnCancelAdd.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int pos = viewHolder.getAdapterPosition();
                            menuItem.editingMode = false;
                            mAddingModeStarterPosition = -1;
                            mInAddingMode = false;
                            triggerAddControlRemoveAnimation(pos, false);

                            if (mListener != null) {
                                mListener.onCancelClick();
                            }

                        }
                    });
                } else {
                    holder.containerButtons.setVisibility(View.VISIBLE);
                    holder.containerCancel.setVisibility(View.GONE);

                    holder.imgAddIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            int pos = viewHolder.getAdapterPosition();

                            //insert items above and below
                            menuItem.editingMode = true;

                            AddItem above = new AddItem(ADD_MENU_ITEM_ABOVE_ID, mContext.getString(R.string.menus_add_item_above));
                            above.flattenedLevel = menuItem.flattenedLevel;
                            mFlattenedMenuItems.add(pos, above);
                            notifyItemInserted(pos);

                            AddItem below = new AddItem(ADD_MENU_ITEM_BELOW_ID, mContext.getString(R.string.menus_add_item_below));
                            below.flattenedLevel = menuItem.flattenedLevel;
                            if (pos == mFlattenedMenuItems.size() - 1) {
                                mFlattenedMenuItems.add(below);
                                notifyItemInserted(mFlattenedMenuItems.size() - 1);
                            } else {
                                mFlattenedMenuItems.add(pos + 2, below);
                                notifyItemInserted(pos + 2);
                            }

                            AddItem toChildren = new AddItem(ADD_MENU_ITEM_TO_CHILDREN_ID, mContext.getString(R.string.menus_add_item_to_children));
                            toChildren.flattenedLevel = menuItem.flattenedLevel + 1;
                            if (pos == mFlattenedMenuItems.size() - 1) {
                                mFlattenedMenuItems.add(toChildren);
                                notifyItemInserted(mFlattenedMenuItems.size() - 1);
                            } else {
                                mFlattenedMenuItems.add(pos + 3, toChildren);
                                notifyItemInserted(pos + 3);
                            }

                            mInAddingMode = true;
                            mAddingModeStarterPosition = pos;

                            EventBus.getDefault().post(new MenuEvents.AddMenuClicked(pos));

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
                            int pos = viewHolder.getAdapterPosition();
                            //if position == 0 we can't move left - the first item always has the left-most level
                            if (pos > 0) {
                                if (mFlattenedMenuItems.size() >= (pos + 1) && (menuItem.flattenedLevel > 0)) {
                                    MenuItemModel nextItem = null;
                                    if (pos == mFlattenedMenuItems.size()-1) {
                                        //last item - special case
                                        if (menuItem.flattenedLevel > 0) {
                                            menuItem.flattenedLevel--;
                                            notifyItemChanged(pos);
                                        }
                                    } else {
                                        nextItem = mFlattenedMenuItems.get(pos + 1);
                                        //if next item is less or equal in level, this item has no children; it's OK to decrement the level
                                        if (nextItem.flattenedLevel <= menuItem.flattenedLevel) {
                                            menuItem.flattenedLevel--;
                                            notifyItemChanged(pos);
                                        } else {
                                            //otherwise if next item has a greater level -> that means it's a child of the current item, so all the children
                                            //need to be moved as well
                                            int currentLevel = menuItem.flattenedLevel;
                                            boolean downTreeBranch = false;
                                            for (int i = pos; i < mFlattenedMenuItems.size(); i++) {
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
                            int pos = viewHolder.getAdapterPosition();
                            //if position == 0 we can't move right - the first item always has the left-most level
                            if (pos > 0) {
                                MenuItemModel prevItem = mFlattenedMenuItems.get(pos - 1);
                                if ((mFlattenedMenuItems.size() >= pos + 1) && (prevItem.flattenedLevel >= menuItem.flattenedLevel)) {
                                    MenuItemModel nextItem = null;
                                    if (pos == mFlattenedMenuItems.size()-1) {
                                        //last item - special case, only check prev item
                                        if (prevItem.flattenedLevel >= menuItem.flattenedLevel) {
                                            menuItem.flattenedLevel++;
                                            notifyItemChanged(pos);
                                        }
                                    } else {
                                        nextItem = mFlattenedMenuItems.get(pos + 1);
                                        //if next item is less or equal in level, it's OK to augment the level
                                        if (nextItem.flattenedLevel <= menuItem.flattenedLevel) {
                                            menuItem.flattenedLevel++;
                                            notifyItemChanged(pos);
                                        } else {
                                            //otherwise if next item has a greater level -> that means it's a child of the current item, so all the children
                                            //need to be moved as well
                                            int currentLevel = menuItem.flattenedLevel;
                                            boolean downTreeBranch = false;
                                            for (int i = pos; i < mFlattenedMenuItems.size(); i++) {
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

    public boolean isEmptyList() {
        return mFlattenedMenuItems.size() == 1 && mFlattenedMenuItems.get(0).itemId == ADD_MENU_ITEM_ALONE_ID;
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
            if (mFlattenedMenuItems.size() == 0) {
                AddItem item = new AddItem(ADD_MENU_ITEM_ALONE_ID, mContext.getString(R.string.menus_add_new_item));
                mFlattenedMenuItems.add(item);
            }
            notifyDataSetChanged();
        }
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

    private void triggerAddControlRemoveAnimation(int pos, boolean removeOnly) {
        if (!isEmptyList()) {
            //eliminate now items above and below
            mFlattenedMenuItems.remove(pos + 1);
            notifyItemRemoved(pos + 1);
            mFlattenedMenuItems.remove(pos + 1);
            notifyItemRemoved(pos + 1);
            mFlattenedMenuItems.remove(pos - 1);
            notifyItemRemoved(pos - 1);
        }

        if (!removeOnly) {
            Handler hdlr = new Handler();
            hdlr.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            }, 350);
        }
    }

    private ItemAddPosition getWhereToAddMenuItem(int posOrigin, int posAdderControl) {
        if (isEmptyList()) {
            return ItemAddPosition.ZERO;
        } else if (posAdderControl == posOrigin - 1) {
            return ItemAddPosition.ABOVE;
        }
        else if (posAdderControl == posOrigin + 1) {
            return ItemAddPosition.BELOW;
        } else if (posAdderControl == posOrigin + 2) {
            return ItemAddPosition.TO_CHILDREN;
        } else { //default: below (should never reach here though)
            return ItemAddPosition.BELOW;
        }
    }

    public List<MenuItemModel> getCurrentMenuItems(){
        return mFlattenedMenuItems;
    }

    private class AddItem extends MenuItemModel {
        public AddItem(long id, String name) {
            this.itemId = id;
            this.name = name;
        }
    }

    public static class MenuItemHolder extends RecyclerView.ViewHolder {
        private final TextView txtTitle;
        private final ImageView imgMenuItemType;
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
            imgAddIcon = (ImageView) view.findViewById(R.id.icon_add);
            imgArrowLeft = (ImageView) view.findViewById(R.id.arrow_left);
            imgArrowRight = (ImageView) view.findViewById(R.id.arrow_right);
            btnCancelAdd = (WPButton) view.findViewById(R.id.cancel_button);
            containerCancel = (ViewGroup) view.findViewById(R.id.layout_cancel_button);
            containerButtons = (ViewGroup) view.findViewById(R.id.layout_buttons);
        }
    }

    public static class AddItemHolder extends RecyclerView.ViewHolder {
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
}
