package org.wordpress.android.ui.menus;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.ViewFlipper;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.ui.menus.items.BaseMenuItemEditor;
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory;
import org.wordpress.android.ui.menus.items.MenuItemEditorFactory.ITEM_TYPE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class EditMenuItemDialog extends DialogFragment implements Toolbar.OnMenuItemClickListener {
    public static final int EDIT_REQUEST_CODE = 10001;

    public static final int SAVE_RESULT_CODE = 1;
    public static final int NOT_SAVED_CODE = 2;

    public static final String EDITED_ITEM_KEY = "edited-item";

    private static final String ITEM_TYPES_ARG = "menu-item-types";
    private static final String ORIGINAL_ITEM_ARG = "original-menu-item";
    private static final String WORKING_ITEM_ARG = "working-menu-item";

    private static final ITEM_TYPE DEFAULT_ITEM_TYPE = ITEM_TYPE.POST;

    public static EditMenuItemDialog newInstance(MenuItemModel menuItem, ArrayList<MenuItemEditorFactory.ITEM_TYPE> types) {
        EditMenuItemDialog dialog = new EditMenuItemDialog();
        Bundle args = new Bundle();
        addToBundle(args, ITEM_TYPES_ARG, types);
        addToBundle(args, ORIGINAL_ITEM_ARG, menuItem);
        dialog.setArguments(args);
        return dialog;
    }

    private static void addToBundle(Bundle bundle, String key, Serializable item) {
        if (bundle != null && !TextUtils.isEmpty(key) && item != null) {
            bundle.putSerializable(key, item);
        }
    }

    private MenuItemModel mOriginalItem;
    private MenuItemModel mWorkingItem;
    private List<MenuItemEditorFactory.ITEM_TYPE> mItemTypes = new ArrayList<>();
    private List<String> mItemTypeNames = new ArrayList<>();
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
        if (args != null) {
            if (args.containsKey(ORIGINAL_ITEM_ARG)) {
                mOriginalItem = (MenuItemModel) args.getSerializable(ORIGINAL_ITEM_ARG);
            }

            if (args.containsKey(ITEM_TYPES_ARG)) {
                ArrayList<MenuItemEditorFactory.ITEM_TYPE> types = (ArrayList<MenuItemEditorFactory.ITEM_TYPE>) args.getSerializable(ITEM_TYPES_ARG);
                if (types != null) {
                    setTypes(types);
                }
            }

            if (args.containsKey(WORKING_ITEM_ARG)) {
                mWorkingItem = (MenuItemModel) args.getSerializable(WORKING_ITEM_ARG);
            }
        }

        if (mWorkingItem == null) {
            mWorkingItem = new MenuItemModel(mOriginalItem);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new Dialog(getActivity(), getTheme()){
            @Override
            public void onBackPressed() {
                if (mCurrentEditor != null){
                    if (mCurrentEditor.isDirty()) {
                        showAlertDialog();
                    } else {
                        sendResultToTarget(NOT_SAVED_CODE, null);
                        super.onBackPressed();
                    }
                } else {
                    sendResultToTarget(NOT_SAVED_CODE, null);
                    super.onBackPressed();
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.menu_item_edit_view, container, false);
        mTypePicker = (Spinner) dialogView.findViewById(R.id.menu_item_type_spinner);
        mEditorFlipper = (ViewFlipper) dialogView.findViewById(R.id.menu_item_editor_flipper);
        mToolbar = (Toolbar) dialogView.findViewById(R.id.toolbar);

        setupToolbar();
        setupTypePicker();
        setType(isCreating() ? DEFAULT_ITEM_TYPE.name() : mOriginalItem.type);

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
        if (item.getItemId() == R.id.menu_item_editor_confirm) {
            if (isCreating() || !mOriginalItem.equals(mCurrentEditor.getMenuItem())) {
                sendResultToTarget(SAVE_RESULT_CODE, mCurrentEditor.getMenuItem());
            }
            dismiss();
            return true;
        }
        return false;
    }

    public void setTypes(@NonNull List<MenuItemEditorFactory.ITEM_TYPE> types) {
        if (types.equals(mItemTypes)) return;
        mItemTypes.clear();
        mItemTypes.addAll(types);

        mItemTypeNames.clear();
        for (MenuItemEditorFactory.ITEM_TYPE type : mItemTypes) {
            mItemTypeNames.add(MenuItemEditorFactory.ITEM_TYPE.nameForItemType(getActivity(), type));
        }
    }

    private void sendResultToTarget(int resultCode, MenuItemModel item) {
        Fragment fragment = getTargetFragment();
        if (fragment == null) return;

        Intent data = new Intent();
        if (item != null) {
            data.putExtra(EDITED_ITEM_KEY, item);
        }
        fragment.onActivityResult(getTargetRequestCode(), resultCode, data);
    }

    public boolean isCreating() {
        return mOriginalItem == null;
    }

    public void setType(String type) {
        mWorkingItem.type = type;
        mWorkingItem.calculateCustomType();
        mToolbar.setTitle(mWorkingItem.name);
        setPickerAndChildViewSelection(mWorkingItem.calculatedType.toUpperCase());
        mCurrentEditor = (BaseMenuItemEditor) mEditorFlipper.getCurrentView();
        if (mCurrentEditor != null) {
            mCurrentEditor.setMenuItem(mWorkingItem);
            mCurrentEditor.setMenuItemNameChangeListener(new BaseMenuItemEditor.MenuItemNameChangeListener() {
                @Override
                public void onNameChanged(String newName) {
                    mToolbar.setTitle(newName);
                }
            });
        }
    }

    private void fillViewFlipper() {
        if (mEditorFlipper == null || !isAdded()) return;

        // add editor views
        for (int i = 0; i < mTypePicker.getCount(); ++i) {
            ITEM_TYPE itemType = mItemTypes.get(i);
            BaseMenuItemEditor editor = MenuItemEditorFactory.getEditor(getActivity(), itemType);
            if (editor != null) {
                mEditorFlipper.addView(editor);
                mItemPositions.put(itemType.name().toUpperCase(), i);
            }
        }
    }

    private void setupToolbar() {
        if (mToolbar == null || !isAdded()) return;

        // add back arrow that dismisses dialog without saving
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (mCurrentEditor != null){
                    if (mCurrentEditor.isDirty()) {
                        showAlertDialog();
                    } else {
                        sendResultToTarget(NOT_SAVED_CODE, null);
                        dismiss();
                    }
                } else {
                    sendResultToTarget(NOT_SAVED_CODE, null);
                    dismiss();
                }
            }
        });
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);

        mToolbar.setTitle(mWorkingItem.name);

        // add options menu
        mToolbar.inflateMenu(R.menu.menu_item_editor);
        mToolbar.setOnMenuItemClickListener(this);
    }

    private void setupTypePicker() {
        if (mTypePicker == null || !isAdded()) return;

        //get names to be shown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, mItemTypeNames);
        mTypePicker.setAdapter(adapter);

        fillViewFlipper();
        setPickerAndChildViewSelection(mWorkingItem.calculatedType.toUpperCase());
        mTypePicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ITEM_TYPE itemType = mItemTypes.get(position);
                setType(itemType.name().toUpperCase());
            }

            // no-op
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setPickerAndChildViewSelection(String type) {
        Integer itemIdx = mItemPositions.get(type);
        mTypePicker.setSelection(itemIdx);
        mEditorFlipper.setDisplayedChild(itemIdx);
    }

    private void showAlertDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
                getActivity());
        dialogBuilder.setTitle(getResources().getText(R.string.menu_item_changes_not_saved_title));
        dialogBuilder.setMessage(getResources().getText(R.string.menu_item_changes_not_saved));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //go back
                        sendResultToTarget(NOT_SAVED_CODE, null);
                        EditMenuItemDialog.this.dismiss();
                    }
                });
        dialogBuilder.setNegativeButton(
                getResources().getText(R.string.no),
                null);
        dialogBuilder.setCancelable(true);
        dialogBuilder.create().show();

    }

}
