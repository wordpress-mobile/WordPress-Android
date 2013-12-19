/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.QuoteSpan;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.Emoticons;
import org.wordpress.android.util.JSONUtil;
import org.wordpress.android.util.WPHtml;
import org.wordpress.android.util.WPHtmlTagHandler;

import com.simperium.client.BucketSchema;
import com.simperium.client.Syncable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Note extends Syncable {

    public static class Schema extends BucketSchema<Note> {

        static public final String NAME = "notes";
        static public final String TIMESTAMP_INDEX = "timestamp";

        private static Indexer<Note> sTimestampIndexer = new Indexer<Note>() {

            @Override
            public List<Index> index(Note note) {
                List<Index> indexes = new ArrayList<Index>(1);
                try {
                    Integer timestamp = Integer.parseInt(note.getTimestamp());
                    indexes.add(new Index(TIMESTAMP_INDEX, timestamp));
                } catch (NumberFormatException e) {
                    // note will not have an indexed timestamp so it will
                    // show up at the end of a query sorting by timestamp
                    android.util.Log.e("WordPress", "Failed to index timestamp", e);
                }
                return indexes;
            }

        };

        public Schema() {
            // save an index with a timestamp
            addIndex(sTimestampIndexer);
        }

        @Override
        public String getRemoteName() {
            return NAME;
        }

        @Override
        public Note build(String key, JSONObject properties) {
            return new Note(properties);
        }

        public void update(Note note, JSONObject properties) {
            note.updateJSON(properties);
        }

    }


    protected static final String TAG="NoteModel";

    protected static final String NOTE_UNKNOWN_TYPE="unknown";
    public static final String NOTE_COMMENT_TYPE="comment";
    public static final String NOTE_COMMENT_LIKE_TYPE="comment_like";
    public static final String NOTE_LIKE_TYPE="like";
    public static final String NOTE_MATCHER_TYPE = "automattcher";

    // Notes have different types of "templates" for displaying differently
    // this is not a canonical list but covers all the types currently in use
    public static final String SINGLE_LINE_LIST_TEMPLATE="single-line-list";
    public static final String MULTI_LINE_LIST_TEMPLATE="multi-line-list";
    public static final String BIG_BADGE_TEMPLATE="big-badge";

    // JSON action keys
    private static final String ACTION_KEY_REPLY = "replyto-comment";
    private static final String ACTION_KEY_APPROVE = "approve-comment";
    private static final String ACTION_KEY_UNAPPROVE = "unapprove-comment";
    private static final String ACTION_KEY_SPAM = "spam-comment";

    public static enum EnabledActions {ACTION_REPLY,
                                       ACTION_APPROVE,
                                       ACTION_UNAPPROVE,
                                       ACTION_SPAM}

    // FIXME: add other types
    private static final Map<String, String> pnType2type = new Hashtable<String, String>() {{
        put("c", "comment");
    }};


    private Map<String,JSONObject> mActions;
    private Reply mReply;
    private JSONObject mNoteJSON;
    private SpannableStringBuilder mComment = new SpannableStringBuilder();
    private boolean mPlaceholder = false;

    private transient String mCommentPreview = null;
    private transient String mSubject = null;
    private transient String mIconUrl = null;
    private transient String mTimestamp = null;

    /**
     * Create a note using JSON from REST API
     */
    public Note(JSONObject noteJSON){
        mNoteJSON = noteJSON;
        preloadContent();
    }

    /**
     * Create a placeholder note from a Push Notification payload
     */
    public Note(Bundle extras) {
        JSONObject tmpNoteJSON = new JSONObject();
        String type = extras.getString("type");
        String finalType = NOTE_UNKNOWN_TYPE;
        if (type != null && pnType2type.containsKey(type)) {
            finalType = pnType2type.get(type);
        }
        JSONObject subject = new JSONObject();
        JSONObject body = new JSONObject();
        JSONObject html = new JSONObject();
        JSONArray items = new JSONArray();
        try {
            // subject
            if (finalType.equals(NOTE_COMMENT_TYPE)) {
                subject.put("text", extras.get("title"));
            } else {
                subject.put("text", extras.get("msg"));
            }
            subject.put("icon", extras.get("icon"));
            subject.put("noticon", extras.get("noticon"));

            html.put("html", extras.get("msg"));
            items.put(html);
            body.put("items", items);

            // fake timestamp to put it in top of the list
            String timestamp = extras.getString("note_timestamp");
            if (timestamp==null || timestamp.equals("")) {
                timestamp = "" + (System.currentTimeMillis() / 1000);
            }
            tmpNoteJSON.put("timestamp", timestamp);

            // root
            tmpNoteJSON.put("id", extras.get("note_id"));
            tmpNoteJSON.put("subject", subject);
            tmpNoteJSON.put("body", body);
            tmpNoteJSON.put("type", finalType);
            tmpNoteJSON.put("unread", "1");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to put key in noteJSON", e);
        }
        mNoteJSON = tmpNoteJSON;
    }

    /**
     * Simperium method @see Diffable
     */
    @Override
    public JSONObject getDiffableValue() {
        return mNoteJSON;
    }

    /**
     * Simperium method for identifying bucket object @see Diffable
     */
    @Override
    public String getSimperiumKey() {
        return getId();
    }

    public boolean isPlaceholder() {
        return mPlaceholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.mPlaceholder = placeholder;
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
        return queryJSON("type", NOTE_UNKNOWN_TYPE);
    }
    private Boolean isType(String type){
        return getType().equals(type);
    }
    public Boolean isCommentType(){
        return isType(NOTE_COMMENT_TYPE);
    }
    public Boolean isCommentLikeType(){
        return isType(NOTE_COMMENT_LIKE_TYPE);
    }
    public Boolean isAutomattcherType(){
        return isType(NOTE_MATCHER_TYPE);
    }
    public String getSubject(){
        if (mSubject==null) {
            String text = queryJSON("subject.text", "").trim();
            if (text.equals("")) {
                text = queryJSON("subject.html", "");
            }
            mSubject = Html.fromHtml(text).toString();
        }
        return mSubject;
    }
    public String getIconURL(){
        if (mIconUrl==null)
            mIconUrl = queryJSON("subject.icon", "");
        return mIconUrl;
    }
    /**
     * Removes HTML and cleans up newlines and whitespace
     */
    public String getCommentPreview(){
        if (mCommentPreview==null)
            mCommentPreview = getCommentBody().toString().replaceAll("\uFFFC", "").replace("\n", " ").replaceAll("[\\s]{2,}", " ").trim();
        return mCommentPreview;
    }
    /**
     * Gets the comment's text with getCommentText() and sends it through HTML.fromHTML
     */
    public Spanned getCommentBody(){
        return mComment;
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
     * Sets the note's read count to "0" and saves it to sync with Simperium
     */
    public void markAsRead() {
        try {
            mNoteJSON.put("unread", "0");
        } catch (JSONException e) {
            Log.e(TAG, "Unable to update note unread property", e);
            return;
        }
        save();
    }

    /**
     * For some reason the unread count is a string in the JSON API but is truly represented
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
        JSONObject replyAction = getActions().get(ACTION_KEY_REPLY);
        String restPath = JSONUtil.queryJSON(replyAction, "params.rest_path", "");
        Log.d(TAG, String.format("Search actions %s", restPath));
        Reply reply = new Reply(this, String.format("%s/replies/new", restPath), content);
        return reply;
    }

    /**
     * Get the timestamp provided by the API for the note - cached for performance
     */
    public String getTimestamp(){
        if (mTimestamp == null)
            mTimestamp = queryJSON("timestamp", "");
        return mTimestamp;
    }

    /*
     * returns a string representing the timespan based on the note's timestamp - used for display
     * in the notification list (ex: "3d")
     */
    public String getTimeSpan() {
        try {
            return DateTimeUtils.timestampToTimeSpan(Long.valueOf(getTimestamp()));
        } catch (NumberFormatException e) {
            Log.e(TAG, "failed to convert timestamp to long", e);
            return "";
        }
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


    protected void updateJSON(JSONObject json){

        mNoteJSON = json;

        // clear out the preloaded content
        mComment = null;
        mCommentPreview = null;
        mSubject = null;
        mIconUrl = null;

        // preload content again
        preloadContent();
    }

    /*
     * returns the actions allowed on this note, assumes it's a comment notification
     */
    public EnumSet<EnabledActions> getEnabledActions() {
        EnumSet<EnabledActions> actions = EnumSet.noneOf(EnabledActions.class);
        Map<String,JSONObject> jsonActions = getActions();
        if (jsonActions == null || jsonActions.size() == 0)
            return actions;
        if (jsonActions.containsKey(ACTION_KEY_REPLY))
            actions.add(EnabledActions.ACTION_REPLY);
        if (jsonActions.containsKey(ACTION_KEY_APPROVE))
            actions.add(EnabledActions.ACTION_APPROVE);
        if (jsonActions.containsKey(ACTION_KEY_UNAPPROVE))
            actions.add(EnabledActions.ACTION_UNAPPROVE);
        if (jsonActions.containsKey(ACTION_KEY_SPAM))
            actions.add(EnabledActions.ACTION_SPAM);
        return actions;
    }

    /**
     * pre-loads commonly-accessed fields - avoids performance hit of loading these
     * fields inside an adapter's getView()
     **/
    protected void preloadContent(){
        if (isCommentType()) {
            // pre-load the comment HTML for being displayed. Cleans up emoticons.
            mComment = Note.prepareHtml(getCommentText());
            // pre-load the preview text
            getCommentPreview();
        }
        // pre-load the subject and avatar url
        getSubject();
        getIconURL();
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

    public static class TimeStampComparator implements Comparator<Note> {
        @Override
        public int compare(Note a, Note b) {
            return b.getTimestamp().compareTo(a.getTimestamp());
        }
    }

    /**
     * Represents a user replying to a note. Holds
     */
    public static class Reply {
        private Note mNote;
        private String mContent;
        private String mRestPath;
        private JSONObject mCommentJson;
        
        Reply(Note note, String restPath, String content){
            mNote = note;
            mRestPath = restPath;
            mContent = content;
        }
        public String getContent(){
            return mContent;
        }
        public Note getNote(){
            return mNote;
        }
        public String getUrl(){
            if (isComplete()) {
                return JSONUtil.queryJSON(mCommentJson, "URL", "");
            }
            return null;
        }
        public String getAvatarUrl(){
            if (isComplete()) {
                return JSONUtil.queryJSON(mCommentJson, "author.avatar_URL", "");
            } else {
                return "";
            }
        }
        /**
         * Passes through Html.fromHtml to remove markup and replaces smilies with emoji
         */
        public String getCommentPreview(){
            if (isComplete()) {
                String text = JSONUtil.queryJSON(mCommentJson, "content", "");
                SpannableStringBuilder html = (SpannableStringBuilder) Html.fromHtml(text);
                return Emoticons.replaceEmoticonsWithEmoji(html).toString().trim();
            } else {
                return "";
            }
        }
        public String getRestPath(){
            return mRestPath;
        }
        public boolean isComplete(){
            return mCommentJson != null;
        }
        public JSONObject getCommentJson(){
            return mCommentJson;
        }
        public void setCommentJson(JSONObject commentJson){
            mCommentJson = commentJson;
        }
    }
    
    /**
     * Replaces emoticons with emoji
     */
    public static SpannableStringBuilder prepareHtml(String text){
        SpannableStringBuilder html = (SpannableStringBuilder) Html.fromHtml(text, null, new WPHtmlTagHandler());
        Emoticons.replaceEmoticonsWithEmoji(html);
        QuoteSpan spans[] = html.getSpans(0, html.length(), QuoteSpan.class);
        for (QuoteSpan span : spans) {
            html.setSpan(new WPHtml.WPQuoteSpan(), html.getSpanStart(span), html.getSpanEnd(span), html.getSpanFlags(span));
            html.removeSpan(span);
        }
        return html;
    }
}