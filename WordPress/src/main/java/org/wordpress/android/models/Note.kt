/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models

import android.text.Spannable
import android.text.TextUtils
import android.util.Base64
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DateUtils.addDays
import org.wordpress.android.util.DateUtils.addMonths
import org.wordpress.android.util.DateUtils.addWeeks
import org.wordpress.android.util.DateUtils.isSameDay
import org.wordpress.android.util.JSONUtils
import org.wordpress.android.util.StringUtils
import java.io.UnsupportedEncodingException
import java.util.Date
import java.util.EnumSet
import java.util.zip.DataFormatException
import java.util.zip.Inflater

class Note {
    private var mActions: JSONObject? = null
    private var mNoteJSON: JSONObject?
    val id: String
    private val mSyncLock = Any()
    private var mLocalStatus: String? = null

    val json: JSONObject
        get() = mNoteJSON ?: JSONObject()

    enum class EnabledActions {
        ACTION_REPLY,
        ACTION_APPROVE,
        ACTION_UNAPPROVE,
        ACTION_SPAM,
        ACTION_LIKE_COMMENT,
        ACTION_LIKE_POST
    }

    enum class NoteTimeGroup {
        GROUP_TODAY,
        GROUP_YESTERDAY,
        GROUP_OLDER_TWO_DAYS,
        GROUP_OLDER_WEEK,
        GROUP_OLDER_MONTH
    }

    constructor(key: String, noteJSON: JSONObject?) {
        id = key
        mNoteJSON = noteJSON
    }

    constructor(noteJSON: JSONObject?) {
        mNoteJSON = noteJSON
        id = mNoteJSON!!.optString("id", "")
    }

    val jSON: JSONObject
        get() = if (mNoteJSON != null) mNoteJSON!! else JSONObject()
    val rawType: String
        get() = queryJSON("type", NOTE_UNKNOWN_TYPE)

    private fun isTypeRaw(rawType: String): Boolean {
        return this.rawType == rawType
    }

    val isCommentType: Boolean
        get() {
            synchronized(mSyncLock) {
                return (isAutomattcherType && JSONUtils.queryJSON(
                    mNoteJSON,
                    "meta.ids.comment",
                    -1
                ) != -1
                        || isTypeRaw(NOTE_COMMENT_TYPE))
            }
        }
    val isAutomattcherType: Boolean
        get() = isTypeRaw(NOTE_MATCHER_TYPE)
    val isNewPostType: Boolean
        get() = isTypeRaw(NOTE_NEW_POST_TYPE)
    val isFollowType: Boolean
        get() = isTypeRaw(NOTE_FOLLOW_TYPE)
    val isLikeType: Boolean
        get() = isPostLikeType || isCommentLikeType
    val isPostLikeType: Boolean
        get() = isTypeRaw(NOTE_LIKE_TYPE)
    val isCommentLikeType: Boolean
        get() = isTypeRaw(NOTE_COMMENT_LIKE_TYPE)
    val isReblogType: Boolean
        get() = isTypeRaw(NOTE_REBLOG_TYPE)
    val isViewMilestoneType: Boolean
        get() = isTypeRaw(NOTE_VIEW_MILESTONE)
    val isCommentReplyType: Boolean
        get() = isCommentType && parentCommentId > 0
    val isCommentWithUserReply: Boolean
        // Returns true if the user has replied to this comment note
        get() = isCommentType && !TextUtils.isEmpty(commentSubjectNoticon)
    val isUserList: Boolean
        get() = isLikeType || isFollowType || isReblogType

    /*
     * does user have permission to moderate/reply/spam this comment?
     */
    fun canModerate() = enabledCommentActions.contains(EnabledActions.ACTION_APPROVE) ||
            enabledCommentActions.contains(EnabledActions.ACTION_UNAPPROVE)

    fun canReply() = enabledCommentActions.contains(EnabledActions.ACTION_REPLY)

    fun canLikeComment() = enabledCommentActions.contains(EnabledActions.ACTION_LIKE_COMMENT)

    fun canLikePost() = enabledPostActions.contains(EnabledActions.ACTION_LIKE_POST)

    var localStatus: String?
        get() = StringUtils.notNullStr(mLocalStatus)
        set(localStatus) {
            mLocalStatus = localStatus
        }
    val subject: JSONObject?
        get() {
            try {
                synchronized(mSyncLock) {
                    val subjectArray = mNoteJSON!!.getJSONArray("subject")
                    if (subjectArray.length() > 0) {
                        return subjectArray.getJSONObject(0)
                    }
                }
            } catch (e: JSONException) {
                return null
            }
            return null
        }

    fun getFormattedSubject(notificationsUtilsWrapper: NotificationsUtilsWrapper): Spannable {
        return notificationsUtilsWrapper.getSpannableContentForRanges(subject!!)
    }

    val title: String
        get() = queryJSON("title", "")
    val iconURL: String
        get() = queryJSON("icon", "")
    val iconURLs: List<String>?
        get() {
            synchronized(mSyncLock) {
                val bodyArray = mNoteJSON!!.optJSONArray("body")
                if (bodyArray != null && bodyArray.length() > 0) {
                    val iconUrls = ArrayList<String>()
                    for (i in 0 until bodyArray.length()) {
                        val iconUrl = JSONUtils.queryJSON(bodyArray, "body[$i].media[0].url", "")
                        if (iconUrl != null && !iconUrl.isEmpty()) {
                            iconUrls.add(iconUrl)
                        }
                    }
                    return iconUrls
                }
            }
            return null
        }
    val commentSubject: String?
        get() {
            synchronized(mSyncLock) {
                val subjectArray = mNoteJSON!!.optJSONArray("subject")
                if (subjectArray != null) {
                    var commentSubject = JSONUtils.queryJSON(subjectArray, "subject[1].text", "")

                    // Trim down the comment preview if the comment text is too large.
                    if (commentSubject != null && commentSubject.length > MAX_COMMENT_PREVIEW_LENGTH) {
                        commentSubject = commentSubject.substring(0, MAX_COMMENT_PREVIEW_LENGTH - 1)
                    }
                    return commentSubject
                }
            }
            return ""
        }
    val commentSubjectNoticon: String
        get() {
            with(queryJSON("subject[0].ranges", JSONArray())) {
                for (i in 0 until length()) {
                    try {
                        val rangeItem = getJSONObject(i)
                        if (rangeItem.has("type") && rangeItem.optString("type") == "noticon") {
                            return rangeItem.optString("value", "")
                        }
                    } catch (e: JSONException) {
                        return ""
                    }
                }
            }
            return ""
        }
    val commentReplyId: Long
        get() = queryJSON("meta.ids.reply_comment", 0).toLong()
    val isUnread: Boolean
        /**
         * The inverse of isRead
         */
        get() = !isRead
    private val isRead: Boolean
        get() = queryJSON("read", 0) == 1

    fun setRead() {
        try {
            mNoteJSON!!.putOpt("read", 1)
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.NOTIFS, "Failed to set 'read' property", e)
        }
    }

    val timestamp: Long
        /**
         * Get the timestamp provided by the API for the note
         */
        get() = DateTimeUtils.timestampFromIso8601(timestampString)
    val timestampString: String
        get() = queryJSON("timestamp", "")
    val body: JSONArray
        get() {
            try {
                synchronized(mSyncLock) { return mNoteJSON!!.getJSONArray("body") }
            } catch (e: JSONException) {
                return JSONArray()
            }
        }

    private val commentActions: JSONObject
        get() {
            if (mActions == null) {
                // Find comment block that matches the root note comment id
                val commentId = commentId
                val bodyArray = body
                for (i in 0 until bodyArray.length()) {
                    try {
                        val bodyItem = bodyArray.getJSONObject(i)
                        if (bodyItem.has("type") && bodyItem.optString("type") == "comment" && commentId == JSONUtils.queryJSON(
                                bodyItem,
                                "meta.ids.comment",
                                0
                            ).toLong()
                        ) {
                            mActions = JSONUtils.queryJSON(bodyItem, "actions", JSONObject())
                            break
                        }
                    } catch (e: JSONException) {
                        break
                    }
                }
                if (mActions == null) {
                    mActions = JSONObject()
                }
            }
            return mActions!!
        }
    private val postActions: JSONObject
        get() {
            if (mActions == null) {
                val bodyArray = body
                for (i in 0 until bodyArray.length()) {
                    try {
                        val bodyItem = bodyArray.getJSONObject(i)
                        if (bodyItem.has("type") && bodyItem.optString("type") == "post" && postId == JSONUtils.queryJSON(
                                bodyItem,
                                "meta.ids.post",
                                0
                            )
                        ) {
                            mActions = JSONUtils.queryJSON(bodyItem, "actions", JSONObject())
                            break
                        }
                    } catch (e: JSONException) {
                        break
                    }
                }
                if (mActions == null) {
                    mActions = JSONObject()
                }
            }
            return mActions!!
        }
    val enabledCommentActions: EnumSet<EnabledActions>
        /**
         * returns the actions allowed on this note, assumes it's a comment notification
         */
        get() = getEnabledActions(commentActions)
    val enabledPostActions: EnumSet<EnabledActions>
        /**
         * returns the actions allowed on this note, assumes it's a post notification
         */
        get() = getEnabledActions(postActions)

    private fun getEnabledActions(jsonActions: JSONObject): EnumSet<EnabledActions> {
        val actions = EnumSet.noneOf(
            EnabledActions::class.java
        )
        if (jsonActions.length() == 0) {
            return actions
        }
        if (jsonActions.has(ACTION_KEY_REPLY)) {
            actions.add(EnabledActions.ACTION_REPLY)
        }
        if (jsonActions.has(ACTION_KEY_APPROVE) && jsonActions.optBoolean(
                ACTION_KEY_APPROVE,
                false
            )
        ) {
            actions.add(EnabledActions.ACTION_UNAPPROVE)
        }
        if (jsonActions.has(ACTION_KEY_APPROVE) && !jsonActions.optBoolean(
                ACTION_KEY_APPROVE,
                false
            )
        ) {
            actions.add(EnabledActions.ACTION_APPROVE)
        }
        if (jsonActions.has(ACTION_KEY_SPAM)) {
            actions.add(EnabledActions.ACTION_SPAM)
        }
        if (jsonActions.has(ACTION_KEY_LIKE_COMMENT)) {
            actions.add(EnabledActions.ACTION_LIKE_COMMENT)
        }
        if (jsonActions.has(ACTION_KEY_LIKE_POST)) {
            actions.add(EnabledActions.ACTION_LIKE_POST)
        }
        return actions
    }

    val siteId: Int
        get() = queryJSON("meta.ids.site", 0)
    val postId: Int
        get() = queryJSON("meta.ids.post", 0)
    val commentId: Long
        get() = queryJSON("meta.ids.comment", 0).toLong()
    val parentCommentId: Long
        get() = queryJSON("meta.ids.parent_comment", 0).toLong()

    /**
     * Rudimentary system for pulling an item out of a JSON object hierarchy
     */
    private fun <U> queryJSON(query: String?, defaultObject: U): U {
        synchronized(mSyncLock) {
            return if (mNoteJSON == null) {
                defaultObject
            } else JSONUtils.queryJSON(mNoteJSON, query, defaultObject)
        }
    }

    /**
     * Constructs a new Comment object based off of data in a Note
     */
    fun buildComment(): CommentModel {
        val comment = CommentModel()
        comment.remotePostId = postId.toLong()
        comment.remoteCommentId = commentId
        comment.authorName = commentAuthorName
        comment.datePublished = DateTimeUtils.iso8601FromTimestamp(timestamp)
        comment.content = commentText
        comment.status = commentStatus.toString()
        comment.authorUrl = commentAuthorUrl
        comment.postTitle = title // unavailable in note model
        comment.authorEmail = "" // unavailable in note model
        comment.authorProfileImageUrl = iconURL
        comment.iLike = hasLikedComment()
        return comment
    }

    val commentAuthorName: String
        get() {
            val bodyArray = body
            for (i in 0 until bodyArray.length()) {
                try {
                    val bodyItem = bodyArray.getJSONObject(i)
                    if (bodyItem.has("type") && bodyItem.optString("type") == "user") {
                        return bodyItem.optString("text")
                    }
                } catch (e: JSONException) {
                    return ""
                }
            }
            return ""
        }
    private val commentText: String
        get() = queryJSON("body[last].text", "")
    private val commentAuthorUrl: String
        get() {
            val bodyArray = body
            for (i in 0 until bodyArray.length()) {
                try {
                    val bodyItem = bodyArray.getJSONObject(i)
                    if (bodyItem.has("type") && bodyItem.optString("type") == "user") {
                        return JSONUtils.queryJSON(bodyItem, "meta.links.home", "")
                    }
                } catch (e: JSONException) {
                    return ""
                }
            }
            return ""
        }
    val commentStatus: CommentStatus
        get() {
            val enabledActions = enabledCommentActions
            if (enabledActions.contains(EnabledActions.ACTION_UNAPPROVE)) {
                return CommentStatus.APPROVED
            } else if (enabledActions.contains(EnabledActions.ACTION_APPROVE)) {
                return CommentStatus.UNAPPROVED
            }
            return CommentStatus.ALL
        }

    fun hasLikedComment() = commentActions.length() > 0 && commentActions.optBoolean(ACTION_KEY_LIKE_COMMENT)

    fun hasLikedPost() = postActions.length() > 0 && postActions.optBoolean(ACTION_KEY_LIKE_POST)

    fun setLikedComment(liked: Boolean) {
        try {
            commentActions.put(ACTION_KEY_LIKE_COMMENT, liked)
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.NOTIFS, "Failed to set 'like' property for the note", e)
        }
    }

    fun setLikedPost(liked: Boolean) {
        try {
            postActions.put(ACTION_KEY_LIKE_POST, liked)
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.NOTIFS, "Failed to set 'like' property for the note", e)
        }
    }

    val url: String
        get() = queryJSON("url", "")
    val header: JSONArray?
        get() {
            synchronized(mSyncLock) { return mNoteJSON?.optJSONArray("header") }
        }

    // this method is used to compare two Notes: as it's potentially a very processing intensive operation,
    // we're only comparing the note id, timestamp, and raw JSON length, which is accurate enough for
    // the purpose of checking if the local Note is any different from a remote note.
    fun equalsTimeAndLength(note: Note?) = note != null &&
            (timestampString.equals(note.timestampString, ignoreCase = true) &&
                    jSON.length() == note.jSON.length())

    companion object {
        private const val TAG = "NoteModel"

        // Maximum character length for a comment preview
        private const val MAX_COMMENT_PREVIEW_LENGTH = 200

        // Note types
        const val NOTE_FOLLOW_TYPE = "follow"
        const val NOTE_LIKE_TYPE = "like"
        const val NOTE_COMMENT_TYPE = "comment"
        const val NOTE_MATCHER_TYPE = "automattcher"
        const val NOTE_COMMENT_LIKE_TYPE = "comment_like"
        const val NOTE_REBLOG_TYPE = "reblog"
        const val NOTE_NEW_POST_TYPE = "new_post"
        const val NOTE_VIEW_MILESTONE = "view_milestone"
        const val NOTE_UNKNOWN_TYPE = "unknown"

        // JSON action keys
        private const val ACTION_KEY_REPLY = "replyto-comment"
        private const val ACTION_KEY_APPROVE = "approve-comment"
        private const val ACTION_KEY_SPAM = "spam-comment"
        private const val ACTION_KEY_LIKE_COMMENT = "like-comment"
        private const val ACTION_KEY_LIKE_POST = "like-post"

        /**
         * Compare note timestamp to now and return a time grouping
         */
        fun getTimeGroupForTimestamp(timestamp: Long): NoteTimeGroup {
            val today = Date()
            val then = Date(timestamp * 1000)
            return if (then.compareTo(addMonths(today, -1)) < 0) {
                NoteTimeGroup.GROUP_OLDER_MONTH
            } else if (then.compareTo(addWeeks(today, -1)) < 0) {
                NoteTimeGroup.GROUP_OLDER_WEEK
            } else if (then.compareTo(addDays(today, -2)) < 0
                || isSameDay(
                    addDays(
                        today,
                        -2
                    ), then
                )
            ) {
                NoteTimeGroup.GROUP_OLDER_TWO_DAYS
            } else if (isSameDay(
                    addDays(
                        today,
                        -1
                    ), then
                )
            ) {
                NoteTimeGroup.GROUP_YESTERDAY
            } else {
                NoteTimeGroup.GROUP_TODAY
            }
        }

        @JvmStatic
        @Synchronized
        fun buildFromBase64EncodedData(noteId: String, base64FullNoteData: String?): Note? {
            var note: Note? = null
            if (base64FullNoteData == null) {
                return null
            }
            val b64DecodedPayload = Base64.decode(base64FullNoteData, Base64.DEFAULT)

            // Decompress the payload
            val decompresser = Inflater()
            decompresser.setInput(b64DecodedPayload, 0, b64DecodedPayload.size)
            val result = ByteArray(4096) // max length an Android PN payload can have
            var resultLength = 0
            try {
                resultLength = decompresser.inflate(result)
                decompresser.end()
            } catch (e: DataFormatException) {
                AppLog.e(
                    AppLog.T.NOTIFS,
                    "Can't decompress the PN BlockListPayload. It could be > 4K",
                    e
                )
            }
            var out: String? = null
            try {
                out = String(result, 0, resultLength, charset("UTF8"))
            } catch (e: UnsupportedEncodingException) {
                AppLog.e(AppLog.T.NOTIFS, "Notification data contains non UTF8 characters.", e)
            }
            if (out != null) {
                try {
                    var jsonObject = JSONObject(out)
                    if (jsonObject.has("notes")) {
                        val jsonArray : JSONArray? = jsonObject.getJSONArray("notes")
                        if (jsonArray != null && jsonArray.length() == 1) {
                            jsonObject = jsonArray.getJSONObject(0)
                        }
                    }
                    note = Note(noteId, jsonObject)
                } catch (e: JSONException) {
                    AppLog.e(AppLog.T.NOTIFS, "Can't parse the Note JSON received in the PN", e)
                }
            }
            return note
        }
    }
}
