package org.wordpress.android.ui.prefs;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AnalyticsUtils;

public class SettingsActivity extends ActionBarActivity {
    public static final int RESULT_SIGNED_OUT = 1;
    public static final String CURRENT_BLOG_CHANGED = "CURRENT_BLOG_CHANGED";
    private Blog mCurrentBlogOnCreate;
    private SettingsFragment mSettingsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0.0f);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mCurrentBlogOnCreate = WordPress.getCurrentBlog();
        setContentView(R.layout.settings_activity);
        mSettingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                            .add(R.id.fragment_container, mSettingsFragment)
                            .commit();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mSettingsFragment.refreshWPComAuthCategory();
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        boolean currentBlogChanged = false;
        if (mCurrentBlogOnCreate != null) {
            if (mCurrentBlogOnCreate.isDotcomFlag()) {
                if (!WordPress.wpDB.isDotComAccountVisible(mCurrentBlogOnCreate.getRemoteBlogId())) {
                    // dotcom blog has been hidden or removed
                    currentBlogChanged = true;
                }
            } else {
                if (!WordPress.wpDB.isBlogInDatabase(mCurrentBlogOnCreate.getRemoteBlogId(), mCurrentBlogOnCreate.getUrl())) {
                    // self hosted blog has been removed
                    currentBlogChanged = true;
                }
            }
        } else {
            // no visible blogs when settings opened
            if (WordPress.wpDB.getNumVisibleAccounts() != 0) {
                // now at least one blog could be selected
                currentBlogChanged = true;
            }
        }
        data.putExtra(SettingsActivity.CURRENT_BLOG_CHANGED, currentBlogChanged);
        setResult(Activity.RESULT_OK, data);
        AnalyticsTracker.loadPrefHasUserOptedOut(this, true);
        AnalyticsUtils.refreshMetadata();
        super.finish();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}
