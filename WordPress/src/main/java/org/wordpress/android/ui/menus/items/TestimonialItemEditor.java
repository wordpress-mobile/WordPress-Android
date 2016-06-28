package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 */
public class TestimonialItemEditor extends BaseMenuItemEditor {
    public TestimonialItemEditor(Context context) {
        this(context, null);
    }

    public TestimonialItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.page_menu_item_edit_view;
    }

    @Override
    public int getNameEditTextRes() {
        return R.id.menu_item_title_edit;
    }
}
