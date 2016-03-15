package org.wordpress.android.stores.example;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class SignInDialog extends DialogFragment {
    public interface Listener {
        void onClick(String username, String password, String url);
    }

    private Listener mListener;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mUrlView;
    private boolean mUrlEnabled;

    public void setListener(Listener onClickListener) {
        mListener = onClickListener;
    }

    public static SignInDialog newInstance(Listener onClickListener, boolean enableUrl) {
        SignInDialog fragment = new SignInDialog();
        fragment.setListener(onClickListener);
        fragment.setUrlEnabled(enableUrl);
        return fragment;
    }

    public boolean isUrlEnabled() {
        return mUrlEnabled;
    }

    public void setUrlEnabled(boolean urlEnabled) {
        mUrlEnabled = urlEnabled;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.signin_dialog, null);
        mUsernameView = (EditText) view.findViewById(R.id.username);
        mPasswordView = (EditText) view.findViewById(R.id.password);
        mUrlView = (EditText) view.findViewById(R.id.url);
        if (!mUrlEnabled) {
            mUrlView.setVisibility(View.GONE);
        }
        builder.setView(view)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mListener.onClick(mUsernameView.getText().toString(), mPasswordView.getText().toString(),
                                mUrlView.getText().toString());
                    }
                })
                .setNegativeButton(android.R.string.cancel, new OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        SignInDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
