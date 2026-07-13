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
    suspend fun fetchXPosts(site: SiteModel): XPostsResult =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchXPosts") {
                // XPosts are an o2/P2 (WP for Teams) feature. Non-teams sites always fail the
                // request with `xposts_require_o2_enabled` (HTTP 400), so skip the network call
                // entirely and record the definitive "no xposts" state.
                if (!site.isWpForTeamsSite) {
                    xPostsSqlUtils.persistNoXpostsForSite(site)
                    return@withDefaultContext XPostsResult.apiResult(emptyList())
                }
                return@withDefaultContext when (val response = xPostsRestClient.fetch(site)) {
                    is Response.Success -> {
                        val xPosts = response.data.toList()
                        xPostsSqlUtils.setXPostsForSite(xPosts, site)
                        XPostsResult.apiResult(xPosts)
                    }
                    is Response.Error -> when (response.error.apiError) {
                        "unauthorized",
                        "xposts_require_o2_enabled" -> {
                            // These errors mean the site does not support xposts for this user
                            xPostsSqlUtils.persistNoXpostsForSite(site)
                            XPostsResult.apiResult(emptyList())
                        }
                        else -> {
                            // Call failed for unknown reason, leave db unchanged and return saved data
                            savedXPosts(site)
                        }
                    }
                }
            }

    suspend fun getXPostsFromDb(site: SiteModel): XPostsResult =
        coroutineEngine.withDefaultContext(AppLog.T.DB, this, "getXPostsFromDb") {
            return@withDefaultContext savedXPosts(site)
        }

    /**
     * Returns either (a) a list of XPosts from the db wrapped in an [XPostsResult.Result], or
     * (b) [XPostsResult.Unknown] if we have never gotten a response from the backend indicating
     * whether this site has any XPosts.
     */
    private fun savedXPosts(site: SiteModel) =
        xPostsSqlUtils.selectXPostsForSite(site)?.let {
            XPostsResult.dbResult(it)
        } ?: XPostsResult.Unknown
}

enum class XPostsSource { REST_API, DB }

sealed class XPostsResult {
    data class Result(val xPosts: List<XPostSiteModel>, val source: XPostsSource) : XPostsResult()
    object Unknown : XPostsResult()
    companion object {
        fun dbResult(xPosts: List<XPostSiteModel>) = Result(xPosts, DB)
        fun apiResult(xPosts: List<XPostSiteModel>) = Result(xPosts, REST_API)
    }
}
