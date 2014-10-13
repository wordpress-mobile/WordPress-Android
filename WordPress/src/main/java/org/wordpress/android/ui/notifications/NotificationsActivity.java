package org.wordpress.android.ui.notifications;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import com.cocosw.undobar.UndoBarController;
import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.CommentStatus;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailActivity;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.ToastUtils;

import javax.annotation.Nonnull;

public class NotificationsActivity extends WPActionBarActivity implements CommentActions.OnNoteCommentActionListener,
        CommentActions.OnCommentChangeListener {
    public static final String NOTIFICATION_ACTION = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String FROM_NOTIFICATION_EXTRA = "fromNotification";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";

    private static final String KEY_INITIAL_UPDATE = "initialUpdate";
    private static final String KEY_REPLY_TEXT = "replyText";
    private static final String KEY_LIST_POSITION = "listPosition";

    private static final String TAG_LIST_VIEW = "notificationsList";
    private static final String TAG_TABLET_DETAIL_VIEW = "notificationsTabletDetail";

    private NotificationsListFragment mNotesListFragment;
    private Fragment mDetailFragment;

    private String mSelectedNoteId;
    private String mRestoredReplyText;
    private boolean mHasPerformedInitialUpdate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        createMenuDrawer(R.layout.notifications_activity);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
        }

        FragmentManager fragmentManager = getFragmentManager();

        if (mNotesListFragment == null) {
            mNotesListFragment = (NotificationsListFragment)fragmentManager.findFragmentByTag(TAG_LIST_VIEW);
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
        }
        setTitle(getResources().getString(R.string.notifications));

        fragmentManager.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mNotesListFragment.setOnNoteClickListener(new NoteClickListener());

        GCMIntentService.clearNotificationsMap();

        if (savedInstanceState != null) {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);

            if (savedInstanceState.getString(KEY_REPLY_TEXT) != null){
                mRestoredReplyText = savedInstanceState.getString(KEY_REPLY_TEXT);
            }

            if (savedInstanceState.getString(NOTE_ID_EXTRA) != null) {
                // Restore last selected note
                openNoteForNoteId(savedInstanceState.getString(NOTE_ID_EXTRA));
            }

            if (savedInstanceState.containsKey(KEY_LIST_POSITION)) {
                mNotesListFragment.setRestoredListPosition(
                        savedInstanceState.getInt(KEY_LIST_POSITION, ListView.INVALID_POSITION)
                );
            }
        } else {
            launchWithNoteId();
        }

        // remove window background since background color is set in fragment (reduces overdraw)
        getWindow().setBackgroundDrawable(null);

        // Show an auth alert if we don't have an authorized Simperium user
        if (SimperiumUtils.isUserNotAuthorized()) {
            AuthenticationDialogUtils.showAuthErrorDialog(this, R.string.sign_in_again,
                    R.string.simperium_connection_error);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        GCMIntentService.clearNotificationsMap();
        launchWithNoteId();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Remove notification if it is showing when we resume this activity.
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(GCMIntentService.PUSH_NOTIFICATION_ID);

        if (SimperiumUtils.isUserAuthorized()) {
            SimperiumUtils.startBuckets();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                FragmentManager fm = getFragmentManager();
                if (fm.getBackStackEntryCount() > 0) {
                    mRestoredReplyText = getCommentReplyText();
                    popNoteDetail();
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.notifications, menu);
        return true;
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                public void onBackStackChanged() {
                    int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
                    if (backStackEntryCount == 0) {
                        mMenuDrawer.setDrawerIndicatorEnabled(true);
                        setTitle(R.string.notifications);
                    } else {
                        mMenuDrawer.setDrawerIndicatorEnabled(false);
                    }
                }
            };

    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId() {
        Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA)) {
            openNoteForNoteId(intent.getStringExtra(NOTE_ID_EXTRA));
        }
    }

    private void openNoteForNoteId(String noteId) {
        Bucket<Note> notesBucket = SimperiumUtils.getNotesBucket();
        try {
            if (notesBucket != null) {
                Note note = notesBucket.get(noteId);
                if (note != null) {
                    openNote(note);
                }
            }
        } catch (BucketObjectMissingException e) {
            AppLog.e(T.NOTIFS, "Could not load notification from bucket.");
        }
    }

    void popNoteDetail() {
        FragmentManager fm = getFragmentManager();
        fm.popBackStack();
    }

    /**
     * Tries to pick the correct fragment detail type for a given note
     * Defaults to NotificationDetailListFragment
     */
    private Fragment getDetailFragmentForNote(Note note) {
        if (note == null)
            return null;

        Fragment fragment;
        if (note.isCommentType()) {
            // show comment detail for comment notifications
            CommentDetailFragment commentDetailFragment = CommentDetailFragment.newInstance(note);
            if (!TextUtils.isEmpty(mRestoredReplyText)) {
                commentDetailFragment.setRestoredReplyText(mRestoredReplyText);
            }

            fragment = commentDetailFragment;
        } else if (note.isAutomattcherType()) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            boolean isPost = (note.getSiteId() != 0 && note.getPostId() != 0 && note.getCommentId() == 0);
            if (isPost) {
                fragment = ReaderPostDetailFragment.newInstance(note.getSiteId(), note.getPostId());
            } else {
                fragment = NotificationsDetailListFragment.newInstance(note);
            }
        } else {
            fragment = NotificationsDetailListFragment.newInstance(note);
        }

        return fragment;
    }

    /**
     * Open a note fragment based on the type of note
     */
    private void openNote(final Note note) {
        if (note == null || isFinishing() || isActivityDestroyed()) {
            return;
        }

        mSelectedNoteId = note.getId();

        // mark the note as read if it's unread
        if (note.isUnread()) {
            // mark as read which syncs with simperium
            note.markAsRead();
        }

        // create detail fragment for this note type
        mDetailFragment = getDetailFragmentForNote(note);

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.hide(mNotesListFragment);
        ft.add(R.id.layout_fragment_container, mDetailFragment);
        mMenuDrawer.setDrawerIndicatorEnabled(false);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();

        // Update title
        if (note.getFormattedSubject() != null) {
            setTitle(note.getTitle());
        }
    }

    public void showCommentDetailForNote(Note note) {
        if (isFinishing() || note == null) return;

        Intent intent = new Intent(this, CommentDetailActivity.class);
        intent.putExtra(CommentDetailActivity.KEY_COMMENT_DETAIL_IS_REMOTE, true);
        intent.putExtra(CommentDetailActivity.KEY_COMMENT_DETAIL_NOTE_ID, note.getId());
        startActivity(intent);
    }

    public void showBlogPreviewActivity(long siteId, String siteUrl) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderBlogPreview(this, siteId, siteUrl);
    }

    public void showPostActivity(long siteId, long postId) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderPostDetail(this, siteId, postId);
    }

    public void showStatsActivityForSite(int localTableSiteId) {
        if (isFinishing()) return;

        Intent intent = new Intent(this, StatsActivity.class);
        intent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, localTableSiteId);
        intent.putExtra(StatsActivity.ARG_NO_MENU_DRAWER, true);
        startActivity(intent);
    }

    public void showWebViewActivityForUrl(String url) {
        if (isFinishing() || url == null)
            return;
        WPWebViewActivity.openUrlByUsingMainWPCOMCredentials(this, url);
    }

    @Override
    public void onModerateCommentForNote(final Note note, final CommentStatus newStatus) {
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }

        if (newStatus == CommentStatus.APPROVED || newStatus == CommentStatus.UNAPPROVED) {
            mNotesListFragment.setNoteIsModerating(note.getId(), true);
            CommentActions.moderateCommentForNote(note, newStatus,
                    new CommentActions.CommentActionListener() {
                        @Override
                        public void onActionResult(boolean succeeded) {
                            if (isFinishing()) return;

                            mNotesListFragment.setNoteIsModerating(note.getId(), false);

                            if (!succeeded) {
                                ToastUtils.showToast(NotificationsActivity.this,
                                        R.string.error_moderate_comment,
                                        ToastUtils.Duration.LONG
                                );
                            }
                        }
                    });
        } else if (newStatus == CommentStatus.TRASH || newStatus == CommentStatus.SPAM) {
            mNotesListFragment.setNoteIsHidden(note.getId(), true);
            // Show undo bar for trash or spam actions
            new UndoBarController.UndoBar(this)
                    .message(newStatus == CommentStatus.TRASH ? R.string.comment_trashed : R.string.comment_spammed)
                    .listener(new UndoBarController.AdvancedUndoListener() {
                        @Override
                        public void onHide(Parcelable parcelable) {
                            if (isFinishing()) return;
                            // Deleted notifications in Simperium never come back, so we won't
                            // make the request until the undo bar fades away
                            CommentActions.moderateCommentForNote(note, newStatus,
                                    new CommentActions.CommentActionListener() {
                                        @Override
                                        public void onActionResult(boolean succeeded) {
                                            if (isFinishing()) return;

                                            if (!succeeded) {
                                                mNotesListFragment.setNoteIsHidden(note.getId(), false);
                                                ToastUtils.showToast(NotificationsActivity.this,
                                                        R.string.error_moderate_comment,
                                                        ToastUtils.Duration.LONG
                                                );
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void onClear() {
                            //noop
                        }

                        @Override
                        public void onUndo(Parcelable parcelable) {
                            mNotesListFragment.setNoteIsHidden(note.getId(), false);
                        }
                    }).show();
        }
    }

    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom, CommentActions.ChangeType changeType) {
        // pop back stack if we edited a comment notification, so simperium will show the change
        if (changedFrom == CommentActions.ChangedFrom.COMMENT_DETAIL
                && changeType == CommentActions.ChangeType.EDITED) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            }
        }
    }

    private class NoteClickListener implements NotificationsListFragment.OnNoteClickListener {
        @Override
        public void onClickNote(Note note) {
            if (note == null) return;

            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            openNote(note);
        }
    }

    // Retrieves comment reply text so we can restore it upon returning to CommentDetailFragment
    private String getCommentReplyText() {
        if (mDetailFragment != null && mDetailFragment instanceof CommentDetailFragment) {
            CommentDetailFragment commentDetailFragment = (CommentDetailFragment)mDetailFragment;
            if (!TextUtils.isEmpty(commentDetailFragment.getReplyText())) {
                return commentDetailFragment.getReplyText();
            }
        }

        return null;
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        outState.putBoolean(KEY_INITIAL_UPDATE, mHasPerformedInitialUpdate);
        if (getFragmentManager().getBackStackEntryCount() > 0
                || getFragmentManager().findFragmentByTag(TAG_TABLET_DETAIL_VIEW) != null) {
            outState.putString(NOTE_ID_EXTRA, mSelectedNoteId);
        }

        // Save text in comment reply EditText
        if (!TextUtils.isEmpty(getCommentReplyText())) {
            outState.putString(KEY_REPLY_TEXT, getCommentReplyText());
        }

        // Save list view scroll position
        if (mNotesListFragment != null) {
            outState.putInt(KEY_LIST_POSITION, mNotesListFragment.getScrollPosition());
        }

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mHasPerformedInitialUpdate) {
            mHasPerformedInitialUpdate = true;
            ReaderAuthActions.updateCookies(this);
        }
    }
}
