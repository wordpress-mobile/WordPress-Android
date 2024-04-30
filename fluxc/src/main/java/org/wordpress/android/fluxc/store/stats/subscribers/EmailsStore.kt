package org.wordpress.android.fluxc.store.stats.subscribers

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.EmailsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailsStore @Inject constructor(
    private val restClient: EmailsRestClient,
    private val sqlUtils: EmailsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchEmails(
        siteModel: SiteModel,
        limitMode: LimitMode.Top,
        sortField: EmailsRestClient.SortField,
        forced: Boolean = false
    ) = coroutineEngine.withDefaultContext(STATS, this, "fetchEmails") {
        if (!forced && sqlUtils.hasFreshRequest(siteModel, limitMode.limit)) {
            return@withDefaultContext OnStatsFetched(getEmails(siteModel, limitMode, sortField), cached = true)
        }

        val response = restClient.fetchEmailsSummary(siteModel, limitMode.limit, sortField, forced)
        return@withDefaultContext when {
            response.isError -> OnStatsFetched(response.error)
            response.response != null -> {
                sqlUtils.insert(siteModel, response.response, requestedItems = limitMode.limit)
                OnStatsFetched(insightsMapper.map(response.response, limitMode, sortField))
            }

            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getEmails(
        site: SiteModel,
        cacheMode: LimitMode,
        sortField: EmailsRestClient.SortField
    ) = coroutineEngine.run(STATS, this, "getEmails") {
        sqlUtils.select(site)?.let { insightsMapper.map(it, cacheMode, sortField) }
    }
}
