package org.wordpress.android.ui.notifications.compose

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRangeType
import org.wordpress.android.models.Note
import org.wordpress.android.models.Notification
import org.wordpress.android.ui.rs.RsDateFormatter
import org.wordpress.android.util.GravatarUtils
import java.util.Date

data class NotificationRowUiModel(
    val noteId: String,
    val subject: AnnotatedString,
    val snippet: String,
    /** One avatar for most notes, up to three for likes and follows by several people. */
    val avatarUrls: List<String>,
    val timestampMillis: Long,
    val dateLabel: String,
    val isUnread: Boolean,
    val hasUserReplied: Boolean,
    val inlineAction: NotificationInlineAction?
)

sealed interface NotificationInlineAction {
    data class LikeComment(val isLiked: Boolean) : NotificationInlineAction
    data class LikePost(val isLiked: Boolean) : NotificationInlineAction
    data object Share : NotificationInlineAction
}

/** Parses note JSON, so call it off the main thread. */
class NotificationRowMapper(
    private val mapSubject: (Note) -> FormattableContent?,
    private val nowLabel: String
) {
    fun map(note: Note) = NotificationRowUiModel(
        noteId = note.id,
        subject = mapSubject(note).toAnnotatedString(),
        snippet = note.commentSubject.orEmpty().trim(),
        avatarUrls = note.avatarUrls().map { GravatarUtils.fixGravatarUrl(it, AVATAR_REQUEST_SIZE_PX) },
        timestampMillis = note.timestamp * MILLIS_PER_SECOND,
        dateLabel = RsDateFormatter.format(Date(note.timestamp * MILLIS_PER_SECOND), nowLabel),
        isUnread = note.isUnread,
        hasUserReplied = note.commentSubjectNoticon.isNotEmpty(),
        inlineAction = note.inlineAction()
    )

    private fun Note.avatarUrls(): List<String> {
        val urls = iconURLs.orEmpty()
        return if ((isFollowType || isLikeType) && urls.size > 1) {
            urls.take(MAX_AVATARS)
        } else {
            listOfNotNull(iconURL.takeIf { it.isNotEmpty() })
        }
    }

    private fun Note.inlineAction(): NotificationInlineAction? = when (Notification.from(this)) {
        Notification.Comment -> if (canLikeComment()) {
            NotificationInlineAction.LikeComment(hasLikedComment())
        } else {
            null
        }
        Notification.NewPost -> if (canLikePost()) NotificationInlineAction.LikePost(hasLikedPost()) else null
        is Notification.PostLike -> NotificationInlineAction.Share
        Notification.Unknown -> null
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1000L
        private const val MAX_AVATARS = 3
        private const val AVATAR_REQUEST_SIZE_PX = 120
    }
}

/** Bolds user, site and post ranges, as the legacy `NoteBlockClickableSpan.getSpanStyle` intended. */
internal fun FormattableContent?.toAnnotatedString(): AnnotatedString {
    val text = this?.text?.trimEnd().orEmpty()
    return buildAnnotatedString {
        append(text)
        this@toAnnotatedString?.ranges.orEmpty().forEach { range ->
            val indices = range.indices ?: return@forEach
            if (indices.size != 2) return@forEach
            val start = indices[0]
            val end = indices[1].coerceAtMost(text.length)
            if (start < 0 || start >= end) return@forEach
            val style = when (range.rangeType()) {
                FormattableRangeType.USER,
                FormattableRangeType.SITE,
                FormattableRangeType.POST,
                FormattableRangeType.COMMENT,
                FormattableRangeType.MATCH,
                FormattableRangeType.B -> SpanStyle(fontWeight = FontWeight.SemiBold)
                FormattableRangeType.BLOCKQUOTE -> SpanStyle(fontStyle = FontStyle.Italic)
                else -> null
            }
            style?.let { addStyle(it, start, end) }
        }
    }
}
