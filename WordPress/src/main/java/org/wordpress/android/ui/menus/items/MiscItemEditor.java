package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 */
public class MiscItemEditor extends BaseMenuItemEditor {
    public MiscItemEditor(Context context) {
        this(context, null);
    }

    public MiscItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.page_menu_item_edit_view;
    }

    @Override
    public MenuItemModel getMenuItem() {
        return null;
    }

    @Override
    public void onSave() {
    }

    @Override
    public void onDelete() {
    }
}
