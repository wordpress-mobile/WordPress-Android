package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 */
public class PageItemEditor extends BaseMenuItemEditor {
    @Override
    public int getIconDrawable() {
        return R.drawable.my_site_icon_pages;
    }

    @Override
    public View getEditView(@NonNull Context context, ViewGroup root) {
        return null;
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = new MenuItemModel();
        return menuItem;
    }

    @Override
    public void onSave() {
        // TODO: save to DB
    }

    @Override
    public void onDelete() {
        // TODO: remove from DB
    }
}
