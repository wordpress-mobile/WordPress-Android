package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.ui.menus.items.BaseMenuItemEditor;
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory;

/**
 */
public class MenuItemEditView extends LinearLayout {
    public interface MenuItemEditorListener {
        void onEditorShown();
        void onEditorHidden();
        void onMenuItemAdded(MenuItemModel menuItem);
        void onMenuItemChanged(MenuItemModel menuItem);
    }

    private MenuItemEditorFactory.ITEM_TYPE mType;
    private MenuItemEditorListener mListener;
    private BaseMenuItemEditor mCurrentEditor;

    private Spinner mTypePicker;
    private Button mAddButton;
    private Button mCancelButton;
    private View mEditView;

    public MenuItemEditView(Context context, MenuItemEditorFactory.ITEM_TYPE type) {
        super(context);
        initView();
        setType(type);
    }

    public MenuItemEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        setType(MenuItemEditorFactory.ITEM_TYPE.NULL);
    }

    public void setListener(MenuItemEditorListener listener) {
        mListener = listener;
    }

    private void initView() {
        inflate(getContext(), R.layout.menu_item_edit_view, this);
        mTypePicker = (Spinner) findViewById(R.id.menu_item_type_spinner);
        mAddButton = (Button) findViewById(R.id.menu_item_edit_add);
        mCancelButton = (Button) findViewById(R.id.menu_item_edit_cancel);
        mEditView = findViewById(R.id.menu_item_edit_view_stub);

        mTypePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // TODO: set type and update editor view
                MenuItemEditorFactory.ITEM_TYPE type = MenuItemEditorFactory.ITEM_TYPE.typeForString(mTypePicker.getSelectedItem().toString());
                setType(type);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });

        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setType(MenuItemEditorFactory.ITEM_TYPE.NULL);
            }
        });
        mAddButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: add menu item to current menu
                notifyAdded(null);
            }
        });
    }

    public void refreshView() {
        switch (mType) {
            case NULL:
                setVisibility(View.GONE);
                notifyHidden();
                break;
            case PAGE:
                setVisibility(View.VISIBLE);
                notifyShown();
                break;
        }
    }

    public void setType(MenuItemEditorFactory.ITEM_TYPE type) {
        mType = type;
        mCurrentEditor = MenuItemEditorFactory.getEditor(type);
        refreshView();
    }

    //
    // Listener callbacks
    //
    private void notifyShown() {
        if (mListener != null) mListener.onEditorShown();
    }

    private void notifyHidden() {
        if (mListener != null) mListener.onEditorHidden();
    }

    private void notifyAdded(MenuItemModel menuItem) {
        if (mListener != null) mListener.onMenuItemAdded(menuItem);
    }

    private void notifyChanged(MenuItemModel menuItem) {
        if (mListener != null) mListener.onMenuItemChanged(menuItem);
    }
}
