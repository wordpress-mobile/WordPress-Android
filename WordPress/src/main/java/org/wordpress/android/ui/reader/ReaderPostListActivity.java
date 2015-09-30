package org.wordpress.android.ui.reader;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;

import javax.annotation.Nonnull;

/*
 * serves as the host for ReaderPostListFragment when showing blog preview & tag preview
 */

public class ReaderPostListActivity extends AppCompatActivity {

    private ReaderPostListType mPostListType;

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

    private ReaderPostListType getPostListType() {
        return (mPostListType != null ? mPostListType : ReaderTypes.DEFAULT_POST_LIST_TYPE);
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

    /*
     * show fragment containing list of latest posts for a specific tag
     */
    private void showListFragmentForTag(final ReaderTag tag, ReaderPostListType listType) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForTag(tag, listType);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForBlog(blogId);
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private void showListFragmentForFeed(long feedId) {
        if (isFinishing()) {
            return;
        }
        Fragment fragment = ReaderPostListFragment.newInstanceForFeed(feedId);
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
