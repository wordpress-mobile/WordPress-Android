package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.widgets.WPTextView;

public class ProfileInputDialogFragment extends DialogFragment {
    private static final String TITLE_TAG = "title";
    private static final String INITIAL_TEXT_TAG = "initial_text";
    private static final String HINT_TAG = "hint";
    private static final String IS_MULTILINE_TAG = "is_multiline";
    private static final String CALLBACK_ID_TAG = "callback_id";

    public static ProfileInputDialogFragment newInstance(String title,
                                                         String initialText,
                                                         String hint,
                                                         boolean isMultiline,
                                                         int callbackId) {
        ProfileInputDialogFragment profileInputDialogFragment = new ProfileInputDialogFragment();
        Bundle args = new Bundle();

        args.putString(TITLE_TAG, title);
        args.putString(INITIAL_TEXT_TAG, initialText);
        args.putString(HINT_TAG, hint);
        args.putBoolean(IS_MULTILINE_TAG, isMultiline);
        args.putInt(CALLBACK_ID_TAG, callbackId);

        profileInputDialogFragment.setArguments(args);
        return profileInputDialogFragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        //noinspection InflateParams
        View promptView = layoutInflater.inflate(R.layout.my_profile_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new MaterialAlertDialogBuilder(getActivity());
        alertDialogBuilder.setView(promptView);

        final WPTextView textView = promptView.findViewById(R.id.my_profile_dialog_label);
        final EditText editText = promptView.findViewById(R.id.my_profile_dialog_input);
        final WPTextView hintView = promptView.findViewById(R.id.my_profile_dialog_hint);

        Bundle args = getArguments();
        String title = args.getString(TITLE_TAG);
        String hint = args.getString(HINT_TAG);
        boolean isMultiline = args.getBoolean(IS_MULTILINE_TAG);
        String initialText = args.getString(INITIAL_TEXT_TAG);
        final int callbackId = args.getInt(CALLBACK_ID_TAG);

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

        alertDialogBuilder.setCancelable(true)
                          .setPositiveButton(android.R.string.ok, (dialog, id) -> {
                              if (getTargetFragment() instanceof Callback) {
                                  ((Callback) getTargetFragment())
                                          .onSuccessfulInput(editText.getText().toString(), callbackId);
                              } else {
                                  AppLog.e(AppLog.T.UTILS,
                                           "Target fragment doesn't implement ProfileInputDialogFragment.Callback");
                              }
                          })
                          .setNegativeButton(R.string.cancel,
                                  (dialog, id) -> dialog.cancel());

        return alertDialogBuilder.create();
    }

    public interface Callback {
        void onSuccessfulInput(String input, int callbackId);
    }
}
