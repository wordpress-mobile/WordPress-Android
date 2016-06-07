package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.wordpress.android.R;

/**
 */
public class MenuItemEditView extends LinearLayout {
    private MenuItemFactory.ITEM_TYPE mType;

    private Spinner mTypePicker;
    private Button mAddButton;
    private Button mCancelButton;
    private View mEditView;

    public MenuItemEditView(Context context, MenuItemFactory.ITEM_TYPE type) {
        super(context);
        initView();
        setType(type);
    }

    public MenuItemEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
        setType(MenuItemFactory.ITEM_TYPE.NULL);
    }

    private void initView() {
        inflate(getContext(), R.layout.menu_item_edit_view, this);
        mTypePicker = (Spinner) findViewById(R.id.menu_item_type_spinner);
        mAddButton = (Button) findViewById(R.id.menu_item_edit_add);
        mCancelButton = (Button) findViewById(R.id.menu_item_edit_cancel);
        mEditView = findViewById(R.id.menu_item_edit_view_stub);

        mCancelButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setType(MenuItemFactory.ITEM_TYPE.NULL);
            }
        });
    }

    public void refreshView() {
        switch (mType) {
            case NULL:
                setVisibility(View.GONE);
                break;
            case PAGE:
                setVisibility(View.VISIBLE);
                break;
        }
    }

    public void setType(MenuItemFactory.ITEM_TYPE type) {
        mType = type;
        refreshView();
    }
}
