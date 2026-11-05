package org.wordpress.android.ui.localcontent

import org.wordpress.android.R
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.rs.RsDateFormatter
import org.wordpress.android.ui.rs.contentlist.ContentListRowUiState
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_mobile_local_content.PublishDate
import uniffi.wp_mobile_local_content.StatusSetting
import uniffi.wp_mobile_local_content.WorkingCopyCommonFields
import uniffi.wp_mobile_local_content.WorkingCopyId
import uniffi.wp_mobile_local_content.WorkingCopyRemoteTarget
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Date
import javax.inject.Inject

sealed interface WorkingCopyListUiState {
    data object Loading : WorkingCopyListUiState
    data class Loaded(val rows: List<WorkingCopyRowUiState>) : WorkingCopyListUiState
    data class Error(val message: String) : WorkingCopyListUiState
}

data class WorkingCopyRowUiState(val id: WorkingCopyId, val row: ContentListRowUiState)

/** Maps a working copy onto the row the rs posts and pages lists draw. */
class WorkingCopyRowMapper @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val appLogWrapper: AppLogWrapper
) {
    /**
     * The row shows the working copy's title and excerpt, when its fields were last saved, and its
     * site. Its badges are its status, Sticky, and New for a working copy with no remote post.
     */
    fun toRow(
        id: WorkingCopyId,
        common: WorkingCopyCommonFields,
        remoteTarget: WorkingCopyRemoteTarget,
        updatedAt: String,
        isSticky: Boolean
    ) = WorkingCopyRowUiState(
        id = id,
        row = ContentListRowUiState(
            // ContentListRow doesn't read the ID, and a working copy's ID isn't a number.
            id = 0L,
            title = common.title,
            excerpt = HtmlUtils.fastStripHtml(common.excerpt.ifBlank { common.content }).trim(),
            dateLabel = parseTimestamp(updatedAt)
                ?.let { RsDateFormatter.format(it, resourceProvider.getString(R.string.rs_date_now)) }
                ?: updatedAt,
            authorName = remoteTarget.site.url.prettyUrl(),
            badges = listOfNotNull(
                statusLabel(common),
                R.string.post_status_sticky.takeIf { isSticky },
                R.string.local_content_new_badge.takeIf { remoteTarget.remotePost == null }
            )
        )
    )

    /** `null` for a custom status, which has no label. */
    private fun statusLabel(common: WorkingCopyCommonFields): Int? = when (common.status) {
        is StatusSetting.Draft -> R.string.post_status_draft
        is StatusSetting.PendingReview -> R.string.post_status_pending_review
        is StatusSetting.Publish -> if (isInFuture(common.publishDate)) {
            R.string.post_status_post_scheduled
        } else {
            R.string.post_status_post_published
        }
        is StatusSetting.Private -> R.string.post_status_post_private
        is StatusSetting.Custom -> null
    }

    private fun isInFuture(publishDate: PublishDate) = publishDate is PublishDate.At && publishDate.date.after(Date())

    /** Parses a working copy's timestamp, which has fractional seconds, such as `2026-11-04T12:34:56.789Z`. */
    private fun parseTimestamp(timestamp: String): Date? = try {
        Date.from(Instant.parse(timestamp))
    } catch (e: DateTimeParseException) {
        appLogWrapper.e(T.POSTS, "Local content: couldn't parse the timestamp $timestamp: $e")
        null
    }
}
