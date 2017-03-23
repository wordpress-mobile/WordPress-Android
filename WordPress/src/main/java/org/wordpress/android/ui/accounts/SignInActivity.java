package org.wordpress.android.ui.accounts;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.accounts.login.LoginEmailFragment;
import org.wordpress.android.ui.accounts.login.LoginSiteAddressFragment;
import org.wordpress.android.ui.accounts.login.LoginUsernamePasswordFragment;
import org.wordpress.android.ui.accounts.login.MagicLinkRequestFragment;
import org.wordpress.android.ui.accounts.login.MagicLinkSentFragment;
import org.wordpress.android.ui.main.WPMainActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import javax.inject.Inject;

public class SignInActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener,
        MagicLinkRequestFragment.OnMagicLinkFragmentInteraction, JetpackCallbacks,
        LoginEmailFragment.OnMagicLinkEmailInteraction,
        SignInFragment.OnMagicLinkRequestInteraction, MagicLinkSentFragment.OnMagicLinkSentInteraction,
        LoginEmailPasswordFragment.OnEmailPasswordLoginInteraction,
        LoginSiteAddressFragment.OnSiteAddressRequestInteraction,
        LoginUsernamePasswordFragment.OnLoginUsernamePasswordInteraction {
    public static final boolean USE_NEW_LOGIN_FLOWS = true;

    public static final int SIGN_IN_REQUEST = 1;
    public static final int REQUEST_CODE = 5000;
    public static final int ADD_SELF_HOSTED_BLOG = 2;
    public static final int SMART_LOCK_SAVE = 5;
    public static final int SMART_LOCK_READ = 6;

    public static final String EXTRA_START_FRAGMENT = "start-fragment";
    public static final String EXTRA_JETPACK_SITE_AUTH = "EXTRA_JETPACK_SITE_AUTH";
    public static final String EXTRA_JETPACK_MESSAGE_AUTH = "EXTRA_JETPACK_MESSAGE_AUTH";
    public static final String EXTRA_IS_AUTH_ERROR = "EXTRA_IS_AUTH_ERROR";
    public static final String EXTRA_PREFILL_URL = "EXTRA_PREFILL_URL";
    public static final String EXTRA_INHIBIT_MAGIC_LOGIN = "INHIBIT_MAGIC_LOGIN";
    public static final String MAGIC_LOGIN = "magic-login";
    public static final String TOKEN_PARAMETER = "token";

    private SmartLockHelper mSmartLockHelper;
    private ProgressDialog mProgressDialog;
    private SiteModel mJetpackSite;

    @Inject SiteStore mSiteStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.welcome_activity);

        String action = getIntent().getAction();
        Uri data = getIntent().getData();

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if (data.getBooleanQueryParameter("selfhosted", false)) {
                getIntent().putExtra(SignInActivity.EXTRA_START_FRAGMENT, SignInActivity.ADD_SELF_HOSTED_BLOG);
                if (data.getQueryParameter("url") != null) {
                    getIntent().putExtra(EXTRA_PREFILL_URL, data.getQueryParameter("url"));
                }
            }
        }

        if (savedInstanceState == null) {
            addLoginFragment();
        }

        mSmartLockHelper = new SmartLockHelper(this);
        mSmartLockHelper.initSmartLockForPasswords();

        ActivityId.trackLastActivity(ActivityId.LOGIN);
    }

    @Override
    protected void onResume() {
        super.onResume();
        actionMode(getIntent().getExtras());
        if (hasMagicLinkLoginIntent()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED);
            attemptLoginWithToken(getIntent().getData());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        cancelProgressDialog();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SMART_LOCK_SAVE) {
            if (resultCode == RESULT_OK) {
                AnalyticsTracker.track(Stat.LOGIN_AUTOFILL_CREDENTIALS_UPDATED);
                AppLog.d(T.NUX, "Credentials saved");
            } else {
                AppLog.d(T.NUX, "Credentials save cancelled");
            }
        } else if (requestCode == SMART_LOCK_READ) {
            if (resultCode == RESULT_OK) {
                AppLog.d(T.NUX, "Credentials retrieved");
                Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                SignInFragment signInFragment = (SignInFragment) getSupportFragmentManager().findFragmentByTag(SignInFragment.TAG);
                if (signInFragment != null) {
                    signInFragment.onCredentialRetrieved(credential);
                }
            } else {
                AppLog.e(T.NUX, "Credential read failed");
            }
        }
    }

    private void cancelProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.cancel();
        }
    }

    private void attemptLoginWithToken(Uri uri) {
        if (!USE_NEW_LOGIN_FLOWS) {
            getSignInFragment().setToken(uri.getQueryParameter(TOKEN_PARAMETER));
            SignInFragment signInFragment = getSignInFragment();
            slideInFragment(signInFragment, false, SignInFragment.TAG);
        } else {
            MagicLinkSentFragment magicLinkSentFragment = getMagicLinkSentFragment();
            magicLinkSentFragment.setToken(uri.getQueryParameter(TOKEN_PARAMETER));
            slideInFragment(magicLinkSentFragment, false, MagicLinkSentFragment.TAG);
        }

        mProgressDialog = ProgressDialog
                .show(this, "", getString(R.string.logging_in), true, true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (!USE_NEW_LOGIN_FLOWS) {
                            getSignInFragment().setToken("");
                        } else {
                            getMagicLinkSentFragment().setToken("");
                        }
                    }
                });
        mProgressDialog.show();
    }

    private boolean hasMagicLinkLoginIntent() {
        String action = getIntent().getAction();
        Uri uri = getIntent().getData();

        return Intent.ACTION_VIEW.equals(action) && uri != null && uri.getHost().contains(MAGIC_LOGIN);
    }

    protected void actionMode(Bundle extras) {
        int actionMode = SIGN_IN_REQUEST;
        String prefillUrl = "";
        if (extras != null) {
            actionMode = extras.getInt(EXTRA_START_FRAGMENT, -1);
            if (extras.containsKey(EXTRA_JETPACK_SITE_AUTH)) {
                mJetpackSite = mSiteStore.getSiteByLocalId(extras.getInt(EXTRA_JETPACK_SITE_AUTH));
                if (mJetpackSite != null) {
                    String customMessage = extras.getString(EXTRA_JETPACK_MESSAGE_AUTH, null);
                    getSignInFragment().setBlogAndCustomMessageForJetpackAuth(mJetpackSite, customMessage);
                }
            } else if (extras.containsKey(EXTRA_IS_AUTH_ERROR)) {
                getSignInFragment().showAuthErrorMessage();
            }
            prefillUrl = extras.getString(EXTRA_PREFILL_URL, "");
        }
        switch (actionMode) {
            case ADD_SELF_HOSTED_BLOG:
                getSignInFragment().forceSelfHostedMode(prefillUrl);
                break;
            default:
                break;
        }
    }

    public SignInFragment getSignInFragment() {
        SignInFragment signInFragment =
                (SignInFragment) getSupportFragmentManager().findFragmentByTag(SignInFragment.TAG);
        if (signInFragment == null) {
            return new SignInFragment();
        } else {
            return signInFragment;
        }
    }

    public LoginEmailFragment getLoginEmailFragment() {
        LoginEmailFragment loginEmailFragment =
                (LoginEmailFragment) getSupportFragmentManager().findFragmentByTag(LoginEmailFragment.TAG);
        if (loginEmailFragment == null) {
            return new LoginEmailFragment();
        } else {
            return loginEmailFragment;
        }
    }

    public MagicLinkSentFragment getMagicLinkSentFragment() {
        MagicLinkSentFragment magicLinkSentFragment =
                (MagicLinkSentFragment) getSupportFragmentManager().findFragmentByTag(MagicLinkSentFragment.TAG);
        if (magicLinkSentFragment == null) {
            return new MagicLinkSentFragment();
        } else {
            return magicLinkSentFragment;
        }
    }

    public LoginEmailPasswordFragment getLoginEmailPasswordFragment(String email) {
        LoginEmailPasswordFragment loginEmailPasswordFragment =
                (LoginEmailPasswordFragment) getSupportFragmentManager().findFragmentByTag(LoginEmailPasswordFragment.TAG);
        if (loginEmailPasswordFragment == null) {
            return LoginEmailPasswordFragment.newInstance(email);
        } else {
            return loginEmailPasswordFragment;
        }
    }

    public SmartLockHelper getSmartLockHelper() {
        return mSmartLockHelper;
    }

    protected void addLoginFragment() {
        if (!USE_NEW_LOGIN_FLOWS) {
            SignInFragment signInFragment = new SignInFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, signInFragment, SignInFragment.TAG);
            fragmentTransaction.commit();
        } else {
            LoginEmailFragment loginEmailFragment = new LoginEmailFragment();
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.replace(R.id.fragment_container, loginEmailFragment, LoginEmailFragment.TAG);
            fragmentTransaction.commit();
        }
    }

    private void slideInFragment(Fragment fragment, String tag) {
        slideInFragment(fragment, true, tag);
    }

    private void slideInFragment(Fragment fragment, boolean shouldAddToBackStack, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AppLog.d(T.NUX, "Connection result: " + connectionResult);
    }

    @Override
    public void onConnected(Bundle bundle) {
//        AppLog.d(T.NUX, "Google API client connected");
//        SignInFragment signInFragment =
//                (SignInFragment) getSupportFragmentManager().findFragmentByTag(SignInFragment.TAG);
//        // Autofill only if signInFragment is there and if it can be autofilled (ie. username and password fields are
//        // empty).
//        if (signInFragment != null && signInFragment.canAutofillUsernameAndPassword()) {
//            mSmartLockHelper.smartLockAutoFill(new Callback() {
//                @Override
//                public void onCredentialRetrieved(Credential credential) {
//                    SignInFragment signInFragment =
//                            (SignInFragment) getSupportFragmentManager().findFragmentByTag(SignInFragment.TAG);
//                    if (signInFragment != null) {
//                        signInFragment.onCredentialRetrieved(credential);
//                    }
//                }
//            });
//        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLog.d(T.NUX, "Google API client connection suspended");
    }

    @Override
    public void onMagicLinkSent() {
        slideInFragment(getMagicLinkSentFragment(), MagicLinkSentFragment.TAG);
    }

    @Override
    public void onEnterPasswordRequested() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_EXITED);

        String email = null;
        if (!USE_NEW_LOGIN_FLOWS) {
            email = getSignInFragment().mUsername;
        } else {
            email = getLoginEmailFragment().getEmail();
        }
        slideInFragment(getLoginEmailPasswordFragment(email), LoginEmailPasswordFragment.TAG);
    }

    @Override
    public void onMagicLinkEmailCheckSuccess(String email) {
        MagicLinkRequestFragment magicLinkRequestFragment = MagicLinkRequestFragment.newInstance(email);
        slideInFragment(magicLinkRequestFragment, MagicLinkRequestFragment.TAG);
    }

    @Override
    public void onLoginViaUsernamePassword() {
        slideInFragment(new LoginSiteAddressFragment(), LoginSiteAddressFragment.TAG);
    }

    @Override
    public void onMagicLinkFlowSucceeded() {
        Intent intent = new Intent(this, WPMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SignInActivity.MAGIC_LOGIN, true);
        startActivity(intent);
    }

    @Override
    public void onMagicLinkRequestSuccess(String email) {
        MagicLinkRequestFragment magicLinkRequestFragment = MagicLinkRequestFragment.newInstance(email);
        slideInFragment(magicLinkRequestFragment, MagicLinkRequestFragment.TAG);
    }

    @Override
    public void onEmailPasswordLoginSuccess() {
        // move on the the main activity
        Intent intent = new Intent(this, WPMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onSiteAddressRequestSuccess(String siteAddress, boolean isSelfHosted) {
        LoginUsernamePasswordFragment loginUsernamePasswordFragment =
                LoginUsernamePasswordFragment.newInstance(siteAddress, isSelfHosted);
        slideInFragment(loginUsernamePasswordFragment, LoginUsernamePasswordFragment.TAG);
    }

    @Override
    public void onUsernamePasswordLoginSuccess() {
        // move on the the main activity
        Intent intent = new Intent(this, WPMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public boolean isJetpackAuth() {
        return mJetpackSite != null;
    }

    @Override
    public SiteModel getJetpackSite() {
        return mJetpackSite;
    }
}
