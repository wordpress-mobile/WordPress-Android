package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.widget.LinearLayout;

import org.wordpress.android.models.MenuItemModel;

/**
 */
public abstract class BaseMenuItemEditor extends LinearLayout {
    public abstract int getLayoutRes();
    public abstract MenuItemModel getMenuItem();
    public abstract void onSave();
    public abstract void onDelete();

    public BaseMenuItemEditor(Context context) {
        super(context);
    }
}
