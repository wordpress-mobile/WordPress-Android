package org.wordpress.android.ui.notifications;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.content.Intent;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Note;
import static org.wordpress.android.WordPress.*;

import com.wordpress.rest.OauthTokenResponseHandler;
import com.wordpress.rest.OauthToken;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class NotificationsActivity extends WPActionBarActivity {
    public static final String TAG="WPNotifications";
    public static final String NOTE_ID_EXTRA="noteId";
    public static final String FROM_NOTIFICATION_EXTRA="fromNotification";
    
    Set<FragmentDetector> fragmentDetectors = new HashSet<FragmentDetector>();

    private NotificationsListFragment mNotesList;
    private MenuItem mRefreshMenuItem;
    private boolean mLoadingMore = false;
    private boolean mFirstLoadComplete = false;
    private Fragment detailFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.notifications);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.notifications));
        
        Blog blog = getCurrentBlog();
        FragmentManager fm = getSupportFragmentManager();
        mNotesList = (NotificationsListFragment) fm.findFragmentById(R.id.notes_list);
        mNotesList.setNoteProvider(new NoteProvider());
        mNotesList.setOnNoteClickListener(new NoteClickListener());
        
        // ok it's time to request notifications
        // TODO: access token should be stored in preferences, not fetched each time
        restClient.requestAccessToken(blog.getUsername(), blog.getPassword(), new OauthTokenResponseHandler(){
            @Override
            public void onStart(){
                startAnimatingRefreshButton(mRefreshMenuItem);
                shouldAnimateRefreshButton = true;
            }
            @Override
            public void onSuccess(OauthToken token){
                launchWithNoteId();
                refreshNotes();
            }
            @Override
            public void onFailure(Throwable e, JSONObject response){
                Log.e(TAG, String.format("Failed: %s", response), e);
            }
            @Override
            public void onFinish(){
            }
        });

        fragmentDetectors.add(new FragmentDetector(){
            @Override
            public Fragment getFragment(Note note){
                if (note.isCommentType()) {
                    Fragment fragment = new NotificationsCommentFragment();
                    return fragment;
                }
                return null;
            }
        });
        
        GCMIntentService.activeNotificationsMap.clear();
    }
    
    
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        GCMIntentService.activeNotificationsMap.clear();
        
    }



    /**
     * Detect if Intent has a noteId extra and display that specific note detail fragment
     */
    private void launchWithNoteId(){
        Intent intent = getIntent();
        if (intent.hasExtra(NOTE_ID_EXTRA)) {
            // find it/load it etc
            RequestParams params = new RequestParams();
            params.put("ids", intent.getStringExtra(NOTE_ID_EXTRA));
            restClient.getNotifications(params, new NotesResponseHandler(){
                @Override
                public void onStart(){
                    Log.d(TAG, "Finding note to display!");
                }
                @Override
                public void onSuccess(List<Note> notes){
                    // there should only be one note!
                    Note note = notes.get(0);
                    openNote(note);
                }
            });
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
        // TODO: change to note detail id
        NotificationDetailFragment f;
        f = (NotificationDetailFragment) fm.findFragmentById(R.id.commentDetail);
        if (f == null) {
            fm.popBackStack();
        }
    }
    
    private Fragment fragmentForNote(Note note){
        Iterator<FragmentDetector> templates = fragmentDetectors.iterator();
        while(templates.hasNext()){
            FragmentDetector detector = templates.next();
            Fragment fragment = detector.getFragment(note);
            if (fragment != null){
                return fragment;
            }
        }
        // by default return plain detail fragment
        return new NotificationDetailFragment();
    }
    /**
     *  Open a note fragment based on the type of note
     */
    public void openNote(Note note){
        if (note == null)
            return;
        FragmentManager fm = getSupportFragmentManager();
        // remove the note detail if it's already on there
        if (fm.getBackStackEntryCount() > 0){
            fm.popBackStack();
        }
        Fragment fragment = fragmentForNote(note);
        // swap the fragment
        NotificationFragment noteFragment = (NotificationFragment) fragment;
        noteFragment.setNote(note);
        FragmentTransaction transaction = fm.beginTransaction();
        transaction.replace(R.id.note_fragment_container, fragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    public void refreshNotes(){
        restClient.getNotifications(new NotesResponseHandler(){
            @Override
            public void onStart(){
                super.onStart();
                mFirstLoadComplete = false;
                startAnimatingRefreshButton(mRefreshMenuItem);
            }
            @Override
            public void onSuccess(List<Note> notes){
                final NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
                adapter.clear();
                adapter.addAll(notes);
                adapter.notifyDataSetChanged();
            }
            public void onFinish(){
                super.onFinish();
                mFirstLoadComplete = true;
                stopAnimatingRefreshButton(mRefreshMenuItem);
            }
        });
    }
    public void requestNotesBefore(Note note){
        RequestParams params = new RequestParams();
        Log.d(TAG, String.format("Requesting more notes before %s", note.queryJSON("timestamp", "")));
        params.put("before", note.queryJSON("timestamp", ""));
        restClient.getNotifications(params, new NotesResponseHandler(){
            @Override
            public void onSuccess(List<Note> notes){
                NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
                adapter.addAll(notes);
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class NoteProvider implements NotificationsListFragment.NoteProvider {
        @Override
        public void onRequestMoreNotifications(ListView notesList, ListAdapter notesAdapter){
            if (mFirstLoadComplete && !mLoadingMore) {
                NotificationsListFragment.NotesAdapter adapter = mNotesList.getNotesAdapter();
                Note lastNote = adapter.getItem(adapter.getCount()-1);
                requestNotesBefore(lastNote);
            }
        }
    }
    
    private class NoteClickListener implements NotificationsListFragment.OnNoteClickListener {
        @Override
        public void onClickNote(Note note){
            openNote(note);
        }
    }
    
    public class NotesResponseHandler extends JsonHttpResponseHandler {
        @Override
        public void onStart(){
            mLoadingMore = true;
        }
        @Override
        public void onFinish(){
            mLoadingMore = false;
        }
        public void onSuccess(List<Note> notes){};
        @Override
        public void onSuccess(int statusCode, JSONObject response){
            List<Note> notes;
            try {
                JSONArray notesJSON = response.getJSONArray("notes");
                notes = new ArrayList<Note>(notesJSON.length());
                for (int i=0; i<notesJSON.length(); i++) {
                    Note n = new Note(notesJSON.getJSONObject(i));
                    notes.add(n);
                }
           } catch (JSONException e) {
               Log.e(TAG, "Did not receive any notes", e);
               onFailure(e, response);
               return;
           }
           onSuccess(notes);
        }
    }
    
    private abstract class FragmentDetector {
        abstract public Fragment getFragment(Note note);
    }
}
