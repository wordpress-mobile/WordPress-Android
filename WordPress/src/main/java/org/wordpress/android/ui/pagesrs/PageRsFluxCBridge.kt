package org.wordpress.android.ui.pagesrs

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.PostStore
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostRetrieveParams
import javax.inject.Inject

/**
 * Bridges a wordpress-rs page into FluxC's local SQLite database so the
 * editor (which reads from FluxC) can open it. Mirrors
 * [org.wordpress.android.ui.postsrs.PostRsFluxCBridge] but targets
 * [PostEndpointType.Pages] and ensures the resulting [PostModel] has
 * `isPage = true`.
 *
 * **Fast path**: if the page is already in FluxC's DB (with `isPage = true`)
 * and not known to be stale, returns it immediately without a network call.
 *
 * **Slow path**: fetches the full page via wordpress-rs, maps it to a
 * [PostModel], inserts it into FluxC's DB, and re-reads it to obtain the
 * auto-assigned local ID.
 */
class PageRsFluxCBridge @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val postStore: PostStore,
    private val postSqlUtils: PostSqlUtils,
    private val mapper: PageRsToFluxCMapper,
) {
    /**
     * Returns a [PostModel] for [remotePageId] that is guaranteed to exist
     * in FluxC's local database with `isPage = true`. If [lastModified] is
     * provided and differs from the cached row's `remoteLastModified`, the
     * cache is considered stale and the page is re-fetched from the server.
     *
     * @throws IllegalStateException if the page cannot be fetched or inserted.
     */
    suspend fun fetchAndBridge(
        remotePageId: Long,
        site: SiteModel,
        lastModified: String? = null
    ): PostModel {
        // Fast path — already in FluxC, marked as a page, and still fresh
        postStore.getPostByRemotePostId(remotePageId, site)?.let { cached ->
            val fresh = lastModified == null ||
                    lastModified == cached.remoteLastModified
            if (cached.isPage && fresh) {
                return cached
            }
        }

        // Slow path — fetch via wordpress-rs (Pages endpoint)
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.posts().retrieveWithEditContext(
                PostEndpointType.Pages,
                remotePageId,
                PostRetrieveParams()
            )
        }
        val rsPage = when (response) {
            is WpRequestResult.Success -> response.response.data
            else -> {
                val msg = (response as? WpRequestResult.WpError<*>)
                    ?.errorMessage ?: "Failed to fetch page"
                throw IllegalStateException(msg)
            }
        }

        // Map and insert (mapper sets isPage = true)
        val pageModel = mapper.map(rsPage, site)
        postSqlUtils.insertOrUpdatePost(pageModel, false)

        // Re-read to get the auto-assigned local ID
        return postStore.getPostByRemotePostId(remotePageId, site)
            ?: error("Page inserted but not found in FluxC")
    }
}
