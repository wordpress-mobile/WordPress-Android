package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPPrefUtils;
import org.wordpress.android.widgets.WPEditText;
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
        View promptView = layoutInflater.inflate(R.layout.my_profile_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(promptView);

        final WPTextView textView = (WPTextView) promptView.findViewById(R.id.my_profile_dialog_label);
        final WPEditText editText = (WPEditText) promptView.findViewById(R.id.my_profile_dialog_input);
        final WPTextView hintView = (WPTextView) promptView.findViewById(R.id.my_profile_dialog_hint);

        Bundle args = getArguments();
        String title = args.getString(TITLE_TAG);
        String hint  = args.getString(HINT_TAG);
        Boolean isMultiline = args.getBoolean(IS_MULTILINE_TAG);
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
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (getActivity() instanceof Callback) {
                            ((Callback) getActivity()).onSuccessfulInput(editText.getText().toString(), callbackId);
                        } else {
                            AppLog.e(AppLog.T.UTILS, getActivity().getClass().getName()
                                    + " doesn't implement ProfileInputDialogFragment.Callback");
                        }
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        return alertDialogBuilder.create();
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        Button positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (positive != null) WPPrefUtils.layoutAsFlatButton(positive);
        if (negative != null) WPPrefUtils.layoutAsFlatButton(negative);
    }

    public interface Callback {
        void onSuccessfulInput(String input, int callbackId);
    }
}
