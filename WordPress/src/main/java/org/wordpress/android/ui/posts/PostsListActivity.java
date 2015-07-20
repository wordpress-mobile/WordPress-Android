package org.wordpress.android.ui.posts;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.util.ProfilingUtils;

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

        mIsPage = getIntent().getBooleanExtra(EXTRA_VIEW_PAGES, false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(mIsPage ? R.string.pages : R.string.posts));
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fm = getFragmentManager();
        mPostList = (PostsListFragment) fm.findFragmentById(R.id.postList);

        showErrorDialogIfNeeded(getIntent().getExtras());
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

    /*
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
        if ((requestCode == RequestCodes.EDIT_POST || requestCode == RequestCodes.PREVIEW_POST)
                && resultCode == RESULT_OK
                && data != null
                && data.getBooleanExtra(EditPostActivity.EXTRA_SHOULD_REFRESH, false)) {
            mPostList.getPostListAdapter().loadPosts();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        super.onSaveInstanceState(outState);
    }
}
