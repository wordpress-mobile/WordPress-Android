package org.wordpress.android.ui.posts;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import static org.wordpress.android.ui.posts.ContentType.PAGE;
import static org.wordpress.android.ui.posts.ContentType.POST;

public class PostsListActivity extends AppCompatActivity {
    public static final String EXTRA_CONTENT_TYPE = "contentType";
    public static final String EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId";

    private int mContentType;
    private PostsListFragment mPostList;

    private SiteModel mSite;
    @Inject
    SiteStore mSiteStore;
    @Inject
    PostStore mPostStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.post_list_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        mContentType = intent.getIntExtra(EXTRA_CONTENT_TYPE, POST);

        // get new intent extras and compare whether the running instance of PostsListActivity has
        // the same values or not. If not, we need to create a new fragment and show the corresponding
        // requested content
        boolean pageHasChanged = false;
        if (intent.hasExtra(EXTRA_CONTENT_TYPE)) {
            int contentType = intent.getIntExtra(EXTRA_CONTENT_TYPE, POST);
            pageHasChanged = contentType != mContentType;
        }
        mContentType = intent.getIntExtra(EXTRA_CONTENT_TYPE, POST);

        boolean siteHasChanged = false;
        if (intent.hasExtra(WordPress.SITE)) {
            SiteModel site = (SiteModel) intent.getSerializableExtra(WordPress.SITE);
            if (mSite != null && site != null) {
                siteHasChanged = site.getId() != mSite.getId();
            }
            mSite = site;
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //TODO Portfolio support
            actionBar.setTitle(getString(mContentType == PAGE ? R.string.pages : R.string.posts));
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        PostModel targetPost = null;
        int targetPostId = intent.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, 0);
        if (targetPostId > 0) {
            targetPost = mPostStore.getPostByLocalPostId(intent.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, 0));
            if (targetPost == null) {
                //TODO Portfolio support
                String errorMessage = getString(mContentType == PAGE ? R.string.error_page_does_not_exist
                        : R.string.error_post_does_not_exist);
                ToastUtils.showToast(this, errorMessage);
            }
        }

        mPostList = (PostsListFragment) getFragmentManager().findFragmentByTag(PostsListFragment.TAG);
        if (mPostList == null || siteHasChanged || pageHasChanged || targetPost != null) {
            PostsListFragment oldFragment = mPostList;
            mPostList = PostsListFragment.newInstance(mSite, mContentType, targetPost);
            if (oldFragment == null) {
                getFragmentManager().beginTransaction()
                        .add(R.id.post_list_container, mPostList, PostsListFragment.TAG)
                        .commit();
            } else {
                getFragmentManager().beginTransaction()
                        .replace(R.id.post_list_container, mPostList, PostsListFragment.TAG)
                        .commit();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //TODO Portfolio support
        ActivityId.trackLastActivity(mContentType == PAGE ? ActivityId.PAGES : ActivityId.POSTS);
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
