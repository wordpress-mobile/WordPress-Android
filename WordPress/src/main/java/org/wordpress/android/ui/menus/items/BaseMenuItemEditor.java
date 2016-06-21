package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.wordpress.android.models.MenuItemModel;

/**
 */
public abstract class BaseMenuItemEditor extends LinearLayout {
    public abstract int getLayoutRes();
    public abstract void onSave();
    public abstract void onDelete();

    protected MenuItemModel mWorkingItem;

    public BaseMenuItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setMenuItem(MenuItemModel menuItem) {
        mWorkingItem = menuItem;
    }

    public MenuItemModel getMenuItem() {
        return mWorkingItem;
    }

    protected void init() {
        inflate(getContext(), getLayoutRes(), this);
    }
}
