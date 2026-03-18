package org.wordpress.android.ui.accounts;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
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
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.ui.accounts.login.LoginAnalyticsListener;
import org.wordpress.android.ui.accounts.login.applicationpassword.LoginSiteApplicationPasswordFragment;
import org.wordpress.android.support.ZendeskExtraTags;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow;
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step;
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
public class LoginActivity extends BaseAppCompatActivity implements
        HasAndroidInjector, BasicDialogPositiveClickInterface {
    public static final String ARG_JETPACK_CONNECT_SOURCE = "ARG_JETPACK_CONNECT_SOURCE";
    public static final String MAGIC_LOGIN = "magic-login";
    public static final String TOKEN_PARAMETER = "token";

    private static final String KEY_UNIFIED_TRACKER_SOURCE = "KEY_UNIFIED_TRACKER_SOURCE";
    private static final String KEY_UNIFIED_TRACKER_FLOW = "KEY_UNIFIED_TRACKER_FLOW";
    private static final String KEY_IS_WAITING_FOR_SITES = "KEY_IS_WAITING_FOR_SITES";
    private static final String KEY_OLD_SITES_IDS = "KEY_OLD_SITES_IDS";
    private static final String KEY_SHARE_FLOW_LOGIN_LAUNCHED = "KEY_SHARE_FLOW_LOGIN_LAUNCHED";

    private int mFragmentContainerId;
    private View mLoadingOverlay;

    /**
     * Check if there's a pending share flow. Used by
     * ApplicationPasswordLoginActivity to determine whether to
     * navigate to main activity or just finish.
     */
    public static boolean consumeShareFlowPending() {
        return AppPrefs.consumeShareFlowPending();
    }

    private LoginFlow mLoginFlow;
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
    // Flag to track that the user actually launched a login during share flow
    private boolean mShareFlowLoginLaunched = false;
    private ArrayList<Integer> mOldSitesIdsForLoginUpdate;

    @Inject ExperimentalFeatures mExperimentalFeatures;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // Attempt Login if this activity was created in response to a user confirming login
        String dataString = getIntent().getDataString();
        if (mLoginHelper.hasOAuthCallback(dataString)) {
            getIntent().setData(null);
            setContentView(R.layout.login_loading);
            mLoginHelper.tryLoginWithDataString(
                    dataString,
                    () -> loggedInAndFinish(new ArrayList<>(), true),
                    error -> showLoginError(error)
            );
            return;
        } else {
            // Not an OAuth callback - clear any pending login flow
            AppPrefs.setPendingLoginFlow(null);
        }

        // Start preloading the WordPress.com login page if needed – this avoids visual hitches
        // when displaying that screen
        mLoginHelper.bindCustomTabsService(this);

        // go no further if the user is already logged in and this is the login screen shown at startup
        LoginFlow loginFlow = getLoginFlow();
        if ((mLoginHelper.isLoggedIn()) && (loginFlow == LoginFlow.PROLOGUE)) {
            // Show loading UI while we fetch account and sites in the background
            setContentView(R.layout.login_loading);
            this.loggedInAndFinish(new ArrayList<Integer>(), true);
            return;
        }

        LoginFlowThemeHelper.injectMissingCustomAttributes(getTheme());

        ViewGroup.LayoutParams matchParent = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        FrameLayout rootContainer = new FrameLayout(this);
        rootContainer.setLayoutParams(matchParent);

        FrameLayout fragmentContainer = new FrameLayout(this);
        mFragmentContainerId = R.id.fragment_container;
        fragmentContainer.setId(mFragmentContainerId);
        fragmentContainer.setFitsSystemWindows(false);
        fragmentContainer.setLayoutParams(matchParent);
        rootContainer.addView(fragmentContainer);

        mLoadingOverlay = LayoutInflater.from(this)
                .inflate(R.layout.login_loading, rootContainer, false);
        mLoadingOverlay.setVisibility(View.GONE);
        rootContainer.addView(mLoadingOverlay);

        setContentView(rootContainer);

        if (savedInstanceState == null) {
            mLoginAnalyticsListener.trackLoginAccessed();

            mUnifiedLoginTracker.setSource(loginFlow.getAnalyticsSource());

            switch (loginFlow.getInitialScreen()) {
                case PROLOGUE:
                    showFragment(new LoginPrologueRevampedFragment(), LoginPrologueRevampedFragment.TAG);
                    break;
                case WPCOM_OAUTH:
                    showWPcomLoginScreen(this);
                    break;
                case SELF_HOSTED:
                    showFragment(
                            new LoginSiteApplicationPasswordFragment(),
                            LoginSiteApplicationPasswordFragment.TAG
                    );
                    break;
            }
        } else {
            String source = savedInstanceState.getString(KEY_UNIFIED_TRACKER_SOURCE);
            if (source != null) {
                mUnifiedLoginTracker.setSource(source);
            }
            mUnifiedLoginTracker.setFlow(savedInstanceState.getString(KEY_UNIFIED_TRACKER_FLOW));
            mIsWaitingForSitesToLoad = savedInstanceState.getBoolean(KEY_IS_WAITING_FOR_SITES);
            mShareFlowLoginLaunched = savedInstanceState.getBoolean(KEY_SHARE_FLOW_LOGIN_LAUNCHED);
            mOldSitesIdsForLoginUpdate = savedInstanceState.getIntegerArrayList(KEY_OLD_SITES_IDS);
        }

        // If we're waiting for sites (e.g. after config change during post-OAuth fetch),
        // show the loading overlay instead of the fragment container
        if (mIsWaitingForSitesToLoad) {
            showLoadingOverlay();
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_UNIFIED_TRACKER_SOURCE, mUnifiedLoginTracker.getSource().getValue());
        Flow flow = mUnifiedLoginTracker.getFlow();
        if (flow != null) {
            outState.putString(KEY_UNIFIED_TRACKER_FLOW, flow.getValue());
        }
        outState.putBoolean(KEY_IS_WAITING_FOR_SITES, mIsWaitingForSitesToLoad);
        outState.putBoolean(KEY_SHARE_FLOW_LOGIN_LAUNCHED, mShareFlowLoginLaunched);
        outState.putIntegerArrayList(KEY_OLD_SITES_IDS, mOldSitesIdsForLoginUpdate);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        mLoginFlow = null;

        // Handle OAuth callback when activity is reused (singleTop)
        String dataString = intent.getDataString();
        if (mLoginHelper.hasOAuthCallback(dataString)) {
            intent.setData(null);
            if (mLoadingOverlay != null) {
                showLoadingOverlay();
            } else {
                setContentView(R.layout.login_loading);
            }
            mLoginHelper.tryLoginWithDataString(
                    dataString,
                    () -> loggedInAndFinish(new ArrayList<>(), true),
                    error -> showLoginError(error)
            );
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
    protected void onDestroy() {
        super.onDestroy();
        mLoginHelper.dispose();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if self-hosted login completed while in share flow
        // ApplicationPasswordLoginActivity finishes back here after successful login
        if (getLoginFlow() == LoginFlow.SHARE_INTENT
                && mShareFlowLoginLaunched
                && mSiteStore.hasSite()) {
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
        ArrayList<Integer> oldSitesIds = mOldSitesIdsForLoginUpdate != null
                ? mOldSitesIdsForLoginUpdate
                : new ArrayList<>();
        mOldSitesIdsForLoginUpdate = null;
        loggedInAndFinish(oldSitesIds, false);
    }

    /**
     * Navigates to the main activity and finishes the login flow.
     * This is the common exit point for successful logins.
     */
    private void navigateToMainActivityOrFinish() {
        startPostLoginServices();

        LoginFlow.CompletionBehavior behavior = getLoginFlow().getCompletionBehavior();
        if (behavior == LoginFlow.CompletionBehavior.MAIN_ACTIVITY) {
            // Select the primary site after WP.com login
            ActivityLauncher.showMainActivity(this, false, true);
        }
        // For FINISH and FINISH_WITH_SITE, just finish and let the caller handle navigation
        setResult(Activity.RESULT_OK);
        finish();
    }

    private void showPrologueScreen() {
        LoginFlowThemeHelper.injectMissingCustomAttributes(getTheme());
        FrameLayout fragmentContainer = new FrameLayout(this);
        mFragmentContainerId = R.id.fragment_container;
        fragmentContainer.setId(mFragmentContainerId);
        fragmentContainer.setFitsSystemWindows(false);
        fragmentContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(fragmentContainer);
        showFragment(
                new LoginPrologueRevampedFragment(),
                LoginPrologueRevampedFragment.TAG
        );
    }

    private void showLoadingOverlay() {
        if (mLoadingOverlay != null) {
            mLoadingOverlay.setVisibility(View.VISIBLE);
        }
        View fragmentContainer = findViewById(mFragmentContainerId);
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.GONE);
        }
    }

    private void hideLoadingOverlay() {
        if (mLoadingOverlay != null) {
            mLoadingOverlay.setVisibility(View.GONE);
        }
        View fragmentContainer = findViewById(mFragmentContainerId);
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    private void showLoginError(@NonNull Exception error) {
        AppLog.e(T.MAIN, "OAuth login failed", error);

        View progressBar = findViewById(R.id.progress_bar);
        View loadingText = findViewById(R.id.loading_text);
        TextView errorText = findViewById(R.id.error_text);
        View retryButton = findViewById(R.id.retry_button);

        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (loadingText != null) loadingText.setVisibility(View.GONE);

        if (errorText != null) {
            errorText.setText(getString(R.string.error_generic_network));
            errorText.setVisibility(View.VISIBLE);
        }

        if (retryButton != null) {
            retryButton.setVisibility(View.VISIBLE);
            retryButton.setOnClickListener(v -> showPrologueScreen());
        }
    }

    private void showFragment(@NonNull Fragment fragment, @NonNull String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(mFragmentContainerId, fragment, tag);
        fragmentTransaction.commit();
    }

    private void slideInFragment(@NonNull Fragment fragment, boolean shouldAddToBackStack, @NonNull String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        fragmentTransaction.replace(mFragmentContainerId, fragment, tag);
        if (shouldAddToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }
        fragmentTransaction.commitAllowingStateLoss();
    }

    public LoginFlow getLoginFlow() {
        if (mLoginFlow != null) {
            // returned the cached value
            return mLoginFlow;
        }

        // compute and cache the Login flow
        mLoginFlow = LoginFlow.fromIntent(getIntent());

        // If the flow is PROLOGUE (default) but we have a pending flow from an OAuth callback,
        // use that instead
        String pendingFlowName = AppPrefs.getPendingLoginFlow();
        if (mLoginFlow == LoginFlow.PROLOGUE
                && pendingFlowName != null) {
            try {
                mLoginFlow = LoginFlow.valueOf(pendingFlowName);
            } catch (IllegalArgumentException ignored) {
                // Invalid value, stick with PROLOGUE
            }
            AppPrefs.setPendingLoginFlow(null);
        }

        return mLoginFlow;
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

        LoginFlow.CompletionBehavior behavior = getLoginFlow().getCompletionBehavior();
        switch (behavior) {
            case FINISH_WITH_SITE:
                // Handle self-hosted site login - find the newly added site and return its ID
                finishWithNewlyAddedSiteId(oldSitesIds);
                break;
            case MAIN_ACTIVITY:
            case FINISH:
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

    public void showWPcomLoginScreen(@NonNull Context context) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WPCOM_WEBVIEW);
        mUnifiedLoginTracker.setFlowAndStep(Flow.WORDPRESS_COM_WEB, Step.WPCOM_WEB_START);

        // Save the current login flow so it survives the OAuth
        // callback (which creates a new activity instance)
        AppPrefs.setPendingLoginFlow(getLoginFlow().name());

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

    public void loginViaSiteAddress() {
        // Track if we're in a share flow so ApplicationPasswordLoginActivity knows to just finish
        if (getLoginFlow() == LoginFlow.SHARE_INTENT) {
            AppPrefs.setShareFlowPending(true);
            mShareFlowLoginLaunched = true;
        }
        slideInFragment(new LoginSiteApplicationPasswordFragment(), true, LoginSiteApplicationPasswordFragment.TAG);
    }

    private void viewHelp(Origin origin) {
        List<String> extraSupportTags = getLoginFlow() == LoginFlow.JETPACK_STATS ? Collections
                .singletonList(ZendeskExtraTags.connectingJetpack) : null;
        ActivityLauncher.viewHelp(this, origin, null, extraSupportTags, mExperimentalFeatures);
    }

    public void helpSiteAddress(String url) {
        viewHelp(Origin.LOGIN_SITE_ADDRESS);
    }

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
