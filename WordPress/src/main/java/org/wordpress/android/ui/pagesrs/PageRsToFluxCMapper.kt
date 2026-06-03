package org.wordpress.android.ui.pagesrs

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.postsrs.PostRsToFluxCMapper
import uniffi.wp_api.AnyPostWithEditContext
import javax.inject.Inject

/**
 * Maps a wordpress-rs [AnyPostWithEditContext] (representing a page) to a
 * FluxC [PostModel] with `isPage = true`, so the editor can load it from
 * FluxC's local database.
 *
 * Pages share the post type surface in wordpress-rs (no distinct
 * `AnyPageWithEditContext`), so this delegates to [PostRsToFluxCMapper] and
 * flips the page flag on the result.
 */
internal class PageRsToFluxCMapper @Inject constructor(
    private val postMapper: PostRsToFluxCMapper,
) {
    suspend fun map(
        page: AnyPostWithEditContext,
        site: SiteModel
    ): PostModel = postMapper.map(page, site).apply {
        setIsPage(true)
    }
}
