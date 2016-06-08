package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.models.MenuItemModel;

/**
 */
public class TestimonialItemEditor extends BaseMenuItemEditor {
    @Override
    public int getIconDrawable() {
        return 0;
    }

    @Override
    public View getEditView(Context context, ViewGroup root) {
        return null;
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = new MenuItemModel();
        return menuItem;
    }

    @Override
    public void onSave() {
    }

    @Override
    public void onDelete() {
    }
}
