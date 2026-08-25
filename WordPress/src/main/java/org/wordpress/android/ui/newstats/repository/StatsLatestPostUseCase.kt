package org.wordpress.android.ui.newstats.repository

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.newstats.datasource.LatestPostDataSource
import org.wordpress.android.ui.newstats.datasource.LatestPostLookupResult
import org.wordpress.android.ui.newstats.datasource.PostViewsData
import javax.inject.Inject

/**
 * Loads the site's newest published post together with its view stats.
 *
 * Two calls: the post ID comes from the site's REST API, the stats from WP.com. There is no
 * caching here -- [org.wordpress.android.ui.newstats.latestpost.LatestPostViewModel] is the only
 * consumer and already tracks whether it has loaded.
 */
class StatsLatestPostUseCase @Inject constructor(
    private val statsRepository: StatsRepository,
    private val latestPostDataSource: LatestPostDataSource,
    private val accountStore: AccountStore
) {
    suspend operator fun invoke(
        site: SiteModel
    ): LatestPostResult {
        val token = accountStore.accessToken
        if (token.isNullOrEmpty()) {
            return LatestPostResult.Error("No access token")
        }
        statsRepository.init(token)

        return when (
            val lookup = latestPostDataSource
                .fetchLatestPublishedPost(site)
        ) {
            is LatestPostLookupResult.NoPosts ->
                LatestPostResult.NoPosts
            is LatestPostLookupResult.Error ->
                LatestPostResult.Error(lookup.message)
            is LatestPostLookupResult.Success ->
                when (
                    val views = statsRepository
                        .fetchPostViews(
                            siteId = site.siteId,
                            postId = lookup.postId
                        )
                ) {
                    is PostViewsResult.Success ->
                        LatestPostResult.Success(
                            data = views.data,
                            featuredImageUrl =
                                lookup.featuredImageUrl
                        )
                    is PostViewsResult.Error ->
                        LatestPostResult.Error(views.message)
                }
        }
    }
}

/**
 * Result of loading the latest post's stats. [NoPosts] means the site has nothing published yet.
 */
sealed class LatestPostResult {
    data class Success(
        val data: PostViewsData,
        /** Null when the post has no featured image. */
        val featuredImageUrl: String?
    ) : LatestPostResult()

    data object NoPosts : LatestPostResult()

    data class Error(
        val message: String
    ) : LatestPostResult()
}
