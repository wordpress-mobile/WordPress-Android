package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.ListView;

import org.wordpress.android.R;

public class PostSettingsDialogFragment extends DialogFragment {
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_CHOICE_INDEX = "choice_index";
    public static final String TAG = "post_settings_dialog_fragment";

    enum DialogType {
        PUBLISH_DATE
    }

    interface OnPostSettingsDialogFragmentListener {
        void onPostSettingsFragmentPositiveButtonClicked(@NonNull PostSettingsDialogFragment fragment);
    }

    private DialogType mDialogTyoe;
    private int mChoiceIndex;
    private OnPostSettingsDialogFragmentListener mListener;

    public static PostSettingsDialogFragment newInstance(@NonNull DialogType dialogType) {
        return newInstance(dialogType, 0);
    }

    public static PostSettingsDialogFragment newInstance(@NonNull DialogType dialogType, int index) {
        PostSettingsDialogFragment fragment = new PostSettingsDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DIALOG_TYPE, dialogType);
        args.putInt(ARG_CHOICE_INDEX, index);
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
        mChoiceIndex = args.getInt(ARG_CHOICE_INDEX);
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

        switch (mDialogTyoe) {
            case PUBLISH_DATE:
                builder.setTitle(R.string.post_settings_status);
                builder.setSingleChoiceItems(R.array.post_settings_statuses, mChoiceIndex, null);
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        ListView listView = ((AlertDialog) dialog).getListView();
                        mChoiceIndex = listView.getCheckedItemPosition();
                        getArguments().putInt(ARG_CHOICE_INDEX, mChoiceIndex);
                        mListener.onPostSettingsFragmentPositiveButtonClicked(PostSettingsDialogFragment.this);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                break;
            default:
                return null;
        }

        return builder.create();
    }

    public DialogType getDialogTyoe() {
        return mDialogTyoe;
    }

    public int getChoiceIndex() {
        return mChoiceIndex;
    }
}

