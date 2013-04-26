/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models;

import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import java.util.Map;
import java.util.HashMap;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Note {
    protected static final String TAG="NoteModel";
    public static final String UNKNOWN_TYPE="unknown";
    public static final String COMMENT_TYPE="comment";
    public static final String LIKE_TYPE="like";
    // Notes have different types of "templates" for displaying differently
    // this is not a canonical list but covers all the types currently in use
    public static final String SINGLE_LINE_LIST_TEMPLATE="single-line-list";
    public static final String MULTI_LINE_LIST_TEMPLATE="multi-line-list";
    public static final String BIG_BADGE_TEMPLATE="big-badge";
    
    private static String QUERY_SEPERATOR=".";
    private static String QUERY_ARRAY_INDEX_START="[";
    private static String QUERY_ARRAY_INDEX_END="]";
    private static String QUERY_ARRAY_FIRST="first";
    private static String QUERY_ARRAY_LAST="last";
    
    private Map<String,JSONObject> mActions;

    private JSONObject mNoteJSON;
    /**
     * Create a note using JSON from REST API
     */
    public Note(JSONObject noteJSON){
        mNoteJSON = noteJSON;
    }

    public String toString(){
        return getSubject();
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
    public String getSubject(){
        String text = queryJSON("subject.text", "").trim();
        return Html.fromHtml(text).toString();
    }
    public String getIconURL(){
        return queryJSON("subject.icon", "");
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
    public String getTemplate(){
        return queryJSON("body.template", "");
    }
    public Boolean isMultiLineListTemplate(){
        return getTemplate().equals(MULTI_LINE_LIST_TEMPLATE);
    }
    public Boolean isSingleLineListTemplate(){
        return getTemplate().equals(SINGLE_LINE_LIST_TEMPLATE);
    }
    public Boolean isBigBadgeTemplate(){
        return getTemplate().equals(BIG_BADGE_TEMPLATE);
    }
    public Map<String,JSONObject> getActions(){
        if (mActions == null) {
            try {
                JSONArray actions = queryJSON("body.actions", new JSONArray());
                mActions = new HashMap<String,JSONObject>(actions.length());
                for (int i=0; i<actions.length(); i++) {
                    JSONObject action = actions.getJSONObject(i);
                    String actionType = queryJSON(action, "type", "");
                    if (!actionType.equals("")) {
                        mActions.put(actionType, action);
                    }
                }
            } catch (JSONException e) {
                Log.e(TAG, "Could not find actions", e);
                mActions = new HashMap<String,JSONObject>();
            }
        }
        return mActions;
    }
    protected Object queryJSON(String query){
        Object defaultObject = "";
        return queryJSON(query, defaultObject);
    }
    /**
     * Rudimentary system for pulling an item out of a JSON object hierarchy
     */
    public <U extends Object> U queryJSON(String query, U defaultObject){
        return queryJSON(this.toJSONObject(), query, defaultObject);
    }
    
    public static <U extends Object> U queryJSON(JSONObject source, String query, U defaultObject){
        int nextSeperator = query.indexOf(QUERY_SEPERATOR);
        int nextIndexStart = query.indexOf(QUERY_ARRAY_INDEX_START);
        if (nextSeperator == -1 && nextIndexStart == -1) {
            // last item let's get it
            try {
                return (U) source.get(query);
            } catch (JSONException e) {
                Log.e(TAG, String.format("Could not complete query %s", query), e);
            }
        }
        int endQuery;
        if (nextSeperator == -1 || nextIndexStart == -1) {
            endQuery = Math.max(nextSeperator, nextIndexStart);
        } else {
            endQuery = Math.min(nextSeperator, nextIndexStart);
        }
        String nextQuery = query.substring(endQuery);
        String key = query.substring(0, endQuery);
        try {
            if (nextQuery.indexOf(QUERY_SEPERATOR) == 0) {
                return queryJSON(source.getJSONObject(key), nextQuery.substring(1), defaultObject);
            } else if (nextQuery.indexOf(QUERY_ARRAY_INDEX_START) == 0){
                return queryJSON(source.getJSONArray(key), nextQuery, defaultObject);
            } else if (!nextQuery.equals("")){
                Log.d(TAG, String.format("Incorrect query for next object %s %d %d", nextQuery, nextQuery.indexOf(QUERY_ARRAY_INDEX_START), nextQuery.indexOf(QUERY_SEPERATOR)));
                return defaultObject;
            }
            return (U) source.get(key);
        } catch (JSONException e) {
            Log.e(TAG, String.format("Could not complete query %s", query), e);
            return defaultObject;
        }
    }
    
    public static <U extends Object> U queryJSON(JSONArray source, String query, U defaultObject){
        // query must start with [ have an index and then have ]
        int indexStart = query.indexOf(QUERY_ARRAY_INDEX_START);
        int indexEnd = query.indexOf(QUERY_ARRAY_INDEX_END);
        if (indexStart == -1 || indexEnd == -1 || indexStart > indexEnd) {
            Log.d(TAG, String.format("Incorrect query for array index %s", query));
            return defaultObject;
        }
        // get "index" from "[index]"
        String indexStr = query.substring(indexStart + 1, indexEnd);
        int index;
        if (indexStr.equals(QUERY_ARRAY_FIRST)) {
            index = 0;
        } else if (indexStr.equals(QUERY_ARRAY_LAST)){
            index = -1;
        } else {
            index = Integer.parseInt(indexStr);
        }
        if(index < 0){
            index = source.length() + index;
        }
        // copy remaining query
        String remainingQuery = query.substring(indexEnd + 1);
        try {
            if (remainingQuery.indexOf(QUERY_ARRAY_INDEX_START) == 0) {
                return queryJSON(source.getJSONArray(index), remainingQuery, defaultObject);
            } else if(remainingQuery.indexOf(QUERY_SEPERATOR) == 0){
                return queryJSON(source.getJSONObject(index), remainingQuery.substring(1), defaultObject);
            } else if(!remainingQuery.equals("")){
                // TODO throw an exception since the query isn't valid?
                Log.d(TAG, String.format("Incorrect query for next object %s", remainingQuery));
                return defaultObject;
            }
            return (U) source.get(index);
        } catch (JSONException e) {
            Log.e(TAG, String.format("Could not complete query %s", query), e);
            return defaultObject;
        }

    }

}