package org.wordpress.android.ui.rs

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
    suspend fun map(
        post: AnyPostWithEditContext,
        site: SiteModel
    ): PostModel = postMapper.applyTo(
        target = PostModel(),
        post = post,
        site = site,
        tagNames = resolveTagNames(post.tags, site),
        isPage = false
    ).apply {
        setIsLocalDraft(false)
        setIsLocallyChanged(false)
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
}
