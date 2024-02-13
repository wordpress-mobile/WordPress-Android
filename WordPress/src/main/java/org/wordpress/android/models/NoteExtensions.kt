package org.wordpress.android.models

import org.wordpress.android.models.Notification.PostNotification.Like
import org.wordpress.android.models.Notification.PostNotification.Reblog
import org.wordpress.android.models.Notification.PostNotification.NewPost

val Note.type
    get() = NoteType.from(rawType)

sealed class Notification {
    sealed class PostNotification: Notification() {
        abstract val url: String
        abstract val title: String
        data class Like(override val url: String, override val title: String): PostNotification()
        data class Reblog(override val url: String, override val title: String): PostNotification()
        data class NewPost(override val url: String, override val title: String): PostNotification()
    }
    data object Comment: Notification()
    data object Unknown: Notification()

    companion object {
        fun from(rawNote: Note) = when(rawNote.type) {
            NoteType.Like -> Like(url = rawNote.url, title = rawNote.title)
            NoteType.Reblog -> Reblog(url= rawNote.url, title = rawNote.title)
            NoteType.NewPost -> NewPost(url= rawNote.url, title = rawNote.title)
            else -> Unknown
        }
    }
}
enum class NoteType(val rawType: String) {
    Follow(Note.NOTE_FOLLOW_TYPE),
    Like(Note.NOTE_LIKE_TYPE),
    Comment(Note.NOTE_COMMENT_TYPE),
    Matcher(Note.NOTE_MATCHER_TYPE),
    CommentLike(Note.NOTE_COMMENT_LIKE_TYPE),
    Reblog(Note.NOTE_REBLOG_TYPE),
    NewPost(Note.NOTE_NEW_POST_TYPE),
    ViewMilestone(Note.NOTE_VIEW_MILESTONE),
    Unknown(Note.NOTE_UNKNOWN_TYPE);

    companion object {
        private val map = entries.associateBy(NoteType::rawType)
        fun from(rawType: String) = map[rawType] ?: Unknown
    }
}
