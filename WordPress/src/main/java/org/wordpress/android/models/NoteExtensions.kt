package org.wordpress.android.models

val Note.type
    get() = NoteType.from(rawType)

sealed class Notification {
    data class Like(val url: String, val title: String): Notification()
    data object Unknown: Notification()

    companion object {
        fun from(rawNote: Note) = when(rawNote.type) {
            NoteType.Like -> Like(url= rawNote.url, title = rawNote.title)
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