package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;

public class PostSettingsListDialogFragment extends DialogFragment {
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_CHECKED_INDEX = "checked_index";
    private static final String ARG_ITEMS = "items";

    public static final String TAG = "post_list_settings_dialog_fragment";

    enum DialogType {
        HOMEPAGE_STATUS,
        POST_STATUS,
        AUTHOR,
        POST_FORMAT
    }

    interface OnPostSettingsDialogFragmentListener {
        void onPostSettingsFragmentPositiveButtonClicked(@NonNull PostSettingsListDialogFragment fragment);
    }

    private DialogType mDialogType;
    private String[] mItems;
    private int mCheckedIndex;
    private OnPostSettingsDialogFragmentListener mListener;

    public static PostSettingsListDialogFragment newInstance(@NonNull DialogType dialogType, int index) {
        return newInstance(dialogType, index, null);
    }

    public static PostSettingsListDialogFragment newInstance(
            @NonNull DialogType dialogType,
            int index,
            String[] items
    ) {
        PostSettingsListDialogFragment fragment = new PostSettingsListDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DIALOG_TYPE, dialogType);
        args.putInt(ARG_CHECKED_INDEX, index);
        args.putStringArray(ARG_ITEMS, items);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mDialogType = (DialogType) args.getSerializable(ARG_DIALOG_TYPE);
        mCheckedIndex = args.getInt(ARG_CHECKED_INDEX);
        mItems = args.getStringArray(ARG_ITEMS);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPostSettingsDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPostSettingsDialogFragmentListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new MaterialAlertDialogBuilder(new ContextThemeWrapper(getActivity(), R.style.PostSettingsTheme));

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCheckedIndex = which;
                getArguments().putInt(ARG_CHECKED_INDEX, mCheckedIndex);
            }
        };

        switch (mDialogType) {
            case HOMEPAGE_STATUS:
                builder.setTitle(R.string.post_settings_status);
                builder.setSingleChoiceItems(
                        R.array.post_settings_homepage_statuses,
                        mCheckedIndex,
                        clickListener);
                break;
            case POST_STATUS:
                builder.setTitle(R.string.post_settings_status);
                builder.setSingleChoiceItems(
                        R.array.post_settings_statuses,
                        mCheckedIndex,
                        clickListener);
                break;
            case AUTHOR:
                builder.setTitle(R.string.post_settings_author);
                builder.setSingleChoiceItems(mItems, mCheckedIndex, clickListener);
                break;
            case POST_FORMAT:
                builder.setTitle(R.string.post_settings_post_format);
                builder.setSingleChoiceItems(
                        R.array.post_format_display_names,
                        mCheckedIndex,
                        clickListener);
                break;
        }

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mListener.onPostSettingsFragmentPositiveButtonClicked(PostSettingsListDialogFragment.this);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    public DialogType getDialogType() {
        return mDialogType;
    }

    public int getCheckedIndex() {
        return mCheckedIndex;
    }

    public @Nullable String getSelectedItem() {
        ListView listView = ((AlertDialog) getDialog()).getListView();
        if (listView != null) {
            try {
                return (String) listView.getItemAtPosition(mCheckedIndex);
            } catch (IndexOutOfBoundsException e) {
                AppLog.e(AppLog.T.POSTS, e);
            }
        }
        return null;
    }
}

