package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.ReaderTag;

import javax.annotation.Nonnull;

/*
 * this activity serves as the host for ReaderPostListFragment when previewing
 * posts with a specific tag or in a specific blog
 */

public class ReaderPostListActivity extends ActionBarActivity {

    private ReaderTypes.ReaderPostListType mPostListType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.color_status_bar));
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity_post_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ReaderConstants.ARG_POST_LIST_TYPE)) {
                mPostListType = (ReaderTypes.ReaderPostListType) intent.getSerializableExtra(ReaderConstants.ARG_POST_LIST_TYPE);
            } else {
                mPostListType = ReaderTypes.DEFAULT_POST_LIST_TYPE;
            }

            switch (mPostListType) {
                case TAG_PREVIEW:
                    setTitle(R.string.reader_title_tag_preview);
                    break;
                case BLOG_PREVIEW:
                    setTitle(R.string.reader_title_blog_preview);
                    break;
            }
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.READER_ACCESSED);

            switch (getPostListType()) {
                case TAG_PREVIEW:
                    ReaderTag tag = (ReaderTag) intent.getSerializableExtra(ReaderConstants.ARG_TAG);
                    showListFragmentForTag(tag);
                    break;
                case BLOG_PREVIEW:
                    long blogId = intent.getLongExtra(ReaderConstants.ARG_BLOG_ID, 0);
                    String blogUrl = intent.getStringExtra(ReaderConstants.ARG_BLOG_URL);
                    showListFragmentForBlog(blogId, blogUrl);
                    break;
            }
        }
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        ReaderPostListFragment fragment = getListFragment();
        if (fragment == null || !fragment.goBackInTagHistory()) {
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

    ReaderTypes.ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
    }

    /*
     * show fragment containing list of latest posts for a specific tag
     */
    private void showListFragmentForTag(final ReaderTag tag) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstance(tag, ReaderTypes.ReaderPostListType.TAG_PREVIEW);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId, String blogUrl) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstance(blogId, blogUrl);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null) {
            return null;
        }
        return ((ReaderPostListFragment) fragment);
    }
}
