package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.XPostSiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.site.XPostsRestClient
import org.wordpress.android.fluxc.persistence.XPostsSqlUtils
import org.wordpress.android.fluxc.store.XPostsSource.DB
import org.wordpress.android.fluxc.store.XPostsSource.REST_API
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class XPostsStore
@Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val xPostsRestClient: XPostsRestClient,
    private val xPostsSqlUtils: XPostsSqlUtils
) {
    suspend fun fetchXPosts(site: SiteModel): FetchXpostsResult =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchXPosts") {
                return@withDefaultContext when (val response = xPostsRestClient.fetch(site)) {
                    is Response.Success -> {
                        val xPosts = response.data.toList()
                        xPostsSqlUtils.insertOrUpdateXPost(xPosts, site)
                        FetchXpostsResult(xPosts, REST_API)
                    }
                    is Response.Error -> {
                        val xPosts = xPostsSqlUtils.selectXPostsForSite(site)
                        FetchXpostsResult(xPosts, DB)
                    }
                }
            }
}

enum class XPostsSource { REST_API, DB }

data class FetchXpostsResult constructor(
    val xPosts: List<XPostSiteModel>,
    val source: XPostsSource
)
