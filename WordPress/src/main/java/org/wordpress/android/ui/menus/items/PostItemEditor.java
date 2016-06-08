package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 */
public class PostItemEditor extends BaseMenuItemEditor {
    @Override
    public int getIconDrawable() {
        return R.drawable.my_site_icon_posts;
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
