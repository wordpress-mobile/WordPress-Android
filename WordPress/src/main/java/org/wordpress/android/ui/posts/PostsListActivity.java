package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class PostsListActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    public static final String EXTRA_SEARCH_TERM = "searchTerm";
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";

    private boolean mIsPage = false;
    private PostsListFragment mPostList;
    private SiteModel mSite;

    private String mCurrentSearch;

    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        // init
        unpackIntent(getIntent());
        if (savedInstanceState != null) {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mCurrentSearch = savedInstanceState.getString(EXTRA_SEARCH_TERM, null);
        }

        // need a Site to continue
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        // set layout and init toolbar
        setContentView(R.layout.post_list_activity);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            setActionBarTitle(getString(mIsPage ? R.string.pages : R.string.posts));
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mPostList = (PostsListFragment) getFragmentManager().findFragmentById(R.id.postList);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // don't add search for self-hosted sites
        if (!mSite.isUsingWpComRestApi()) {
            return true;
        }

        getMenuInflater().inflate(R.menu.posts_list, menu);

        MenuItem searchAction = menu.findItem(R.id.search_posts_list);
        SearchView actionView = searchAction == null ? null : (SearchView) searchAction.getActionView();
        if (actionView != null) {
            MenuItemCompat.setOnActionExpandListener(searchAction, new MenuItemCompat.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(MenuItem menuItem) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                    setActionBarTitle(getString(mIsPage ? R.string.pages : R.string.posts));
                    return true;
                }
            });
            actionView.setOnQueryTextListener(this);

            if (!TextUtils.isEmpty(mCurrentSearch)) {
                actionView.setQuery(mCurrentSearch, true);
            }
        }

        return true;
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
        if (!TextUtils.isEmpty(mCurrentSearch)) {
            outState.putString(EXTRA_SEARCH_TERM, mCurrentSearch);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mPostList.filterPosts(query);
        setActionBarTitle("'" + query + "'");
        return true;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        mCurrentSearch = query;
        mPostList.filterPosts(query);
        return true;
    }

    public boolean isRefreshing() {
        return mPostList.isRefreshing();
    }

    private void setActionBarTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void unpackIntent(@NonNull Intent intent) {
        if (intent.hasExtra(WordPress.SITE)) {
            mSite = (SiteModel) intent.getSerializableExtra(WordPress.SITE);
        }
        if (intent.hasExtra(EXTRA_VIEW_PAGES)) {
            mIsPage = intent.getBooleanExtra(EXTRA_VIEW_PAGES, false);
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
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true);

        builder.create().show();
    }
}
