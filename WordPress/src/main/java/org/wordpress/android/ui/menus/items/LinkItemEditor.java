package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;

/**
 */
public class LinkItemEditor extends BaseMenuItemEditor {
    public static final String NEW_TAB_LINK_TARGET = "_blank";

    private EditText mUrlEditText;
    private CheckBox mOpenNewTabCheckBox;

    public LinkItemEditor(Context context) {
        this(context, null);
    }

    public LinkItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        mUrlEditText = (EditText) child.findViewById(R.id.link_editor_url_input);
        mOpenNewTabCheckBox = (CheckBox) child.findViewById(R.id.link_editor_open_new_tab_checkbox);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.link_menu_item_edit_view;
    }

    @Override
    public int getNameEditTextRes() {
        return R.id.menu_item_title_edit;
    }

    @Override
    public MenuItemModel getMenuItem() {
        MenuItemModel menuItem = super.getMenuItem();
        fillData(menuItem);
        return menuItem;
    }

    @Override
    public void setMenuItem(MenuItemModel menuItem) {
        super.setMenuItem(menuItem);
        if (menuItem != null) {
            setUrl(menuItem.url);
            setOpenInNewTab(NEW_TAB_LINK_TARGET.equals(menuItem.linkTarget));
        }
    }

    public void setUrl(String url) {
        if (mUrlEditText != null) {
            mUrlEditText.setText(url);
        }
    }

    public void setOpenInNewTab(boolean shouldOpenNewTab) {
        if (mOpenNewTabCheckBox != null) {
            mOpenNewTabCheckBox.setChecked(shouldOpenNewTab);
        }
    }

    public String getUrl() {
        return mUrlEditText != null ? String.valueOf(mUrlEditText.getText()) : "";
    }

    public boolean shouldOpenNewTab() {
        return mOpenNewTabCheckBox != null && mOpenNewTabCheckBox.isChecked();
    }

    private void fillData(@NonNull MenuItemModel menuItem) {
        menuItem.type = MenuItemEditorFactory.ITEM_TYPE.CUSTOM.name().toLowerCase(); //default type: CUSTOM
        menuItem.typeFamily = "custom";
        menuItem.typeLabel = "Custom Link";
        menuItem.url = getUrl();
        menuItem.linkTarget = shouldOpenNewTab() ? NEW_TAB_LINK_TARGET : "";
    }
}
