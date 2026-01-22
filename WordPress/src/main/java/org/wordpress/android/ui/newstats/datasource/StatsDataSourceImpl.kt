package org.wordpress.android.ui.newstats.datasource

import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.ui.newstats.extension.statsCommentsData
import org.wordpress.android.ui.newstats.extension.statsLikesData
import org.wordpress.android.ui.newstats.extension.statsPostsData
import org.wordpress.android.ui.newstats.extension.statsVisitorsData
import org.wordpress.android.ui.newstats.extension.statsVisitsData
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.StatsVisitsParams
import uniffi.wp_api.StatsVisitsUnit
import javax.inject.Inject

/**
 * Implementation of [StatsDataSource] that fetches stats data from the WordPress.com API
 * using the wordpress-rs library.
 */
class StatsDataSourceImpl @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider
) : StatsDataSource {
    /**
     * Access token for API authentication.
     * Marked as @Volatile to ensure visibility across threads since this data source is accessed
     * from multiple coroutine contexts.
     */
    @Volatile
    private var accessToken: String? = null

    private val wpComApiClient: WpComApiClient by lazy {
        check(accessToken != null) { "DataSource not initialized" }
        wpComApiClientProvider.getWpComApiClient(accessToken!!)
    }

    override fun init(accessToken: String) {
        this.accessToken = accessToken
    }

    override suspend fun fetchStatsVisits(
        siteId: Long,
        unit: StatsUnit,
        quantity: Int,
        endDate: String
    ): StatsVisitsDataResult {
        if (accessToken == null) {
            return StatsVisitsDataResult.Error("DataSource not initialized")
        }

        val params = StatsVisitsParams(
            unit = unit.toApiUnit(),
            quantity = quantity.toUInt(),
            endDate = endDate
        )

        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsVisits().getStatsVisits(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        return when (result) {
            is WpRequestResult.Success -> {
                StatsVisitsDataResult.Success(mapResponseToStatsVisitsData(result.response.data))
            }
            is WpRequestResult.WpError -> StatsVisitsDataResult.Error(result.errorMessage)
            else -> StatsVisitsDataResult.Error("Unknown error")
        }
    }

    private fun mapResponseToStatsVisitsData(
        response: uniffi.wp_api.StatsVisitsResponse
    ): StatsVisitsData = StatsVisitsData(
        visits = response.statsVisitsData().map { VisitsDataPoint(it.period, it.visits.toLong()) },
        visitors = response.statsVisitorsData().map { VisitorsDataPoint(it.period, it.visitors.toLong()) },
        likes = response.statsLikesData().map { LikesDataPoint(it.period, it.likes.toLong()) },
        comments = response.statsCommentsData().map { CommentsDataPoint(it.period, it.comments.toLong()) },
        posts = response.statsPostsData().map { PostsDataPoint(it.period, it.posts.toLong()) }
    )

    private fun StatsUnit.toApiUnit(): StatsVisitsUnit = when (this) {
        StatsUnit.HOUR -> StatsVisitsUnit.HOUR
        StatsUnit.DAY -> StatsVisitsUnit.DAY
    }
}
