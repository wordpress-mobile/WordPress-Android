package org.wordpress.android.ui.history

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.revisions.Diff
import org.wordpress.android.fluxc.model.revisions.RevisionModel
import org.wordpress.android.ui.history.HistoryListItem.ViewType.FOOTER
import org.wordpress.android.ui.history.HistoryListItem.ViewType.HEADER
import org.wordpress.android.ui.history.HistoryListItem.ViewType.REVISION
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.toFormattedDateString
import org.wordpress.android.util.toFormattedTimeString
import java.util.ArrayList
import java.util.Date

sealed class HistoryListItem(val type: ViewType) {
    open fun longId(): Long = hashCode().toLong()

    data class Footer(val text: String) : HistoryListItem(FOOTER)

    data class Header(val text: String) : HistoryListItem(HEADER)

    @Parcelize
    @SuppressLint("ParcelCreator")
    data class Revision(
        val revisionId: Long,
        val diffFromVersion: Long,
        val totalAdditions: Int,
        val totalDeletions: Int,
        val postContent: String?,
        val postExcerpt: String?,
        val postTitle: String?,
        val postDateGmt: String?,
        val postModifiedGmt: String?,
        val postAuthorId: String?,
        val titleDiffs: ArrayList<@RawValue Diff>,
        val contentDiffs: ArrayList<@RawValue Diff>,
        var authorDisplayName: String? = null,
        var authorAvatarURL: String? = null
    ) : HistoryListItem(REVISION), Parcelable {
        // Replace space with T since API returns yyyy-MM-dd hh:mm:ssZ and ISO 8601 format is yyyy-MM-ddThh:mm:ssZ.
        @IgnoredOnParcel private val postDate: Date = DateTimeUtils.dateUTCFromIso8601(postDateGmt?.replace(" ", "T"))
        @IgnoredOnParcel val timeSpan: String = DateTimeUtils.javaDateToTimeSpan(postDate, WordPress.getContext())
        @IgnoredOnParcel val formattedDate: String = postDate.toFormattedDateString()
        @IgnoredOnParcel val formattedTime: String = postDate.toFormattedTimeString()

        constructor(model: RevisionModel) : this(
                model.revisionId,
                model.diffFromVersion,
                model.totalAdditions,
                model.totalDeletions,
                model.postContent ?: "",
                model.postExcerpt ?: "",
                model.postTitle ?: "",
                model.postDateGmt ?: "",
                model.postModifiedGmt ?: "",
                model.postAuthorId ?: "",
                model.titleDiffs,
                model.contentDiffs)

        override fun longId(): Long = revisionId.hashCode().toLong()
    }

    enum class ViewType(val id: Int) {
        FOOTER(0),
        HEADER(1),
        REVISION(2)
    }
}
