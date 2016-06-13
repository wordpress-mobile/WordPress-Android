package org.wordpress.android.ui.menus.items;

import android.content.Context;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 */
public class TagItemEditor extends BaseMenuItemEditor {
    public TagItemEditor(Context context) {
        super(context);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.tag_menu_item_edit_view;
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
