package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.stores.model.SiteModel;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;

public class PostsListActivity extends AppCompatActivity {
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";
    public static final String EXTRA_SELECT_SITE_LOCAL_ID = "EXTRA_SELECT_SITE_LOCAL_ID";

    private boolean mIsPage = false;
    private PostsListFragment mPostList;
    private SiteModel mSite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.post_list_activity);

        mIsPage = getIntent().getBooleanExtra(EXTRA_VIEW_PAGES, false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(mIsPage ? R.string.pages : R.string.posts));
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fm = getFragmentManager();
        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);
        mSite = (SiteModel) getIntent().getSerializableExtra(ActivityLauncher.EXTRA_SITE);

        showErrorDialogIfNeeded(getIntent().getExtras());
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(mIsPage ? ActivityId.PAGES : ActivityId.POSTS);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    /**
     * intent extras will contain error info if this activity was started from an
     * upload error notification
     */
    private void showErrorDialogIfNeeded(Bundle extras) {
        if (extras == null || !extras.containsKey(EXTRA_ERROR_MSG) || isFinishing()) {
            return;
        }

        final String errorMessage = extras.getString(EXTRA_ERROR_MSG);

        if (TextUtils.isEmpty(errorMessage)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getText(R.string.error))
               .setMessage(errorMessage)
               .setPositiveButton(R.string.ok, null)
               .setCancelable(true);

        builder.create().show();
    }

    public boolean isRefreshing() {
        return mPostList.isRefreshing();
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    public SiteModel getSelectedSite() {
        return mSite;
    }
}
