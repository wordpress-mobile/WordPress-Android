package org.wordpress.android.fluxc.network.rest.wpapi.post

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates between a wordpress-rs [AnyPostWithEditContext] and FluxC's [PostModel].
 *
 * This is the single definition of that mapping: the FluxC REST client uses it to fold a
 * create/update/retrieve response back into the model, and the app's `RsToFluxCMapper` uses it to
 * hand an rs post to the legacy editor. Keeping one copy is the same reasoning as `WpRsRouting` —
 * two mappings for the same payload would drift.
 *
 * Tag names are passed in rather than resolved here: the rs payload carries tag *ids*, and turning
 * those into names is a second network call that only some callers need.
 */
@Singleton
class RsPostMapper @Inject constructor() {
    /**
     * Copies every server-owned field of [post] onto [target], leaving FluxC-owned bookkeeping
     * (local id, local-change flags set by the caller) alone.
     */
    @Suppress("LongParameterList")
    fun applyTo(
        target: PostModel,
        post: AnyPostWithEditContext,
        site: SiteModel,
        tagNames: List<String>,
        isPage: Boolean
    ): PostModel = target.apply {
        val modified = DateTimeUtils.iso8601UTCFromDate(post.modifiedGmt)

        setRemotePostId(post.id)
        setLocalSiteId(site.id)
        setRemoteSiteId(site.siteId)

        setTitle(post.title?.raw?.takeIf { it.isNotBlank() } ?: post.title?.rendered ?: "")
        setContent(post.content.raw?.takeIf { it.isNotBlank() } ?: post.content.rendered)
        // Only `raw` is the author's excerpt. `rendered` is WordPress's auto-generated summary of
        // the content when no excerpt is set, so falling back to it would save an excerpt the
        // author never wrote (CMM-2392). Edit context always returns `raw`, which means an empty
        // value is a real one.
        setExcerpt(post.excerpt?.raw ?: "")

        setStatus(statusToSlug(post.status))
        setLink(post.link)
        setSlug(post.slug)
        setPassword(post.password ?: "")
        setSticky(post.sticky ?: false)

        setAuthorId(post.author ?: 0L)
        setFeaturedImageId(post.featuredMedia ?: 0L)
        setPostFormat(formatToSlug(post.format))

        setDateCreated(DateTimeUtils.iso8601UTCFromDate(post.dateGmt))
        setLastModified(modified)
        setRemoteLastModified(modified)

        setCategoryIdList(post.categories ?: emptyList())
        setTagNameList(tagNames)

        setIsPage(isPage)
        if (isPage) {
            setParentId(post.parent ?: 0L)
        }
    }

    fun statusToSlug(status: PostStatus?): String = when (status) {
        is PostStatus.Publish -> STATUS_PUBLISH
        is PostStatus.Draft -> STATUS_DRAFT
        is PostStatus.Pending -> STATUS_PENDING
        is PostStatus.Private -> STATUS_PRIVATE
        is PostStatus.Future -> STATUS_FUTURE
        is PostStatus.Trash -> STATUS_TRASH
        is PostStatus.Custom -> status.v1
        is PostStatus.Any, null -> STATUS_DRAFT
    }

    fun statusFromSlug(slug: String?): PostStatus = when (slug) {
        STATUS_PUBLISH -> PostStatus.Publish
        STATUS_DRAFT -> PostStatus.Draft
        STATUS_PENDING -> PostStatus.Pending
        STATUS_PRIVATE -> PostStatus.Private
        STATUS_FUTURE -> PostStatus.Future
        STATUS_TRASH -> PostStatus.Trash
        // PostModel carries whatever status string the site uses, including statuses registered by
        // a plugin, so anything unrecognised is forwarded verbatim rather than coerced to a draft.
        null, "" -> PostStatus.Draft
        else -> PostStatus.Custom(slug)
    }

    fun formatToSlug(format: PostFormat?): String = when (format) {
        is PostFormat.Standard -> "standard"
        is PostFormat.Aside -> "aside"
        is PostFormat.Audio -> "audio"
        is PostFormat.Chat -> "chat"
        is PostFormat.Gallery -> "gallery"
        is PostFormat.Image -> "image"
        is PostFormat.Link -> "link"
        is PostFormat.Quote -> "quote"
        is PostFormat.Status -> "status"
        is PostFormat.Video -> "video"
        is PostFormat.Custom -> format.v1
        null -> "standard"
    }

    fun formatFromSlug(slug: String?): PostFormat? = when {
        slug.isNullOrEmpty() -> null
        slug == "standard" -> PostFormat.Standard
        slug == "aside" -> PostFormat.Aside
        slug == "audio" -> PostFormat.Audio
        slug == "chat" -> PostFormat.Chat
        slug == "gallery" -> PostFormat.Gallery
        slug == "image" -> PostFormat.Image
        slug == "link" -> PostFormat.Link
        slug == "quote" -> PostFormat.Quote
        slug == "status" -> PostFormat.Status
        slug == "video" -> PostFormat.Video
        else -> PostFormat.Custom(slug)
    }

    companion object {
        private const val STATUS_PUBLISH = "publish"
        private const val STATUS_DRAFT = "draft"
        private const val STATUS_PENDING = "pending"
        private const val STATUS_PRIVATE = "private"
        private const val STATUS_FUTURE = "future"
        const val STATUS_TRASH = "trash"
    }
}
