package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Person;

import javax.inject.Inject;

public class PostSettingsListDialogFragment extends DialogFragment {
    private static final String ARG_DIALOG_TYPE = "dialog_type";
    private static final String ARG_CHECKED_INDEX = "checked_index";
    private static final String ARG_POST_AUTHOR_ID = "post_author_id";

    public static final String TAG = "post_list_settings_dialog_fragment";

    public enum DialogType {
        HOMEPAGE_STATUS,
        POST_STATUS,
        AUTHOR,
        POST_FORMAT
    }

    private DialogType mDialogType;
    private int mCheckedIndex;
    private long mPostAuthorId;
    @Nullable private String[] mDialogItems; // Store the actual items shown in the dialog
    @Inject ViewModelProvider.Factory mViewModelFactory;
    private EditPostPublishSettingsViewModel mPublishedViewModel;
    @Nullable private EditPostSettingsViewModel mSettingsViewModel;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
        setCancelable(true);
        mPublishedViewModel = new ViewModelProvider(getActivity(), mViewModelFactory)
                .get(EditPostPublishSettingsViewModel.class);
        mSettingsViewModel = new ViewModelProvider(getActivity(), mViewModelFactory)
                .get(EditPostSettingsViewModel.class);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mDialogType = (DialogType) args.getSerializable(ARG_DIALOG_TYPE);
        mCheckedIndex = args.getInt(ARG_CHECKED_INDEX);
        mPostAuthorId = args.getLong(ARG_POST_AUTHOR_ID);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new MaterialAlertDialogBuilder(new ContextThemeWrapper(getActivity(), R.style.PostSettingsTheme));

        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(@Nullable DialogInterface dialog, int which) {
                mCheckedIndex = which;
                if (getArguments() != null) {
                    getArguments().putInt(ARG_CHECKED_INDEX, mCheckedIndex);
                }
            }
        };

        switch (mDialogType) {
            case HOMEPAGE_STATUS:
                builder.setTitle(R.string.post_settings_status);
                mDialogItems = getResources().getStringArray(R.array.post_settings_homepage_statuses);
                builder.setSingleChoiceItems(
                        mDialogItems,
                        mCheckedIndex,
                        clickListener);
                break;
            case POST_STATUS:
                builder.setTitle(R.string.post_settings_status);
                mDialogItems = getResources().getStringArray(R.array.post_settings_statuses);
                builder.setSingleChoiceItems(
                        mDialogItems,
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
                    mDialogItems = authors.stream().map(Person::getDisplayName).toArray(String[]::new);
                    builder.setSingleChoiceItems(
                            mDialogItems,
                            mPublishedViewModel.getAuthorIndex(mPostAuthorId),
                            clickListener
                    );
                    setupPositiveButton(builder);
                    builder.setNegativeButton(R.string.cancel, null);
                    builder.create().show();
                });
                break;
            case POST_FORMAT:
                builder.setTitle(R.string.post_settings_post_format);
                mDialogItems = getResources().getStringArray(R.array.post_format_display_names);
                builder.setSingleChoiceItems(
                        mDialogItems,
                        mCheckedIndex,
                        clickListener);
                break;
        }

        // Only add buttons to dialogs that are ready for user interaction
        // AUTHOR dialog sets up buttons after authors load, but needs cancel button during loading
        if (mDialogType != DialogType.AUTHOR) {
            setupPositiveButton(builder);
            builder.setNegativeButton(R.string.cancel, null);
        } else {
            // Loading dialog should at least have cancel button for user to back out
            builder.setNegativeButton(R.string.cancel, null);
        }

        return builder.create();
    }

    private void setupPositiveButton(@NonNull Builder builder) {
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            if (mSettingsViewModel != null) {
                mSettingsViewModel.onDialogResult(
                    mDialogType,
                    mCheckedIndex,
                    getSelectedItem()
                );
            }
        });
    }

    public @Nullable String getSelectedItem() {
        if (mCheckedIndex < 0 || mDialogItems == null || mCheckedIndex >= mDialogItems.length) {
            return null;
        }
        return mDialogItems[mCheckedIndex];
    }
}

