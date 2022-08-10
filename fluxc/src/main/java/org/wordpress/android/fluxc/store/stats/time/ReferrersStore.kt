package org.wordpress.android.fluxc.store.stats.time

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.time.TimeStatsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import org.wordpress.android.fluxc.persistence.TimeStatsSqlUtils.ReferrersSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnReportReferrerAsSpam
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReferrersStore @Inject constructor(
    private val restClient: ReferrersRestClient,
    private val sqlUtils: ReferrersSqlUtils,
    private val timeStatsMapper: TimeStatsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchReferrers(
        site: SiteModel,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date,
        forced: Boolean = false
    ) = coroutineEngine.withDefaultContext(STATS, this, "fetchReferrers") {
        if (!forced && sqlUtils.hasFreshRequest(site, granularity, date, limitMode.limit)) {
            return@withDefaultContext OnStatsFetched(getReferrers(site, granularity, limitMode, date), cached = true)
        }
        val payload = restClient.fetchReferrers(site, granularity, date, limitMode.limit + 1, forced)
        return@withDefaultContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response, granularity, date, limitMode.limit)
                OnStatsFetched(timeStatsMapper.map(payload.response, limitMode))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getReferrers(site: SiteModel, granularity: StatsGranularity, limitMode: Top, date: Date) =
            coroutineEngine.run(STATS, this, "getReferrers") {
                sqlUtils.select(site, granularity, date)?.let { timeStatsMapper.map(it, limitMode) }
            }

    suspend fun reportReferrerAsSpam(
        site: SiteModel,
        domain: String,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date
    ) = coroutineEngine.withDefaultContext(STATS, this, "reportReferrerAsSpam") {
        val payload = restClient.reportReferrerAsSpam(site, domain)

        if (payload.response != null || payload.error.type == StatsErrorType.ALREADY_SPAMMED) {
            updateCacheWithMarkedSpam(site, granularity, date, domain, limitMode, true)
        }
        return@withDefaultContext when {
            payload.isError -> OnReportReferrerAsSpam(payload.error)
            payload.response != null -> OnReportReferrerAsSpam(payload.response)
            else -> OnReportReferrerAsSpam(StatsError(INVALID_RESPONSE))
        }
    }

    suspend fun unreportReferrerAsSpam(
        site: SiteModel,
        domain: String,
        granularity: StatsGranularity,
        limitMode: Top,
        date: Date
    ) = coroutineEngine.withDefaultContext(STATS, this, "unreportReferrerAsSpam") {
        val payload = restClient.unreportReferrerAsSpam(site, domain)

        if (payload.response != null || payload.error.type == StatsErrorType.ALREADY_SPAMMED) {
            updateCacheWithMarkedSpam(site, granularity, date, domain, limitMode, false)
        }
        return@withDefaultContext when {
            payload.isError -> OnReportReferrerAsSpam(payload.error)
            payload.response != null -> OnReportReferrerAsSpam(payload.response)
            else -> OnReportReferrerAsSpam(StatsError(INVALID_RESPONSE))
        }
    }

    @Suppress("LongParameterList")
    private fun updateCacheWithMarkedSpam(
        site: SiteModel,
        granularity: StatsGranularity,
        date: Date,
        domain: String,
        limitMode: Top,
        spam: Boolean
    ) {
        val currentModel = sqlUtils.select(site, granularity, date)
        if (currentModel != null) {
            val updatedModel = setSelectForSpam(currentModel, domain, spam)
            if (currentModel != updatedModel) {
                sqlUtils.insert(site, updatedModel, granularity, date, limitMode.limit)
            }
        }
    }

    fun setSelectForSpam(model: ReferrersResponse, domain: String, spam: Boolean): ReferrersResponse {
        val updatedGroups = model.referrerGroups.map { group ->
            // Many groups has url as null, but they can still be spammed using their names as url
            val groupMarkedAsSpam = if (group.url == domain || group.name == domain) {
                spam
            } else {
                group.markedAsSpam
            }
            val updatedReferrers = group.referrers?.map { referrer ->
                val referrerMarkedAsSpam = if (referrer.url == domain ||
                        referrer.children?.any { it.url == domain } == true) {
                    spam
                } else {
                    referrer.markedAsSpam
                }
                referrer.copy(markedAsSpam = referrerMarkedAsSpam)
            }
            group.copy(markedAsSpam = groupMarkedAsSpam, referrers = updatedReferrers)
        }
        return model.copy(referrerGroups = updatedGroups)
    }
}
