package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.models.MenuItemModel;

/**
 */
public abstract class BaseMenuItemEditor {
    public abstract int getIconDrawable();
    public abstract View getEditView(Context context, ViewGroup root);
    public abstract MenuItemModel getMenuItem();
    public abstract void onSave();
    public abstract void onDelete();
}
