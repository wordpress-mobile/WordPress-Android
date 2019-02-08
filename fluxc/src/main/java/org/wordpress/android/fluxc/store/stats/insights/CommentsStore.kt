package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.CacheMode
import org.wordpress.android.fluxc.model.stats.CacheMode.All
import org.wordpress.android.fluxc.model.stats.CommentsModel
import org.wordpress.android.fluxc.model.stats.FetchMode
import org.wordpress.android.fluxc.model.stats.FetchMode.Top
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class CommentsStore @Inject constructor(
    private val restClient: InsightsRestClient,
    private val sqlUtils: InsightsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchComments(siteModel: SiteModel, fetchMode: FetchMode, forced: Boolean = false) =
            withContext(coroutineContext) {
                val responsePayload = restClient.fetchTopComments(siteModel, forced = forced)
                return@withContext when {
                    responsePayload.isError -> {
                        OnStatsFetched(responsePayload.error)
                    }
                    responsePayload.response != null -> {
                        sqlUtils.insert(siteModel, responsePayload.response)
                        val cacheMode = if (fetchMode is Top)
                            CacheMode.Top(fetchMode.limit)
                        else
                            All
                        OnStatsFetched(insightsMapper.map(responsePayload.response, cacheMode))
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getComments(site: SiteModel, cacheMode: CacheMode): CommentsModel? {
        return sqlUtils.selectCommentInsights(site)?.let { insightsMapper.map(it, cacheMode) }
    }
}
