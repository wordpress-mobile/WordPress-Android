package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.TagsModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils.TagsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class TagsStore @Inject constructor(
    private val restClient: TagsRestClient,
    private val sqlUtils: TagsSqlUtils,
    private val insightsMapper: InsightsMapper,
    private val coroutineContext: CoroutineContext
) {
    suspend fun fetchTags(siteModel: SiteModel, limitMode: Top, forced: Boolean = false) =
            withContext(coroutineContext) {
                val response = restClient.fetchTags(siteModel, max = limitMode.limit + 1, forced = forced)
                return@withContext when {
                    response.isError -> {
                        OnStatsFetched(response.error)
                    }
                    response.response != null -> {
                        sqlUtils.insert(siteModel, response.response)
                        OnStatsFetched(
                                insightsMapper.map(response.response, limitMode)
                        )
                    }
                    else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
                }
            }

    fun getTags(site: SiteModel, cacheMode: LimitMode): TagsModel? {
        return sqlUtils.select(site)?.let { insightsMapper.map(it, cacheMode) }
    }
}
