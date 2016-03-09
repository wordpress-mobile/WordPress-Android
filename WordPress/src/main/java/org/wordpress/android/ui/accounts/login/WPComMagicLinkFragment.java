package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.widgets.WPTextView;

public class WPComMagicLinkFragment extends Fragment {
    public interface OnMagicLinkFragmentInteraction {
        void onMagicLinkSent();
        void onSelfHostedRequested();
    }

    private static final String ARG_EMAIL_ADDRESS = "arg_email_address";

    private WPTextView mMagicLinkButton;
    private RelativeLayout mPasswordSection;
    private String mEmail;
    private OnMagicLinkFragmentInteraction mListener;

    public WPComMagicLinkFragment() {
    }

    public static WPComMagicLinkFragment newInstance(String email) {
        WPComMagicLinkFragment fragment = new WPComMagicLinkFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EMAIL_ADDRESS, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mEmail = getArguments().getString(ARG_EMAIL_ADDRESS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wpcom_magic_link, container, false);
        mMagicLinkButton = (WPTextView) view.findViewById(R.id.magic_button);
        mPasswordSection = (RelativeLayout) view.findViewById(R.id.nux_password_layout);
        mMagicLinkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMagicLinkRequest();
            }
        });

        TextView forgotPassword = (TextView) view.findViewById(R.id.forgot_password);
        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordField();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMagicLinkFragmentInteraction) {
            mListener = (OnMagicLinkFragmentInteraction) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private void togglePasswordField() {
        if (mPasswordSection.getVisibility() == View.GONE) {
            mPasswordSection.setVisibility(View.VISIBLE);
        } else {
            mPasswordSection.setVisibility(View.GONE);
        }
    }

    private void sendMagicLinkRequest() {
        // send magic link
        boolean ok = true;
        if (ok) {
            if (mListener != null) {
                mListener.onMagicLinkSent();
            }
        }
    }
}
