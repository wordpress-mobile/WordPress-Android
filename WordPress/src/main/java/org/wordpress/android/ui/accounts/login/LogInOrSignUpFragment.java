package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.ui.accounts.JetpackCallbacks;

public class LogInOrSignUpFragment extends Fragment {

    public static final String TAG = "login_or_signup_fragment_tag";
    private LogInOrSignUpFragment.OnLogInOrSignUpFragmentInteraction mListener;

    public interface OnLogInOrSignUpFragmentInteraction {
        void onLoginTapped();
        void onCreateSiteTapped();
    }

    private JetpackCallbacks mJetpackCallbacks;

    public static LogInOrSignUpFragment newInstance() {
        LogInOrSignUpFragment fragment = new LogInOrSignUpFragment();
        return fragment;
    }

    public LogInOrSignUpFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.login_signup_screen, container, false);

        Button loginButton = (Button) view.findViewById(R.id.login_button);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginTapped();
            }
        });
        Button createSiteButton = (Button) view.findViewById(R.id.create_site_button);
        createSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateSiteTapped();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof LogInOrSignUpFragment.OnLogInOrSignUpFragmentInteraction) {
            mListener = (LogInOrSignUpFragment.OnLogInOrSignUpFragmentInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
        if (context instanceof JetpackCallbacks) {
            mJetpackCallbacks = (JetpackCallbacks) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement JetpackCallbacks");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    private void onLoginTapped() {
        if (mListener != null) {
            mListener.onLoginTapped();
        }
    }

    private void onCreateSiteTapped() {
        if (mListener != null) {
            mListener.onCreateSiteTapped();
        }
    }
}
