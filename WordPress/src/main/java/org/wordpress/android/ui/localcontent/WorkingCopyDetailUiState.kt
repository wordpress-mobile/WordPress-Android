package org.wordpress.android.ui.localcontent

import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.PostCommentStatus
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostPingStatus
import uniffi.wp_mobile_local_content.FeaturedImage
import uniffi.wp_mobile_local_content.PageWorkingCopy
import uniffi.wp_mobile_local_content.PostWorkingCopy
import uniffi.wp_mobile_local_content.PublishDate
import uniffi.wp_mobile_local_content.StatusSetting
import uniffi.wp_mobile_local_content.WorkingCopyCommonFields
import uniffi.wp_mobile_local_content.WorkingCopyId
import uniffi.wp_mobile_local_content.WorkingCopyRemoteTarget
import uniffi.wp_mobile_local_content.WorkingCopyTag
import java.util.Date
import javax.inject.Inject

sealed interface WorkingCopyDetailUiState {
    data object Loading : WorkingCopyDetailUiState
    data object NotFound : WorkingCopyDetailUiState
    data class Error(val message: String) : WorkingCopyDetailUiState
    data class Loaded(val title: String, val sections: List<WorkingCopyFieldSection>) : WorkingCopyDetailUiState
}

/** Fields of one object in the working copy, titled with that object's path or type. */
data class WorkingCopyFieldSection(val title: String, val fields: List<WorkingCopyField>)

/** A field named as the Kotlin bindings name it, and its value as text. */
data class WorkingCopyField(val name: String, val value: String)

/**
 * Lists a working copy's stored values for the read-only detail screen, named as the Kotlin bindings
 * name them. A password shows only whether there is one.
 */
class WorkingCopyDetailMapper @Inject constructor() {
    fun toUiState(workingCopy: PostWorkingCopy) = WorkingCopyDetailUiState.Loaded(
        title = workingCopy.fields.common.title,
        sections = listOf(
            workingCopySection("PostWorkingCopy", workingCopy.id, workingCopy.createdAt, workingCopy.updatedAt),
            remoteTargetSection(workingCopy.remoteTarget),
            WorkingCopyFieldSection(
                title = "fields",
                fields = listOf(
                    WorkingCopyField("categories", workingCopy.fields.categories.toString()),
                    WorkingCopyField("tags", workingCopy.fields.tags.joinToString(prefix = "[", postfix = "]") { tag(it) }),
                    WorkingCopyField("format", format(workingCopy.fields.format)),
                    WorkingCopyField("sticky", workingCopy.fields.sticky.toString())
                )
            ),
            commonSection(workingCopy.fields.common)
        )
    )

    fun toUiState(workingCopy: PageWorkingCopy) = WorkingCopyDetailUiState.Loaded(
        title = workingCopy.fields.common.title,
        sections = listOf(
            workingCopySection("PageWorkingCopy", workingCopy.id, workingCopy.createdAt, workingCopy.updatedAt),
            remoteTargetSection(workingCopy.remoteTarget),
            WorkingCopyFieldSection(
                title = "fields",
                fields = listOf(WorkingCopyField("parent", workingCopy.fields.parent.orNull()))
            ),
            commonSection(workingCopy.fields.common)
        )
    )

    private fun workingCopySection(type: String, id: WorkingCopyId, createdAt: String, updatedAt: String) =
        WorkingCopyFieldSection(
            title = type,
            fields = listOf(
                WorkingCopyField("id", id),
                WorkingCopyField("createdAt", createdAt),
                WorkingCopyField("updatedAt", updatedAt)
            )
        )

    private fun remoteTargetSection(remoteTarget: WorkingCopyRemoteTarget): WorkingCopyFieldSection {
        val remotePost = remoteTarget.remotePost
        return WorkingCopyFieldSection(
            title = "remoteTarget",
            fields = listOfNotNull(
                WorkingCopyField("site.url", remoteTarget.site.url.url()),
                WorkingCopyField("site.wordpressComSiteId", remoteTarget.site.wordpressComSiteId.orNull()),
                WorkingCopyField("remotePost", NULL).takeIf { remotePost == null },
                remotePost?.let { WorkingCopyField("remotePost.postId", it.postId.toString()) },
                remotePost?.let { WorkingCopyField("remotePost.baseModifiedGmt", date(it.baseModifiedGmt)) }
            )
        )
    }

    private fun commonSection(common: WorkingCopyCommonFields) = WorkingCopyFieldSection(
        title = "fields.common",
        fields = listOf(
            WorkingCopyField("title", common.title),
            WorkingCopyField("status", status(common.status)),
            WorkingCopyField("password", if (password(common.status) == null) NULL else PASSWORD_SET),
            WorkingCopyField("publishDate", publishDate(common.publishDate)),
            WorkingCopyField("slug", common.slug.orNull()),
            WorkingCopyField("author", common.author.orNull()),
            WorkingCopyField("featuredImage", featuredImage(common.featuredImage)),
            WorkingCopyField("commentStatus", common.commentStatus?.let { commentStatus(it) }.orNull()),
            WorkingCopyField("pingStatus", common.pingStatus?.let { pingStatus(it) }.orNull()),
            WorkingCopyField("excerpt", common.excerpt),
            WorkingCopyField("content", common.content)
        )
    )

    private fun status(status: StatusSetting) = when (status) {
        is StatusSetting.Draft -> "Draft"
        is StatusSetting.PendingReview -> "PendingReview"
        is StatusSetting.Publish -> "Publish"
        is StatusSetting.Private -> "Private"
        is StatusSetting.Custom -> "Custom(status = ${status.status})"
    }

    private fun password(status: StatusSetting) = when (status) {
        is StatusSetting.Draft -> status.password
        is StatusSetting.PendingReview -> status.password
        is StatusSetting.Publish -> status.password
        is StatusSetting.Private -> null
        is StatusSetting.Custom -> status.password
    }

    private fun publishDate(publishDate: PublishDate) = when (publishDate) {
        is PublishDate.Immediately -> "Immediately"
        is PublishDate.At -> "At(date = ${date(publishDate.date)})"
    }

    private fun featuredImage(featuredImage: FeaturedImage) = when (featuredImage) {
        is FeaturedImage.None -> "None"
        is FeaturedImage.Remote -> "Remote(mediaId = ${featuredImage.mediaId})"
    }

    private fun commentStatus(status: PostCommentStatus) = when (status) {
        is PostCommentStatus.Open -> "Open"
        is PostCommentStatus.Closed -> "Closed"
        is PostCommentStatus.Custom -> "Custom(${status.v1})"
    }

    private fun pingStatus(status: PostPingStatus) = when (status) {
        is PostPingStatus.Open -> "Open"
        is PostPingStatus.Closed -> "Closed"
        is PostPingStatus.Custom -> "Custom(${status.v1})"
    }

    private fun tag(tag: WorkingCopyTag) = when (tag) {
        is WorkingCopyTag.Existing -> "Existing(id = ${tag.id})"
        is WorkingCopyTag.New -> "New(name = ${tag.name})"
    }

    private fun format(format: PostFormat) = when (format) {
        is PostFormat.Standard -> "Standard"
        is PostFormat.Aside -> "Aside"
        is PostFormat.Audio -> "Audio"
        is PostFormat.Chat -> "Chat"
        is PostFormat.Gallery -> "Gallery"
        is PostFormat.Image -> "Image"
        is PostFormat.Link -> "Link"
        is PostFormat.Quote -> "Quote"
        is PostFormat.Status -> "Status"
        is PostFormat.Video -> "Video"
        is PostFormat.Custom -> "Custom(${format.v1})"
    }

    private fun date(date: Date) = DateTimeUtils.iso8601UTCFromDate(date)

    private fun Any?.orNull() = this?.toString() ?: NULL

    companion object {
        private const val NULL = "null"
        private const val PASSWORD_SET = "(set)"
    }
}
