package org.wordpress.android.ui.posts;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogNegativeClickInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogOnDismissByOutsideTouchInterface;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.posts.GutenbergWarningFragmentDialog.GutenbergWarningDialogClickInterface;
import org.wordpress.android.util.LocaleManager;

import javax.inject.Inject;

public class PostsListActivity extends AppCompatActivity implements BasicDialogPositiveClickInterface,
        BasicDialogNegativeClickInterface, BasicDialogOnDismissByOutsideTouchInterface,
        GutenbergWarningDialogClickInterface {
    public static final String EXTRA_TARGET_POST_LOCAL_ID = "targetPostLocalId";

    private SiteModel mSite;

    @Inject SiteStore mSiteStore;
    @Inject PostStore mPostStore;
    @Inject ViewModelProvider.Factory mViewModelFactory;
    private PostsPagerAdapter mPostsPagerAdapter;
    private ViewPager mPager;
    private FloatingActionButton mFab;
    private PostListMainViewModel mViewModel;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.post_list_activity);


        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        setupActionBar();
        setupContent();
        initViewModel();
        handleIntent(getIntent());
    }

    private void setupContent() {
        mPager = findViewById(R.id.postPager);
        mPostsPagerAdapter = new PostsPagerAdapter(PostListMainViewModelKt.getPOST_LIST_PAGES(), mSite, this,
                getSupportFragmentManager());
        mPager.setAdapter(mPostsPagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mPager);

        mPager.addOnPageChangeListener(new OnPageChangeListener() {
            @Override public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override public void onPageSelected(int position) {
                mViewModel.onTabChanged(position);
            }

            @Override public void onPageScrollStateChanged(int state) {
            }
        });
        mFab = findViewById(R.id.fab_button);
        mFab.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View view) {
                mViewModel.newPost();
            }
        });
    }

    private void setupActionBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            setTitle(getString(R.string.my_site_btn_blog_posts));
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initViewModel() {
        mViewModel = ViewModelProviders.of(this, mViewModelFactory).get(PostListMainViewModel.class);
        mViewModel.start(mSite);
        mViewModel.getPostListAction().observe(this, new Observer<PostListAction>() {
            @Override public void onChanged(@Nullable PostListAction postListAction) {
                if (postListAction != null) {
                    PostListActionKt.handlePostListAction(PostsListActivity.this, postListAction);
                }
            }
        });
        mViewModel.isFabVisible().observe(this, new Observer<Boolean>() {
            @Override public void onChanged(@Nullable Boolean show) {
                if (show != null && show) {
                    mFab.show();
                } else {
                    mFab.hide();
                }
            }
        });
    }

    private void handleIntent(Intent intent) {
        // TODO site has changed and postListActivity opened with a target post is not implemented
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
            PostListFragment fragment = getCurrentFragment();
            if (fragment != null) {
                getCurrentFragment().handleEditPostResult(resultCode, data);
            }
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
        PostListFragment fragment = getCurrentFragment();
        if (fragment != null) {
            getCurrentFragment().onPositiveClickedForBasicDialog(instanceTag);
        }
    }

    @Override
    public void onNegativeClicked(@NotNull String instanceTag) {
        PostListFragment fragment = getCurrentFragment();
        if (fragment != null) {
            getCurrentFragment().onNegativeClickedForBasicDialog(instanceTag);
        }
    }

    @Override
    public void onDismissByOutsideTouch(@NotNull String instanceTag) {
        PostListFragment fragment = getCurrentFragment();
        if (fragment != null) {
            getCurrentFragment().onDismissByOutsideTouchForBasicDialog(instanceTag);
        }
    }

// GutenbergWarningDialogClickInterface Callbacks

    @Override
    public void onGutenbergWarningDialogEditPostClicked(long gutenbergRemotePostId) {
        PostListFragment fragment = getCurrentFragment();
        if (fragment != null) {
            getCurrentFragment().onGutenbergWarningDialogEditPostClicked(gutenbergRemotePostId);
        }
    }

    @Override
    public void onGutenbergWarningDialogCancelClicked(long gutenbergRemotePostId) {
        PostListFragment fragment = getCurrentFragment();
        if (fragment != null) {
            getCurrentFragment().onGutenbergWarningDialogCancelClicked(gutenbergRemotePostId);
        }
    }

    @Override
    public void onGutenbergWarningDialogLearnMoreLinkClicked(long gutenbergRemotePostId) {
        PostListFragment fragment = getCurrentFragment();
        if (fragment != null) {
            getCurrentFragment().onGutenbergWarningDialogLearnMoreLinkClicked(gutenbergRemotePostId);
        }
    }

    @Override
    public void onGutenbergWarningDialogDontShowAgainClicked(long gutenbergRemotePostId, boolean checked) {
        PostListFragment fragment = getCurrentFragment();
        if (fragment != null) {
            getCurrentFragment().onGutenbergWarningDialogDontShowAgainClicked(gutenbergRemotePostId, checked);
        }
    }

    @Nullable
    private PostListFragment getCurrentFragment() {
        return mPostsPagerAdapter.getItemAtPosition(mPager.getCurrentItem());
    }
}
