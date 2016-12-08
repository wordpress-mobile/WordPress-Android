package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.CommentActionBuilder;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.CommentStore;
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload;
import org.wordpress.android.models.CommentList;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.comments.CommentsListFragment.OnCommentSelectedListener;
import org.wordpress.android.ui.notifications.NotificationFragment;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

public class CommentsActivity extends AppCompatActivity
        implements OnCommentSelectedListener,
                   NotificationFragment.OnPostClickListener,
                   CommentActions.OnCommentActionListener,
                   CommentActions.OnCommentChangeListener {
    private static final String KEY_SELECTED_COMMENT = "selected_comment_id";
    static final String KEY_AUTO_REFRESHED = "has_auto_refreshed";
    static final String KEY_EMPTY_VIEW_MESSAGE = "empty_view_message";
    private static final String SAVED_COMMENTS_STATUS_TYPE = "saved_comments_status_type";
    private final CommentList mTrashedComments = new CommentList();

    private CommentStatus mCurrentCommentStatusType = CommentStatus.ALL;

    private SiteModel mSite;
    private CommentModel mComment;

    @Inject Dispatcher mDispatcher;
    @Inject CommentStore mCommentStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.comment_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setTitle(R.string.comments);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
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

        if (getIntent() != null && getIntent().hasExtra(SAVED_COMMENTS_STATUS_TYPE)) {
            mCurrentCommentStatusType = (CommentStatus) getIntent().getSerializableExtra(SAVED_COMMENTS_STATUS_TYPE);
        } else {
            // Read the value from app preferences here. Default to 0 - All
            mCurrentCommentStatusType = AppPrefs.getCommentsStatusFilter().toCommentStatus();
        }

        if (savedInstanceState == null) {
            CommentsListFragment commentsListFragment = new CommentsListFragment();
            // initialize comment status filter first time
            commentsListFragment.setCommentStatusFilter(mCurrentCommentStatusType);
            getFragmentManager().beginTransaction()
                    .add(R.id.layout_fragment_container, commentsListFragment, getString(R.string
                            .fragment_tag_comment_list))
                    .commitAllowingStateLoss();
        } else {
            getIntent().putExtra(KEY_AUTO_REFRESHED, savedInstanceState.getBoolean(KEY_AUTO_REFRESHED));
            getIntent().putExtra(KEY_EMPTY_VIEW_MESSAGE, savedInstanceState.getString(KEY_EMPTY_VIEW_MESSAGE));
            mComment = (CommentModel) savedInstanceState.getSerializable(KEY_SELECTED_COMMENT);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.COMMENTS);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.d(AppLog.T.COMMENTS, "comment activity new intent");
    }


    private CommentDetailFragment getDetailFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(
                R.string.fragment_tag_comment_detail));
        if (fragment == null) {
            return null;
        }
        return (CommentDetailFragment) fragment;
    }

    private boolean hasDetailFragment() {
        return (getDetailFragment() != null);
    }

    private CommentsListFragment getListFragment() {
        Fragment fragment = getFragmentManager().findFragmentByTag(getString(
                R.string.fragment_tag_comment_list));
        if (fragment == null) {
            return null;
        }
        return (CommentsListFragment) fragment;
    }

    private boolean hasListFragment() {
        return (getListFragment() != null);
    }

    private void showReaderFragment(long remoteBlogId, long postId) {
        FragmentManager fm = getFragmentManager();
        fm.executePendingTransactions();

        Fragment fragment = ReaderPostDetailFragment.newInstance(remoteBlogId, postId);
        FragmentTransaction ft = fm.beginTransaction();
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        ft.add(R.id.layout_fragment_container, fragment, tagForFragment)
          .addToBackStack(tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        if (hasDetailFragment())
            ft.hide(getDetailFragment());
        ft.commit();
    }

    /*
     * called from comment list when user taps a comment
     */
    @Override
    public void onCommentSelected(long commentId) {
        FragmentManager fm = getFragmentManager();
        if (fm == null) {
            return;
        }

        mComment = mCommentStore.getCommentBySiteAndRemoteId(mSite, commentId);

        fm.executePendingTransactions();
        CommentsListFragment listFragment = getListFragment();

        FragmentTransaction ft = fm.beginTransaction();
        String tagForFragment = getString(R.string.fragment_tag_comment_detail);
        CommentDetailFragment detailFragment = CommentDetailFragment.newInstance(mSite, mComment);
        ft.add(R.id.layout_fragment_container, detailFragment, tagForFragment).addToBackStack(tagForFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        if (listFragment != null) {
            ft.hide(listFragment);
        }
        ft.commitAllowingStateLoss();
    }

    /*
     * called from comment detail when user taps a link to a post - show the post in a
     * reader detail fragment
     */
    @Override
    public void onPostClicked(Note note, long remoteBlogId, int postId) {
        showReaderFragment(remoteBlogId, postId);
    }

    /*
     * reload the comment list from existing data
     */
    private void reloadCommentList() {
        CommentsListFragment listFragment = getListFragment();
        if (listFragment != null)
            listFragment.loadComments();
    }

    /*
     * tell the comment list to get recent comments from server
     */
    private void updateCommentList() {
        CommentsListFragment listFragment = getListFragment();
        if (listFragment != null) {
            //listFragment.setRefreshing(true);
            listFragment.setCommentStatusFilter(mCurrentCommentStatusType);
            listFragment.updateComments(false);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(WordPress.SITE, mSite);

        // retain the id of the highlighted and selected comments
        if (mComment != null && hasDetailFragment()) {
            outState.putSerializable(KEY_SELECTED_COMMENT, mComment);
        }

        if (hasListFragment()) {
            outState.putBoolean(KEY_AUTO_REFRESHED, getListFragment().mHasAutoRefreshedComments);
            outState.putString(KEY_EMPTY_VIEW_MESSAGE, getListFragment().getEmptyViewMessage());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = CommentDialogs.createCommentDialog(this, id);
        if (dialog != null)
            return dialog;
        return super.onCreateDialog(id);
    }

    @Override
    public void onModerateComment(final SiteModel site, final CommentModel comment,
                                  final CommentStatus newStatus) {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }

        if (newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED) {
            getListFragment().setCommentIsModerating(comment.getRemoteCommentId(), true);
            getListFragment().updateEmptyView();
            comment.setStatus(newStatus.toString());
            mDispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(comment));
            mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(new RemoteCommentPayload(mSite, comment)));
        } else if (newStatus == CommentStatus.SPAM || newStatus == CommentStatus.TRASH
                || newStatus == CommentStatus.DELETED) {
            mTrashedComments.add(comment);
            getListFragment().removeComment(comment);
            getListFragment().setCommentIsModerating(comment.getRemoteCommentId(), true);
            getListFragment().updateEmptyView();

            String message = (newStatus == CommentStatus.TRASH ? getString(R.string.comment_trashed) :
                    newStatus == CommentStatus.SPAM ? getString(R.string.comment_spammed) :
                            getString(R.string.comment_deleted_permanently)  );
            View.OnClickListener undoListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTrashedComments.remove(comment);
                    getListFragment().setCommentIsModerating(comment.getRemoteCommentId(), false);
                    getListFragment().loadComments();
                }
            };

            Snackbar snackbar = Snackbar.make(getListFragment().getView(), message, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo, undoListener);

            // do the actual moderation once the undo bar has been hidden
            snackbar.setCallback(new Snackbar.Callback() {
                @Override
                public void onDismissed(Snackbar snackbar, int event) {
                    super.onDismissed(snackbar, event);

                    // comment will no longer exist in moderating list if action was undone
                    if (!mTrashedComments.contains(comment)) {
                        return;
                    }
                    mTrashedComments.remove(comment);
                    moderateComment(comment, newStatus);
                }
            });

            snackbar.show();
        }
    }

    private void moderateComment(CommentModel comment, CommentStatus newStatus) {
        if (newStatus == CommentStatus.DELETED) {
            // For deletion, we need to dispatch a specific action.
            mDispatcher.dispatch(CommentActionBuilder.newDeleteCommentAction(new RemoteCommentPayload(mSite, comment)));
        } else {
            // Actual moderation (push the modified comment).
            comment.setStatus(newStatus.toString());
            mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(new RemoteCommentPayload(mSite, comment)));
        }
    }

    @Override
    public void onCommentChanged(CommentActions.ChangeType changeType) {
        switch (changeType) {
            case EDITED:
                reloadCommentList();
                break;
            case REPLIED:
                updateCommentList();
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
