package org.wordpress.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.widgets.OpenSansEditText;
import org.wordpress.android.widgets.WPTextView;

public class DialogUtils {

    public static void showMyProfileDialog(Context context,
                                           String title,
                                           String initialText,
                                           String hint,
                                           boolean isMultiline,
                                           final Callback callback) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.my_profile_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptView);

        final WPTextView textView = (WPTextView) promptView.findViewById(R.id.my_profile_dialog_label);
        final OpenSansEditText editText = (OpenSansEditText) promptView.findViewById(R.id.my_profile_dialog_input);
        final WPTextView hintView = (WPTextView) promptView.findViewById(R.id.my_profile_dialog_hint);

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
                        callback.onSuccessfulInput(editText.getText().toString());
                    }
                })
                .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                callback.onUnSuccessfulInput();
                            }
                        });

        AlertDialog alert = alertDialogBuilder.create();
        alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                callback.setDialogState();
            }
        });
        alert.show();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                callback.onInputChanged(s.toString());
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // DO NOTHING
            }

            @Override
            public void afterTextChanged(Editable s) {
                callback.onInputChanged(s.toString());
            }
        });

    }

    public interface Callback {
        void onSuccessfulInput(String input);
        void onUnSuccessfulInput();
        void onInputChanged(String input);
        void setDialogState();
    }
}
