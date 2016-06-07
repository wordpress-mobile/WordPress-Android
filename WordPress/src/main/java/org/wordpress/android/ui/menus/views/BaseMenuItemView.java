package org.wordpress.android.ui.menus.views;

import android.view.View;

/**
 */
public abstract class BaseMenuItemView {
    public abstract int getIconDrawable();
    public abstract View getEditView();
    public abstract void onSave();
    public abstract void onDelete();
}
