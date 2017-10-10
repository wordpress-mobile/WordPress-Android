package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.Login2FaFragment;
import org.wordpress.android.login.LoginEmailFragment;
import org.wordpress.android.login.LoginEmailPasswordFragment;
import org.wordpress.android.login.LoginListener;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment;
import org.wordpress.android.login.LoginUsernamePasswordFragment;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.SmartLockHelper.Callback;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkRequestFragment;
import org.wordpress.android.ui.accounts.login.LoginMagicLinkSentFragment;
import org.wordpress.android.ui.accounts.login.LoginPrologueFragment;
import org.wordpress.android.ui.accounts.login.LoginSiteAddressFragment;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateService;
import org.wordpress.android.ui.reader.services.ReaderUpdateService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.HelpshiftHelper.Tag;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements ConnectionCallbacks, OnConnectionFailedListener,
        Callback, LoginListener {
    private static final String KEY_SMARTLOCK_COMPLETED = "KEY_SMARTLOCK_COMPLETED";

    private static final String FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword";

    private SmartLockHelper mSmartLockHelper;
    private boolean mSmartLockCompleted;

    private LoginMode mLoginMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_ACCESSED);

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
        } else {
            mSmartLockCompleted = savedInstanceState.getBoolean(KEY_SMARTLOCK_COMPLETED);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_SMARTLOCK_COMPLETED, mSmartLockCompleted);
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

    private LoginPrologueFragment getLoginPrologueFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LoginPrologueFragment.TAG);
        return fragment == null ? null : (LoginPrologueFragment) fragment;
    }

    private LoginEmailFragment getLoginEmailFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(LoginEmailFragment.TAG);
        return fragment == null ? null : (LoginEmailFragment) fragment;
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
        boolean smartLockStarted = false;
        if (!mSmartLockCompleted && mSmartLockHelper == null) {
            mSmartLockHelper = new SmartLockHelper(this);
            smartLockStarted = mSmartLockHelper.initSmartLockForPasswords();
        }

        if (!smartLockStarted) {
            startLogin();
        }
    }

    private void startLogin() {
        if (getLoginEmailFragment() != null) {
            // email screen is already shown so, login has already started. Just bail.
            return;
        }

        if (getLoginPrologueFragment() == null) {
            // prologue fragment is not shown so, the email screen will be the initial screen on the fragment container
            showFragment(new LoginEmailFragment(), LoginEmailFragment.TAG);
        } else {
            // prologue fragment is shown so, slide in the email screen (and add to history)
            slideInFragment(new LoginEmailFragment(), true, LoginEmailFragment.TAG);
        }
    }

    // LoginListener implementation methods

    @Override
    public void showEmailLoginScreen() {
        checkSmartLockPasswordAndStartLogin();
    }

    @Override
    public void doStartSignup() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATE_ACCOUNT_INITIATED);
        NewUserFragment newUserFragment = NewUserFragment.newInstance();
        slideInFragment(newUserFragment, true, NewUserFragment.TAG);
    }

    @Override
    public void loggedInViaSignup(ArrayList<Integer> oldSitesIds) {
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
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED);
            WPActivityUtils.openEmailClient(this);
        } else {
            ToastUtils.showToast(this, R.string.login_email_client_not_found);
        }
    }

    @Override
    public void usePasswordInstead(String email) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_EXITED);
        LoginEmailPasswordFragment loginEmailPasswordFragment = LoginEmailPasswordFragment.newInstance(email, null);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
    }

    @Override
    public void forgotPassword(String url) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FORGOT_PASSWORD_CLICKED);
        ActivityLauncher.openUrlExternal(this, url + FORGOT_PASSWORD_URL_SUFFIX);
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

    private void launchHelpshift(String url, String username, boolean isWpcom, Tag origin) {
        Intent intent = new Intent(this, HelpActivity.class);
        // Used to pass data to an eventual support service
        intent.putExtra(HelpshiftHelper.ENTERED_URL_KEY, url);
        intent.putExtra(HelpshiftHelper.ENTERED_USERNAME_KEY, username);
        intent.putExtra(HelpshiftHelper.ORIGIN_KEY, origin);
        if (getLoginMode() == LoginMode.JETPACK_STATS) {
            Tag[] tags = new Tag[]{Tag.CONNECTING_JETPACK};
            intent.putExtra(HelpshiftHelper.EXTRA_TAGS_KEY, tags);
        }
        startActivity(intent);
    }

    @Override
    public void helpSiteAddress(String url) {
        launchHelpshift(url, null, false, Tag.ORIGIN_LOGIN_SITE_ADDRESS);
    }

    @Override
    public void helpFindingSiteAddress(String username, SiteStore siteStore) {
        HelpshiftHelper.getInstance().showConversation(this, siteStore, Tag.ORIGIN_LOGIN_SITE_ADDRESS, username);
    }

    @Override
    public void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds);
    }

    @Override
    public void helpEmailScreen(String email) {
        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_EMAIL);
    }

    @Override
    public void helpMagicLinkRequest(String email) {
        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_MAGIC_LINK);
    }

    @Override
    public void helpMagicLinkSent(String email) {
        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_MAGIC_LINK);
    }

    @Override
    public void helpEmailPasswordScreen(String email) {
        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_EMAIL_PASSWORD);
    }

    @Override
    public void help2FaScreen(String email) {
        launchHelpshift(null, email, true, Tag.ORIGIN_LOGIN_2FA);
    }

    @Override
    public void startPostLoginServices() {
        // Get reader tags so they're available as soon as the Reader is accessed - done for
        // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
        // uses the application context since the activity is finished immediately below
        ReaderUpdateService.startService(getApplicationContext(), EnumSet.of(ReaderUpdateService.UpdateTask.TAGS));

        // Start Notification service
        NotificationsUpdateService.startService(getApplicationContext());
    }

    @Override
    public void helpUsernamePassword(String url, String username, boolean isWpcom) {
        launchHelpshift(url, username, isWpcom, Tag.ORIGIN_LOGIN_USERNAME_PASSWORD);
    }

    @Override
    public void setHelpContext(String faqId, String faqSection) {
        // nothing implemented here yet. This will set the context the `help()` callback should work with
    }

    // SmartLock

    @Override
    public void saveCredentials(@NonNull final String username, @NonNull final String password,
                                @NonNull final String displayName, @Nullable final Uri profilePicture) {
        mSmartLockHelper.saveCredentialsInSmartLock(username, password, displayName, profilePicture);
    }

    // Analytics

    @Override
    public void track(AnalyticsTracker.Stat stat) {
        AnalyticsTracker.track(stat);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, Map<String, ?> properties) {
        AnalyticsTracker.track(stat, properties);
    }

    @Override
    public void track(AnalyticsTracker.Stat stat, String errorContext, String errorType, String errorDescription) {
        AnalyticsTracker.track(stat, errorContext, errorType, errorDescription);
    }

    @Override
    public void trackAnalyticsSignIn(AccountStore accountStore, SiteStore siteStore, boolean isWpcomLogin) {
        AnalyticsUtils.trackAnalyticsSignIn(accountStore, siteStore, isWpcomLogin);
    }

    // Injectors

    @Override
    public void inject(Login2FaFragment object) {
        ((WordPress) getApplication()).component().inject(object);
    }

    @Override
    public void inject(LoginEmailFragment object) {
        ((WordPress) getApplication()).component().inject(object);
    }

    @Override
    public void inject(LoginEmailPasswordFragment object) {
        ((WordPress) getApplication()).component().inject(object);
    }

    @Override
    public void inject(LoginSiteAddressHelpDialogFragment object) {
        ((WordPress) getApplication()).component().inject(object);
    }

    @Override
    public void inject(LoginUsernamePasswordFragment object) {
        ((WordPress) getApplication()).component().inject(object);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AppLog.d(AppLog.T.NUX, "Connection result: " + connectionResult);
    }

    @Override
    public void onConnected(Bundle bundle) {
        AppLog.d(AppLog.T.NUX, "Google API client connected");

        if (mSmartLockCompleted) {
            return;
        }

        // force account chooser
        mSmartLockHelper.disableAutoSignIn();

        mSmartLockHelper.smartLockAutoFill(this);
    }

    @Override
    public void onCredentialRetrieved(Credential credential) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_FILLED);

        mSmartLockCompleted = true;

        final String username = credential.getId();
        final String password = credential.getPassword();
        jumpToUsernamePassword(username, password);
    }

    @Override
    public void onCredentialsUnavailable() {
        mSmartLockCompleted = true;

        startLogin();
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLog.d(AppLog.T.NUX, "Google API client connection suspended");
    }
}
