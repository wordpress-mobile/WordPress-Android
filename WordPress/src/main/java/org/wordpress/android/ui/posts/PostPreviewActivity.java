package org.wordpress.android.ui.posts;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;

public class PostPreviewActivity extends AppCompatActivity {

    public static final String ARG_LOCAL_POST_ID = "local_post_id";
    public static final String ARG_LOCAL_BLOG_ID = "local_blog_id";
    public static final String ARG_IS_PAGE = "is_page";

    private long mLocalPostId;
    private int mLocalBlogId;
    private boolean mIsPage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.post_preview_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        if (savedInstanceState != null) {
            mLocalPostId = savedInstanceState.getLong(ARG_LOCAL_POST_ID);
            mLocalBlogId = savedInstanceState.getInt(ARG_LOCAL_BLOG_ID);
            mIsPage = savedInstanceState.getBoolean(ARG_IS_PAGE);
        } else {
            mLocalPostId = getIntent().getLongExtra(ARG_LOCAL_POST_ID, 0);
            mLocalBlogId = getIntent().getIntExtra(ARG_LOCAL_BLOG_ID, 0);
            mIsPage = getIntent().getBooleanExtra(ARG_IS_PAGE, false);
        }

        setTitle(mIsPage? getString(R.string.preview_page) : getString(R.string.preview_post));

        if (!hasPreviewFragment()) {
            showPreviewFragment();
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    private void showPreviewFragment() {
        FragmentManager fm = getFragmentManager();
        fm.executePendingTransactions();

        String tagForFragment = getString(R.string.fragment_tag_post_preview);
        Fragment fragment = PostPreviewFragment.newInstance(mLocalBlogId, mLocalPostId, mIsPage);

        fm.beginTransaction()
          .replace(R.id.fragment_container, fragment, tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
          .commitAllowingStateLoss();
    }

    private boolean hasPreviewFragment() {
        return (getPreviewFragment() != null);
    }

    private PostPreviewFragment getPreviewFragment() {
        String tagForFragment = getString(R.string.fragment_tag_post_preview);
        Fragment fragment = getFragmentManager().findFragmentByTag(tagForFragment);
        if (fragment != null) {
            return (PostPreviewFragment) fragment;
        } else {
            return null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong(ARG_LOCAL_POST_ID, mLocalPostId);
        outState.putInt(ARG_LOCAL_BLOG_ID, mLocalBlogId);
        outState.putBoolean(ARG_IS_PAGE, mIsPage);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.post_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_edit) {
            ActivityLauncher.editBlogPostOrPageForResult(this, mLocalPostId, mIsPage);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // reload preview if used returned from editor
        if (requestCode == RequestCodes.EDIT_POST && resultCode == RESULT_OK) {
            PostPreviewFragment fragment = getPreviewFragment();
            if (fragment != null) {
                fragment.loadPreview();
            }
            // this will tell PostListActivity is needs to refresh
            setResult(RESULT_OK, data);
        }
    }
}
