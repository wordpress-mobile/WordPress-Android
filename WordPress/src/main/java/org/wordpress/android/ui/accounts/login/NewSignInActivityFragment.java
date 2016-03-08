package org.wordpress.android.ui.accounts.login;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class NewSignInActivityFragment extends Fragment {

    public NewSignInActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_new_sign_in2, container, false);
        final EditText email = (EditText) view.findViewById(R.id.email_address);
        WPTextView button = (WPTextView) view.findViewById(R.id.magic_link_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewSignInActivity newSignInActivity = (NewSignInActivity) getActivity();
                newSignInActivity.email = email.getText().toString();
                newSignInActivity.logIn();
            }
        });

        return view;
    }
}
