package org.wordpress.android.ui.accounts;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.accounts.SmartLockHelper.Callback;
import org.wordpress.android.ui.accounts.login.MagicLinkRequestFragment;
import org.wordpress.android.ui.accounts.login.MagicLinkSentFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

public class SignInActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener,
        MagicLinkRequestFragment.OnMagicLinkFragmentInteraction, SignInFragment.OnMagicLinkRequestInteraction,
        MagicLinkSentFragment.OnMagicLinkSentInteraction, JetpackCallbacks {
    public static final int SIGN_IN_REQUEST = 1;
    public static final int REQUEST_CODE = 5000;
    public static final int ADD_SELF_HOSTED_BLOG = 2;
    public static final int SHOW_CERT_DETAILS = 4;
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
    private Blog mJetpackBlog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            addSignInFragment();
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

        if (requestCode == SHOW_CERT_DETAILS) {
            getSignInFragment().askForSslTrust();
        } else if (requestCode == SMART_LOCK_SAVE) {
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
        getSignInFragment().setToken(uri.getQueryParameter(TOKEN_PARAMETER));
        SignInFragment signInFragment = getSignInFragment();
        slideInFragment(signInFragment, false);

        mProgressDialog = ProgressDialog
                .show(this, "", getString(R.string.logging_in), true, true, new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        getSignInFragment().setToken("");
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
                mJetpackBlog = WordPress.getBlog(extras.getInt(EXTRA_JETPACK_SITE_AUTH));
                if (mJetpackBlog != null) {
                    String customMessage = extras.getString(EXTRA_JETPACK_MESSAGE_AUTH, null);
                    getSignInFragment().setCustomMessageForJetpackAuth(customMessage);
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

    public SmartLockHelper getSmartLockHelper() {
        return mSmartLockHelper;
    }

    private void saveEmailToAccount(String email) {
        Account account = AccountHelper.getDefaultAccount();
        account.setUserName(email);
        account.save();
    }

    private void popBackStackToSignInFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.getBackStackEntryCount() > 1) {
            fragmentManager.popBackStackImmediate();
        }

        getSupportFragmentManager().popBackStack();
    }

    protected void addSignInFragment() {
        SignInFragment signInFragment = new SignInFragment();
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, signInFragment, SignInFragment.TAG);
        fragmentTransaction.commit();
    }

    private void slideInFragment(Fragment fragment) {
        slideInFragment(fragment, true);
    }

    private void slideInFragment(Fragment fragment, boolean shouldAddToBackStack) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
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
        AppLog.d(T.NUX, "Google API client connected");
        SignInFragment signInFragment =
                (SignInFragment) getSupportFragmentManager().findFragmentByTag(SignInFragment.TAG);
        // Autofill only if signInFragment is there and if it can be autofilled (ie. username and password fields are
        // empty).
        if (signInFragment != null && signInFragment.canAutofillUsernameAndPassword()) {
            mSmartLockHelper.smartLockAutoFill(new Callback() {
                @Override
                public void onCredentialRetrieved(Credential credential) {
                    SignInFragment signInFragment =
                            (SignInFragment) getSupportFragmentManager().findFragmentByTag(SignInFragment.TAG);
                    if (signInFragment != null) {
                        signInFragment.onCredentialRetrieved(credential);
                    }
                }
            });
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLog.d(T.NUX, "Google API client connection suspended");
    }

    @Override
    public void onMagicLinkSent() {
        MagicLinkSentFragment magicLinkSentFragment = new MagicLinkSentFragment();
        slideInFragment(magicLinkSentFragment);
    }

    @Override
    public void onEnterPasswordRequested() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_EXITED);
        getSignInFragment().setIsMagicLinkEnabled(false);

        popBackStackToSignInFragment();
    }

    @Override
    public void onMagicLinkRequestSuccess(String email) {
        saveEmailToAccount(email);

        MagicLinkRequestFragment magicLinkRequestFragment = MagicLinkRequestFragment.newInstance(email);
        slideInFragment(magicLinkRequestFragment);
    }

    @Override
    public boolean isJetpackAuth() {
        return mJetpackBlog != null;
    }

    @Override
    public Blog getJetpackBlog() {
        return mJetpackBlog;
    }
}
