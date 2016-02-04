package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.DualPaneContentActivity;
import org.wordpress.android.util.ToastUtils;

/**
 * Serves as the host for {@link PostsListFragment} when showing uploaded posts/pages.
 */
public class PostsListActivity extends DualPaneContentActivity {
    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";
    public static final String EXTRA_ERROR_INFO_TITLE = "errorInfoTitle";
    public static final String EXTRA_ERROR_INFO_LINK = "errorInfoLink";
    public static final String EXTRA_BLOG_LOCAL_ID = "EXTRA_BLOG_LOCAL_ID";

    private boolean mIsPage = false;
    private PostsListFragment mPostList;

    @Override
    protected String getContentFragmentTag() {
        return PostsListFragment.class.getSimpleName();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        FragmentManager fragmentManager = getSupportFragmentManager();
        mPostList = (PostsListFragment) fragmentManager.findFragmentByTag(getContentFragmentTag());

        if (mPostList == null) {
            mPostList = (PostsListFragment) Fragment.instantiate(this, PostsListFragment.class.getName(),
                    getIntent().getExtras());
            mPostList.setInitialSavedState(getFragmentSavedState());
            fragmentManager.beginTransaction().replace(R.id.fragment_container, mPostList, getContentFragmentTag()).commit();
        }

        showErrorDialogIfNeeded(getIntent().getExtras());
        showWarningToastIfNeeded(getIntent().getExtras());
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(mIsPage ? ActivityId.PAGES : ActivityId.POSTS);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
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
        final String errorInfoTitle = extras.getString(EXTRA_ERROR_INFO_TITLE);
        final String errorInfoLink = extras.getString(EXTRA_ERROR_INFO_LINK);

        if (TextUtils.isEmpty(errorMessage)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getText(R.string.error))
                .setMessage(errorMessage)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true);

        // enable browsing error link if one exists
        if (!TextUtils.isEmpty(errorInfoTitle) && !TextUtils.isEmpty(errorInfoLink)) {
            builder.setNeutralButton(errorInfoTitle,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(errorInfoLink)));
                        }
                    });
        }

        builder.create().show();
    }

    /**
     * Show a toast when the user taps a Post Upload notification referencing a post that's not from the current
     * selected Blog
     */
    private void showWarningToastIfNeeded(Bundle extras) {
        if (extras == null || !extras.containsKey(EXTRA_BLOG_LOCAL_ID) || isFinishing()) {
            return;
        }
        if (extras.getInt(EXTRA_BLOG_LOCAL_ID, -1) != WordPress.getCurrentLocalTableBlogId()) {
            ToastUtils.showToast(this, R.string.error_open_list_from_notification);
        }
    }

    public boolean isRefreshing() {
        return mPostList.isRefreshing();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}
