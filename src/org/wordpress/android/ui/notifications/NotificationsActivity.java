package org.wordpress.android.ui.notifications;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
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
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.stats.AnalyticsTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wordpress.android.WordPress.getRestClientUtils;

public class NotificationsActivity extends WPActionBarActivity
                                   implements CommentActions.OnCommentChangeListener,
                                              NotificationFragment.OnPostClickListener,
                                              NotificationFragment.OnCommentClickListener {
    public static final String NOTIFICATION_ACTION = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA = "noteId";
    public static final String FROM_NOTIFICATION_EXTRA = "fromNotification";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    private static final String KEY_INITIAL_UPDATE = "initial_update";
    private static final String KEY_SELECTED_COMMENT_ID = "selected_comment_id";
    private static final String KEY_SELECTED_POST_ID = "selected_post_id";

    private static final int UNSPECIFIED_NOTE_ID = -1;

    private NotificationsListFragment mNotesList;
    private boolean mLoadingMore = false;
    private boolean mFirstLoadComplete = false;
    private BroadcastReceiver mBroadcastReceiver;
    private boolean mDualPane;
    private int mSelectedNoteId;
    private boolean mHasPerformedInitialUpdate;
    private BlogPairId mTmpSelectedComment;
    private BlogPairId mTmpSelectedReaderPost;
    private BlogPairId mSelectedComment;
    private BlogPairId mSelectedReaderPost;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);

        createMenuDrawer(R.layout.notifications);
        View fragmentContainer = findViewById(R.id.layout_fragment_container);
        mDualPane = fragmentContainer != null && getString(R.string.dual_pane_mode).equals(fragmentContainer.getTag());

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getResources().getString(R.string.notifications));

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mNotesList = (NotificationsListFragment) fm.findFragmentById(R.id.fragment_notes_list);
        mNotesList.setNoteProvider(new NoteProvider());
        mNotesList.setOnNoteClickListener(new NoteClickListener());

        restoreSavedInstance(savedInstanceState);
        GCMIntentService.activeNotificationsMap.clear();

        if (mBroadcastReceiver == null) {
            createBroadcastReceiver();
        }

        // remove window background since background color is set in fragment (reduces overdraw)
        getWindow().setBackgroundDrawable(null);
    }

    private void restoreSavedInstance(final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
            int noteId = savedInstanceState.getInt(NOTE_ID_EXTRA, UNSPECIFIED_NOTE_ID);

            LoadNotesCallback notesLoadedCallback = new LoadNotesCallback() {
                @Override
                public void notesLoaded() {
                    // restore the post detail fragment if one was selected
                    BlogPairId selectedPostId = (BlogPairId) savedInstanceState.get(KEY_SELECTED_POST_ID);
                    if (selectedPostId != null) {
                        onPostClicked(null, (int) selectedPostId.mRemoteBlogId, (int) selectedPostId.mId);
                    }

                    // restore the comment detail fragment if one was selected
                    BlogPairId selectedCommentId = (BlogPairId) savedInstanceState.get(KEY_SELECTED_COMMENT_ID);
                    if (selectedCommentId != null) {
                        onCommentClicked(null, (int) selectedCommentId.mRemoteBlogId, selectedCommentId.mId);
                    }
                }
            };

            if (!mDualPane && noteId != UNSPECIFIED_NOTE_ID) {
                // Not dual pane and a specified note, we want to open the note fragment and then load the list in
                // background (the list is still needed when the user tap back)
                Note note = WordPress.wpDB.getNoteById(noteId);
                openNote(note, false);
                loadNotes(false, noteId, notesLoadedCallback);
            } else {
                loadNotes(true, noteId, notesLoadedCallback);
            }
        } else {
            loadNotes(true, UNSPECIFIED_NOTE_ID, null);
        }
    }

    private void loadNotes(final boolean launchWithNoteId, final int noteId, final LoadNotesCallback callback) {
        new Thread() {
            @Override
            public void run() {
                final List<Note> notes = WordPress.wpDB.getLatestNotes();
                NotificationsActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshNotificationsListFragment(notes);
                        if (launchWithNoteId) {
                            launchWithNoteId(noteId);
                        } else {
                            if (noteId != UNSPECIFIED_NOTE_ID) {
                                Note note = WordPress.wpDB.getNoteById(noteId);
                                if (note != null) {
                                    mNotesList.setNoteSelected(note, true);
                                }
                            }
                        }

                        if (callback != null) {
                            callback.notesLoaded();
                        }
                    }
                });
            }
        }.start();
    }

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                loadNotes(true, UNSPECIFIED_NOTE_ID, null);
            }
        };
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        GCMIntentService.activeNotificationsMap.clear();
        launchWithNoteId(UNSPECIFIED_NOTE_ID);
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener =
            new FragmentManager.OnBackStackChangedListener() {
                public void onBackStackChanged() {
                    int backStackEntryCount = getSupportFragmentManager().getBackStackEntryCount();
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
    private void launchWithNoteId(int noteId) {
        final Intent intent = getIntent();
        if (noteId == UNSPECIFIED_NOTE_ID) {
            if (intent.hasExtra(NOTE_ID_EXTRA)) {
                noteId = Integer.valueOf(intent.getStringExtra(NOTE_ID_EXTRA));
            }
        }
        if (noteId != UNSPECIFIED_NOTE_ID) {
            Note note = WordPress.wpDB.getNoteById(noteId);
            if (note != null) {
                openNote(note, true);
            } else {
                // find it/load it etc
                Map<String, String> params = new HashMap<String, String>();
                params.put("ids", Integer.toString(noteId));
                NotesResponseHandler handler = new NotesResponseHandler() {
                    @Override
                    public void onNotes(List<Note> notes) {
                        // there should only be one note!
                        if (!notes.isEmpty()) {
                            openNote(notes.get(0), true);
                        }
                    }
                };
                getRestClientUtils().getNotifications(params, handler, handler);
            }
        } else {
            // Dual pane and no note specified then open first note
            if (mDualPane && mNotesList.hasAdapter() && !mNotesList.getNotesAdapter().isEmpty()) {
                openNote(mNotesList.getNotesAdapter().getItem(0), false);
            }
            mNotesList.animateRefresh(true);
            refreshNotes();
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
                    FragmentManager fm = getSupportFragmentManager();
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
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.notifications, menu);
        return true;
    }

    void popNoteDetail(){
        FragmentManager fm = getSupportFragmentManager();
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

    /*
     * mark a single notification as read, both on the server and locally
     */
    private void markNoteAsRead(final Note note) {
        if (note == null)
            return;
        getRestClientUtils().markNoteAsRead(note,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // clear the unread count then save to local db
                        note.setUnreadCount("0");
                        WordPress.wpDB.addNote(note, note.isPlaceholder());
                        // reflect the change in the note list
                        if (!isFinishing() && mNotesList != null)
                            mNotesList.updateNote(note);
                    }
                },
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(T.NOTIFS, String.format("Failed to mark as read %s", error));
                    }
                }
        );
    }

    /**
     *  Open a note fragment based on the type of note
     */
    private void openNote(final Note note, boolean scrollToNote) {
        if (note == null || isFinishing()) {
            return;
        }
        mSelectedNoteId = StringUtils.stringToInt(note.getId());
        mNotesList.setNoteSelected(note, scrollToNote);

        // mark the note as read if it's unread
        if (note.isUnread()) {
            markNoteAsRead(note);
        }
        FragmentManager fm = getSupportFragmentManager();

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
        ft.replace(R.id.layout_fragment_container, detailFragment).setTransition(
                FragmentTransaction.TRANSIT_FRAGMENT_FADE);

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

    /*
     * triggered from the comment details fragment whenever a comment is changed (moderated, added,
     * deleted, etc.) - refresh notifications so changes are reflected here
     */
    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom, CommentActions.ChangeType changeType) {
        // remove the comment detail fragment if the comment was trashed
        if (changeType == CommentActions.ChangeType.TRASHED && changedFrom == CommentActions.ChangedFrom.COMMENT_DETAIL) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
            }
        }

        mNotesList.animateRefresh(true);
        refreshNotes();
    }

    private void refreshNotificationsListFragment(List<Note> notes) {
        AppLog.d(T.NOTIFS, "refreshing note list fragment");
        mNotesList.getNotesAdapter().addAll(notes, true);
        // mark last seen timestamp
        if (!notes.isEmpty()) {
            updateLastSeen(notes.get(0).getTimestamp());
        }
    }

    public void refreshNotes() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            mNotesList.animateRefresh(false);
            return;
        }

        mFirstLoadComplete = false;
        NotesResponseHandler notesHandler = new NotesResponseHandler(){
            @Override
            public void onNotes(final List<Note> notes) {
                mFirstLoadComplete = true;
                mNotesList.setAllNotesLoaded(false);
                // nbradbury - saving notes can be slow, so do it in the background
                new Thread() {
                    @Override
                    public void run() {
                        WordPress.wpDB.saveNotes(notes, true);
                        NotificationsActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshNotificationsListFragment(notes);
                                mNotesList.animateRefresh(false);
                            }
                        });
                    }
                }.start();
            }
            @Override
            public void onErrorResponse(VolleyError error){
                //We need to show an error message? and remove the loading indicator from the list?
                mFirstLoadComplete = true;
                mNotesList.getNotesAdapter().addAll(new ArrayList<Note>(), true);
                ToastUtils.showToastOrAuthAlert(NotificationsActivity.this, error, getString(R.string.error_refresh_notifications));
                mNotesList.animateRefresh(false);
            }
        };
        NotificationUtils.refreshNotifications(notesHandler, notesHandler);
    }

    private void updateLastSeen(String timestamp) {
        getRestClientUtils().markNotificationsSeen(timestamp, new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        AppLog.d(T.NOTIFS, String.format("Set last seen time %s", response));
                    }
                }, new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AppLog.d(T.NOTIFS, String.format("Could not set last seen time %s", error));
                    }
                }
        );
    }

    private void requestNotesBefore(final Note note){
        Map<String, String> params = new HashMap<String, String>();
        AppLog.d(T.NOTIFS, String.format("Requesting more notes before %s", note.queryJSON("timestamp", "")));
        params.put("before", note.queryJSON("timestamp", ""));
        NotesResponseHandler notesHandler = new NotesResponseHandler(){
            @Override
            public void onNotes(List<Note> notes){
                // API returns 'on or before' timestamp, so remove first item
                if (notes.size() >= 1)
                    notes.remove(0);
                mNotesList.setAllNotesLoaded(notes.size() == 0);
                mNotesList.getNotesAdapter().addAll(notes, false);
            }
        };
        getRestClientUtils().getNotifications(params, notesHandler, notesHandler);
    }

    private class NoteProvider implements NotificationsListFragment.NoteProvider {
        @Override
        public boolean canRequestMore() {
            return mFirstLoadComplete && !mLoadingMore;
        }

        @Override
        public void onRequestMoreNotifications(){
            if (canRequestMore()) {
                NotesAdapter adapter = mNotesList.getNotesAdapter();
                if (adapter.getCount() > 0) {
                    Note lastNote = adapter.getItem(adapter.getCount()-1);
                    requestNotesBefore(lastNote);
                }
            }
        }
    }

    private class NoteClickListener implements NotificationsListFragment.OnNoteClickListener {
        @Override
        public void onClickNote(Note note){
            if (note == null)
                return;
            // open the latest version of this note just in case it has changed - this can
            // happen if the note was tapped from the list fragment after it was updated
            // by another fragment (such as NotificationCommentLikeFragment)
            Note updatedNote = WordPress.wpDB.getNoteById(StringUtils.stringToInt(note.getId()));
            openNote(updatedNote != null ? updatedNote : note, false);
        }
    }

    abstract class NotesResponseHandler implements RestRequest.Listener, RestRequest.ErrorListener {
        NotesResponseHandler(){
            mLoadingMore = true;
        }
        abstract void onNotes(List<Note> notes);

        @Override
        public void onResponse(JSONObject response){
            mLoadingMore = false;

            if( response == null ) {
                //Not sure this could ever happen, but make sure we're catching all response types
                AppLog.w(T.NOTIFS, "Success, but did not receive any notes");
                onNotes(new ArrayList<Note>(0));
                return;
            }

            try {
                List<Note> notes = NotificationUtils.parseNotes(response);
                onNotes(notes);
            } catch (JSONException e) {
                AppLog.e(T.NOTIFS, "Success, but can't parse the response", e);
                showError(getString(R.string.error_parsing_response));
            }
        }

        @Override
        public void onErrorResponse(VolleyError error){
            mLoadingMore = false;
            showError();
            AppLog.d(T.NOTIFS, String.format("Error retrieving notes: %s", error));
        }

        public void showError(final String errorMessage){
            Toast.makeText(NotificationsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }

        public void showError(){
            showError(getString(R.string.error_generic));
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
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(mBroadcastReceiver, new IntentFilter(NOTIFICATION_ACTION));
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
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.layout_fragment_container, commentFragment, tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
          .addToBackStack(tagForFragment)
          .commit();
    }

    private interface LoadNotesCallback {
        void notesLoaded();
    }
}
