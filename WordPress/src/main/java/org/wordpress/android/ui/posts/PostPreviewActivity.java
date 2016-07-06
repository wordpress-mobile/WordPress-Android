package org.wordpress.android.ui.posts;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostStatus;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.xmlrpc.android.ApiHelper;

import de.greenrobot.event.EventBus;

public class PostPreviewActivity extends AppCompatActivity {

    public static final String ARG_LOCAL_POST_ID = "local_post_id";
    public static final String ARG_LOCAL_BLOG_ID = "local_blog_id";
    public static final String ARG_IS_PAGE = "is_page";

    private long mLocalPostId;
    private int mLocalBlogId;
    private boolean mIsPage;
    private boolean mIsUpdatingPost;

    private Post mPost;

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

        setTitle(mIsPage ? getString(R.string.preview_page) : getString(R.string.preview_post));
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);

        mPost = WordPress.wpDB.getPostForLocalTablePostId(mLocalPostId);
        if (hasPreviewFragment()) {
            refreshPreview();
        } else {
            showPreviewFragment();
        }
        showMessageViewIfNecessary();
    }

    @Override
    protected void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
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
        Fragment fragment = PostPreviewFragment.newInstance(mLocalBlogId, mLocalPostId);

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

    private void refreshPreview() {
        if (!isFinishing()) {
            PostPreviewFragment fragment = getPreviewFragment();
            if (fragment != null) {
                fragment.refreshPreview();
            }
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

    /*
     * if this is a local draft or has local changes, show the message explaining what these
     * states mean, and hook up the publish and revert buttons
     */
    private void showMessageViewIfNecessary() {
        final ViewGroup messageView = (ViewGroup) findViewById(R.id.message_container);

        if (mPost == null
                || mIsUpdatingPost
                || PostUploadService.isPostUploading(mPost.getLocalTablePostId())
                || (!mPost.isLocalChange() && !mPost.isLocalDraft()) && mPost.getStatusEnum() != PostStatus.DRAFT) {
            messageView.setVisibility(View.GONE);
            return;
        }

        TextView messageText = (TextView) messageView.findViewById(R.id.message_text);
        if (mPost.isLocalChange()) {
            messageText.setText(R.string.local_changes_explainer);
        } else if (mPost.isLocalDraft()) {
            messageText.setText(R.string.local_draft_explainer);
        } else if (mPost.getStatusEnum() == PostStatus.DRAFT) {
            messageText.setText(R.string.draft_explainer);
        }

        // publish applies to both local draft and local changes
        View btnPublish = messageView.findViewById(R.id.btn_publish);
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AniUtils.animateBottomBar(messageView, false);
                publishPost();
            }
        });

        // revert applies to only local changes
        View btnRevert = messageView.findViewById(R.id.btn_revert);
        btnRevert.setVisibility(mPost.isLocalChange() ? View.VISIBLE : View.GONE);
        if (mPost.isLocalChange() ) {
            btnRevert.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AniUtils.animateBottomBar(messageView, false);
                    revertPost();
                    AnalyticsTracker.track(Stat.EDITOR_DISCARDED_CHANGES);
                }
            });
        }

        // if both buttons are visible, show them below the message instead of to the right of it
        if (mPost.isLocalChange()) {
            RelativeLayout.LayoutParams paramsMessage = (RelativeLayout.LayoutParams) messageText.getLayoutParams();
            // passing "0" removes the param (necessary since removeRule() is API 17+)
            paramsMessage.addRule(RelativeLayout.LEFT_OF, 0);
            paramsMessage.addRule(RelativeLayout.CENTER_VERTICAL, 0);
            ViewGroup.MarginLayoutParams marginsMessage = (ViewGroup.MarginLayoutParams) messageText.getLayoutParams();
            marginsMessage.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margin_small);

            ViewGroup buttonsView = (ViewGroup) messageView.findViewById(R.id.layout_buttons);
            RelativeLayout.LayoutParams paramsButtons = (RelativeLayout.LayoutParams) buttonsView.getLayoutParams();
            paramsButtons.addRule(RelativeLayout.BELOW, R.id.message_text);
            ViewGroup.MarginLayoutParams marginsButtons = (ViewGroup.MarginLayoutParams) buttonsView.getLayoutParams();
            marginsButtons.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margin_large);
        }

        // first set message bar to invisible so it takes up space, then animate it in
        // after a brief delay to give time for preview to render first
        messageView.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && messageView.getVisibility() != View.VISIBLE) {
                    AniUtils.animateBottomBar(messageView, true);
                }
            }
        }, 1000);
    }

    /*
     * reverts local changes for this post, replacing it with the latest version from the server
     */
    private void revertPost() {
        if (isFinishing() || !NetworkUtils.checkConnection(this)) {
            return;
        }

        if (mIsUpdatingPost) {
            AppLog.d(AppLog.T.POSTS, "post preview > already updating post");
        } else {
            new UpdatePostTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void publishPost() {
        if (!isFinishing() && NetworkUtils.checkConnection(this)) {
            if (!mPost.isLocalDraft()) {
                AnalyticsUtils.trackWithBlogDetails(
                        AnalyticsTracker.Stat.EDITOR_UPDATED_POST,
                        WordPress.getBlog(mPost.getLocalTableBlogId())
                );
            }

            if (mPost.getStatusEnum() == PostStatus.DRAFT) {
                mPost.setPostStatus(PostStatus.toString(PostStatus.PUBLISHED));
            }

            PostUploadService.addPostToUpload(mPost);
            startService(new Intent(this, PostUploadService.class));
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadStarted event) {
        if (event.mLocalBlogId == mLocalBlogId) {
            showProgress();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadEnded event) {
        if (event.mLocalBlogId == mLocalBlogId) {
            hideProgress();
            refreshPreview();
        }
    }

    private void showProgress() {
        if (!isFinishing()) {
            findViewById(R.id.progress).setVisibility(View.VISIBLE);
        }
    }

    private void hideProgress() {
        if (!isFinishing()) {
            findViewById(R.id.progress).setVisibility(View.GONE);
        }
    }

    private class UpdatePostTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mIsUpdatingPost = true;
            showProgress();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            mIsUpdatingPost = false;
            hideProgress();
        }

        @Override
        protected Boolean doInBackground(Void... nada) {
            return ApiHelper.updateSinglePost(mLocalBlogId, mPost.getRemotePostId(), mIsPage);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!isFinishing()) {
                hideProgress();
                if (result) {
                    refreshPreview();
                }
            }
            mIsUpdatingPost = false;
        }
    }
}
