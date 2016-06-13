package org.wordpress.android.ui.menus.items;

import android.content.Context;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 */
public class CategoryItemEditor extends BaseMenuItemEditor {
    public CategoryItemEditor(Context context) {
        super(context);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.category_menu_item_edit_view;
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
