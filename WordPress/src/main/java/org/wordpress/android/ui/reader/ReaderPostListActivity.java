package org.wordpress.android.ui.reader;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderBlog;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.util.ToastUtils;

/*
 * serves as the host for ReaderPostListFragment when showing blog preview & tag preview
 */
public class ReaderPostListActivity extends LocaleAwareActivity {
    private ReaderPostListType mPostListType;
    private long mSiteId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_list);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent().hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
            mPostListType = (ReaderPostListType) getIntent().getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
        } else {
            mPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE;
        }

        if (getPostListType() == ReaderPostListType.TAG_PREVIEW
            || getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
            // show an X in the toolbar which closes the activity - if this is tag preview, then
            // using the back button will navigate through tags if the user explores beyond a single tag
            toolbar.setNavigationIcon(R.drawable.ic_cross_white_24dp);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
                setTitle(R.string.reader_title_blog_preview);
                if (savedInstanceState == null) {
                    long blogId = getIntent().getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                    long feedId = getIntent().getLongExtra(ReaderConstants.ARG_FEED_ID, 0);
                    if (feedId != 0) {
                        showListFragmentForFeed(feedId);
                        mSiteId = feedId;
                    } else {
                        showListFragmentForBlog(blogId);
                        mSiteId = blogId;
                    }
                } else {
                    mSiteId = savedInstanceState.getLong(ReaderConstants.KEY_SITE_ID);
                }
            } else if (getPostListType() == ReaderPostListType.TAG_PREVIEW) {
                setTitle(R.string.reader_title_tag_preview);
                ReaderTag tag = (ReaderTag) getIntent().getSerializableExtra(ReaderConstants.ARG_TAG);
                if (tag != null && savedInstanceState == null) {
                    showListFragmentForTag(tag, mPostListType);
                }
            }
        }

        // restore the activity title
        if (savedInstanceState != null && savedInstanceState.containsKey(ReaderConstants.KEY_ACTIVITY_TITLE)) {
            setTitle(savedInstanceState.getString(ReaderConstants.KEY_ACTIVITY_TITLE));
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        // this particular Activity doesn't show filtering, so we'll disable the FilteredRecyclerView toolbar here
        disableFilteredRecyclerViewToolbar();
    }

    /*
    * This method hides the FilteredRecyclerView toolbar with spinner so to disable content filtering, for reusability
    * */
    private void disableFilteredRecyclerViewToolbar() {
        // make it invisible - setting height to zero here because setting visibility to View.GONE wouldn't take the
        // occupied space, as otherwise expected
        AppBarLayout appBarLayout = findViewById(R.id.app_bar_layout);
        if (appBarLayout != null) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
            lp.height = 0;
            appBarLayout.setLayoutParams(lp);
        }

        // disabling any CoordinatorLayout behavior for scrolling
        Toolbar toolbarWithSpinner = findViewById(R.id.toolbar_with_spinner);
        if (toolbarWithSpinner != null) {
            AppBarLayout.LayoutParams p = (AppBarLayout.LayoutParams) toolbarWithSpinner.getLayoutParams();
            p.setScrollFlags(0);
            toolbarWithSpinner.setLayoutParams(p);
        }
    }

    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }

        // store the title for blog/tag preview so we can restore it upon recreation
        if (getPostListType() == ReaderPostListType.BLOG_PREVIEW
            || getPostListType() == ReaderPostListType.TAG_PREVIEW) {
            outState.putString(ReaderConstants.KEY_ACTIVITY_TITLE, getTitle().toString());
            outState.putLong(ReaderConstants.KEY_SITE_ID, mSiteId);
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null || !fragment.onActivityBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getPostListType() == ReaderPostListType.BLOG_PREVIEW) {
            getMenuInflater().inflate(R.menu.share, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_share:
                shareSite();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareSite() {
        ReaderBlog blog = ReaderBlogTable.getBlogInfo(mSiteId);

        if (blog != null && blog.hasUrl()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, blog.getUrl());

            if (blog.hasName()) {
                intent.putExtra(Intent.EXTRA_SUBJECT, blog.getName());
            }

            try {
                AnalyticsTracker.track(Stat.READER_SITE_SHARED);
                startActivity(Intent.createChooser(intent, getString(R.string.share_link)));
            } catch (ActivityNotFoundException exception) {
                ToastUtils.showToast(ReaderPostListActivity.this, R.string.reader_toast_err_share_intent);
            }
        } else {
            ToastUtils.showToast(ReaderPostListActivity.this, R.string.reader_toast_err_share_intent);
        }
    }

    /*
     * show fragment containing list of latest posts for a specific tag
     */
    private void showListFragmentForTag(final ReaderTag tag, ReaderPostListType listType) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForTag(tag, listType);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();

        setTitle(tag.getTagDisplayName());
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForBlog(blogId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();

        String title = ReaderBlogTable.getBlogName(blogId);
        if (title.isEmpty()) {
            title = getString(R.string.reader_title_blog_preview);
        }
        setTitle(title);
    }

    private void showListFragmentForFeed(long feedId) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForFeed(feedId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();

        String title = ReaderBlogTable.getFeedName(feedId);
        if (title.isEmpty()) {
            title = getString(R.string.reader_title_blog_preview);
        }
        setTitle(title);
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment =
                getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null) {
            return null;
        }
        return ((ReaderPostListFragment) fragment);
    }
}
