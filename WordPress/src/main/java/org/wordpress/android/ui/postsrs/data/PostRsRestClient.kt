package org.wordpress.android.ui.postsrs.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.postsrs.models.PostRsModel
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostListParams
import uniffi.wp_api.PostStatus
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpApiParamPostsOrderBy
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REST client for fetching posts via wordpress-rs.
 */
@Singleton
class PostRsRestClient @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
) {
    suspend fun fetchPosts(
        site: SiteModel,
        statuses: List<PostStatus>,
        offset: Int = 0,
        order: WpApiParamOrder = WpApiParamOrder.DESC
    ): PostRsListResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.posts().listWithEditContext(
                postEndpointType = PostEndpointType.Posts,
                params = PostListParams(
                    perPage = PAGE_SIZE,
                    offset = offset.toUInt(),
                    status = statuses,
                    order = order,
                    orderby = WpApiParamPostsOrderBy.DATE
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                val posts = response.response.data
                appLogWrapper.d(
                    AppLog.T.API,
                    "Fetched ${posts.size} posts"
                )
                val models = posts.map { post ->
                    val title =
                        post.title?.raw?.takeIf { it.isNotBlank() }
                            ?: post.title?.rendered
                            ?: ""
                    val excerpt =
                        post.excerpt?.raw?.takeIf { it.isNotBlank() }
                            ?: post.excerpt?.rendered
                            ?: ""
                    PostRsModel(
                        remotePostId = post.id,
                        title = title,
                        excerpt = excerpt,
                        date = post.date,
                        status = post.status
                    )
                }
                val canLoadMore =
                    posts.size.toUInt() == PAGE_SIZE
                PostRsListResult.Success(models, canLoadMore)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(
                    AppLog.T.API,
                    "Failed to fetch posts: $errorMessage"
                )
                PostRsListResult.Error(errorMessage)
            }
        }
    }

    private fun parseErrorMessage(
        response: WpRequestResult<*>
    ): String {
        appLogWrapper.e(AppLog.T.API, "API error: $response")

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return context.getString(R.string.no_network_message)
        }

        return when (response) {
            is WpRequestResult.WpError<*> ->
                response.errorMessage
                    .takeIf { it.isNotBlank() }
                    ?: context.getString(
                        R.string.request_failed_message
                    )
            else -> context.getString(
                R.string.request_failed_message
            )
        }
    }

    // ========== Result Types ==========

    sealed class PostRsListResult {
        data class Success(
            val posts: List<PostRsModel>,
            val canLoadMore: Boolean
        ) : PostRsListResult()

        data class Error(
            val message: String
        ) : PostRsListResult()
    }

    companion object {
        const val PAGE_SIZE = 20u
    }
}
