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

import org.wordpress.android.util.JSONUtil;

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
    // JSON keys and values for looking up values
    private static final String NOTE_ACTION_REPLY="replyto-comment";
    private static final String REPLY_CONTENT_PARAM_KEY="content";

    private Map<String,JSONObject> mActions;
    private Reply mReply;
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
        if (text.equals("")) {
            text = queryJSON("subject.html", "");
        }
        return Html.fromHtml(text).toString();
    }
    public String getIconURL(){
        return queryJSON("subject.icon", "");
    }
    /**
     * Removes HTML and cleans up newlines and whitespace
     */
    public String getCommentPreview(){
        return getCommentBody().toString().replace("\n", " ").replaceAll("[\\s]{2,}", " ").trim();
    }
    /**
     * Gets the comment's text with getCommentText() and sends it through HTML.fromHTML
     */
    public Spanned getCommentBody(){
        return Html.fromHtml(getCommentText());
    }
    /**
     * For a comment note the text is in the body object's last item. It currently
     * is only provided in HTML format.
     */
    public String getCommentText(){
        return queryJSON("body.items[last].html", "");
    }
    /**
     * The inverse of isRead
     */
    public Boolean isUnread(){
        return !isRead();
    }
    /**
     * A note can have an "unread" of 0 or more ("likes" can have unread of 2+) to indicate the
     * quantity of likes that are "unread" within the single note. So for a note to be "read" it
     * should have "0"
     */
    public Boolean isRead(){
        return getUnreadCount().equals("0");
    }
    /**
     * For some reason the unread count is a string in the JSON API but is truly representd
     * by an Integer. We can handle a simple string.
     */
    public String getUnreadCount(){
        return queryJSON("unread", "0");
    }
    /**
     * 
     */
    public void setUnreadCount(String count){
        try {
            mNoteJSON.putOpt("unread", count);            
        } catch (JSONException e){
            Log.e(TAG, "Failed to set unread property", e);
        }
    }
    public Reply buildReply(String content){
        JSONObject replyAction = getActions().get(NOTE_ACTION_REPLY);
        Integer siteId = JSONUtil.queryJSON(replyAction, "params.blog_id", (Integer) 0);
        String commentId = JSONUtil.queryJSON(replyAction, "params.comment_id", "");
        Reply reply = new Reply(this, siteId.toString(), commentId, content);
        return reply;
    }
    /**
     * Get the timestamp provided by the API for the note.
     */
    public String getTimestamp(){
        return queryJSON("timestamp", "");
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
                    String actionType = JSONUtil.queryJSON(action, "type", "");
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
        return JSONUtil.queryJSON(this.toJSONObject(), query, defaultObject);
    }
    /**
     * Rudimentary system for pulling an item out of a JSON object hierarchy
     */
    public <U> U queryJSON(String query, U defaultObject){
        return JSONUtil.queryJSON(this.toJSONObject(), query, defaultObject);
    }
    /**
     * Represents a user replying to a note. Holds
     */
    public static class Reply {
        private Note mNote;
        private String mContent;
        private String mSiteId;
        private String mCommentId;
        
        Reply(Note note, String siteId, String commentId, String content){
            mNote = note;
            mSiteId = siteId;
            mCommentId = commentId;
            mContent = content;
        }
        public String getSiteId(){
            return mSiteId;
        }
        public String getCommentId(){
            return mCommentId;
        }
        public String getContent(){
            return mContent;
        }
        public Note getNote(){
            return mNote;
        }
    }

}