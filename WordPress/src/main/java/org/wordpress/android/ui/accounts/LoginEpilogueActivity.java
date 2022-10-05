package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.CloseWithResultOk;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.CreateNewSite;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.SelectSite;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites;
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowPostSignupInterstitialScreen;
import org.wordpress.android.ui.accounts.login.LoginEpilogueFragment;
import org.wordpress.android.ui.accounts.login.LoginEpilogueListener;
import org.wordpress.android.ui.accounts.login.jetpack.LoginNoSitesFragment;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.ui.mysite.SelectedSiteRepository;
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource;

import java.util.ArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginEpilogueActivity extends LocaleAwareActivity implements LoginEpilogueListener {
    public static final String EXTRA_DO_LOGIN_UPDATE = "EXTRA_DO_LOGIN_UPDATE";
    public static final String EXTRA_SHOW_AND_RETURN = "EXTRA_SHOW_AND_RETURN";
    public static final String ARG_OLD_SITES_IDS = "ARG_OLD_SITES_IDS";
    public static final String KEY_SITE_CREATED_FROM_LOGIN_EPILOGUE = "SITE_CREATED_FROM_LOGIN_EPILOGUE";

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    @Inject LoginEpilogueViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            } else if (loginEvent instanceof SelectSite) {
                selectSite(((SelectSite) loginEvent).getLocalId());
            } else if (loginEvent instanceof CreateNewSite) {
                createNewSite();
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
    public void onSiteClick(int localId) {
        mViewModel.onSiteClick(localId);
    }

    @Override
    public void onCreateNewSite() {
        mViewModel.onCreateNewSite();
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

    private void selectSite(int localId) {
        setResult(RESULT_OK, new Intent().putExtra(SitePickerActivity.KEY_SITE_LOCAL_ID, localId));
        finish();
    }

    private void createNewSite() {
        ActivityLauncher.newBlogForResult(this, SiteCreationSource.LOGIN_EPILOGUE);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.CREATE_SITE
            && resultCode == RESULT_OK
            && data != null
        ) {
            int newSiteLocalID = data.getIntExtra(
                    SitePickerActivity.KEY_SITE_LOCAL_ID,
                    SelectedSiteRepository.UNAVAILABLE
            );
            boolean isTitleTaskCompleted = data.getBooleanExtra(
                    SitePickerActivity.KEY_SITE_TITLE_TASK_COMPLETED,
                    false
            );
            setResult(RESULT_OK, new Intent()
                    .putExtra(SitePickerActivity.KEY_SITE_LOCAL_ID, newSiteLocalID)
                    .putExtra(SitePickerActivity.KEY_SITE_TITLE_TASK_COMPLETED, isTitleTaskCompleted)
                    .putExtra(KEY_SITE_CREATED_FROM_LOGIN_EPILOGUE, true)
            );
            finish();
        }
    }
}
