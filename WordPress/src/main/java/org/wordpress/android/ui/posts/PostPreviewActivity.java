package org.wordpress.android.ui.posts;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.PostActionBuilder;
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged;
import org.wordpress.android.fluxc.model.PostModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.post.PostStatus;
import org.wordpress.android.fluxc.store.PostStore;
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged;
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded;
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload;
import org.wordpress.android.support.ZendeskHelper;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.accounts.HelpActivity.Origin;
import org.wordpress.android.ui.uploads.PostEvents;
import org.wordpress.android.ui.uploads.UploadService;
import org.wordpress.android.util.AccessibilityUtils;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.ui.posts.EditPostActivity.EXTRA_POST_LOCAL_ID;

public class PostPreviewActivity extends AppCompatActivity implements
        BasicFragmentDialog.BasicDialogNegativeClickInterface,
        BasicFragmentDialog.BasicDialogPositiveClickInterface {
    private static final String TAG_DISCARDING_CHANGES_ERROR_DIALOG = "tag_discarding_changes_error_dialog";

    private boolean mIsDiscardingChanges;
    private boolean mIsUpdatingPost;

    private PostModel mPost;
    private PostModel mPostWithLocalChanges;
    private SiteModel mSite;
    private ViewGroup mMessageView;

    @Inject Dispatcher mDispatcher;
    @Inject PostStore mPostStore;
    @Inject ZendeskHelper mZendeskHelper;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.post_preview_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        int localPostId;
        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
            localPostId = getIntent().getIntExtra(EXTRA_POST_LOCAL_ID, 0);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            localPostId = savedInstanceState.getInt(EXTRA_POST_LOCAL_ID);
        }
        mPost = mPostStore.getPostByLocalPostId(localPostId);
        if (mSite == null || mPost == null) {
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

        if (mPost == null || (mPost = mPostStore.getPostByLocalPostId(mPost.getId())) == null) {
            finish();
            return;
        }
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
        FragmentManager fm = getSupportFragmentManager();
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
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tagForFragment);
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
        outState.putSerializable(EXTRA_POST_LOCAL_ID, mPost.getId());
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
        mMessageView = findViewById(R.id.message_container);

        if (mPost == null
            || mIsUpdatingPost
            || UploadService.isPostUploadingOrQueued(mPost)
            || (!mPost.isLocallyChanged() && !mPost.isLocalDraft())
               && PostStatus.fromPost(mPost) != PostStatus.DRAFT) {
            mMessageView.setVisibility(View.GONE);
            return;
        }

        TextView messageText = mMessageView.findViewById(R.id.message_text);
        if (mPost.isLocallyChanged()) {
            messageText.setText(R.string.local_changes_explainer);
        } else if (mPost.isLocalDraft()) {
            messageText.setText(R.string.local_draft_explainer);
        } else if (PostStatus.fromPost(mPost) == PostStatus.DRAFT) {
            messageText.setText(R.string.draft_explainer);
        }

        // publish applies to both local draft and local changes
        View btnPublish = mMessageView.findViewById(R.id.btn_publish);
        btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AniUtils.animateBottomBar(mMessageView, false);
                publishPost();
            }
        });

        // discard changes applies to only local changes
        View btnDiscardChanges = mMessageView.findViewById(R.id.btn_discard_changes);
        btnDiscardChanges.setVisibility(mPost.isLocallyChanged() ? View.VISIBLE : View.GONE);
        if (mPost.isLocallyChanged()) {
            btnDiscardChanges.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AniUtils.animateBottomBar(mMessageView, false);
                    discardChanges();
                    AnalyticsTracker.track(Stat.EDITOR_DISCARDED_CHANGES);
                }
            });
        }

        // if both buttons are visible, show them below the message instead of to the right of it
        if (mPost.isLocallyChanged()) {
            RelativeLayout.LayoutParams paramsMessage = (RelativeLayout.LayoutParams) messageText.getLayoutParams();
            paramsMessage.removeRule(RelativeLayout.LEFT_OF);
            paramsMessage.removeRule(RelativeLayout.START_OF);
            paramsMessage.removeRule(RelativeLayout.CENTER_VERTICAL);

            ViewGroup.MarginLayoutParams marginsMessage = (ViewGroup.MarginLayoutParams) messageText.getLayoutParams();
            marginsMessage.bottomMargin = getResources().getDimensionPixelSize(R.dimen.snackbar_message_margin_bottom);

            ViewGroup buttonsView = mMessageView.findViewById(R.id.layout_buttons);
            RelativeLayout.LayoutParams paramsButtons = (RelativeLayout.LayoutParams) buttonsView.getLayoutParams();
            paramsButtons.addRule(RelativeLayout.BELOW, R.id.message_text);
            ViewGroup.MarginLayoutParams marginsButtons = (ViewGroup.MarginLayoutParams) buttonsView.getLayoutParams();
            marginsButtons.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margin_large);
        }

        // first set message bar to invisible so it takes up space, then animate it in
        // after a brief delay to give time for preview to render first
        mMessageView.setVisibility(View.INVISIBLE);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && mMessageView.getVisibility() != View.VISIBLE) {
                    AniUtils.animateBottomBar(mMessageView, true);
                }
            }
        }, 1000);
    }

    /*
     * discard local changes for this post, replacing it with the latest version from the server
     */
    private void discardChanges() {
        if (isFinishing() || !NetworkUtils.checkConnection(this)) {
            return;
        }

        if (mIsUpdatingPost) {
            AppLog.d(AppLog.T.POSTS, "post preview > already updating post");
        } else {
            mIsDiscardingChanges = true;
            showProgress();
            mPostWithLocalChanges = mPost.clone();

            RemotePostPayload payload = new RemotePostPayload(mPost, mSite);
            mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload));
        }
    }

    private void publishPost() {
        if (!isFinishing() && NetworkUtils.checkConnection(this)) {
            if (!mPost.isLocalDraft()) {
                Map<String, Object> properties = new HashMap<>();
                properties.put(AnalyticsUtils.HAS_GUTENBERG_BLOCKS_KEY,
                        PostUtils.contentContainsGutenbergBlocks(mPost.getContent()));
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.EDITOR_UPDATED_POST, mSite, properties);
            }

            if (PostStatus.fromPost(mPost) == PostStatus.DRAFT) {
                // Remote draft being published
                mPost.setStatus(PostStatus.PUBLISHED.toString());
                UploadService.uploadPostAndTrackAnalytics(this, mPost);
            } else if (mPost.isLocalDraft() && PostStatus.fromPost(mPost) == PostStatus.PUBLISHED) {
                // Local draft being published
                UploadService.uploadPostAndTrackAnalytics(this, mPost);
            } else {
                // Not a first-time publish
                UploadService.uploadPost(this, mPost);
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PostEvents.PostUploadStarted event) {
        if (event.post.getLocalSiteId() == mSite.getId()) {
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

    private void showDialogError() {
        BasicFragmentDialog dialog = new BasicFragmentDialog();
        dialog.initialize(TAG_DISCARDING_CHANGES_ERROR_DIALOG, "", getString(R.string.local_changes_discarding_error),
                getString(R.string.contact_support), getString(R.string.cancel), null);
        dialog.show(getSupportFragmentManager(), TAG_DISCARDING_CHANGES_ERROR_DIALOG);
    }

    @Override
    public void onNegativeClicked(@NotNull String tag) {
        switch (tag) {
            case TAG_DISCARDING_CHANGES_ERROR_DIALOG:
                // noop
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog tag is not recognized");
                throw new UnsupportedOperationException("Dialog tag is not recognized");
        }
    }

    @Override
    public void onPositiveClicked(@NotNull String tag) {
        switch (tag) {
            case TAG_DISCARDING_CHANGES_ERROR_DIALOG:
                mZendeskHelper.createNewTicket(this, Origin.DISCARD_CHANGES, mSite);
                break;
            default:
                AppLog.e(T.EDITOR, "Dialog tag is not recognized");
                throw new UnsupportedOperationException("Dialog tag is not recognized");
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPostChanged(OnPostChanged event) {
        if (event.causeOfChange instanceof CauseOfOnPostChanged.UpdatePost) {
            hideProgress();

            if (!event.isError()) {
                if (mIsUpdatingPost) {
                    mIsUpdatingPost = false;
                    mPost = mPostStore.getPostByLocalPostId(mPost.getId());
                    refreshPreview();

                    if (mMessageView != null) {
                        Snackbar.make(mMessageView, getString(R.string.local_changes_discarded),
                                AccessibilityUtils.getSnackbarDuration(PostPreviewActivity.this, Snackbar.LENGTH_LONG))
                                .setAction(getString(R.string.undo), new OnClickListener() {
                                    @Override public void onClick(View view) {
                                        AnalyticsTracker.track(Stat.EDITOR_DISCARDED_CHANGES_UNDO);
                                        RemotePostPayload payload = new RemotePostPayload(mPostWithLocalChanges, mSite);
                                        mDispatcher.dispatch(PostActionBuilder.newFetchPostAction(payload));
                                        mPost = mPostWithLocalChanges.clone();
                                        refreshPreview();
                                    }
                                })
                                .show();
                    }
                }

                if (mIsDiscardingChanges) {
                    mIsDiscardingChanges = false;
                    mPost = mPostStore.getPostByLocalPostId(mPost.getId());
                    mDispatcher.dispatch(PostActionBuilder.newUpdatePostAction(mPost));
                    mIsUpdatingPost = true;
                }
            } else {
                if (mIsDiscardingChanges) {
                    showDialogError();
                }

                mIsDiscardingChanges = false;
                mIsUpdatingPost = false;
                AppLog.e(AppLog.T.POSTS, "UPDATE_POST failed: " + event.error.type + " - " + event.error.message);
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
