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
import org.wordpress.android.models.Comment;
import org.wordpress.android.models.Note;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.comments.CommentActions;
import org.wordpress.android.ui.comments.CommentDetailFragment;
import org.wordpress.android.ui.reader_native.actions.ReaderAuthActions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.wordpress.android.WordPress.restClient;

public class NotificationsActivity extends WPActionBarActivity implements CommentActions.OnCommentChangeListener {
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
    private static final String KEY_INITIAL_UPDATE = "initial_update";

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
        mNotesList.setOnNoteClickListener(new NoteClickListener());

        fragmentDetectors.add(new FragmentDetector(){
            @Override
            public Fragment getFragment(Note note){
                if (note.isCommentType()) {
                    //Fragment fragment = new NoteCommentFragment();
                    Fragment fragment = CommentDetailFragment.newInstance(note);
                    return fragment;
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
                    Fragment fragment = new SingleLineListFragment();
                    return fragment;
                }
                return null;
            }
        });

        fragmentDetectors.add(new FragmentDetector(){
            @Override
            public Fragment getFragment(Note note){
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
    private boolean mHasPerformedInitialUpdate;

    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId(){
        final Intent intent = getIntent();
        // TODO: Check bucket for note
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.equals(mRefreshMenuItem)) {
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
        menu.removeItem(R.id.menu_refresh);
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
            // mark as read which syncs with simperium
            note.markAsRead();
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
                        }
                    }
                };
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

    /**
     * these four methods implement OnCommentChangedListener and are triggered from the comment details fragment whenever a comment is changed
     */
    @Override
    public void onCommentAdded() {
    }

    @Override
    public void onCommentDeleted() {
    }

    @Override
    public void onCommentModerated(final Comment comment, final Note note) {
        if (isFinishing())
            return;
        if (note == null) 
            return;
        // Simperium notifies that the object is updated
    }

    @Override
    public void onCommentsModerated(final List<Comment> comments) {
    }

    protected void updateLastSeen(String timestamp){
        // TODO: Write to meta bucket last seen time
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
}
