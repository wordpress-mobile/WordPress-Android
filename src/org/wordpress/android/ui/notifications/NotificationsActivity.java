package org.wordpress.android.ui.notifications;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.simperium.client.Bucket;
import com.simperium.client.BucketObjectMissingException;
import com.simperium.client.User;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.models.BlogPairId;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.comments.CommentDialogs;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SimperiumUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

public class NotificationsActivity extends WPActionBarActivity
        implements CommentActions.OnCommentChangeListener, NotificationFragment.OnPostClickListener,
        NotificationFragment.OnCommentClickListener {
    public static final String NOTIFICATION_ACTION = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String FROM_NOTIFICATION_EXTRA = "fromNotification";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    private static final String KEY_INITIAL_UPDATE = "initial_update";
    private static final String KEY_SELECTED_COMMENT_ID = "selected_comment_id";
    private static final String KEY_SELECTED_POST_ID = "selected_post_id";

    private NotificationsListFragment mNotesList;
    private boolean mDualPane;
    private int mSelectedNoteId;
    private boolean mHasPerformedInitialUpdate;
    private BlogPairId mTmpSelectedComment;
    private BlogPairId mTmpSelectedReaderPost;
    private BlogPairId mSelectedComment;
    private BlogPairId mSelectedReaderPost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // savedInstanceState will be non-null if activity is being re-created
        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
        }

        createMenuDrawer(R.layout.notifications);
        View fragmentContainer = findViewById(R.id.layout_fragment_container);
        mDualPane = fragmentContainer != null && getString(R.string.dual_pane_mode).equals(fragmentContainer.getTag());

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getResources().getString(R.string.notifications));

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mNotesList = (NotificationsListFragment) fm.findFragmentById(R.id.fragment_notes_list);
        mNotesList.setOnNoteClickListener(new NoteClickListener());

        GCMIntentService.activeNotificationsMap.clear();

        if (savedInstanceState != null) {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
            popNoteDetail();
        } else {
            launchWithNoteId();
        }

        // remove window background since background color is set in fragment (reduces overdraw)
        getWindow().setBackgroundDrawable(null);

        // Show an auth alert if we don't have an authorized Simperium user
        if (SimperiumUtils.getSimperium() != null) {
            User user = SimperiumUtils.getSimperium().getUser();
            if (user != null && user.getStatus() == User.Status.NOT_AUTHORIZED) {
                ToastUtils.showAuthErrorDialog(this, R.string.sign_in_again, R.string.simperium_connection_error);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        GCMIntentService.activeNotificationsMap.clear();
        launchWithNoteId();
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                public void onBackStackChanged() {
                    int backStackEntryCount = getFragmentManager().getBackStackEntryCount();
                    // This is ugly, but onBackStackChanged is not called just after a fragment commit.
                    // In a 2 commits in a row case, onBackStackChanged is called twice but after the
                    // 2 commits. That's why mSelectedPostId can't be affected correctly after the first commit.
                    switch (backStackEntryCount) {
                        case 2:
                            mSelectedReaderPost = mTmpSelectedReaderPost;
                            mSelectedComment = mTmpSelectedComment;
                            mTmpSelectedReaderPost = null;
                            mTmpSelectedComment = null;
                            break;
                        case 1:
                            if (mDualPane) {
                                mSelectedReaderPost = mTmpSelectedReaderPost;
                                mSelectedComment = mTmpSelectedComment;
                            } else {
                                mSelectedReaderPost = null;
                                mSelectedComment = null;
                            }
                            break;
                        case 0:
                            mMenuDrawer.setDrawerIndicatorEnabled(true);
                            mSelectedReaderPost = null;
                            mSelectedComment = null;
                            break;
                    }
                }
            };

    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId() {
        Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA)) {
            String noteID = intent.getStringExtra(NOTE_ID_EXTRA);

            Bucket<Note> notesBucket = SimperiumUtils.getNotesBucket();
            try {
                if (notesBucket != null) {
                    Note note = notesBucket.get(noteID);
                    if (note != null) {
                        openNote(note);
                    }
                }
            } catch (BucketObjectMissingException e) {
                AppLog.e(T.NOTIFS, "Could not load notification from bucket.");
            }
        } else {
            // Dual pane and no note specified then select the first note
            if (mDualPane && mNotesList != null) {
                mNotesList.setShouldLoadFirstNote(true);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDualPane) {
                    // let WPActionBarActivity handle it (toggles menu drawer)
                    return super.onOptionsItemSelected(item);
                } else {
                    FragmentManager fm = getFragmentManager();
                    if (fm.getBackStackEntryCount() > 0) {
                        popNoteDetail();
                        return true;
                    } else {
                        return super.onOptionsItemSelected(item);
                    }
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

    /*
     * triggered from the comment details fragment whenever a comment is changed (moderated, added,
     * deleted, etc.) - refresh notifications so changes are reflected here
     */
    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom, CommentActions.ChangeType changeType) {
        // remove the comment detail fragment if the comment was trashed
        if (changeType == CommentActions.ChangeType.TRASHED && changedFrom == CommentActions.ChangedFrom.COMMENT_DETAIL) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            }
        }

        mNotesList.refreshNotes();
    }

    void popNoteDetail(){
        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fragment_comment_detail);
        if (f == null) {
            fm.popBackStack();
        }
    }

    /**
     * Tries to pick the correct fragment detail type for a given note
     */
    private Fragment getDetailFragmentForNote(Note note){
        if (note == null)
            return null;

        if (note.isCommentType()) {
            // show comment detail for comment notifications
            return CommentDetailFragment.newInstance(note);
        } else if (note.isCommentLikeType()) {
            return new NoteCommentLikeFragment();
        } else if (note.isAutomattcherType()) {
            // show reader post detail for automattchers about posts - note that comment
            // automattchers are handled by note.isCommentType() above
            boolean isPost = (note.getBlogId() !=0 && note.getPostId() != 0 && note.getCommentId() == 0);
            if (isPost) {
                return ReaderPostDetailFragment.newInstance(note.getBlogId(), note.getPostId());
            } else {
                // right now we'll never get here
                return new NoteMatcherFragment();
            }
        } else if (note.isSingleLineListTemplate()) {
            return new NoteSingleLineListFragment();
        } else if (note.isBigBadgeTemplate()) {
            return new BigBadgeFragment();
        }

        return null;
    }

    /**
     *  Open a note fragment based on the type of note
     */
    private void openNote(final Note note) {
        if (note == null || isFinishing() || isActivityDestroyed()) {
            return;
        }

        mSelectedNoteId = StringUtils.stringToInt(note.getId());

        // mark the note as read if it's unread
        if (note.isUnread()) {
            // mark as read which syncs with simperium
            note.markAsRead();
        }
        FragmentManager fm = getFragmentManager();

        // remove the note detail if it's already on there
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        }

        // create detail fragment for this note type
        Fragment detailFragment = getDetailFragmentForNote(note);
        if (detailFragment == null) {
            AppLog.d(T.NOTIFS, String.format("No fragment found for %s", note.toJSONObject()));
            return;
        }

        // set the note if this is a NotificationFragment (ReaderPostDetailFragment is the only
        // fragment used here that is not a NotificationFragment)
        if (detailFragment instanceof NotificationFragment) {
            ((NotificationFragment) detailFragment).setNote(note);
        }

        // swap the fragment
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.layout_fragment_container, detailFragment).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_OPENED_NOTIFICATION_DETAILS);
        // only add to backstack if we're removing the list view from the fragment container
        View container = findViewById(R.id.layout_fragment_container);
        if (container.findViewById(R.id.fragment_notes_list) != null) {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
            ft.addToBackStack(null);
            if (mNotesList != null) {
                ft.hide(mNotesList);
            }
        }
        ft.commitAllowingStateLoss();
    }

    private class NoteClickListener implements NotificationsListFragment.OnNoteClickListener {
        @Override
        public void onClickNote(Note note){
            if (note == null)
                return;
            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            //Note updatedNote = WordPress.wpDB.getNoteById(StringUtils.stringToInt(note.getId()));
            openNote(note);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (outState.isEmpty()) {
            outState.putBoolean("bug_19917_fix", true);
        }
        outState.putBoolean(KEY_INITIAL_UPDATE, mHasPerformedInitialUpdate);
        outState.putInt(NOTE_ID_EXTRA, mSelectedNoteId);
        if (mSelectedReaderPost != null) {
            outState.putSerializable(KEY_SELECTED_POST_ID, mSelectedReaderPost);
        }
        if (mSelectedComment != null) {
            outState.putSerializable(KEY_SELECTED_COMMENT_ID, mSelectedComment);
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

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = CommentDialogs.createCommentDialog(this, id);
        if (dialog != null)
            return dialog;
        return super.onCreateDialog(id);
    }

    /**
     * called from fragment when a link to a post is tapped - shows the post in a reader
     * detail fragment
     */
    @Override
    public void onPostClicked(Note note, int remoteBlogId, int postId) {
        mTmpSelectedReaderPost = new BlogPairId(remoteBlogId, postId);
        ReaderPostDetailFragment readerFragment = ReaderPostDetailFragment.newInstance(remoteBlogId, postId);
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.layout_fragment_container, readerFragment, tagForFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(tagForFragment)
                .commit();
    }

    /**
     * called from fragment when a link to a comment is tapped - shows the comment in the comment
     * detail fragment
     */
    @Override
    public void onCommentClicked(Note note, int remoteBlogId, long commentId) {
        mTmpSelectedComment = new BlogPairId(remoteBlogId, commentId);
        CommentDetailFragment commentFragment = CommentDetailFragment.newInstance(note);
        String tagForFragment = getString(R.string.fragment_tag_comment_detail);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.layout_fragment_container, commentFragment, tagForFragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(tagForFragment)
                .commit();
    }
}
