package org.wordpress.android.ui.reader;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.reader.adapters.ReaderPostAdapter;

public class ReaderBlogDetailActivity extends SherlockFragmentActivity {

    private ReaderBlogInfoHeader mBlogInfoHeader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_blog_detail);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        long blogId = getIntent().getLongExtra(ReaderActivity.ARG_BLOG_ID, 0);
        mBlogInfoHeader = (ReaderBlogInfoHeader) findViewById(R.id.blog_info_header);
        mBlogInfoHeader.setBlogId(blogId);
        showListFragmentForBlog(blogId);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgress() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_loading);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_loading);
        progressBar.setVisibility(View.GONE);
    }

    /*
     * show fragment containing list of latest posts in a specific blog
     */
    private void showListFragmentForBlog(long blogId) {
        Fragment fragment = ReaderPostListFragment.newInstance(blogId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, getString(R.string.fragment_tag_reader_post_list))
                .commit();
    }

    private ReaderPostListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(R.string.fragment_tag_reader_post_list));
        if (fragment == null)
            return null;
        return ((ReaderPostListFragment) fragment);
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }
}
