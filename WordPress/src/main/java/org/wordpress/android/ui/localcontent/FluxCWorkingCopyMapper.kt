package org.wordpress.android.ui.localcontent

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.PostUtilsWrapper
import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.ParsedUrl
import uniffi.wp_api.PostFormat
import uniffi.wp_mobile_local_content.FeaturedImage
import uniffi.wp_mobile_local_content.PageWorkingCopyFields
import uniffi.wp_mobile_local_content.PostWorkingCopyFields
import uniffi.wp_mobile_local_content.PublishDate
import uniffi.wp_mobile_local_content.RemotePostReference
import uniffi.wp_mobile_local_content.SiteIdentity
import uniffi.wp_mobile_local_content.StatusSetting
import uniffi.wp_mobile_local_content.WorkingCopyCommonFields
import uniffi.wp_mobile_local_content.WorkingCopyTag
import javax.inject.Inject

/**
 * Maps a FluxC [PostModel] to working copy fields, so a post the editor saves through FluxC can be
 * saved to the local content database too.
 *
 * FluxC doesn't hold everything a working copy can: tags are names, not IDs, so they map to
 * [WorkingCopyTag.New], and comment and ping status aren't mapped.
 */
class FluxCWorkingCopyMapper @Inject constructor(
    private val postUtils: PostUtilsWrapper,
) {
    /** Returns `null` for a trashed post, which isn't edited. */
    fun toPostFields(post: PostModel): PostWorkingCopyFields? = common(post)?.let { common ->
        PostWorkingCopyFields(
            common = common,
            categories = post.categoryIdList,
            tags = post.tagNameList.map { WorkingCopyTag.New(name = it) },
            format = postFormat(post.postFormat),
            sticky = post.sticky
        )
    }

    /** Returns `null` for a trashed page, which isn't edited. */
    fun toPageFields(post: PostModel): PageWorkingCopyFields? = common(post)?.let { common ->
        PageWorkingCopyFields(common = common, parent = post.parentId.takeIf { it > 0 })
    }

    /**
     * The post on the server that FluxC's [post] was fetched from or uploaded to, with FluxC's
     * `lastModified` as its base version.
     *
     * Returns `null` for a post that isn't on the server, and for one FluxC has no `lastModified` for.
     */
    fun toRemotePostReference(post: PostModel): RemotePostReference? {
        if (post.remotePostId <= 0) return null
        val lastModified = DateTimeUtils.dateFromIso8601(post.lastModified) ?: return null
        return RemotePostReference(postId = post.remotePostId, baseModifiedGmt = lastModified)
    }

    /** A site has a WordPress.com site ID only when FluxC's `siteId` isn't 0. */
    fun toSiteIdentity(site: SiteModel): SiteIdentity = SiteIdentity(
        url = ParsedUrl.parse(site.url),
        wordpressComSiteId = site.siteId.takeIf { it > 0 }?.toULong()
    )

    private fun common(post: PostModel): WorkingCopyCommonFields? {
        val password = post.password.takeIf { it.isNotEmpty() }
        val status = when (post.status) {
            // A post FluxC hasn't given a status yet is a new draft.
            "draft", "" -> StatusSetting.Draft(password = password)
            "pending" -> StatusSetting.PendingReview(password = password)
            "publish", "future" -> StatusSetting.Publish(password = password)
            // WordPress clears the password of a private post.
            "private" -> StatusSetting.Private
            "trash" -> return null
            else -> StatusSetting.Custom(status = post.status, password = password)
        }
        return WorkingCopyCommonFields(
            title = post.title,
            content = post.content,
            excerpt = post.excerpt,
            slug = post.slug.takeIf { it.isNotEmpty() },
            status = status,
            publishDate = publishDate(post),
            author = post.authorId.takeIf { it > 0 },
            featuredImage = post.featuredImageId.takeIf { it > 0 }
                ?.let { FeaturedImage.Remote(mediaId = it) }
                ?: FeaturedImage.None,
            commentStatus = null,
            pingStatus = null
        )
    }

    /** Follows `PostUtils.shouldPublishImmediately`, so a draft dated in the past publishes immediately. */
    private fun publishDate(post: PostModel): PublishDate {
        val date = DateTimeUtils.dateFromIso8601(post.dateCreated)
        return if (date == null || postUtils.shouldPublishImmediately(PostStatus.fromPost(post), post.dateCreated)) {
            PublishDate.Immediately
        } else {
            PublishDate.At(date = date)
        }
    }

    private fun postFormat(format: String): PostFormat = when (format) {
        "standard", "" -> PostFormat.Standard
        "aside" -> PostFormat.Aside
        "audio" -> PostFormat.Audio
        "chat" -> PostFormat.Chat
        "gallery" -> PostFormat.Gallery
        "image" -> PostFormat.Image
        "link" -> PostFormat.Link
        "quote" -> PostFormat.Quote
        "status" -> PostFormat.Status
        "video" -> PostFormat.Video
        else -> PostFormat.Custom(v1 = format)
    }
}
