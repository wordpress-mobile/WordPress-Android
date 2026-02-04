package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.network.MemorizingTrustManager;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.login.LoginAnalyticsListener;
import org.wordpress.android.login.LoginListener;
import org.wordpress.android.login.LoginMode;
import org.wordpress.android.ui.accounts.login.applicationpassword.LoginSiteApplicationPasswordFragment;
import org.wordpress.android.support.ZendeskExtraTags;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Source;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
import org.wordpress.android.ui.accounts.login.LoginCompletionUseCase;
import org.wordpress.android.ui.accounts.login.LoginCompletionUseCase.LoginCompletionAction;
import org.wordpress.android.ui.accounts.login.LoginCompletionUseCase.MainNavigationDestination;
import org.wordpress.android.ui.accounts.login.LoginPrologueListener;
import org.wordpress.android.ui.accounts.login.LoginPrologueRevampedFragment;
import org.wordpress.android.ui.accounts.login.WPcomLoginHelper;
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesFragment;
import org.wordpress.android.ui.accounts.login.jetpack.LoginSiteCheckErrorFragment;
import org.wordpress.android.ui.main.BaseAppCompatActivity;
import org.wordpress.android.ui.main.ChooseSiteActivity;
import org.wordpress.android.ui.notifications.services.NotificationsUpdateServiceStarter;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SelfSignedSSLUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import dagger.hilt.android.AndroidEntryPoint;

import static org.wordpress.android.util.ActivityUtils.hideKeyboard;

@AndroidEntryPoint
public class LoginActivity extends BaseAppCompatActivity implements LoginListener, LoginPrologueListener,
        HasAndroidInjector, BasicDialogPositiveClickInterface {
    public static final String ARG_JETPACK_CONNECT_SOURCE = "ARG_JETPACK_CONNECT_SOURCE";
    public static final String MAGIC_LOGIN = "magic-login";
    public static final String TOKEN_PARAMETER = "token";

    private static final String KEY_UNIFIED_TRACKER_SOURCE = "KEY_UNIFIED_TRACKER_SOURCE";
    private static final String KEY_UNIFIED_TRACKER_FLOW = "KEY_UNIFIED_TRACKER_FLOW";

    // Static field to preserve login mode across OAuth flow (when callback creates new activity)
    private static LoginMode sPendingLoginMode;

    // Static field to track if we're in a share flow (for self-hosted login via ApplicationPasswordLoginActivity)
    private static boolean sIsShareFlowPending;

    /**
     * Check if there's a pending share flow. Used by ApplicationPasswordLoginActivity
     * to determine whether to navigate to main activity or just finish.
     */
    public static boolean consumeShareFlowPending() {
        boolean result = sIsShareFlowPending;
        sIsShareFlowPending = false;
        return result;
    }

    private LoginMode mLoginMode;
    private LoginViewModel mViewModel;
    @Inject protected WPcomLoginHelper mLoginHelper;

    @Inject DispatchingAndroidInjector<Object> mDispatchingAndroidInjector;
    @Inject protected LoginAnalyticsListener mLoginAnalyticsListener;
    @Inject UnifiedLoginTracker mUnifiedLoginTracker;
    @Inject protected SiteStore mSiteStore;
    @Inject protected AccountStore mAccountStore;
    @Inject protected Dispatcher mDispatcher;
    @Inject protected ViewModelProvider.Factory mViewModelFactory;

    // Flag to track when we're waiting for account/sites to load after OAuth login
    private boolean mIsWaitingForSitesToLoad = false;
    private ArrayList<Integer> mOldSitesIdsForLoginUpdate;

    @Inject ExperimentalFeatures mExperimentalFeatures;
    @Inject LoginCompletionUseCase mLoginCompletionUseCase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Attempt Login if this activity was created in response to a user confirming login, and if
        // successful clear the intent so we don't reuse the OAuth code if the activity is recreated
        boolean loginProcessed = mLoginHelper.tryLoginWithDataString(getIntent().getDataString());

        if (loginProcessed) {
            getIntent().setData(null);
            // OAuth login successful - show loading UI and finish the login flow
            setContentView(R.layout.login_loading);
            this.loggedInAndFinish(new ArrayList<Integer>(), true);
            return;
        } else {
            // Not an OAuth callback - clear any pending login mode from a previous flow
            sPendingLoginMode = null;
        }

        // Start preloading the WordPress.com login page if needed – this avoids visual hitches
        // when displaying that screen
        mLoginHelper.bindCustomTabsService(this);

        // go no further if the user is already logged in and this is the login screen shown at startup
        //      FULL = WPAndroid
        //      JETPACK_LOGIN_ONLY = JPAndroid
        LoginMode loginMode = getLoginMode();
        if ((mLoginHelper.isLoggedIn()) && (loginMode == LoginMode.FULL || loginMode == LoginMode.JETPACK_LOGIN_ONLY)) {
            // Show loading UI while we fetch account and sites in the background
            setContentView(R.layout.login_loading);
            this.loggedInAndFinish(new ArrayList<Integer>(), true);
            return;
        }

        LoginFlowThemeHelper.injectMissingCustomAttributes(getTheme());

        setContentView(R.layout.login_activity);

        if (savedInstanceState == null) {
            mLoginAnalyticsListener.trackLoginAccessed();

            switch (loginMode) {
                case FULL:
                case JETPACK_LOGIN_ONLY:
                    mUnifiedLoginTracker.setSource(Source.DEFAULT);
                    showFragment(new LoginPrologueRevampedFragment(), LoginPrologueRevampedFragment.TAG);
                    break;
                case WPCOM_LOGIN_ONLY:
                case JETPACK_REST_CONNECT:
                    mUnifiedLoginTracker.setSource(Source.ADD_WORDPRESS_COM_ACCOUNT);
                    showWPcomLoginScreen(this);
                    break;
                case SELFHOSTED_ONLY:
                    mUnifiedLoginTracker.setSource(Source.SELF_HOSTED);
                    showFragment(new LoginSiteApplicationPasswordFragment(), LoginSiteApplicationPasswordFragment.TAG);
                    break;
                case JETPACK_STATS:
                    mUnifiedLoginTracker.setSource(Source.JETPACK);
                    showWPcomLoginScreen(this);
                    break;
                case WPCOM_LOGIN_DEEPLINK:
                    mUnifiedLoginTracker.setSource(Source.DEEPLINK);
                    showWPcomLoginScreen(this);
                    break;
                case WPCOM_REAUTHENTICATE:
                    mUnifiedLoginTracker.setSource(Source.REAUTHENTICATION);
                    showWPcomLoginScreen(this);
                    break;
                case SHARE_INTENT:
                    mUnifiedLoginTracker.setSource(Source.SHARE);
                    showFragment(new LoginPrologueRevampedFragment(), LoginPrologueRevampedFragment.TAG);
                    break;
            }
        } else {
            String source = savedInstanceState.getString(KEY_UNIFIED_TRACKER_SOURCE);
            if (source != null) {
                mUnifiedLoginTracker.setSource(source);
            }
            mUnifiedLoginTracker.setFlow(savedInstanceState.getString(KEY_UNIFIED_TRACKER_FLOW));
        }

        initViewModel();
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(LoginViewModel.class);

        // initObservers
        mViewModel.getNavigationEvents().observe(this, event -> {
            LoginNavigationEvents loginEvent = event.getContentIfNotHandled();
            if (loginEvent instanceof ShowSiteAddressError) {
                showSiteAddressError((ShowSiteAddressError) loginEvent);
            } else if (loginEvent instanceof ShowNoJetpackSites) {
                showNoJetpackSites();
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_UNIFIED_TRACKER_SOURCE, mUnifiedLoginTracker.getSource().getValue());
        Flow flow = mUnifiedLoginTracker.getFlow();
        if (flow != null) {
            outState.putString(KEY_UNIFIED_TRACKER_FLOW, flow.getValue());
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Handle OAuth callback when activity is reused (singleTop)
        boolean loginProcessed = mLoginHelper.tryLoginWithDataString(intent.getDataString());
        if (loginProcessed) {
            intent.setData(null);
            setContentView(R.layout.login_loading);
            this.loggedInAndFinish(new ArrayList<Integer>(), true);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mDispatcher.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDispatcher.unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if self-hosted login completed while in share flow
        // ApplicationPasswordLoginActivity finishes back here after successful login
        if (getLoginMode() == LoginMode.SHARE_INTENT && mSiteStore.hasSite()) {
            setResult(Activity.RESULT_OK);
            finish();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (mIsWaitingForSitesToLoad && mAccountStore.hasAccessToken()) {
            if (event.isError()) {
                AppLog.e(T.MAIN, "Account fetch failed: " + event.error.type + " - " + event.error.message);
                String errMsg = this.getString(R.string.error_fetching_account, event.error.message);
                ToastUtils.showToast(this, errMsg, Duration.LONG);
            } else if (event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                // Account fetched, now fetch sites
                AppLog.i(T.MAIN, "Account fetched, now fetching sites");
                mDispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(SiteUtils.getFetchSitesPayload()));
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        if (mIsWaitingForSitesToLoad) {
            if (event.isError()) {
                AppLog.e(T.MAIN, "Site fetch failed: " + event.error.type);
            }
            // Sites loaded (or failed), proceed to main activity
            AppLog.i(T.MAIN, "Sites loaded, proceeding to main activity");
            finishLoginAfterSitesLoaded();
        }
    }

    private void finishLoginAfterSitesLoaded() {
        mIsWaitingForSitesToLoad = false;
        navigateToMainActivityOrFinish();
    }

    /**
     * Navigates to the main activity and finishes the login flow.
     * This is the common exit point for successful logins.
     */
    private void navigateToMainActivityOrFinish() {
        MainNavigationDestination destination =
                mLoginCompletionUseCase.getMainNavigationDestination(getLoginMode());
        switch (destination) {
            case MAIN_ACTIVITY:
                // Select the primary site after WP.com login
                ActivityLauncher.showMainActivity(this, false, true);
                break;
            case FINISH_ONLY:
            default:
                // For other modes (JETPACK_STATS, JETPACK_REST_CONNECT, WPCOM_LOGIN_DEEPLINK,
                // WPCOM_REAUTHENTICATE, etc.), just finish and let the caller handle navigation
                break;
        }
        setResult(Activity.RESULT_OK);
        finish();
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
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

        // If the mode is FULL (default) but we have a pending mode from an OAuth flow, use that instead
        if (mLoginMode == LoginMode.FULL && sPendingLoginMode != null) {
            mLoginMode = sPendingLoginMode;
            sPendingLoginMode = null; // Clear after use
        }

        return mLoginMode;
    }

    private void loggedInAndFinish(ArrayList<Integer> oldSitesIds, boolean doLoginUpdate) {
        AppPrefs.setIsJetpackMigrationEligible(false);
        AppPrefs.setIsJetpackMigrationInProgress(false);

        // If doLoginUpdate is true, we need to fetch account and sites before navigating.
        // This happens after WordPress.com OAuth login where we only have the token.
        if (doLoginUpdate) {
            AppLog.i(T.MAIN, "Fetching account and sites before proceeding");
            mIsWaitingForSitesToLoad = true;
            mOldSitesIdsForLoginUpdate = oldSitesIds;
            mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction());
            return; // Wait for onAccountChanged -> onSiteChanged before navigating
        }

        LoginCompletionAction action = mLoginCompletionUseCase.getLoginCompletionAction(getLoginMode());
        switch (action) {
            case FINISH_WITH_NEW_SITE:
                // Handle self-hosted site login - find the newly added site and return its ID
                finishWithNewlyAddedSiteId(oldSitesIds);
                break;
                case FINISH_ONLY:
                // WooCommerce handles its own navigation
                break;
            case NAVIGATE_TO_MAIN:
            default:
                // For all other modes, use the common navigation logic
                navigateToMainActivityOrFinish();
                break;
        }
    }

    /**
     * Finds the newly added self-hosted site and finishes with its ID in the result intent.
     */
    private void finishWithNewlyAddedSiteId(ArrayList<Integer> oldSitesIds) {
        ArrayList<Integer> newSitesIds = new ArrayList<>();
        for (SiteModel site : mSiteStore.getSites()) {
            newSitesIds.add(site.getId());
        }
        newSitesIds.removeAll(oldSitesIds);

        if (!newSitesIds.isEmpty()) {
            Intent intent = new Intent();
            intent.putExtra(ChooseSiteActivity.KEY_SITE_LOCAL_ID, newSitesIds.get(0));
            setResult(Activity.RESULT_OK, intent);
        } else {
            AppLog.e(T.MAIN, "Couldn't detect newly added self-hosted site. "
                             + "Expected at least 1 site ID but was 0.");
            ToastUtils.showToast(this, R.string.site_picker_failed_selecting_added_site);
            setResult(Activity.RESULT_OK);
        }
        finish();
    }

    // LoginPrologueListener implementation methods

    public void showWPcomLoginScreen(@NonNull Context context) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WPCOM_WEBVIEW);
        mUnifiedLoginTracker.setFlowAndStep(Flow.WORDPRESS_COM_WEB, Step.WPCOM_WEB_START);

        // Save the current login mode so it survives the OAuth callback (which creates a new activity)
        sPendingLoginMode = getLoginMode();

        CustomTabsIntent intent = getCustomTabsIntent();

        Uri loginUri = mLoginHelper.getWpcomLoginUri();
        try {
            intent.launchUrl(this, loginUri);
        } catch (SecurityException | ActivityNotFoundException e) {
            AppLog.e(AppLog.T.UTILS, "Error opening login uri in CustomTabsIntent, attempting external browser", e);
            ActivityLauncher.openUrlExternal(this, loginUri.toString());
        }
    }

    @NonNull private CustomTabsIntent getCustomTabsIntent() {
        return new CustomTabsIntent.Builder()
                .setShareState(CustomTabsIntent.SHARE_STATE_OFF)
                .setStartAnimations(this, R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left)
                .setExitAnimations(this, R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right)
                .setUrlBarHidingEnabled(true)
                .setInstantAppsEnabled(false)
                .setShowTitle(false)
                .build();
    }

    // LoginListener implementation methods

    @Override
    public void loginViaSiteAddress() {
        // Track if we're in a share flow so ApplicationPasswordLoginActivity knows to just finish
        if (getLoginMode() == LoginMode.SHARE_INTENT) {
            sIsShareFlowPending = true;
        }
        slideInFragment(new LoginSiteApplicationPasswordFragment(), true, LoginSiteApplicationPasswordFragment.TAG);
    }

    @Override
    public void alreadyLoggedInWpcom(ArrayList<Integer> oldSitesIds) {
        ToastUtils.showToast(this, R.string.already_logged_in_wpcom, ToastUtils.Duration.LONG);
        loggedInAndFinish(oldSitesIds, false);
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

    private void viewHelp(Origin origin) {
        List<String> extraSupportTags = getLoginMode() == LoginMode.JETPACK_STATS ? Collections
                .singletonList(ZendeskExtraTags.connectingJetpack) : null;
        ActivityLauncher.viewHelp(this, origin, null, extraSupportTags, mExperimentalFeatures);
    }

    @Override
    public void helpSiteAddress(String url) {
        viewHelp(Origin.LOGIN_SITE_ADDRESS);
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
    public void onPositiveClicked(@NonNull String instanceTag) {
        // No dialog tags currently handled
    }

    @Override public AndroidInjector<Object> androidInjector() {
        return mDispatchingAndroidInjector;
    }

    @Override public void startOver() {
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
    public void handleSiteAddressError(ConnectSiteInfoPayload siteInfo) {
        mViewModel.onHandleSiteAddressError(siteInfo);
    }

    public void handleNoJetpackSites() {
        // hide keyboard if you can
        hideKeyboard(this);
        mViewModel.onHandleNoJetpackSites();
    }


    private void showSiteAddressError(ShowSiteAddressError event) {
        LoginSiteCheckErrorFragment fragment = LoginSiteCheckErrorFragment.Companion.newInstance(event.getUrl());
        slideInFragment(fragment, true, LoginSiteCheckErrorFragment.TAG);
    }

    private void showNoJetpackSites() {
        LoginNoSitesFragment fragment = LoginNoSitesFragment.Companion.newInstance();
        slideInFragment(fragment, false, LoginNoSitesFragment.TAG);
    }
}
