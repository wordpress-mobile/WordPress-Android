package org.wordpress.android.login;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.util.SiteUtils;
import org.wordpress.android.login.widgets.WPLoginInputRow;
import org.wordpress.android.login.widgets.WPLoginInputRow.OnEditorCommitListener;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.EditTextUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.ArrayList;

public class LoginUsernamePasswordFragment extends LoginBaseFormFragment<LoginListener> implements TextWatcher,
        OnEditorCommitListener {
    private static final String KEY_LOGIN_FINISHED = "KEY_LOGIN_FINISHED";
    private static final String KEY_REQUESTED_USERNAME = "KEY_REQUESTED_USERNAME";
    private static final String KEY_REQUESTED_PASSWORD = "KEY_REQUESTED_PASSWORD";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";

    private static final String ARG_INPUT_SITE_ADDRESS = "ARG_INPUT_SITE_ADDRESS";
    private static final String ARG_ENDPOINT_ADDRESS = "ARG_ENDPOINT_ADDRESS";
    private static final String ARG_SITE_NAME = "ARG_SITE_NAME";
    private static final String ARG_SITE_ICON_URL = "ARG_SITE_ICON_URL";
    private static final String ARG_INPUT_USERNAME = "ARG_INPUT_USERNAME";
    private static final String ARG_INPUT_PASSWORD = "ARG_INPUT_PASSWORD";
    private static final String ARG_IS_WPCOM = "ARG_IS_WPCOM";

    private static final String FORGOT_PASSWORD_URL_WPCOM = "https://wordpress.com/";

    public static final String TAG = "login_username_password_fragment_tag";

    private ScrollView mScrollView;
    private WPLoginInputRow mUsernameInput;
    private WPLoginInputRow mPasswordInput;

    private boolean mAuthFailed;
    private boolean mLoginFinished;

    private String mRequestedUsername;
    private String mRequestedPassword;
    ArrayList<Integer> mOldSitesIDs;

    private String mInputSiteAddress;
    private String mInputSiteAddressWithoutSuffix;
    private String mEndpointAddress;
    private String mSiteName;
    private String mSiteIconUrl;
    private String mInputUsername;
    private String mInputPassword;
    private boolean mIsWpcom;

    public static LoginUsernamePasswordFragment newInstance(String inputSiteAddress, String endpointAddress,
            String siteName, String siteIconUrl, String inputUsername, String inputPassword, boolean isWpcom) {
        LoginUsernamePasswordFragment fragment = new LoginUsernamePasswordFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INPUT_SITE_ADDRESS, inputSiteAddress);
        args.putString(ARG_ENDPOINT_ADDRESS, endpointAddress);
        args.putString(ARG_SITE_NAME, siteName);
        args.putString(ARG_SITE_ICON_URL, siteIconUrl);
        args.putString(ARG_INPUT_USERNAME, inputUsername);
        args.putString(ARG_INPUT_PASSWORD, inputPassword);
        args.putBoolean(ARG_IS_WPCOM, isWpcom);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected @LayoutRes int getContentLayout() {
        return R.layout.login_username_password_screen;
    }

    @Override
    protected @LayoutRes int getProgressBarText() {
        return R.string.logging_in;
    }

    @Override
    protected void setupLabel(TextView label) {
        // no label in this screen
    }

    @Override
    protected void setupContent(ViewGroup rootView) {
        mScrollView = (ScrollView) rootView.findViewById(R.id.scroll_view);

        rootView.findViewById(R.id.login_site_title_static).setVisibility(mIsWpcom ? View.GONE : View.VISIBLE);
        rootView.findViewById(R.id.login_blavatar_static).setVisibility(mIsWpcom ? View.GONE : View.VISIBLE);
        rootView.findViewById(R.id.login_blavatar).setVisibility(mIsWpcom ? View.VISIBLE : View.GONE);

        if (mSiteIconUrl != null) {
            Glide.with(this)
                .load(mSiteIconUrl)
                .apply(RequestOptions.placeholderOf(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp))
                .apply(RequestOptions.errorOf(R.drawable.ic_placeholder_blavatar_grey_lighten_20_40dp))
                .into(((ImageView) rootView.findViewById(R.id.login_blavatar)));
        }

        TextView siteNameView = ((TextView) rootView.findViewById(R.id.login_site_title));
        siteNameView.setText(mSiteName);
        siteNameView.setVisibility(mSiteName != null ? View.VISIBLE : View.GONE);

        TextView siteAddressView = ((TextView) rootView.findViewById(R.id.login_site_address));
        siteAddressView.setText(UrlUtils.removeScheme(UrlUtils.removeXmlrpcSuffix(mInputSiteAddress)));
        siteAddressView.setVisibility(mInputSiteAddress != null ? View.VISIBLE : View.GONE);

        mInputSiteAddressWithoutSuffix = UrlUtils.removeXmlrpcSuffix(mEndpointAddress);

        mUsernameInput = (WPLoginInputRow) rootView.findViewById(R.id.login_username_row);
        mUsernameInput.setText(mInputUsername);
        if (BuildConfig.DEBUG) {
            mUsernameInput.getEditText().setText(BuildConfig.DEBUG_DOTCOM_LOGIN_USERNAME);
        }
        mUsernameInput.addTextChangedListener(this);
        mUsernameInput.setOnEditorCommitListener(new OnEditorCommitListener() {
            @Override
            public void OnEditorCommit() {
                showError(null);
                mPasswordInput.getEditText().requestFocus();
            }
        });

        mPasswordInput = (WPLoginInputRow) rootView.findViewById(R.id.login_password_row);
        mPasswordInput.setText(mInputPassword);
        if (BuildConfig.DEBUG) {
            mPasswordInput.getEditText().setText(BuildConfig.DEBUG_DOTCOM_LOGIN_PASSWORD);
        }
        mPasswordInput.addTextChangedListener(this);

        mPasswordInput.setOnEditorCommitListener(this);
    }

    @Override
    protected void setupBottomButtons(Button secondaryButton, Button primaryButton) {
        secondaryButton.setText(R.string.forgot_password);
        secondaryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoginListener != null) {
                    if (mIsWpcom) {
                        mLoginListener.forgotPassword(FORGOT_PASSWORD_URL_WPCOM);
                    } else {
                        mLoginListener.forgotPassword(mInputSiteAddressWithoutSuffix);
                    }
                }
            }
        });
        primaryButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                next();
            }
        });
    }

    @Override
    protected EditText getEditTextToFocusOnStart() {
        return mUsernameInput.getEditText();
    }

    @Override
    protected void onHelp() {
        if (mLoginListener != null) {
            mLoginListener.helpUsernamePassword(mInputSiteAddress, mRequestedUsername, mIsWpcom);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoginListener.inject(this);

        mInputSiteAddress = getArguments().getString(ARG_INPUT_SITE_ADDRESS);
        mEndpointAddress = getArguments().getString(ARG_ENDPOINT_ADDRESS);
        mSiteName = getArguments().getString(ARG_SITE_NAME);
        mSiteIconUrl = getArguments().getString(ARG_SITE_ICON_URL);
        mInputUsername = getArguments().getString(ARG_INPUT_USERNAME);
        mInputPassword = getArguments().getString(ARG_INPUT_PASSWORD);
        mIsWpcom = getArguments().getBoolean(ARG_IS_WPCOM);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mLoginFinished = savedInstanceState.getBoolean(KEY_LOGIN_FINISHED);

            mRequestedUsername = savedInstanceState.getString(KEY_REQUESTED_USERNAME);
            mRequestedPassword = savedInstanceState.getString(KEY_REQUESTED_PASSWORD);
            mOldSitesIDs = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_USERNAME_PASSWORD_FORM_VIEWED);

            // auto-login if username and password are set for wpcom login
            if (mIsWpcom && !TextUtils.isEmpty(mInputUsername) && !TextUtils.isEmpty(mInputPassword)) {
                getPrimaryButton().post(new Runnable() {
                    @Override
                    public void run() {
                        getPrimaryButton().performClick();
                    }
                });
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_LOGIN_FINISHED, mLoginFinished);
        outState.putString(KEY_REQUESTED_USERNAME, mRequestedUsername);
        outState.putString(KEY_REQUESTED_PASSWORD, mRequestedPassword);
        outState.putIntegerArrayList(KEY_REQUESTED_PASSWORD, mOldSitesIDs);
    }

    protected void next() {
        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        if (TextUtils.isEmpty(getCleanedUsername())) {
            showError(getString(R.string.login_empty_username));
            EditTextUtils.showSoftInput(mUsernameInput.getEditText());
            return;
        }

        startProgress();

        mRequestedUsername = getCleanedUsername();
        mRequestedPassword = mPasswordInput.getEditText().getText().toString();

        // clear up the authentication-failed flag before
        mAuthFailed = false;

        mOldSitesIDs = SiteUtils.getCurrentSiteIds(mSiteStore, false);

        if (mIsWpcom) {
            AccountStore.AuthenticatePayload payload =
                    new AccountStore.AuthenticatePayload(mRequestedUsername, mRequestedPassword);
            mDispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(payload));
        } else {
            SiteStore.RefreshSitesXMLRPCPayload selfHostedPayload = new SiteStore.RefreshSitesXMLRPCPayload();
            selfHostedPayload.username = mRequestedUsername;
            selfHostedPayload.password = mRequestedPassword;
            selfHostedPayload.url = mEndpointAddress;
            mDispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(selfHostedPayload));
        }
    }

    private String getCleanedUsername() {
        return EditTextUtils.getText(mUsernameInput.getEditText()).trim();
    }

    @Override
    public void OnEditorCommit() {
        showError(null);
        next();
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        showError(null);
    }

    private void showError(String errorMessage) {
        mUsernameInput.setError(errorMessage != null ? " " : null);
        mPasswordInput.setError(errorMessage);

        if (errorMessage != null) {
            mPasswordInput.post(new Runnable() {
                @Override
                public void run() {
                    Rect rect = new Rect(); //coordinates to scroll to
                    mPasswordInput.getHitRect(rect);
                    mScrollView.requestChildRectangleOnScreen(mPasswordInput, rect, false);
                }
            });
        }
    }

    @Override
    protected void endProgress() {
        super.endProgress();
        mRequestedUsername = null;
        mRequestedPassword = null;
    }

    private void handleAuthError(AccountStore.AuthenticationErrorType error, String errorMessage) {
        switch (error) {
            case INCORRECT_USERNAME_OR_PASSWORD:
            case NOT_AUTHENTICATED: // NOT_AUTHENTICATED is the generic error from XMLRPC response on first call.
                showError(getString(R.string.username_or_password_incorrect));
                break;
            case INVALID_OTP:
            case INVALID_TOKEN:
            case AUTHORIZATION_REQUIRED:
            case NEEDS_2FA:
                if (mIsWpcom) {
                    if (mLoginListener != null) {
                        mLoginListener.needs2fa(mRequestedUsername, mRequestedPassword);
                    }
                } else {
                    showError("2FA not supported for self-hosted sites. Please use an app-password.");
                }
                break;
            default:
                AppLog.e(T.NUX, "Server response: " + errorMessage);

                ToastUtils.showToast(getActivity(),
                        errorMessage == null ? getString(R.string.error_generic) : errorMessage);
                break;
        }
    }

    // OnChanged events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAuthenticationChanged(AccountStore.OnAuthenticationChanged event) {
        // emitted when wpcom site or when the selfhosted login failed (but not when succeeded)

        if (!isAdded() || mLoginFinished) {
            // just bail
            return;
        }

        if (event.isError()) {
            if (mRequestedUsername == null) {
                // just bail since the operation was cancelled
                return;
            }

            mAuthFailed = true;
            AppLog.e(T.API, "Login with username/pass onAuthenticationChanged has error: " + event.error.type + " - " +
                    event.error.message);
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, event.getClass().getSimpleName(),
                    event.error.type.toString(), event.error.message);

            handleAuthError(event.error.type, event.error.message);

            // end the progress last since it cleans up the requested username/password and those might be needed
            //  in handleAuthError()
            endProgress();

            return;
        }

        AppLog.i(T.NUX, "onAuthenticationChanged: " + event.toString());

        doFinishLogin();
    }

    @Override
    protected void onLoginFinished() {
        mLoginListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, mIsWpcom);

        mLoginListener.loggedInViaPassword(mOldSitesIDs);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(SiteStore.OnSiteChanged event) {
        if (!isAdded() || mLoginFinished) {
            return;
        }

        if (event.isError()) {
            if (mRequestedUsername == null) {
                // just bail since the operation was cancelled
                return;
            }

            endProgress();

            String errorMessage;
            if (event.error.type == SiteStore.SiteErrorType.DUPLICATE_SITE) {
                if (event.rowsAffected == 0) {
                    // If there is a duplicate site and not any site has been added, show an error and
                    // stop the sign in process
                    errorMessage = getString(R.string.cannot_add_duplicate_site);
                } else {
                    // If there is a duplicate site, notify the user something could be wrong,
                    // but continue the sign in process
                    errorMessage = getString(R.string.duplicate_site_detected);
                }
            } else {
                errorMessage = getString(R.string.login_error_while_adding_site, event.error.type.toString());
            }

            AppLog.e(T.API, "Login with username/pass onSiteChanged has error: " + event.error.type + " - " +
                    errorMessage);

            if (!mAuthFailed) {
                // show the error if not already displayed in onAuthenticationChanged (like in username/pass error)
                showError(errorMessage);
            }

            return;
        }

        // continue with success, even if the operation was cancelled since the user got logged in regardless. So, go on
        //  with finishing the login process

        mLoginListener.trackAnalyticsSignIn(mAccountStore, mSiteStore, mIsWpcom);

        mLoginListener.startPostLoginServices();

        // mark as finished so any subsequent onSiteChanged (e.g. triggered by WPMainActivity) won't be intercepted
        mLoginFinished = true;

        if (mLoginListener != null) {
            if (mIsWpcom) {
                saveCredentialsInSmartLock(mLoginListener, mRequestedUsername, mRequestedPassword);
            }

            mLoginListener.loggedInViaUsernamePassword(mOldSitesIDs);
        }
    }
}
