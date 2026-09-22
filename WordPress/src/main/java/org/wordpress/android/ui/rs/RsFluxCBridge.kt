package org.wordpress.android.ui.rs

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
 * Bridges a wordpress-rs post or page into FluxC's local SQLite database so the editor, which
 * reads from FluxC, can open it.
 *
 * **Fast path**: if the item is already in FluxC's DB as the right kind and is not known to be
 * stale, it is returned without a network call.
 *
 * **Slow path**: fetches the full item via wordpress-rs, maps it to a [PostModel], inserts it, and
 * re-reads it to pick up the local id FluxC assigned.
 */
class RsFluxCBridge @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val postStore: PostStore,
    private val postSqlUtils: PostSqlUtils,
    private val mapper: RsToFluxCMapper,
) {
    /** @throws IllegalStateException if the post cannot be fetched or inserted. */
    suspend fun fetchAndBridgePost(
        remoteId: Long,
        site: SiteModel,
        lastModified: String? = null
    ): PostModel = bridge(remoteId, site, lastModified, isPage = false)

    /** @throws IllegalStateException if the page cannot be fetched or inserted. */
    suspend fun fetchAndBridgePage(
        remoteId: Long,
        site: SiteModel,
        lastModified: String? = null
    ): PostModel = bridge(remoteId, site, lastModified, isPage = true)

    /**
     * Returns a [PostModel] for [remoteId] that exists in FluxC's local database with the matching
     * `isPage` flag.
     *
     * When [lastModified] is given and differs from the cached row's `remoteLastModified` the cache
     * is stale and the item is re-fetched - unless the row holds unsynced local edits, which are
     * returned as-is. FluxC will not overwrite a locally-changed row, so fetching would be wasted
     * and would risk losing what the editor has not saved yet.
     */
    private suspend fun bridge(
        remoteId: Long,
        site: SiteModel,
        lastModified: String?,
        isPage: Boolean
    ): PostModel {
        val kind = if (isPage) "page" else "post"

        postStore.getPostByRemotePostId(remoteId, site)?.let { cached ->
            val fresh = lastModified == null || lastModified == cached.remoteLastModified
            // The kind has to match: a row cached as the other type would open in the wrong
            // editor. Remote ids do not collide across types, so this should never reject a row.
            if (cached.isPage == isPage && (fresh || cached.isLocallyChanged)) {
                return cached
            }
        }

        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.posts().retrieveWithEditContext(
                if (isPage) PostEndpointType.Pages else PostEndpointType.Posts,
                remoteId,
                PostRetrieveParams()
            )
        }
        val fetched = when (response) {
            is WpRequestResult.Success -> response.response.data
            else -> {
                val msg = (response as? WpRequestResult.WpError<*>)?.errorMessage
                    ?: "Failed to fetch $kind"
                throw IllegalStateException(msg)
            }
        }

        val model = mapper.map(fetched, site).apply { setIsPage(isPage) }
        postSqlUtils.insertOrUpdatePost(model, false)

        // Re-read to get the auto-assigned local ID
        return postStore.getPostByRemotePostId(remoteId, site)
            ?: error("${kind.replaceFirstChar { it.uppercase() }} inserted but not found in FluxC")
    }
}
