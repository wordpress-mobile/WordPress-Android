package org.wordpress.android.ui.accounts.login;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.SignInFragment;
import org.wordpress.android.widgets.WPTextView;

public class NewSignInActivityFragment extends Fragment {
    public interface OnEmailCheckListener {
        void onEmailChecked(boolean isWPCom);
        void onSelfHostedRequested(boolean isWPCom);
    }

    private String mEmail = "";
    private OnEmailCheckListener mListener;

    public NewSignInActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_sign_in2, container, false);
        final EditText email = (EditText) view.findViewById(R.id.email_address);
        WPTextView button = (WPTextView) view.findViewById(R.id.magic_link_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEmail = email.getText().toString();
                checkEmail();
            }
        });

        WPTextView selfHostedSite = (WPTextView) view.findViewById(R.id.nux_self_hosted_button);
        selfHostedSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onSelfHostedRequested(false);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnEmailCheckListener) {
            mListener = (OnEmailCheckListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnEmailCheckListener");
        }
    }

    private void checkEmail() {
        // request email check to server

        boolean isWPCom = true;
        mListener.onEmailChecked(isWPCom);
    }
}
