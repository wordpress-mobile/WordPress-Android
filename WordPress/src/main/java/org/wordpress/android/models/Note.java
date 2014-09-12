/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models;

import android.text.Html;
import android.text.Spannable;
import android.util.Log;

import com.simperium.client.BucketSchema;
import com.simperium.client.Syncable;

import org.apache.commons.lang.time.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.JSONUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

public class Note extends Syncable {
    private static final String TAG = "NoteModel";

    // Maximum character length for a comment preview
    static private final int MAX_COMMENT_PREVIEW_LENGTH = 200;

    private static final String NOTE_UNKNOWN_TYPE = "unknown";
    private static final String NOTE_COMMENT_TYPE = "comment";
    private static final String NOTE_MATCHER_TYPE = "automattcher";

    // JSON action keys
    private static final String ACTION_KEY_REPLY = "replyto-comment";
    private static final String ACTION_KEY_APPROVE = "approve-comment";
    private static final String ACTION_KEY_SPAM = "spam-comment";
    private static final String ACTION_KEY_LIKE = "like-comment";

    private JSONObject mActions;
    private JSONObject mNoteJSON;

    public static enum EnabledActions {
        ACTION_REPLY,
        ACTION_APPROVE,
        ACTION_UNAPPROVE,
        ACTION_SPAM,
        ACTION_LIKE
    }

    public static enum NoteTimeGroup {
        GROUP_TODAY,
        GROUP_YESTERDAY,
        GROUP_OLDER_TWO_DAYS,
        GROUP_OLDER_WEEK,
        GROUP_OLDER_MONTH
    }

    /**
     * Create a note using JSON from Simperium
     */
    private Note(JSONObject noteJSON) {
        mNoteJSON = noteJSON;
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

    private JSONObject toJSONObject() {
        return mNoteJSON;
    }

    public String getId() {
        return String.valueOf(queryJSON("id", 0));
    }

    private String getType() {
        return queryJSON("type", NOTE_UNKNOWN_TYPE);
    }

    private Boolean isType(String type) {
        return getType().equals(type);
    }

    public Boolean isCommentType() {
        return (isAutomattcherType() && JSONUtil.queryJSON(mNoteJSON, "meta.ids.comment", -1) != -1) ||
                isType(NOTE_COMMENT_TYPE);
    }

    public Boolean isAutomattcherType() {
        return isType(NOTE_MATCHER_TYPE);
    }

    private JSONObject getSubject() {
        try {
            JSONArray subjectArray = mNoteJSON.getJSONArray("subject");
            if (subjectArray.length() > 0) {
                return subjectArray.getJSONObject(0);
            }
        } catch (JSONException e) {
            return null;
        }

        return null;
    }

    public Spannable getFormattedSubject() {
        return NotificationsUtils.getSpannableTextFromIndices(getSubject(), null);
    }

    public String getTitle() {
        return queryJSON("title", "");
    }

    private String getIconURL() {
        return queryJSON("icon", "");
    }

    private String getCommentSubject() {
        JSONArray subjectArray = mNoteJSON.optJSONArray("subject");
        if (subjectArray != null) {
            String commentSubject = JSONUtil.queryJSON(subjectArray, "subject[1].text", "");

            // Trim down the comment preview if the comment text is too large.
            if (commentSubject != null && commentSubject.length() > MAX_COMMENT_PREVIEW_LENGTH) {
                commentSubject = commentSubject.substring(0, MAX_COMMENT_PREVIEW_LENGTH - 1);
            }

            return commentSubject;
        }

        return "";
    }

    public String getHeaderSnippet() {
        return queryJSON("header[1].text", "");
    }

    /**
     * Compare note timestamp to now and return a time grouping
     */
    public static NoteTimeGroup getTimeGroupForTimestamp(long timestamp) {
        Date today = new Date();
        Date then = new Date(timestamp * 1000);

        if (then.compareTo(DateUtils.addMonths(today, -1)) < 0) {
            return NoteTimeGroup.GROUP_OLDER_MONTH;
        } else if (then.compareTo(DateUtils.addWeeks(today, -1)) < 0) {
            return NoteTimeGroup.GROUP_OLDER_WEEK;
        } else if (then.compareTo(DateUtils.addDays(today, -2)) < 0
                || DateUtils.isSameDay(DateUtils.addDays(today, -2), then)) {
            return NoteTimeGroup.GROUP_OLDER_TWO_DAYS;
        } else if (DateUtils.isSameDay(DateUtils.addDays(today, -1), then)) {
            return NoteTimeGroup.GROUP_YESTERDAY;
        } else {
            return NoteTimeGroup.GROUP_TODAY;
        }
    }

    /**
     * The inverse of isRead
     */
    public Boolean isUnread() {
        return !isRead();
    }

    Boolean isRead() {
        return queryJSON("read", 0) == 1;
    }

    public void markAsRead() {
        try {
            mNoteJSON.put("read", 1);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to update note read property", e);
            return;
        }
        save();
    }

    /**
     * Get the timestamp provided by the API for the note
     */
    public long getTimestamp() {
        return DateTimeUtils.iso8601ToTimestamp(queryJSON("timestamp", ""));
    }

    public JSONArray getBody() {
        try {
            return mNoteJSON.getJSONArray("body");
        } catch (JSONException e) {
            return null;
        }
    }

    // returns character code for notification font
    private String getNoticonCharacter() {
        return queryJSON("noticon", "");
    }

    JSONObject getCommentActions() {
        if (mActions == null) {
            mActions = queryJSON("body[last].actions", new JSONObject());
        }

        return mActions;
    }


    private void updateJSON(JSONObject json) {
        mNoteJSON = json;
    }

    /*
     * returns the actions allowed on this note, assumes it's a comment notification
     */
    public EnumSet<EnabledActions> getEnabledActions() {
        EnumSet<EnabledActions> actions = EnumSet.noneOf(EnabledActions.class);
        JSONObject jsonActions = getCommentActions();
        if (jsonActions == null || jsonActions.length() == 0) {
            return actions;
        }

        if (jsonActions.has(ACTION_KEY_REPLY)) {
            actions.add(EnabledActions.ACTION_REPLY);
        }
        if (jsonActions.has(ACTION_KEY_APPROVE) && jsonActions.optBoolean(ACTION_KEY_APPROVE, false)) {
            actions.add(EnabledActions.ACTION_UNAPPROVE);
        }
        if (jsonActions.has(ACTION_KEY_APPROVE) && !jsonActions.optBoolean(ACTION_KEY_APPROVE, false)) {
            actions.add(EnabledActions.ACTION_APPROVE);
        }
        if (jsonActions.has(ACTION_KEY_SPAM)) {
            actions.add(EnabledActions.ACTION_SPAM);
        }
        if (jsonActions.has(ACTION_KEY_LIKE)) {
            actions.add(EnabledActions.ACTION_LIKE);
        }

        return actions;
    }

    public int getSiteId() {
        return JSONUtil.queryJSON(mNoteJSON, "meta.ids.site", 0);
    }

    public int getPostId() {
        return JSONUtil.queryJSON(mNoteJSON, "meta.ids.post", 0);
    }

    public long getCommentId() {
        return JSONUtil.queryJSON(mNoteJSON, "meta.ids.comment", 0);
    }


    public long getParentCommentId() {
        return JSONUtil.queryJSON(mNoteJSON, "meta.ids.parent_comment", 0);
    }

    /**
     * Rudimentary system for pulling an item out of a JSON object hierarchy
     */
    private <U> U queryJSON(String query, U defaultObject) {
        return JSONUtil.queryJSON(this.toJSONObject(), query, defaultObject);
    }

    /**
     * Constructs a new Comment object based off of data in a Note
     */
    public Comment buildComment() {
        return new Comment(
                getPostId(),
                getCommentId(),
                getCommentAuthorName(),
                DateTimeUtils.timestampToIso8601Str(getTimestamp()),
                getCommentText(),
                CommentStatus.toString(getCommentStatus()),
                "", // post title is unavailable in note model
                getCommentAuthorUrl(),
                "", // user email is unavailable in note model
                getIconURL()
        );
    }

    public String getCommentAuthorName() {
        JSONArray bodyArray = getBody();

        for (int i=0; i < bodyArray.length(); i++) {
            try {
                JSONObject bodyItem = bodyArray.getJSONObject(i);
                if (bodyItem.has("type") && bodyItem.optString("type").equals("user")) {
                    return bodyItem.optString("text");
                }
            } catch (JSONException e) {
                return "";
            }
        }

        return "";
    }

    private String getCommentText() {
        return queryJSON("body[last].text", "");
    }

    private String getCommentAuthorUrl() {
        JSONArray bodyArray = getBody();

        for (int i=0; i < bodyArray.length(); i++) {
            try {
                JSONObject bodyItem = bodyArray.getJSONObject(i);
                if (bodyItem.has("type") && bodyItem.optString("type").equals("user")) {
                    return JSONUtil.queryJSON(bodyItem, "meta.links.home", "");
                }
            } catch (JSONException e) {
                return "";
            }
        }

        return "";
    }

    public CommentStatus getCommentStatus() {
        EnumSet<EnabledActions> enabledActions = getEnabledActions();

        if (enabledActions.contains(EnabledActions.ACTION_UNAPPROVE)) {
            return CommentStatus.APPROVED;
        } else if (enabledActions.contains(EnabledActions.ACTION_APPROVE)) {
            return CommentStatus.UNAPPROVED;
        }

        return CommentStatus.UNKNOWN;
    }

    public boolean hasLikedComment() {
        JSONObject jsonActions = getCommentActions();
        return !(jsonActions == null || jsonActions.length() == 0) && jsonActions.optBoolean(ACTION_KEY_LIKE);
    }

    public JSONArray getHeader() {
        return mNoteJSON.optJSONArray("header");
    }

    /**
     * Represents a user replying to a note.
     */
    public static class Reply {
        private final String mContent;
        private final String mRestPath;

        Reply(String restPath, String content) {
            mRestPath = restPath;
            mContent = content;
        }

        public String getContent() {
            return mContent;
        }

        public String getRestPath() {
            return mRestPath;
        }
    }

    public Reply buildReply(String content) {
        String restPath;
        if (this.isCommentType()) {
            restPath = String.format("sites/%d/comments/%d", getSiteId(), getCommentId());
        } else {
            restPath = String.format("sites/%d/posts/%d", getSiteId(), getPostId());
        }

        return new Reply(String.format("%s/replies/new", restPath), content);
    }

    /**
     * Simperium Schema
     */
    public static class Schema extends BucketSchema<Note> {

        static public final String NAME = "note20";
        static public final String TIMESTAMP_INDEX = "timestamp";
        static public final String SUBJECT_INDEX = "subject";
        static public final String SNIPPET_INDEX = "snippet";
        static public final String UNREAD_INDEX = "unread";
        static public final String NOTICON_INDEX = "noticon";
        static public final String ICON_URL_INDEX = "icon";

        private static final Indexer<Note> sNoteIndexer = new Indexer<Note>() {

            @Override
            public List<Index> index(Note note) {
                List<Index> indexes = new ArrayList<Index>();
                try {
                    indexes.add(new Index(TIMESTAMP_INDEX, note.getTimestamp()));
                } catch (NumberFormatException e) {
                    // note will not have an indexed timestamp so it will
                    // show up at the end of a query sorting by timestamp
                    android.util.Log.e("WordPress", "Failed to index timestamp", e);
                }

                indexes.add(new Index(SUBJECT_INDEX, Html.toHtml(note.getFormattedSubject())));
                indexes.add(new Index(SNIPPET_INDEX, note.getCommentSubject()));
                indexes.add(new Index(UNREAD_INDEX, note.isUnread()));
                indexes.add(new Index(NOTICON_INDEX, note.getNoticonCharacter()));
                indexes.add(new Index(ICON_URL_INDEX, note.getIconURL()));

                return indexes;
            }

        };

        public Schema() {
            addIndex(sNoteIndexer);
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
}