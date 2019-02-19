package org.wordpress.android.ui.posts;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class PostsListActivity extends AppCompatActivity implements BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface, BasicDialogOnDismissByOutsideTouchInterface {
    public static final String EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId";

    private PostListFragment mPostList;
    private SiteModel mSite;

    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

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
            setTitle(getString(R.string.my_site_btn_blog_posts));
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        PostModel targetPost = null;
        int targetPostId = intent.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, 0);
        if (targetPostId > 0) {
            targetPost = mPostStore.getPostByLocalPostId(intent.getIntExtra(EXTRA_TARGET_POST_LOCAL_ID, 0));
            if (targetPost == null) {
                String errorMessage = getString(R.string.error_post_does_not_exist);
                ToastUtils.showToast(this, errorMessage);
            }
        }

        mPostList = (PostListFragment) getSupportFragmentManager().findFragmentByTag(PostListFragment.TAG);
        if (mPostList == null || siteHasChanged || targetPost != null) {
            PostListFragment oldFragment = mPostList;
            mPostList = PostListFragment.newInstance(mSite, targetPost);
            if (oldFragment == null) {
                getSupportFragmentManager().beginTransaction()
                                    .add(R.id.post_list_container, mPostList, PostListFragment.TAG)
                                    .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.post_list_container, mPostList, PostListFragment.TAG)
                                    .commit();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.POSTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RequestCodes.EDIT_POST) {
            mPostList.handleEditPostResult(resultCode, data);
        }
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

    // BasicDialogFragment Callbacks

    @Override
    public void onPositiveClicked(@NotNull String instanceTag) {
        if (mPostList != null) {
            mPostList.onPositiveClickedForBasicDialog(instanceTag);
        }
    }

    @Override
    public void onNegativeClicked(@NotNull String instanceTag) {
        if (mPostList != null) {
            mPostList.onNegativeClickedForBasicDialog(instanceTag);
        }
    }

    @Override
    public void onDismissByOutsideTouch(@NotNull String instanceTag) {
        if (mPostList != null) {
            mPostList.onDismissByOutsideTouchForBasicDialog(instanceTag);
        }
    }
}
