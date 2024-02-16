/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models;

import android.text.Spannable;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.fluxc.model.CommentModel;
import org.wordpress.android.fluxc.model.CommentStatus;
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.DateUtils;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Note {
    private static final String TAG = "NoteModel";

    // Maximum character length for a comment preview
    private static final int MAX_COMMENT_PREVIEW_LENGTH = 200;

    // Note types
    public static final String NOTE_FOLLOW_TYPE = "follow";
    public static final String NOTE_LIKE_TYPE = "like";
    public static final String NOTE_COMMENT_TYPE = "comment";
    public static final String NOTE_MATCHER_TYPE = "automattcher";
    public static final String NOTE_COMMENT_LIKE_TYPE = "comment_like";
    public static final String NOTE_REBLOG_TYPE = "reblog";
    public static final String NOTE_NEW_POST_TYPE = "new_post";
    public static final String NOTE_VIEW_MILESTONE = "view_milestone";
    public static final String NOTE_UNKNOWN_TYPE = "unknown";

    // JSON action keys
    private static final String ACTION_KEY_REPLY = "replyto-comment";
    private static final String ACTION_KEY_APPROVE = "approve-comment";
    private static final String ACTION_KEY_SPAM = "spam-comment";
    private static final String ACTION_KEY_LIKE_COMMENT = "like-comment";
    private static final String ACTION_KEY_LIKE_POST = "like-post";

    private JSONObject mActions;
    private JSONObject mNoteJSON;
    private final String mKey;

    private final Object mSyncLock = new Object();
    private String mLocalStatus;

    public enum EnabledActions {
        ACTION_REPLY,
        ACTION_APPROVE,
        ACTION_UNAPPROVE,
        ACTION_SPAM,
        ACTION_LIKE_COMMENT,
        ACTION_LIKE_POST,
    }

    public enum NoteTimeGroup {
        GROUP_TODAY,
        GROUP_YESTERDAY,
        GROUP_OLDER_TWO_DAYS,
        GROUP_OLDER_WEEK,
        GROUP_OLDER_MONTH
    }

    public Note(String key, JSONObject noteJSON) {
        mKey = key;
        mNoteJSON = noteJSON;
    }

    public Note(JSONObject noteJSON) {
        mNoteJSON = noteJSON;
        mKey = mNoteJSON.optString("id", "");
    }

    public JSONObject getJSON() {
        return mNoteJSON != null ? mNoteJSON : new JSONObject();
    }

    public String getId() {
        return mKey;
    }

    @NonNull
    public String getRawType() {
        return queryJSON("type", NOTE_UNKNOWN_TYPE);
    }

    @NonNull
    private Boolean isTypeRaw(@NonNull String rawType) {
        return getRawType().equals(rawType);
    }

    @NonNull
    public Boolean isCommentType() {
        synchronized (mSyncLock) {
            return (isAutomattcherType() && JSONUtils.queryJSON(mNoteJSON, "meta.ids.comment", -1) != -1)
                   || isTypeRaw(NOTE_COMMENT_TYPE);
        }
    }

    @NonNull
    public Boolean isAutomattcherType() {
        return isTypeRaw(NOTE_MATCHER_TYPE);
    }

    @NonNull
    public Boolean isNewPostType() {
        return isTypeRaw(NOTE_NEW_POST_TYPE);
    }

    @NonNull
    public Boolean isFollowType() {
        return isTypeRaw(NOTE_FOLLOW_TYPE);
    }

    @NonNull
    public Boolean isLikeType() {
        return isPostLikeType() || isCommentLikeType();
    }

    @NonNull
    public Boolean isPostLikeType() {
        return isTypeRaw(NOTE_LIKE_TYPE);
    }

    @NonNull
    public Boolean isCommentLikeType() {
        return isTypeRaw(NOTE_COMMENT_LIKE_TYPE);
    }

    @NonNull
    public Boolean isReblogType() {
        return isTypeRaw(NOTE_REBLOG_TYPE);
    }

    @NonNull
    public Boolean isViewMilestoneType() {
        return isTypeRaw(NOTE_VIEW_MILESTONE);
    }

    @NonNull
    public Boolean isCommentReplyType() {
        return isCommentType() && getParentCommentId() > 0;
    }

    // Returns true if the user has replied to this comment note
    @NonNull
    public Boolean isCommentWithUserReply() {
        return isCommentType() && !TextUtils.isEmpty(getCommentSubjectNoticon());
    }

    @NonNull
    public Boolean isUserList() {
        return isLikeType() || isFollowType() || isReblogType();
    }

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    public boolean canModerate() {
        EnumSet<EnabledActions> enabledActions = getEnabledCommentActions();
        return enabledActions != null && (enabledActions.contains(EnabledActions.ACTION_APPROVE) || enabledActions
                .contains(EnabledActions.ACTION_UNAPPROVE));
    }

    public boolean canMarkAsSpam() {
        EnumSet<EnabledActions> enabledActions = getEnabledCommentActions();
        return (enabledActions != null && enabledActions.contains(EnabledActions.ACTION_SPAM));
    }

    public boolean canReply() {
        EnumSet<EnabledActions> enabledActions = getEnabledCommentActions();
        return (enabledActions != null && enabledActions.contains(EnabledActions.ACTION_REPLY));
    }

    public boolean canTrash() {
        return canModerate();
    }

    public boolean canLikeComment() {
        EnumSet<EnabledActions> enabledActions = getEnabledCommentActions();
        return (enabledActions != null && enabledActions.contains(EnabledActions.ACTION_LIKE_COMMENT));
    }

    public boolean canLikePost() {
        EnumSet<EnabledActions> enabledActions = getEnabledPostActions();
        return (enabledActions != null && enabledActions.contains(EnabledActions.ACTION_LIKE_POST));
    }

    public String getLocalStatus() {
        return StringUtils.notNullStr(mLocalStatus);
    }

    public void setLocalStatus(String localStatus) {
        mLocalStatus = localStatus;
    }

    public JSONObject getSubject() {
        try {
            synchronized (mSyncLock) {
                JSONArray subjectArray = mNoteJSON.getJSONArray("subject");
                if (subjectArray.length() > 0) {
                    return subjectArray.getJSONObject(0);
                }
            }
        } catch (JSONException e) {
            return null;
        }

        return null;
    }

    public Spannable getFormattedSubject(NotificationsUtilsWrapper notificationsUtilsWrapper) {
        return notificationsUtilsWrapper.getSpannableContentForRanges(getSubject());
    }

    public String getTitle() {
        return queryJSON("title", "");
    }

    public String getIconURL() {
        return queryJSON("icon", "");
    }

    public @Nullable List<String> getIconURLs() {
        synchronized (mSyncLock) {
            JSONArray bodyArray = mNoteJSON.optJSONArray("body");
            if (bodyArray != null && bodyArray.length() > 0) {
                ArrayList<String> iconUrls = new ArrayList<>();
                for (int i = 0; i < bodyArray.length(); i++) {
                    String iconUrl = JSONUtils.queryJSON(bodyArray, "body[" + i + "].media[0].url", "");
                    if (iconUrl != null && !iconUrl.isEmpty()) {
                        iconUrls.add(iconUrl);
                    }
                }
                return iconUrls;
            }
        }
        return null;
    }

    public String getCommentSubject() {
        synchronized (mSyncLock) {
            JSONArray subjectArray = mNoteJSON.optJSONArray("subject");
            if (subjectArray != null) {
                String commentSubject = JSONUtils.queryJSON(subjectArray, "subject[1].text", "");

                // Trim down the comment preview if the comment text is too large.
                if (commentSubject != null && commentSubject.length() > MAX_COMMENT_PREVIEW_LENGTH) {
                    commentSubject = commentSubject.substring(0, MAX_COMMENT_PREVIEW_LENGTH - 1);
                }

                return commentSubject;
            }
        }

        return "";
    }

    public String getCommentSubjectNoticon() {
        JSONArray subjectRanges = queryJSON("subject[0].ranges", new JSONArray());
        if (subjectRanges != null) {
            for (int i = 0; i < subjectRanges.length(); i++) {
                try {
                    JSONObject rangeItem = subjectRanges.getJSONObject(i);
                    if (rangeItem.has("type") && rangeItem.optString("type").equals("noticon")) {
                        return rangeItem.optString("value", "");
                    }
                } catch (JSONException e) {
                    return "";
                }
            }
        }

        return "";
    }

    public long getCommentReplyId() {
        return queryJSON("meta.ids.reply_comment", 0);
    }

    /**
     * Compare note timestamp to now and return a time grouping
     */
    @NonNull
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

    private Boolean isRead() {
        return queryJSON("read", 0) == 1;
    }

    public void setRead() {
        try {
            mNoteJSON.putOpt("read", 1);
        } catch (JSONException e) {
            AppLog.e(AppLog.T.NOTIFS, "Failed to set 'read' property", e);
        }
    }

    /**
     * Get the timestamp provided by the API for the note
     */
    public long getTimestamp() {
        return DateTimeUtils.timestampFromIso8601(getTimestampString());
    }

    public String getTimestampString() {
        return queryJSON("timestamp", "");
    }

    public JSONArray getBody() {
        try {
            synchronized (mSyncLock) {
                return mNoteJSON.getJSONArray("body");
            }
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    // returns character code for notification font
    public String getNoticonCharacter() {
        return queryJSON("noticon", "");
    }

    private JSONObject getCommentActions() {
        if (mActions == null) {
            // Find comment block that matches the root note comment id
            long commentId = getCommentId();
            JSONArray bodyArray = getBody();
            for (int i = 0; i < bodyArray.length(); i++) {
                try {
                    JSONObject bodyItem = bodyArray.getJSONObject(i);
                    if (bodyItem.has("type") && bodyItem.optString("type").equals("comment")
                        && commentId == JSONUtils.queryJSON(bodyItem, "meta.ids.comment", 0)) {
                        mActions = JSONUtils.queryJSON(bodyItem, "actions", new JSONObject());
                        break;
                    }
                } catch (JSONException e) {
                    break;
                }
            }

            if (mActions == null) {
                mActions = new JSONObject();
            }
        }

        return mActions;
    }

    private JSONObject getPostActions() {
        if (mActions == null) {
            // Find comment block that matches the root note comment id
            long commentId = getPostId();
            JSONArray bodyArray = getBody();
            for (int i = 0; i < bodyArray.length(); i++) {
                try {
                    JSONObject bodyItem = bodyArray.getJSONObject(i);
                    if (bodyItem.has("type") && bodyItem.optString("type").equals("post")
                        && commentId == JSONUtils.queryJSON(bodyItem, "meta.ids.post", 0)) {
                        mActions = JSONUtils.queryJSON(bodyItem, "actions", new JSONObject());
                        break;
                    }
                } catch (JSONException e) {
                    break;
                }
            }

            if (mActions == null) {
                mActions = new JSONObject();
            }
        }

        return mActions;
    }

    /*
     * returns the actions allowed on this note, assumes it's a comment notification
     */
    public EnumSet<EnabledActions> getEnabledCommentActions() {
        return getEnabledActions(getCommentActions());
    }

    /*
     * returns the actions allowed on this note, assumes it's a post notification
     */
    public EnumSet<EnabledActions> getEnabledPostActions() {
        return getEnabledActions(getPostActions());
    }

    private EnumSet<EnabledActions> getEnabledActions(final JSONObject jsonActions) {
        EnumSet<EnabledActions> actions = EnumSet.noneOf(EnabledActions.class);
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
        if (jsonActions.has(ACTION_KEY_LIKE_COMMENT)) {
            actions.add(EnabledActions.ACTION_LIKE_COMMENT);
        }
        if (jsonActions.has(ACTION_KEY_LIKE_POST)) {
            actions.add(EnabledActions.ACTION_LIKE_POST);
        }
        return actions;
    }

    public int getSiteId() {
        return queryJSON("meta.ids.site", 0);
    }

    public int getPostId() {
        return queryJSON("meta.ids.post", 0);
    }

    public long getCommentId() {
        return queryJSON("meta.ids.comment", 0);
    }

    public long getParentCommentId() {
        return queryJSON("meta.ids.parent_comment", 0);
    }

    /**
     * Rudimentary system for pulling an item out of a JSON object hierarchy
     */
    @NonNull
    private <U> U queryJSON(@Nullable String query, @NonNull U defaultObject) {
        synchronized (mSyncLock) {
            if (mNoteJSON == null) {
                return defaultObject;
            }
            return JSONUtils.queryJSON(mNoteJSON, query, defaultObject);
        }
    }

    /**
     * Constructs a new Comment object based off of data in a Note
     */
    @NonNull
    public CommentModel buildComment() {
        CommentModel comment = new CommentModel();
        comment.setRemotePostId(getPostId());
        comment.setRemoteCommentId(getCommentId());
        comment.setAuthorName(getCommentAuthorName());
        comment.setDatePublished(DateTimeUtils.iso8601FromTimestamp(getTimestamp()));
        comment.setContent(getCommentText());
        comment.setStatus(getCommentStatus().toString());
        comment.setAuthorUrl(getCommentAuthorUrl());
        comment.setPostTitle(getTitle()); // unavailable in note model
        comment.setAuthorEmail(""); // unavailable in note model
        comment.setAuthorProfileImageUrl(getIconURL());
        comment.setILike(hasLikedComment());
        return comment;
    }

    public String getCommentAuthorName() {
        JSONArray bodyArray = getBody();

        for (int i = 0; i < bodyArray.length(); i++) {
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

        for (int i = 0; i < bodyArray.length(); i++) {
            try {
                JSONObject bodyItem = bodyArray.getJSONObject(i);
                if (bodyItem.has("type") && bodyItem.optString("type").equals("user")) {
                    return JSONUtils.queryJSON(bodyItem, "meta.links.home", "");
                }
            } catch (JSONException e) {
                return "";
            }
        }

        return "";
    }

    public CommentStatus getCommentStatus() {
        EnumSet<EnabledActions> enabledActions = getEnabledCommentActions();

        if (enabledActions.contains(EnabledActions.ACTION_UNAPPROVE)) {
            return CommentStatus.APPROVED;
        } else if (enabledActions.contains(EnabledActions.ACTION_APPROVE)) {
            return CommentStatus.UNAPPROVED;
        }

        return CommentStatus.ALL;
    }

    public boolean hasLikedComment() {
        JSONObject jsonActions = getCommentActions();
        return !(jsonActions == null || jsonActions.length() == 0) && jsonActions.optBoolean(ACTION_KEY_LIKE_COMMENT);
    }

    public boolean hasLikedPost() {
        JSONObject jsonActions = getPostActions();
        return !(jsonActions == null || jsonActions.length() == 0) && jsonActions.optBoolean(ACTION_KEY_LIKE_POST);
    }

    public void setLikedComment(boolean liked) {
        JSONObject jsonActions = getCommentActions();
        if (jsonActions != null) {
            try {
                jsonActions.put(ACTION_KEY_LIKE_COMMENT, liked);
            } catch (JSONException e) {
                AppLog.e(T.NOTIFS, "Failed to set 'like' property for the note", e);
            }
        }
    }

    public void setLikedPost(boolean liked) {
        JSONObject jsonActions = getPostActions();
        if (jsonActions != null) {
            try {
                jsonActions.put(ACTION_KEY_LIKE_POST, liked);
            } catch (JSONException e) {
                AppLog.e(T.NOTIFS, "Failed to set 'like' property for the note", e);
            }
        }
    }

    public String getUrl() {
        return queryJSON("url", "");
    }

    public JSONArray getHeader() {
        synchronized (mSyncLock) {
            return mNoteJSON.optJSONArray("header");
        }
    }

    // this method is used to compare two Notes: as it's potentially a very processing intensive operation,
    // we're only comparing the note id, timestamp, and raw JSON length, which is accurate enough for
    // the purpose of checking if the local Note is any different from a remote note.
    public boolean equalsTimeAndLength(Note note) {
        if (note == null) {
            return false;
        }

        if (this.getTimestampString().equalsIgnoreCase(note.getTimestampString())
            && this.getJSON().length() == note.getJSON().length()) {
            return true;
        }
        return false;
    }

    public static synchronized Note buildFromBase64EncodedData(String noteId, String base64FullNoteData) {
        Note note = null;

        if (base64FullNoteData == null) {
            return null;
        }

        byte[] b64DecodedPayload = Base64.decode(base64FullNoteData, Base64.DEFAULT);

        // Decompress the payload
        Inflater decompresser = new Inflater();
        decompresser.setInput(b64DecodedPayload, 0, b64DecodedPayload.length);
        byte[] result = new byte[4096]; // max length an Android PN payload can have
        int resultLength = 0;
        try {
            resultLength = decompresser.inflate(result);
            decompresser.end();
        } catch (DataFormatException e) {
            AppLog.e(AppLog.T.NOTIFS, "Can't decompress the PN BlockListPayload. It could be > 4K", e);
        }

        String out = null;
        try {
            out = new String(result, 0, resultLength, "UTF8");
        } catch (UnsupportedEncodingException e) {
            AppLog.e(AppLog.T.NOTIFS, "Notification data contains non UTF8 characters.", e);
        }

        if (out != null) {
            try {
                JSONObject jsonObject = new JSONObject(out);
                if (jsonObject.has("notes")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("notes");
                    if (jsonArray != null && jsonArray.length() == 1) {
                        jsonObject = jsonArray.getJSONObject(0);
                    }
                }
                note = new Note(noteId, jsonObject);
            } catch (JSONException e) {
                AppLog.e(AppLog.T.NOTIFS, "Can't parse the Note JSON received in the PN", e);
            }
        }

        return note;
    }
}
