package org.wordpress.android.ui.posts;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.databinding.PostSettingsInputDialogBinding;
import org.wordpress.android.util.ActivityUtils;

public class PostSettingsInputDialogFragment extends DialogFragment implements TextWatcher {
    public static final String TAG = "post_settings_input_dialog_fragment";

    public interface PostSettingsInputDialogListener {
        void onInputUpdated(String input);
    }

    private static final String INPUT_TAG = "input";
    private static final String TITLE_TAG = "title";
    private static final String HINT_TAG = "hint";
    private static final String DISABLE_EMPTY_INPUT_TAG = "disable_empty_input";
    private static final String MULTILINE_INPUT_TAG = "is_multiline_input";

    private String mCurrentInput;
    private String mTitle;
    private String mHint;
    private boolean mDisableEmptyInput;
    private boolean mIsMultilineInput;
    private PostSettingsInputDialogListener mListener;
    private AlertDialog mDialog;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mCurrentInput = savedInstanceState.getString(INPUT_TAG, "");
            mTitle = savedInstanceState.getString(TITLE_TAG, "");
            mHint = savedInstanceState.getString(HINT_TAG, "");
            mDisableEmptyInput = savedInstanceState.getBoolean(DISABLE_EMPTY_INPUT_TAG, false);
            mIsMultilineInput = savedInstanceState.getBoolean(MULTILINE_INPUT_TAG, false);
        } else if (getArguments() != null) {
            mCurrentInput = getArguments().getString(INPUT_TAG, "");
            mTitle = getArguments().getString(TITLE_TAG, "");
            mHint = getArguments().getString(HINT_TAG, "");
            mDisableEmptyInput = getArguments().getBoolean(DISABLE_EMPTY_INPUT_TAG, false);
            mIsMultilineInput = getArguments().getBoolean(MULTILINE_INPUT_TAG, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(INPUT_TAG, mCurrentInput);
        outState.putSerializable(TITLE_TAG, mTitle);
        outState.putSerializable(HINT_TAG, mHint);
        outState.putBoolean(DISABLE_EMPTY_INPUT_TAG, mDisableEmptyInput);
        outState.putBoolean(MULTILINE_INPUT_TAG, mIsMultilineInput);
    }

    public static PostSettingsInputDialogFragment newInstance(String currentText, String title, String hint,
                                                              boolean disableEmptyInput, boolean isMultiline) {
        PostSettingsInputDialogFragment dialogFragment = new PostSettingsInputDialogFragment();
        Bundle args = new Bundle();
        args.putString(INPUT_TAG, currentText);
        args.putString(TITLE_TAG, title);
        args.putString(HINT_TAG, hint);
        args.putBoolean(DISABLE_EMPTY_INPUT_TAG, disableEmptyInput);
        args.putBoolean(MULTILINE_INPUT_TAG, isMultiline);
        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        ActivityUtils.hideKeyboard(getActivity());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder =
                new MaterialAlertDialogBuilder(new ContextThemeWrapper(getActivity(), R.style.PostSettingsTheme));
        LayoutInflater layoutInflater = requireActivity().getLayoutInflater();
        //noinspection InflateParams
        PostSettingsInputDialogBinding binding =
                PostSettingsInputDialogBinding.inflate(layoutInflater, null, false);
        builder.setView(binding.getRoot());
        if (mIsMultilineInput) {
            binding.postSettingsInputDialogEditText.setRawInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        } else {
            binding.postSettingsInputDialogEditText.setInputType(InputType.TYPE_CLASS_TEXT);
        }
        if (!TextUtils.isEmpty(mCurrentInput)) {
            binding.postSettingsInputDialogEditText.setText(mCurrentInput);
            // move the cursor to the end
            binding.postSettingsInputDialogEditText.setSelection(mCurrentInput.length());
        }
        binding.postSettingsInputDialogEditText.addTextChangedListener(this);

        binding.postSettingsInputDialogInputLayout.setHint(mTitle);

        binding.postSettingsInputDialogHint.setText(mHint);

        builder.setNegativeButton(R.string.cancel, null);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Editable text = binding.postSettingsInputDialogEditText.getText();
                if (mListener != null && text != null) {
                    mCurrentInput = text.toString();
                    mListener.onInputUpdated(mCurrentInput);
                }
            }
        });

        mDialog = builder.create();
        return mDialog;
    }

    public void setPostSettingsInputDialogListener(PostSettingsInputDialogListener listener) {
        mListener = listener;
    }


    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // no-op
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        // no-op
    }

    @Override
    public void afterTextChanged(Editable editable) {
        if (mDialog != null) {
            boolean disabled = mDisableEmptyInput && TextUtils.isEmpty(editable);
            mDialog.getButton(Dialog.BUTTON_POSITIVE).setEnabled(!disabled);
        }
    }
}
