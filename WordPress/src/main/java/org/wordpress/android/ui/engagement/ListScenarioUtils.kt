package org.wordpress.android.ui.engagement

import android.app.Activity
import android.text.Spannable
import dagger.Reusable
import org.json.JSONArray
import org.json.JSONException
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.models.Note
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_COMMENT_LIKES
import org.wordpress.android.ui.engagement.ListScenarioType.LOAD_POST_LIKES
import org.wordpress.android.ui.engagement.UserName.UserNameCharSequence
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
import java.util.ArrayList
import javax.inject.Inject

@Reusable
class ListScenarioUtils @Inject constructor(
    val imageManager: ImageManager,
    val notificationsUtilsWrapper: NotificationsUtilsWrapper
) {
    fun mapLikeNoteToListScenario(note: Note, activity: Activity): ListScenario {
        if (!note.isLikeType) throw IllegalArgumentException("mapLikeNoteToListScenario > unexpected note type ${note.type}")

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
            span.enableColors(activity)
        }

        return ListScenario(
                type = if (note.isPostLikeType) LOAD_POST_LIKES else LOAD_COMMENT_LIKES,
                siteId = note.siteId.toLong(),
                itemId = if (note.isPostLikeType) note.postId.toLong() else note.commentId,
                headerData = HeaderData(
                        name = UserNameCharSequence(spannable),
                        snippet = headerNoteBlock.getHeader(1).getTextOrEmpty(),
                        avatarUrl = headerNoteBlock.getHeader(0).getMediaUrlOrEmpty(0),
                        userId = headerNoteBlock.getHeader(0).getRangeIdOrZero(0),
                        userSiteId = headerNoteBlock.getHeader(0).getRangeSiteIdOrZero(0),
                        siteUrl = headerNoteBlock.getHeader(0).getRangeUrlOrEmpty(0)
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
