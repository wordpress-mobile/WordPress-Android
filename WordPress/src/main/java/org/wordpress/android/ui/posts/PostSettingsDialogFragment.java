package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;

public class PostSettingsDialogFragment extends DialogFragment {
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_CHECKED_INDEX = "checked_index";
    public static final String TAG = "post_settings_dialog_fragment";

    enum DialogType {
        PUBLISH_DATE,
        POST_FORMAT
    }

    interface OnPostSettingsDialogFragmentListener {
        void onPostSettingsFragmentPositiveButtonClicked(@NonNull PostSettingsDialogFragment fragment);
    }

    private DialogType mDialogTyoe;
    private int mCheckedIndex;
    private OnPostSettingsDialogFragmentListener mListener;

    public static PostSettingsDialogFragment newInstance(@NonNull DialogType dialogType) {
        return newInstance(dialogType, 0);
    }

    public static PostSettingsDialogFragment newInstance(@NonNull DialogType dialogType, int index) {
        PostSettingsDialogFragment fragment = new PostSettingsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DIALOG_TYPE, dialogType);
        args.putInt(ARG_CHECKED_INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(true);
        int style = DialogFragment.STYLE_NORMAL, theme = 0;
        setStyle(style, theme);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mDialogTyoe = (DialogType) args.getSerializable(ARG_DIALOG_TYPE);
        mCheckedIndex = args.getInt(ARG_CHECKED_INDEX);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnPostSettingsDialogFragmentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPostSettingsDialogFragmentListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCheckedIndex = which;
                getArguments().putInt(ARG_CHECKED_INDEX, mCheckedIndex);
            }
        };

        switch (mDialogTyoe) {
            case PUBLISH_DATE:
                builder.setTitle(R.string.post_settings_status);
                builder.setSingleChoiceItems(
                        R.array.post_settings_statuses,
                        mCheckedIndex,
                        clickListener);
                break;
            case POST_FORMAT:
                builder.setTitle(R.string.post_settings_post_format);
                builder.setSingleChoiceItems(
                        R.array.post_format_display_names,
                        mCheckedIndex,
                        clickListener);
                break;
            default:
                return null;
        }

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mListener.onPostSettingsFragmentPositiveButtonClicked(PostSettingsDialogFragment.this);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);

        return builder.create();
    }

    public DialogType getDialogTyoe() {
        return mDialogTyoe;
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

