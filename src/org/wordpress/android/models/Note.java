/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models;

import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.Emoticons;
import org.wordpress.android.util.HtmlUtils;
import org.wordpress.android.util.JSONUtil;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class Note {
    private static final String NOTE_UNKNOWN_TYPE     = "unknown";
    private static final String NOTE_COMMENT_TYPE     = "comment";
    public static final String NOTE_COMMENT_LIKE_TYPE = "comment_like";
    public static final String NOTE_LIKE_TYPE         = "like";
    private static final String NOTE_MATCHER_TYPE     = "automattcher";

    // Notes have different types of "templates" for displaying differently
    // this is not a canonical list but covers all the types currently in use
    private static final String SINGLE_LINE_LIST_TEMPLATE = "single-line-list";
    private static final String MULTI_LINE_LIST_TEMPLATE  = "multi-line-list";
    private static final String BIG_BADGE_TEMPLATE        = "big-badge";

    // JSON action keys
    private static final String ACTION_KEY_REPLY     = "replyto-comment";
    private static final String ACTION_KEY_APPROVE   = "approve-comment";
    private static final String ACTION_KEY_UNAPPROVE = "unapprove-comment";
    private static final String ACTION_KEY_SPAM      = "spam-comment";

    public static enum EnabledActions {ACTION_REPLY,
                                       ACTION_APPROVE,
                                       ACTION_UNAPPROVE,
                                       ACTION_SPAM}

    private Map<String,JSONObject> mActions;
    private final JSONObject mNoteJSON;
    private SpannableStringBuilder mComment = new SpannableStringBuilder();
    private boolean mPlaceholder = false;

    private int mBlogId;
    private int mPostId;
    private long mCommentId;
    private long mCommentParentId;

    private transient String mCommentPreview;
    private transient String mSubject;
    private transient String mIconUrl;
    private transient String mTimestamp;
    private transient String mSnippet;

    // TODO: add other types
    private static final Map<String, String> pnType2type = new Hashtable<String, String>() {{
        put("c", "comment");
    }};

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
            AppLog.e(T.NOTIFS, "Failed to put key in noteJSON", e);
        }
        mNoteJSON = tmpNoteJSON;
    }

    public boolean isPlaceholder() {
        return mPlaceholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.mPlaceholder = placeholder;
    }

    /*
     * this is the text that is displayed in the notes adapter in NotificationListFragment
     */
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
    Spanned getCommentBody(){
        return mComment;
    }

    /**
     * For a comment note the text is in the body object's last item. It currently
     * is only provided in HTML format.
     */
    String getCommentText(){
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
    Boolean isRead(){
        return getUnreadCount().equals("0");
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
            AppLog.e(T.NOTIFS, "Failed to set unread property", e);
        }
    }

    public Reply buildReply(String content){
        JSONObject replyAction = getActions().get(ACTION_KEY_REPLY);
        String restPath = JSONUtil.queryJSON(replyAction, "params.rest_path", "");
        AppLog.d(T.NOTIFS, String.format("Search actions %s", restPath));
        return new Reply(this, String.format("%s/replies/new", restPath), content);
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
            AppLog.e(T.NOTIFS, "failed to convert timestamp to long", e);
            return "";
        }
    }

    String getTemplate(){
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
    Map<String,JSONObject> getActions(){
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
                AppLog.e(T.NOTIFS, "Could not find actions", e);
                mActions = new HashMap<String,JSONObject>();
            }
        }
        return mActions;
    }

    /*
     * returns the "meta" section of the note's JSON (not guaranteed to exist)
     */
    private JSONObject getJSONMeta() {
        return JSONUtil.getJSONChild(this.toJSONObject(), "meta");
    }

    /*
     * returns the value of the passed name in the meta section of the JSON
     */
    public int getMetaValueAsInt(String name, int defaultValue) {
        JSONObject jsonMeta = getJSONMeta();
        if (jsonMeta == null)
            return defaultValue;
        return jsonMeta.optInt(name, defaultValue);
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
    void preloadContent(){
        if (isCommentType()) {
            // pre-load the comment HTML for being displayed. Cleans up emoticons.
            mComment = HtmlUtils.fromHtml(getCommentText());
            // pre-load the preview text
            getCommentPreview();
        }

        // pre-load the subject and avatar url
        getSubject();
        getIconURL();

        // pre-load site/post/comment IDs
        preloadMetaIds();
    }

    /*
     * nbradbury - preload the blog, post, & comment IDs from the meta section
     * ids={"site":61509427,"self":993925505,"post":161,"comment":178,"comment_parent":0}
     */
    private void preloadMetaIds() {
        JSONObject jsonMeta = getJSONMeta();
        if (jsonMeta == null)
            return;
        JSONObject jsonIDs = jsonMeta.optJSONObject("ids");
        if (jsonIDs == null)
            return;
        mBlogId = jsonIDs.optInt("site");
        mPostId = jsonIDs.optInt("post");
        mCommentId = jsonIDs.optLong("comment");
        mCommentParentId = jsonIDs.optLong("comment_parent");
    }

    public int getBlogId() {
        return mBlogId;
    }
    public int getPostId() {
        return mPostId;
    }
    public long getCommentId() {
        return mCommentId;
    }
    public long getCommentParentId() {
        return mCommentParentId;
    }

    /*
     * plain-text snippet returned by the server - currently shown only for comments
     */
    String getSnippet() {
        if (mSnippet == null) {
            mSnippet = queryJSON("snippet", "");
        }
        return mSnippet;
    }

    public boolean hasSnippet() {
        return !TextUtils.isEmpty(getSnippet());
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
        private final Note mNote;
        private final String mContent;
        private final String mRestPath;
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
}