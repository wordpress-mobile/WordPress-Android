package org.wordpress.android.ui.menus.items;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.widgets.WPEditText;

/**
 */
public abstract class BaseMenuItemEditor extends LinearLayout {
    public abstract int getLayoutRes();
    public abstract int getNameEditTextRes();
    public abstract void onSave();
    public abstract void onDelete();

    protected MenuItemModel mWorkingItem;
    protected MenuItemNameChangeListener mMenuItemNameChangeListener;
    private WPEditText mItemNameEditText;

    public interface MenuItemNameChangeListener {
        void onNameChanged(String newName);
    }

    public BaseMenuItemEditor(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setMenuItem(MenuItemModel menuItem) {
        mWorkingItem = menuItem;
        if (mItemNameEditText != null) {
            mItemNameEditText.setText(mWorkingItem.name);
        }
    }

    public MenuItemModel getMenuItem() {
        return mWorkingItem;
    }

    public void setMenuItemNameChangeListener(MenuItemNameChangeListener listener){
        mMenuItemNameChangeListener = listener;
    }

    protected void init() {
        inflate(getContext(), getLayoutRes(), this);

        int editTextResId = getNameEditTextRes();
        mItemNameEditText = (WPEditText) findViewById(editTextResId);
        if (mItemNameEditText != null) {
            mItemNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    mWorkingItem.name = editable.toString();
                    if (mMenuItemNameChangeListener != null) {
                        mMenuItemNameChangeListener.onNameChanged(mWorkingItem.name);
                    }
                }
            });
        }

    }
}
