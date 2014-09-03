/**
 * Represents a single item in the WPActionBarActivity's menu drawer. A MenuDrawerItem determines
 * the label and icon to use in the menu, its presence in the menu, its selection state, and the
 * action that happens when the item is selected.
 */
package org.wordpress.android.ui;

import android.view.View;

public abstract class MenuDrawerItem {
    /**
     * Called when the menu item is selected.
     */
    abstract public void onSelectItem();
    /**
     * Determines if the menu item should be displayed in the menu. Default is always true.
     */
    public Boolean isVisible(){
        return true;
    }
    /**
     * Determines if the item is selected. Default is always false.
     */
    public Boolean isSelected(){
        return false;
    }
    /**
     * Method to allow the menu item to provide additional configuration to the view, default
     * implementation does nothing.
     */
    public void onConfigureView(View view){};

    // Resource id for the title string
    protected int mTitle;
    // Resource id for the icon drawable
    protected int mIconRes;
    // ID for the item for remembering which item was selected
    private ActivityId mItemId;
    /**
     * Creates a MenuDrawerItem with the specific id, string resource id and drawable resource id
     */
    MenuDrawerItem(ActivityId itemId, int stringRes, int iconRes) {
        mTitle = stringRes;
        mIconRes = iconRes;
        mItemId = itemId;
    }
    /**
     * Creates a MenuDrawerItem with NO_ITEM_ID for it's id for items that shouldn't be remembered
     * between application launches.
     */
    MenuDrawerItem(int stringRes, int iconRes){
        this(ActivityId.UNKNOWN, stringRes, iconRes);
    }
    /**
     * Determines if the item has an id for remembering the last selected item
     */
    public boolean hasItemId(){
        return getItemId() != ActivityId.UNKNOWN;
    }
    /**
     * Get's the item's unique ID
     */
    public ActivityId getItemId(){
        return mItemId;
    }
    /**
     * Returns the item's string representation (used by ArrayAdapter.getView)
     */
    public String toString(){
        return "";
    }
    /**
     * The resource id to use for the menu item's title
     */
    public int getTitleRes(){
        return mTitle;
    }
    /**
     * The resource id to use for the menu item's icon
     */
    public int getIconRes(){
        return mIconRes;
    }

    public void selectItem(){
        onSelectItem();
    }
    /**
     * Allows the menu item to do additional manipulation to the view
     */
    public void configureView(View v){
        onConfigureView(v);
    }
}
