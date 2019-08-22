package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.CountryViewsRestClient.CountryViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.FileDownloadsRestClient.FileDownloadsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient.VideoPlaysResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient.VisitsAndViewsResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.network.utils.StatsGranularity.DAYS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.MONTHS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.WEEKS
import org.wordpress.android.fluxc.network.utils.StatsGranularity.YEARS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.AUTHORS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.CLICKS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.COUNTRY_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.FILE_DOWNLOADS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTS_AND_PAGES_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.REFERRERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.SEARCH_TERMS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.VIDEO_PLAYS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.VISITS_AND_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import java.util.Date
import javax.inject.Inject

open class TimeStatsSqlUtils<RESPONSE_TYPE>(
    private val statsSqlUtils: StatsSqlUtils,
    private val statsUtils: StatsUtils,
    private val statsRequestSqlUtils: StatsRequestSqlUtils,
    private val blockType: BlockType,
    private val classOfResponse: Class<RESPONSE_TYPE>
) {
    fun insert(
        site: SiteModel,
        data: RESPONSE_TYPE,
        granularity: StatsGranularity,
        date: Date,
        requestedItems: Int? = null
    ) {
        insert(site, data, granularity, statsUtils.getFormattedDate(date), requestedItems)
    }

    fun insert(
        site: SiteModel,
        data: RESPONSE_TYPE,
        granularity: StatsGranularity,
        formattedDate: String,
        requestedItems: Int?
    ) {
        statsSqlUtils.insert(site, blockType, granularity.toStatsType(), data, true, formattedDate)
        statsRequestSqlUtils.insert(
                site,
                blockType,
                granularity.toStatsType(),
                requestedItems,
                formattedDate
        )
    }

    fun select(site: SiteModel, granularity: StatsGranularity, date: Date): RESPONSE_TYPE? {
        return select(site, granularity, statsUtils.getFormattedDate(date))
    }

    fun select(site: SiteModel, granularity: StatsGranularity, date: String): RESPONSE_TYPE? {
        return statsSqlUtils.select(
                site,
                blockType,
                granularity.toStatsType(),
                classOfResponse,
                date
        )
    }

    fun hasFreshRequest(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        requestedItems: Int? = null
    ): Boolean {
        return hasFreshRequest(site,
                granularity,
                statsUtils.getFormattedDate(date),
                requestedItems
        )
    }

    fun hasFreshRequest(
        site: SiteModel,
        granularity: StatsGranularity,
        date: String,
        requestedItems: Int? = null
    ): Boolean {
        return statsRequestSqlUtils.hasFreshRequest(
                site,
                blockType,
                granularity.toStatsType(),
                requestedItems,
                date = date
        )
    }

    class PostsAndPagesSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<PostAndPageViewsResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            POSTS_AND_PAGES_VIEWS,
            PostAndPageViewsResponse::class.java
    )

    class ReferrersSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<ReferrersResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            REFERRERS,
            ReferrersResponse::class.java
    )

    class ClicksSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<ClicksResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            CLICKS,
            ClicksResponse::class.java
    )

    class VisitsAndViewsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<VisitsAndViewsResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            VISITS_AND_VIEWS,
            VisitsAndViewsResponse::class.java
    )

    class CountryViewsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<CountryViewsResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            COUNTRY_VIEWS,
            CountryViewsResponse::class.java
    )

    class AuthorsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<AuthorsResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            AUTHORS,
            AuthorsResponse::class.java
    )

    class SearchTermsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<SearchTermsResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            SEARCH_TERMS,
            SearchTermsResponse::class.java
    )

    class VideoPlaysSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<VideoPlaysResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            VIDEO_PLAYS,
            VideoPlaysResponse::class.java
    )

    class FileDownloadsSqlUtils
    @Inject constructor(
        statsSqlUtils: StatsSqlUtils,
        statsUtils: StatsUtils,
        statsRequestSqlUtils: StatsRequestSqlUtils
    ) : TimeStatsSqlUtils<FileDownloadsResponse>(
            statsSqlUtils,
            statsUtils,
            statsRequestSqlUtils,
            FILE_DOWNLOADS,
            FileDownloadsResponse::class.java
    )

    private fun StatsGranularity.toStatsType(): StatsType {
        return when (this) {
            DAYS -> StatsType.DAY
            WEEKS -> StatsType.WEEK
            MONTHS -> StatsType.MONTH
            YEARS -> StatsType.YEAR
        }
    }
}
