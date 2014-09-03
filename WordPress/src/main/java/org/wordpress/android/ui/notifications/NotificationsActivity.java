package org.wordpress.android.ui.notifications;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.comments.CommentDialogs;
import org.wordpress.android.ui.notifications.utils.SimperiumUtils;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.DisplayUtils;

import javax.annotation.Nonnull;

public class NotificationsActivity extends WPActionBarActivity
        implements CommentActions.OnCommentChangeListener {
    public static final String NOTIFICATION_ACTION = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String FROM_NOTIFICATION_EXTRA = "fromNotification";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";

    private static final String KEY_INITIAL_UPDATE = "initialUpdate";
    private static final String TAG_LIST_VIEW = "notificationsList";

    private NotificationsListFragment mNotesListFragment;

    private String mSelectedNoteId;
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

            if (getIntent().hasExtra(NOTE_ID_EXTRA)) {
                launchWithNoteId();
            } else if (savedInstanceState.getString(NOTE_ID_EXTRA) != null) {
                // Restore last selected note
                openNoteForNoteId(savedInstanceState.getString(NOTE_ID_EXTRA));
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
        } else if (DisplayUtils.isLandscapeTablet(this) && mNotesListFragment != null) {
            mNotesListFragment.setShouldLoadFirstNote(true);
        }
    }

    private void openNoteForNoteId(String noteId) {
        Bucket<Note> notesBucket = SimperiumUtils.getNotesBucket();
        try {
            if (notesBucket != null) {
                Note note = notesBucket.get(noteId);
                if (note != null) {
                    openNote(note);
                    if (mNotesListFragment != null) {
                        mNotesListFragment.setSelectedNoteId(noteId);
                    }
                }
            }
        } catch (BucketObjectMissingException e) {
            AppLog.e(T.NOTIFS, "Could not load notification from bucket.");
        }
    }

    /*
     * triggered from the comment details fragment whenever a comment is changed (moderated, added,
     * deleted, etc.) - refresh notifications so changes are reflected here
     */
    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom, CommentActions.ChangeType changeType) {
        // remove the comment detail fragment if the comment was trashed
        if ((changeType == CommentActions.ChangeType.TRASHED || changeType == CommentActions.ChangeType.SPAMMED)
                && changedFrom == CommentActions.ChangedFrom.COMMENT_DETAIL) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            }
        }

        mNotesListFragment.refreshNotes();
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
            fragment = CommentDetailFragment.newInstance(note);
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
        Fragment fragment = getDetailFragmentForNote(note);

        if (DisplayUtils.isLandscapeTablet(this)) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.replace(R.id.notifications_detail_fragment_container, fragment);
            ft.commitAllowingStateLoss();
            return;
        }

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.replace(R.id.layout_fragment_container, fragment);
        mMenuDrawer.setDrawerIndicatorEnabled(false);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();

        // Update title
        if (note.getFormattedSubject() != null) {
            setTitle(note.getTitle());
        }
    }

    public void showBlogPreviewActivity(long siteId, String siteUrl) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderBlogPreview(this, siteId, siteUrl);
    }

    public void showPostActivity(long siteId, long postId, String title) {
        if (isFinishing()) return;

        ReaderActivityLauncher.showReaderPostDetail(this, siteId, postId);
    }

    public void showWebViewActivityForUrl(String url) {
        if (isFinishing()) return;

        Intent intent = new Intent(this, AuthenticatedWebViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
    }

    private class NoteClickListener implements NotificationsListFragment.OnNoteClickListener {
        @Override
        public void onClickNote(Note note, int position) {
            if (note == null) return;

            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            openNote(note);
        }
    }

    @Override
    public void onSaveInstanceState(@Nonnull Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        outState.putBoolean(KEY_INITIAL_UPDATE, mHasPerformedInitialUpdate);
        outState.putString(NOTE_ID_EXTRA, mSelectedNoteId);

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

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = CommentDialogs.createCommentDialog(this, id);
        if (dialog != null)
            return dialog;
        return super.onCreateDialog(id);
    }
}
