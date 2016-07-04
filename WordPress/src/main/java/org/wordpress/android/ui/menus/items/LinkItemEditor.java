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
    public static final String LINK_URL_KEY = "url";
    public static final String LINK_NEW_TAB_KEY = "open-new-tab";

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

        if (child.getId() == R.id.link_editor_url_input) {
            mUrlEditText = (EditText) child;
        } else if (child.getId() == R.id.link_editor_open_new_tab_checkbox) {
            mOpenNewTabCheckBox = (CheckBox) child;
        }
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
        MenuItemModel menuItem = new MenuItemModel();
        fillData(menuItem);
        return menuItem;
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
        if (mUrlEditText != null) {
            menuItem.addData(LINK_URL_KEY, String.valueOf(mUrlEditText.getText()));
        }
        if (mOpenNewTabCheckBox != null) {
            menuItem.addData(LINK_NEW_TAB_KEY, String.valueOf(mOpenNewTabCheckBox.isChecked()));
        }
    }
}
