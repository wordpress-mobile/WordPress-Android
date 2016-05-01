package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.widgets.WPEditText;
import org.wordpress.android.widgets.WPTextView;


/**
 * Menu add/remove control used in menus editing
 */
public class MenuAddEditRemoveView extends LinearLayout {
    private LinearLayout mMenuInactiveStateView;
    private TextView mMenuInactiveTitleText;
    private ImageButton mMenuInactiveEditButton;
    private WPEditText mMenuEditText;
    private ImageButton mMenuRemove;
    private Button mMenuSave;
    private boolean mIsActive;

    private MenuModel mCurrentMenu;
    private boolean mNewMenu;

    private  MenuAddEditRemoveActionListener mActionListener;

    public interface MenuAddEditRemoveActionListener {
        void onMenuCreated(MenuModel menu);
        void onMenuDeleted(MenuModel menu);
        void onMenuUpdated(MenuModel menu);
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
        mMenuInactiveEditButton = (ImageButton) findViewById(R.id.menu_title_edit_btn);
        mMenuEditText = (WPEditText) findViewById(R.id.menu_title_edit);
        mMenuRemove = (ImageButton) findViewById(R.id.menu_remove);
        mMenuSave = (Button) findViewById(R.id.menu_save);

        setupVisibilityBehavior();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mMenuInactiveStateView.setEnabled(enabled);
        mMenuInactiveTitleText.setEnabled(enabled);
        mMenuInactiveEditButton.setEnabled(enabled);
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

        mMenuRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null){
                    mActionListener.onMenuDeleted(mCurrentMenu);
                }
            }
        });

        mMenuSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mActionListener != null){
                    if (mCurrentMenu == null){
                        MenuModel menu = new MenuModel();
                        menu.name = mMenuEditText.getText().toString();
                        mActionListener.onMenuCreated(menu);
                    } else {
                        mActionListener.onMenuUpdated(mCurrentMenu);
                    }
                }
            }
        });
    }

    // called when the user clicks on
    public void setActive(boolean active){
        this.mIsActive = active;

        if (active) {
            mMenuInactiveStateView.setVisibility(View.GONE);
            mMenuEditText.setVisibility(View.VISIBLE);
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
            if (isDefault){
                mMenuRemove.setVisibility(View.GONE);
            } else {
                mMenuRemove.setVisibility(View.VISIBLE);
            }
        }
    }

    public MenuModel getMenu() {
        return this.mCurrentMenu;
    }

    //set a listener to listen for SAVE and REMOVE buttons
    public void setMenuActionListener(MenuAddEditRemoveActionListener listener){
        this.mActionListener = listener;
    }


}