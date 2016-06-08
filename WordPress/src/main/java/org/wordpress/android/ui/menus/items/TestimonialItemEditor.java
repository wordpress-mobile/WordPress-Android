package org.wordpress.android.ui.menus.items;

import android.content.Context;

import org.wordpress.android.models.MenuItemModel;

/**
 */
public class TestimonialItemEditor extends BaseMenuItemEditor {
    public TestimonialItemEditor(Context context) {
        super(context);
    }

    @Override
    public int getIconDrawable() {
        return 0;
    }

    @Override
    public int getLayoutRes() {
        return 0;
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
