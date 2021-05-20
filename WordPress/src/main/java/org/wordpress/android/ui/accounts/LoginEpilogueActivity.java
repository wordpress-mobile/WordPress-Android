package org.wordpress.android.ui.accounts;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.CloseWithResultOk;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowPostSignupInterstitialScreen;
import org.wordpress.android.ui.accounts.login.LoginEpilogueFragment;
import org.wordpress.android.ui.accounts.login.LoginEpilogueListener;
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesFragment;

import java.util.ArrayList;

import javax.inject.Inject;

public class LoginEpilogueActivity extends LocaleAwareActivity implements LoginEpilogueListener {
    public static final String EXTRA_DO_LOGIN_UPDATE = "EXTRA_DO_LOGIN_UPDATE";
    public static final String EXTRA_SHOW_AND_RETURN = "EXTRA_SHOW_AND_RETURN";
    public static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject LoginEpilogueViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        LoginFlowThemeHelper.injectMissingCustomAttributes(getTheme());

        setContentView(R.layout.login_epilogue_activity);

        if (savedInstanceState == null) {
            boolean doLoginUpdate = getIntent().getBooleanExtra(EXTRA_DO_LOGIN_UPDATE, false);
            boolean showAndReturn = getIntent().getBooleanExtra(EXTRA_SHOW_AND_RETURN, false);
            ArrayList<Integer> oldSitesIds = getIntent().getIntegerArrayListExtra(ARG_OLD_SITES_IDS);

            addPostLoginFragment(doLoginUpdate, showAndReturn, oldSitesIds);
        }

        initViewModel();
        initObservers();
    }

    private void initViewModel() {
        mViewModel = new ViewModelProvider(this, mViewModelFactory).get(LoginEpilogueViewModel.class);
    }

    private void initObservers() {
        mViewModel.getNavigationEvents().observe(this, event -> {
            LoginNavigationEvents loginEvent = event.getContentIfNotHandled();
            if (loginEvent instanceof ShowPostSignupInterstitialScreen) {
                showPostSignupInterstitialScreen();
            } else if (loginEvent instanceof CloseWithResultOk) {
                closeWithResultOk();
            } else if (loginEvent instanceof ShowNoJetpackSites) {
                showNoJetpackSites();
            }
        });
    }

    protected void addPostLoginFragment(boolean doLoginUpdate, boolean showAndReturn, ArrayList<Integer> oldSitesIds) {
        LoginEpilogueFragment loginEpilogueFragment = LoginEpilogueFragment.newInstance(doLoginUpdate, showAndReturn,
                oldSitesIds);
        showFragment(loginEpilogueFragment, LoginEpilogueFragment.TAG, false);
    }

    @Override
    public void onConnectAnotherSite() {
        if (mAccountStore.hasAccessToken()) {
            ActivityLauncher.addSelfHostedSiteForResult(this);
        } else {
            ActivityLauncher.showSignInForResult(this);
        }

        finish();
    }

    @Override
    public void onContinue() {
        mViewModel.onContinue();
    }

    private void showPostSignupInterstitialScreen() {
        ActivityLauncher.showPostSignupInterstitial(this);
    }

    private void closeWithResultOk() {
        setResult(RESULT_OK);
        finish();
    }

    private void showNoJetpackSites() {
        LoginNoSitesFragment fragment = LoginNoSitesFragment.Companion.newInstance();
        showFragment(fragment, LoginNoSitesFragment.TAG, true);
    }

    private void showFragment(Fragment fragment, String tag, boolean applySlideAnimation) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (applySlideAnimation) {
            fragmentTransaction
                    .setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                            R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        fragmentTransaction.commit();
    }
}
