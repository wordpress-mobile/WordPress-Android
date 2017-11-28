package org.wordpress.android.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore.OnAvailabilityChecked;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.android.support.AndroidSupportInjection;

public class LoginEmailFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener {
    private static final String KEY_GOOGLE_EMAIL = "KEY_GOOGLE_EMAIL";
    private static final String KEY_IS_SOCIAL = "KEY_IS_SOCIAL";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";
    private static final String KEY_REQUESTED_EMAIL = "KEY_REQUESTED_EMAIL";

    public static final String TAG = "login_email_fragment_tag";
    public static final int MAX_EMAIL_LENGTH = 100;

    private ArrayList<Integer> mOldSitesIDs;
    private String mGoogleEmail;
    private String mRequestedEmail;
    private WPLoginInputRow mEmailInput;
    private boolean isSocialLogin;

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_email_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return isSocialLogin ? R.string.logging_in : R.string.checking_email;
    }

    @Override
    protected void setupLabel(@NonNull TextView label) {
        switch (mLoginListener.getLoginMode()) {
            case WPCOM_LOGIN_DEEPLINK:
                label.setText(R.string.login_log_in_for_deeplink);
                break;
            case SHARE_INTENT:
                label.setText(R.string.login_log_in_for_share_intent);
                break;
            case FULL:
                label.setText(R.string.enter_email_wordpress_com);
                break;
            case JETPACK_STATS:
                label.setText(R.string.stats_sign_in_jetpack_different_com_account);
                break;
            case WPCOM_REAUTHENTICATE:
                label.setText(R.string.auth_required);
                break;
        }
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mEmailInput = rootView.findViewById(R.id.login_email_row);
        if (BuildConfig.DEBUG) {
            mEmailInput.getEditText().setText(BuildConfig.DEBUG_DOTCOM_LOGIN_EMAIL);
        }
        mEmailInput.addTextChangedListener(this);
        mEmailInput.setOnEditorCommitListener(this);

        LinearLayout googleLoginButton = rootView.findViewById(R.id.login_google_button);
        googleLoginButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            @Override
            public void onClick(View view) {
                mLoginListener.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_CLICK);
                WPActivityUtils.hideKeyboard(getActivity().getCurrentFocus());

                if (NetworkUtils.checkConnection(getActivity())) {
                    mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);
                    isSocialLogin = true;
                    addGoogleFragment();
                }
            }
        });
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    if (mLoginListener.getLoginMode() == LoginMode.JETPACK_STATS) {
                        mLoginListener.loginViaWpcomUsernameInstead();
                    } else {
                        mLoginListener.loginViaSiteAddress();
                    }
                }
            }
        });

        switch (mLoginListener.getLoginMode()) {
            case FULL:
            case SHARE_INTENT:
                // all features enabled and with typical values
                secondaryButton.setText(R.string.enter_site_address_instead);
                break;
            case JETPACK_STATS:
                secondaryButton.setText(R.string.enter_username_instead);
                break;
            case WPCOM_LOGIN_DEEPLINK:
                secondaryButton.setVisibility(View.GONE);
                break;
            case WPCOM_REAUTHENTICATE:
                secondaryButton.setVisibility(View.GONE);
                break;
        }

        primaryButton.setOnClickListener(new OnClickListener() {
            @SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
            public void onClick(View v) {
                next(getCleanedEmail());
            }
        });
    }

    @Override
    protected EditText getEditTextToFocusOnStart() {
        return mEmailInput.getEditText();
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            if (isSocialLogin) {
                // Send last email chosen from Google login if available.
                mLoginListener.helpSocialEmailScreen(mGoogleEmail);
            } else {
                // Send exact string the user has inputted for email
                mLoginListener.helpEmailScreen(EditTextUtils.getText(mEmailInput.getEditText()));
            }
        }
    }

    private void addGoogleFragment() {
        LoginGoogleFragment loginGoogleFragment;
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        loginGoogleFragment = (LoginGoogleFragment) fragmentManager.findFragmentByTag(LoginGoogleFragment.TAG);

        if (loginGoogleFragment != null) {
            fragmentTransaction.remove(loginGoogleFragment);
        }

        loginGoogleFragment = new LoginGoogleFragment();
        loginGoogleFragment.setRetainInstance(true);
        fragmentTransaction.add(loginGoogleFragment, LoginGoogleFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
            mRequestedEmail = savedInstanceState.getString(KEY_REQUESTED_EMAIL);
            mGoogleEmail = savedInstanceState.getString(KEY_GOOGLE_EMAIL);
            isSocialLogin = savedInstanceState.getBoolean(KEY_IS_SOCIAL);
        } else {
            mLoginListener.track(AnalyticsTracker.Stat.LOGIN_EMAIL_FORM_VIEWED);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList(KEY_OLD_SITES_IDS, mOldSitesIDs);
        outState.putString(KEY_REQUESTED_EMAIL, mRequestedEmail);
        outState.putString(KEY_GOOGLE_EMAIL, mGoogleEmail);
        outState.putBoolean(KEY_IS_SOCIAL, isSocialLogin);
    }

    protected void next(String email) {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (isValidEmail(email)) {
            startProgress();
            mRequestedEmail = email;
            mDispatcher.dispatch(AccountActionBuilder.newIsAvailableEmailAction(email));
        } else {
            showEmailError(R.string.email_invalid);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mLoginListener = null;
    }

    private String getCleanedEmail() {
        return EditTextUtils.getText(mEmailInput.getEditText()).trim();
    }

    private boolean isValidEmail(String email) {
        Pattern emailRegExPattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = emailRegExPattern.matcher(email);

        return matcher.find() && email.length() <= MAX_EMAIL_LENGTH;
    }

    @Override
    public void OnEditorCommit() {
        next(getCleanedEmail());
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        mEmailInput.setError(null);
        isSocialLogin = false;
    }

    private void showEmailError(int messageId) {
        mEmailInput.setError(getString(messageId));
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedEmail = null;
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAvailabilityChecked(OnAvailabilityChecked event) {
        if (mRequestedEmail == null || !mRequestedEmail.equalsIgnoreCase(event.value)) {
            // bail if user canceled or a different email request is outstanding
            return;
        }

        if (isInProgress()) {
            endProgress();
        }

        if (event.isError()) {
            // report the error but don't bail yet.
            AppLog.e(T.API, "OnAvailabilityChecked has error: " + event.error.type + " - " + event.error.message);
            showEmailError(R.string.email_not_registered_wpcom);
            return;
        }

        switch (event.type) {
            case EMAIL:
                if (event.isAvailable) {
                    // email address is available on wpcom, so apparently the user can't login with that one.
                    showEmailError(R.string.email_not_registered_wpcom);
                } else if (mLoginListener != null) {
                    EditTextUtils.hideSoftInput(mEmailInput.getEditText());
                    mLoginListener.gotWpcomEmail(event.value);
                }
                break;
            default:
                AppLog.e(T.API, "OnAvailabilityChecked unhandled event type: " + event.error.type);
                break;
        }
    }

    public void setGoogleEmail(String email) {
        mGoogleEmail = email;
    }

    public void finishLogin() {
        doFinishLogin();
    }

    @Override
    protected void onLoginFinished() {
        mLoginListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, true);
        mLoginListener.loggedInViaSocialAccount(mOldSitesIDs);
    }
}
