package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.util.AnalyticsUtils;

import javax.inject.Inject;

import static com.helpshift.util.HelpshiftContext.getApplicationContext;

public class LoginErrorFragment extends LoginBaseFormFragment<LoginListener> {
    @Inject AccountStore mAccountStore;

    private String mEmail;
    private boolean mIsEmailNotFound;

    private static final String ARG_EMAIL = "ARG_EMAIL";
    private static final String ARG_IS_EMAIL_NOT_FOUND = "ARG_IS_EMAIL_NOT_FOUND";

    public static final String TAG = "login_error_fragment_tag";

    public static LoginErrorFragment newInstance(boolean isEmailNotFound, String email) {
        LoginErrorFragment loginErrorFragment = new LoginErrorFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(ARG_IS_EMAIL_NOT_FOUND, isEmailNotFound);
        bundle.putString(ARG_EMAIL, email);
        loginErrorFragment.setArguments(bundle);
        return loginErrorFragment;
    }

    @Override
    public void afterTextChanged(Editable s) {
        // No text input field on error screen.
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // No text input field on error screen.
    }

    @Override
    protected ViewGroup createMainView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return (ViewGroup) inflater.inflate(R.layout.login_error_footer, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EPILOGUE_VIEWED);
        }
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        // Inflate the view in createMainView().
        return 0;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.logging_in;
    }

    @Override
    protected boolean listenForLogin() {
        return super.listenForLogin();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mIsEmailNotFound = getArguments().getBoolean(ARG_IS_EMAIL_NOT_FOUND);
        mEmail = getArguments().getString(ARG_EMAIL);
    }

    @Override
    protected void onHelp() {
        // No help action on error screen.
    }

    @Override
    protected void onLoginFinished() {
        AnalyticsUtils.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);
        endProgress();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // No text input field on error screen.
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        // No primary or secondary buttons on error screen.
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        LayoutInflater layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        ViewGroup header = (ViewGroup) rootView.findViewById(R.id.header);
        View view;

        if (mIsEmailNotFound) {
            view = layoutInflater.inflate(R.layout.login_error_header_email_not_found, null);
        } else {
            // TODO: Inflate generic error header.
        }

        header.addView(view, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout layoutAnotherEmail = (LinearLayout) rootView.findViewById(R.id.footer_another_email);
        layoutAnotherEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Another email not implemented", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout layoutSiteAddress = (LinearLayout) rootView.findViewById(R.id.footer_site_address);
        layoutSiteAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Site address not implemented", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout layoutSignUp = (LinearLayout) rootView.findViewById(R.id.footer_sign_up);
        layoutSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Sign up not implemented", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void setupLabel(TextView label) {
        // No label on error screen.
    }
}
