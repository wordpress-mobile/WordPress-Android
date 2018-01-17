package org.wordpress.android.ui.accounts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.accounts.signup.SiteCreationCategoryFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationCreatingFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationListener;
import org.wordpress.android.ui.accounts.signup.SiteCreationService;
import org.wordpress.android.ui.accounts.signup.SiteCreationSiteDetailsFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeFragment;
import org.wordpress.android.ui.accounts.signup.SiteCreationThemeLoaderFragment;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.ToastUtils;

public class SiteCreationActivity extends AppCompatActivity implements SiteCreationListener {
    private static final String KEY_CATERGORY = "KEY_CATERGORY";
    private static final String KEY_THEME_ID = "KEY_THEME_ID";
    private static final String KEY_SITE_TITLE = "KEY_SITE_TITLE";
    private static final String KEY_SITE_TAGLINE = "KEY_SITE_TAGLINE";

    private String mCategory;
    private String mThemeId;
    private String mSiteTitle;
    private String mSiteTagline;

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

    private void launchHelpshift(HelpshiftHelper.Tag origin) {
        Intent intent = new Intent(this, HelpActivity.class);
        // Used to pass data to an eventual support service
        intent.putExtra(HelpshiftHelper.ORIGIN_KEY, origin);
        startActivity(intent);
    }

    // SiteCreationListener implementation methods

    @Override
    public void withCategory(String category) {
        mCategory = category;
        slideInFragment(SiteCreationThemeFragment.newInstance(category), SiteCreationThemeFragment.TAG);
    }

    @Override
    public void helpCategoryScreen() {
        launchHelpshift(HelpshiftHelper.Tag.ORIGIN_SITE_CREATION_CATEGORY);
    }

    @Override
    public void withTheme(String themeId) {
        mThemeId = themeId;
        slideInFragment(new SiteCreationSiteDetailsFragment(), SiteCreationSiteDetailsFragment.TAG);
    }

    @Override
    public void helpThemeScreen() {
        launchHelpshift(HelpshiftHelper.Tag.ORIGIN_SITE_CREATION_THEME);
    }

    @Override
    public void withSiteDetails(String siteTitle, String siteTagline) {
        mSiteTitle = siteTitle;
        mSiteTagline = siteTagline;

        // TODO: insert the Domain selection screen here. Jump to "Creating" screen is for dev purposes

        // start the Service to perform the site creation
        String siteSlug = WordPress.getBuildConfigString(this, "DEBUG_DOTCOM_NEW_SITE_SLUG");
        SiteCreationService.createSite(this, siteTitle, siteTagline, siteSlug, mThemeId);

        slideInFragment(new SiteCreationCreatingFragment(), SiteCreationCreatingFragment.TAG);
    }

    @Override
    public void helpSiteDetailsScreen() {
        launchHelpshift(HelpshiftHelper.Tag.ORIGIN_SITE_CREATION_DETAILS);
    }

    @Override
    public void creationSuccess() {
        ToastUtils.showToast(this, "Success!");
    }

    @Override
    public void helpSiteCreatingScreen() {
        launchHelpshift(HelpshiftHelper.Tag.ORIGIN_SITE_CREATION_CREATING);
    }

    @Override
    public void setHelpContext(String faqId, String faqSection) {
        // nothing implemented here yet. This will set the context the `help()` callback should work with
    }
}
