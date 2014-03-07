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
import android.widget.ListAdapter;
import android.widget.ListView;
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
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.comments.CommentDialogs;
import org.wordpress.android.ui.notifications.NotificationsListFragment.NotesAdapter;
import org.wordpress.android.ui.reader.ReaderPostDetailFragment;
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wordpress.android.WordPress.getRestClientUtils;

public class NotificationsActivity extends WPActionBarActivity
                                   implements CommentActions.OnCommentChangeListener,
                                              NotificationFragment.OnPostClickListener,
                                              NotificationFragment.OnCommentClickListener {

    public static final String NOTIFICATION_ACTION      = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA            = "noteId";
    public static final String FROM_NOTIFICATION_EXTRA  = "fromNotification";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";

    private static final String KEY_INITIAL_UPDATE = "initial_update";

    private NotificationsListFragment mNotesList;
    private MenuItem mRefreshMenuItem;
    private boolean mLoadingMore = false;
    private boolean mFirstLoadComplete = false;
    private List<Note> mNotes;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.notifications);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getResources().getString(R.string.notifications));

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        mNotesList = (NotificationsListFragment) fm.findFragmentById(R.id.fragment_notes_list);
        mNotesList.setNoteProvider(new NoteProvider());
        mNotesList.setOnNoteClickListener(new NoteClickListener());

        // Load notes
        mNotes = WordPress.wpDB.getLatestNotes();

        GCMIntentService.activeNotificationsMap.clear();

        if (savedInstanceState == null) {
            launchWithNoteId();
        } else {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
        }

        refreshNotificationsListFragment(mNotes);

        if (savedInstanceState != null)
            popNoteDetail();
        if (mBroadcastReceiver == null)
            createBroadcastReceiver();

        // remove window background since background color is set in fragment (reduces overdraw)
        getWindow().setBackgroundDrawable(null);
    }

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mNotes = WordPress.wpDB.getLatestNotes();
                refreshNotificationsListFragment(mNotes);
            }
        };
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        GCMIntentService.activeNotificationsMap.clear();

        launchWithNoteId();
    }

    private final FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };
    private boolean mHasPerformedInitialUpdate;

    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId(){
        final Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA)) {
            int noteID = Integer.valueOf(intent.getStringExtra(NOTE_ID_EXTRA));
            Note note = WordPress.wpDB.getNoteById(noteID);
            if (note != null) {
                openNote(note);
            } else {
                // find it/load it etc
                Map<String, String> params = new HashMap<String, String>();
                params.put("ids", intent.getStringExtra(NOTE_ID_EXTRA));
                NotesResponseHandler handler = new NotesResponseHandler(){
                    @Override
                    public void onNotes(List<Note> notes){
                        // there should only be one note!
                        if (!notes.isEmpty()) {
                            Note note = notes.get(0);
                            openNote(note);
                        }
                    }
                };
                getRestClientUtils().getNotifications(params, handler, handler);
            }
        } else {
            // on a tablet: open first note if none selected
            String fragmentTag = mNotesList.getTag();
            if (fragmentTag != null && fragmentTag.equals("tablet-view")) {
                if (mNotes != null && mNotes.size() > 0) {
                    Note note = mNotes.get(0);
                    if (note != null) {
                        openNote(note);
                    }
                }
            }
            refreshNotes();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshNotes();
                return true;
            case android.R.id.home:
                if (isLargeOrXLarge()) {
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
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        if (mShouldAnimateRefreshButton) {
            mShouldAnimateRefreshButton = false;
            startAnimatingRefreshButton(mRefreshMenuItem);
        }
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

    /**
     *  Open a note fragment based on the type of note
     */
    private void openNote(final Note note){
        if (note == null || isFinishing())
            return;

        // if note is "unread" set note to "read"
        if (note.isUnread()) {
            // send a request to mark note as read
            getRestClientUtils().markNoteAsRead(note, new RestRequest.Listener() {
                        @Override
                        public void onResponse(JSONObject response) {
                            if (isFinishing())
                                return;

                            final NotesAdapter notesAdapter = mNotesList.getNotesAdapter();

                            note.setUnreadCount("0");
                            if (notesAdapter.getPosition(note) < 0) {
                                // edge case when a note is opened with a note_id, and not tapping on the list. Loop over all notes
                                // in the adapter and find a match with the noteID
                                for (int i = 0; i < notesAdapter.getCount(); i++) {
                                    Note item = notesAdapter.getItem(i);
                                    if (item.getId().equals(note.getId())) {
                                        item.setUnreadCount("0");
                                        break;
                                    }
                                }
                            }

                            WordPress.wpDB.addNote(note, false); //Update the DB
                            notesAdapter.notifyDataSetChanged();
                        }
                    }, new RestRequest.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            AppLog.d(T.NOTIFS, String.format("Failed to mark as read %s", error));
                        }
                    }
            );
        }

        FragmentManager fm = getSupportFragmentManager();

        // remove the note detail if it's already on there
        if (fm.getBackStackEntryCount() > 0){
            fm.popBackStack();
        }

        // create detail fragment for this note type
        Fragment detailFragment = getDetailFragmentForNote(note);
        if (detailFragment == null) {
            AppLog.d(T.NOTIFS, String.format("No fragment found for %s", note.toJSONObject()));
            return;
        }

        // set arguments from activity if called from a notification
        /*Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA) && intent.getStringExtra(NOTE_ID_EXTRA).equals(note.getId())) {
            if (intent.hasExtra(NOTE_REPLY_EXTRA) || intent.hasExtra(NOTE_INSTANT_REPLY_EXTRA)) {
                detailFragment.setArguments(intent.getExtras());
            }
        }*/

        // set the note if this is a NotificationFragment (ReaderPostDetailFragment is the only
        // fragment used here that is not a NotificationFragment)
        if (detailFragment instanceof NotificationFragment)
            ((NotificationFragment) detailFragment).setNote(note);

        // swap the fragment
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.layout_fragment_container, detailFragment);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        // only add to backstack if we're removing the list view from the fragment container
        View container = findViewById(R.id.layout_fragment_container);
        if (container.findViewById(R.id.fragment_notes_list) != null) {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
            ft.addToBackStack(null);
            if (mNotesList != null)
                ft.hide(mNotesList);
        }

        ft.commitAllowingStateLoss();
    }

    /*
     * triggered from the comment details fragment whenever a comment is changed (moderated, added,
     * deleted, etc.) - refresh notifications so changes are reflected here
     */
    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom, CommentActions.ChangeType changeType) {
        refreshNotes();
    }

    private void refreshNotificationsListFragment(List<Note> notes) {
        AppLog.d(T.NOTIFS, "refreshing note list fragment");
        final NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
        adapter.addAll(notes, true);
        // mark last seen timestamp
        if (!notes.isEmpty()) {
            updateLastSeen(notes.get(0).getTimestamp());
        }
    }

    private void refreshNotes(){
        if (!NetworkUtils.checkConnection(this))
            return;

        mFirstLoadComplete = false;
        mShouldAnimateRefreshButton = true;
        startAnimatingRefreshButton(mRefreshMenuItem);
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
                                stopAnimatingRefreshButton(mRefreshMenuItem);
                            }
                        });
                    }
                }.start();
            }
            @Override
            public void onErrorResponse(VolleyError error){
                //We need to show an error message? and remove the loading indicator from the list?
                mFirstLoadComplete = true;
                final NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
                adapter.addAll(new ArrayList<Note>(), true);

                ToastUtils.showToastOrAuthAlert(NotificationsActivity.this, error, getString(R.string.error_refresh_notifications));

                stopAnimatingRefreshButton(mRefreshMenuItem);
                mShouldAnimateRefreshButton = false;
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
                NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
                adapter.addAll(notes, false);
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
        public void onRequestMoreNotifications(ListView notesList, ListAdapter notesAdapter){
            if (canRequestMore()) {
                NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
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
            openNote(note);
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
                mNotes = new ArrayList<Note>(0);
                onNotes(mNotes);
                return;
            }

            try {
                mNotes = NotificationUtils.parseNotes(response);
                onNotes(mNotes);
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
        outState.remove(NOTE_ID_EXTRA);
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

    /*
     * called from fragment when a link to a post is tapped - shows the post in a reader
     * detail fragment
     */
    @Override
    public void onPostClicked(Note note, int remoteBlogId, int postId) {
        ReaderPostDetailFragment readerFragment = ReaderPostDetailFragment.newInstance(remoteBlogId, postId);
        String tagForFragment = getString(R.string.fragment_tag_reader_post_detail);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.layout_fragment_container, readerFragment, tagForFragment)
          .addToBackStack(tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
          .commit();
    }

    /*
     * called from fragment when a link to a comment is tapped - shows the comment in the comment
     * detail fragment
     */
    @Override
    public void onCommentClicked(Note note, int remoteBlogId, long commentId) {
        CommentDetailFragment commentFragment = CommentDetailFragment.newInstance(note);
        String tagForFragment = getString(R.string.fragment_tag_comment_detail);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.layout_fragment_container, commentFragment, tagForFragment)
          .addToBackStack(tagForFragment)
          .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
          .commit();
    }

}
