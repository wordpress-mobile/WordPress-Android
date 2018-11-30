package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTS_AND_PAGES_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.REFERRERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeStatsSqlUtils
@Inject constructor(private val statsSqlUtils: StatsSqlUtils) {
    fun insert(site: SiteModel, data: PostAndPageViewsResponse, period: StatsGranularity) {
        statsSqlUtils.insert(site, POSTS_AND_PAGES_VIEWS, period.toStatsType(), data)
    }

    fun insert(site: SiteModel, data: ReferrersResponse, period: StatsGranularity) {
        statsSqlUtils.insert(site, REFERRERS, period.toStatsType(), data)
    }

    fun selectPostAndPageViews(site: SiteModel, period: StatsGranularity): PostAndPageViewsResponse? {
        return statsSqlUtils.select(
                site,
                POSTS_AND_PAGES_VIEWS,
                period.toStatsType(),
                PostAndPageViewsResponse::class.java
        )
    }

    fun selectReferrers(site: SiteModel, period: StatsGranularity): ReferrersResponse? {
        return statsSqlUtils.select(
                site,
                REFERRERS,
                period.toStatsType(),
                ReferrersResponse::class.java
        )
    }

    private fun StatsGranularity.toStatsType(): StatsType {
        return when (this) {
            DAYS -> StatsType.DAY
            WEEKS -> StatsType.WEEK
            MONTHS -> StatsType.MONTH
            YEARS -> StatsType.YEAR
        }
    }
}
