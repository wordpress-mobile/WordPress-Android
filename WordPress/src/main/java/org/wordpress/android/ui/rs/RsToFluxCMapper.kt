package org.wordpress.android.ui.rs

import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.post.RsPostMapper
import org.wordpress.android.ui.rs.data.RsSiteRestClient
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.TermEndpointType
import javax.inject.Inject

/**
 * Maps a wordpress-rs [AnyPostWithEditContext] to a FluxC [PostModel]
 * so the editor can load it from FluxC's local database.
 *
 * The field-by-field mapping lives in [RsPostMapper], inside FluxC, because the FluxC REST post
 * client needs the same translation when it writes a post. All this adds is resolving the tag
 * *names* the editor shows from the tag ids the payload carries.
 */
class RsToFluxCMapper @Inject constructor(
    private val restClient: RsSiteRestClient,
    private val postMapper: RsPostMapper,
) {
    /**
     * [isPage] has to be known here rather than set on the result: [RsPostMapper.applyTo] only
     * copies the parent across for a page, so passing `false` for one would leave `parentId` at 0
     * and the next save would detach the page from its parent.
     *
     * @throws RsBridgeException if the post's tag names cannot be read. See [resolveTagNames].
     */
    suspend fun map(
        post: AnyPostWithEditContext,
        site: SiteModel,
        isPage: Boolean
    ): PostModel = postMapper.applyTo(
        target = PostModel(),
        post = post,
        site = site,
        tagNames = resolveTagNames(post.tags, site),
        isPage = isPage
    ).apply {
        setIsLocalDraft(false)
        setIsLocallyChanged(false)
    }

    /**
     * wp/v2 returns a term name as WordPress stores it, which is HTML-encoded: a tag typed `AT&T`
     * comes back as `AT&amp;T`. Decoding it here keeps [PostModel.tagNameList] holding the text the
     * author actually typed, which is what the editor shows and what the REST post client encodes
     * again when it looks the tag up by name on save.
     *
     * A lookup that failed aborts the bridge rather than mapping the post with the names it did
     * read. The row the bridge inserts is what the editor then saves, and wp/v2 replaces a post's
     * whole tag set, so a short list - or no list at all - would delete the missing tags from the
     * site on the next save. Failing here surfaces a retryable error instead, which is what the
     * bridge already does when the post itself cannot be fetched.
     */
    private suspend fun resolveTagNames(
        ids: List<Long>?,
        site: SiteModel
    ): List<String> {
        if (ids.isNullOrEmpty()) return emptyList()
        val nameMap = restClient.fetchTermNames(
            site, ids, TermEndpointType.Tags
        ) ?: throw RsBridgeException("Failed to fetch the tag names of a post with ${ids.size} tags")
        return ids.mapNotNull { id ->
            nameMap[id]?.let { StringEscapeUtils.unescapeHtml4(it) }
        }
    }
}
