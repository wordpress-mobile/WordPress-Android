package com.wordpress.notereader;

import static com.wordpress.notereader.WPClient.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.JsonHttpResponseHandler;

import android.content.SharedPreferences;
import android.app.ListActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import android.text.Html;

public class NotesActivity extends ListActivity
{
    private static final int LOGIN_REQUEST_CODE=0xCC;
    private static final String PREFERENCES_NAME="account-prefs";
    private static final String OAUTH_TOKEN_PREFERENCE="oauth-access-token";
    public static final String TAG="WPNotes";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        loadAccessTokenFromPreferences();
        setContentView(R.layout.main);
        setListAdapter(new NotesAdapter(this));
    }
    
    @Override
    public void onStart(){
        super.onStart();
        if (!isAuthenticated()) {
            startLoginActivity();
        } else {
            refreshNotifications();
        }
    }
    
    public void startLoginActivity(){
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST_CODE);
    }
        
    private void refreshNotifications(){
        get("notifications", new NotesResponseHandler());
    }
    
    private NotesAdapter getNotesAdapter(){
        return (NotesAdapter) getListAdapter();
    }
    
    private class NotesAdapter extends ArrayAdapter<Note> {
        private static final int LAYOUT_ID=R.layout.note;
        private static final int VIEW_ID=R.id.note_label;
        public NotesAdapter(Context context){
            super(context, LAYOUT_ID, VIEW_ID);
        }
        
        public void bindView(int position, View view){
            Log.d(TAG, String.format("Bind view for %d", position));
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            View view = super.getView(position, convertView, parent);
            
            return view;
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(TAG, String.format("Activity finished %d with result %d", requestCode, resultCode));
        switch (requestCode) {
            case LOGIN_REQUEST_CODE:
            Log.d(TAG, String.format("Was result (%d) ok? %d", resultCode, Activity.RESULT_OK));
            if (resultCode == RESULT_OK) {
                String accessToken = data.getStringExtra(LoginActivity.OAUTH_TOKEN_EXTRA);
                setAccessToken(accessToken);
                saveAccessTokenToPreferences(accessToken);
                refreshNotifications();
            }
            break;
            
        }
    }
    
    private void loadAccessTokenFromPreferences(){
        SharedPreferences prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String token = prefs.getString(OAUTH_TOKEN_PREFERENCE, null);
        Log.d(TAG, String.format("Retrieved access token: %s", token));
        setAccessToken(token);
    }
    
    private void saveAccessTokenToPreferences(String token){
        Log.d(TAG, String.format("Saving access token %s", token));
        SharedPreferences.Editor prefs = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit();
        prefs.putString(OAUTH_TOKEN_PREFERENCE, token);
        prefs.commit();
    }
    
    private class NotesResponseHandler extends JsonHttpResponseHandler {
        private static final String NOTES_KEY="notes";
        
        @Override
        public void onStart(){
            Log.d(TAG, "Fetching notes");
        }
        @Override
        public void onFinish(){
            Log.d(TAG, "Finished notes request");
        }
        @Override
        public void onFailure(Throwable error, JSONObject response){
            Log.e(TAG, String.format("Failed to retrieve notes: %s"), error);
        }
        
        @Override
        public void onFailure(Throwable error, String response){
            Log.e(TAG, String.format("Failed: %s", response), error);
        }
        
        @Override 
        public void onSuccess(int statuScode, JSONObject notes){
           try {
               final JSONArray notesArray = notes.getJSONArray(NOTES_KEY);
               getNotesAdapter().clear();
               for (int i=0;i<notesArray.length();i++) {
                   JSONObject noteJson = notesArray.getJSONObject(i);
                   getNotesAdapter().add(new Note(noteJson));
               }
               
               runOnUiThread( new Runnable(){
                  @Override
                  public void run(){
                      getNotesAdapter().notifyDataSetChanged();
                  } 
               });
               
           } catch (JSONException e) {
               Log.e(TAG, "Couldn't retrieve notes", e);
           }
        }
        
    }
    
    private class Note {
        
        private static final String SUBJECT_KEY="subject";
        private static final String SUBJECT_TEXT_KEY="text";
        private static final String UNKNOWN_SUBJECT="no subject";
        
        private JSONObject mNoteData;
        
        public Note(JSONObject noteData){
            mNoteData = noteData;
        }
        
        public String toString(){
            // try to get the subject.text property
            try {
                JSONObject subject =  mNoteData.getJSONObject(SUBJECT_KEY);
                String subjectText = subject.getString(SUBJECT_TEXT_KEY);
                return Html.fromHtml(subjectText.trim()).toString();
            } catch (JSONException error) {
                return UNKNOWN_SUBJECT;
            }
        }
        
    }
    
}
