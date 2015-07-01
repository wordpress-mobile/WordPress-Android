package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ProfilingUtils;
import org.xmlrpc.android.ApiHelper;

public class PostsListActivity extends AppCompatActivity {

    public static final String EXTRA_VIEW_PAGES = "viewPages";
    public static final String EXTRA_ERROR_MSG = "errorMessage";
    public static final String EXTRA_ERROR_INFO_TITLE = "errorInfoTitle";
    public static final String EXTRA_ERROR_INFO_LINK = "errorInfoLink";

    private boolean mIsPage = false;
    private PostsListFragment mPostList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfilingUtils.split("PostsListActivity.onCreate");
        ProfilingUtils.dump();

        setContentView(R.layout.post_list_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fm = getFragmentManager();
        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mIsPage = extras.getBoolean(EXTRA_VIEW_PAGES);
            showErrorDialogIfNeeded(extras);
        }

        if (mIsPage) {
            getSupportActionBar().setTitle(getString(R.string.pages));
        } else {
            getSupportActionBar().setTitle(getString(R.string.posts));
        }

        WordPress.currentPost = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        bumpActivityAnalytics();
    }

    protected void bumpActivityAnalytics() {
        ActivityId.trackLastActivity(ActivityId.POSTS);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    private void showPostUploadErrorAlert(String errorMessage, String infoTitle,
                                          final String infoURL) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PostsListActivity.this);
        dialogBuilder.setTitle(getResources().getText(R.string.error));
        dialogBuilder.setMessage(errorMessage);
        dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.
                    }
                }
        );
        if (infoTitle != null && infoURL != null) {
            dialogBuilder.setNeutralButton(infoTitle,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(infoURL)));
                    }
                });
        }
        dialogBuilder.setCancelable(true);
        if (!isFinishing())
            dialogBuilder.create().show();
    }

    private void showErrorDialogIfNeeded(Bundle extras) {
        if (extras == null) {
            return;
        }
        String errorMessage = extras.getString(EXTRA_ERROR_MSG);
        if (!TextUtils.isEmpty(errorMessage)) {
            String errorInfoTitle = extras.getString(EXTRA_ERROR_INFO_TITLE);
            String errorInfoLink = extras.getString(EXTRA_ERROR_INFO_LINK);
            showPostUploadErrorAlert(errorMessage, errorInfoTitle, errorInfoLink);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RequestCodes.EDIT_POST
                && resultCode == RESULT_OK
                && data != null
                && data.getBooleanExtra(EditPostActivity.EXTRA_SHOULD_REFRESH, false)) {
            mPostList.getPostListAdapter().loadPosts();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void refreshComments() {
        new refreshCommentsTask().execute();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }

    private class refreshCommentsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            Object[] commentParams = {WordPress.currentBlog.getRemoteBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword()};

            try {
                ApiHelper.refreshComments(WordPress.currentBlog, commentParams);
            } catch (final Exception e) {
                AppLog.e(AppLog.T.POSTS, e);
            }
            return null;
        }
    }
}
