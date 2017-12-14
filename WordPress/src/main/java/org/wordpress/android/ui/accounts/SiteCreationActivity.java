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
import org.wordpress.android.ui.accounts.signup.SiteCreationListener;
import org.wordpress.android.util.HelpshiftHelper;
import org.wordpress.android.util.ToastUtils;

public class SiteCreationActivity extends AppCompatActivity implements SiteCreationListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.site_creation_activity);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_ACCESSED);

            showFragment(new SiteCreationCategoryFragment(), SiteCreationCategoryFragment.TAG);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void showFragment(Fragment fragment, String tag) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag);
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
    public void startWithBlog() {
        // TODO: Jump to theme selection for a Blog
        ToastUtils.showToast(this, "Blog category selected");
    }

    @Override
    public void startWithWebsite() {
        // TODO: Jump to theme selection for a Website
        ToastUtils.showToast(this, "Website category selected");
    }

    @Override
    public void startWithPortfolio() {
        // TODO: Jump to theme selection for a Portfolio
        ToastUtils.showToast(this, "Portfolio category selected");
    }

    @Override
    public void helpCategoryScreen() {
        launchHelpshift(HelpshiftHelper.Tag.ORIGIN_SITE_CREATION_CATEGORY);
    }

    @Override
    public void setHelpContext(String faqId, String faqSection) {
        // nothing implemented here yet. This will set the context the `help()` callback should work with
    }
}
