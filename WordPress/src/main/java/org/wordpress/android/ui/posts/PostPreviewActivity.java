package org.wordpress.android.ui.posts;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
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

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.posts.services.PostEvents;
import org.wordpress.android.ui.posts.services.PostUploadService;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class PostPreviewActivity extends AppCompatActivity {
    public static final String EXTRA_POST = "postModel";

    private boolean mIsUpdatingPost;

    private PostModel mPost;
    private SiteModel mSite;

    @Inject Dispatcher mDispatcher;
    @Inject PostStore mPostStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.post_preview_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        if (savedInstanceState != null) {
            mPost = (PostModel) savedInstanceState.getSerializable(EXTRA_POST);
        } else {
            mPost = (PostModel) getIntent().getSerializableExtra(EXTRA_POST);
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }
        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }


        setTitle(mPost.isPage() ? getString(R.string.preview_page) : getString(R.string.preview_post));
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        mDispatcher.register(this);

        mPost = mPostStore.getPostByLocalPostId(mPost.getId());
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
        mDispatcher.unregister(this);
        super.onPause();
    }

    private void showPreviewFragment() {
        FragmentManager fm = getFragmentManager();
        fm.executePendingTransactions();

        String tagForFragment = getString(R.string.fragment_tag_post_preview);
        Fragment fragment = PostPreviewFragment.newInstance(mSite, mPost);

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
                fragment.setPost(mPost);
                fragment.refreshPreview();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(EXTRA_POST, mPost);
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
            ActivityLauncher.editPostOrPageForResult(this, mSite, mPost);
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
                || PostUploadService.isPostUploading(mPost)
                || (!mPost.isLocallyChanged() && !mPost.isLocalDraft())
                && PostStatus.fromPost(mPost) != PostStatus.DRAFT) {
            messageView.setVisibility(View.GONE);
            return;
        }

        TextView messageText = (TextView) messageView.findViewById(R.id.message_text);
        if (mPost.isLocallyChanged()) {
            messageText.setText(R.string.local_changes_explainer);
        } else if (mPost.isLocalDraft()) {
            messageText.setText(R.string.local_draft_explainer);
        } else if (PostStatus.fromPost(mPost) == PostStatus.DRAFT) {
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
        btnRevert.setVisibility(mPost.isLocallyChanged() ? View.VISIBLE : View.GONE);
        if (mPost.isLocallyChanged()) {
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
        if (mPost.isLocallyChanged()) {
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
            mIsUpdatingPost = true;
            showProgress();

            RemotePostPayload payload = new RemotePostPayload(mPost, mSite);
            mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload));
        }
    }

    private void publishPost() {
        if (!isFinishing() && NetworkUtils.checkConnection(this)) {
            if (!mPost.isLocalDraft()) {
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_UPDATED_POST, mSite);
            }

            if (PostStatus.fromPost(mPost) == PostStatus.DRAFT) {
                // Remote draft being published
                mPost.setStatus(PostStatus.PUBLISHED.toString());
                PostUploadService.addPostToUploadAndTrackAnalytics(mPost);
            } else if (mPost.isLocalDraft() && PostStatus.fromPost(mPost) == PostStatus.PUBLISHED) {
                // Local draft being published
                PostUploadService.addPostToUploadAndTrackAnalytics(mPost);
            } else {
                // Not a first-time publish
                PostUploadService.addPostToUpload(mPost);
            }
            startService(new Intent(this, PostUploadService.class));
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadStarted event) {
        if (event.mLocalBlogId == mSite.getId()) {
            showProgress();
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void OnPostChanged(OnPostChanged event) {
        switch (event.causeOfChange) {
            case UPDATE_POST:
                mIsUpdatingPost = false;
                hideProgress();
                if (event.isError()) {
                    // TODO: Report error to user
                    AppLog.e(AppLog.T.POSTS, "UPDATE_POST failed: " + event.error.type + " - " + event.error.message);
                } else {
                    mPost = mPostStore.getPostByLocalPostId(mPost.getId());
                    refreshPreview();
                }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostUploaded(OnPostUploaded event) {
        if (event.post.getLocalSiteId() == mSite.getId()) {
            hideProgress();
            refreshPreview();
        }
    }
}
