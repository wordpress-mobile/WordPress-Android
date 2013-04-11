package org.wordpress.android.ui.notifications;

import java.util.List;
import java.util.ArrayList;

import android.os.Bundle;
import android.util.Log;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Note;
import static org.wordpress.android.WordPress.*;

import com.wordpress.rest.OauthTokenResponseHandler;
import com.wordpress.rest.OauthToken;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;


public class NotificationsActivity extends WPActionBarActivity {
    public final String TAG="WPNotifications";

    private NotificationsListFragment mNotesList;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        createMenuDrawer(R.layout.notifications);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        setTitle(getString(R.string.notifications));
        
        FragmentManager fm = getSupportFragmentManager();
        mNotesList = (NotificationsListFragment) fm.findFragmentById(R.id.notes_list);
        
        Blog blog = getCurrentBlog();
        
        // ok it's time to request notifications
        // TODO: access token should be stored in preferences, not fetched each time
        restClient.requestAccessToken(blog.getUsername(), blog.getPassword(), new OauthTokenResponseHandler(){
            @Override
            public void onStart(){
                Log.d(TAG, "Requesting token");
            }
            @Override
            public void onSuccess(OauthToken token){
                refreshNotes();
            }
            @Override
            public void onFailure(Throwable e, JSONObject response){
                Log.e(TAG, String.format("Failed: %s", response), e);
            }
            @Override
            public void onFinish(){
                Log.d(TAG, "Done requesting token");
            }
        });
        Log.d(TAG, "Created");
    }
    
    public void refreshNotes(){
        restClient.getNotifications(new JsonHttpResponseHandler(){
           @Override
           public void onSuccess(int responseCode, JSONObject response) {
               try {
                   JSONArray notesJSON = response.getJSONArray("notes");
                   final List<Note> notesList = new ArrayList<Note>(notesJSON.length());
                   for (int i=0; i<notesJSON.length(); i++) {
                       Note n = new Note(notesJSON.getJSONObject(i));
                       notesList.add(n);
                   }
                   runOnUiThread(new Runnable(){
                       @Override
                       public void run(){
                           displayNotes(notesList);
                       }
                   });
                   
               } catch (JSONException e) {
                   Log.e(TAG, "Did not receive any notes", e);
               }
           }
        });
    }
    
    public void displayNotes(List<Note> notes){
        // create a new ListAdapter and set it on the fragment
        ArrayAdapter<Note> adapter = new ArrayAdapter<Note>(this, R.layout.note_list_item, R.id.note_label, notes);
        mNotesList.setListAdapter(adapter);
    }
}