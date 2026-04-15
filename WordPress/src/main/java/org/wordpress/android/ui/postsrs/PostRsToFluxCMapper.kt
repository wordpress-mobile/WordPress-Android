package org.wordpress.android.ui.postsrs

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.util.DateTimeUtils
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus
import uniffi.wp_api.TermEndpointType
import javax.inject.Inject

/**
 * Maps a wordpress-rs [AnyPostWithEditContext] to a FluxC [PostModel]
 * so the editor can load it from FluxC's local database.
 */
class PostRsToFluxCMapper @Inject constructor(
    private val restClient: PostRsRestClient,
) {
    suspend fun map(
        post: AnyPostWithEditContext,
        site: SiteModel
    ): PostModel {
        val modified = DateTimeUtils.iso8601UTCFromDate(
            post.modifiedGmt
        )
        return PostModel().apply {
            setRemotePostId(post.id)
            setLocalSiteId(site.id)
            setRemoteSiteId(site.siteId)

            setTitle(
                post.title?.raw?.takeIf { it.isNotBlank() }
                    ?: post.title?.rendered ?: ""
            )
            setContent(
                post.content.raw?.takeIf { it.isNotBlank() }
                    ?: post.content.rendered
            )
            setExcerpt(
                post.excerpt?.raw?.takeIf { it.isNotBlank() }
                    ?: post.excerpt?.rendered ?: ""
            )

            setStatus(mapStatus(post.status))
            setLink(post.link)
            setSlug(post.slug)
            setPassword(post.password ?: "")
            setSticky(post.sticky ?: false)

            setAuthorId(post.author ?: 0L)
            setFeaturedImageId(post.featuredMedia ?: 0L)
            setPostFormat(mapFormat(post.format))

            setDateCreated(
                DateTimeUtils.iso8601UTCFromDate(post.dateGmt)
            )
            setLastModified(modified)
            setRemoteLastModified(modified)

            setCategoryIdList(post.categories ?: emptyList())
            setTagNameList(resolveTagNames(post.tags, site))

            setIsPage(false)
            setIsLocalDraft(false)
            setIsLocallyChanged(false)
        }
    }

    private suspend fun resolveTagNames(
        ids: List<Long>?,
        site: SiteModel
    ): List<String> {
        if (ids.isNullOrEmpty()) return emptyList()
        val nameMap = restClient.fetchTermNames(
            site, ids, TermEndpointType.Tags
        )
        return ids.mapNotNull { nameMap[it] }
    }

    private fun mapStatus(status: PostStatus?): String =
        when (status) {
            is PostStatus.Publish -> "publish"
            is PostStatus.Draft -> "draft"
            is PostStatus.Pending -> "pending"
            is PostStatus.Private -> "private"
            is PostStatus.Future -> "future"
            is PostStatus.Trash -> "trash"
            else -> "draft"
        }

    private fun mapFormat(format: PostFormat?): String =
        when (format) {
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
}
