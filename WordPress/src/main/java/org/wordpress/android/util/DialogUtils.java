package org.wordpress.android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;

import org.wordpress.android.widgets.OpenSansEditText;
import org.wordpress.android.widgets.WPTextView;

public class DialogUtils {
    public static void showMyProfileDialog(Context context, String title, final Callback callback) {
        showMyProfileDialog(context, title, null, callback);
    }

    public static void showMyProfileDialog(Context context, String title, String hint, final Callback callback) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(org.wordpress.android.R.layout.my_profile_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptView);

        final WPTextView textView = (WPTextView) promptView.findViewById(org.wordpress.android.R.id.my_profile_dialog_label);
        final OpenSansEditText editText = (OpenSansEditText) promptView.findViewById(org.wordpress.android.R.id.my_profile_dialog_input);
        final WPTextView hintView = (WPTextView) promptView.findViewById(org.wordpress.android.R.id.my_profile_dialog_hint);

        textView.setText(title);
        if (hint != null && !hint.isEmpty()) {
            hintView.setText(hint);
        }
        else {
            hintView.setVisibility(View.GONE);
        }

        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(org.wordpress.android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        callback.onSuccessfulInput(editText.getText().toString());
                    }
                })
                .setNegativeButton(org.wordpress.android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public interface Callback {
        void onSuccessfulInput(String input);
    }
}
