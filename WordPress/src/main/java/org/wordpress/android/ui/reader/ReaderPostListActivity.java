package org.wordpress.android.ui.reader;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.util.LocaleManager;

/*
 * serves as the host for ReaderPostListFragment when showing blog preview & tag preview
 */
public class ReaderPostListActivity extends AppCompatActivity {
    private ReaderPostListType mPostListType;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
                    } else {
                        showListFragmentForBlog(blogId);
                    }
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
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);
        if (appBarLayout != null) {
            CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams();
            lp.height = 0;
            appBarLayout.setLayoutParams(lp);
        }

        // disabling any CoordinatorLayout behavior for scrolling
        Toolbar toolbarWithSpinner = (Toolbar) findViewById(R.id.toolbar_with_spinner);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
