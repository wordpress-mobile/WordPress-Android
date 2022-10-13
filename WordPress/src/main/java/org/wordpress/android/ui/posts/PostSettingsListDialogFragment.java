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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;

public class PostSettingsListDialogFragment extends DialogFragment {
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_CHECKED_INDEX = "checked_index";
    private static final String ARG_POST_AUTHOR_ID = "post_author_id";

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
    private int mCheckedIndex;
    private OnPostSettingsDialogFragmentListener mListener;
    private long mPostAuthorId;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    private EditPostPublishSettingsViewModel mPublishedViewModel;

    public static PostSettingsListDialogFragment newInstance(
            @NonNull DialogType dialogType,
            int index
    ) {
        return newInstance(dialogType, index, -1);
    }

    public static PostSettingsListDialogFragment newAuthorListInstance(long postAuthorId) {
        return newInstance(DialogType.AUTHOR, -1, postAuthorId);
    }

    private static PostSettingsListDialogFragment newInstance(
            @NonNull DialogType dialogType,
            int index,
            long postAuthorId
    ) {
        PostSettingsListDialogFragment fragment = new PostSettingsListDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_DIALOG_TYPE, dialogType);
        if (index >= 0) {
            args.putInt(ARG_CHECKED_INDEX, index);
        }
        if (postAuthorId > 0) {
            args.putLong(ARG_POST_AUTHOR_ID, postAuthorId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
        setCancelable(true);
        mPublishedViewModel = new ViewModelProvider(getActivity(), mViewModelFactory)
                .get(EditPostPublishSettingsViewModel.class);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mDialogType = (DialogType) args.getSerializable(ARG_DIALOG_TYPE);
        mCheckedIndex = args.getInt(ARG_CHECKED_INDEX);
        mPostAuthorId = args.getLong(ARG_POST_AUTHOR_ID);
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
                builder.setMessage(R.string.loading);
                mPublishedViewModel.getAuthors().observe(this, authors -> {
                    // Dismiss the loading dialog and show a new dialog with the list.
                    dismiss();

                    builder.setMessage(null);
                    String[] authorNames = authors.stream().map(Person::getDisplayName).toArray(String[]::new);
                    builder.setSingleChoiceItems(
                            authorNames,
                            mPublishedViewModel.getAuthorIndex(mPostAuthorId),
                            clickListener
                    ).create().show();
                });
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

