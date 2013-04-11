/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models;

import android.text.Html;

import org.json.JSONObject;
import org.json.JSONException;

public class Note {
    private JSONObject mNoteJSON;
    /**
     * Create a note using JSON from REST API
     */
    public Note(JSONObject noteJSON){
        mNoteJSON = noteJSON;
    }

    public String toString(){
        String label = "";
        try {
            JSONObject subjectJSON = mNoteJSON.getJSONObject("subject");
            label = Html.fromHtml(subjectJSON.getString("text").trim().toString()).toString();
        } catch (JSONException e) {
            label = "";
        }
        return label;
    }
    
}