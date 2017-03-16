package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class PostsListActivity extends AppCompatActivity {
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";

    private boolean mIsPage = false;
    private PostsListFragment mPostList;
    private SiteModel mSite;

    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

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
        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);
        showErrorDialogIfNeeded(getIntent().getExtras());
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(mIsPage ? ActivityId.PAGES : ActivityId.POSTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.EDIT_POST) {
            mPostList.handleEditPostResult(resultCode, data);
        }
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
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }
}
