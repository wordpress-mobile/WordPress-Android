package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.CountryViewsRestClient.CountryViewsResponse
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
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.AUTHORS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.CLICKS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.COUNTRY_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.POSTS_AND_PAGES_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.REFERRERS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.SEARCH_TERMS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.VIDEO_PLAYS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.BlockType.VISITS_AND_VIEWS
import org.wordpress.android.fluxc.persistence.StatsSqlUtils.StatsType
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeStatsSqlUtils
@Inject constructor(private val statsSqlUtils: StatsSqlUtils, private val statsUtils: StatsUtils) {
    fun insert(site: SiteModel, data: PostAndPageViewsResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                POSTS_AND_PAGES_VIEWS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun insert(site: SiteModel, data: ReferrersResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                REFERRERS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun insert(site: SiteModel, data: ClicksResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                CLICKS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun insert(site: SiteModel, data: VisitsAndViewsResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                VISITS_AND_VIEWS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun insert(site: SiteModel, data: CountryViewsResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                COUNTRY_VIEWS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun insert(site: SiteModel, data: AuthorsResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                AUTHORS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun insert(site: SiteModel, data: SearchTermsResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                SEARCH_TERMS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun insert(site: SiteModel, data: VideoPlaysResponse, granularity: StatsGranularity, date: Date) {
        statsSqlUtils.insert(
                site,
                VIDEO_PLAYS,
                granularity.toStatsType(),
                data,
                true,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectPostAndPageViews(site: SiteModel, granularity: StatsGranularity, date: Date): PostAndPageViewsResponse? {
        return statsSqlUtils.select(
                site,
                POSTS_AND_PAGES_VIEWS,
                granularity.toStatsType(),
                PostAndPageViewsResponse::class.java,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectReferrers(site: SiteModel, granularity: StatsGranularity, date: Date): ReferrersResponse? {
        return statsSqlUtils.select(
                site,
                REFERRERS,
                granularity.toStatsType(),
                ReferrersResponse::class.java,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectClicks(site: SiteModel, granularity: StatsGranularity, date: Date): ClicksResponse? {
        return statsSqlUtils.select(
                site,
                CLICKS,
                granularity.toStatsType(),
                ClicksResponse::class.java,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectVisitsAndViews(site: SiteModel, granularity: StatsGranularity, date: Date): VisitsAndViewsResponse? {
        return statsSqlUtils.select(
                site,
                VISITS_AND_VIEWS,
                granularity.toStatsType(),
                VisitsAndViewsResponse::class.java,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectCountryViews(site: SiteModel, granularity: StatsGranularity, date: Date): CountryViewsResponse? {
        return statsSqlUtils.select(
                site,
                COUNTRY_VIEWS,
                granularity.toStatsType(),
                CountryViewsResponse::class.java,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectAuthors(site: SiteModel, granularity: StatsGranularity, date: Date): AuthorsResponse? {
        return statsSqlUtils.select(
                site,
                AUTHORS,
                granularity.toStatsType(),
                AuthorsResponse::class.java,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectSearchTerms(site: SiteModel, granularity: StatsGranularity, date: Date): SearchTermsResponse? {
        return statsSqlUtils.select(
                site,
                SEARCH_TERMS,
                granularity.toStatsType(),
                SearchTermsResponse::class.java,
                statsUtils.getFormattedDate(date)
        )
    }

    fun selectVideoPlays(site: SiteModel, granularity: StatsGranularity, date: Date): VideoPlaysResponse? {
        return statsSqlUtils.select(
                site,
                VIDEO_PLAYS,
                granularity.toStatsType(),
                VideoPlaysResponse::class.java,
                statsUtils.getFormattedDate(date)
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
