package org.wordpress.android.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.WPTextView;

public class TextInputDialogFragment extends DialogFragment {
    private static final String TITLE_TAG = "title";
    private static final String INITIAL_TEXT_TAG = "initial_text";
    private static final String HINT_TAG = "hint";
    private static final String IS_MULTILINE_TAG = "is_multiline";
    private static final String CALLBACK_ID_TAG = "callback_id";
    private static final String IS_INPUT_ENABLED = "is_input_enabled";

    public static final String TAG = "text_input_dialog_fragment";

    public int callbackId = -1;

    public static TextInputDialogFragment newInstance(String title,
                                                      String initialText,
                                                      String hint,
                                                      boolean isMultiline,
                                                      boolean isInputEnabled,
                                                      int callbackId) {
        TextInputDialogFragment textInputDialogFragment = new TextInputDialogFragment();
        Bundle args = new Bundle();

        args.putString(TITLE_TAG, title);
        args.putString(INITIAL_TEXT_TAG, initialText);
        args.putString(HINT_TAG, hint);
        args.putBoolean(IS_MULTILINE_TAG, isMultiline);
        args.putInt(CALLBACK_ID_TAG, callbackId);
        args.putBoolean(IS_INPUT_ENABLED, isInputEnabled);

        textInputDialogFragment.setArguments(args);
        return textInputDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        //noinspection InflateParams
        View promptView = layoutInflater.inflate(R.layout.text_input_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
        alertDialogBuilder.setView(promptView);

        final WPTextView textView = promptView.findViewById(R.id.text_input_dialog_label);
        final EditText editText = promptView.findViewById(R.id.text_input_dialog_input);
        final WPTextView hintView = promptView.findViewById(R.id.text_input_dialog_hint);

        Bundle args = getArguments();
        String title = args.getString(TITLE_TAG);
        String hint = args.getString(HINT_TAG);
        boolean isMultiline = args.getBoolean(IS_MULTILINE_TAG);
        String initialText = args.getString(INITIAL_TEXT_TAG);
        callbackId = args.getInt(CALLBACK_ID_TAG);

        textView.setText(title);
        if (!TextUtils.isEmpty(hint)) {
            hintView.setText(hint);
        } else {
            hintView.setVisibility(View.GONE);
        }

        if (!isMultiline) {
            editText.setMaxLines(1);
        }
        if (!TextUtils.isEmpty(initialText)) {
            editText.setText(initialText);
            editText.setSelection(0, initialText.length());
        }

        boolean isInputEnabled = args.getBoolean(IS_INPUT_ENABLED);
        editText.setEnabled(isInputEnabled);

        alertDialogBuilder.setCancelable(true)
                          .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                              if (getTargetFragment() instanceof Callback) {
                                  ((Callback) getTargetFragment())
                                          .onSuccessfulInput(editText.getText().toString(), callbackId);
                              } else {
                                  AppLog.e(AppLog.T.UTILS,
                                           "Target fragment doesn't implement TextInputDialogFragment.Callback");
                              }
                          })
                          .setNegativeButton(R.string.cancel,
                                  (dialog, id) -> dialog.cancel());

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.setOnShowListener(dialog -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setEnabled(isInputEnabled);
            }
        });

        return alertDialog;
    }

    @Override public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getTargetFragment() instanceof Callback) {
            ((Callback) getTargetFragment()).onTextInputDialogDismissed(callbackId);
        } else {
            AppLog.e(AppLog.T.UTILS,
                    "Target fragment doesn't implement TextInputDialogFragment.Callback");
        }
    }

    public interface Callback {
        void onSuccessfulInput(String input, int callbackId);
        void onTextInputDialogDismissed(int callbackId);
    }
}
