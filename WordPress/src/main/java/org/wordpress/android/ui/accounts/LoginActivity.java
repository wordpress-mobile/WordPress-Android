package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.login.GoogleFragment.GoogleListener;
import org.wordpress.android.login.Login2FaFragment;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.login.LoginEmailFragment;
import org.wordpress.android.login.LoginEmailPasswordFragment;
import org.wordpress.android.login.LoginGoogleFragment;
import org.wordpress.android.login.LoginListener;
import org.wordpress.android.login.LoginMagicLinkRequestFragment;
import org.wordpress.android.login.LoginMagicLinkSentFragment;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.login.LoginSiteAddressFragment;
import org.wordpress.android.login.LoginUsernamePasswordFragment;
import org.wordpress.android.login.SignupBottomSheetDialogFragment;
import org.wordpress.android.login.SignupBottomSheetDialogFragment.SignupSheetListener;
import org.wordpress.android.login.SignupEmailFragment;
import org.wordpress.android.login.SignupGoogleFragment;
import org.wordpress.android.login.SignupMagicLinkFragment;
import org.wordpress.android.support.ZendeskExtraTags;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.JetpackConnectionSource;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.accounts.SmartLockHelper.Callback;
import org.wordpress.android.ui.accounts.login.LoginPrologueFragment;
import org.wordpress.android.ui.accounts.login.LoginPrologueListener;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.CrashLoggingUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.WPUrlUtils;
import org.wordpress.android.widgets.WPSnackbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;

public class LoginActivity extends LocaleAwareActivity implements ConnectionCallbacks, OnConnectionFailedListener,
        Callback, LoginListener, GoogleListener, LoginPrologueListener, SignupSheetListener,
        HasSupportFragmentInjector, BasicDialogPositiveClickInterface {
    public static final String ARG_JETPACK_CONNECT_SOURCE = "ARG_JETPACK_CONNECT_SOURCE";
    public static final String MAGIC_LOGIN = "magic-login";
    public static final String TOKEN_PARAMETER = "token";

    private static final String KEY_SIGNUP_SHEET_DISPLAYED = "KEY_SIGNUP_SHEET_DISPLAYED";
    private static final String KEY_SMARTLOCK_HELPER_STATE = "KEY_SMARTLOCK_HELPER_STATE";
    private static final String KEY_SIGNUP_FROM_LOGIN_ENABLED = "KEY_SIGNUP_FROM_LOGIN_ENABLED";

    private static final String FORGOT_PASSWORD_URL_SUFFIX = "wp-login.php?action=lostpassword";

    private static final String GOOGLE_ERROR_DIALOG_TAG = "google_error_dialog_tag";

    private enum SmartLockHelperState {
        NOT_TRIGGERED,
        TRIGGER_FILL_IN_ON_CONNECT,
        FINISH_ON_CONNECT,
        FINISHED
    }

    private SmartLockHelper mSmartLockHelper;
    private SmartLockHelperState mSmartLockHelperState = SmartLockHelperState.NOT_TRIGGERED;
    private JetpackConnectionSource mJetpackConnectSource;
    private boolean mIsJetpackConnect;

    private boolean mIsSignupFromLoginEnabled;
    private boolean mIsSmartLockTriggeredFromPrologue;

    private LoginMode mLoginMode;

    @Inject DispatchingAndroidInjector<Fragment> mFragmentInjector;
    @Inject protected LoginAnalyticsListener mLoginAnalyticsListener;
    @Inject ZendeskHelper mZendeskHelper;
    @Inject protected SiteStore mSiteStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((WordPress) getApplication()).component().inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            if (getIntent() != null) {
                mJetpackConnectSource =
                        (JetpackConnectionSource) getIntent().getSerializableExtra(ARG_JETPACK_CONNECT_SOURCE);
            }

            mLoginAnalyticsListener.trackLoginAccessed();

            switch (getLoginMode()) {
                case FULL:
                case WPCOM_LOGIN_ONLY:
                    mIsSignupFromLoginEnabled = true;
                    showFragment(new LoginPrologueFragment(), LoginPrologueFragment.TAG);
                    if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
                        mIsSmartLockTriggeredFromPrologue = true;
                        initSmartLockIfNotFinished(true);
                    }
                    break;
                case SELFHOSTED_ONLY:
                    showFragment(new LoginSiteAddressFragment(), LoginSiteAddressFragment.TAG);
                    break;
                case JETPACK_STATS:
                    mIsSignupFromLoginEnabled = true;
                    checkSmartLockPasswordAndStartLogin();
                    break;
                case WPCOM_LOGIN_DEEPLINK:
                case WPCOM_REAUTHENTICATE:
                case SHARE_INTENT:
                    checkSmartLockPasswordAndStartLogin();
                    break;
            }
        } else {
            mSmartLockHelperState = SmartLockHelperState.valueOf(
                    savedInstanceState.getString(KEY_SMARTLOCK_HELPER_STATE));

            if (mSmartLockHelperState != SmartLockHelperState.NOT_TRIGGERED) {
                // reconnect SmartLockHelper
                initSmartLockHelperConnection();
            }

            mIsSignupFromLoginEnabled = savedInstanceState.getBoolean(KEY_SIGNUP_FROM_LOGIN_ENABLED);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_SMARTLOCK_HELPER_STATE, mSmartLockHelperState.name());
        outState.putBoolean(KEY_SIGNUP_FROM_LOGIN_ENABLED, mIsSignupFromLoginEnabled);
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

    private void loggedInAndFinish(ArrayList<Integer> oldSitesIds, boolean doLoginUpdate) {
        switch (getLoginMode()) {
            case FULL:
            case WPCOM_LOGIN_ONLY:
                if (!mSiteStore.hasSite() && AppPrefs.shouldShowPostSignupInterstitial() && !doLoginUpdate) {
                    ActivityLauncher.showPostSignupInterstitial(this);
                } else {
                    ActivityLauncher.showMainActivityAndLoginEpilogue(this, oldSitesIds, doLoginUpdate);
                }
                setResult(Activity.RESULT_OK);
                finish();
                break;
            case JETPACK_STATS:
                ActivityLauncher.showLoginEpilogueForResult(this, true, oldSitesIds, true);
                break;
            case WPCOM_LOGIN_DEEPLINK:
            case WPCOM_REAUTHENTICATE:
                ActivityLauncher.showLoginEpilogueForResult(this, true, oldSitesIds, false);
                break;
            case SHARE_INTENT:
            case SELFHOSTED_ONLY:
                // We are comparing list of site ID's before self-hosted site was added and after, trying to find a
                // newly added self-hosted site's ID, so we can select it
                ArrayList<Integer> newSitesIds = new ArrayList<>();
                for (SiteModel site : mSiteStore.getSites()) {
                    newSitesIds.add(site.getId());
                }
                newSitesIds.removeAll(oldSitesIds);

                if (newSitesIds.size() > 0) {
                    Intent intent = new Intent();
                    intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, newSitesIds.get(0));
                    setResult(Activity.RESULT_OK, intent);
                } else {
                    AppLog.e(T.MAIN, "Couldn't detect newly added self-hosted site. "
                                     + "Expected at least 1 site ID but was 0.");
                    ToastUtils.showToast(this, R.string.site_picker_failed_selecting_added_site);
                    setResult(Activity.RESULT_OK);
                }

                // skip the epilogue when only added a self-hosted site or sharing to WordPress
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        AppLog.d(T.MAIN, "LoginActivity: onActivity Result - requestCode" + requestCode);
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.SHOW_LOGIN_EPILOGUE_AND_RETURN:
            case RequestCodes.SHOW_SIGNUP_EPILOGUE_AND_RETURN:
                // we showed the epilogue screen as informational and sites got loaded so, just
                // return to login caller now
                setResult(RESULT_OK);
                finish();
                break;
            case RequestCodes.SMART_LOCK_SAVE:
                if (resultCode == RESULT_OK) {
                    mLoginAnalyticsListener.trackLoginAutofillCredentialsUpdated();
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

    private boolean initSmartLockHelperConnection() {
        mSmartLockHelper = new SmartLockHelper(this);
        return mSmartLockHelper.initSmartLockForPasswords();
    }

    private void checkSmartLockPasswordAndStartLogin() {
        initSmartLockIfNotFinished(true);

        if (mSmartLockHelperState == SmartLockHelperState.FINISHED) {
            startLogin();
        }
    }

    /**
     * @param triggerFillInOnConnect set to true, if you want to show an account chooser dialog when the user has
     *                               stored their credentials in the past. Set to false, if you just want to
     *                               initialize SmartLock eg. when you want to use it just to save users credentials.
     */
    private void initSmartLockIfNotFinished(boolean triggerFillInOnConnect) {
        if (mSmartLockHelperState == SmartLockHelperState.NOT_TRIGGERED) {
            if (initSmartLockHelperConnection()) {
                if (triggerFillInOnConnect) {
                    mSmartLockHelperState = SmartLockHelperState.TRIGGER_FILL_IN_ON_CONNECT;
                } else {
                    mSmartLockHelperState = SmartLockHelperState.FINISH_ON_CONNECT;
                }
            } else {
                // just shortcircuit the attempt to use SmartLockHelper
                mSmartLockHelperState = SmartLockHelperState.FINISHED;
            }
        }
    }

    private void startLogin() {
        if (getLoginEmailFragment() != null) {
            // email screen is already shown so, login has already started. Just bail.
            return;
        }

        if (getLoginPrologueFragment() == null) {
            // prologue fragment is not shown so, the email screen will be the initial screen on the fragment container
            showFragment(LoginEmailFragment.newInstance(mIsSignupFromLoginEnabled), LoginEmailFragment.TAG);

            if (getLoginMode() == LoginMode.JETPACK_STATS) {
                mIsJetpackConnect = true;
            }
        } else {
            // prologue fragment is shown so, slide in the email screen (and add to history)
            slideInFragment(LoginEmailFragment.newInstance(mIsSignupFromLoginEnabled), true, LoginEmailFragment.TAG);
        }
    }

    // LoginPrologueListener implementation methods

    @Override
    public void showEmailLoginScreen() {
        checkSmartLockPasswordAndStartLogin();
    }

    @Override
    public void doStartSignup() {
        // This stat is part of a funnel that provides critical information.  Before
        // making ANY modification to this stat please refer to: p4qSXL-35X-p2
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_BUTTON_TAPPED);
        SignupBottomSheetDialogFragment signupFragment = SignupBottomSheetDialogFragment.newInstance();
        signupFragment.show(getSupportFragmentManager(), SignupBottomSheetDialogFragment.TAG);
    }

    @Override
    public void onSignupSheetCanceled() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_CANCELED);
    }

    @Override
    public void onSignupSheetEmailClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATE_ACCOUNT_INITIATED);
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_BUTTON_TAPPED);
        dismissSignupSheet();
        slideInFragment(new SignupEmailFragment(), true, SignupEmailFragment.TAG);
    }

    @Override
    public void onSignupSheetGoogleClicked() {
        dismissSignupSheet();
        AnalyticsTracker.track(AnalyticsTracker.Stat.CREATE_ACCOUNT_INITIATED);
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_BUTTON_TAPPED);

        if (NetworkUtils.checkConnection(this)) {
            SignupGoogleFragment signupGoogleFragment;
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            signupGoogleFragment = new SignupGoogleFragment();
            signupGoogleFragment.setRetainInstance(true);
            fragmentTransaction.add(signupGoogleFragment, SignupGoogleFragment.TAG);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void onSignupSheetTermsOfServiceClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_TERMS_OF_SERVICE_TAPPED);
        ActivityLauncher.openUrlExternal(this, WPUrlUtils.buildTermsOfServiceUrl(this));
    }

    @Override
    public void loggedInViaSignup(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds, false);
    }

    @Override
    public void newUserCreatedButErrored(String email, String password) {
        LoginEmailPasswordFragment loginEmailPasswordFragment =
                LoginEmailPasswordFragment.newInstance(email, password, null, null, false);
        slideInFragment(loginEmailPasswordFragment, false, LoginEmailPasswordFragment.TAG);
    }

    // LoginListener implementation methods

    @Override
    public void gotWpcomEmail(String email, boolean verifyEmail) {
        initSmartLockIfNotFinished(false);
        if (getLoginMode() != LoginMode.WPCOM_LOGIN_DEEPLINK && getLoginMode() != LoginMode.SHARE_INTENT) {
            LoginMagicLinkRequestFragment loginMagicLinkRequestFragment = LoginMagicLinkRequestFragment.newInstance(
                    email, AuthEmailPayloadScheme.WORDPRESS, mIsJetpackConnect,
                    mJetpackConnectSource != null ? mJetpackConnectSource.toString() : null, verifyEmail);
            slideInFragment(loginMagicLinkRequestFragment, true, LoginMagicLinkRequestFragment.TAG);
        } else {
            LoginEmailPasswordFragment loginEmailPasswordFragment =
                    LoginEmailPasswordFragment.newInstance(email, null, null, null, false);
            slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
        }
    }

    @Override
    public void loginViaSiteAddress() {
        LoginSiteAddressFragment loginSiteAddressFragment = new LoginSiteAddressFragment();
        slideInFragment(loginSiteAddressFragment, true, LoginSiteAddressFragment.TAG);
    }

    @Override
    public void loginViaSocialAccount(String email, String idToken, String service, boolean isPasswordRequired) {
        dismissSignupSheet();
        LoginEmailPasswordFragment loginEmailPasswordFragment =
                LoginEmailPasswordFragment.newInstance(email, null, idToken, service, isPasswordRequired);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
    }

    @Override
    public void loggedInViaSocialAccount(ArrayList<Integer> oldSitesIds, boolean doLoginUpdate) {
        mLoginAnalyticsListener.trackLoginSocialSuccess();
        loggedInAndFinish(oldSitesIds, doLoginUpdate);
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
    public void showSignupMagicLink(String email) {
        SignupMagicLinkFragment signupMagicLinkFragment = SignupMagicLinkFragment.newInstance(email, mIsJetpackConnect,
                mJetpackConnectSource != null ? mJetpackConnectSource.toString() : null);
        slideInFragment(signupMagicLinkFragment, true, SignupMagicLinkFragment.TAG);
    }

    @Override
    public void openEmailClient(boolean isLogin) {
        if (WPActivityUtils.isEmailClientAvailable(this)) {
            if (isLogin) {
                mLoginAnalyticsListener.trackLoginMagicLinkOpenEmailClientClicked();
            } else {
                mLoginAnalyticsListener.trackSignupMagicLinkOpenEmailClientClicked();
            }

            WPActivityUtils.openEmailClient(this);
        } else {
            ToastUtils.showToast(this, R.string.login_email_client_not_found);
        }
    }

    @Override
    public void usePasswordInstead(String email) {
        mLoginAnalyticsListener.trackLoginMagicLinkExited();
        LoginEmailPasswordFragment loginEmailPasswordFragment =
                LoginEmailPasswordFragment.newInstance(email, null, null, null, false);
        slideInFragment(loginEmailPasswordFragment, true, LoginEmailPasswordFragment.TAG);
    }

    @Override
    public void forgotPassword(String url) {
        mLoginAnalyticsListener.trackLoginForgotPasswordClicked();
        ActivityLauncher.openUrlExternal(this, url + FORGOT_PASSWORD_URL_SUFFIX);
    }

    @Override
    public void needs2fa(String email, String password) {
        Login2FaFragment login2FaFragment = Login2FaFragment.newInstance(email, password);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void needs2faSocial(String email, String userId, String nonceAuthenticator, String nonceBackup,
                               String nonceSms) {
        dismissSignupSheet();
        mLoginAnalyticsListener.trackLoginSocial2faNeeded();
        Login2FaFragment login2FaFragment = Login2FaFragment.newInstanceSocial(email, userId,
                nonceAuthenticator, nonceBackup,
                nonceSms);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void needs2faSocialConnect(String email, String password, String idToken, String service) {
        mLoginAnalyticsListener.trackLoginSocial2faNeeded();
        Login2FaFragment login2FaFragment =
                Login2FaFragment.newInstanceSocialConnect(email, password, idToken, service);
        slideInFragment(login2FaFragment, true, Login2FaFragment.TAG);
    }

    @Override
    public void loggedInViaPassword(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds, false);
    }

    @Override
    public void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds) {
        ToastUtils.showToast(this, R.string.already_logged_in_wpcom, ToastUtils.Duration.LONG);
        loggedInAndFinish(oldSitesIds, false);
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
    public void handleSslCertificateError(MemorizingTrustManager memorizingTrustManager,
                                          final SelfSignedSSLCallback callback) {
        SelfSignedSSLUtils.showSSLWarningDialog(this, memorizingTrustManager, new SelfSignedSSLUtils.Callback() {
            @Override
            public void certificateTrusted() {
                callback.certificateTrusted();
            }
        });
    }

    private void viewHelpAndSupport(Origin origin) {
        List<String> extraSupportTags = getLoginMode() == LoginMode.JETPACK_STATS ? Collections
                .singletonList(ZendeskExtraTags.connectingJetpack) : null;
        ActivityLauncher.viewHelpAndSupport(this, origin, null, extraSupportTags);
    }

    @Override
    public void helpSiteAddress(String url) {
        viewHelpAndSupport(Origin.LOGIN_SITE_ADDRESS);
    }

    @Override
    public void helpFindingSiteAddress(String username, SiteStore siteStore) {
        mZendeskHelper.createNewTicket(this, Origin.LOGIN_SITE_ADDRESS, null);
    }

    @Override
    public void loggedInViaUsernamePassword(ArrayList<Integer> oldSitesIds) {
        loggedInAndFinish(oldSitesIds, false);
    }

    @Override
    public void helpEmailScreen(String email) {
        viewHelpAndSupport(Origin.LOGIN_EMAIL);
    }

    @Override
    public void helpSignupEmailScreen(String email) {
        viewHelpAndSupport(Origin.SIGNUP_EMAIL);
    }

    @Override
    public void helpSignupMagicLinkScreen(String email) {
        viewHelpAndSupport(Origin.SIGNUP_MAGIC_LINK);
    }

    @Override
    public void helpSocialEmailScreen(String email) {
        viewHelpAndSupport(Origin.LOGIN_SOCIAL);
    }

    @Override
    public void addGoogleLoginFragment() {
        LoginGoogleFragment loginGoogleFragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        loginGoogleFragment = new LoginGoogleFragment();
        loginGoogleFragment.setRetainInstance(true);
        fragmentTransaction.add(loginGoogleFragment, LoginGoogleFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public void helpMagicLinkRequest(String email) {
        viewHelpAndSupport(Origin.LOGIN_MAGIC_LINK);
    }

    @Override
    public void helpMagicLinkSent(String email) {
        viewHelpAndSupport(Origin.LOGIN_MAGIC_LINK);
    }

    @Override
    public void helpEmailPasswordScreen(String email) {
        viewHelpAndSupport(Origin.LOGIN_EMAIL_PASSWORD);
    }

    @Override
    public void help2FaScreen(String email) {
        viewHelpAndSupport(Origin.LOGIN_2FA);
    }

    @Override
    public void startPostLoginServices() {
        // Get reader tags so they're available as soon as the Reader is accessed - done for
        // both wp.com and self-hosted (self-hosted = "logged out" reader) - note that this
        // uses the application context since the activity is finished immediately below
        ReaderUpdateServiceStarter.startService(getApplicationContext(), EnumSet.of(ReaderUpdateLogic.UpdateTask.TAGS));

        // Start Notification service
        NotificationsUpdateServiceStarter.startService(getApplicationContext());
    }

    @Override
    public void helpUsernamePassword(String url, String username, boolean isWpcom) {
        viewHelpAndSupport(Origin.LOGIN_USERNAME_PASSWORD);
    }

    // SmartLock

    @Override
    public void saveCredentialsInSmartLock(@Nullable final String username, @Nullable final String password,
                                           @NonNull final String displayName, @Nullable final Uri profilePicture) {
        if (getLoginMode() == LoginMode.SELFHOSTED_ONLY) {
            // bail if we are on the selfhosted flow since we haven't initialized SmartLock-for-Passwords for it.
            // Otherwise, logging in to WPCOM via the site-picker flow (for example) results in a crash.
            // See https://github.com/wordpress-mobile/WordPress-Android/issues/7182#issuecomment-362791364
            // There might be more circumstances that lead to this crash though. Not all crash reports seem to
            // originate from the site-picker.
            return;
        }

        if (mSmartLockHelper == null) {
            // log some data to help us debug https://github.com/wordpress-mobile/WordPress-Android/issues/7182
            final String loginModeStr = "LoginMode: " + (getLoginMode() != null ? getLoginMode().name() : "null");
            AppLog.w(AppLog.T.NUX, "Internal inconsistency error! mSmartLockHelper found null!" + loginModeStr);
            CrashLoggingUtils.logException(
                    new RuntimeException("Internal inconsistency error! mSmartLockHelper found null!"),
                    AppLog.T.NUX,
                    loginModeStr);

            // bail
            return;
        }

        mSmartLockHelper.saveCredentialsInSmartLock(StringUtils.notNullStr(username), StringUtils.notNullStr(password),
                displayName, profilePicture);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AppLog.d(AppLog.T.NUX, "Connection result: " + connectionResult);
        mSmartLockHelperState = SmartLockHelperState.FINISHED;
    }

    @Override
    public void onConnected(Bundle bundle) {
        AppLog.d(AppLog.T.NUX, "Google API client connected");

        switch (mSmartLockHelperState) {
            case NOT_TRIGGERED:
                // should not reach this state here!
                throw new RuntimeException("Internal inconsistency error!");
            case TRIGGER_FILL_IN_ON_CONNECT:
                mSmartLockHelperState = SmartLockHelperState.FINISHED;

                // force account chooser
                mSmartLockHelper.disableAutoSignIn();

                mSmartLockHelper.smartLockAutoFill(this);
                break;
            case FINISH_ON_CONNECT:
                mSmartLockHelperState = SmartLockHelperState.FINISHED;
                break;
            case FINISHED:
                // don't do anything special. We're reconnecting the GoogleApiClient on rotation.
                break;
        }
    }

    @Override
    public void onCredentialRetrieved(Credential credential) {
        mLoginAnalyticsListener.trackLoginAutofillCredentialsFilled();

        mSmartLockHelperState = SmartLockHelperState.FINISHED;

        final String username = credential.getId();
        final String password = credential.getPassword();
        jumpToUsernamePassword(username, password);
    }

    @Override
    public void onCredentialsUnavailable() {
        mSmartLockHelperState = SmartLockHelperState.FINISHED;
        if (mIsSmartLockTriggeredFromPrologue) {
            return;
        }
        startLogin();
    }

    @Override
    public void onConnectionSuspended(int i) {
        AppLog.d(AppLog.T.NUX, "Google API client connection suspended");
    }

    @Override
    public void showSignupToLoginMessage() {
        WPSnackbar.make(
                findViewById(R.id.main_view),
                R.string.signup_user_exists,
                Snackbar.LENGTH_LONG
        ).show();
    }

    // GoogleListener

    @Override
    public void onGoogleEmailSelected(String email) {
        LoginEmailFragment loginEmailFragment =
                (LoginEmailFragment) getSupportFragmentManager().findFragmentByTag(LoginEmailFragment.TAG);
        loginEmailFragment.setGoogleEmail(email);
    }

    @Override
    public void onGoogleLoginFinished() {
        LoginEmailFragment loginEmailFragment =
                (LoginEmailFragment) getSupportFragmentManager().findFragmentByTag(LoginEmailFragment.TAG);
        loginEmailFragment.finishLogin();
    }

    @Override
    public void onGoogleSignupFinished(String name, String email, String photoUrl, String username) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_SUCCESS);
        if (mIsJetpackConnect) {
            ActivityLauncher.showSignupEpilogueForResult(this, name, email, photoUrl, username, false);
        } else {
            ActivityLauncher.showMainActivityAndSignupEpilogue(this, name, email, photoUrl, username);
        }

        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void onGoogleSignupError(String msg) {
        // Only show the error dialog if the activity is still active
        if (!getSupportFragmentManager().isStateSaved()) {
            BasicFragmentDialog dialog = new BasicFragmentDialog();
            dialog.initialize(GOOGLE_ERROR_DIALOG_TAG, getString(R.string.error),
                    msg,
                    getString(org.wordpress.android.login.R.string.login_error_button),
                    null,
                    null);
            dialog.show(getSupportFragmentManager(), GOOGLE_ERROR_DIALOG_TAG);
        } else {
            AppLog.d(T.MAIN, "'Google sign up failed' dialog not shown, because the activity wasn't visible.");
        }
    }

    @Override
    public void onPositiveClicked(@NotNull String instanceTag) {
        switch (instanceTag) {
            case GOOGLE_ERROR_DIALOG_TAG:
                // just dismiss the dialog
                break;
        }
    }

    private void dismissSignupSheet() {
        SignupBottomSheetDialogFragment signupFragment =
                (SignupBottomSheetDialogFragment) getSupportFragmentManager()
                        .findFragmentByTag(SignupBottomSheetDialogFragment.TAG);

        if (signupFragment != null) {
            signupFragment.dismiss();
        }
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return mFragmentInjector;
    }

    @Override
    public void showHelpFindingConnectedEmail() {
        // Not used in WordPress app
    }

    @Override
    public void gotConnectedSiteInfo(
            @NonNull String siteAddress,
            @Nullable String redirectUrl,
            boolean hasJetpack) {
        // Not used in WordPress app
    }

    @Override
    public void helpHandleDiscoveryError(
            String siteAddress,
            String endpointAddress,
            String username,
            String password,
            String userAvatarUrl,
            int errorMessage) {
        // Not used in WordPress app
    }

    @Override
    public void helpNoJetpackScreen(
            String siteAddress,
            String endpointAddress,
            String username,
            String password,
            String userAvatarUrl,
            Boolean checkJetpackAvailability) {
        // Not used in WordPress app
    }

    @Override
    public void loginViaSiteCredentials(String inputSiteAddress) {
        // Not used in WordPress app
    }
}
