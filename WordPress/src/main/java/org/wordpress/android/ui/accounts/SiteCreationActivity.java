package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.signup.SiteCreationCategoryFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationCreatingFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationDomainFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationListener;
import org.wordpress.android.ui.accounts.signup.SiteCreationSiteDetailsFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeLoaderFragment;
import org.wordpress.android.ui.main.SitePickerActivity;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ToastUtils;

public class SiteCreationActivity extends AppCompatActivity implements SiteCreationListener {
    public static final String KEY_DO_NEW_POST = "KEY_DO_NEW_POST";

    private static final String KEY_CATERGORY = "KEY_CATERGORY";
    private static final String KEY_THEME_ID = "KEY_THEME_ID";
    private static final String KEY_SITE_TITLE = "KEY_SITE_TITLE";
    private static final String KEY_SITE_TAGLINE = "KEY_SITE_TAGLINE";

    private String mCategory;
    private String mThemeId;
    private String mSiteTitle;
    private String mSiteTagline;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.site_creation_activity);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_ACCESSED);

            earlyLoadThemeLoaderFragment();
            showFragment(new SiteCreationCategoryFragment(), SiteCreationCategoryFragment.TAG);
        } else {
            mCategory = savedInstanceState.getString(KEY_CATERGORY);
            mThemeId = savedInstanceState.getString(KEY_THEME_ID);
            mSiteTitle = savedInstanceState.getString(KEY_SITE_TITLE);
            mSiteTagline = savedInstanceState.getString(KEY_SITE_TAGLINE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(KEY_CATERGORY, mCategory);
        outState.putString(KEY_THEME_ID, mThemeId);
        outState.putString(KEY_SITE_TITLE, mSiteTitle);
        outState.putString(KEY_SITE_TAGLINE, mSiteTagline);
    }

    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        fragmentTransaction.commit();
    }

    private void slideInFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left,
                                                R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right);
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void earlyLoadThemeLoaderFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        SiteCreationThemeLoaderFragment themeLoaderFragment = new SiteCreationThemeLoaderFragment();
        themeLoaderFragment.setRetainInstance(true);
        fragmentTransaction.add(themeLoaderFragment, SiteCreationThemeLoaderFragment.TAG);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return false;
    }

    private void launchZendesk(HelpActivity.Origin origin) {
        ActivityLauncher.viewHelpAndSupport(this, origin, null, null);
    }

    private enum SiteCreationBackStackMode {
        NORMAL,
        MODAL,
        FINISH_OK,
        FINISH_DISMISS
    }

    private SiteCreationBackStackMode getSiteCreationBackStackMode() {
        SiteCreationCreatingFragment siteCreationCreatingFragment =
                (SiteCreationCreatingFragment) getSupportFragmentManager()
                        .findFragmentByTag(SiteCreationCreatingFragment.TAG);

        if (siteCreationCreatingFragment == null || siteCreationCreatingFragment.canGoBack()) {
            return SiteCreationBackStackMode.NORMAL;
        } else if (siteCreationCreatingFragment.isInModalMode()) {
            return SiteCreationBackStackMode.MODAL;
        } else if (siteCreationCreatingFragment.isCreationSucceeded()) {
            return SiteCreationBackStackMode.FINISH_OK;
        } else {
            return SiteCreationBackStackMode.FINISH_DISMISS;
        }
    }

    @Override
    public void onBackPressed() {
        switch (getSiteCreationBackStackMode()) {
            case NORMAL:
                super.onBackPressed();
                break;
            case MODAL:
                ToastUtils.showToast(this, R.string.site_creation_creating_modal);
                break;
            case FINISH_OK:
                setResult(RESULT_OK);
                finish();
                break;
            case FINISH_DISMISS:
                finish();
                break;
        }
    }

    // SiteCreationListener implementation methods

    @Override
    public void withCategory(String category) {
        mCategory = category;
        slideInFragment(SiteCreationThemeFragment.newInstance(category), SiteCreationThemeFragment.TAG);
    }

    @Override
    public void helpCategoryScreen() {
        launchZendesk(HelpActivity.Origin.SITE_CREATION_CATEGORY);
    }

    @Override
    public void withTheme(String themeId) {
        mThemeId = themeId;
        slideInFragment(new SiteCreationSiteDetailsFragment(), SiteCreationSiteDetailsFragment.TAG);
    }

    @Override
    public void helpThemeScreen() {
        launchZendesk(HelpActivity.Origin.SITE_CREATION_THEME);
    }

    @Override
    public void withSiteDetails(String siteTitle, String siteTagline) {
        mSiteTitle = siteTitle;
        mSiteTagline = siteTagline;

        SiteCreationDomainFragment fragment = SiteCreationDomainFragment.newInstance(mSiteTitle, mCategory);
        slideInFragment(fragment, SiteCreationDomainFragment.TAG);
    }

    @Override
    public void helpSiteDetailsScreen() {
        launchZendesk(HelpActivity.Origin.SITE_CREATION_DETAILS);
    }

    @Override
    public void withDomain(String domain) {
        SiteCreationCreatingFragment siteCreationCreatingFragment =
                SiteCreationCreatingFragment.newInstance(mSiteTitle, mSiteTagline, domain, mThemeId);
        slideInFragment(siteCreationCreatingFragment, SiteCreationCreatingFragment.TAG);
    }

    @Override
    public void helpDomainScreen() {
        launchZendesk(HelpActivity.Origin.SITE_CREATION_DOMAIN);
    }

    @Override
    public void doConfigureSite(int siteLocalId) {
        Intent intent = new Intent();
        intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, siteLocalId);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void doWriteFirstPost(int siteLocalId) {
        Intent intent = new Intent();
        intent.putExtra(SitePickerActivity.KEY_LOCAL_ID, siteLocalId);
        intent.putExtra(KEY_DO_NEW_POST, true);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void helpSiteCreatingScreen() {
        launchZendesk(HelpActivity.Origin.SITE_CREATION_CREATING);
    }
}
