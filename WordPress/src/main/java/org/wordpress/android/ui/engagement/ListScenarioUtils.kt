package org.wordpress.android.ui.engagement

import android.content.Context
import android.text.Spannable
import dagger.Reusable
import org.json.JSONArray
import org.json.JSONException
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.models.Note
import org.wordpress.android.ui.engagement.AuthorName.AuthorNameCharSequence
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_COMMENT_LIKES
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_POST_LIKES
import org.wordpress.android.ui.notifications.blocks.HeaderNoteBlock
import org.wordpress.android.ui.notifications.blocks.NoteBlockClickableSpan
import org.wordpress.android.ui.notifications.utils.NotificationsUtilsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.NOTIFS
import org.wordpress.android.util.getMediaUrlOrEmpty
import org.wordpress.android.util.getRangeIdOrZero
import org.wordpress.android.util.getRangeSiteIdOrZero
import org.wordpress.android.util.getRangeUrlOrEmpty
import org.wordpress.android.util.getTextOrEmpty
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.image.ImageType.AVATAR_WITH_BACKGROUND
import javax.inject.Inject

@Reusable
class ListScenarioUtils @Inject constructor(
    val imageManager: ImageManager,
    val notificationsUtilsWrapper: NotificationsUtilsWrapper
) {
    fun mapLikeNoteToListScenario(note: Note, context: Context): ListScenario {
        require(note.isLikeType) { "mapLikeNoteToListScenario > unexpected note type ${note.type}" }

        val imageType = AVATAR_WITH_BACKGROUND
        val headerNoteBlock = HeaderNoteBlock(
            null,
            transformToFormattableContentList(note.header),
            imageType,
            null,
            null,
            imageManager,
            notificationsUtilsWrapper
        )
        headerNoteBlock.setIsComment(note.isCommentType)

        val spannable: Spannable = notificationsUtilsWrapper.getSpannableContentForRanges(headerNoteBlock.getHeader(0))
        val spans = spannable.getSpans(
            0, spannable.length,
            NoteBlockClickableSpan::class.java
        )
        for (span in spans) {
            span.enableColors(context)
        }

        return ListScenario(
            type = if (note.isPostLikeType) LOAD_POST_LIKES else LOAD_COMMENT_LIKES,
            source = EngagementNavigationSource.LIKE_NOTIFICATION_LIST,
            siteId = note.siteId.toLong(),
            postOrCommentId = if (note.isPostLikeType) note.postId.toLong() else note.commentId,
            commentPostId = if (note.isCommentLikeType) note.postId.toLong() else 0L,
            commentSiteUrl = if (note.isCommentLikeType) note.url else "",
            headerData = HeaderData(
                authorName = AuthorNameCharSequence(spannable),
                snippetText = headerNoteBlock.getHeader(1).getTextOrEmpty(),
                authorAvatarUrl = headerNoteBlock.getHeader(0).getMediaUrlOrEmpty(0),
                authorUserId = headerNoteBlock.getHeader(0).getRangeIdOrZero(0),
                authorPreferredSiteId = headerNoteBlock.getHeader(0).getRangeSiteIdOrZero(0),
                authorPreferredSiteUrl = headerNoteBlock.getHeader(0).getRangeUrlOrEmpty(0)
            )
        )
    }

    fun transformToFormattableContentList(headerArray: JSONArray?): List<FormattableContent> {
        val headersList: MutableList<FormattableContent> = ArrayList()
        if (headerArray != null) {
            for (i in 0 until headerArray.length()) {
                try {
                    headersList.add(
                        notificationsUtilsWrapper.mapJsonToFormattableContent(
                            headerArray.getJSONObject(i)
                        )
                    )
                } catch (e: JSONException) {
                    AppLog.e(NOTIFS, "Header array has invalid format.")
                }
            }
        }
        return headersList
    }
}
