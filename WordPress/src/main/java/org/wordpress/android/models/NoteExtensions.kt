package org.wordpress.android.models

val Note.type
    get() = runCatching { NoteType.valueOf(rawType) }.getOrDefault(NoteType.Unknown)

enum class NoteType(val value: String) {
    Follow(Note.NOTE_FOLLOW_TYPE),
    Like(Note.NOTE_LIKE_TYPE),
    Comment(Note.NOTE_COMMENT_TYPE),
    Matcher(Note.NOTE_MATCHER_TYPE),
    CommentLike(Note.NOTE_COMMENT_LIKE_TYPE),
    Reblog(Note.NOTE_REBLOG_TYPE),
    NewPost(Note.NOTE_NEW_POST_TYPE),
    ViewMilestone(Note.NOTE_VIEW_MILESTONE),
    Unknown(Note.NOTE_UNKNOWN_TYPE),
}
