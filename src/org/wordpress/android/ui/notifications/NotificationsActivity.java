package org.wordpress.android.ui.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.IntentCompat;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wordpress.android.WordPress.restClient;

public class NotificationsActivity extends WPActionBarActivity {
    public static final String TAG="WPNotifications";
    public static final String NOTIFICATION_ACTION = "org.wordpress.android.NOTIFICATION";
    public static final String NOTE_ID_EXTRA="noteId";
    public static final String MD5_NOTE_ID_EXTRA="md5NoteId";
    public static final String FROM_NOTIFICATION_EXTRA="fromNotification";
    public static final String NOTE_REPLY_EXTRA="replyContent";
    public static final String NOTE_INSTANT_REPLY_EXTRA = "instantReply";
    public static final int FLAG_FROM_NOTE=Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_SINGLE_TOP |
                                            Intent.FLAG_ACTIVITY_NEW_TASK |
                                            IntentCompat.FLAG_ACTIVITY_CLEAR_TASK;

    Set<FragmentDetector> fragmentDetectors = new HashSet<FragmentDetector>();

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

        fragmentDetectors.add(new FragmentDetector(){
            @Override
            public Fragment getFragment(Note note){
                if (note.isCommentType()) {
                    Fragment fragment = new NoteCommentFragment();
                    return fragment;
                }
                return null;
            }
        });
        fragmentDetectors.add(new FragmentDetector(){
           @Override
           public Fragment getFragment(Note note){
               if (note.isSingleLineListTemplate()) {
                   Fragment fragment = new SingleLineListFragment();
                   return fragment;
               }
               return null;
           } 
        });
        fragmentDetectors.add(new FragmentDetector(){
            @Override
            public Fragment getFragment(Note note){
                Log.d(TAG, String.format("Is it a big badge template? %b", note.isBigBadgeTemplate()));
                if (note.isBigBadgeTemplate()) {
                    Fragment fragment = new BigBadgeFragment();
                    return fragment;
                }
                return null;
            }
        });
        
        GCMIntentService.activeNotificationsMap.clear();

        if (savedInstanceState == null)
            launchWithNoteId();

        // Load notes
        notes = WordPress.wpDB.loadNotes();
        refreshNotificationsListFragment(notes);

        if (savedInstanceState != null)
            popNoteDetail();
        if (mBroadcastReceiver == null)
            createBroadcastReceiver();
    }

    private void createBroadcastReceiver() {
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                notes = WordPress.wpDB.loadNotes();
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

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId(){
        final Intent intent = getIntent();
        if (intent.hasExtra(MD5_NOTE_ID_EXTRA)) {
            int hashid = Integer.valueOf(intent.getStringExtra(MD5_NOTE_ID_EXTRA));
            Note note = WordPress.wpDB.getNoteById(hashid);
            if (note != null) {
                openNote(note);
            }
        } else if (intent.hasExtra(NOTE_ID_EXTRA)) {
            // FIXME: check if in DB and not placeholder...
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
        } else {
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
        if (shouldAnimateRefreshButton) {
            shouldAnimateRefreshButton = false;
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
        Iterator<FragmentDetector> templates = fragmentDetectors.iterator();
        while(templates.hasNext()){
            FragmentDetector detector = templates.next();
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
    public void openNote(final Note note){
        if (note == null || isFinishing())
            return;
        // if note is "unread" set note to "read"
        if (note.isUnread()) {
            // send a request to mark note as read
            restClient.markNoteAsRead(note,
                new RestRequest.Listener(){
                    @Override
                    public void onResponse(JSONObject response){
                        note.setUnreadCount("0");
                        mNotesList.getNotesAdapter().notifyDataSetChanged();
                    }
                },
                new RestRequest.ErrorListener(){
                    @Override
                    public void onErrorResponse(VolleyError error){
                        Log.d(TAG, String.format("Failed to mark as read %s", error));
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
            Log.d(TAG, String.format("No fragment found for %s", note.toJSONObject()));
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
                            updateNote(originalNote, updatedNote);
                        }
                    }
                };
                WordPress.restClient.getNotifications(params, handler, handler);
            }
        };
        RestRequest.ErrorListener failure = new RestRequest.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error){
                Log.d(TAG, String.format("Error moderating comment: %s", error));
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
    
    public void updateNote(Note originalNote, Note updatedNote) {
        if (isFinishing())
            return;
        int position = mNotesList.getNotesAdapter().getPosition(originalNote);
        if (position >= 0) {
            mNotesList.getNotesAdapter().remove(originalNote);
            mNotesList.getNotesAdapter().insert(updatedNote, position);
            mNotesList.getNotesAdapter().notifyDataSetChanged();
            // Update comment detail fragment if we're still viewing the same note
            if (position == mNotesList.getListView().getCheckedItemPosition()) {
                FragmentManager fm = getSupportFragmentManager();
                NoteCommentFragment f = (NoteCommentFragment) fm.findFragmentById(R.id.note_fragment_container);
                if (f != null) {
                    f.setNote(updatedNote);
                    f.onStart();
                    f.animateModeration(false);
                }
            }
        }
    }

    public void refreshNotificationsListFragment(List<Note> notes) {
        final NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
        adapter.clear();
        adapter.addAll(notes);
        adapter.notifyDataSetChanged();
        // mark last seen timestamp
        if (!notes.isEmpty()) {
            updateLastSeen(notes.get(0).getTimestamp());
        }
    }

    public void refreshNotes(){
        mFirstLoadComplete = false;
        shouldAnimateRefreshButton = true;
        startAnimatingRefreshButton(mRefreshMenuItem);
        NotesResponseHandler handler = new NotesResponseHandler(){
            @Override
            public void onNotes(List<Note> notes) {
                mFirstLoadComplete = true;
                WordPress.wpDB.clearNotes();
                WordPress.wpDB.saveNotes(notes);
                refreshNotificationsListFragment(notes);
                stopAnimatingRefreshButton(mRefreshMenuItem);
            }
            @Override
            public void onErrorResponse(VolleyError error){
                //We need to show an error message? and remove the loading indicator from the list?
                mFirstLoadComplete = true;
                final NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
                adapter.clear();
                adapter.addAll(new ArrayList<Note>());
                adapter.notifyDataSetChanged();
                Toast.makeText(NotificationsActivity.this, String.format(getResources().getString(R.string.error_refresh), getResources().getText(R.string.notifications).toString().toLowerCase()), Toast.LENGTH_LONG).show();
                stopAnimatingRefreshButton(mRefreshMenuItem);
                shouldAnimateRefreshButton = false;
            }
        };
        NotificationUtils.refreshNotifications(handler, handler);
    }

    protected void updateLastSeen(String timestamp){
        
        restClient.markNotificationsSeen(timestamp,
            new RestRequest.Listener(){
                @Override
                public void onResponse(JSONObject response){
                    Log.d(TAG, String.format("Set last seen time %s", response));
                }
            },
            new RestRequest.ErrorListener(){
                @Override
                public void onErrorResponse(VolleyError error){
                    Log.d(TAG, String.format("Could not set last seen time %s", error));
                }
            }
        );
    }
    public void requestNotesBefore(Note note){
        Map<String, String> params = new HashMap<String, String>();
        Log.d(TAG, String.format("Requesting more notes before %s", note.queryJSON("timestamp", "")));
        params.put("before", note.queryJSON("timestamp", ""));
        NotesResponseHandler handler = new NotesResponseHandler(){
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
        restClient.getNotifications(params, handler, handler);
    }

    private class NoteProvider implements NotificationsListFragment.NoteProvider {
        @Override
        public void onRequestMoreNotifications(ListView notesList, ListAdapter notesAdapter){
            if (mFirstLoadComplete && !mLoadingMore) {
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
                Log.w(TAG, "Success, but did not receive any notes");
                notes = new ArrayList<Note>(0);
                onNotes(notes);
                return;
            }
            
            try {
                notes = NotificationUtils.parseNotes(response);
                onNotes(notes);
            } catch (JSONException e) {
                Log.e(TAG, "Success, but can't parse the response", e);
                showError(getString(R.string.error_parsing_response));
                return;
            }
        }
        
        @Override
        public void onErrorResponse(VolleyError error){
            mLoadingMore = false;
            showError();
            Log.d(TAG, String.format("Error retrieving notes: %s", error));
        }

        public void showError(String errorMessage){
            Toast.makeText(NotificationsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
        }
        
        public void showError(){
            Toast.makeText(NotificationsActivity.this, getString(R.string.error_generic), Toast.LENGTH_LONG).show();
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
}
