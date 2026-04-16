package org.wordpress.android.ui.postsrs

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
 * Bridges a wordpress-rs post into FluxC's local SQLite database
 * so the editor (which reads from FluxC) can open it.
 *
 * **Fast path**: if the post is already in FluxC's DB, returns it
 * immediately without a network call.
 *
 * **Slow path**: fetches the full post via wordpress-rs, maps it
 * to a [PostModel], inserts it into FluxC's DB, and re-reads it
 * to obtain the auto-assigned local ID.
 */
class PostRsFluxCBridge @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val postStore: PostStore,
    private val postSqlUtils: PostSqlUtils,
    private val mapper: PostRsToFluxCMapper,
) {
    /**
     * Returns a [PostModel] for [remotePostId] that is guaranteed
     * to exist in FluxC's local database. If [lastModified] is
     * provided and differs from the cached row's
     * `remoteLastModified`, the cache is considered
     * stale and the post is re-fetched from the server.
     *
     * @throws IllegalStateException if the post cannot be fetched or inserted.
     */
    suspend fun fetchAndBridge(
        remotePostId: Long,
        site: SiteModel,
        lastModified: String? = null
    ): PostModel {
        // Fast path — already in FluxC and still fresh
        postStore.getPostByRemotePostId(remotePostId, site)?.let { cached ->
            if (lastModified == null ||
                lastModified == cached.remoteLastModified
            ) {
                return cached
            }
        }

        // Slow path — fetch via wordpress-rs
        val client = wpApiClientProvider.getWpApiClient(site)
        val response = client.request {
            it.posts().retrieveWithEditContext(
                PostEndpointType.Posts,
                remotePostId,
                PostRetrieveParams()
            )
        }
        val rsPost = when (response) {
            is WpRequestResult.Success -> response.response.data
            else -> {
                val msg = (response as? WpRequestResult.WpError<*>)
                    ?.errorMessage ?: "Failed to fetch post"
                throw IllegalStateException(msg)
            }
        }

        // Map and insert
        val postModel = mapper.map(rsPost, site)
        postSqlUtils.insertOrUpdatePost(postModel, false)

        // Re-read to get the auto-assigned local ID
        return postStore.getPostByRemotePostId(remotePostId, site)
            ?: error("Post inserted but not found in FluxC")
    }
}
