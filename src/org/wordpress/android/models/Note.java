/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models;

import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Note {
    protected static final String TAG="NoteModel";
    public static final String UNKNOWN_TYPE="unknown";
    public static final String COMMENT_TYPE="comment";
    public static final String LIKE_TYPE="like";
    
    private static String QUERY_SEPERATOR=".";
    private static String QUERY_ARRAY_INDEX_START="[";
    private static String QUERY_ARRAY_INDEX_END="]";
    private static String QUERY_ARRAY_FIRST="first";
    private static String QUERY_ARRAY_LAST="last";

    private JSONObject mNoteJSON;
    /**
     * Create a note using JSON from REST API
     */
    public Note(JSONObject noteJSON){
        mNoteJSON = noteJSON;
        Log.d(TAG, String.format("Built note of type: %s", getType()));
    }

    public String toString(){
        String labelText = queryJSON("subject.text", "");
        String label = Html.fromHtml(labelText.trim()).toString();
        return label;
    }
    
    public JSONObject toJSONObject(){
        return mNoteJSON;
    }
    public String getId(){
        return queryJSON("id", "0");
    }
    public String getType(){
        return queryJSON("type", UNKNOWN_TYPE);
    }
    public Boolean isType(String type){
        return getType().equals(type);
    }
    public Boolean isCommentType(){
        return isType(COMMENT_TYPE);
    }
    public String getIconURL(){
        return (String) queryJSON("subject.icon", "");
    }
    public String getCommentPreview(){
        return getCommentBody().toString().replace("\n", "").trim();
    }
    public Spanned getCommentBody(){
        return Html.fromHtml(getCommentText());
    }
    public String getCommentText(){
        return queryJSON("body.items[last].html", "");
    }
    public Boolean isUnread(){
        return queryJSON("unread", "0").equals("1");
    }
    protected Object queryJSON(String query){
        Object defaultObject = "";
        return queryJSON(query, defaultObject);
    }
    /**
     * Rudimentary system for pulling an item out of a JSON object hierarchy
     */
    public <U extends Object> U queryJSON(String query, U defaultObject){
        int offset = 0;
        JSONObject queryJSON = mNoteJSON;
        do {
            // find the next . or [
            int next_seperator = query.indexOf(QUERY_SEPERATOR, offset);
            boolean has_next_seperator = next_seperator > -1;
            int start_array = query.indexOf(QUERY_ARRAY_INDEX_START, offset);
            // pull out the key up to the seperator
            if (next_seperator == -1 && start_array == -1) {
                try {
                    return (U) queryJSON.get(query.substring(offset));
                } catch (JSONException e) {
                    Log.e(TAG, String.format("Failed to query %s", query), e);
                    return defaultObject;
                }
            }
            String key;
            if (start_array == -1 || (next_seperator > -1 && start_array > next_seperator )) {
                try {
                    queryJSON = queryJSON.getJSONObject(query.substring(offset, next_seperator));
                } catch (JSONException e) {
                    Log.e(TAG, String.format("Failed to query key %s", query), e);
                    return defaultObject;
                }
                offset = next_seperator+1;
                continue;
            }
            if (next_seperator == -1 || start_array == -1) {
                key = query.substring(offset, Math.max(next_seperator, start_array));
            } else {
                key = query.substring(offset, Math.min(next_seperator, start_array));
            }
            if (start_array > -1 && (start_array < next_seperator || !has_next_seperator)) {
                // time to pull off arrays
                try {
                    JSONArray arrayJSON = queryJSON.getJSONArray(key);
                    do {
                        int end_array = query.indexOf(QUERY_ARRAY_INDEX_END, start_array);
                        if (end_array <= start_array) break;
                        offset = end_array;
                        String index = query.substring(start_array+1, end_array);
                        int i;
                        if (index.equals(QUERY_ARRAY_FIRST)) {
                            i = 0;
                        } else if (index.equals(QUERY_ARRAY_LAST)) {
                            i = -1;
                        } else {
                            i = Integer.parseInt(index);
                        }
                        if (i < 0)
                            i = arrayJSON.length() + i;
                        start_array = query.indexOf(QUERY_ARRAY_INDEX_START, end_array);
                        // no more arrays and no seperator, end of query, return object at index
                        // e.g. key[0][0]
                        if (start_array == -1 && !has_next_seperator) {
                            return (U) arrayJSON.get(i);
                        }
                        // no more arrays but there's a seperator, we must have a JSONObject
                        if (start_array == -1 && has_next_seperator) {
                            queryJSON = arrayJSON.getJSONObject(i);
                            break;
                        }
                        // theres more query but this is the last array in this section
                        // eg key[0][0][0].something[0]
                        arrayJSON = arrayJSON.getJSONArray(i);
                        // the next item is an array, so pull the array off and continue
                    } while(start_array < next_seperator);
                    offset = next_seperator + 1;
                    continue;
                } catch (JSONException e) {
                    Log.e(TAG, String.format("Failed to query array %s", query), e);
                    return defaultObject;
                }
            }
            Log.d(TAG, String.format("Invalid query: %s", query));
            offset = -1;
        } while(offset > 0);
        return defaultObject;
    }
    
}