package org.wordpress.android.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.widgets.OpenSansEditText;
import org.wordpress.android.widgets.WPTextView;

public class DialogUtils extends DialogFragment {

    private static final String TITLE_TAG = "TITLE";
    private static final String INITIAL_TEXT_TAG = "INITIAL-TEXT";
    private static final String HINT_TAG = "HINT";
    private static final String IS_MULTILINE_TAG = "IS-MULTILINE";

    private static Context mContext;
    private static Callback mCallback;

    public static DialogUtils newInstance(Context context,
                                   String title,
                                   String initialText,
                                   String hint,
                                   boolean isMultiline,
                                   Callback callback) {

        DialogUtils dialogUtils = new DialogUtils();
        Bundle args = new Bundle();

        mContext = context;
        mCallback = callback;

        args.putString(TITLE_TAG, title);
        args.putString(INITIAL_TEXT_TAG, initialText);
        args.putString(HINT_TAG, hint);
        args.putBoolean(IS_MULTILINE_TAG, isMultiline);

        dialogUtils.setArguments(args);
        return dialogUtils;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View promptView = layoutInflater.inflate(R.layout.my_profile_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mContext);
        alertDialogBuilder.setView(promptView);

        final WPTextView textView = (WPTextView) promptView.findViewById(R.id.my_profile_dialog_label);
        final OpenSansEditText editText = (OpenSansEditText) promptView.findViewById(R.id.my_profile_dialog_input);
        final WPTextView hintView = (WPTextView) promptView.findViewById(R.id.my_profile_dialog_hint);

        Bundle args = getArguments();
        String title = args.getString(TITLE_TAG);
        String hint  = args.getString(HINT_TAG);
        Boolean isMultiline = args.getBoolean(IS_MULTILINE_TAG);
        String initialText = args.getString(INITIAL_TEXT_TAG);

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
                        mCallback.onSuccessfulInput(editText.getText().toString());
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

    public interface Callback {
        void onSuccessfulInput(String input);
    }
}
