package org.wordpress.android.ui.rs.contentlist

import org.wordpress.android.R
import org.wordpress.android.ui.rs.RsDateFormatter
import org.wordpress.android.ui.rs.toLabel
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.HtmlUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostCommentStatus
import uniffi.wp_api.PostStatus
import uniffi.wp_mobile.FullEntityAnyPostWithEditContext
import uniffi.wp_mobile.PostItemState

/**
 * Maps what the rs collection reports for one row onto [ContentItemUiModel].
 *
 * A collection reports a row before it has the data for it, so the states without data map to a
 * placeholder or an error row carrying only [remoteId] - the list still has to render something
 * in that position.
 *
 * [showStatus] is set while searching, where results mix statuses and the row has to say which one
 * it is. [nowLabel] is the word a date within the last day is shown as.
 */
fun <A : RsMenuAction> PostItemState.toContentItemUiModel(
    remoteId: Long,
    nowLabel: String,
    showStatus: Boolean = false
): ContentItemUiModel<A> = when (this) {
    is PostItemState.Fresh -> data.toContentItemUiModel(showStatus, nowLabel)
    is PostItemState.Stale -> data.toContentItemUiModel(showStatus, nowLabel)
    is PostItemState.FetchingWithData ->
        data.toContentItemUiModel(showStatus, nowLabel, ContentDisplayState.FETCHING_WITH_DATA)
    is PostItemState.FailedWithData ->
        data.toContentItemUiModel(showStatus, nowLabel, ContentDisplayState.FAILED_WITH_DATA)
    is PostItemState.Missing,
    is PostItemState.Fetching -> blankItem(remoteId, ContentDisplayState.PLACEHOLDER)
    is PostItemState.Failed -> blankItem(remoteId, ContentDisplayState.ERROR)
}

private fun <A : RsMenuAction> blankItem(remoteId: Long, displayState: ContentDisplayState) =
    ContentItemUiModel<A>(
        remoteId = remoteId,
        title = "",
        excerpt = "",
        date = "",
        displayState = displayState
    )

private fun <A : RsMenuAction> FullEntityAnyPostWithEditContext.toContentItemUiModel(
    showStatus: Boolean,
    nowLabel: String,
    displayState: ContentDisplayState = ContentDisplayState.NORMAL
): ContentItemUiModel<A> {
    val item: AnyPostWithEditContext = data
    return ContentItemUiModel(
        remoteId = item.id,
        title = item.title?.raw?.takeIf { it.isNotBlank() }
            ?: item.title?.rendered
            ?: "",
        excerpt = (
            item.excerpt?.raw?.takeIf { it.isNotBlank() }
                ?: item.excerpt?.rendered
                ?: ""
            ).let { HtmlUtils.fastStripHtml(it).trim() },
        date = RsDateFormatter.format(item.dateGmt, nowLabel, isScheduled = item.status is PostStatus.Future),
        dateGmtMillis = item.dateGmt.time,
        parentId = item.parent ?: 0L,
        lastModified = DateTimeUtils.iso8601UTCFromDate(item.modifiedGmt),
        link = item.link,
        hasPassword = !item.password.isNullOrEmpty(),
        commentsOpen = item.commentStatus is PostCommentStatus.Open,
        status = item.status,
        statusLabelResId = if (showStatus) item.status.toLabel() else 0,
        authorId = item.author ?: 0L,
        featuredImageId = item.featuredMedia ?: 0L,
        badges = buildList {
            if (item.status is PostStatus.Private) {
                add(R.string.post_status_post_private)
            }
            if (item.status is PostStatus.Pending) {
                add(R.string.post_status_pending_review)
            }
            // Pages have no sticky flag - WordPress does not return one for them, so this is
            // inert there rather than conditional on the post type. ContentItemMapperTest pins it.
            if (item.sticky == true) {
                add(R.string.post_status_sticky)
            }
        },
        displayState = displayState
    )
}
