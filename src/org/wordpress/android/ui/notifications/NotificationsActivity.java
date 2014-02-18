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
import android.support.v4.content.IntentCompat;
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
import org.wordpress.android.ui.reader.actions.ReaderAuthActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wordpress.android.WordPress.getContext;
import static org.wordpress.android.WordPress.restClient;

public class NotificationsActivity extends WPActionBarActivity implements CommentActions.OnCommentChangeListener {
    public static final String NOTIFICATION_ACTION = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA="noteId";
    public static final String FROM_NOTIFICATION_EXTRA="fromNotification";
    public static final String NOTE_REPLY_EXTRA="replyContent";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final int FLAG_FROM_NOTE=Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                            IntentCompat.FLAG_ACTIVITY_CLEAR_TASK;
    private static final String KEY_INITIAL_UPDATE = "initial_update";

    private final Set<FragmentDetector> fragmentDetectors = new HashSet<FragmentDetector>();

    private NotificationsListFragment mNotesList;
    private MenuItem mRefreshMenuItem;
    private boolean mLoadingMore = false;
    private boolean mFirstLoadComplete = false;
    private List<Note> notes;
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
        mNotesList = (NotificationsListFragment) fm.findFragmentById(R.id.notes_list);
        mNotesList.setNoteProvider(new NoteProvider());
        mNotesList.setOnNoteClickListener(new NoteClickListener());

        // Load notes
        notes = WordPress.wpDB.getLatestNotes();

        fragmentDetectors.add(new FragmentDetector(){
            @Override
            public Fragment getFragment(Note note){
                if (note.isCommentType()) {
                    return CommentDetailFragment.newInstance(note);
                }
                return null;
            }
        });
        fragmentDetectors.add(new FragmentDetector() {
            @Override
            public Fragment getFragment(Note note) {
                if (note.isMultiLineListTemplate()){
                    Fragment fragment = null;
                    if (note.isCommentLikeType())
                        fragment = new NoteCommentLikeFragment();
                    else if (note.isAutomattcherType())
                        fragment = new NoteMatcherFragment();
                    return fragment;
                }
                return null;
            }
        });
        fragmentDetectors.add(new FragmentDetector() {
            @Override
            public Fragment getFragment(Note note) {
                if (note.isSingleLineListTemplate()) {
                    return new SingleLineListFragment();
                }
                return null;
            }
        });
        fragmentDetectors.add(new FragmentDetector(){
            @Override
            public Fragment getFragment(Note note){
                AppLog.d(T.NOTIFS, String.format("Is it a big badge template? %b", note.isBigBadgeTemplate()));
                if (note.isBigBadgeTemplate()) {
                    return new BigBadgeFragment();
                }
                return null;
            }
        });

        GCMIntentService.activeNotificationsMap.clear();

        if (savedInstanceState == null) {
            launchWithNoteId();
        } else {
            mHasPerformedInitialUpdate = savedInstanceState.getBoolean(KEY_INITIAL_UPDATE);
        }

        refreshNotificationsListFragment(notes);

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
                notes = WordPress.wpDB.getLatestNotes();
                refreshNotificationsListFragment(notes);
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
                restClient.getNotifications(params, handler, handler);
            }
        } else {
            // on a tablet: open first note if none selected
            String fragmentTag = mNotesList.getTag();
            if (fragmentTag != null && fragmentTag.equals("tablet-view")) {
                if (notes != null && notes.size() > 0) {
                    Note note = notes.get(0);
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
        if (item.equals(mRefreshMenuItem)) {
            refreshNotes();
            return true;
        } else if (item.getItemId() == android.R.id.home){
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popNoteDetail();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
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

    public void popNoteDetail(){
        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentById(R.id.commentDetail);
        if (f == null) {
            fm.popBackStack();
        }
    }
    /**
     * Tries to pick the correct fragment detail type for a given note using the
     * fragment detectors
     */
    private Fragment fragmentForNote(Note note){
        for (FragmentDetector detector: fragmentDetectors) {
            Fragment fragment = detector.getFragment(note);
            if (fragment != null){
                return fragment;
            }
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
            restClient.markNoteAsRead(note,
                new RestRequest.Listener(){
                    @Override
                    public void onResponse(JSONObject response){
                        if (isFinishing())
                            return;

                        final NotesAdapter notesAdapter = mNotesList.getNotesAdapter();

                        note.setUnreadCount("0");
                        if (notesAdapter.getPosition(note) < 0) {
                            //Edge case when a note is opened with a note_id, and not tapping on the list. Loop over all notes in the adapter and find a match with the noteID
                            for (int i=0; i<notesAdapter.getCount(); i++) {
                                Note item = notesAdapter.getItem(i);
                                if( item.getId().equals(note.getId()) ) {
                                    item.setUnreadCount("0");
                                    break;
                                }
                            }
                        }
                       
                        WordPress.wpDB.addNote(note, false); //Update the DB
                        notesAdapter.notifyDataSetChanged();
                    }
                },
                new RestRequest.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
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
        Fragment fragment = fragmentForNote(note);
        if (fragment == null) {
            AppLog.d(T.NOTIFS, String.format("No fragment found for %s", note.toJSONObject()));
            return;
        }
        // swap the fragment
        NotificationFragment noteFragment = (NotificationFragment) fragment;
        Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA) && intent.getStringExtra(NOTE_ID_EXTRA).equals(note.getId())) {
            if (intent.hasExtra(NOTE_REPLY_EXTRA) || intent.hasExtra(NOTE_INSTANT_REPLY_EXTRA)) {
                fragment.setArguments(intent.getExtras());
            }
        }
        noteFragment.setNote(note);
        FragmentTransaction transaction = fm.beginTransaction();
        View container = findViewById(R.id.note_fragment_container);
        transaction.replace(R.id.note_fragment_container, fragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        // only add to backstack if we're removing the list view from the fragment container
        if (container.findViewById(R.id.notes_list) != null) {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
            transaction.addToBackStack(null);
        }
        transaction.commitAllowingStateLoss();
    }

    public void moderateComment(String siteId, String commentId, String status, final Note originalNote) {
        RestRequest.Listener success = new RestRequest.Listener(){
            @Override
            public void onResponse(JSONObject response){
                Map<String, String> params = new HashMap<String, String>();
                params.put("ids", originalNote.getId());
                NotesResponseHandler handler = new NotesResponseHandler() {
                    @Override
                    public void onNotes(List<Note> notes) {
                        // there should only be one note!
                        if (!notes.isEmpty()) {
                            Note updatedNote = notes.get(0);
                            updateModeratedNote(originalNote, updatedNote);
                        }
                    }
                };
                WordPress.restClient.getNotifications(params, handler, handler);
            }
        };
        RestRequest.ErrorListener failure = new RestRequest.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                AppLog.d(T.NOTIFS, String.format("Error moderating comment: %s", error));
                if (isFinishing())
                    return;
                Toast.makeText(NotificationsActivity.this, getString(R.string.error_moderate_comment), Toast.LENGTH_LONG).show();
                FragmentManager fm = getSupportFragmentManager();
                NoteCommentFragment f = (NoteCommentFragment) fm.findFragmentById(R.id.note_fragment_container);
                if (f != null) {
                    f.animateModeration(false);
                }
            }
        };
        WordPress.restClient.moderateComment(siteId, commentId, status, success, failure);
    }

    /*
     * passed note has just been moderated, update it in the list adapter and note fragment
     */
    private void updateModeratedNote(Note originalNote, Note updatedNote) {
        if (isFinishing())
            return;

        // TODO: position will be -1 for notes displayed from push notification, even though the note exists
        int position = mNotesList.getNotesAdapter().updateNote(originalNote, updatedNote);

        NoteCommentFragment f = (NoteCommentFragment) getSupportFragmentManager().findFragmentById(R.id.note_fragment_container);
        if (f != null) {
            // if this is the active note, update it in the fragment
            if (position >= 0 && position == mNotesList.getListView().getCheckedItemPosition()) {
                f.setNote(updatedNote);
                f.onStart();
            }
            // stop animating the moderation
            f.animateModeration(false);
        }
    }


    /*
     * triggered from the comment details fragment whenever a comment is changed (moderated, added,
     * deleted, etc.) - refresh notifications show changes are reflected here
     */
    @Override
    public void onCommentChanged(CommentActions.ChangedFrom changedFrom) {
        refreshNotes();
    }

    private void refreshNotificationsListFragment(List<Note> notes) {
        final NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
        adapter.clear();
        adapter.addAll(notes);
        adapter.notifyDataSetChanged();
        // mark last seen timestamp
        if (!notes.isEmpty()) {
            updateLastSeen(notes.get(0).getTimestamp());
        }
    }

    private void refreshNotes(){
        mFirstLoadComplete = false;
        mShouldAnimateRefreshButton = true;
        startAnimatingRefreshButton(mRefreshMenuItem);
        NotesResponseHandler notesHandler = new NotesResponseHandler(){
            @Override
            public void onNotes(final List<Note> notes) {
                mFirstLoadComplete = true;
                // nbradbury - saving notes can be slow, so do it in the background
                new Thread() {
                    @Override
                    public void run() {
                        WordPress.wpDB.clearNotes();
                        WordPress.wpDB.saveNotes(notes);
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
                adapter.clear();
                adapter.addAll(new ArrayList<Note>());
                adapter.notifyDataSetChanged();
                ToastUtils.showToast(getContext(), R.string.error_refresh_notifications,
                        ToastUtils.Duration.LONG);
                stopAnimatingRefreshButton(mRefreshMenuItem);
                mShouldAnimateRefreshButton = false;
            }
        };
        NotificationUtils.refreshNotifications(notesHandler, notesHandler);
    }

    private void updateLastSeen(String timestamp){
        restClient.markNotificationsSeen(timestamp,
            new RestRequest.Listener(){
                @Override
                public void onResponse(JSONObject response){
                    AppLog.d(T.NOTIFS, String.format("Set last seen time %s", response));
                }
            },
            new RestRequest.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){
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
                adapter.addAll(notes);
                adapter.notifyDataSetChanged();
            }
        };
        restClient.getNotifications(params, notesHandler, notesHandler);
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
                notes = new ArrayList<Note>(0);
                onNotes(notes);
                return;
            }

            try {
                notes = NotificationUtils.parseNotes(response);
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

    private abstract class FragmentDetector {
        abstract public Fragment getFragment(Note note);
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
}
