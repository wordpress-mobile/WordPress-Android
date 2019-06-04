package org.wordpress.android.login;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import org.wordpress.android.util.EditTextUtils;

public class LoginHttpAuthDialogFragment extends DialogFragment {
    public static final String TAG = "login_http_auth_dialog_fragment_tag";

    public static final int DO_HTTP_AUTH = Activity.RESULT_FIRST_USER + 1;

    public static final String ARG_URL = "ARG_URL";
    public static final String ARG_USERNAME = "ARG_USERNAME";
    public static final String ARG_PASSWORD = "ARG_PASSWORD";

    private String mUrl;

    public static LoginHttpAuthDialogFragment newInstance(@NonNull final String url) {
        LoginHttpAuthDialogFragment fragment = new LoginHttpAuthDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUrl = getArguments().getString(ARG_URL);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder alert = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.LoginTheme));
        alert.setTitle(R.string.http_authorization_required);

        //noinspection InflateParams
        View httpAuth = getActivity().getLayoutInflater().inflate(R.layout.login_alert_http_auth, null);
        alert.setView(httpAuth);

        final EditText usernameEditText = (EditText) httpAuth.findViewById(R.id.login_http_username);
        final EditText passwordEditText = (EditText) httpAuth.findViewById(R.id.login_http_password);

        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String username = EditTextUtils.getText(usernameEditText);
                String password = EditTextUtils.getText(passwordEditText);
                sendResult(username, password);

                dismiss();

                return true;
            }
        });

        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });
        alert.setPositiveButton(R.string.next, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String username = EditTextUtils.getText(usernameEditText);
                String password = EditTextUtils.getText(passwordEditText);
                sendResult(username, password);
            }
        });

        final AlertDialog alertDialog = alert.create();

        // update the Next button when username edit box changes
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                updateButton(alertDialog, usernameEditText);
            }
        });

        // update the Next button on first appearance
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                updateButton(alertDialog, usernameEditText);
            }
        });

        return alertDialog;
    }

    private void updateButton(AlertDialog alertDialog, EditText editText) {
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled((editText.getText().length() > 0));
    }

    private void sendResult(String username, String password) {
        Intent intent = new Intent();
        intent.putExtra(ARG_URL, mUrl);
        intent.putExtra(ARG_USERNAME, username);
        intent.putExtra(ARG_PASSWORD, password);
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
    }
}
