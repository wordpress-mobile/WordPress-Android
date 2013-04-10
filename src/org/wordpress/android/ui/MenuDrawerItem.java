package org.wordpress.android.ui;

import android.view.View;
import android.content.Intent;

abstract class MenuDrawerItem {
    public static int NO_ITEM_ID = -1;
    
    /**
     * When menu item is selected this method is called. Returns the intent that will be started.
     */
    abstract public Intent onSelectItem();
    /**
     * Determines if the menu item should be displayed in the menu
     */
    public Boolean isVisible(){
        return true;
    };
    /**
     * Determines if the item 
     */
    public Boolean isSelected(){
        return false;
    }
    /**
     * Method to allow the menu item to provide additional configuration to the view
     */
    public void onConfigureView(View view){};
    
    // Resource id for the title string
    protected int mTitle;
    // Resource id for the icon drawable
    protected int mIconRes;
    // ID for the item for remembering which item was selected
    private int mItemId;
    /**
     * Creates a MenuDrawerItem with the specific id, string resource id and drawable resource id
     */
    MenuDrawerItem(int itemId, int stringRes, int iconRes) {
        mTitle = stringRes;
        mIconRes = iconRes;
        mItemId = itemId;
    }
    /**
     * Creates a MenuDrawerItem with NO_ITEM_ID for it's id for items that shouldn't be remembered
     * between application launches.
     */
    MenuDrawerItem(int stringRes, int iconRes){
        this(NO_ITEM_ID, stringRes, iconRes);
    }
    public boolean hasItemId(){
        return getItemId() != NO_ITEM_ID;
    }
    public int getItemId(){
        return mItemId;
    }
    public String toString(){
        return "";
    }

    public int getTitleRes(){
        return mTitle;
    }

    public int getIconRes(){
        return mIconRes;
    }

    public Intent selectItem(){
        return onSelectItem();
    }

    public void configureView(View v){
        // By default do nothing
        onConfigureView(v);
    }

}
