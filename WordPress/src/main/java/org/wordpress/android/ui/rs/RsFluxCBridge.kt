package org.wordpress.android.ui.rs

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.PostStore
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostRetrieveParams
import uniffi.wp_api.RequestExecutionErrorReason
import uniffi.wp_api.WpErrorCode
import javax.inject.Inject

/**
 * Bridges a wordpress-rs post or page into FluxC's database so the editor, which reads from FluxC,
 * can open it. Returns the cached row when it is fresh, otherwise fetches and inserts it.
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
     * A row whose [lastModified] differs is re-fetched, unless it holds unsynced local edits:
     * FluxC won't overwrite those, and the editor must not lose them.
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
            // A row cached as the other type would open in the wrong editor.
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
        if (response !is WpRequestResult.Success) {
            throw when (response) {
                is WpRequestResult.WpError ->
                    RsBridgeException(response.errorMessage, errorCode = response.errorCode)
                is WpRequestResult.RequestExecutionFailed ->
                    RsBridgeException("Failed to fetch $kind", reason = response.reason)
                else -> RsBridgeException("Failed to fetch $kind")
            }
        }

        val model = mapper.map(response.response.data, site).apply { setIsPage(isPage) }
        postSqlUtils.insertOrUpdatePost(model, false)

        // Re-read to get the auto-assigned local ID
        return postStore.getPostByRemotePostId(remoteId, site)
            ?: error("${kind.replaceFirstChar { it.uppercase() }} inserted but not found in FluxC")
    }
}

/**
 * A post or page the bridge couldn't fetch. Keeps what rs reported, so
 * [RsErrorUtils.friendlyErrorMessage] can tell a dropped connection from a rejected credential.
 */
class RsBridgeException(
    message: String,
    val reason: RequestExecutionErrorReason? = null,
    val errorCode: WpErrorCode? = null,
) : IllegalStateException(message)
