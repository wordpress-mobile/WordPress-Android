package org.wordpress.android.ui.menus;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.ViewFlipper;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.ui.menus.items.BaseMenuItemEditor;
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory;
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory.ITEM_TYPE;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class EditMenuItemDialog extends DialogFragment implements Toolbar.OnMenuItemClickListener {
    private static final String ORIGINAL_ITEM_ARG = "original-menu-item";
    private static final String WORKING_ITEM_ARG = "working-menu-item";

    private static final String DEFAULT_ITEM_TYPE = ITEM_TYPE.POST.name();

    public static EditMenuItemDialog newInstance(MenuItemModel menuItem) {
        EditMenuItemDialog dialog = new EditMenuItemDialog();
        Bundle args = new Bundle();
        addToBundle(args, ORIGINAL_ITEM_ARG, menuItem);
        dialog.setArguments(args);
        return dialog;
    }

    private static void addToBundle(Bundle bundle, String key, MenuItemModel item) {
        if (bundle != null && !TextUtils.isEmpty(key) && item != null) {
            bundle.putSerializable(key, item);
        }
    }

    private MenuItemModel mOriginalItem;
    private MenuItemModel mWorkingItem;
    private Map<String, Integer> mItemPositions = new HashMap<>();

    // Views
    private BaseMenuItemEditor mCurrentEditor;
    private ViewFlipper mEditorFlipper;
    private Toolbar mToolbar;
    private Spinner mTypePicker;

    public EditMenuItemDialog() {
        // empty constructor required
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, R.style.CalypsoTheme);

        Bundle args = getArguments();
        if (args != null && args.containsKey(ORIGINAL_ITEM_ARG)) {
            mOriginalItem = (MenuItemModel) args.getSerializable(ORIGINAL_ITEM_ARG);
        }
        mWorkingItem = new MenuItemModel(mOriginalItem);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.menu_item_edit_view, container, false);
        mTypePicker = (Spinner) dialogView.findViewById(R.id.menu_item_type_spinner);
        mEditorFlipper = (ViewFlipper) dialogView.findViewById(R.id.menu_item_editor_flipper);
        mToolbar = (Toolbar) dialogView.findViewById(R.id.toolbar);

        setupToolbar();
        fillViewFlipper();
        setupTypePicker();
        setType(isCreating() ? DEFAULT_ITEM_TYPE : mOriginalItem.type);

        return dialogView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        addToBundle(outState, ORIGINAL_ITEM_ARG, mOriginalItem);
        addToBundle(outState, WORKING_ITEM_ARG, mWorkingItem);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.save_item) {
            mCurrentEditor.onSave();
            return true;
        } else if (item.getItemId() == R.id.delete_item) {
            mCurrentEditor.onDelete();
            return true;
        }
        return false;
    }

    public boolean isCreating() {
        return mOriginalItem == null;
    }

    public void setType(String type) {
        mWorkingItem.type = type;
        mToolbar.setTitle(mWorkingItem.name);
        mTypePicker.setSelection(mItemPositions.get(type.toUpperCase()));
        mEditorFlipper.setDisplayedChild(mItemPositions.get(type.toUpperCase()));
        mCurrentEditor = (BaseMenuItemEditor) mEditorFlipper.getCurrentView();
        mCurrentEditor.setMenuItem(mWorkingItem);
    }

    private void fillViewFlipper() {
        if (mEditorFlipper == null || !isAdded()) return;

        // add editor views
        for (int i = 0; i < mTypePicker.getCount(); ++i) {
            String type = mTypePicker.getItemAtPosition(i).toString();
            ITEM_TYPE itemType = ITEM_TYPE.typeForString(type);
            BaseMenuItemEditor editor = MenuItemEditorFactory.getEditor(getActivity(), itemType);
            if (editor != null) {
                mEditorFlipper.addView(editor);
                mItemPositions.put(type.toUpperCase(), i);
            }
        }
    }

    private void setupToolbar() {
        if (mToolbar == null || !isAdded()) return;

        // add back arrow that dismisses dialog without saving
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss(); }
        });
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);

        mToolbar.setTitle(mWorkingItem.name);

        // add options menu
        mToolbar.inflateMenu(R.menu.menu_item_editor);
        mToolbar.setOnMenuItemClickListener(this);
    }

    private void setupTypePicker() {
        if (mTypePicker == null || !isAdded()) return;

        mTypePicker.setSelection(mItemPositions.get(mWorkingItem.type.toUpperCase()));
        mTypePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setType(mTypePicker.getItemAtPosition(position).toString());
            }

            // no-op
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }
}
