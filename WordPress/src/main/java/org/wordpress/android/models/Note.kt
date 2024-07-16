/**
 * Note represents a single WordPress.com notification
 */
package org.wordpress.android.models

import android.text.Spannable
import android.text.SpannableString
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
import java.util.zip.ZipException

class Note {
    val id: String
    var localStatus: String? = null
        get() = StringUtils.notNullStr(field)

    private var mNoteJSON: JSONObject? = null

    constructor(key: String, noteJSON: JSONObject?) {
        id = key
        mNoteJSON = noteJSON
    }

    constructor(noteJSON: JSONObject?) {
        mNoteJSON = noteJSON
        id = mNoteJSON?.optString("id", "") ?: ""
    }

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

    /**
     * Immutable lazily initialised properties from the note JSON
     */

    val siteId: Int by lazy { queryJSON("meta.ids.site", 0) }
    val postId: Int by lazy { queryJSON("meta.ids.post", 0) }
    val rawType: String by lazy { queryJSON("type", NOTE_UNKNOWN_TYPE) }
    val commentId: Long by lazy { queryJSON("meta.ids.comment", 0).toLong() }
    val parentCommentId: Long by lazy { queryJSON("meta.ids.parent_comment", 0).toLong() }
    val url: String by lazy { queryJSON("url", "") }
    val header: JSONArray? by lazy { mNoteJSON?.optJSONArray("header") }
    val commentReplyId: Long by lazy { queryJSON("meta.ids.reply_comment", 0).toLong() }
    val title: String by lazy { queryJSON("title", "") }
    val iconURL: String by lazy { queryJSON("icon", "") }
    val enabledCommentActions: EnumSet<EnabledActions> by lazy { getEnabledActions(commentActions) }
    private val enabledPostActions: EnumSet<EnabledActions> by lazy { getEnabledActions(postActions) }
    private val timestampString: String by lazy { queryJSON("timestamp", "") }
    private val commentText: String by lazy { queryJSON("body[last].text", "") }
    private val commentActions: JSONObject by lazy { getActions(commentId, "comment") }
    private val postActions: JSONObject by lazy { getActions(postId.toLong(), "post") }

    val body: JSONArray by lazy {
        runCatching {
            mNoteJSON?.getJSONArray("body") ?: JSONArray()
        }.getOrElse {
            JSONArray()
        }
    }

    val subject: JSONObject? by lazy {
        runCatching {
            val subjectArray = mNoteJSON?.getJSONArray("subject")
            if (subjectArray != null && subjectArray.length() > 0) {
                subjectArray.getJSONObject(0)
            } else null
        }.getOrElse {
            null
        }
    }

    val iconURLs: List<String>? by lazy {
        val bodyArray = mNoteJSON?.optJSONArray("body")
        if (bodyArray != null && bodyArray.length() > 0) {
            val iconUrls = ArrayList<String>()
            for (i in 0 until bodyArray.length()) {
                val iconUrl = JSONUtils.queryJSON(bodyArray, "body[$i].media[0].url", "")
                if (iconUrl != null && iconUrl.isNotEmpty()) {
                    iconUrls.add(iconUrl)
                }
            }
            return@lazy iconUrls
        }
        null
    }

    val commentSubject: String? by lazy {
        val subjectArray = mNoteJSON?.optJSONArray("subject")
        if (subjectArray != null) {
            var commentSubject = JSONUtils.queryJSON(subjectArray, "subject[1].text", "")

            // Trim down the comment preview if the comment text is too large.
            if (commentSubject != null && commentSubject.length > MAX_COMMENT_PREVIEW_LENGTH) {
                commentSubject = commentSubject.substring(0, MAX_COMMENT_PREVIEW_LENGTH - 1)
            }
            return@lazy commentSubject
        }
        ""
    }

    val commentSubjectNoticon: String by lazy {
        with(queryJSON("subject[0].ranges", JSONArray())) {
            for (i in 0 until length()) {
                runCatching {
                    val rangeItem = getJSONObject(i)
                    if (rangeItem.has("type") && rangeItem.optString("type") == "noticon") {
                        return@lazy rangeItem.optString("value", "")
                    }
                }.getOrElse {
                    return@lazy ""
                }
            }
        }
        ""
    }

    val commentAuthorName: String by lazy {
        val bodyArray = body
        for (i in 0 until bodyArray.length()) {
            runCatching {
                val bodyItem = bodyArray.getJSONObject(i)
                if (bodyItem.has("type") && bodyItem.optString("type") == "user") {
                    return@lazy bodyItem.optString("text")
                }
            }.getOrElse {
                return@lazy ""
            }
        }
        ""
    }

    val isCommentType: Boolean by lazy {
        isTypeRaw(NOTE_COMMENT_TYPE) ||
                isAutomattcherType && JSONUtils.queryJSON(mNoteJSON, "meta.ids.comment", -1) != -1
    }

    private val commentAuthorUrl: String by lazy {
        val bodyArray = body
        for (i in 0 until bodyArray.length()) {
            runCatching {
                val bodyItem = bodyArray.getJSONObject(i)
                if (bodyItem.has("type") && bodyItem.optString("type") == "user") {
                    return@lazy JSONUtils.queryJSON(bodyItem, "meta.links.home", "")
                }
            }.getOrElse {
                return@lazy ""
            }
        }
        ""
    }

    /**
     * Computed properties
     */
    val json: JSONObject
        get() = mNoteJSON ?: JSONObject()
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
    val isViewMilestoneType: Boolean
        get() = isTypeRaw(NOTE_VIEW_MILESTONE)
    val isCommentReplyType: Boolean
        get() = isCommentType && parentCommentId > 0
    val isCommentWithUserReply: Boolean
        // Returns true if the user has replied to this comment note
        get() = isCommentType && !TextUtils.isEmpty(commentSubjectNoticon)
    val isUserList: Boolean
        get() = isLikeType || isFollowType
    val isUnread: Boolean // Parsing every time since it may change
        get() = queryJSON("read", 0) != 1
    val timestamp: Long
        get() = DateTimeUtils.timestampFromIso8601(timestampString)
    val commentStatus: CommentStatus
        get() = if (enabledCommentActions.contains(EnabledActions.ACTION_UNAPPROVE)) {
            CommentStatus.APPROVED
        } else if (enabledCommentActions.contains(EnabledActions.ACTION_APPROVE)) {
            CommentStatus.UNAPPROVED
        } else {
            CommentStatus.ALL
        }

    /**
     * Setters
     */

    fun setRead() = updateReadState(1)

    fun setUnread() = updateReadState(0)

    private fun updateReadState(read: Int) {
        try {
            mNoteJSON?.putOpt("read", read)
        } catch (e: JSONException) {
            AppLog.e(AppLog.T.NOTIFS, "Failed to set 'read' property", e)
        }
    }

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

    /**
     * Helper methods
     */

    fun canModerate() = enabledCommentActions.contains(EnabledActions.ACTION_APPROVE) ||
            enabledCommentActions.contains(EnabledActions.ACTION_UNAPPROVE)

    fun canReply() = enabledCommentActions.contains(EnabledActions.ACTION_REPLY)

    fun canLikeComment() = enabledCommentActions.contains(EnabledActions.ACTION_LIKE_COMMENT)

    fun canLikePost() = enabledPostActions.contains(EnabledActions.ACTION_LIKE_POST)

    fun getFormattedSubject(notificationsUtilsWrapper: NotificationsUtilsWrapper): Spannable {
        return subject?.let { notificationsUtilsWrapper.getSpannableContentForRanges(it) } ?: SpannableString("")
    }

    fun hasLikedComment() = commentActions.length() > 0 && commentActions.optBoolean(ACTION_KEY_LIKE_COMMENT)

    fun hasLikedPost() = postActions.length() > 0 && postActions.optBoolean(ACTION_KEY_LIKE_POST)

    /**
     * Compares two notes to see if they are the same: as it's potentially a very processing intensive operation,
     * we're only comparing the note id, timestamp, and raw JSON length, which is accurate enough for the purpose of
     * checking if the local Note is any different from a remote note.
     */
    fun equalsTimeAndLength(note: Note?) = note != null &&
            (timestampString.equals(note.timestampString, ignoreCase = true) &&
                    json.length() == note.json.length())

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

    private fun isTypeRaw(rawType: String) = this.rawType == rawType

    /**
     * Rudimentary system for pulling an item out of a JSON object hierarchy
     */
    private fun <U> queryJSON(query: String?, defaultObject: U): U =
        if (mNoteJSON == null) defaultObject
        else JSONUtils.queryJSON(mNoteJSON, query, defaultObject)

    /**
     * Get the actions for a given comment or post
     * @param itemId The comment or post id
     * @param type The type of the item: `post` or `comment`
     */
    private fun getActions(itemId: Long, type: String): JSONObject {
        var actions: JSONObject? = null
        var foundOrError = false
        var i = 0
        while (!foundOrError && i < body.length()) {
            val bodyItem = runCatching { body.getJSONObject(i) }.getOrNull()
            if (bodyItem?.has("type") == true && bodyItem.optString("type") == type &&
                itemId == JSONUtils.queryJSON(bodyItem, "meta.ids.$type", 0).toLong()) {
                actions = JSONUtils.queryJSON(bodyItem, "actions", JSONObject())
                foundOrError = true
            }
            i++
        }
        return actions ?: JSONObject()
    }

    private fun getEnabledActions(jsonActions: JSONObject): EnumSet<EnabledActions> {
        val actions = EnumSet.noneOf(EnabledActions::class.java)
        if (jsonActions.length() == 0) return actions

        if (jsonActions.has(ACTION_KEY_REPLY)) {
            actions.add(EnabledActions.ACTION_REPLY)
        }
        if (jsonActions.has(ACTION_KEY_APPROVE) && jsonActions.optBoolean(ACTION_KEY_APPROVE, false)) {
            actions.add(EnabledActions.ACTION_UNAPPROVE)
        }
        if (jsonActions.has(ACTION_KEY_APPROVE) && !jsonActions.optBoolean(ACTION_KEY_APPROVE, false)) {
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

    companion object {
        private const val MAX_COMMENT_PREVIEW_LENGTH = 200 // maximum character length for a comment preview
        private const val MAX_PN_LENGTH = 4096 // max length an Android PN payload can have

        // Note types
        const val NOTE_FOLLOW_TYPE = "follow"
        const val NOTE_LIKE_TYPE = "like"
        const val NOTE_COMMENT_TYPE = "comment"
        const val NOTE_MATCHER_TYPE = "automattcher"
        const val NOTE_COMMENT_LIKE_TYPE = "comment_like"
        const val NOTE_NEW_POST_TYPE = "new_post"
        const val NOTE_VIEW_MILESTONE = "view_milestone"
        const val NOTE_UNKNOWN_TYPE = "unknown"

        // JSON action keys
        private const val ACTION_KEY_REPLY = "replyto-comment"
        private const val ACTION_KEY_APPROVE = "approve-comment"
        private const val ACTION_KEY_SPAM = "spam-comment"
        private const val ACTION_KEY_LIKE_COMMENT = "like-comment"
        private const val ACTION_KEY_LIKE_POST = "like-post"

        // Time constants
        private const val LAST_MONTH = -1
        private const val LAST_WEEK = -1
        private const val LAST_TWO_DAYS = -2
        private const val SINCE_YESTERDAY = -1
        private const val MILLISECOND = 1000

        /**
         * Compare note timestamp to now and return a time grouping
         */
        fun getTimeGroupForTimestamp(timestamp: Long): NoteTimeGroup {
            val today = Date()
            val then = Date(timestamp * MILLISECOND)
            return when {
                then < addMonths(today, LAST_MONTH) -> NoteTimeGroup.GROUP_OLDER_MONTH
                then < addWeeks(today, LAST_WEEK) -> NoteTimeGroup.GROUP_OLDER_WEEK
                then < addDays(today, LAST_TWO_DAYS) ||
                        isSameDay(addDays(today, LAST_TWO_DAYS), then) -> NoteTimeGroup.GROUP_OLDER_TWO_DAYS
                isSameDay(addDays(today, SINCE_YESTERDAY), then) -> NoteTimeGroup.GROUP_YESTERDAY
                else -> NoteTimeGroup.GROUP_TODAY
            }
        }

        @JvmStatic
        @Synchronized
        fun buildFromBase64EncodedData(noteId: String, base64FullNoteData: String?): Note? {
            if (base64FullNoteData == null) return null
            val b64DecodedPayload = Base64.decode(base64FullNoteData, Base64.DEFAULT)

            // Decompress the payload
            val decompresser = Inflater()
            decompresser.setInput(b64DecodedPayload, 0, b64DecodedPayload.size)
            val result = ByteArray(MAX_PN_LENGTH)
            val resultLength = try {
                val length = decompresser.inflate(result)
                decompresser.end()
                length
            } catch (e: DataFormatException) {
                AppLog.e(AppLog.T.NOTIFS, "Can't decompress the PN BlockListPayload. It could be > 4K", e)
                0
            } catch (e: ZipException) {
                AppLog.e(AppLog.T.NOTIFS, "Can't decompress the PN BlockListPayload. Possible zip traversal exploit", e)
                0
            }
            val out: String? = try {
                String(result, 0, resultLength, charset("UTF8"))
            } catch (e: UnsupportedEncodingException) {
                AppLog.e(AppLog.T.NOTIFS, "Notification data contains non UTF8 characters.", e)
                null
            }
            return out?.runCatching {
                var jsonObject = JSONObject(out)
                if (jsonObject.has("notes")) {
                    val jsonArray: JSONArray? = jsonObject.getJSONArray("notes")
                    if (jsonArray != null && jsonArray.length() == 1) {
                        jsonObject = jsonArray.getJSONObject(0)
                    }
                }
                Note(noteId, jsonObject)
            }?.getOrElse {
                AppLog.e(AppLog.T.NOTIFS, "Can't parse the Note JSON received in the PN")
                null
            }
        }
    }
}
