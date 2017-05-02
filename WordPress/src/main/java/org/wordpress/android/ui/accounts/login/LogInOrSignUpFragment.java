package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.login.nav.LoginEvents;
import org.wordpress.android.ui.accounts.login.nav.LoginNavigationFsmGetter;

public class LogInOrSignUpFragment extends Fragment {

    public static final String TAG = "login_or_signup_fragment_tag";

    LoginEvents.LoginNavPrologue mLoginNavPrologue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_signup_screen, container, false);

        view.findViewById(R.id.login_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginNavPrologue != null) {
                    mLoginNavPrologue.doStartLogin();
                }
            }
        });

        view.findViewById(R.id.create_site_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginNavPrologue != null) {
                    mLoginNavPrologue.doStartSignup();
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LoginNavigationFsmGetter) {
            mLoginNavPrologue = ((LoginNavigationFsmGetter) context).getLoginNavPrologue();
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginNavigationFsmGetter");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginNavPrologue = null;
    }
}
