package org.wordpress.android.fluxc.store.stats.insights

import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMapper
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient
import org.wordpress.android.fluxc.persistence.InsightsSqlUtils
import org.wordpress.android.fluxc.store.StatsStore.OnStatsFetched
import org.wordpress.android.fluxc.store.StatsStore.StatsError
import org.wordpress.android.fluxc.store.StatsStore.StatsErrorType.INVALID_RESPONSE
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class PostingActivityStore
@Inject constructor(
    private val restClient: PostingActivityRestClient,
    private val sqlUtils: InsightsSqlUtils,
    private val coroutineContext: CoroutineContext,
    private val mapper: InsightsMapper
) {
    suspend fun fetchPostingActivity(
        site: SiteModel,
        startDate: Date,
        endDate: Date,
        forced: Boolean = false
    ) = withContext(coroutineContext) {
        val payload = restClient.fetchPostingActivity(site, startDate, endDate, forced)
        return@withContext when {
            payload.isError -> OnStatsFetched(payload.error)
            payload.response != null -> {
                sqlUtils.insert(site, payload.response)
                OnStatsFetched(mapper.map(payload.response, startDate, endDate))
            }
            else -> OnStatsFetched(StatsError(INVALID_RESPONSE))
        }
    }

    fun getPostingActivity(site: SiteModel, startDate: Date, endDate: Date): PostingActivityModel? {
        return sqlUtils.selectPostingActivity(site)?.let { mapper.map(it, startDate, endDate) }
    }
}
