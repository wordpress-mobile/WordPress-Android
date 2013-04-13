package org.wordpress.android.ui.notifications;

import android.support.v4.app.Fragment;
import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import org.wordpress.android.R;
import org.wordpress.android.models.Note;

import static org.wordpress.android.WordPress.*;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

class NotificationsDetailFragment extends Fragment {
    public static final String TAG="NoteDetail";
    public static final String NOTE_ID_ARGUMENT="note_id";
    public static final String NOTE_JSON_ARGUMENT="note_json";
    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        // if we have a noteID
        Bundle arguments = getArguments();
        if (arguments != null) {
            if (arguments.containsKey(NOTE_ID_ARGUMENT)) {
                String noteId = arguments.getString(NOTE_ID_ARGUMENT);
                RequestParams params = new RequestParams();
                params.put("ids", noteId);
                restClient.getNotifications(params, new NoteResponseHandler(){
                    @Override
                    public void onSuccess(Note note){
                        loadNote(note);
                    }
                });
            }
        }
        
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle state){
        View view = inflater.inflate(R.layout.notifications_detail, parent, false);
        return view;
    }
    /**
     * Display the note in the provided view
     */
    public void loadNote(Note note){
        Log.d(TAG, String.format("You should load the note bro: %s", Thread.currentThread().getName()));
    }
    
    private class NoteResponseHandler extends JsonHttpResponseHandler {
        public void onSuccess(Note note){}
        @Override
        public void onSuccess(JSONObject response){
            Note note = null;
            try {
                JSONArray notesJson = response.getJSONArray("notes");
                JSONObject noteData = notesJson.getJSONObject(0);
                note = new Note(noteData);
            } catch (JSONException e) {
                Log.e(TAG, String.format("Failed to get a note %s", response), e);
                onFailure(e, response);
                return;
            }
            onSuccess(note);
        }
    }
}