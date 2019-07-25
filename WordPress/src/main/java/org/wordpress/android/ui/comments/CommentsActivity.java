package org.wordpress.android.ui.comments;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
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
import org.wordpress.android.ui.posts.BasicFragmentDialog;
import org.wordpress.android.ui.posts.BasicFragmentDialog.BasicDialogPositiveClickInterface;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPSnackbar;

import javax.inject.Inject;

public class CommentsActivity extends AppCompatActivity
        implements OnCommentSelectedListener,
        NotificationFragment.OnPostClickListener,
        BasicFragmentDialog.BasicDialogPositiveClickInterface {
    static final String KEY_AUTO_REFRESHED = "has_auto_refreshed";
    static final String KEY_EMPTY_VIEW_MESSAGE = "empty_view_message";
    private static final String SAVED_COMMENTS_STATUS_TYPE = "saved_comments_status_type";
    public static final String COMMENT_MODERATE_ID_EXTRA = "commentModerateId";
    public static final String COMMENT_MODERATE_STATUS_EXTRA = "commentModerateStatus";
    private final CommentList mTrashedComments = new CommentList();

    private CommentStatus mCurrentCommentStatusType = CommentStatus.ALL;

    private SiteModel mSite;

    @Inject Dispatcher mDispatcher;
    @Inject CommentStore mCommentStore;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        setContentView(R.layout.comment_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
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
            getSupportFragmentManager().beginTransaction()
                                .add(R.id.layout_fragment_container, commentsListFragment,
                                     getString(R.string.fragment_tag_comment_list))
                                .commitAllowingStateLoss();
        } else {
            getIntent().putExtra(KEY_AUTO_REFRESHED, savedInstanceState.getBoolean(KEY_AUTO_REFRESHED));
            getIntent().putExtra(KEY_EMPTY_VIEW_MESSAGE, savedInstanceState.getString(KEY_EMPTY_VIEW_MESSAGE));
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
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.d(AppLog.T.COMMENTS, "comment activity new intent");
    }

    private CommentsListFragment getListFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(getString(
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
        FragmentManager fm = getSupportFragmentManager();
        fm.executePendingTransactions();

        Fragment fragment = ReaderPostDetailFragment.newInstance(remoteBlogId, postId);
        FragmentTransaction ft = fm.beginTransaction();
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        ft.add(R.id.layout_fragment_container, fragment, tagForFragment)
          .addToBackStack(tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    /*
     * called from comment list when user taps a comment
     */
    @Override
    public void onCommentSelected(long commentId, CommentStatus statusFilter) {
        Intent detailIntent = new Intent(this, CommentsDetailActivity.class);
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_ID_EXTRA, commentId);
        detailIntent.putExtra(CommentsDetailActivity.COMMENT_STATUS_FILTER_EXTRA, statusFilter);
        detailIntent.putExtra(WordPress.SITE, mSite);
        startActivityForResult(detailIntent, 1);
    }

    /*
     * called from comment detail when user taps a link to a post - show the post in a
     * reader detail fragment
     */
    @Override
    public void onPostClicked(Note note, long remoteBlogId, int postId) {
        showReaderFragment(remoteBlogId, postId);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(WordPress.SITE, mSite);

        // retain the id of the highlighted comments
        if (hasListFragment()) {
            outState.putBoolean(KEY_AUTO_REFRESHED, getListFragment().mHasAutoRefreshedComments);
            outState.putString(KEY_EMPTY_VIEW_MESSAGE, getListFragment().getEmptyViewMessage());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = CommentDialogs.createCommentDialog(this, id);
        if (dialog != null) {
            return dialog;
        }
        return super.onCreateDialog(id);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            long commentId = data.getLongExtra(COMMENT_MODERATE_ID_EXTRA, -1);
            String newStatus = data.getStringExtra(COMMENT_MODERATE_STATUS_EXTRA);
            if (commentId >= 0 && !TextUtils.isEmpty(newStatus)) {
                onModerateComment(mCommentStore.getCommentBySiteAndRemoteId(mSite, commentId),
                                  CommentStatus.fromString(newStatus));
            }
        }
    }

    public void onModerateComment(final CommentModel comment,
                                  final CommentStatus newStatus) {
        if (newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED) {
            getListFragment().updateEmptyView();
            comment.setStatus(newStatus.toString());
            mDispatcher.dispatch(CommentActionBuilder.newUpdateCommentAction(comment));
            mDispatcher.dispatch(CommentActionBuilder.newPushCommentAction(new RemoteCommentPayload(mSite, comment)));
        } else if (newStatus == CommentStatus.SPAM || newStatus == CommentStatus.TRASH
                   || newStatus == CommentStatus.DELETED) {
            mTrashedComments.add(comment);
            getListFragment().removeComment(comment);

            String message = (newStatus == CommentStatus.TRASH ? getString(R.string.comment_trashed)
                    : newStatus == CommentStatus.SPAM ? getString(R.string.comment_spammed)
                            : getString(R.string.comment_deleted_permanently));
            View.OnClickListener undoListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mTrashedComments.remove(comment);
                    getListFragment().loadComments();
                }
            };

            WPSnackbar snackbar = WPSnackbar.make(getListFragment().getView(), message, Snackbar.LENGTH_LONG)
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPositiveClicked(@NotNull String instanceTag) {
        Fragment fragmentById = getSupportFragmentManager().findFragmentById(R.id.layout_fragment_container);
        if (fragmentById instanceof BasicFragmentDialog.BasicDialogPositiveClickInterface) {
            ((BasicDialogPositiveClickInterface) fragmentById).onPositiveClicked(instanceTag);
        }
    }
}
