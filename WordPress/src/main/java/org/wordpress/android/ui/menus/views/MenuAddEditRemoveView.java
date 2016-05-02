package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.widgets.WPEditText;
import org.wordpress.android.widgets.WPTextView;


/**
 * Menu add/remove control used in menus editing
 */
public class MenuAddEditRemoveView extends LinearLayout {
    private LinearLayout mMenuInactiveStateView;
    private WPTextView mMenuInactiveTitleText;
    private WPEditText mMenuEditText;
    private WPTextView mMenuRemove;
    private Button mMenuSave;
    private boolean mIsActive;

    private MenuModel mCurrentMenu;

    private  MenuAddEditRemoveActionListener mActionListener;

    public interface MenuAddEditRemoveActionListener {
        void onMenuCreate(MenuModel menu);
        void onMenuDelete(MenuModel menu);
        void onMenuUpdate(MenuModel menu);
    }

    public MenuAddEditRemoveView(Context context){
        super(context);
        initView(context);
    }

    public MenuAddEditRemoveView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public MenuAddEditRemoveView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.menus_menu_add_edit_remove, this);
        mMenuInactiveStateView = (LinearLayout) findViewById(R.id.menu_title_inactive_state);
        mMenuInactiveTitleText = (WPTextView) findViewById(R.id.menu_title_tv);
        mMenuEditText = (WPEditText) findViewById(R.id.menu_title_edit);
        mMenuRemove = (WPTextView) findViewById(R.id.menu_remove);
        mMenuSave = (Button) findViewById(R.id.menu_save);

        setupVisibilityBehavior();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mMenuInactiveStateView.setEnabled(enabled);
        mMenuInactiveTitleText.setEnabled(enabled);
        mMenuEditText.setEnabled(enabled);
        mMenuRemove.setEnabled(enabled);
        mMenuSave.setEnabled(enabled);
    }

    private void setupVisibilityBehavior(){
        //initialize in inactive state
        setActive(false);

        mMenuInactiveStateView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setActive(true);
            }
        });

        mMenuEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (v.getId() == R.id.menu_title_edit && !hasFocus) {
                    //the edit text lost the focus, so we need to bring the inactive view to the front again
                    setActive(false);
                }
            }
        });

        mMenuEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    mMenuSave.setEnabled(true);
                } else {
                    mMenuSave.setEnabled(false);
                }
            }
        });

        mMenuRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setActive(false);
                if (mActionListener != null) {
                    if (mCurrentMenu != null) {
                        mActionListener.onMenuDelete(mCurrentMenu);
                        setMenu(null, false);
                    } else {
                        // in case this is a new menu (i.e. not really editing) we don't call the listener
                        // - we just clear the text and set inactive
                        mMenuInactiveTitleText.setText(null);
                        mMenuEditText.setText(null);
                        setActive(false);
                    }
                }
            }
        });

        mMenuSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String menuTitle = mMenuEditText.getText().toString();
                if (TextUtils.isEmpty(menuTitle)){
                    // this place shouldn't be reached as emptiness is being checked in the onTextChangedListener,
                    // but otherwise:
                    // TODO show a snackbar indicating you need to enter a menu title in order to create a menu
                } else {
                    mMenuInactiveTitleText.setText(menuTitle);
                    setActive(false);
                    if (mActionListener != null){
                        if (mCurrentMenu == null){
                            MenuModel menu = new MenuModel();
                            menu.name = menuTitle;
                            mActionListener.onMenuCreate(menu);
                        } else {
                            mActionListener.onMenuUpdate(mCurrentMenu);
                        }
                    }
                }
            }
        });
    }

    // called when the user clicks on the menu name control to start editing/entering the name
    public void setActive(boolean active){
        this.mIsActive = active;

        if (active) {
            mMenuInactiveStateView.setVisibility(View.GONE);
            mMenuEditText.setVisibility(View.VISIBLE);
            mMenuEditText.selectAll();
            mMenuEditText.requestFocus();
        } else {
            mMenuInactiveStateView.setVisibility(View.VISIBLE);
            mMenuEditText.setVisibility(View.GONE);
        }
    }

    public boolean isActive(){
        return this.mIsActive;
    }

    public void setMenu(MenuModel menu, boolean isDefault){
        this.mCurrentMenu = menu;
        if (menu != null){
            mMenuEditText.setText(menu.name);
            mMenuInactiveTitleText.setText(menu.name);
            
            //save button is enabled only if a menu name is present
            if (!TextUtils.isEmpty(menu.name)) {
                mMenuSave.setEnabled(true);
            } else {
                mMenuSave.setEnabled(false);
            }

        } else {
            mMenuEditText.setText(null);
            mMenuInactiveTitleText.setText(null);
            mMenuSave.setEnabled(false);
        }

        if (isDefault){
            mMenuRemove.setVisibility(View.GONE);
        } else {
            mMenuRemove.setVisibility(View.VISIBLE);
        }

        setActive(false);
    }

    public MenuModel getMenu() {
        return this.mCurrentMenu;
    }

    //set a listener to listen for SAVE and REMOVE buttons
    public void setMenuActionListener(MenuAddEditRemoveActionListener listener){
        this.mActionListener = listener;
    }


}