package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.login.Login2FaFragment;
import org.wordpress.android.ui.accounts.login.LoginEmailFragment;
import org.wordpress.android.ui.accounts.login.LoginEmailPasswordFragment;
import org.wordpress.android.ui.accounts.login.LoginListener;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkRequestFragment;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkSentFragment;
import org.wordpress.android.ui.accounts.login.LoginPrologueFragment;
import org.wordpress.android.ui.accounts.login.LoginSiteAddressFragment;
import org.wordpress.android.ui.accounts.login.LoginUsernamePasswordFragment;
import org.wordpress.android.ui.accounts.SmartLockHelper.Callback;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;

import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener,
        Callback, LoginListener {
    private static final String FORGOT_PASSWORD_URL = "https://wordpress.com/wp-login.php?action=lostpassword";

    private SmartLockHelper mSmartLockHelper;

    private LoginMode mLoginMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            switch (getLoginMode()) {
                case FULL:
                    showFragment(new LoginPrologueFragment(), LoginPrologueFragment.TAG);
                    break;
                case SELFHOSTED_ONLY:
                    showFragment(new LoginSiteAddressFragment(), LoginSiteAddressFragment.TAG);
                    break;
                case JETPACK_STATS:
                case WPCOM_LOGIN_DEEPLINK:
                case WPCOM_REAUTHENTICATE:
                    checkSmartLockPasswordAndStartLogin();
                    break;
            }
        }
    }

    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        fragmentTransaction.commit();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return false;
    }

    @Override
    public LoginMode getLoginMode() {
        if (mLoginMode != null) {
            // returned the cached value
            return mLoginMode;
        }

        // compute and cache the Login mode
        mLoginMode = LoginMode.fromIntent(getIntent());

        return mLoginMode;
    }

    private void loggedInAndFinish(ArrayList<Integer> oldSitesIds) {
        switch (getLoginMode()) {
            case FULL:
                ActivityLauncher.showMainActivityAndLoginEpilogue(this, oldSitesIds);
                setResult(Activity.RESULT_OK);
                finish();
                break;
            case JETPACK_STATS:
            case WPCOM_LOGIN_DEEPLINK:
            case WPCOM_REAUTHENTICATE:
                ActivityLauncher.showLoginEpilogueForResult(this, true, oldSitesIds);
                break;
            case SELFHOSTED_ONLY:
                // skip the epilogue when only added a selfhosted site
                setResult(Activity.RESULT_OK);
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.SHOW_LOGIN_EPILOGUE_AND_RETURN:
                // we showed the epilogue screen as informational and sites got loaded so, just return to login caller now
                setResult(RESULT_OK);
                finish();
                break;
            case RequestCodes.SMART_LOCK_SAVE:
                if (resultCode == RESULT_OK) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_UPDATED);
                    AppLog.d(AppLog.T.NUX, "Credentials saved");
                } else {
                    AppLog.d(AppLog.T.NUX, "Credentials save cancelled");
                }
                break;
            case RequestCodes.SMART_LOCK_READ:
                if (resultCode == RESULT_OK) {
                    AppLog.d(AppLog.T.NUX, "Credentials retrieved");
                    Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                    onCredentialRetrieved(credential);
                } else {
                    AppLog.e(AppLog.T.NUX, "Credential read failed");
                    onCredentialsUnavailable();
                }
                break;
        }
    }

    private void jumpToUsernamePassword(String username, String password) {
        LoginUsernamePasswordFragment loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
                "wordpress.com", "wordpress.com", "WordPress.com", "https://s0.wp.com/i/webclip.png", username,
                password, true);
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG);
    }

    private void checkSmartLockPasswordAndStartLogin() {
        if (mSmartLockHelper == null) {
            mSmartLockHelper = new SmartLockHelper(this);
            mSmartLockHelper.initSmartLockForPasswords();
        } else {
            startLogin();
        }
    }

    private void startLogin() {
        slideInFragment(new LoginEmailFragment(), true, LoginEmailFragment.TAG);
    }

    // LoginListener implementation methods

    @Override
    public void showEmailLoginScreen() {
        checkSmartLockPasswordAndStartLogin();
    }

    @Override
    public void doStartSignup() {
        NewUserFragment newUserFragment = NewUserFragment.newInstance();
        slideInFragment(newUserFragment, true, NewUserFragment.TAG);
    }

    @Override
    public void loggedInViaSigUp(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds);
    }

    @Override
    public void newUserCreatedButErrored(String email, String password) {
        LoginEmailPasswordFragment loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, password);
        slideInFragment(loginEmailPasswordFragment, false, LoginEmailPasswordFragment.TAG);
    }

    @Override
    public void gotWpcomEmail(String email) {
        if (getLoginMode() != LoginMode.WPCOM_LOGIN_DEEPLINK) {
            LoginMagicLinkRequestFragment loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment.newInstance(email);
            slideInFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG);
        } else {
            LoginEmailPasswordFragment loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null);
            slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
        }
    }

    @Override
    public void loginViaSiteAddress() {
        LoginSiteAddressFragment loginSiteAddressFragment = new LoginSiteAddressFragment();
        slideInFragment(loginSiteAddressFragment, true, LoginSiteAddressFragment.TAG);
    }

    @Override
    public void loginViaWpcomUsernameInstead() {
        jumpToUsernamePassword(null, null);
    }

    @Override
    public void showMagicLinkSentScreen(String email) {
        LoginMagicLinkSentFragment loginMagicLinkSentFragment = LoginMagicLinkSentFragment.newInstance(email);
        slideInFragment(loginMagicLinkSentFragment, true, LoginMagicLinkSentFragment.TAG);
    }

    @Override
    public void openEmailClient() {
        if (WPActivityUtils.isEmailClientAvailable(this)) {
            WPActivityUtils.openEmailClient(this);
        } else {
            ToastUtils.showToast(this, R.string.login_email_client_not_found);
        }
    }

    @Override
    public void usePasswordInstead(String email) {
        LoginEmailPasswordFragment loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailFragment.TAG);
    }

    @Override
    public void forgotPassword() {
        ActivityLauncher.openUrlExternal(this, FORGOT_PASSWORD_URL);
    }

    @Override
    public void needs2fa(String email, String password) {
        Login2FaFragment login2FaFragment = Login2FaFragment.newInstance(email, password);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void loggedInViaPassword(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds);
    }

    @Override
    public void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds) {
        ToastUtils.showToast(this, R.string.already_logged_in_wpcom, ToastUtils.Duration.LONG);
        loggedInAndFinish(oldSitesIds);
    }

    public void gotWpcomSiteInfo(String siteAddress, String siteName, String siteIconUrl) {
        LoginUsernamePasswordFragment loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
                siteAddress, siteAddress, siteName, siteIconUrl, null, null, true);
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG);
    }

    @Override
    public void gotXmlRpcEndpoint(String inputSiteAddress, String endpointAddress) {
        LoginUsernamePasswordFragment loginUsernamePasswordFragment = LoginUsernamePasswordFragment.newInstance(
                inputSiteAddress, endpointAddress, null, null, null, null, false);
        slideInFragment(loginUsernamePasswordFragment, true, LoginUsernamePasswordFragment.TAG);
    }

    @Override
    public void helpWithSiteAddress() {
        ToastUtils.showToast(this, "Help finding site address is not implemented yet.");
    }

    @Override
    public void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds);
    }

    @Override
    public void help() {
        ToastUtils.showToast(this, "Help is not implemented yet.");
    }

    @Override
    public void setHelpContext(String faqId, String faqSection) {
        // nothing implemented here yet. This will set the context the `help()` callback should work with
    }

    // SmartLock

    @Override
    public SmartLockHelper getSmartLockHelper() {
        return mSmartLockHelper;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AppLog.d(AppLog.T.NUX, "Connection result: " + connectionResult);
    }

    @Override
    public void onConnected(Bundle bundle) {
        AppLog.d(AppLog.T.NUX, "Google API client connected");

        // force account chooser
        mSmartLockHelper.disableAutoSignIn();

        mSmartLockHelper.smartLockAutoFill(this);
    }

    @Override
    public void onCredentialRetrieved(Credential credential) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_FILLED);

        final String username = credential.getId();
        final String password = credential.getPassword();
        jumpToUsernamePassword(username, password);
    }

    @Override
    public void onCredentialsUnavailable() {
        startLogin();
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLog.d(AppLog.T.NUX, "Google API client connection suspended");
    }
}
