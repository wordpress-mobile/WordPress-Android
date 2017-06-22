package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.MenuItemCompat.OnActionExpandListener;
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
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class PostsListActivity extends AppCompatActivity implements OnActionExpandListener, SearchView.OnQueryTextListener {
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";

    private static final String EXTRA_SAVED_QUERY = "savedQuery";

    private boolean mIsPage = false;
    private PostsListFragment mPostList;
    private SiteModel mSite;

    // Search
    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;
    private String mQuery;

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

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            mQuery = savedInstanceState.getString(EXTRA_SAVED_QUERY);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        mPostList = (PostsListFragment) getFragmentManager().findFragmentByTag(PostsListFragment.TAG);
        if (mPostList == null) {
            mPostList = PostsListFragment.newInstance(mSite, mIsPage);
            getFragmentManager().beginTransaction()
                    .add(R.id.post_list_container, mPostList, PostsListFragment.TAG)
                    .commit();
        }

        showErrorDialogIfNeeded(getIntent().getExtras());
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(mIsPage ? ActivityId.PAGES : ActivityId.POSTS);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // collapse search view when paused
        if (mSearchMenuItem != null) {
            // save the query temporarily so it won't get erased when search view collapses
            String tempQuery = mQuery;
            MenuItemCompat.collapseActionView(mSearchMenuItem);
            mQuery = tempQuery;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.posts_list, menu);

        mSearchMenuItem = menu.findItem(R.id.search_posts_list);
        MenuItemCompat.setOnActionExpandListener(mSearchMenuItem, this);

        mSearchView = (SearchView) mSearchMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(this);

        // open search bar if we were searching for something before
        if (!TextUtils.isEmpty(mQuery)) {
            String tempQuery = mQuery; //temporary hold onto query
            MenuItemCompat.expandActionView(mSearchMenuItem); //this will reset mQuery
            onQueryTextSubmit(tempQuery);
            mSearchView.setQuery(mQuery, true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.search_posts_list:
                onSearchPostsListSelected();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mQuery = query;
        mSearchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (mPostList != null) {
            mPostList.search(newText);
        }
        mQuery = newText;
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putString(EXTRA_SAVED_QUERY, mQuery);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.EDIT_POST) {
            mPostList.handleEditPostResult(resultCode, data);
        }
    }

    public boolean isRefreshing() {
        return mPostList.isRefreshing();
    }

    /** Called when search MenuItem is selected */
    private void onSearchPostsListSelected() {
        if (!NetworkUtils.checkConnection(this)) {
            // if there's no network we can't perform a search, close the search view
            mSearchView.clearFocus();
            MenuItemCompat.collapseActionView(mSearchMenuItem);
        } else {
            MenuItemCompat.expandActionView(mSearchMenuItem);
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
